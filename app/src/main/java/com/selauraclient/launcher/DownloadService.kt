package com.selauraclient.launcher

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.os.Parcelable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.NotificationCompat
import androidx.core.content.IntentCompat.getParcelableArrayListExtra
import com.selauraclient.launcher.global.Data.okHttpClient
import com.selauraclient.launcher.global.Downloader.bindToService
import com.selauraclient.launcher.global.Downloader.unbindFromService
import com.selauraclient.launcher.utils.formatSpeed
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.coroutineContext

enum class DownloadState { IDLE, DOWNLOADING, PAUSED, COMPLETED, FAILED, CANCELLED }

data class DownloadInfo(
    val progress: Float = 0f,
    val speedKbps: Double = 0.0,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val state: DownloadState = DownloadState.IDLE,
    val error: String? = null,
    var currentBatchName: String? = null
)

@Parcelize
data class DownloadRequest(
    val url: String,
    val name: String,
    val size: Long,
    val outputDir: String,
    val id: String = UUID.randomUUID().toString() 
) : Parcelable

private class DownloadTaskInstance(
    val request: DownloadRequest,
    private val coroutineScope: CoroutineScope, 
    private val okHttpClient: OkHttpClient,
    private val onProgressDelta: (bytesIncrement: Long) -> Unit,
    private val onStateChange: (taskId: String, newState: DownloadState, error: String?) -> Unit,
    private val segmentsCount: Int = 4, 
    initialProgress: Long = 0L,
    initialSegmentProgress: LongArray? = null
) {
    val id: String get() = request.id
    val fileSize: Long get() = request.size
    private val outputDirFile = File(request.outputDir).apply { mkdirs() }
    val targetFile = outputDirFile.resolve(request.name)

    private val _taskState = MutableStateFlow(DownloadState.IDLE)
    val taskState = _taskState.asStateFlow()

    private val currentDownloadedBytes = AtomicLong(initialProgress)
    private var segmentJobs = mutableListOf<Job>()
    private val activeHttpCalls = ConcurrentLinkedQueue<Call>()

    private var segmentProgress: LongArray = initialSegmentProgress ?: LongArray(segmentsCount)
    private var segmentOriginalEndBytes: LongArray = LongArray(segmentsCount)
    private var segmentFileOffsets: LongArray = LongArray(segmentsCount)
    private val taskFinalized = AtomicBoolean(false)
    private var isResuming = false

    init {
        if (initialProgress > 0 && initialSegmentProgress == null) {
            logW("Task $id initialized with progress $initialProgress but no segment data. This might lead to issues if not handled.")
        }
    }
    fun startOrResume() {
        coroutineScope.launch {
            if (_taskState.value == DownloadState.DOWNLOADING && !isResuming) {
                logW("Task $id: Already downloading. Ignoring start.")
                return@launch
            }
            if (_taskState.value != DownloadState.PAUSED && isResuming) {
                logW("Task $id: Resume called but not in PAUSED state. Current: ${_taskState.value}")
                isResuming = false

                if (_taskState.value != DownloadState.IDLE && _taskState.value != DownloadState.FAILED) {
                    return@launch
                }
            }

            if (!isResuming) { 
                resetForNewStart()
            } else {
                var persistedBytes = 0L
                segmentProgress.forEach { persistedBytes += it }
                currentDownloadedBytes.set(persistedBytes)
                logI("Task $id: Resuming. Recalculated downloaded bytes from segments: ${currentDownloadedBytes.get()}")
            }

            _taskState.value = DownloadState.DOWNLOADING
            onStateChange(id, DownloadState.DOWNLOADING, null)
            prepareSegmentsIfNeeded() 
            launchSegmentDownloadsInternal()
        }
    }

    private fun resetForNewStart() {
        taskFinalized.set(false)
        currentDownloadedBytes.set(0L)
        segmentJobs.forEach { it.cancel() }
        segmentJobs.clear()
        activeHttpCalls.forEach { it.cancel() }
        activeHttpCalls.clear()
        segmentProgress = LongArray(segmentsCount) 
        isResuming = false
        logD("Task $id: Reset for new start.")
    }

    private fun prepareSegmentsIfNeeded() {
        if (segmentOriginalEndBytes.all { it == 0L } || !isResuming) {
            val segmentDiskSize = fileSize / segmentsCount
            for (i in 0 until segmentsCount) {
                segmentFileOffsets[i] = i * segmentDiskSize
                segmentOriginalEndBytes[i] = if (i == segmentsCount - 1) fileSize - 1 else (segmentFileOffsets[i] + segmentDiskSize - 1)
                if (!isResuming) segmentProgress[i] = 0L 
            }
            logD("Task $id: Segments prepared. Resuming: $isResuming")
        } else {
            logD("Task $id: Segments already prepared or resuming with existing segment data.")
        }
    }

    private fun launchSegmentDownloadsInternal() {
        segmentJobs.clear() 
        logD("Task $id: Launching segment downloads. Segments: $segmentsCount. Resuming: $isResuming")

        var allSegmentsCompleteInitially = true
        for (i in 0 until segmentsCount) {
            val segmentExpectedEndByte = segmentOriginalEndBytes[i]
            val currentSegmentProgress = segmentProgress[i]
            val segmentPhysicalStartOffset = segmentFileOffsets[i]
            val segmentTotalSizeForThisPart = (segmentExpectedEndByte - segmentPhysicalStartOffset + 1)

            if (currentSegmentProgress < segmentTotalSizeForThisPart) {
                allSegmentsCompleteInitially = false
                segmentJobs += coroutineScope.launch {
                    downloadSingleSegment(i, segmentPhysicalStartOffset, segmentExpectedEndByte, currentSegmentProgress)
                }
            } else {
                logD("Task $id Segment $i: Already completed (Progress: $currentSegmentProgress, Total: $segmentTotalSizeForThisPart).")
            }
        }
        isResuming = false 

        if (allSegmentsCompleteInitially && currentDownloadedBytes.get() >= fileSize) {
            logD("Task $id: All segments were already complete on start/resume and file size matches.")
            finalizeTask(DownloadState.COMPLETED)
            return
        }
        if (segmentJobs.isEmpty() && !allSegmentsCompleteInitially) {
            logE("Task $id: No segments to download, but not all were initially complete. Downloaded: ${currentDownloadedBytes.get()}, FileSize: $fileSize")
            if (fileSize > 0 ) handleTaskFailure("Task $id: No active segments to download despite incomplete state.")
            else finalizeTask(DownloadState.COMPLETED) 
            return
        }

        if (segmentJobs.isNotEmpty()) {
            coroutineScope.launch {
                try {
                    segmentJobs.joinAll() 
                    if (coroutineContext.isActive && !taskFinalized.get()) {
                        if (_taskState.value == DownloadState.DOWNLOADING) {
                            if (currentDownloadedBytes.get() == fileSize) {
                                finalizeTask(DownloadState.COMPLETED)
                            } else {
                                logE("Task $id: Segments joined, but download incomplete. Downloaded: ${currentDownloadedBytes.get()}, Total: $fileSize. Segments Progress: ${segmentProgress.joinToString()}")
                                if (_taskState.value != DownloadState.PAUSED) { 
                                    handleTaskFailure("Incomplete download for $id after segments joined.")
                                }
                            }
                        }
                    }
                } catch (e: kotlinx.coroutines.CancellationException) {
                    logI("Task $id: Segment supervision job cancelled. State: ${_taskState.value}", e)
                } catch (e: Exception) {
                    logE("Task $id: Error in segment supervision job", e)
                    if (_taskState.value == DownloadState.DOWNLOADING) {
                        handleTaskFailure(e.message ?: "Unknown error supervising segments for $id")
                    }
                }
            }
        }
    }

    private suspend fun downloadSingleSegment(
        segmentIndex: Int,
        physicalFileOffset: Long, 
        originalSegmentEndByte: Long, 
        currentProgressForThisSegment: Long 
    ) {
        val bytesToSkipForRangeHeader = currentProgressForThisSegment
        val actualDownloadStartOffsetInRemoteFile = physicalFileOffset + bytesToSkipForRangeHeader
        val actualWriteOffsetInLocalFile = physicalFileOffset + currentProgressForThisSegment

        if (actualDownloadStartOffsetInRemoteFile > originalSegmentEndByte) {
            logD("Task $id Segment $segmentIndex: Already fully downloaded or range invalid. Skipping.")
            return
        }

        val requestBuilder = Request.Builder()
            .url(this.request.url)
            .addHeader("Range", "bytes=$actualDownloadStartOffsetInRemoteFile-$originalSegmentEndByte")

        val call = okHttpClient.newCall(requestBuilder.build())
        activeHttpCalls.add(call)

        try {
            logD("Task $id Segment $segmentIndex: Requesting range $actualDownloadStartOffsetInRemoteFile-$originalSegmentEndByte. Writing from $actualWriteOffsetInLocalFile")
            val response = call.execute()

            if (!coroutineScope.isActive || !coroutineContext.isActive) {
                response.close()
                throw kotlinx.coroutines.CancellationException("Segment $segmentIndex ($id) cancelled post-execute (scope inactive)")
            }

            response.use { resp ->
                if (!resp.isSuccessful) {
                    val errorBody = resp.body?.string() ?: "No error body"
                    logE("Task $id Segment $segmentIndex: HTTP error ${resp.code}. URL: ${this.request.url}. Range: $actualDownloadStartOffsetInRemoteFile-$originalSegmentEndByte. Body: $errorBody")
                    throw IOException("HTTP error ${resp.code} for segment $segmentIndex ($id).")
                }
                val body = resp.body ?: throw IOException("Task $id Segment $segmentIndex: Response body is null.")
                writeSegmentDataToFile(body, actualWriteOffsetInLocalFile, segmentIndex)
            }
            logI("Task $id Segment $segmentIndex: Download and write finished successfully.")

        } catch (e: Exception) {
            when (e) {
                is kotlinx.coroutines.CancellationException -> {
                    logI("Task $id Segment $segmentIndex: Cancelled during download/write. Message: ${e.message}")
                    throw e
                }
                is IOException -> {
                    if (call.isCanceled() || e.message?.contains("Socket closed", ignoreCase = true) == true || e.message?.contains("Canceled", ignoreCase = true) == true || !coroutineScope.isActive) {
                        logI("Task $id Segment $segmentIndex: I/O operation cancelled (likely by OkHttp call.cancel() or scope change): ${e.message}")
                        throw kotlinx.coroutines.CancellationException("Segment $segmentIndex ($id) I/O operation cancelled.", e)
                    } else {
                        logE("Task $id Segment $segmentIndex: IOException during download.", e)
                        if (_taskState.value != DownloadState.PAUSED && _taskState.value != DownloadState.CANCELLED) { 
                            handleTaskFailure("Segment $segmentIndex ($id) IO Error: ${e.message}")
                        }
                        throw e 
                    }
                }
                else -> {
                    logE("Task $id Segment $segmentIndex: Unexpected error.", e)
                    if (_taskState.value != DownloadState.PAUSED && _taskState.value != DownloadState.CANCELLED) {
                        handleTaskFailure("Segment $segmentIndex ($id) Error: ${e.message}")
                    }
                    throw e 
                }
            }
        } finally {
            activeHttpCalls.remove(call)
        }
    }

    private fun writeSegmentDataToFile(body: ResponseBody, initialSeekOffsetInFile: Long, segmentIndex: Int) {
        RandomAccessFile(targetFile, "rw").use { raf ->
            raf.seek(initialSeekOffsetInFile)
            body.byteStream().use { input ->
                val buffer = ByteArray(16 * 1024) 
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    if (!coroutineScope.isActive || _taskState.value == DownloadState.PAUSED || _taskState.value == DownloadState.CANCELLED || _taskState.value == DownloadState.FAILED) {
                        logW("Task $id Segment $segmentIndex: Write aborted. Scope active: ${coroutineScope.isActive}, State: ${_taskState.value}")
                        throw kotlinx.coroutines.CancellationException("Write aborted for $id segment $segmentIndex, state changed or scope inactive.")
                    }
                    raf.write(buffer, 0, bytesRead)
                    currentDownloadedBytes.addAndGet(bytesRead.toLong())
                    segmentProgress[segmentIndex] += bytesRead.toLong()
                    onProgressDelta(bytesRead.toLong())
                }
            }
        }
        logD("Task $id Segment $segmentIndex: Finished writing. Segment progress: ${segmentProgress[segmentIndex]}. Task total downloaded: ${currentDownloadedBytes.get()}")
    }

    fun pause() {
        if (_taskState.value != DownloadState.DOWNLOADING) {
            logW("Task $id: Cannot pause, not in DOWNLOADING state. Current: ${_taskState.value}")
            return
        }
        isResuming = true 
        _taskState.value = DownloadState.PAUSED
        logI("Task $id: Pausing. Cancelling ${activeHttpCalls.size} HTTP calls and ${segmentJobs.size} segment jobs.")

        activeHttpCalls.forEach { it.cancel() } 
        activeHttpCalls.clear()

        segmentJobs.forEach { it.cancel(kotlinx.coroutines.CancellationException("Task $id paused by user")) }

        onStateChange(id, DownloadState.PAUSED, null)
        logI("Task $id: Paused. Downloaded bytes: ${currentDownloadedBytes.get()}, Segment Progress: ${segmentProgress.joinToString(",")}")
    }

    fun cancel() {
        if (taskFinalized.get() && (_taskState.value == DownloadState.COMPLETED || _taskState.value == DownloadState.CANCELLED)) {
            logW("Task $id: Already finalized as COMPLETED or CANCELLED. Ignoring cancel.")
            return
        }
        taskFinalized.set(true) 
        val previousState = _taskState.value
        _taskState.value = DownloadState.CANCELLED
        logI("Task $id: Cancelling from state $previousState. Cancelling ${activeHttpCalls.size} HTTP calls and ${segmentJobs.size} segment jobs.")

        activeHttpCalls.forEach { it.cancel() }
        activeHttpCalls.clear()

        segmentJobs.forEach { it.cancel(kotlinx.coroutines.CancellationException("Task $id cancelled by user")) }
        segmentJobs.clear()

        try {
            if (targetFile.exists()) {
                targetFile.delete()
                if (targetFile.parentFile!!.listFiles()!!.isEmpty()) {
                    targetFile.parentFile!!.delete()
                }
                logI("Task $id: Deleted partial file ${targetFile.name}")
            }
        } catch (e: Exception) {
            logE("Task $id: Error deleting file on cancellation", e)
        }
        onStateChange(id, DownloadState.CANCELLED, null)
    }

    private fun finalizeTask(finalState: DownloadState, errorMsg: String? = null) {
        if (taskFinalized.compareAndSet(false, true)) {
            _taskState.value = finalState

            activeHttpCalls.forEach { it.cancel() }
            activeHttpCalls.clear()
            segmentJobs.forEach { it.cancel(kotlinx.coroutines.CancellationException("Task $id finalizing with state $finalState")) }
            segmentJobs.clear()

            var finalErrorMessage = errorMsg
            if (finalState == DownloadState.COMPLETED) {
                if (currentDownloadedBytes.get() != fileSize) {
                    logE("Task $id: Finalize COMPLETED but bytes mismatch! Downloaded: ${currentDownloadedBytes.get()}, Expected: $fileSize. Segments: ${segmentProgress.joinToString()}")
                    _taskState.value = DownloadState.FAILED
                    finalErrorMessage = "File integrity check failed for $id on completion."
                    onStateChange(id, DownloadState.FAILED, finalErrorMessage)
                } else {
                    logI("Task $id: Completed successfully. Path: ${targetFile.absolutePath}")
                    onStateChange(id, DownloadState.COMPLETED, null)
                }
            } else if (finalState == DownloadState.FAILED) {
                logE("Task $id: Failed. Reason: $finalErrorMessage")
                onStateChange(id, DownloadState.FAILED, finalErrorMessage)
            }

        } else {
            logW("Task $id: Attempted to finalize but already finalized. Current final state: ${_taskState.value}")
        }
    }

    private fun handleTaskFailure(reason: String) {
        if (_taskState.value == DownloadState.COMPLETED || _taskState.value == DownloadState.FAILED || _taskState.value == DownloadState.CANCELLED) {
            logW("Task $id: Attempted to handle failure ($reason) but already in a final state: ${_taskState.value}")
            return
        }
        logE("Task $id: Handling failure - $reason")
        finalizeTask(DownloadState.FAILED, reason)
    }

    fun resume() {
        if (_taskState.value != DownloadState.PAUSED) {
            logW("Task $id: Cannot resume, not in PAUSED state. Current: ${_taskState.value}")
            return
        }
        logI("Task $id: Resuming download.")
        isResuming = true

        startOrResume()
    }
}

@Suppress("KotlinConstantConditions")
class DownloadService : Service() {
    private val binder = DownloadBinder()
    private val serviceExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        logE("Unhandled exception in ServiceScope", throwable)
        if (_overallDownloadInfo.value.state == DownloadState.DOWNLOADING || _overallDownloadInfo.value.state == DownloadState.PAUSED) {
            handleOverallFailure("Unhandled service error: ${throwable.message ?: "Unknown"}", null)
        }
    }
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob() + serviceExceptionHandler)

    private lateinit var notificationManager: NotificationManager
    private val _overallDownloadInfo = MutableStateFlow(DownloadInfo())
    val overallDownloadInfo = _overallDownloadInfo.asStateFlow()

    private val activeDownloadTasks = ConcurrentHashMap<String, DownloadTaskInstance>()
    private var currentBatchName: String? = null

    private val overallDownloadedBytes = AtomicLong(0L)
    private var overallTotalBytes = 0L

    private var lastProgressTimestamp = 0L
    private var lastOverallBytesSnapshot = 0L
    private var batchName = mutableStateOf("Download Batch")
    private val overallBatchFinalized = AtomicBoolean(false)

    private var progressUpdaterJob: Job? = null

    companion object {
        private const val CHANNEL_ID = "DownloadChannel"
        private const val NOTIFICATION_ID = 1
        const val ACTION_START_BATCH = "ACTION_START_BATCH"
        const val ACTION_PAUSE_BATCH = "ACTION_PAUSE_BATCH"
        const val ACTION_RESUME_BATCH = "ACTION_RESUME_BATCH"
        const val ACTION_CANCEL_BATCH = "ACTION_CANCEL_BATCH"
        const val EXTRA_DOWNLOAD_REQUEST_LIST = "EXTRA_DOWNLOAD_REQUEST_LIST"
        const val EXTRA_BATCH_NAME = "EXTRA_BATCH_NAME"

        fun Context.startDownloads(requests: List<DownloadRequest>, batchName: String = "Download Batch") {
            if (requests.isEmpty()) {
                logW("startDownloads called with empty request list.")
                return
            }
            Intent(this, DownloadService::class.java).apply {
                action = ACTION_START_BATCH
                putParcelableArrayListExtra(EXTRA_DOWNLOAD_REQUEST_LIST, ArrayList(requests))
                putExtra(EXTRA_BATCH_NAME, batchName)
            }.also { startService(it) }
            bindToService()
        }

        fun Context.pause() = startService(
            Intent(this, DownloadService::class.java).setAction(ACTION_PAUSE_BATCH)
        )

        fun Context.resume() = startService(
            Intent(this, DownloadService::class.java).setAction(ACTION_RESUME_BATCH)
        )

        fun Context.cancel() {
            startService(Intent(this, DownloadService::class.java).setAction(ACTION_CANCEL_BATCH))
            unbindFromService()
        }
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
        logD("Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        logD("onStartCommand received action: ${intent?.action}")

        when (intent?.action) {
            ACTION_START_BATCH -> {
                batchName.value = intent.getStringExtra(EXTRA_BATCH_NAME) ?: "Download Batch"
                val requests = getParcelableArrayListExtra(
                    intent,
                    EXTRA_DOWNLOAD_REQUEST_LIST,
                    DownloadRequest::class.java
                )
                if (requests.isNullOrEmpty()) {
                    logE("$ACTION_START_BATCH: No download requests found.")
                    if (activeDownloadTasks.isEmpty()) stopSelf(startId)
                    return START_NOT_STICKY
                }

                if (activeDownloadTasks.isNotEmpty() && _overallDownloadInfo.value.state != DownloadState.COMPLETED && _overallDownloadInfo.value.state != DownloadState.FAILED && _overallDownloadInfo.value.state != DownloadState.CANCELLED) {
                    logW("New batch requested while an old one is active. Cancelling previous batch.")
                    cancelOverallDownloadInternal(false)
                }
                initializeNewBatch(requests, batchName.value)
            }

            ACTION_PAUSE_BATCH -> pauseOverallDownloadInternal()
            ACTION_RESUME_BATCH -> resumeOverallDownloadInternal()
            ACTION_CANCEL_BATCH -> cancelOverallDownloadInternal(deleteFiles = true)
        }
        return START_STICKY
    }

    private fun initializeNewBatch(requests: List<DownloadRequest>, newBatchName: String) {
        logI("Initializing new batch: $newBatchName with ${requests.size} tasks.")
        currentBatchName = newBatchName
        overallBatchFinalized.set(false)
        overallDownloadedBytes.set(0L)
        overallTotalBytes = requests.sumOf { it.size }
        lastProgressTimestamp = 0L
        lastOverallBytesSnapshot = 0L

        activeDownloadTasks.clear()

        updateOverallState(
            DownloadState.IDLE,
            batchName = newBatchName
        )

        if (overallTotalBytes <= 0 && requests.isNotEmpty()) {
            logI("Batch $newBatchName contains only zero-byte files or invalid sizes.")
            updateOverallState(DownloadState.COMPLETED, 100f, 0.0, 0, 0, batchName = newBatchName)
            stopForeground(STOP_FOREGROUND_DETACH)
            notificationManager.notify(
                NOTIFICATION_ID,
                buildNotificationForOverall(100f, 0.0, DownloadState.COMPLETED)
            )

            return
        }
        if (requests.isEmpty()) {
            logI("Batch $newBatchName is empty.")
            updateOverallState(DownloadState.IDLE, batchName = newBatchName)

            return
        }

        startForeground(
            NOTIFICATION_ID,
            buildNotificationForOverall(0f, 0.0, DownloadState.IDLE, "Preparing downloads...")
        )
        updateOverallState(DownloadState.DOWNLOADING, batchName = newBatchName)

        requests.forEach { req ->
            activeDownloadTasks[req.id] = DownloadTaskInstance( req, serviceScope, okHttpClient, ::handleTaskProgressDelta, ::handleTaskStateChange).apply {
                startOrResume()
            }
        }

        if (activeDownloadTasks.isEmpty() && requests.isNotEmpty()) {
            logE("Batch $newBatchName: No tasks were activated despite requests.")
            handleOverallFailure("Failed to activate any download tasks.", newBatchName)
        } else if (activeDownloadTasks.isNotEmpty()) {
            startProgressUpdater()
        }
    }

    private fun handleTaskProgressDelta(bytesIncrement: Long) {
        overallDownloadedBytes.addAndGet(bytesIncrement)
    }

    private fun handleTaskStateChange(taskId: String, newState: DownloadState, error: String?) {
        serviceScope.launch {
            val task = activeDownloadTasks[taskId] ?: return@launch
            logD("Task $taskId state changed to $newState. Error: $error. Current batch: $currentBatchName")

            if (newState == DownloadState.FAILED) {
                logE("Task $taskId failed: $error. Triggering overall failure for batch $currentBatchName.")
                handleOverallFailure("Task ${task.request.name} failed: $error", currentBatchName)
                return@launch
            }

            checkOverallCompletionOrPause()
        }
    }

    private fun checkOverallCompletionOrPause() {
        if (activeDownloadTasks.isEmpty() && !overallBatchFinalized.get()) {
            logI("No active tasks remaining for batch $currentBatchName, but batch not finalized. Assuming completion or error state handled elsewhere.")

            return
        }
        if (overallBatchFinalized.get()) return

        var allCompleted = true
        var anyDownloading = false
        var anyPaused = false
        var anyFailed = false

        activeDownloadTasks.values.forEach { task ->
            when (task.taskState.value) {
                DownloadState.DOWNLOADING -> {
                    anyDownloading = true
                    allCompleted = false
                }

                DownloadState.PAUSED -> {
                    anyPaused = true
                    allCompleted = false
                }

                DownloadState.FAILED -> {
                    anyFailed = true
                    allCompleted = false
                }

                DownloadState.IDLE -> {
                    allCompleted = false; anyDownloading = true
                }
                else -> {}
            }
        }
        if (anyFailed) {
            if (!overallBatchFinalized.get()) handleOverallFailure("One or more tasks failed.", currentBatchName)
            return
        }

        val currentOverallState = _overallDownloadInfo.value.state
        when {
            allCompleted -> {
                if (currentOverallState != DownloadState.COMPLETED) {
                    logI("All tasks in batch $currentBatchName completed.")
                    finalizeOverallDownload(DownloadState.COMPLETED)
                }
            }

            anyDownloading -> {
                if (currentOverallState != DownloadState.DOWNLOADING) {
                    updateOverallState(DownloadState.DOWNLOADING, batchName = currentBatchName)
                    startProgressUpdater()
                }
            }

            anyPaused && !anyDownloading -> {
                if (currentOverallState != DownloadState.PAUSED) {
                    updateOverallState(DownloadState.PAUSED, batchName = currentBatchName)
                    stopProgressUpdater()
                }
            }

            activeDownloadTasks.values.all { it.taskState.value == DownloadState.CANCELLED } -> {
                if (currentOverallState != DownloadState.CANCELLED && !overallBatchFinalized.get()) {
                    logI("All tasks in batch $currentBatchName are cancelled.")
                    finalizeOverallDownload(DownloadState.CANCELLED)
                }
            }

            !anyDownloading && !anyPaused && !allCompleted && activeDownloadTasks.isNotEmpty() -> {
                logW("Batch $currentBatchName in ambiguous state: No tasks downloading or paused, but not all complete. Tasks: ${activeDownloadTasks.values.map { it.id + ":" + it.taskState.value }}")
                if (activeDownloadTasks.values.any { it.taskState.value == DownloadState.IDLE }) {
                    if (currentOverallState != DownloadState.DOWNLOADING) updateOverallState(
                        DownloadState.DOWNLOADING,
                        batchName = currentBatchName
                    )
                }
            }
        }
    }

    private fun startProgressUpdater() {
        progressUpdaterJob?.cancel()
        progressUpdaterJob = serviceScope.launch {
            logD("Progress updater started for batch $currentBatchName")
            lastProgressTimestamp = System.currentTimeMillis()
            lastOverallBytesSnapshot = overallDownloadedBytes.get()
            val speeds = mutableStateListOf<Double>()
            while (isActive && (_overallDownloadInfo.value.state == DownloadState.DOWNLOADING || _overallDownloadInfo.value.state == DownloadState.IDLE)) {
                delay(1000)
                if (_overallDownloadInfo.value.state != DownloadState.DOWNLOADING && _overallDownloadInfo.value.state != DownloadState.IDLE) break
                val now = System.currentTimeMillis()
                val currentBytes = overallDownloadedBytes.get()
                val timeDiff = now - lastProgressTimestamp
                var speed = 0.0
                val isFull = speeds.size > 5
                if (isFull) speeds.removeFirstOrNull()
                if (timeDiff > 0) {
                    val bytesDiff = currentBytes - lastOverallBytesSnapshot
                    speed = (bytesDiff.toDouble() / timeDiff * 1000) / 1024
                    speeds.add(speed)
                }
                lastProgressTimestamp = now
                lastOverallBytesSnapshot = currentBytes

                val progress =
                    if (overallTotalBytes > 0) ((currentBytes * 100f) / overallTotalBytes) else 0f

                if (_overallDownloadInfo.value.state == DownloadState.DOWNLOADING) {
                    updateOverallState(
                        DownloadState.DOWNLOADING,
                        progress.coerceIn(0f, 100f),
                        speedKbps = (if (isFull) speeds.average() else speed).coerceAtLeast(0.0),
                        downloaded = currentBytes,
                        total = overallTotalBytes,
                        batchName = currentBatchName,
                    )
                }
            }
            logD("Progress updater stopped. Reason: isActive=$isActive, State=${_overallDownloadInfo.value.state}")
        }
    }

    private fun stopProgressUpdater() {
        progressUpdaterJob?.cancel()
        progressUpdaterJob = null
        logD("Progress updater explicitly stopped.")
    }

    private fun pauseOverallDownloadInternal() {
        if (_overallDownloadInfo.value.state != DownloadState.DOWNLOADING) {
            logW("Cannot pause batch $currentBatchName, not in DOWNLOADING state. Current: ${_overallDownloadInfo.value.state}")
            return
        }
        logI("Pausing all tasks for batch $currentBatchName.")
        stopProgressUpdater()
        activeDownloadTasks.values.forEach { it.pause() }

        updateOverallState(
            DownloadState.PAUSED,
            batchName = currentBatchName
        )
        stopForeground(STOP_FOREGROUND_DETACH)
        notificationManager.notify(
            NOTIFICATION_ID, buildNotificationForOverall(
                _overallDownloadInfo.value.progress, 0.0, DownloadState.PAUSED
            )
        )
    }

    private fun resumeOverallDownloadInternal() {
        if (_overallDownloadInfo.value.state != DownloadState.PAUSED) {
            logW("Cannot resume batch $currentBatchName, not in PAUSED state. Current: ${_overallDownloadInfo.value.state}")
            return
        }
        logI("Resuming all tasks for batch $currentBatchName.")
        startForeground(
            NOTIFICATION_ID, buildNotificationForOverall(
                _overallDownloadInfo.value.progress,
                _overallDownloadInfo.value.speedKbps,
                DownloadState.DOWNLOADING,
                "Resuming downloads..."
            )
        )
        updateOverallState(
            DownloadState.DOWNLOADING,
            batchName = currentBatchName
        )
        activeDownloadTasks.values.filter { it.taskState.value == DownloadState.PAUSED }
            .forEach { it.resume() }
        startProgressUpdater()
    }

    private fun cancelOverallDownloadInternal(deleteFiles: Boolean) {
        val batchToCancel = currentBatchName
        logI("Cancelling batch $batchToCancel. Delete files: $deleteFiles")
        if (overallBatchFinalized.get() && (_overallDownloadInfo.value.state == DownloadState.COMPLETED || _overallDownloadInfo.value.state == DownloadState.CANCELLED)) {
            logW("Batch $batchToCancel already finalized as COMPLETED or CANCELLED. Ignoring cancel command.")
            return
        }

        overallBatchFinalized.set(true)
        stopProgressUpdater()

        val tasksToCancel = ArrayList(activeDownloadTasks.values)
        activeDownloadTasks.clear()

        tasksToCancel.forEach { task ->
            if (deleteFiles) {
                task.cancel()
            } else {
                task.cancel()
                logI("Task ${task.id} cancelled for batch $batchToCancel. File deletion was: $deleteFiles (task default may override)")
            }
        }

        updateOverallState(
            DownloadState.CANCELLED,
            error = if (deleteFiles) "Batch cancelled by user." else "Batch operation superseded.",
            batchName = batchToCancel
        )
        stopForeground(STOP_FOREGROUND_REMOVE)
        notificationManager.notify(
            NOTIFICATION_ID, buildNotificationForOverall(
                _overallDownloadInfo.value.progress, 0.0, DownloadState.CANCELLED
            )
        )

        if (activeDownloadTasks.isEmpty()) {
            logI("All tasks cancelled for batch $batchToCancel, no active tasks remaining.")
        }
    }

    private fun finalizeOverallDownload(finalState: DownloadState) {
        if (overallBatchFinalized.compareAndSet(false, true)) {
            logI("Finalizing batch $currentBatchName with state $finalState. Downloaded: ${overallDownloadedBytes.get()}, Total: $overallTotalBytes")

            val finalProgress =
                if (finalState == DownloadState.COMPLETED && overallTotalBytes > 0) 100f
                else if (overallTotalBytes > 0) ((overallDownloadedBytes.get() * 100f) / overallTotalBytes)
                else 0f

            updateOverallState(
                finalState,
                finalProgress.coerceIn(0f, 100f),
                0.0,
                overallDownloadedBytes.get(),
                overallTotalBytes,
                batchName = currentBatchName
            )
            stopProgressUpdater()
            stopForeground(STOP_FOREGROUND_DETACH)
            notificationManager.notify(
                NOTIFICATION_ID, buildNotificationForOverall(
                    _overallDownloadInfo.value.progress, 0.0, finalState
                )
            )

            if (finalState == DownloadState.COMPLETED) {
                logI("Batch $currentBatchName: All downloads completed.")
            } else {
                logI("Batch $currentBatchName: Finalized as $finalState.")
            }
        } else {
            logW("Attempted to finalize batch $currentBatchName but already finalized. Current overall state: ${_overallDownloadInfo.value.state}")
        }
    }

    private fun handleOverallFailure(reason: String, batchNameForFailure: String?) {
        if (overallBatchFinalized.compareAndSet(false, true)) {
            logE("Handling overall failure for batch $batchNameForFailure: $reason")
            stopProgressUpdater()
            val tasksToHandleFailure = activeDownloadTasks.values.toList()
            tasksToHandleFailure.forEach { task ->
                if (task.taskState.value != DownloadState.FAILED &&
                    task.taskState.value != DownloadState.COMPLETED &&
                    task.taskState.value != DownloadState.CANCELLED
                ) {
                    task.cancel()
                }
            }

            updateOverallState(DownloadState.FAILED, error = reason, batchName = batchNameForFailure)
            stopForeground(STOP_FOREGROUND_DETACH)
            notificationManager.notify(
                NOTIFICATION_ID, buildNotificationForOverall(
                    _overallDownloadInfo.value.progress, 0.0, DownloadState.FAILED
                )
            )
            logE("Batch $batchNameForFailure failed. Reason: $reason")
        } else {
            logW("handleOverallFailure for batch $batchNameForFailure called but batch already finalized. Reason: $reason. Current State: ${_overallDownloadInfo.value.state}")
        }
    }

    private fun updateOverallState(
        state: DownloadState,
        progress: Float = _overallDownloadInfo.value.progress,
        speedKbps: Double = _overallDownloadInfo.value.speedKbps,
        downloaded: Long = overallDownloadedBytes.get(),
        total: Long = overallTotalBytes,
        error: String? = _overallDownloadInfo.value.error,
        batchName: String?
    ) {
        if (batchName != currentBatchName && !(state == DownloadState.FAILED && currentBatchName == null)) {
            logW("State update for batch $batchName ignored, current batch is $currentBatchName. New state: $state")

            if (!(state == DownloadState.FAILED && currentBatchName == null && activeDownloadTasks.isEmpty())) {
                return
            }
        }

        val newError = if (state == DownloadState.FAILED && error != null) error
        else if (state != DownloadState.FAILED) null
        else _overallDownloadInfo.value.error

        _overallDownloadInfo.update {
            it.copy(
                progress = progress,
                speedKbps = speedKbps,
                downloadedBytes = downloaded,
                totalBytes = total,
                state = state,
                error = newError,
                currentBatchName = currentBatchName
            )
        }
        notificationManager.notify(
            NOTIFICATION_ID,
            buildNotificationForOverall(progress, speedKbps, state)
        )

        logD("Overall state updated: ${_overallDownloadInfo.value}")
    }

    override fun onBind(intent: Intent): IBinder = binder
    inner class DownloadBinder : Binder() {
        fun getService(): DownloadService = this@DownloadService
    }

    override fun onDestroy() {
        super.onDestroy()
        logD("Service Destroyed. Cancelling service scope and active downloads.")
        val tasksToCancelOnDestroy = ArrayList(activeDownloadTasks.values)
        tasksToCancelOnDestroy.forEach {
            it.pause()
        }
        activeDownloadTasks.clear()
        serviceScope.cancel("Service is being destroyed")
        stopProgressUpdater()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "Downloads", NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "File download progress for batch"
            setSound(null, null)
            enableVibration(false)
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun buildNotificationForOverall(
        progress: Float,
        speed: Double,
        state: DownloadState,
        customContent: String? = null
    ): Notification {
        val title = when (state) {
            DownloadState.IDLE -> "Preparing: ${batchName.value}"
            DownloadState.DOWNLOADING -> "Downloading: ${batchName.value} (${activeDownloadTasks.size} files)"
            DownloadState.PAUSED -> "Paused: ${batchName.value}"
            DownloadState.COMPLETED -> "Completed: ${batchName.value}"
            DownloadState.FAILED -> "Failed: ${batchName.value}"
            DownloadState.CANCELLED -> "Cancelled: ${batchName.value}"
        }
        val contentText = customContent ?: when (state) {
            DownloadState.IDLE -> "Waiting to start batch..."
            DownloadState.DOWNLOADING -> "Overall Speed: ${formatSpeed(speed)}"
            DownloadState.PAUSED -> "Batch download is paused."
            DownloadState.COMPLETED -> "Batch download finished successfully."
            DownloadState.FAILED -> "Error: ${_overallDownloadInfo.value.error ?: "Unknown batch error"}"
            DownloadState.CANCELLED -> "Batch download was cancelled."
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setOnlyAlertOnce(true)
            .setOngoing(state == DownloadState.DOWNLOADING || state == DownloadState.PAUSED || state == DownloadState.IDLE)

        val pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

        when (state) {
            DownloadState.DOWNLOADING, DownloadState.IDLE -> {
                builder.setSmallIcon(android.R.drawable.stat_sys_download)
                builder.setProgress(100, progress.toInt(), false)
                val pauseIntent =
                    Intent(this, DownloadService::class.java).setAction(ACTION_PAUSE_BATCH)
                val pendingPauseIntent =
                    PendingIntent.getService(this, 0, pauseIntent, pendingIntentFlags)
                builder.addAction(android.R.drawable.ic_media_pause, "Pause", pendingPauseIntent)

            }
            DownloadState.PAUSED -> {
                builder.setSmallIcon(android.R.drawable.stat_sys_download)
                builder.setProgress(100, progress.toInt(), false)
                val resumeIntent =
                    Intent(this, DownloadService::class.java).setAction(ACTION_RESUME_BATCH)
                val pendingResumeIntent =
                    PendingIntent.getService(this, 2, resumeIntent, pendingIntentFlags)
                builder.addAction(android.R.drawable.ic_media_play, "Resume", pendingResumeIntent)
            }
            else -> {
                builder.setSmallIcon(
                    when (state) {
                        DownloadState.COMPLETED -> android.R.drawable.stat_sys_download_done
                        DownloadState.CANCELLED -> R.drawable.ic_logo
                        else -> android.R.drawable.stat_notify_error
                    }
                )
                builder.setProgress(0, 0, false)
            }
        }

        if (state == DownloadState.DOWNLOADING || state == DownloadState.PAUSED || state == DownloadState.IDLE) {
            val cancelIntent =
                Intent(this, DownloadService::class.java).setAction(ACTION_CANCEL_BATCH)
            val pendingCancelIntent =
                PendingIntent.getService(this, 1, cancelIntent, pendingIntentFlags)
            builder.addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Cancel",
                pendingCancelIntent
            )
        }
        return builder.build()
    }
    
    
}


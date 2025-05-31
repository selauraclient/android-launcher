package com.selauraclient.launcher

import android.database.Cursor
import android.database.MatrixCursor
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Root
import android.provider.DocumentsProvider
import android.webkit.MimeTypeMap
import java.io.File

class StorageProvider : DocumentsProvider() {

    private lateinit var internalGamesDir: File
    private lateinit var externalGamesDir: File

    override fun onCreate(): Boolean {
        internalGamesDir = File(context!!.dataDir, "games").apply {
            if (!exists()) mkdirs()
        }
        externalGamesDir = File(context!!.getExternalFilesDir(null), "games").apply {
            if (!exists()) mkdirs()
        }
        return true
    }

    override fun queryRoots(projection: Array<out String>?): Cursor {
        val result = MatrixCursor(arrayOf(
            Root.COLUMN_ROOT_ID,
            Root.COLUMN_DOCUMENT_ID,
            Root.COLUMN_TITLE,
            Root.COLUMN_SUMMARY,
            Root.COLUMN_FLAGS,
            Root.COLUMN_MIME_TYPES,
            Root.COLUMN_ICON
        ))

        result.newRow().apply {
            add(Root.COLUMN_ROOT_ID, "internal_games_root")
            add(Root.COLUMN_DOCUMENT_ID, internalGamesDir.absolutePath)
            add(Root.COLUMN_TITLE, "Minecraft Game Files")
            add(Root.COLUMN_SUMMARY, "Internal storage")
            add(Root.COLUMN_FLAGS, Root.FLAG_SUPPORTS_CREATE)
            add(Root.COLUMN_MIME_TYPES, "*/*")
            add(Root.COLUMN_ICON, R.mipmap.ic_launcher)
        }

        result.newRow().apply {
            add(Root.COLUMN_ROOT_ID, "external_games_root")
            add(Root.COLUMN_DOCUMENT_ID, externalGamesDir.absolutePath)
            add(Root.COLUMN_TITLE, "Minecraft Game Files")
            add(Root.COLUMN_SUMMARY, "External storage")
            add(Root.COLUMN_FLAGS, Root.FLAG_SUPPORTS_CREATE)
            add(Root.COLUMN_MIME_TYPES, "*/*")
            add(Root.COLUMN_ICON, R.mipmap.ic_launcher)
        }

        return result
    }

    override fun queryDocument(documentId: String, projection: Array<out String>?): Cursor {
        val file = File(documentId)
        val result = MatrixCursor(DEFAULT_DOCUMENT_PROJECTION)
        includeFile(result, file)
        return result
    }

    override fun queryChildDocuments(
        parentDocumentId: String,
        projection: Array<out String>?,
        sortOrder: String?
    ): Cursor {
        val parent = File(parentDocumentId)
        val result = MatrixCursor(DEFAULT_DOCUMENT_PROJECTION)
        parent.listFiles()?.forEach { file ->
            includeFile(result, file)
        }
        return result
    }

    override fun openDocument(
        documentId: String,
        mode: String,
        signal: CancellationSignal?
    ): ParcelFileDescriptor {
        val file = File(documentId)
        val accessMode = ParcelFileDescriptor.parseMode(mode)
        return ParcelFileDescriptor.open(file, accessMode)
    }

    private fun includeFile(cursor: MatrixCursor, file: File) {
        val flags = DocumentsContract.Document.FLAG_SUPPORTS_DELETE or
                DocumentsContract.Document.FLAG_SUPPORTS_WRITE or
                DocumentsContract.Document.FLAG_SUPPORTS_RENAME

        val row = cursor.newRow()
        row.add(DocumentsContract.Document.COLUMN_DOCUMENT_ID, file.absolutePath)
        row.add(DocumentsContract.Document.COLUMN_DISPLAY_NAME, file.name)
        row.add(DocumentsContract.Document.COLUMN_SIZE, file.length())
        row.add(DocumentsContract.Document.COLUMN_MIME_TYPE, getMimeType(file))
        row.add(DocumentsContract.Document.COLUMN_LAST_MODIFIED, file.lastModified())
        row.add(DocumentsContract.Document.COLUMN_FLAGS, flags)
    }
    
    override fun deleteDocument(documentId: String) {
        val file = File(documentId)
        if (!file.delete()) {
            throw IllegalStateException("Failed to delete document")
        }
    }

    override fun createDocument(
        parentDocumentId: String,
        mimeType: String,
        displayName: String
    ): String {
        val parent = File(parentDocumentId)
        val file = File(parent, displayName)
        file.createNewFile()
        return file.absolutePath
    }

    override fun renameDocument(documentId: String, displayName: String): String {
        val file = File(documentId)
        val newFile = File(file.parentFile, displayName)
        if (!file.renameTo(newFile)) {
            throw IllegalStateException("Failed to rename document")
        }
        return newFile.absolutePath
    }
    private fun getMimeType(file: File): String {
        return if (file.isDirectory) {
            DocumentsContract.Document.MIME_TYPE_DIR
        } else {
            val extension = file.extension.lowercase()
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "application/octet-stream"
        }
    }

    companion object {
        private val DEFAULT_DOCUMENT_PROJECTION = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            DocumentsContract.Document.COLUMN_FLAGS
        )
    }
}

// noinspection ModifierParameter
package com.selauraclient.launcher.ui.core

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.selauraclient.launcher.R
import com.selauraclient.launcher.global.Data
import com.selauraclient.launcher.global.DialogState
import com.selauraclient.launcher.ui.theme.poppins

@Composable
fun CategoryTitle(text: String) {
    Text(text, Modifier.padding(16.dp, 6.dp), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
}

@Composable
fun AnimatedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(7.dp),
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable (RowScope.() -> Unit)
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (isPressed) .95f else 1f)
    Button(onClick, modifier
        .scale(scale)
        .pointerInput(isPressed) {
            awaitPointerEventScope {
                isPressed = if (isPressed) {
                    waitForUpOrCancellation()
                    false
                } else {
                    awaitFirstDown(false)
                    true
                }
            }
        }, enabled, shape, colors, elevation, border, contentPadding, interactionSource, content
    )
}

@Composable
fun PermissionHandler(
    checkPermission: () -> Boolean,
    requestPermission: (ManagedActivityResultLauncher<Intent, ActivityResult>) -> Unit,
    onPermissionResult: (granted: Boolean) -> Unit = {},
    dialogContent: @Composable (() -> Unit)? = null
) {
    var hasPermission by remember { mutableStateOf(checkPermission()) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        hasPermission = checkPermission()
    }
    LaunchedEffect(hasPermission) {
        if (!hasPermission) {
            Data.dialogState.value = DialogState(
                show = true,
                cancellable = false,
                okText = "Grant",
                onOk = {
                    requestPermission(launcher)
                }
            ) {
                dialogContent?.invoke()
            }
        } else {
            Data.dialogState.value.hide()
            onPermissionResult(true)
        }
    }
}

@Composable
fun IconButton(
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
   modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors(),
    content: @Composable () -> Unit
) {
    Box(
        modifier
            .minimumInteractiveComponentSize()
            .size(40.dp)
            .clip(CircleShape)
            .background(if (enabled) colors.containerColor else colors.disabledContainerColor)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
                enabled = enabled,
                role = Role.Button
            ),
        Alignment.Center
    ) {
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Dialog(dialogState: DialogState) {
    if (!dialogState.show) return
    BasicAlertDialog({dialogState.onDismiss(); dialogState.hideIfCancellable()}) {
        Surface(shape = MaterialTheme.shapes.large) {
            Column(dialogState.modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                dialogState.content(dialogState)
                Row(Modifier.fillMaxWidth(), Arrangement.End) {
                    if (dialogState.cancellable && dialogState.cancelText.isNotEmpty()) {
                        AnimatedButton(dialogState.onCancel) { Text(dialogState.cancelText) }
                    }
                    if (dialogState.okText.isNotEmpty()) {
                        AnimatedButton(dialogState.onOk) { Text(dialogState.okText) }
                    }
                }
            }
        }
    }
}

@Composable
fun Container(
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardColors(containerColor, contentColor, containerColor, contentColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp, disabledElevation = 1.dp),
        border = BorderStroke(0.dp, MaterialTheme.colorScheme.outline),
    ) {
        this.content()
    }
}

fun header() = @Composable @ReadOnlyComposable {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
            painterResource(R.drawable.ic_logo),
            "Logo",
            Modifier
                .padding(5.dp)
                .size(32.dp),
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
        )
        Text("selaura", fontSize = 25.sp, fontWeight = FontWeight.Thin, fontFamily = poppins)
    }
}

@Composable
fun Title(text: String) {
    Text(text,Modifier.padding(bottom = 5.dp), fontSize = 18.sp)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheet(showBottomSheet: Boolean, onDismissRequest: () -> Unit = { }, skipExpanded: Boolean = false, content: @Composable () -> Unit) {
    if (!showBottomSheet) return
    ModalBottomSheet(
        onDismissRequest = { onDismissRequest() },
        sheetState = rememberModalBottomSheetState(skipExpanded),
    ) {
        content()
    }
}

@Composable
fun BaseDialog(showDialog: MutableState<Boolean>, onDismissRequest: () -> Unit = {}, content: @Composable () -> Unit) {
    var animateDialog by remember { mutableStateOf(false) }
    LaunchedEffect(showDialog.value) { if (showDialog.value) animateDialog = true }
    if (!animateDialog) return
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        var animateIn by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { animateIn = true }
        Box(Modifier.pointerInput(Unit) { detectTapGestures { onDismissRequest() } }.fillMaxSize().background(Color.Black.copy(alpha = .56f)))
        AnimatedComposable(animateIn && showDialog.value, { animateDialog = false }){
            content()
        }
        BackHandler {
            onDismissRequest()
        }
    }
}

val animationEnter = fadeIn(spring(stiffness = Spring.StiffnessHigh)) + scaleIn(
    spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMediumLow), .8f
)
val animationExit = scaleOut(
    spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMediumLow), .8f
) + fadeOut(spring(stiffness = Spring.StiffnessMedium))

@Composable
private fun AnimatedComposable(visible: Boolean, onDismissRequest: () -> Unit = {}, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = animationEnter,
        exit = animationExit
    ) {
        content()
        DisposableEffect(Unit) { onDispose { onDismissRequest() } }
    }
}

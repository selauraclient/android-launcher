package com.selauraclient.launcher.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.crossfade
import com.selauraclient.launcher.R
import com.selauraclient.launcher.global.Data
import com.selauraclient.launcher.global.Data.authData
import com.selauraclient.launcher.global.Data.authDataLoading
import com.selauraclient.launcher.global.Data.dialogState
import com.selauraclient.launcher.global.Data.showLoginScreen
import com.selauraclient.launcher.global.Data.showMessage
import com.selauraclient.launcher.global.Data.showMoreDialog
import com.selauraclient.launcher.global.DialogState
import com.selauraclient.launcher.ui.core.BaseDialog
import com.selauraclient.launcher.utils.NetworkHelper
import com.selauraclient.launcher.utils.SettingsManager
import com.selauraclient.launcher.utils.browse

data class Option(val title: String, val icon: ImageVector, val onClick: () -> Unit)

@Composable
fun MoreDialog() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsManager = SettingsManager(context, scope)
    val showLogs by settingsManager.getBooleanAsFlow("show_logs").collectAsState(false)
    BaseDialog(showMoreDialog, { showMoreDialog.value = false }) {
        Surface(Modifier.clip(MaterialTheme.shapes.extraLarge)) {
            Column(
                Modifier
                    .width(350.dp)
                    .verticalScroll(rememberScrollState()),
                Arrangement.spacedBy(4.dp),
                Alignment.CenterHorizontally
            ) {
                DialogTopBar(stringResource(R.string.app_name)) { showMoreDialog.value = false }
                Surface(Modifier.padding(horizontal = 10.dp).fillMaxWidth(), MaterialTheme.shapes.extraLarge, MaterialTheme.colorScheme.surfaceContainer, shadowElevation = 1.dp, border = BorderStroke(.5.dp, MaterialTheme.colorScheme.outline)) {
                    AccountHeader()
                }
                Column {
                    listOf(
                        Option(
                            "Settings",
                            Icons.Default.Settings
                        ) {
                            //nothing yet (lazy)
                        },
                        Option(if (showLogs) "Hide Logs" else "Show Logs", Icons.Default.Terminal) { settingsManager.setBoolean("show_logs", !showLogs) },
                        Option("About", Icons.Default.Info) { context.browse("https://github.com/selauraclient/android-launcher/edit/master/README.md") }
                    ).forEach { option ->
                        SettingOption(option)
                    }
                }
                Footer()
            }
        }
    }
}
@Composable
private fun AccountHeader() {
    val loading = authDataLoading.value
    val context = LocalContext.current
    Column(
        modifier = Modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val authData = authData.value
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(15.dp, Alignment.Start)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(authData?.userProfile?.artwork?.url ?: R.drawable.ic_google)
                    .crossfade(true)
                    .allowHardware(false)
                    .build(),
                contentDescription = "Account Profile",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .requiredSize(36.dp)
                    .clip(CircleShape)
            )
            Column {
                val name = authData?.userProfile?.name ?: "Sign In to download versions"
                val email = authData?.userProfile?.email ?: "Sign In to download versions"
                repeat(2) {
                    Text(
                        text = if (it == 0) name else email,
                        modifier = Modifier
                            .padding(1.dp)
                            .clip(MaterialTheme.shapes.extraSmall),
                        fontWeight = if (it == 0) FontWeight.Normal else FontWeight.Light,
                        fontSize = if (it == 0) 15.sp else 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        OutlinedButton(
            onClick = {
                if (!loading) {
                    if (authData != null) {
                        dialogState.value = DialogState(
                            true, true, "Logout", "",{
                                val accountData = context.getSharedPreferences("account_data", Context.MODE_PRIVATE)
                                accountData.edit(true) {
                                    remove("token")
                                    remove("email")
                                }
                                Data.authData.value = null
                                dialogState.value.hide()
                            }
                        ) {
                            Text("Logout", style = MaterialTheme.typography.titleLarge)
                            Text("Are you sure you want to log out?")
                        }
                    } else {
                        if (NetworkHelper(context).checkConnection()) {
                            showLoginScreen.value = true
                        } else showMessage("No internet connection")
                    }
                }
            },
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier
                .fillMaxWidth()
                .let {
                    if (loading) {
                        it
                            .padding(vertical = 4.dp)
                            .height(ButtonDefaults.MinHeight)
                            .clip(RoundedCornerShape(12.dp))
                    } else it
                }
        ) { Text(if (authData != null) "Log Out" else "Sign In") }
    }
}

@Composable
fun SettingOption(option: Option) {
    Row(Modifier.fillMaxWidth().height(50.dp).clickable { option.onClick() }.padding(horizontal = 24.dp), Arrangement.spacedBy(12.dp), Alignment.CenterVertically) {
        Icon(option.icon, option.title, Modifier.size(24.dp))
        Text(option.title)
    }
}

@Composable
fun DialogTopBar(title: String, extras: @Composable BoxScope.() -> Unit = {}, onClose: () -> Unit) {
    Box(Modifier.fillMaxWidth()) {
        IconButton({ onClose() }) {
            Icon(Icons.Default.Close,"Close", Modifier.size(24.dp))
        }
        Text(
            modifier = Modifier
                .padding(10.dp)
                .align(Alignment.Center),
            text = title,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        extras(this)
    }
}

@Composable
private fun Footer() {
    Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally), Alignment.CenterVertically) {
        LinkButton("Privacy Policy", "https://github.com/selauraclient/android-launcher/blob/master/README.md#-privacy-policy-notice")
        Text("•")
        LinkButton("Terms of Service", "https://github.com/selauraclient/android-launcher/blob/master/README.md")
    }
}

@SuppressLint("ModifierParameter")
@Composable
fun LinkButton(text: String, link: String, colors: ButtonColors = ButtonDefaults.textButtonColors(),  modifier: Modifier = Modifier) {
    val context = LocalContext.current
    TextButton({ context.browse(link) }, modifier, colors = colors) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
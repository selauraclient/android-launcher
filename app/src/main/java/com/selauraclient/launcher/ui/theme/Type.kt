package com.selauraclient.launcher.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import com.selauraclient.launcher.R

val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)
val poppins = FontFamily(Font(googleFont = GoogleFont("Poppins"), fontProvider = provider))
@Composable
fun typography(): Typography {
    return Typography().run {
        copy(
            displayLarge = displayLarge.copy(fontFamily = poppins),
            displayMedium = displayMedium.copy(fontFamily = poppins),
            displaySmall = displaySmall.copy(fontFamily = poppins),
            headlineLarge = headlineLarge.copy(fontFamily = poppins),
            headlineMedium = headlineMedium.copy(fontFamily = poppins),
            headlineSmall = headlineSmall.copy(fontFamily = poppins),
            titleLarge = titleLarge.copy(fontFamily = poppins),
            titleMedium = titleMedium.copy(fontFamily = poppins),
            titleSmall = titleSmall.copy(fontFamily = poppins),
            bodyLarge = bodyLarge.copy(fontFamily = poppins),
            bodyMedium = bodyMedium.copy(fontFamily = poppins),
            bodySmall = bodySmall.copy(fontFamily = poppins),
            labelLarge = labelLarge.copy(fontFamily = poppins),
            labelMedium = labelMedium.copy(fontFamily = poppins),
            labelSmall = labelSmall.copy(fontFamily = poppins)
        )
    }
}
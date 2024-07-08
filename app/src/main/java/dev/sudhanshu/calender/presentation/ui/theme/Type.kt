package dev.sudhanshu.calender.presentation.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.material.Typography as MaterialTypography
import androidx.compose.ui.unit.sp
import dev.sudhanshu.calender.R


val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

// Define Poppins FontFamily
val poppinsFont = GoogleFont("Poppins")

val poppinsFontFamily  = FontFamily(
    Font(googleFont = poppinsFont, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = poppinsFont, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = poppinsFont, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = poppinsFont, fontProvider = provider, weight = FontWeight.Bold)
)


// Define Typography
val Typography = MaterialTypography(
    h1 = TextStyle(
        fontFamily = poppinsFontFamily,
        fontWeight = FontWeight.Bold
    ),
    h2 = TextStyle(
        fontFamily = poppinsFontFamily,
        fontWeight = FontWeight.SemiBold
    ),
    h3 = TextStyle(
        fontFamily = poppinsFontFamily,
        fontWeight = FontWeight.Medium
    ),
    h4 = TextStyle(
        fontFamily = poppinsFontFamily,
        fontWeight = FontWeight.Normal
    ),
    h5 = TextStyle(
        fontFamily = poppinsFontFamily,
        fontWeight = FontWeight.Light
    )
    // Define other text styles as needed
)
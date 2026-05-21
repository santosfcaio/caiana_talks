package com.caiana.talks.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import com.caiana.talks.R

private val Antonio = FontFamily(Font(R.font.antonio_regular))

val LcarsTypography = Typography(
    displayLarge = TextStyle(fontFamily = Antonio, fontSize = 32.sp, letterSpacing = 2.sp),
    displayMedium = TextStyle(fontFamily = Antonio, fontSize = 28.sp, letterSpacing = 2.sp),
    headlineLarge = TextStyle(fontFamily = Antonio, fontSize = 24.sp, letterSpacing = 1.sp),
    headlineMedium = TextStyle(fontFamily = Antonio, fontSize = 20.sp, letterSpacing = 1.sp),
    titleLarge = TextStyle(fontFamily = Antonio, fontSize = 18.sp, letterSpacing = 0.5.sp),
    titleMedium = TextStyle(fontFamily = Antonio, fontSize = 16.sp, letterSpacing = 0.5.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, letterSpacing = 0.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, letterSpacing = 0.sp),
    bodySmall = TextStyle(fontSize = 12.sp, letterSpacing = 0.sp),
    labelLarge = TextStyle(fontSize = 14.sp, letterSpacing = 0.sp),
    labelMedium = TextStyle(fontSize = 12.sp, letterSpacing = 0.sp),
    labelSmall = TextStyle(fontSize = 11.sp, letterSpacing = 0.sp),
)

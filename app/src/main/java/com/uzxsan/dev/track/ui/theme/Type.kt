package com.uzxsan.dev.track.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.uzxsan.dev.track.R

/*
 * Inter (SIL Open Font License, see assets/licenses/Inter-OFL.txt) stands in for Apple's SF Pro,
 * which can't be bundled in Android apps. Like SF Pro Display / Text, Inter has two optical sizes:
 * Display for large headings and the regular cut for everything else.
 */
val InterText = FontFamily(
    Font(R.font.inter_light, FontWeight.Light),
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

val InterDisplay = FontFamily(
    Font(R.font.inter_display_extralight, FontWeight.ExtraLight),
    Font(R.font.inter_display_light, FontWeight.Light),
    Font(R.font.inter_display_regular, FontWeight.Normal),
    Font(R.font.inter_display_medium, FontWeight.Medium),
)

private val base = Typography()

private fun TextStyle.inter(family: FontFamily, tracking: TextUnit) =
    copy(fontFamily = family, letterSpacing = tracking)

// Material's tracking is tuned for Roboto; Inter (like SF) reads better slightly tighter.
val Typography = Typography(
    displayLarge = base.displayLarge.inter(InterDisplay, (-0.5).sp),
    displayMedium = base.displayMedium.inter(InterDisplay, (-0.4).sp),
    displaySmall = base.displaySmall.inter(InterDisplay, (-0.3).sp),
    headlineLarge = base.headlineLarge.inter(InterDisplay, (-0.3).sp),
    headlineMedium = base.headlineMedium.inter(InterDisplay, (-0.25).sp),
    headlineSmall = base.headlineSmall.inter(InterDisplay, (-0.2).sp),
    titleLarge = base.titleLarge.inter(InterDisplay, (-0.15).sp),
    titleMedium = base.titleMedium.inter(InterText, (-0.1).sp),
    titleSmall = base.titleSmall.inter(InterText, 0.sp),
    bodyLarge = base.bodyLarge.inter(InterText, (-0.2).sp),
    bodyMedium = base.bodyMedium.inter(InterText, (-0.1).sp),
    bodySmall = base.bodySmall.inter(InterText, 0.sp),
    labelLarge = base.labelLarge.inter(InterText, 0.sp),
    labelMedium = base.labelMedium.inter(InterText, 0.1.sp),
    labelSmall = base.labelSmall.inter(InterText, 0.1.sp),
)

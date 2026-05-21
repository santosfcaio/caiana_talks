package com.caiana.talks.ui.theme

import androidx.compose.foundation.LocalIndication
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

private val LcarsColorScheme = darkColorScheme(
    background = LcarsColors.Black,
    surface = LcarsColors.Surface,
    primary = LcarsColors.Orange,
    secondary = LcarsColors.Blue,
    tertiary = LcarsColors.Purple,
    error = LcarsColors.Maroon,
    onBackground = LcarsColors.Text,
    onSurface = LcarsColors.Text,
    onPrimary = LcarsColors.Black,
    onSecondary = LcarsColors.Black,
    onTertiary = LcarsColors.Black,
    onError = LcarsColors.Text,
    surfaceVariant = LcarsColors.Surface,
    onSurfaceVariant = LcarsColors.TextDim,
)

@Composable
fun LcarsTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalIndication provides LcarsIndication) {
        MaterialTheme(
            colorScheme = LcarsColorScheme,
            typography = LcarsTypography,
            shapes = Shapes(
                extraSmall = LcarsShapes.dataPanel,
                small = LcarsShapes.dataPanel,
                medium = LcarsShapes.dataPanel,
            ),
            content = content,
        )
    }
}

package com.caiana.talks.ui.theme.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.caiana.talks.ui.theme.LcarsColors
import com.caiana.talks.ui.theme.LcarsShapes

@Composable
fun LcarsFrame(
    accentColor: Color,
    modifier: Modifier = Modifier,
    topBarHeight: Dp = 48.dp,
    bottomBarHeight: Dp = 24.dp,
    leftStripWidth: Dp = 20.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    Column(modifier = modifier.fillMaxSize().background(LcarsColors.Black)) {
        Box(Modifier.fillMaxWidth().height(topBarHeight).background(accentColor, RectangleShape))
        Row(Modifier.weight(1f).fillMaxWidth()) {
            Box(
                Modifier.width(leftStripWidth).fillMaxHeight()
                    .background(accentColor, LcarsShapes.panelElbow)
            )
            Spacer(Modifier.width(8.dp))
            Box(modifier = Modifier.weight(1f).fillMaxHeight(), content = content)
        }
        Box(
            Modifier.fillMaxWidth().height(bottomBarHeight)
                .background(accentColor.copy(alpha = 0.6f), RectangleShape)
        )
    }
}

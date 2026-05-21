package com.caiana.talks.ui.theme.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.caiana.talks.ui.theme.LcarsColors

internal fun activeSegmentCount(elapsedMs: Long, segmentCount: Int): Int =
    ((elapsedMs / 250L) % (segmentCount + 1)).toInt()

@Composable
fun LcarsProgressBar(
    modifier: Modifier = Modifier,
    segmentCount: Int = 12,
    color: Color = LcarsColors.Orange,
) {
    val totalDurationMs = segmentCount * 250
    val infiniteTransition = rememberInfiniteTransition(label = "lcarsProgress")
    val elapsed by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = totalDurationMs.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = totalDurationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "elapsedMs",
    )
    val active = activeSegmentCount(elapsed.toLong(), segmentCount)

    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(segmentCount) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(16.dp)
                    .background(
                        color = if (index < active) color else color.copy(alpha = 0.2f),
                        shape = CircleShape,
                    )
            )
        }
    }
}

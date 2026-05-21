package com.caiana.talks.ui.conversation

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

object SpectrumWaveform {
    private const val FLOOR = 0.05f
    private val periods = FloatArray(64) { i -> 0.5f + (i % 7) * 0.15f }
    private val phases = FloatArray(64) { i -> (i * 137.5f % 360f) * (PI / 180f).toFloat() }

    fun barHeights(timeMs: Long, barCount: Int, speaking: Boolean): List<Float> {
        if (!speaking) return List(barCount) { FLOOR }
        val t = timeMs / 1000f
        return List(barCount) { i ->
            val idx = i % periods.size
            val raw = sin(2f * PI.toFloat() * periods[idx] * t + phases[idx])
            val normalized = (raw + 1f) / 2f
            FLOOR + normalized * (1f - FLOOR)
        }
    }
}

@Composable
fun AudioSpectrum(
    speaking: Boolean,
    barCount: Int = 24,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "spectrum")
    val timeMs by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 10_000f,
        animationSpec = infiniteRepeatable(
            animation = tween(10_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "timeMs"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
    ) {
        val barWidth = size.width / barCount
        val heights = SpectrumWaveform.barHeights(timeMs.toLong(), barCount, speaking)
        heights.forEachIndexed { i, h ->
            val barH = size.height * h
            drawRect(
                color = Color(0xFF6200EE),
                topLeft = Offset(i * barWidth + 2f, size.height - barH),
                size = Size(barWidth - 4f, barH)
            )
        }
    }
}

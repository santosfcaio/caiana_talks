package com.caiana.talks.ui.theme

import androidx.compose.foundation.Indication
import androidx.compose.foundation.IndicationInstance
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

object LcarsIndication : Indication {
    @Composable
    override fun rememberUpdatedInstance(interactionSource: InteractionSource): IndicationInstance {
        val scope = rememberCoroutineScope()
        return remember(interactionSource) { LcarsIndicationInstance(interactionSource, scope) }
    }
}

internal class LcarsIndicationInstance(
    interactionSource: InteractionSource,
    scope: CoroutineScope
) : IndicationInstance {
    var pressed by mutableStateOf(false)
        private set

    init {
        scope.launch {
            interactionSource.interactions.collect { interaction ->
                pressed = interaction is PressInteraction.Press
            }
        }
    }

    override fun ContentDrawScope.drawIndication() {
        drawContent()
        if (pressed) drawRect(color = Color.Black.copy(alpha = 0.3f))
    }
}

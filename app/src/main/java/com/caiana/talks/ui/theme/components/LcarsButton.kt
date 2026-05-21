package com.caiana.talks.ui.theme.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.caiana.talks.ui.theme.LcarsColors
import com.caiana.talks.ui.theme.LcarsShapes

@Composable
fun LcarsButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = LcarsColors.Orange,
    textColor: Color = LcarsColors.Black,
    enabled: Boolean = true,
    fullWidth: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val sizeModifier = if (fullWidth) Modifier.fillMaxWidth()
                       else Modifier.wrapContentWidth().padding(horizontal = 24.dp)
    Button(
        onClick = onClick,
        modifier = modifier.then(sizeModifier),
        enabled = enabled,
        shape = LcarsShapes.pill,
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = textColor,
            disabledContainerColor = color.copy(alpha = 0.3f),
            disabledContentColor = textColor.copy(alpha = 0.3f),
        ),
        content = content,
    )
}

package com.caiana.talks.ui.theme.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.caiana.talks.ui.theme.LcarsColors
import com.caiana.talks.ui.theme.LcarsShapes

@Composable
fun LcarsDataPanel(
    accentColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(LcarsColors.Surface, LcarsShapes.dataPanel)
            .border(3.dp, accentColor, LcarsShapes.dataPanel)
            .padding(12.dp),
        content = content,
    )
}

package com.caiana.talks.ui.theme.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.caiana.talks.ui.theme.LcarsColors

@Composable
fun LcarsTopBar(
    title: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth().height(56.dp).background(accentColor),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        navigationIcon?.invoke()
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.displayMedium,
            color = LcarsColors.Black,
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
        )
        actions?.invoke(this)
    }
}

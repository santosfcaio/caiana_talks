package com.caiana.talks.ui.theme.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.caiana.talks.ui.theme.LcarsColors

@Composable
fun LcarsTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = if (placeholder.isNotEmpty()) {
            { Text(placeholder) }
        } else null,
        singleLine = true,
        modifier = modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = LcarsColors.Black,
            unfocusedContainerColor = LcarsColors.Black,
            focusedBorderColor = LcarsColors.Orange,
            unfocusedBorderColor = LcarsColors.Orange.copy(alpha = 0.5f),
            focusedLabelColor = LcarsColors.Orange,
            unfocusedLabelColor = LcarsColors.Orange.copy(alpha = 0.7f),
            focusedTextColor = LcarsColors.Text,
            unfocusedTextColor = LcarsColors.Text,
            focusedPlaceholderColor = LcarsColors.TextDim,
            unfocusedPlaceholderColor = LcarsColors.TextDim,
            cursorColor = LcarsColors.Orange,
        ),
    )
}

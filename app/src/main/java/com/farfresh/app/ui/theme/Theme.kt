package com.farfresh.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val FarFreshColorScheme = lightColorScheme(
    primary = FarFreshBlue,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = FarFreshBlueDark,
    onPrimaryContainer = androidx.compose.ui.graphics.Color.White
)

@Composable
fun FarFreshTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FarFreshColorScheme,
        content = content
    )
}

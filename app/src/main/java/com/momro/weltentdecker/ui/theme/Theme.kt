package com.momro.weltentdecker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val KidsScheme = lightColorScheme(
    primary = Color(0xFF43A047),
    onPrimary = Color.White,
    secondary = Color(0xFF0288D1),
    onSecondary = Color.White,
    background = Color.White,
    surface = Color.White,
)

@Composable
fun WeltEntdeckerTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = KidsScheme, content = content)
}

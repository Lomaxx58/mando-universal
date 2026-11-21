package com.marcos.mandouniversal.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Background = Color(0xFF121418)
val Surface1 = Color(0xFF1C1F25)
val Surface2 = Color(0xFF2A2F38)
val Accent = Color(0xFFFF8A3D)
val PowerRed = Color(0xFFE5484D)
val TextMain = Color(0xFFE8EAED)
val TextDim = Color(0xFF8A93A3)

@Composable
fun MandoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Accent,
            onPrimary = Color.Black,
            background = Background,
            onBackground = TextMain,
            surface = Surface1,
            onSurface = TextMain,
            surfaceVariant = Surface2,
            onSurfaceVariant = TextDim,
            error = PowerRed
        ),
        content = content
    )
}

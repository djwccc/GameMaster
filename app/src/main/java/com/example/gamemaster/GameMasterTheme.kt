package com.example.gamemaster

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val LightColorScheme = lightColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFFBB86FC),
    onPrimary = androidx.compose.ui.graphics.Color(0xFF003566),
    secondary = androidx.compose.ui.graphics.Color(0xFF03DAC5),
    onSecondary = androidx.compose.ui.graphics.Color(0xFF003566)
)

private val DarkColorScheme = darkColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF6200EE),
    onPrimary = androidx.compose.ui.graphics.Color(0xFFE0E0E0),
    secondary = androidx.compose.ui.graphics.Color(0xFF03DAC5),
    onSecondary = androidx.compose.ui.graphics.Color(0xFFE0E0E0)
)

@Composable
fun GameMasterTheme(content: @Composable () -> Unit) {
    val typography = Typography(
        bodyLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.5.sp
        )
        // 你可以继续定义其他文本样式
    )
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColorScheme else LightColorScheme,
        typography = typography,
        content = content
    )
}
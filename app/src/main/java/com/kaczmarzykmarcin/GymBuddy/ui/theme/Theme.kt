package com.kaczmarzykmarcin.GymBuddy.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Kolory aplikacji
val Black = Color(0xFF000000)
val White = Color(0xFFFFFFFF)
val Orange = Color(0xFFF5931E) // Kolor pomarańczowy widoczny w logo na makiecie
val Gray = Color(0xFFF2F2F2) // Kolor tła niektórych elementów
val DarkGray = Color(0xFF757575) // Dla mniej istotnych tekstów
val LightGray = Color(0xFFE0E0E0) // Dla dividerów i obramowań
val LightGrayBackground = Color(0xFFEFF1F5) // Dla tła przycisków filtrowania

val AppBackgroundLight = Color(0xFFFAFAFA)

// Schemat kolorów dla trybu jasnego
private val LightColors = lightColorScheme(
    primary = Orange,
    onPrimary = White,
    secondary = Black,
    onSecondary = White,
    background = AppBackgroundLight,
    onBackground = Black,
    surface = White,
    onSurface = Black
)

// Schemat kolorów dla trybu ciemnego
private val DarkColors = darkColorScheme(
    primary = Orange,
    onPrimary = Black,
    secondary = Gray,
    onSecondary = Black,
    background = Color(0xFF121212),
    onBackground = White,
    surface = Color(0xFF252525),
    onSurface = White
)

// Typografia aplikacji
val AppTypography = Typography(
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp
    )
)

@Composable
fun GymBuddyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
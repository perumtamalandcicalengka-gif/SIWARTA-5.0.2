package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val SiwartaDarkColorScheme =
  darkColorScheme(
    primary = BrandBlue400,
    onPrimary = Navy950,
    primaryContainer = BrandBlue600,
    onPrimaryContainer = Color.White,
    secondary = Emerald500,
    onSecondary = Navy950,
    background = Slate900,
    onBackground = Color(0xFFF1F5F9),
    surface = Slate800,
    onSurface = Color(0xFFF8FAFC),
    error = Rose500,
    onError = Color.White,
  )

private val SiwartaLightColorScheme =
  lightColorScheme(
    primary = BrandBlue600,
    onPrimary = Color.White,
    primaryContainer = BrandBlue100,
    onPrimaryContainer = Navy900,
    secondary = Emerald500,
    onSecondary = Color.White,
    background = Slate50,
    onBackground = Slate900,
    surface = Color.White,
    onSurface = Slate900,
    error = Rose500,
    onError = Color.White,
  )

@Composable
fun SiwartaTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      darkTheme -> SiwartaDarkColorScheme
      else -> SiwartaLightColorScheme
    }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content,
  )
}

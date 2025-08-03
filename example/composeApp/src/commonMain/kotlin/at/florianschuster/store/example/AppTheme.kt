package at.florianschuster.store.example

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun AppTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = when {
            isSystemInDarkTheme() -> DarkAppColorScheme
            else -> LightAppColorScheme
        }
    ) {
        Surface(color = MaterialTheme.colorScheme.background) {
            content()
        }
    }
}

object AppTheme {
    val colorScheme: ColorScheme
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme

    val dimensions: Dimensions = Dimensions()
}

data class Dimensions(
    val small: Dp = 6.dp,
    val medium: Dp = 12.dp,
    val large: Dp = 24.dp,
)

private val DarkAppColorScheme = darkColorScheme()
private val LightAppColorScheme = lightColorScheme()

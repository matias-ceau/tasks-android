package fr.matiasceau.tasks.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val OverdueColor = Color(0xFFEF5350)
val OverdueContainer = Color(0xFF3A1D1D)
val TodayColor = Color(0xFFFFA726)
val TodayContainer = Color(0xFF3A2C17)
val DoneColor = Color(0xFF2E7D32)
val DeleteColor = Color(0xFFC62828)

private val colors = darkColorScheme(
    primary = Color(0xFF90CAF9),
    secondary = Color(0xFF80CBC4),
    background = Color(0xFF121212),
    surface = Color(0xFF121212),
)

@Composable
fun TasksTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, content = content)
}

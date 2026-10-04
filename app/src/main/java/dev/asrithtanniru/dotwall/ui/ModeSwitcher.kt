package dev.asrithtanniru.dotwall.ui

import androidx.compose.runtime.Composable
import dev.asrithtanniru.dotwall.model.Mode

@Composable
fun ModeSwitcher(mode: Mode, onChange: (Mode) -> Unit) {
    Segmented(
        options = listOf("Year" to Mode.Year, "Life" to Mode.Life, "Goal" to Mode.Goal),
        selected = mode,
        onSelect = onChange,
    )
}

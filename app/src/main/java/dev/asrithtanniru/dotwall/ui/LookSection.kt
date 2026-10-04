package dev.asrithtanniru.dotwall.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.asrithtanniru.dotwall.model.AccentPresets
import dev.asrithtanniru.dotwall.model.BackgroundStyle
import dev.asrithtanniru.dotwall.model.Density
import dev.asrithtanniru.dotwall.model.DotShape
import dev.asrithtanniru.dotwall.model.Look
import dev.asrithtanniru.dotwall.model.Mode
import dev.asrithtanniru.dotwall.model.RenderSpec

private fun hex(argb: Int) = "%06X".format(argb and 0xFFFFFF)

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun LookSection(spec: RenderSpec, onUpdate: ((RenderSpec) -> RenderSpec) -> Unit) {
    val look = spec.look
    fun setLook(f: (Look) -> Look) = onUpdate { it.copy(look = f(it.look)) }

    Section("Look") {
        Label("Accent")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AccentPresets.all.forEach { (name, argb) ->
                Swatch(Color(argb), selected = look.accent == argb, description = name) { setLook { it.copy(accent = argb) } }
            }
        }
        var text by remember(look.accent) { mutableStateOf(hex(look.accent)) }
        OutlinedTextField(
            value = text,
            onValueChange = { raw ->
                text = raw.removePrefix("#").uppercase().filter { it in "0123456789ABCDEF" }.take(6)
                if (text.length == 6) setLook { it.copy(accent = (0xFF000000 or text.toLong(16)).toInt()) }
            },
            label = { Text("Custom hex") },
            prefix = { Text("#") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Label("Background")
        Segmented(
            listOf("Charcoal" to BackgroundStyle.Charcoal, "Pure black" to BackgroundStyle.Black),
            look.background,
        ) { b -> setLook { it.copy(background = b) } }

        Label("Dot shape")
        Segmented(
            listOf("Circle" to DotShape.Circle, "Rounded square" to DotShape.RoundedSquare),
            look.shape,
        ) { s -> setLook { it.copy(shape = s) } }

        Label("Density")
        Segmented(
            listOf("Comfortable" to Density.Comfortable, "Compact" to Density.Compact),
            look.density,
        ) { d -> setLook { it.copy(density = d) } }

        ToggleRow("Footer text", look.showFooter) { v -> setLook { it.copy(showFooter = v) } }
        if (spec.mode == Mode.Goal) {
            ToggleRow("Goal title", look.showGoalTitle) { v -> setLook { it.copy(showGoalTitle = v) } }
        }
    }
}

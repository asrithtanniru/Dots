package dev.asrithtanniru.dotwall.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import dev.asrithtanniru.dotwall.model.RenderSpec
import dev.asrithtanniru.dotwall.model.WeekStart
import dev.asrithtanniru.dotwall.model.YearStyle
import kotlin.math.roundToInt

@Composable
fun YearSettings(spec: RenderSpec, onUpdate: ((RenderSpec) -> RenderSpec) -> Unit) {
    Section("Year") {
        Segmented(
            options = listOf("Grid" to YearStyle.Grid, "Months" to YearStyle.Months),
            selected = spec.yearStyle,
            onSelect = { s -> onUpdate { it.copy(yearStyle = s) } },
        )
        if (spec.yearStyle == YearStyle.Months) {
            Label("Week starts on")
            Segmented(
                options = listOf("Monday" to WeekStart.Monday, "Sunday" to WeekStart.Sunday),
                selected = spec.weekStart,
                onSelect = { w -> onUpdate { it.copy(weekStart = w) } },
            )
        }
    }
}

@Composable
fun LifeSettings(spec: RenderSpec, onUpdate: ((RenderSpec) -> RenderSpec) -> Unit) {
    Section("Life") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            DateField("Birthdate", spec.birthDate) { d -> onUpdate { it.copy(birthDate = d) } }
            LabeledSlider(
                label = "Expected lifespan",
                value = spec.lifespanYears.toFloat(),
                range = 50f..100f,
                steps = 49,
                valueText = "${spec.lifespanYears} years",
                onChange = { v -> onUpdate { it.copy(lifespanYears = v.roundToInt().coerceIn(50, 100)) } },
            )
        }
    }
}

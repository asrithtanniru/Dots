package dev.asrithtanniru.dotwall.ui

import androidx.compose.runtime.Composable
import dev.asrithtanniru.dotwall.model.ApplyTarget
import dev.asrithtanniru.dotwall.model.Placement
import dev.asrithtanniru.dotwall.model.RenderSpec
import kotlin.math.roundToInt

private const val MIN_BAND = 0.1f

private fun pct(v: Float) = "${(v * 100).roundToInt()}%"

@Composable
fun PlacementSection(spec: RenderSpec, onUpdate: ((RenderSpec) -> RenderSpec) -> Unit) {
    val p = spec.placement
    fun setPlacement(f: (Placement) -> Placement) = onUpdate { it.copy(placement = f(it.placement)) }

    Section("Placement") {
        LabeledSlider("Top", p.top, 0f..0.8f, pct(p.top), onChange = { v ->
            setPlacement { it.copy(top = v, bottom = maxOf(it.bottom, v + MIN_BAND)) }
        })
        LabeledSlider("Bottom", p.bottom, 0.2f..1f, pct(p.bottom), onChange = { v ->
            setPlacement { it.copy(bottom = v, top = minOf(it.top, v - MIN_BAND)) }
        })
        LabeledSlider("Side margin", p.side, 0f..0.25f, pct(p.side), onChange = { v ->
            setPlacement { it.copy(side = v) }
        })
        Label("Apply to")
        Segmented(
            listOf("Lock screen" to ApplyTarget.Lock, "Lock + Home" to ApplyTarget.LockAndHome),
            spec.target,
        ) { t -> onUpdate { it.copy(target = t) } }
    }
}

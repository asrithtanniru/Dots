package dev.asrithtanniru.dotwall.model

import java.time.LocalDate

enum class Mode { Year, Life, Goal }
enum class YearStyle { Grid, Months }
enum class WeekStart { Monday, Sunday }
enum class DotShape { Circle, RoundedSquare }
enum class Density(val radiusFactor: Float) { Comfortable(0.36f), Compact(0.30f) }
enum class BackgroundStyle(val argb: Int) { Charcoal(0xFF0D0D0C.toInt()), Black(0xFF000000.toInt()) }
enum class ApplyTarget { Lock, LockAndHome }

data class Goal(
    val id: String,
    val title: String,
    val start: LocalDate,
    val end: LocalDate,
    val missed: Boolean = false,
)

data class Look(
    val accent: Int = AccentPresets.Clay,
    val background: BackgroundStyle = BackgroundStyle.Charcoal,
    val shape: DotShape = DotShape.Circle,
    val density: Density = Density.Comfortable,
    val showFooter: Boolean = true,
    val showGoalTitle: Boolean = true,
)

data class Placement(
    val top: Float = 0.28f,
    val bottom: Float = 0.875f,
    val side: Float = 0.085f,
)

object AccentPresets {
    const val Clay = 0xFFD97757.toInt()
    const val Ember = 0xFFE5532D.toInt()
    const val Mint = 0xFF5BC8A8.toInt()
    const val Sky = 0xFF5B9BD5.toInt()
    const val Lilac = 0xFFA58BDB.toInt()
    const val Mono = 0xFFFFFFFF.toInt()

    val all: List<Pair<String, Int>> = listOf(
        "Clay" to Clay, "Ember" to Ember, "Mint" to Mint,
        "Sky" to Sky, "Lilac" to Lilac, "Mono" to Mono,
    )
}

data class RenderSpec(
    val mode: Mode = Mode.Year,
    val yearStyle: YearStyle = YearStyle.Grid,
    val weekStart: WeekStart = WeekStart.Monday,
    val birthDate: LocalDate = LocalDate.of(2000, 1, 1),
    val lifespanYears: Int = 80,
    val goals: List<Goal> = emptyList(),
    val activeGoalId: String? = null,
    val look: Look = Look(),
    val placement: Placement = Placement(),
    val target: ApplyTarget = ApplyTarget.Lock,
) {
    val activeGoal: Goal? get() = goals.firstOrNull { it.id == activeGoalId }
}

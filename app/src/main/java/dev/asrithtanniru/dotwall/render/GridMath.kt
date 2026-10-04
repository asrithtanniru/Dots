package dev.asrithtanniru.dotwall.render

import dev.asrithtanniru.dotwall.model.Goal
import dev.asrithtanniru.dotwall.model.Mode
import dev.asrithtanniru.dotwall.model.RenderSpec
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/** Dot counts and progress for one grid. [filled] dots are past, [today] is the accent index or -1. */
data class DotCounts(val total: Int, val filled: Int, val today: Int)

/** Footer text: [left] is drawn in the accent color, [right] in muted. */
data class ProgressText(val left: String, val right: String)

data class Fit(val pitch: Float, val cols: Int, val rows: Int)

/** What the wallpaper shows after goal-expiry rules are applied. */
sealed interface Resolved {
    data object Year : Resolved
    data object Life : Resolved
    data class ActiveGoal(val goal: Goal) : Resolved
    /** Deadline passed less than [GridMath.DONE_DAYS] days ago. */
    data class FinishedGoal(val goal: Goal) : Resolved
}

object GridMath {
    const val DONE_DAYS = 3L
    const val LIFE_COLS = 52
    const val GOAL_COLS = 14

    // ---- Year ----
    fun yearDays(year: Int): Int = if (java.time.Year.isLeap(year.toLong())) 366 else 365

    fun yearCounts(today: LocalDate): DotCounts {
        val idx = today.dayOfYear - 1
        return DotCounts(yearDays(today.year), idx, idx)
    }

    fun yearProgress(today: LocalDate): ProgressText {
        val n = yearDays(today.year)
        val idx = today.dayOfYear - 1
        return ProgressText("${n - idx - 1}d left", "${percent(idx + 1, n)}%")
    }

    // ---- Life ----
    fun weeksLived(birth: LocalDate, today: LocalDate, lifespanYears: Int): Int {
        val days = ChronoUnit.DAYS.between(birth, today)
        return (days / 7).coerceIn(0, (lifespanYears * LIFE_COLS).toLong()).toInt()
    }

    fun lifeCounts(birth: LocalDate, today: LocalDate, lifespanYears: Int): DotCounts {
        val n = lifespanYears * LIFE_COLS
        val lived = weeksLived(birth, today, lifespanYears)
        // Mark the current week as "today" unless life is over.
        return DotCounts(n, lived, if (lived < n) lived else -1)
    }

    fun lifeProgress(birth: LocalDate, today: LocalDate, lifespanYears: Int): ProgressText {
        val n = lifespanYears * LIFE_COLS
        val lived = weeksLived(birth, today, lifespanYears)
        val leftYears = max(0.0, lifespanYears - ChronoUnit.DAYS.between(birth, today) / 365.25)
        return ProgressText("${Math.round(leftYears)}y left", "${percent(lived, n)}% lived")
    }

    // ---- Goal ----
    fun goalDays(goal: Goal): Int = (ChronoUnit.DAYS.between(goal.start, goal.end) + 1).toInt()

    fun goalCounts(goal: Goal, today: LocalDate): DotCounts {
        val n = goalDays(goal)
        val idx = ChronoUnit.DAYS.between(goal.start, today).toInt()
        return when {
            idx < 0 -> DotCounts(n, 0, -1)
            idx >= n -> DotCounts(n, n, -1)
            else -> DotCounts(n, idx, idx)
        }
    }

    fun goalProgress(goal: Goal, today: LocalDate): ProgressText {
        val n = goalDays(goal)
        val idx = ChronoUnit.DAYS.between(goal.start, today).toInt()
        val left = max(0L, ChronoUnit.DAYS.between(today, goal.end))
        val done = (idx + 1).coerceIn(0, n)
        return ProgressText("${left}d left", "${percent(done, n)}%")
    }

    fun finishedProgress(goal: Goal): ProgressText =
        ProgressText(if (goal.missed) "Missed" else "Done", "100%")

    /** Applies the goal-expiry rule: Done/Missed for [DONE_DAYS] days, then Year. */
    fun resolve(spec: RenderSpec, today: LocalDate): Resolved = when (spec.mode) {
        Mode.Year -> Resolved.Year
        Mode.Life -> Resolved.Life
        Mode.Goal -> {
            val goal = spec.activeGoal
            when {
                goal == null -> Resolved.Year
                !today.isAfter(goal.end) -> Resolved.ActiveGoal(goal)
                ChronoUnit.DAYS.between(goal.end, today) <= DONE_DAYS -> Resolved.FinishedGoal(goal)
                else -> Resolved.Year
            }
        }
    }

    // ---- Layout ----
    fun percent(part: Int, total: Int): Int =
        if (total <= 0) 0 else Math.round(100.0 * part / total).toInt()

    /** Column count with the largest dot pitch; first column count wins ties. */
    fun bestCols(n: Int, w: Float, h: Float, lo: Int = 7, hi: Int = 60): Fit {
        var best: Fit? = null
        for (c in lo..hi) {
            val r = ceil(n / c.toDouble()).toInt()
            val p = min(w / c, h / r)
            if (best == null || p > best.pitch + 1e-6f) best = Fit(p, c, r)
        }
        return best!!
    }

    fun fixedCols(n: Int, cols: Int, w: Float, h: Float): Fit {
        val rows = ceil(n / cols.toDouble()).toInt()
        return Fit(min(w / cols, h / rows), cols, rows)
    }
}

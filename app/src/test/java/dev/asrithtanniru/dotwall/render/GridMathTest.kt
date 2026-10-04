package dev.asrithtanniru.dotwall.render

import dev.asrithtanniru.dotwall.model.Goal
import dev.asrithtanniru.dotwall.model.Mode
import dev.asrithtanniru.dotwall.model.RenderSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class GridMathTest {
    private fun d(y: Int, m: Int, day: Int) = LocalDate.of(y, m, day)

    @Test fun leapYearHas366Dots() {
        assertEquals(366, GridMath.yearCounts(d(2028, 6, 1)).total)
        assertEquals(365, GridMath.yearCounts(d(2026, 6, 1)).total)
    }

    @Test fun jan1AndDec31() {
        val jan1 = GridMath.yearCounts(d(2026, 1, 1))
        assertEquals(0, jan1.filled); assertEquals(0, jan1.today)
        assertEquals("364d left", GridMath.yearProgress(d(2026, 1, 1)).left)
        assertEquals("0%", GridMath.yearProgress(d(2026, 1, 1)).right)
        val dec31 = GridMath.yearCounts(d(2026, 12, 31))
        assertEquals(364, dec31.filled); assertEquals(364, dec31.today)
        assertEquals(ProgressText("0d left", "100%"), GridMath.yearProgress(d(2026, 12, 31)))
    }

    @Test fun yearReferenceText() {
        assertEquals(ProgressText("88d left", "76%"), GridMath.yearProgress(d(2026, 10, 4)))
    }

    @Test fun lifeWeeksAndText() {
        val c = GridMath.lifeCounts(d(2004, 3, 15), d(2026, 10, 4), 80)
        assertEquals(4160, c.total)
        assertEquals((d(2004, 3, 15).until(d(2026, 10, 4), java.time.temporal.ChronoUnit.DAYS) / 7).toInt(), c.filled)
        assertEquals(c.filled, c.today)
        assertEquals(ProgressText("57y left", "28% lived"), GridMath.lifeProgress(d(2004, 3, 15), d(2026, 10, 4), 80))
    }

    @Test fun lifeLifespanReached() {
        val c = GridMath.lifeCounts(d(1900, 1, 1), d(2026, 1, 1), 80)
        assertEquals(4160, c.filled); assertEquals(-1, c.today)
        assertEquals(ProgressText("0y left", "100% lived"), GridMath.lifeProgress(d(1900, 1, 1), d(2026, 1, 1), 80))
    }

    @Test fun lifeBeforeBirthClamps() {
        val c = GridMath.lifeCounts(d(2030, 1, 1), d(2026, 1, 1), 80)
        assertEquals(0, c.filled)
    }

    private val goal = Goal("g", "Half marathon", d(2026, 9, 1), d(2026, 12, 31))

    @Test fun goalDuringRange() {
        val c = GridMath.goalCounts(goal, d(2026, 10, 4))
        assertEquals(122, c.total); assertEquals(33, c.filled); assertEquals(33, c.today)
        assertEquals(ProgressText("88d left", "28%"), GridMath.goalProgress(goal, d(2026, 10, 4)))
    }

    @Test fun goalTodayBeforeStart() {
        val c = GridMath.goalCounts(goal, d(2026, 8, 1))
        assertEquals(0, c.filled); assertEquals(-1, c.today)
        assertEquals("0%", GridMath.goalProgress(goal, d(2026, 8, 1)).right)
    }

    @Test fun goalTodayAfterEnd() {
        val c = GridMath.goalCounts(goal, d(2027, 1, 5))
        assertEquals(c.total, c.filled); assertEquals(-1, c.today)
        assertEquals(ProgressText("0d left", "100%"), GridMath.goalProgress(goal, d(2027, 1, 5)))
    }

    @Test fun goalStartEqualsEnd() {
        val one = Goal("o", "x", d(2026, 5, 5), d(2026, 5, 5))
        assertEquals(1, GridMath.goalDays(one))
        assertEquals(DotCounts(1, 0, 0), GridMath.goalCounts(one, d(2026, 5, 5)))
        assertEquals(ProgressText("0d left", "100%"), GridMath.goalProgress(one, d(2026, 5, 5)))
    }

    @Test fun percentRoundsHalfUp() {
        assertEquals(13, GridMath.percent(1, 8)) // 12.5
        assertEquals(0, GridMath.percent(0, 0))
        assertEquals(28, GridMath.percent(34, 122))
    }

    @Test fun bestColsMatchesReference() {
        val w = (1 - 2 * 0.085f) * 1080f
        val h = (0.905f - 0.375f) * 2400f - 56f
        for (n in listOf(365, 366)) {
            val fit = GridMath.bestCols(n, w, h)
            assertEquals(16, fit.cols); assertEquals(23, fit.rows)
            assertEquals(52.8696, fit.pitch.toDouble(), 0.01)
        }
    }

    @Test fun goalExpiryFallsBackToYear() {
        val spec = RenderSpec(mode = Mode.Goal, goals = listOf(goal), activeGoalId = "g")
        assertTrue(GridMath.resolve(spec, d(2026, 12, 31)) is Resolved.ActiveGoal)
        assertTrue(GridMath.resolve(spec, d(2027, 1, 1)) is Resolved.FinishedGoal)
        assertTrue(GridMath.resolve(spec, d(2027, 1, 3)) is Resolved.FinishedGoal)
        assertEquals(Resolved.Year, GridMath.resolve(spec, d(2027, 1, 4)))
    }

    @Test fun goalModeWithoutActiveGoalIsYear() {
        assertEquals(Resolved.Year, GridMath.resolve(RenderSpec(mode = Mode.Goal), d(2026, 1, 1)))
    }

    @Test fun finishedText() {
        assertEquals("Done", GridMath.finishedProgress(goal).left)
        assertEquals("Missed", GridMath.finishedProgress(goal.copy(missed = true)).left)
    }
}

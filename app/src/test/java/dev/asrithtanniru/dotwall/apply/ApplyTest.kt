package dev.asrithtanniru.dotwall.apply

import dev.asrithtanniru.dotwall.data.AppliedState
import dev.asrithtanniru.dotwall.model.ApplyTarget
import dev.asrithtanniru.dotwall.model.Look
import dev.asrithtanniru.dotwall.model.RenderSpec
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class ApplyTest {
    private val ist = ZoneId.of("Asia/Kolkata")
    private fun at(zone: ZoneId, y: Int, m: Int, d: Int, h: Int, min: Int) = ZonedDateTime.of(y, m, d, h, min, 0, 0, zone)

    // ---- next 00:05 ----
    @Test fun nextRunLaterTonight() {
        assertEquals(Duration.ofHours(1).plusMinutes(5), Scheduler.delayUntilNextRun(at(ist, 2026, 10, 4, 23, 0)))
    }

    @Test fun nextRunAfterMidnightIsTomorrow() {
        assertEquals(Duration.ofHours(23).plusMinutes(55), Scheduler.delayUntilNextRun(at(ist, 2026, 10, 4, 0, 10)))
    }

    @Test fun nextRunExactlyAtTargetMovesToNextDay() {
        assertEquals(Duration.ofHours(24), Scheduler.delayUntilNextRun(at(ist, 2026, 10, 4, 0, 5)))
    }

    @Test fun nextRunBeforeTargetSameDay() {
        assertEquals(Duration.ofMinutes(3), Scheduler.delayUntilNextRun(at(ist, 2026, 10, 4, 0, 2)))
    }

    @Test fun nextRunAcrossSpringForward() {
        val ny = ZoneId.of("America/New_York") // 2026-03-08 has 23 hours
        assertEquals(Duration.ofHours(22).plusMinutes(55), Scheduler.delayUntilNextRun(at(ny, 2026, 3, 8, 0, 10)))
        assertEquals(Duration.ofHours(1).plusMinutes(5), Scheduler.delayUntilNextRun(at(ny, 2026, 3, 7, 23, 0)))
    }

    @Test fun nextRunAcrossFallBack() {
        val ny = ZoneId.of("America/New_York") // 2026-11-01 has 25 hours
        assertEquals(Duration.ofHours(24).plusMinutes(55), Scheduler.delayUntilNextRun(at(ny, 2026, 11, 1, 0, 10)))
    }

    // ---- skip logic ----
    private val today = LocalDate.of(2026, 10, 4)
    private val spec = RenderSpec()
    private val hash = ApplyPolicy.hash(spec, "1080x2400")

    @Test fun hashIsStableAndSensitive() {
        assertEquals(hash, ApplyPolicy.hash(RenderSpec(), "1080x2400"))
        assert(hash != ApplyPolicy.hash(spec.copy(look = Look(showFooter = false)), "1080x2400"))
        assert(hash != ApplyPolicy.hash(spec, "1080x2340"))
    }

    @Test fun policyTable() {
        val same = AppliedState(today, hash, 1L)
        val oldDay = AppliedState(today.minusDays(1), hash, 1L)
        val otherSpec = AppliedState(today, hash + 1, 1L)
        val never = AppliedState(null, null, null)
        assertEquals(true, ApplyPolicy.shouldApply(Trigger.Manual, same, today, hash))
        assertEquals(false, ApplyPolicy.shouldApply(Trigger.Background, same, today, hash))
        assertEquals(true, ApplyPolicy.shouldApply(Trigger.Background, oldDay, today, hash))
        assertEquals(true, ApplyPolicy.shouldApply(Trigger.Background, otherSpec, today, hash))
        assertEquals(false, ApplyPolicy.shouldApply(Trigger.Background, never, today, hash))
        assertEquals(false, ApplyPolicy.shouldApply(Trigger.AppOpen, otherSpec, today, hash))
        assertEquals(true, ApplyPolicy.shouldApply(Trigger.AppOpen, oldDay, today, hash))
        assertEquals(false, ApplyPolicy.shouldApply(Trigger.AppOpen, never, today, hash))
    }

    private class Fakes(var spec: RenderSpec, var applied: AppliedState = AppliedState(null, null, null)) {
        val sets = mutableListOf<Pair<String, ApplyTarget>>()
        val core = ApplyCore<String>(
            loadSpec = { spec },
            loadApplied = { applied },
            markApplied = { d, h, t -> applied = AppliedState(d, h, t) },
            render = { _, d -> "img-$d" },
            set = { img, target -> sets += img to target },
            sizeKey = "1080x2400",
        )
    }

    @Test fun applyIsIdempotentWithinADay() = runTest {
        val f = Fakes(RenderSpec(target = ApplyTarget.LockAndHome))
        val now = at(ist, 2026, 10, 4, 9, 0)
        assertEquals(Outcome.Applied, f.core.apply(Trigger.Manual, now))
        assertEquals(Outcome.Skipped, f.core.apply(Trigger.Background, now.plusHours(3)))
        assertEquals(listOf("img-2026-10-04" to ApplyTarget.LockAndHome), f.sets)
        assertEquals(Outcome.Applied, f.core.apply(Trigger.Background, now.plusDays(1).withHour(0).withMinute(5)))
        assertEquals(2, f.sets.size)
        assertEquals("img-2026-10-05", f.sets.last().first)
    }

    @Test fun backgroundNeverAppliesBeforeFirstManualApply() = runTest {
        val f = Fakes(RenderSpec())
        assertEquals(Outcome.Skipped, f.core.apply(Trigger.Background, at(ist, 2026, 10, 4, 9, 0)))
        assertEquals(0, f.sets.size)
    }
}

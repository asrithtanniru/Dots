package dev.asrithtanniru.dotwall.apply

import dev.asrithtanniru.dotwall.data.AppliedState
import dev.asrithtanniru.dotwall.model.ApplyTarget
import dev.asrithtanniru.dotwall.model.RenderSpec
import java.time.LocalDate
import java.time.ZonedDateTime

/** Why an apply was requested. */
enum class Trigger {
    /** Apply button: always applies. */
    Manual,
    /** Worker or system broadcast: skips when today's image for this spec is already set. */
    Background,
    /** App opened: only catches up when the day changed, so unsaved tweaks are not applied silently. */
    AppOpen,
}

enum class Outcome { Applied, Skipped }

object ApplyPolicy {
    /** Stable across processes: enum hashCode is identity-based, so hash the text form. */
    fun hash(spec: RenderSpec, sizeKey: String): Int = "$spec|$sizeKey".hashCode()

    fun shouldApply(trigger: Trigger, applied: AppliedState, today: LocalDate, hash: Int): Boolean = when {
        trigger == Trigger.Manual -> true
        applied.date == null -> false // never applied: user has not opted in
        trigger == Trigger.AppOpen -> applied.date != today
        else -> !(applied.date == today && applied.hash == hash)
    }
}

/** Render-and-set flow with all side effects injected, so the skip logic can be tested with fakes. */
class ApplyCore<B>(
    private val loadSpec: suspend () -> RenderSpec,
    private val loadApplied: suspend () -> AppliedState,
    private val markApplied: suspend (LocalDate, Int, Long) -> Unit,
    private val render: suspend (RenderSpec, LocalDate) -> B,
    private val set: suspend (B, ApplyTarget) -> Unit,
    private val sizeKey: String,
) {
    suspend fun apply(trigger: Trigger, now: ZonedDateTime): Outcome {
        val spec = loadSpec()
        val today = now.toLocalDate()
        val hash = ApplyPolicy.hash(spec, sizeKey)
        if (!ApplyPolicy.shouldApply(trigger, loadApplied(), today, hash)) return Outcome.Skipped
        set(render(spec, today), spec.target)
        markApplied(today, hash, now.toInstant().toEpochMilli())
        return Outcome.Applied
    }
}

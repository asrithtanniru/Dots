package dev.asrithtanniru.dotwall.apply

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

object Scheduler {
    private const val WORK_NAME = "dots-daily"
    private const val HOUR = 0
    private const val MINUTE = 5

    /** Time from [now] until the next 00:05 local time, DST-safe because the target is built from the local date. */
    fun delayUntilNextRun(now: ZonedDateTime): Duration {
        var target = now.toLocalDate().atTime(HOUR, MINUTE).atZone(now.zone)
        if (!target.isAfter(now)) target = now.toLocalDate().plusDays(1).atTime(HOUR, MINUTE).atZone(now.zone)
        return Duration.between(now, target)
    }

    /** Queue the next daily run, replacing any pending one. */
    fun scheduleNext(context: Context, now: ZonedDateTime = ZonedDateTime.now()) =
        enqueue(context, delayUntilNextRun(now))

    /** Run as soon as possible; the worker schedules the following day when done. */
    fun runNow(context: Context) = enqueue(context, Duration.ZERO)

    private fun enqueue(context: Context, delay: Duration) {
        val request = OneTimeWorkRequestBuilder<DailyWorker>()
            .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }
}

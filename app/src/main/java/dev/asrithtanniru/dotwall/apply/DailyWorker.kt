package dev.asrithtanniru.dotwall.apply

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class DailyWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        try {
            WallpaperApplier(applicationContext).apply(Trigger.Background)
        } catch (e: Exception) {
            // Keep the daily chain alive; the next run or an app open retries.
        } finally {
            // Last step: REPLACE cancels this very worker, so nothing may follow.
            Scheduler.scheduleNext(applicationContext)
        }
        return Result.success()
    }
}

package dev.asrithtanniru.dotwall.apply

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Boot, clock, date and time-zone changes: apply now (if stale) and re-arm the daily run. */
class TimeChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Scheduler.runNow(context)
    }
}

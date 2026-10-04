package dev.asrithtanniru.dotwall.render

import android.content.Context
import android.util.Size
import android.view.WindowManager

object ScreenSize {
    /** Full display size in portrait, whatever the current rotation. */
    fun portrait(context: Context): Size {
        val wm = context.getSystemService(WindowManager::class.java)
        val b = wm.maximumWindowMetrics.bounds
        return Size(minOf(b.width(), b.height()), maxOf(b.width(), b.height()))
    }
}

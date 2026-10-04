package dev.asrithtanniru.dotwall.apply

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import dev.asrithtanniru.dotwall.data.SettingsRepo
import dev.asrithtanniru.dotwall.model.ApplyTarget
import dev.asrithtanniru.dotwall.render.ScreenSize
import dev.asrithtanniru.dotwall.render.WallpaperFonts
import dev.asrithtanniru.dotwall.render.WallpaperRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.ZonedDateTime

class WallpaperApplier(context: Context) {
    private val app = context.applicationContext
    private val repo = SettingsRepo(app)
    private val size = ScreenSize.portrait(app)
    private val fonts = WallpaperFonts.load(app)

    private val core = ApplyCore<Bitmap>(
        loadSpec = repo::currentSpec,
        loadApplied = repo::currentApplied,
        markApplied = repo::markApplied,
        render = { spec, today -> withContext(Dispatchers.Default) { WallpaperRenderer.render(spec, size, today, fonts) } },
        set = { bmp, target ->
            withContext(Dispatchers.IO) {
                val flags = when (target) {
                    ApplyTarget.Lock -> WallpaperManager.FLAG_LOCK
                    ApplyTarget.LockAndHome -> WallpaperManager.FLAG_LOCK or WallpaperManager.FLAG_SYSTEM
                }
                WallpaperManager.getInstance(app).setBitmap(bmp, null, true, flags)
                bmp.recycle()
            }
        },
        sizeKey = "${size.width}x${size.height}",
    )

    suspend fun apply(trigger: Trigger, now: ZonedDateTime = ZonedDateTime.now()): Outcome =
        lock.withLock { core.apply(trigger, now) }

    private companion object {
        /** One apply at a time across worker, receiver and UI. */
        val lock = Mutex()
    }
}

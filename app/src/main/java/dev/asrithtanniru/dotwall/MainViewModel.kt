package dev.asrithtanniru.dotwall

import android.app.Application
import android.content.ContentValues
import android.graphics.Bitmap
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.asrithtanniru.dotwall.apply.Outcome
import dev.asrithtanniru.dotwall.apply.Scheduler
import dev.asrithtanniru.dotwall.apply.Trigger
import dev.asrithtanniru.dotwall.apply.WallpaperApplier
import dev.asrithtanniru.dotwall.data.AppliedState
import dev.asrithtanniru.dotwall.data.SettingsRepo
import dev.asrithtanniru.dotwall.model.RenderSpec
import dev.asrithtanniru.dotwall.render.ScreenSize
import dev.asrithtanniru.dotwall.render.WallpaperRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = SettingsRepo(app)
    val screen = ScreenSize.portrait(app)

    /** Null until DataStore has loaded. */
    val spec: StateFlow<RenderSpec?> = repo.spec.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val preview: StateFlow<Bitmap?> = spec.filterNotNull()
        .debounce(100)
        .mapLatest { s -> withContext(Dispatchers.Default) { WallpaperRenderer.render(s, screen, LocalDate.now()) } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val applier = WallpaperApplier(app)

    val applied: StateFlow<AppliedState?> = repo.applied.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Apply now, arm the daily run, report success. */
    fun apply(onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = runCatching {
                applier.apply(Trigger.Manual)
                Scheduler.scheduleNext(getApplication())
            }.isSuccess
            onDone(ok)
        }
    }

    /** Catch up after missed runs (battery saver, no receiver); no-op until the first manual apply. */
    fun catchUp() {
        viewModelScope.launch {
            runCatching {
                if (applier.apply(Trigger.AppOpen) == Outcome.Applied) Scheduler.scheduleNext(getApplication())
            }
        }
    }

    fun update(transform: (RenderSpec) -> RenderSpec) {
        viewModelScope.launch { repo.update(transform) }
    }

    /** Debug aid: full-size render into Pictures/Dots to compare against wall-*.png. */
    fun savePng(onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val s = spec.value ?: return@launch
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    val bmp = WallpaperRenderer.render(s, screen, LocalDate.now())
                    val resolver = getApplication<Application>().contentResolver
                    val values = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, "dots-${s.mode.name.lowercase()}-${System.currentTimeMillis()}.png")
                        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Dots")
                    }
                    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)!!
                    resolver.openOutputStream(uri)!!.use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
                }.isSuccess
            }
            onDone(ok)
        }
    }
}

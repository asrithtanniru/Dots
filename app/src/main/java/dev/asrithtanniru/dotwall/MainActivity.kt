package dev.asrithtanniru.dotwall

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.asrithtanniru.dotwall.model.Mode
import dev.asrithtanniru.dotwall.ui.ApplyBar
import dev.asrithtanniru.dotwall.ui.DotsColors
import dev.asrithtanniru.dotwall.ui.GoalSettings
import dev.asrithtanniru.dotwall.ui.LifeSettings
import dev.asrithtanniru.dotwall.ui.LookSection
import dev.asrithtanniru.dotwall.ui.ModeSwitcher
import dev.asrithtanniru.dotwall.ui.PlacementSection
import dev.asrithtanniru.dotwall.ui.YearSettings
import dev.asrithtanniru.dotwall.ui.DotsTheme
import dev.asrithtanniru.dotwall.ui.Preview

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        vm.catchUp()
        setContent {
            val spec by vm.spec.collectAsState()
            val preview by vm.preview.collectAsState()
            val applied by vm.applied.collectAsState()
            val context = LocalContext.current
            DotsTheme(accent = spec?.look?.accent?.let { Color(it) } ?: Color(0xFFD97757)) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .background(DotsColors.Background)
                        .safeDrawingPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    Text("Dots", style = MaterialTheme.typography.headlineMedium, color = DotsColors.Text)
                    var ghost by remember { mutableStateOf(true) }
                    Preview(
                        bitmap = preview,
                        aspect = vm.screen.width.toFloat() / vm.screen.height,
                        showGhost = ghost,
                        modifier = Modifier.padding(horizontal = 40.dp),
                    )
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Lock-screen ghosts", color = DotsColors.Muted, modifier = Modifier.weight(1f))
                        Switch(checked = ghost, onCheckedChange = { ghost = it })
                    }
                    spec?.let { sp ->
                        ModeSwitcher(sp.mode) { m -> vm.update { it.copy(mode = m) } }
                        when (sp.mode) {
                            Mode.Year -> YearSettings(sp, vm::update)
                            Mode.Life -> LifeSettings(sp, vm::update)
                            Mode.Goal -> GoalSettings(sp, vm::update)
                        }
                        LookSection(sp, vm::update)
                        PlacementSection(sp, vm::update)
                        ApplyBar(applied) {
                            vm.apply { ok ->
                                Toast.makeText(
                                    context,
                                    if (ok) "Wallpaper applied" else "Could not set wallpaper",
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                        }
                    }
                    if (BuildConfig.DEBUG) {
                        TextButton(onClick = {
                            vm.savePng { ok ->
                                Toast.makeText(context, if (ok) "Saved to Pictures/Dots" else "Save failed", Toast.LENGTH_SHORT).show()
                            }
                        }) { Text("Save PNG to Pictures") }
                    }
                }
            }
        }
    }
}

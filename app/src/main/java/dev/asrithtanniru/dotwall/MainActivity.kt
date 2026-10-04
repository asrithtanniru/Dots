package dev.asrithtanniru.dotwall

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.asrithtanniru.dotwall.ui.DotsColors
import dev.asrithtanniru.dotwall.ui.DotsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DotsTheme {
                Box(Modifier.fillMaxSize().background(DotsColors.Background).safeDrawingPadding()) {
                    Text("Dots", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(24.dp))
                }
            }
        }
    }
}

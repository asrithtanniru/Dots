package dev.asrithtanniru.dotwall.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.asrithtanniru.dotwall.data.AppliedState
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val timeFormat = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)
private val dateTimeFormat = DateTimeFormatter.ofPattern("d MMM HH:mm", Locale.ENGLISH)

private fun lastApplied(state: AppliedState?): String {
    val millis = state?.epochMillis ?: return "not applied yet"
    val at = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
    val text = if (at.toLocalDate() == LocalDate.now()) timeFormat.format(at) else dateTimeFormat.format(at)
    return "last applied $text"
}

@Composable
fun ApplyBar(applied: AppliedState?, onApply: () -> Unit) {
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Button(onClick = onApply, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text("Apply", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
        }
        Text(
            "Auto-updates daily · ${lastApplied(applied)}",
            style = MaterialTheme.typography.bodySmall,
            color = DotsColors.Muted,
        )
    }
}

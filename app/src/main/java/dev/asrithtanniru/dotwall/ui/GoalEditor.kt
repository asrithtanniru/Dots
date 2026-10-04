package dev.asrithtanniru.dotwall.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.asrithtanniru.dotwall.model.Goal
import dev.asrithtanniru.dotwall.model.RenderSpec
import java.time.LocalDate
import java.util.UUID

private const val MAX_TITLE = 40

@Composable
fun GoalSettings(spec: RenderSpec, onUpdate: ((RenderSpec) -> RenderSpec) -> Unit) {
    // null = closed, Goal with blank id = new goal
    var editing by remember { mutableStateOf<Goal?>(null) }
    Section("Goals") {
        if (spec.goals.isEmpty()) Label("No goals yet. Add one to see it on the wallpaper.")
        spec.goals.forEach { goal ->
            GoalRow(
                goal = goal,
                active = goal.id == spec.activeGoalId,
                onActivate = { onUpdate { it.copy(activeGoalId = goal.id) } },
                onEdit = { editing = goal },
                onDelete = {
                    onUpdate { s ->
                        val rest = s.goals.filterNot { it.id == goal.id }
                        s.copy(
                            goals = rest,
                            activeGoalId = if (s.activeGoalId == goal.id) rest.firstOrNull()?.id else s.activeGoalId,
                        )
                    }
                },
            )
        }
        TextButton(onClick = {
            val today = LocalDate.now()
            editing = Goal("", "", today, today.plusDays(90))
        }) { Text("Add goal") }
    }
    editing?.let { draft ->
        GoalDialog(
            initial = draft,
            onDismiss = { editing = null },
            onSave = { saved ->
                onUpdate { s ->
                    if (saved.id.isEmpty()) {
                        val g = saved.copy(id = UUID.randomUUID().toString())
                        s.copy(goals = s.goals + g, activeGoalId = s.activeGoalId ?: g.id)
                    } else {
                        s.copy(goals = s.goals.map { if (it.id == saved.id) saved else it })
                    }
                }
                editing = null
            },
        )
    }
}

@Composable
private fun GoalRow(goal: Goal, active: Boolean, onActivate: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DotsColors.Surface)
            .clickable(onClick = onActivate)
            .padding(start = 4.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = active, onClick = onActivate)
        Column(Modifier.weight(1f).padding(vertical = 10.dp)) {
            Text(goal.title, color = DotsColors.Text, fontWeight = FontWeight.Medium, maxLines = 1)
            Text(
                "${goal.start.pretty()} → ${goal.end.pretty()}" + if (goal.missed) " · missed" else "",
                style = MaterialTheme.typography.bodySmall,
                color = DotsColors.Muted,
            )
        }
        TextButton(onClick = onEdit) { Text("Edit") }
        TextButton(onClick = onDelete) { Text("Delete") }
    }
}

@Composable
private fun GoalDialog(initial: Goal, onDismiss: () -> Unit, onSave: (Goal) -> Unit) {
    var title by remember { mutableStateOf(initial.title) }
    var start by remember { mutableStateOf(initial.start) }
    var end by remember { mutableStateOf(initial.end) }
    var missed by remember { mutableStateOf(initial.missed) }
    val validDates = end.isAfter(start)
    val valid = title.isNotBlank() && validDates
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DotsColors.Surface,
        title = { Text(if (initial.id.isEmpty()) "New goal" else "Edit goal") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(MAX_TITLE) },
                    label = { Text("Title") },
                    singleLine = true,
                    supportingText = { Text("${title.length}/$MAX_TITLE") },
                )
                DateField("Start", start) { start = it }
                DateField("Deadline", end) { end = it }
                if (!validDates) Text("Deadline must be after start.", color = MaterialTheme.colorScheme.error)
                ToggleRow("Mark as missed", missed) { missed = it }
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = {
                onSave(initial.copy(title = title.trim(), start = start, end = end, missed = missed))
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

package fr.matiasceau.tasks.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.matiasceau.tasks.data.NewTask
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private const val MAX_DESCRIPTION = 500
private val priorities = listOf(null, "H", "M", "L")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskDialog(onDismiss: () -> Unit, onConfirm: (NewTask) -> Unit) {
    var description by rememberSaveable { mutableStateOf("") }
    var project by rememberSaveable { mutableStateOf("") }
    var tags by rememberSaveable { mutableStateOf("") }
    var priority by rememberSaveable { mutableStateOf<String?>(null) }
    var dueDate by rememberSaveable { mutableStateOf<LocalDate?>(null) }
    var priorityExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nouvelle tâche") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it.take(MAX_DESCRIPTION) },
                    label = { Text("Description *") },
                    supportingText = { Text("${description.length}/$MAX_DESCRIPTION") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = project,
                    onValueChange = { project = it },
                    label = { Text("Projet") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    label = { Text("Tags (séparés par des virgules)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                ExposedDropdownMenuBox(
                    expanded = priorityExpanded,
                    onExpandedChange = { priorityExpanded = it },
                ) {
                    OutlinedTextField(
                        value = priority ?: "Aucune",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Priorité") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(priorityExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = priorityExpanded,
                        onDismissRequest = { priorityExpanded = false },
                    ) {
                        priorities.forEach { p ->
                            DropdownMenuItem(
                                text = { Text(p ?: "Aucune") },
                                onClick = {
                                    priority = p
                                    priorityExpanded = false
                                },
                            )
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { showDatePicker = true }) {
                        Text(
                            dueDate?.let {
                                "Échéance : " + it.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                            } ?: "Échéance (optionnelle)",
                        )
                    }
                    if (dueDate != null) {
                        TextButton(onClick = { dueDate = null }) { Text("Retirer") }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = description.isNotBlank(),
                onClick = {
                    onConfirm(
                        NewTask(
                            description = description.trim(),
                            project = project.trim().ifBlank { null },
                            tags = tags.split(',').map { it.trim() }.filter { it.isNotEmpty() }
                                .ifEmpty { null },
                            priority = priority,
                            due = dueDate?.let { toApiDue(it) },
                        ),
                    )
                },
            ) { Text("Ajouter") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )

    if (showDatePicker) {
        val pickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    // DatePicker returns UTC midnight of the selected day.
                    pickerState.selectedDateMillis?.let {
                        dueDate = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Annuler") }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

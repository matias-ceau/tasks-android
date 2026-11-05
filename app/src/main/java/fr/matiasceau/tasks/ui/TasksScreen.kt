package fr.matiasceau.tasks.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.matiasceau.tasks.TasksUiState
import fr.matiasceau.tasks.data.NewTask
import fr.matiasceau.tasks.data.Task
import fr.matiasceau.tasks.ui.theme.DoneColor
import fr.matiasceau.tasks.ui.theme.OverdueColor
import fr.matiasceau.tasks.ui.theme.OverdueContainer
import fr.matiasceau.tasks.ui.theme.TodayColor
import fr.matiasceau.tasks.ui.theme.TodayContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    state: TasksUiState,
    snackbarHostState: SnackbarHostState,
    onRefresh: () -> Unit,
    onAdd: (NewTask) -> Unit,
    onDone: (Task) -> Unit,
    onDelete: (Task) -> Unit,
    onOpenSettings: () -> Unit,
) {
    var showAdd by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Task?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tasks (${state.tasks.size})") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Paramètres")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) {
                Icon(Icons.Default.Add, contentDescription = "Ajouter")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.refreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (state.tasks.isEmpty() && !state.refreshing) {
                    item {
                        Text(
                            "Aucune tâche. Tirez pour rafraîchir.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
                items(state.tasks, key = { it.id }) { task ->
                    SwipeableTaskRow(
                        task = task,
                        onDone = { onDone(task) },
                        onLongPress = { pendingDelete = task },
                    )
                }
            }
        }
    }

    if (showAdd) {
        AddTaskDialog(
            onDismiss = { showAdd = false },
            onConfirm = {
                showAdd = false
                onAdd(it)
            },
        )
    }

    pendingDelete?.let { task ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Supprimer la tâche ?") },
            text = { Text(task.description) },
            confirmButton = {
                TextButton(onClick = {
                    pendingDelete = null
                    onDelete(task)
                }) { Text("Supprimer", color = OverdueColor) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Annuler") }
            },
        )
    }
}

@Composable
private fun SwipeableTaskRow(task: Task, onDone: () -> Unit, onLongPress: () -> Unit) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.StartToEnd) {
                onDone()
                true
            } else {
                false
            }
        },
    )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromEndToStart = false,
        backgroundContent = {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(DoneColor, CardDefaults.shape)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Icon(Icons.Default.Check, contentDescription = "Terminer", tint = Color.White)
            }
        },
    ) {
        TaskCard(task, onLongPress)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TaskCard(task: Task, onLongPress: () -> Unit) {
    val due = dueState(task.due)
    val (container, accent) = when (due) {
        DueState.OVERDUE -> OverdueContainer to OverdueColor
        DueState.TODAY -> TodayContainer to TodayColor
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = container),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = {}, onLongClick = onLongPress),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                task.priority?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.labelLarge,
                        color = priorityColor(it),
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    task.description,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "%.1f".format(task.urgency),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            val meta = buildList {
                task.project?.let { add(it) }
                if (task.tags.isNotEmpty()) add(task.tags.joinToString(" ") { "+$it" })
            }
            if (meta.isNotEmpty()) {
                Text(
                    meta.joinToString("  ·  "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            parseDue(task.due)?.let {
                val prefix = when (due) {
                    DueState.OVERDUE -> "En retard · "
                    DueState.TODAY -> "Aujourd'hui · "
                    else -> ""
                }
                Text(
                    prefix + formatDue(it),
                    style = MaterialTheme.typography.bodySmall,
                    color = accent,
                )
            }
        }
    }
}

@Composable
private fun priorityColor(priority: String): Color = when (priority) {
    "H" -> OverdueColor
    "M" -> TodayColor
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

package fr.matiasceau.tasks

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.matiasceau.tasks.ui.SettingsScreen
import fr.matiasceau.tasks.ui.TasksScreen
import fr.matiasceau.tasks.ui.theme.TasksTheme

class MainActivity : ComponentActivity() {
    private val vm: TasksViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TasksTheme {
                val settings by vm.settings.collectAsStateWithLifecycle()
                val ui by vm.ui.collectAsStateWithLifecycle()
                val snackbar = remember { SnackbarHostState() }
                var showSettings by rememberSaveable { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    vm.messages.collect { snackbar.showSnackbar(it) }
                }

                // Wait for DataStore to load once to avoid flashing the wrong screen.
                val current = settings ?: return@TasksTheme
                val hasKey = current.apiKey.isNotBlank()

                if (showSettings || !hasKey) {
                    BackHandler(enabled = hasKey) { showSettings = false }
                    SettingsScreen(
                        initial = current,
                        canGoBack = hasKey,
                        onBack = { showSettings = false },
                        onSave = {
                            vm.saveSettings(it)
                            showSettings = false
                        },
                    )
                } else {
                    TasksScreen(
                        state = ui,
                        snackbarHostState = snackbar,
                        onRefresh = vm::refresh,
                        onAdd = vm::add,
                        onDone = vm::complete,
                        onDelete = vm::delete,
                        onOpenSettings = { showSettings = true },
                    )
                }
            }
        }
    }
}

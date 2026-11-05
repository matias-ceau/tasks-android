package fr.matiasceau.tasks

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fr.matiasceau.tasks.data.NewTask
import fr.matiasceau.tasks.data.Settings
import fr.matiasceau.tasks.data.SettingsRepository
import fr.matiasceau.tasks.data.Task
import fr.matiasceau.tasks.data.TasksApi
import fr.matiasceau.tasks.data.toUserMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TasksUiState(
    val tasks: List<Task> = emptyList(),
    val refreshing: Boolean = false,
)

class TasksViewModel(app: Application) : AndroidViewModel(app) {
    private val settingsRepo = SettingsRepository(app)

    /** null until DataStore has been read once (used to decide the start screen). */
    val settings: StateFlow<Settings?> =
        settingsRepo.settings.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _ui = MutableStateFlow(TasksUiState())
    val ui: StateFlow<TasksUiState> = _ui.asStateFlow()

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    private var api: TasksApi? = null

    init {
        viewModelScope.launch {
            settingsRepo.settings.distinctUntilChanged().collect { s ->
                api = null
                _ui.update { it.copy(tasks = emptyList()) }
                if (s.apiKey.isBlank()) return@collect
                try {
                    api = TasksApi.build(s.baseUrl, s.apiKey)
                    refresh()
                } catch (e: IllegalArgumentException) {
                    _messages.send(e.toUserMessage())
                }
            }
        }
    }

    fun refresh() {
        val api = api ?: return
        viewModelScope.launch {
            _ui.update { it.copy(refreshing = true) }
            safeCall {
                val tasks = api.list().data.sortedByDescending { it.urgency }
                _ui.update { it.copy(tasks = tasks) }
            }
            _ui.update { it.copy(refreshing = false) }
        }
    }

    fun add(task: NewTask) = mutate { it.create(task) }

    // Optimistic removal: the row disappears immediately; the refresh restores it on failure.
    fun complete(task: Task) {
        _ui.update { s -> s.copy(tasks = s.tasks.filterNot { it.id == task.id }) }
        mutate { it.done(task.id) }
    }

    fun delete(task: Task) {
        _ui.update { s -> s.copy(tasks = s.tasks.filterNot { it.id == task.id }) }
        mutate { it.delete(task.id) }
    }

    fun saveSettings(settings: Settings) {
        viewModelScope.launch { settingsRepo.save(settings) }
    }

    private fun mutate(block: suspend (TasksApi) -> Unit) {
        val api = api ?: return
        viewModelScope.launch {
            safeCall { block(api) }
            refresh()
        }
    }

    private suspend fun safeCall(block: suspend () -> Unit) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _messages.send(e.toUserMessage())
        }
    }
}

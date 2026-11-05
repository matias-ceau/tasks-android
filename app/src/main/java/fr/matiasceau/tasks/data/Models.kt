package fr.matiasceau.tasks.data

import kotlinx.serialization.Serializable

// `local_id` is intentionally not mapped: it is an Int in GET but a String in POST responses.
@Serializable
data class Task(
    val id: String,
    val description: String,
    val project: String? = null,
    val tags: List<String> = emptyList(),
    val priority: String? = null,
    val due: String? = null,
    val urgency: Double = 0.0,
    val status: String = "pending",
    val entry: String? = null,
)

@Serializable
data class TaskList(val data: List<Task> = emptyList())

@Serializable
data class NewTask(
    val description: String,
    val project: String? = null,
    val tags: List<String>? = null,
    val priority: String? = null,
    val due: String? = null,
)

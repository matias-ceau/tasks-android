package fr.matiasceau.tasks.ui

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

enum class DueState { NONE, LATER, TODAY, OVERDUE }

fun parseDue(due: String?): Instant? = due?.let { runCatching { Instant.parse(it) }.getOrNull() }

fun dueState(
    due: String?,
    now: Instant = Instant.now(),
    zone: ZoneId = ZoneId.systemDefault(),
): DueState {
    val instant = parseDue(due) ?: return DueState.NONE
    return when {
        instant.isBefore(now) -> DueState.OVERDUE
        instant.atZone(zone).toLocalDate() == now.atZone(zone).toLocalDate() -> DueState.TODAY
        else -> DueState.LATER
    }
}

private val displayFormat = DateTimeFormatter.ofPattern("EEE dd/MM HH:mm")

fun formatDue(instant: Instant, zone: ZoneId = ZoneId.systemDefault()): String =
    instant.atZone(zone).format(displayFormat)

/** Time of day used when the user only picks a date in the add dialog. */
val DEFAULT_DUE_TIME: LocalTime = LocalTime.of(18, 0)

/** API format: UTC, second precision, e.g. 2026-11-05T18:00:00Z. */
fun toApiDue(date: LocalDate, zone: ZoneId = ZoneId.systemDefault()): String =
    DateTimeFormatter.ISO_INSTANT.format(date.atTime(DEFAULT_DUE_TIME).atZone(zone).toInstant())

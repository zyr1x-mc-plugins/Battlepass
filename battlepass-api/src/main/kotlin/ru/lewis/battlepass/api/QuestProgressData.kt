package ru.lewis.battlepass.api

/**
 * Snapshot of a single quest's progress.
 */
data class QuestProgressData(
    val questId: String,
    val questType: String,
    val current: Int,
    val required: Int,
    val completed: Boolean,
)

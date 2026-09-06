package ru.lewis.battlepass.api

import java.util.UUID

/**
 * Snapshot of a player's BattlePass state.
 */
data class BattlePassPlayerData(
    val uuid: UUID,
    val xp: Long,
    val isPremium: Boolean,
)

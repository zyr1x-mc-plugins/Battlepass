package ru.lewis.battlepass.model

import java.util.UUID

data class BattlePassPlayer(
    val uuid: UUID,
    var xp: Long = 0,
    var isPremium: Boolean = false,
    var lastDailyReset: Long = 0,
)

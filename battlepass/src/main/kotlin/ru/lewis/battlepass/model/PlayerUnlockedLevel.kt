package ru.lewis.battlepass.model

import java.util.UUID

data class PlayerUnlockedLevel(
    val uuid: UUID,
    val levelNumber: Int,
)

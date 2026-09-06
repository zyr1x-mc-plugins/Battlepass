package ru.lewis.battlepass.model

import java.util.UUID

data class PlayerReward(
    val uuid: UUID,
    val rewardNumber: Int,
    val isPremium: Boolean,
)

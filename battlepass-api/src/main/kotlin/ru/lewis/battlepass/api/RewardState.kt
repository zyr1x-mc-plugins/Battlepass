package ru.lewis.battlepass.api

/**
 * State of a BattlePass reward for a player.
 */
enum class RewardState {
    LOCKED,
    UNLOCKED,
    CLAIMED,
    PREMIUM_LOCKED,
    PREMIUM_UNLOCKED,
    PREMIUM_CLAIMED
}

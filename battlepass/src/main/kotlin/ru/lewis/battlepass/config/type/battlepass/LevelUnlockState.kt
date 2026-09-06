package ru.lewis.battlepass.config.type.battlepass

/**
 * State of a BattlePass level for a player.
 *
 * Levels unlock strictly in order: a level can only be opened once the previous
 * defined level is unlocked. XP is the cost of opening a level (it is spent
 * when the level unlocks), not an absolute cumulative threshold.
 */
enum class LevelUnlockState {
    /** Previous level is not unlocked yet — this one cannot be opened. */
    LOCKED_BY_PREVIOUS,

    /** Previous level is unlocked, but the player does not have enough XP. */
    NOT_ENOUGH_XP,

    /** Previous level is unlocked and the player has enough XP to open it. */
    AVAILABLE_TO_UNLOCK,

    /** The level has been opened (its XP cost was already spent). */
    UNLOCKED
}

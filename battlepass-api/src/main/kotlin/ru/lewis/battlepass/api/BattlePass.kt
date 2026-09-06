package ru.lewis.battlepass.api

import java.util.UUID
import java.util.concurrent.CompletableFuture

/**
 * Main entry point for the BattlePass API.
 *
 * Usage:
 * ```kotlin
 * val api = BattlePass.get()
 * val xp = api.getXp(playerUuid)
 * ```
 */
interface BattlePass {

    companion object {
        @Volatile
        private var instance: BattlePass? = null

        /**
         * Returns the BattlePass API instance.
         * Must be called after the BattlePass plugin has been enabled.
         */
        @JvmStatic
        fun get(): BattlePass {
            return instance
                ?: throw IllegalStateException(
                    "BattlePass API is not initialized yet. " +
                        "Wait for the plugin to fully enable."
                )
        }

        fun init(impl: BattlePass) {
            instance = impl
        }

        fun shutdown() {
            instance = null
        }
    }

    // ── XP ──────────────────────────────────────────────────────────────

    /**
     * Returns the player's current BattlePass XP.
     * If the player has no data yet, returns 0.
     */
    fun getXp(player: UUID): CompletableFuture<Long>

    /**
     * Adds XP to the player's BattlePass.
     * Returns true if XP was successfully added.
     */
    fun addXp(player: UUID, amount: Long): CompletableFuture<Boolean>

    /**
     * Sets the player's BattlePass XP to the given value.
     * Returns true if XP was successfully set.
     */
    fun setXp(player: UUID, amount: Long): CompletableFuture<Boolean>

    // ── Premium ─────────────────────────────────────────────────────────

    /**
     * Returns whether the player has an active Premium BattlePass.
     */
    fun hasPremium(player: UUID): CompletableFuture<Boolean>

    /**
     * Grants Premium BattlePass to the player.
     * Returns true if Premium was successfully granted.
     */
    fun givePremium(player: UUID): CompletableFuture<Boolean>

    /**
     * Removes Premium BattlePass from the player.
     * Returns true if Premium was successfully removed.
     */
    fun removePremium(player: UUID): CompletableFuture<Boolean>

    // ── Levels / Rewards ────────────────────────────────────────────────
    //
    // BattlePass is composed of levels. Each level has exactly one required
    // XP threshold and owns two rewards: the FREE track and the PREMIUM track.
    // XP unlocks levels; Premium only unlocks the PREMIUM track of levels
    // already unlocked by XP.

    /**
     * Returns the XP required to unlock the given level, or null if the
     * level does not exist.
     */
    fun getRequiredXp(level: Int): CompletableFuture<Long?>

    /**
     * Returns the list of defined level numbers, in ascending order.
     */
    fun getLevels(): CompletableFuture<List<Int>>

    /**
     * Returns the highest level the player has unlocked via XP, or 0 if none.
     */
    fun getPlayerLevel(player: UUID): CompletableFuture<Int>

    /**
     * Returns true if the player's XP is enough to unlock the given level.
     */
    fun isLevelUnlocked(player: UUID, level: Int): CompletableFuture<Boolean>

    /**
     * Checks if the FREE reward of the given level is unlocked for the player
     * (level exists, enough XP).
     */
    fun isRewardUnlocked(player: UUID, level: Int): CompletableFuture<Boolean>

    /**
     * Checks if the player has already claimed the FREE reward of the given level.
     */
    fun isRewardClaimed(player: UUID, level: Int): CompletableFuture<Boolean>

    /**
     * Checks if the player has already claimed the PREMIUM reward of the given level.
     */
    fun hasClaimedFreeReward(player: UUID, level: Int): CompletableFuture<Boolean>

    /**
     * Checks if the player has already claimed the PREMIUM reward of the given level.
     */
    fun hasClaimedPremiumReward(player: UUID, level: Int): CompletableFuture<Boolean>

    /**
     * Checks whether the FREE reward of the given level can be claimed
     * (level exists, unlocked by XP, not yet claimed).
     */
    fun canClaimFreeReward(player: UUID, level: Int): CompletableFuture<Boolean>

    /**
     * Checks whether the PREMIUM reward of the given level can be claimed
     * (level exists, unlocked by XP, player has Premium, not yet claimed).
     */
    fun canClaimPremiumReward(player: UUID, level: Int): CompletableFuture<Boolean>

    /**
     * Attempts to claim the FREE reward of the given level for the player.
     * Returns true if the reward was successfully claimed.
     */
    fun claimReward(player: UUID, level: Int): CompletableFuture<Boolean>

    /**
     * Attempts to claim the PREMIUM reward of the given level for the player.
     * Returns true if the reward was successfully claimed.
     */
    fun claimPremiumReward(player: UUID, level: Int): CompletableFuture<Boolean>

    // ── Quests ──────────────────────────────────────────────────────────

    /**
     * Returns the player's current quest progress (current/required).
     */
    fun getQuestProgress(player: UUID): CompletableFuture<List<QuestProgressData>>

    // ── Data ────────────────────────────────────────────────────────────

    /**
     * Returns the full BattlePass player data.
     */
    fun getPlayerData(player: UUID): CompletableFuture<BattlePassPlayerData?>

    /**
     * Reloads all BattlePass configurations from disk.
     */
    fun reload(): CompletableFuture<Boolean>
}

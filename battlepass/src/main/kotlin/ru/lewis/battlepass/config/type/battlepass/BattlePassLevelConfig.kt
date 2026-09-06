package ru.lewis.battlepass.config.type.battlepass

import org.spongepowered.configurate.objectmapping.ConfigSerializable
import ru.lewis.battlepass.config.type.ItemTemplate

/**
 * One BattlePass level, stored in its own config file (levels/level-N.yml).
 *
 * A level owns exactly two independent reward tracks: [free] and [premium].
 * Both tracks share the same [requiredXp] threshold — XP unlocks the level,
 * Premium only gates the PREMIUM track of already-unlocked levels.
 *
 * Each track keeps its own [BattlePassLevelConfig.LevelTrack.display] (visual
 * Preview items, never part of the real issuance) and
 * [BattlePassLevelConfig.LevelTrack.rewards] (the actual items + commands that
 * are given when claiming).
 */
@ConfigSerializable
data class BattlePassLevelConfig(
    val level: Int = 0,
    val requiredXp: Long = 0,
    val free: LevelTrack = LevelTrack(),
    val premium: LevelTrack = LevelTrack(),
) {
    @ConfigSerializable
    data class LevelTrack(
        // Visual-only Preview items for this track. Not part of real
        // issuance.
        val display: List<ItemTemplate> = listOf(),
        val rewards: RewardContent = RewardContent(),
    ) {
        @ConfigSerializable
        data class RewardContent(
            val items: List<ItemTemplate> = listOf(),
            val commands: List<String> = listOf(),
        )
    }
}

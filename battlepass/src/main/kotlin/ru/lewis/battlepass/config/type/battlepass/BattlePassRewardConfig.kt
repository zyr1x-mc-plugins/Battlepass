package ru.lewis.battlepass.config.type.battlepass

import org.spongepowered.configurate.objectmapping.ConfigSerializable
import ru.lewis.battlepass.config.type.ItemTemplate
import ru.lewis.battlepass.config.type.MiniMessageComponent

@ConfigSerializable
data class BattlePassRewardConfig(
    val number: Int = 0,
    val requiredXp: Long = 0,
    val display: RewardDisplay = RewardDisplay(),
    val rewards: RewardContent = RewardContent(),
    val premium: RewardContent = RewardContent(),
) {
    @ConfigSerializable
    data class RewardDisplay(
        val item: ItemTemplate = ItemTemplate(),
        val additionalLore: List<MiniMessageComponent> = listOf(),
        val claimedItem: ItemTemplate? = null,
        val freePreview: List<ItemTemplate> = listOf(),
        val premiumPreview: List<ItemTemplate> = listOf(),
    )

    @ConfigSerializable
    data class RewardContent(
        val items: List<ItemTemplate> = listOf(),
        val commands: List<String> = listOf(),
    )
}

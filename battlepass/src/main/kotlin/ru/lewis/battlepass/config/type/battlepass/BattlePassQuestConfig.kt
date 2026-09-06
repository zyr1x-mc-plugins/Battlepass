package ru.lewis.battlepass.config.type.battlepass

import org.bukkit.Material
import org.bukkit.entity.EntityType
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import ru.lewis.battlepass.config.type.ItemTemplate
import ru.lewis.battlepass.config.type.MiniMessageComponent
import ru.lewis.battlepass.extensions.asMiniMessageComponent

@ConfigSerializable
data class BattlePassQuestConfig(
    val type: String = "",
    val required: Int = 1,
    val xp: Long = 0,
    // Blocks/items relevant to the quest. Used as a material filter for
    // BREAK_BLOCK / MINE_BLOCK / PLACE_BLOCK (the block involved), FISH (the
    // caught fish item) and CRAFT_ITEM (the crafted result). When empty the
    // quest accepts any target.
    val materials: List<Material> = listOf(),
    // Creatures relevant to the quest. Used as the target filter for
    // KILL_ENTITY quests. When empty the quest accepts any mob kill.
    val entities: List<EntityType> = listOf(),
    // Configurable per-quest display name, e.g. "<gold>Поставить блоки".
    // If not set in the config, falls back to a readable version of `type`
    // ("PLACE_BLOCK" -> "Place Block") instead of the raw enum-like string.
    val displayName: MiniMessageComponent? = null,
    // Configurable per-quest icon, e.g. a dirt block for a "place blocks"
    // quest or a fishing rod for a "fish" quest. If not set, the GUI falls
    // back to the global questItem.inProgressIcon/completedIcon.
    val icon: ItemTemplate? = null,
) {
    fun resolvedDisplayName(): MiniMessageComponent =
        displayName ?: formatTypeFallback(type).asMiniMessageComponent()

    companion object {
        private fun formatTypeFallback(type: String): String {
            if (type.isBlank()) return type
            return type.lowercase()
                .split('_')
                .filter { it.isNotEmpty() }
                .joinToString(" ") { it.replaceFirstChar(Char::uppercase) }
        }
    }
}
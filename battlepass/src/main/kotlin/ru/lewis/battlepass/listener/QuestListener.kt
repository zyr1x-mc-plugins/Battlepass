package ru.lewis.battlepass.listener

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.bukkit.Material
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.inventory.CraftItemEvent
import org.bukkit.event.player.PlayerFishEvent
import ru.lewis.battlepass.config.type.battlepass.BattlePassQuestConfig
import ru.lewis.battlepass.config.type.battlepass.DailyQuestType
import ru.lewis.battlepass.model.PlayerQuestProgress
import ru.lewis.battlepass.service.BattlePassService
import ru.lewis.battlepass.service.BattlePassConfigService
import ru.lewis.battlepass.service.WorldGuardHelper
import java.util.concurrent.ConcurrentHashMap

@Singleton
class QuestListener @Inject constructor(
    private val battlePassService: BattlePassService,
    private val configService: BattlePassConfigService,
    private val worldGuardHelper: WorldGuardHelper,
) : Listener {

    // Cached as IndexedValue so we always know exactly which entry in
    // configService.questConfigs a given quest came from - see
    // progressMatches() for why that index matters.
    private val materialCache = ConcurrentHashMap<Material, List<IndexedValue<BattlePassQuestConfig>>>()
    private val entityCache = ConcurrentHashMap<EntityType, List<IndexedValue<BattlePassQuestConfig>>>()

    private fun getQuestsForMaterial(material: Material): List<IndexedValue<BattlePassQuestConfig>> {
        return materialCache.getOrPut(material) {
            configService.questConfigs.withIndex().filter { (_, quest) ->
                quest.type in setOf(
                    DailyQuestType.BREAK_BLOCK.name,
                    DailyQuestType.PLACE_BLOCK.name,
                    DailyQuestType.MINE_BLOCK.name
                ) && (quest.materials.isEmpty() || material in quest.materials)
            }
        }
    }

    private fun getQuestsForEntity(entityType: EntityType): List<IndexedValue<BattlePassQuestConfig>> {
        return entityCache.getOrPut(entityType) {
            configService.questConfigs.withIndex().filter { (_, quest) ->
                quest.type in setOf(
                    DailyQuestType.KILL_ENTITY.name,
                    DailyQuestType.KILL_PLAYER.name
                )
            }
        }
    }

    private fun matchesQuest(quest: BattlePassQuestConfig, type: DailyQuestType): Boolean {
        return quest.type == type.name
    }

    // A quest `type` (BREAK_BLOCK, FISH, ...) is a category, not a unique id:
    // several variants (different materials/entities/required/xp) can share
    // the same type. So matching progress by `questType` alone is ambiguous
    // whenever more than one such variant exists - e.g. crediting a
    // "break netherrack" quest for breaking plain stone, just because both
    // happen to be BREAK_BLOCK entries. When we know which exact config
    // index this piece of progress was generated from, require it to match
    // the variant actually being credited here. Progress saved before
    // `configIndex` existed (-1) falls back to the old type-only match.
    private fun progressMatches(progress: PlayerQuestProgress, questIndex: Int, questType: String): Boolean {
        if (progress.completed || progress.questType != questType) return false
        if (progress.configIndex != -1) return progress.configIndex == questIndex
        return true
    }

    private fun grantProgress(player: Player, progress: PlayerQuestProgress) {
        progress.current++
        if (progress.current >= progress.required) {
            progress.completed = true
            battlePassService.completeQuest(player, progress)
        } else {
            battlePassService.updateQuestProgress(player, progress)
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBlockBreak(event: BlockBreakEvent) {
        val player = event.player
        val material = event.block.type
        val matchingQuests = getQuestsForMaterial(material).filter { (_, quest) ->
            matchesQuest(quest, DailyQuestType.BREAK_BLOCK) || matchesQuest(quest, DailyQuestType.MINE_BLOCK)
        }
        if (matchingQuests.isEmpty()) return

        val progressList = battlePassService.getQuestProgress(player.uniqueId)
        for ((index, quest) in matchingQuests) {
            val progress = progressList.find { progressMatches(it, index, quest.type) } ?: continue
            grantProgress(player, progress)
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBlockPlace(event: BlockPlaceEvent) {
        val player = event.player
        val material = event.block.type
        val matchingQuests = getQuestsForMaterial(material).filter { (_, quest) ->
            matchesQuest(quest, DailyQuestType.PLACE_BLOCK)
        }
        if (matchingQuests.isEmpty()) return

        val progressList = battlePassService.getQuestProgress(player.uniqueId)
        for ((index, quest) in matchingQuests) {
            val progress = progressList.find { progressMatches(it, index, quest.type) } ?: continue
            grantProgress(player, progress)
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onEntityDeath(event: EntityDeathEvent) {
        val killer = event.entity.killer ?: return
        val entityType = event.entityType
        val matchingQuests = getQuestsForEntity(entityType)

        val type = if (entityType == EntityType.PLAYER) DailyQuestType.KILL_PLAYER else DailyQuestType.KILL_ENTITY
        val filteredQuests = matchingQuests.filter { (_, quest) -> matchesQuest(quest, type) }
        if (filteredQuests.isEmpty()) return

        val progressList = battlePassService.getQuestProgress(killer.uniqueId)
        for ((index, quest) in filteredQuests) {
            // KILL_ENTITY quests can be scoped to specific creatures via the
            // `entities` field; KILL_PLAYER is always scoped to players.
            val targets = quest.entities
            if (type == DailyQuestType.KILL_ENTITY && targets.isNotEmpty() && entityType !in targets) continue

            val progress = progressList.find { progressMatches(it, index, quest.type) } ?: continue
            grantProgress(killer, progress)
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onCraft(event: CraftItemEvent) {
        val player = event.whoClicked as? Player ?: return
        // The crafted result, e.g. a Furnace when crafting a furnace. Used to
        // scope CRAFT_ITEM quests to a specific crafted item via `materials`.
        val result = event.inventory.result?.type ?: return
        val matchingQuests = configService.questConfigs.withIndex()
            .filter { (_, quest) -> matchesQuest(quest, DailyQuestType.CRAFT_ITEM) }
        if (matchingQuests.isEmpty()) return

        val progressList = battlePassService.getQuestProgress(player.uniqueId)
        for ((index, quest) in matchingQuests) {
            val items = quest.materials
            if (items.isNotEmpty() && result !in items) continue

            val progress = progressList.find { progressMatches(it, index, quest.type) } ?: continue
            grantProgress(player, progress)
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onFish(event: PlayerFishEvent) {
        if (event.state != PlayerFishEvent.State.CAUGHT_FISH) return
        val player = event.player
        // The caught item (Cod, Salmon, Pufferfish, Tropical Fish, ...) scopes
        // FISH quests to a specific catch via `materials`.
        val caught = (event.caught as? org.bukkit.entity.Item)?.itemStack?.type
        val matchingQuests = configService.questConfigs.withIndex()
            .filter { (_, quest) -> matchesQuest(quest, DailyQuestType.FISH) }
        if (matchingQuests.isEmpty()) return

        val progressList = battlePassService.getQuestProgress(player.uniqueId)
        for ((index, quest) in matchingQuests) {
            val items = quest.materials
            if (items.isNotEmpty()) {
                if (caught == null || caught !in items) continue
            }

            val progress = progressList.find { progressMatches(it, index, quest.type) } ?: continue
            grantProgress(player, progress)
        }
    }

    fun invalidateCache() {
        materialCache.clear()
        entityCache.clear()
    }
}

package ru.lewis.battlepass.service

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.bukkit.entity.Player
import ru.lewis.battlepass.config.type.battlepass.BattlePassQuestConfig
import ru.lewis.battlepass.config.type.battlepass.DailyQuestType
import ru.lewis.battlepass.model.PlayerQuestProgress
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import java.util.concurrent.ThreadLocalRandom

@Singleton
class DailyQuestManager @Inject constructor(
    private val configService: BattlePassConfigService,
) {
    fun needsDailyReset(lastReset: Long): Boolean {
        val today = LocalDate.now(ZoneId.systemDefault())
        val todayStart = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return lastReset < todayStart
    }

    fun generateDailyQuests(playerUuid: UUID): List<PlayerQuestProgress> {
        val availableQuests = configService.questConfigs
        if (availableQuests.isEmpty()) return emptyList()

        val random = ThreadLocalRandom.current()

        // Group config entries by quest type (BREAK_BLOCK, FISH, ...) first.
        // A type can have several variants configured (different materials/
        // entities/required amounts/xp), but a player should only ever get
        // ONE quest per type on a given day - otherwise two variants of the
        // same type (e.g. "break 64 netherrack" and "break 32 stone") end up
        // active at once and can't be told apart by type alone, which is
        // what caused daily quests to look repeated/mismatched. Pick a
        // single random variant per type, then pick `amount` distinct types.
        val variantsByType = availableQuests.withIndex().groupBy { (_, quest) -> quest.type }
        val onePerType = variantsByType.values.map { variants -> variants[random.nextInt(variants.size)] }

        val amount = configService.config.dailyQuests.amount.coerceAtMost(onePerType.size)
        val selected = onePerType.shuffled(random).take(amount)

        return selected.map { (index, quest) ->
            PlayerQuestProgress(
                uuid = playerUuid,
                questId = "${quest.type}-${random.nextLong(Long.MAX_VALUE)}",
                questType = quest.type,
                required = quest.required,
                current = 0,
                completed = false,
                configIndex = index,
            )
        }
    }

    fun getQuestConfig(questType: String): BattlePassQuestConfig? {
        return configService.questConfigs.find { it.type == questType }
    }

    // Resolves the exact quest variant that was rolled for this piece of
    // progress, instead of just the first config entry that happens to share
    // its `questType`. Falls back to a type-only lookup for progress saved
    // before `configIndex` existed (or if the config list was reloaded/
    // reordered since the quest was generated), so old data doesn't break.
    fun getQuestConfigForProgress(progress: PlayerQuestProgress): BattlePassQuestConfig? {
        val byIndex = configService.questConfigs.getOrNull(progress.configIndex)
        if (byIndex != null && byIndex.type == progress.questType) return byIndex
        return getQuestConfig(progress.questType)
    }

    fun getTodayEpochMillis(): Long {
        val today = LocalDate.now(ZoneId.systemDefault())
        return today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}

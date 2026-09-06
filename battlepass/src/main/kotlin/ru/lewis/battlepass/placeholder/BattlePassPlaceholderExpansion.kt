package ru.lewis.battlepass.placeholder

import com.google.inject.Inject
import com.google.inject.Singleton
import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.entity.Player
import ru.lewis.battlepass.config.type.battlepass.RewardClaimState
import ru.lewis.battlepass.service.BattlePassConfigService
import ru.lewis.battlepass.service.BattlePassService
import ru.lewis.battlepass.service.DailyQuestManager
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

@Singleton
class BattlePassPlaceholderExpansion @Inject constructor(
    private val battlePassService: BattlePassService,
    private val configService: BattlePassConfigService,
    private val dailyQuestManager: DailyQuestManager,
) : PlaceholderExpansion() {

    override fun getIdentifier(): String = "battlepass"

    override fun getAuthor(): String = "Lewis Carrol"

    override fun getVersion(): String = "1.0"

    override fun persist(): Boolean = true

    override fun canRegister(): Boolean = true

    override fun onPlaceholderRequest(player: Player?, params: String): String? {
        if (player == null) return null
        val uuid = player.uniqueId
        val bpPlayer = battlePassService.getOrCreatePlayer(uuid)
        val placeholderConfig = configService.config.placeholders

        return when {
            // ── General ─────────────────────────────────────────────
            params == "has" -> "true"

            params == "xp" -> bpPlayer.xp.toString()

            params == "xp_formatted" -> bpPlayer.xp.toString()

            // ── Premium ────────────────────────────────────────────
            params == "premium" -> bpPlayer.isPremium.toString()

            params == "premium_status" -> {
                if (bpPlayer.isPremium) placeholderConfig.premiumActive
                else placeholderConfig.premiumInactive
            }

            // ── Progress ───────────────────────────────────────────
            params == "progress" || params == "progress_formatted" -> {
                val nextLevel = battlePassService.getNextLockedLevel(uuid)
                if (nextLevel == null) "100%"
                else {
                    val prevXp = battlePassService.sortedLevels
                        .filter { it.requiredXp <= bpPlayer.xp }
                        .maxOfOrNull { it.requiredXp } ?: 0L
                    val needed = nextLevel.requiredXp - prevXp
                    val current = bpPlayer.xp - prevXp
                    val percent = if (needed > 0) ((current * 100) / needed).toInt().coerceIn(0, 100) else 100
                    placeholderConfig.progressFormat
                        .replace("%percent%", percent.toString())
                }
            }

            // ── Rewards ────────────────────────────────────────────
            params == "rewards_total" -> battlePassService.sortedLevels.size.toString()

            params == "rewards_claimed" -> {
                battlePassService.sortedLevels.count { battlePassService.hasClaimedReward(uuid, it.level) }.toString()
            }

            params == "rewards_available" -> {
                battlePassService.sortedLevels.count { level ->
                    battlePassService.getFreeRewardState(uuid, level) == RewardClaimState.UNLOCKED
                }.toString()
            }

            params == "rewards_locked" -> {
                battlePassService.sortedLevels.count { level ->
                    battlePassService.getFreeRewardState(uuid, level) == RewardClaimState.LOCKED
                }.toString()
            }

            // ── Premium Rewards ────────────────────────────────────
            params == "premium_rewards_total" -> battlePassService.sortedLevels.size.toString()

            params == "premium_rewards_claimed" -> {
                battlePassService.sortedLevels.count { battlePassService.hasClaimedPremiumReward(uuid, it.level) }.toString()
            }

            params == "premium_rewards_available" -> {
                if (!bpPlayer.isPremium) "0"
                else {
                    battlePassService.sortedLevels.count { level ->
                        battlePassService.getPremiumRewardState(uuid, level) == RewardClaimState.PREMIUM_UNLOCKED
                    }.toString()
                }
            }

            params == "premium_rewards_locked" -> {
                battlePassService.sortedLevels.count { level ->
                    battlePassService.getPremiumRewardState(uuid, level) == RewardClaimState.PREMIUM_LOCKED
                }.toString()
            }

            // ── Next Reward ────────────────────────────────────────
            params == "next_reward" -> {
                val next = battlePassService.getNextLockedLevel(uuid)
                next?.level?.toString() ?: placeholderConfig.noData
            }

            params == "next_reward_required_xp" -> {
                val next = battlePassService.getNextLockedLevel(uuid)
                next?.requiredXp?.toString() ?: placeholderConfig.noData
            }

            params == "next_reward_remaining_xp" -> {
                val next = battlePassService.getNextLockedLevel(uuid)
                if (next == null) "0"
                else (next.requiredXp - bpPlayer.xp).coerceAtLeast(0).toString()
            }

            // ── Unclaimed ──────────────────────────────────────────
            params == "has_unclaimed_rewards" -> {
                battlePassService.hasUnclaimedRewards(uuid).toString()
            }

            params == "unclaimed_rewards" -> {
                battlePassService.sortedLevels.count { level ->
                    battlePassService.getFreeRewardState(uuid, level) == RewardClaimState.UNLOCKED
                }.toString()
            }

            // ── Quests ─────────────────────────────────────────────
            params == "quests_total" -> {
                battlePassService.getQuestProgress(uuid).size.toString()
            }

            params == "quests_completed" -> {
                battlePassService.getQuestProgress(uuid).count { it.completed }.toString()
            }

            params == "quests_remaining" -> {
                battlePassService.getQuestProgress(uuid).count { !it.completed }.toString()
            }

            params == "quests_progress" -> {
                val quests = battlePassService.getQuestProgress(uuid)
                val completed = quests.count { it.completed }
                val total = quests.size
                placeholderConfig.questsProgressFormat
                    .replace("%completed%", completed.toString())
                    .replace("%total%", total.toString())
            }

            params == "quests_completed_percent" -> {
                val quests = battlePassService.getQuestProgress(uuid)
                val completed = quests.count { it.completed }
                val total = quests.size
                val percent = if (total > 0) (completed * 100) / total else 0
                "$percent%"
            }

            params == "all_quests_completed" -> {
                val quests = battlePassService.getQuestProgress(uuid)
                (quests.isNotEmpty() && quests.all { it.completed }).toString()
            }

            // ── Quest Reset ────────────────────────────────────────
            params == "quests_reset" -> {
                val now = Instant.now()
                val tomorrow = now.atZone(ZoneId.systemDefault())
                    .toLocalDate()
                    .plusDays(1)
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant()
                val duration = Duration.between(now, tomorrow)
                val hours = duration.toHours()
                val minutes = duration.toMinutesPart()
                val seconds = duration.toSecondsPart()
                String.format("%02d:%02d:%02d", hours, minutes, seconds)
            }

            // ── Quest by ID ────────────────────────────────────────
            params.startsWith("quest_") && params.endsWith("_progress") -> {
                val questId = params.removePrefix("quest_").removeSuffix("_progress")
                val progress = battlePassService.getQuestProgress(uuid).find { it.questId == questId }
                progress?.current?.toString() ?: placeholderConfig.noData
            }

            params.startsWith("quest_") && params.endsWith("_required") -> {
                val questId = params.removePrefix("quest_").removeSuffix("_required")
                val progress = battlePassService.getQuestProgress(uuid).find { it.questId == questId }
                progress?.required?.toString() ?: placeholderConfig.noData
            }

            params.startsWith("quest_") && params.endsWith("_completed") -> {
                val questId = params.removePrefix("quest_").removeSuffix("_completed")
                val progress = battlePassService.getQuestProgress(uuid).find { it.questId == questId }
                progress?.completed?.toString() ?: "false"
            }

            else -> null
        }
    }
}

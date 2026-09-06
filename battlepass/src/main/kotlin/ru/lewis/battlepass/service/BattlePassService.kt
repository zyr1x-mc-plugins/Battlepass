package ru.lewis.battlepass.service

import com.google.inject.Inject
import com.google.inject.Singleton
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import ru.lewis.battlepass.config.type.battlepass.BattlePassLevelConfig
import ru.lewis.battlepass.config.type.battlepass.LevelUnlockState
import ru.lewis.battlepass.config.type.battlepass.RewardClaimState
import ru.lewis.battlepass.model.BattlePassPlayer
import ru.lewis.battlepass.model.PlayerQuestProgress
import ru.lewis.battlepass.repository.BattlePassRepository
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CompletableFuture
import java.util.logging.Level

@Singleton
class BattlePassService @Inject constructor(
    private val repository: BattlePassRepository,
    private val configService: BattlePassConfigService,
    private val dailyQuestManager: DailyQuestManager,
) {
    private val notifiedPlayers = ConcurrentHashMap.newKeySet<UUID>()

    val sortedLevels: List<BattlePassLevelConfig>
        get() = configService.levelConfigs.values.sortedBy { it.level }

    fun getLevel(levelNumber: Int): BattlePassLevelConfig? =
        configService.levelConfigs[levelNumber]

    fun getRequiredXp(levelNumber: Int): Long? =
        getLevel(levelNumber)?.requiredXp

    fun getPlayerLevel(uuid: UUID): Int {
        val unlocked = repository.loadUnlockedLevels(uuid)
        return sortedLevels.lastOrNull { unlocked.contains(it.level) }?.level ?: 0
    }

    fun getLevelNumbers(): List<Int> =
        sortedLevels.map { it.level }

    // ── Level unlocking (sequential, XP is a spent cost) ────────────────

    /**
     * State of a level for the player. Unlocking is strictly sequential: a
     * level can only be opened once the previous defined level is unlocked.
     */
    fun getLevelState(uuid: UUID, levelNumber: Int): LevelUnlockState {
        val sorted = sortedLevels
        val level = sorted.firstOrNull { it.level == levelNumber } ?: return LevelUnlockState.LOCKED_BY_PREVIOUS
        val unlocked = repository.loadUnlockedLevels(uuid)
        if (unlocked.contains(levelNumber)) return LevelUnlockState.UNLOCKED

        val idx = sorted.indexOfFirst { it.level == levelNumber }
        val prevUnlocked = if (idx <= 0) true else unlocked.contains(sorted[idx - 1].level)
        if (!prevUnlocked) return LevelUnlockState.LOCKED_BY_PREVIOUS

        return if (getOrCreatePlayer(uuid).xp >= level.requiredXp) {
            LevelUnlockState.AVAILABLE_TO_UNLOCK
        } else {
            LevelUnlockState.NOT_ENOUGH_XP
        }
    }

    /**
     * Automatically opens every level the player can currently afford, in
     * order, spending the required XP cost of each opened level. Returns how
     * many levels were newly unlocked. XP deduction and the unlocked-level
     * rows are persisted atomically by the repository.
     */
    fun autoUnlock(player: Player): Int = autoUnlock(player.uniqueId)

    fun autoUnlock(uuid: UUID): Int {
        val bpPlayer = getOrCreatePlayer(uuid)
        val unlocked = repository.loadUnlockedLevels(uuid)
        val toUnlock = mutableListOf<BattlePassLevelConfig>()
        var prevUnlocked = true

        for (level in sortedLevels) {
            val alreadyUnlocked = unlocked.contains(level.level)
            if (!prevUnlocked && !alreadyUnlocked) break
            if (alreadyUnlocked) {
                prevUnlocked = true
                continue
            }
            // prevUnlocked == true here and this level is not yet open.
            if (bpPlayer.xp < level.requiredXp) {
                prevUnlocked = false
                break
            }
            bpPlayer.xp -= level.requiredXp
            toUnlock.add(level)
            prevUnlocked = true
        }

        if (toUnlock.isEmpty()) return 0

        val success = repository.unlockLevelsAndDeductXp(bpPlayer, toUnlock.map { it.level })
        if (!success) {
            // Keep the in-memory XP consistent with the rollback done in the
            // repository (unlocked entries there were rolled back).
            toUnlock.forEach { bpPlayer.xp += it.requiredXp }
            return 0
        }
        return toUnlock.size
    }

    fun loadPlayer(uuid: UUID): CompletableFuture<BattlePassPlayer> {
        return CompletableFuture.supplyAsync {
            repository.loadPlayer(uuid)
        }
    }

    fun getOrCreatePlayer(uuid: UUID): BattlePassPlayer {
        return repository.loadPlayer(uuid)
    }

    fun savePlayer(player: BattlePassPlayer) {
        CompletableFuture.runAsync {
            repository.savePlayer(player)
        }
    }

    fun addXp(player: Player, amount: Long): Int = addXp(player.uniqueId, amount)

    fun addXp(uuid: UUID, amount: Long): Int {
        val bpPlayer = getOrCreatePlayer(uuid)
        bpPlayer.xp += amount
        savePlayer(bpPlayer)
        // Newly gained XP is immediately spent on opening levels in order.
        return autoUnlock(uuid)
    }

    fun setXp(player: Player, amount: Long): Int = setXp(player.uniqueId, amount)

    fun setXp(uuid: UUID, amount: Long): Int {
        val bpPlayer = getOrCreatePlayer(uuid)
        bpPlayer.xp = amount
        savePlayer(bpPlayer)
        return autoUnlock(uuid)
    }

    fun isLevelUnlocked(uuid: UUID, levelNumber: Int): Boolean {
        return repository.loadUnlockedLevels(uuid).contains(levelNumber)
    }

    fun hasClaimedReward(uuid: UUID, levelNumber: Int): Boolean {
        return repository.loadClaimedRewards(uuid).contains(levelNumber)
    }

    fun hasClaimedPremiumReward(uuid: UUID, levelNumber: Int): Boolean {
        return repository.loadClaimedPremiumRewards(uuid).contains(levelNumber)
    }

    fun canClaimFreeReward(player: Player, levelNumber: Int): Boolean {
        if (getLevel(levelNumber) == null) return false
        val uuid = player.uniqueId
        if (!isLevelUnlocked(uuid, levelNumber)) return false
        return !hasClaimedReward(uuid, levelNumber)
    }

    fun canClaimPremiumReward(player: Player, levelNumber: Int): Boolean {
        if (getLevel(levelNumber) == null) return false
        val uuid = player.uniqueId
        if (!isLevelUnlocked(uuid, levelNumber)) return false
        if (!getOrCreatePlayer(uuid).isPremium) return false
        return !hasClaimedPremiumReward(uuid, levelNumber)
    }

    fun claimReward(player: Player, levelNumber: Int): Boolean {
        val uuid = player.uniqueId
        val level = getLevel(levelNumber) ?: return false

        if (!canClaimFreeReward(player, levelNumber)) return false
        val success = executeRewardContent(player, level.free.rewards)
        if (!success) return false

        repository.saveClaimedReward(uuid, levelNumber, false)
        return true
    }

    fun claimPremiumReward(player: Player, levelNumber: Int): Boolean {
        val uuid = player.uniqueId
        val level = getLevel(levelNumber) ?: return false

        if (!canClaimPremiumReward(player, levelNumber)) return false
        val success = executeRewardContent(player, level.premium.rewards)
        if (!success) return false

        repository.saveClaimedReward(uuid, levelNumber, true)
        return true
    }

    private fun executeRewardContent(player: Player, content: BattlePassLevelConfig.LevelTrack.RewardContent): Boolean {
        try {
            for (itemTemplate in content.items) {
                val item: ItemStack = itemTemplate.toCleanItem()
                val leftOver = player.inventory.addItem(item)
                if (leftOver.isNotEmpty()) {
                    for (overflow in leftOver.values) {
                        player.world.dropItemNaturally(player.location, overflow)
                    }
                }
            }

            for (command in content.commands) {
                val resolvedCommand = command.replace("%player%", player.name)
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), resolvedCommand)
            }
            return true
        } catch (e: Exception) {
            Bukkit.getLogger().log(Level.SEVERE, "Error executing reward content for ${player.name}", e)
            return false
        }
    }

    fun getFreeRewardState(uuid: UUID, level: BattlePassLevelConfig): RewardClaimState {
        return when {
            hasClaimedReward(uuid, level.level) -> RewardClaimState.CLAIMED
            isLevelUnlocked(uuid, level.level) -> RewardClaimState.UNLOCKED
            else -> RewardClaimState.LOCKED
        }
    }

    fun getPremiumRewardState(uuid: UUID, level: BattlePassLevelConfig): RewardClaimState {
        val bpPlayer = getOrCreatePlayer(uuid)
        return when {
            hasClaimedPremiumReward(uuid, level.level) -> RewardClaimState.PREMIUM_CLAIMED
            !isLevelUnlocked(uuid, level.level) -> RewardClaimState.PREMIUM_LOCKED
            !bpPlayer.isPremium -> RewardClaimState.PREMIUM_LOCKED
            else -> RewardClaimState.PREMIUM_UNLOCKED
        }
    }

    // Returns the first level (by ascending order) the player has NOT yet
    // unlocked, or null when every level is unlocked. Because unlocking is
    // sequential this is always the current frontier.
    fun getNextLockedLevel(uuid: UUID): BattlePassLevelConfig? {
        return sortedLevels.firstOrNull { !isLevelUnlocked(uuid, it.level) }
    }

    fun getQuestProgress(uuid: UUID): MutableList<PlayerQuestProgress> {
        return repository.loadQuestProgress(uuid)
    }

    fun checkAndResetDailyQuests(player: Player): Boolean {
        val bpPlayer = getOrCreatePlayer(player.uniqueId)
        val dateRolledOver = dailyQuestManager.needsDailyReset(bpPlayer.lastDailyReset)
        // Even if the date hasn't rolled over, the player's quest state may be
        // missing (e.g. first-ever open racing the async join load, a DB entry
        // that never got created, or state that was cleared externally). In
        // that case lastDailyReset already says "today" so a pure date check
        // would skip generation and leave the GUI empty until tomorrow — so
        // also regenerate whenever there is no quest state at all.
        val hasNoQuestState = repository.loadQuestProgress(player.uniqueId).isEmpty()
        if (!dateRolledOver && !hasNoQuestState) return false

        repository.clearDailyQuests(player.uniqueId)
        val newQuests = dailyQuestManager.generateDailyQuests(player.uniqueId)
        for (quest in newQuests) {
            repository.saveQuestProgress(player.uniqueId, quest)
        }

        bpPlayer.lastDailyReset = dailyQuestManager.getTodayEpochMillis()
        savePlayer(bpPlayer)
        return true
    }

    fun completeQuest(player: Player, progress: PlayerQuestProgress) {
        val questConfig = dailyQuestManager.getQuestConfigForProgress(progress) ?: return
        addXp(player, questConfig.xp)
        repository.saveQuestProgress(player.uniqueId, progress)

        val tagResolvers = arrayOf(
            Placeholder.unparsed("xp", questConfig.xp.toString()),
            Placeholder.unparsed("quest_progress", progress.current.toString()),
            Placeholder.unparsed("quest_required", progress.required.toString()),
        )

        player.sendMessage(configService.config.messages.questCompleted.resolve(*tagResolvers))

        val titleConfig = configService.config.titles.questCompleted
        val resolvedTitle = titleConfig.copy(
            mainTitle = titleConfig.mainTitle.resolve(*tagResolvers),
            subTitle = titleConfig.subTitle.resolve(*tagResolvers),
        )
        resolvedTitle.show(player)
    }

    fun updateQuestProgress(player: Player, progress: PlayerQuestProgress) {
        repository.saveQuestProgress(player.uniqueId, progress)

        val percent = progress.current.toFloat() / progress.required
        if (percent >= 0.5f && progress.current - 1 < (progress.required * 0.5f).toInt()) {
            val tagResolvers = arrayOf(
                Placeholder.unparsed("progress", (percent * 100).toInt().toString()),
            )
            val titleConfig = configService.config.titles.questProgress50
            val resolved = titleConfig.copy(
                mainTitle = titleConfig.mainTitle.resolve(*tagResolvers),
                subTitle = titleConfig.subTitle.resolve(*tagResolvers),
            )
            resolved.show(player)
        }
    }

    fun hasUnclaimedRewards(uuid: UUID): Boolean {
        return sortedLevels.any { level ->
            isLevelUnlocked(uuid, level.level) && !hasClaimedReward(uuid, level.level)
        }
    }

    fun setPremium(uuid: UUID, premium: Boolean) {
        val bpPlayer = getOrCreatePlayer(uuid)
        bpPlayer.isPremium = premium
        savePlayer(bpPlayer)
    }

    fun onPlayerJoin(player: Player) {
        loadPlayer(player.uniqueId).thenAccept { bpPlayer ->
            if (dailyQuestManager.needsDailyReset(bpPlayer.lastDailyReset)) {
                Bukkit.getScheduler().runTask(
                    Bukkit.getPluginManager().getPlugin("Battlepass")!!,
                    Runnable { checkAndResetDailyQuests(player) }
                )
            }
            // Best-effort sync so any XP that already covers a level's cost is
            // spent and that level is opened for the player.
            Bukkit.getScheduler().runTask(
                Bukkit.getPluginManager().getPlugin("Battlepass")!!,
                Runnable { autoUnlock(player) }
            )
        }
    }

    fun onPlayerQuit(player: Player) {
        notifiedPlayers.remove(player.uniqueId)
    }

    fun sendNotificationIfNeeded(player: Player) {
        if (notifiedPlayers.contains(player.uniqueId)) return
        if (hasUnclaimedRewards(player.uniqueId)) {
            player.sendMessage(configService.config.notifications.unclaimedRewards.message)
            notifiedPlayers.add(player.uniqueId)
        }
    }

    fun startNotificationTask() {
        val settings = configService.config.notifications.unclaimedRewards
        if (!settings.enabled) return

        val intervalTicks = settings.interval.toMillis() / 50L
        Bukkit.getScheduler().runTaskTimer(
            Bukkit.getPluginManager().getPlugin("Battlepass")!!,
            Runnable {
                for (player in Bukkit.getOnlinePlayers()) {
                    sendNotificationIfNeeded(player)
                }
            },
            intervalTicks,
            intervalTicks,
        )
    }
}
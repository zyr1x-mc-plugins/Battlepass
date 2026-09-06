package ru.lewis.battlepass.api

import org.bukkit.Bukkit
import ru.lewis.battlepass.service.BattlePassConfigService
import ru.lewis.battlepass.service.BattlePassService
import java.util.UUID
import java.util.concurrent.CompletableFuture

class BattlePassImpl(
    private val battlePassService: BattlePassService,
    private val configService: BattlePassConfigService,
) : BattlePass {

    override fun getXp(player: UUID): CompletableFuture<Long> {
        return battlePassService.loadPlayer(player).thenApply { it.xp }
    }

    override fun addXp(player: UUID, amount: Long): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            battlePassService.addXp(player, amount)
            true
        }
    }

    override fun setXp(player: UUID, amount: Long): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            battlePassService.setXp(player, amount)
            true
        }
    }

    override fun hasPremium(player: UUID): CompletableFuture<Boolean> {
        return battlePassService.loadPlayer(player).thenApply { it.isPremium }
    }

    override fun givePremium(player: UUID): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            battlePassService.setPremium(player, true)
            true
        }
    }

    override fun removePremium(player: UUID): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            battlePassService.setPremium(player, false)
            true
        }
    }

    override fun getRequiredXp(level: Int): CompletableFuture<Long?> {
        return CompletableFuture.completedFuture(battlePassService.getRequiredXp(level))
    }

    override fun getLevels(): CompletableFuture<List<Int>> {
        return CompletableFuture.completedFuture(battlePassService.getLevelNumbers())
    }

    override fun getPlayerLevel(player: UUID): CompletableFuture<Int> {
        return CompletableFuture.supplyAsync {
            battlePassService.getPlayerLevel(player)
        }
    }

    override fun isLevelUnlocked(player: UUID, level: Int): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            battlePassService.isLevelUnlocked(player, level)
        }
    }

    override fun isRewardUnlocked(player: UUID, level: Int): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            val levelConfig = battlePassService.getLevel(level) ?: return@supplyAsync false
            battlePassService.getFreeRewardState(player, levelConfig) ==
                ru.lewis.battlepass.config.type.battlepass.RewardClaimState.UNLOCKED
        }
    }

    override fun isRewardClaimed(player: UUID, level: Int): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            battlePassService.hasClaimedReward(player, level)
        }
    }

    override fun hasClaimedFreeReward(player: UUID, level: Int): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            battlePassService.hasClaimedReward(player, level)
        }
    }

    override fun hasClaimedPremiumReward(player: UUID, level: Int): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            battlePassService.hasClaimedPremiumReward(player, level)
        }
    }

    override fun canClaimFreeReward(player: UUID, level: Int): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            val onlinePlayer = Bukkit.getPlayer(player) ?: return@supplyAsync false
            battlePassService.canClaimFreeReward(onlinePlayer, level)
        }
    }

    override fun canClaimPremiumReward(player: UUID, level: Int): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            val onlinePlayer = Bukkit.getPlayer(player) ?: return@supplyAsync false
            battlePassService.canClaimPremiumReward(onlinePlayer, level)
        }
    }

    override fun claimReward(player: UUID, level: Int): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            val onlinePlayer = Bukkit.getPlayer(player) ?: return@supplyAsync false
            battlePassService.claimReward(onlinePlayer, level)
        }
    }

    override fun claimPremiumReward(player: UUID, level: Int): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            val onlinePlayer = Bukkit.getPlayer(player) ?: return@supplyAsync false
            battlePassService.claimPremiumReward(onlinePlayer, level)
        }
    }

    override fun getQuestProgress(player: UUID): CompletableFuture<List<QuestProgressData>> {
        return CompletableFuture.supplyAsync {
            battlePassService.getQuestProgress(player).map { progress ->
                QuestProgressData(
                    questId = progress.questId,
                    questType = progress.questType,
                    current = progress.current,
                    required = progress.required,
                    completed = progress.completed,
                )
            }
        }
    }

    override fun getPlayerData(player: UUID): CompletableFuture<BattlePassPlayerData?> {
        return battlePassService.loadPlayer(player).thenApply { bpPlayer ->
            BattlePassPlayerData(
                uuid = bpPlayer.uuid,
                xp = bpPlayer.xp,
                isPremium = bpPlayer.isPremium,
            )
        }
    }

    override fun reload(): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            try {
                configService.load()
                true
            } catch (e: Exception) {
                Bukkit.getLogger().severe("[BattlePass] Failed to reload config: ${e.message}")
                false
            }
        }
    }
}

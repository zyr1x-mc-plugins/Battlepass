package ru.lewis.battlepass.repository

import jakarta.inject.Inject
import jakarta.inject.Singleton
import ru.lewis.battlepass.extensions.fromTransaction
import ru.lewis.battlepass.extensions.inTransaction
import ru.lewis.battlepass.model.*
import ru.lewis.point.api.PointAPI
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Singleton
class BattlePassRepository @Inject constructor() {

    private val playerCache = ConcurrentHashMap<UUID, BattlePassPlayer>()
    private val rewardCache = ConcurrentHashMap<UUID, MutableSet<Int>>()
    private val premiumRewardCache = ConcurrentHashMap<UUID, MutableSet<Int>>()
    private val unlockedLevelCache = ConcurrentHashMap<UUID, MutableSet<Int>>()
    private val questCache = ConcurrentHashMap<UUID, MutableList<PlayerQuestProgress>>()

    private val sessionFactory get() = PointAPI.get().databaseService.sessionFactory

    fun loadPlayer(uuid: UUID): BattlePassPlayer {
        return playerCache[uuid] ?: run {
            val entity = sessionFactory.fromTransaction { session ->
                session.createQuery(
                    "FROM BattlePassPlayerEntity WHERE uuid = :uuid",
                    BattlePassPlayerEntity::class.java
                ).setParameter("uuid", uuid.toString()).uniqueResultOptional().orElse(null)
            }
            val player = entity?.toModel() ?: BattlePassPlayer(uuid)
            playerCache[uuid] = player
            player
        }
    }

    fun savePlayer(player: BattlePassPlayer) {
        playerCache[player.uuid] = player
        sessionFactory.inTransaction { session ->
            val existing = session.find(BattlePassPlayerEntity::class.java, player.uuid.toString())
            if (existing != null) {
                existing.xp = player.xp
                existing.isPremium = player.isPremium
                existing.lastDailyReset = player.lastDailyReset
                session.merge(existing)
            } else {
                session.persist(BattlePassPlayerEntity.fromModel(player))
            }
        }
    }

    fun loadClaimedRewards(uuid: UUID): MutableSet<Int> {
        return rewardCache[uuid] ?: run {
            val entities = sessionFactory.fromTransaction { session ->
                session.createQuery(
                    "FROM PlayerRewardEntity WHERE uuid = :uuid AND isPremium = false",
                    PlayerRewardEntity::class.java
                ).setParameter("uuid", uuid.toString()).resultList
            }
            val rewards = entities.map { it.rewardNumber }.toMutableSet()
            rewardCache[uuid] = rewards
            rewards
        }
    }

    fun loadClaimedPremiumRewards(uuid: UUID): MutableSet<Int> {
        return premiumRewardCache[uuid] ?: run {
            val entities = sessionFactory.fromTransaction { session ->
                session.createQuery(
                    "FROM PlayerRewardEntity WHERE uuid = :uuid AND isPremium = true",
                    PlayerRewardEntity::class.java
                ).setParameter("uuid", uuid.toString()).resultList
            }
            val rewards = entities.map { it.rewardNumber }.toMutableSet()
            premiumRewardCache[uuid] = rewards
            rewards
        }
    }

    fun saveClaimedReward(uuid: UUID, rewardNumber: Int, isPremium: Boolean) {
        if (isPremium) {
            premiumRewardCache.getOrPut(uuid) { mutableSetOf() }.add(rewardNumber)
        } else {
            rewardCache.getOrPut(uuid) { mutableSetOf() }.add(rewardNumber)
        }
        sessionFactory.inTransaction { session ->
            val entity = PlayerRewardEntity.fromModel(PlayerReward(uuid, rewardNumber, isPremium))
            session.persist(entity)
        }
    }

    fun loadUnlockedLevels(uuid: UUID): MutableSet<Int> {
        return unlockedLevelCache[uuid] ?: run {
            val entities = sessionFactory.fromTransaction { session ->
                session.createQuery(
                    "FROM PlayerUnlockedLevelEntity WHERE uuid = :uuid",
                    PlayerUnlockedLevelEntity::class.java
                ).setParameter("uuid", uuid.toString()).resultList
            }
            val levels = entities.map { it.levelNumber }.toMutableSet()
            unlockedLevelCache[uuid] = levels
            levels
        }
    }

    /**
     * Unlocks the given levels and deducts their XP cost ATOMICALLY: the
     * player's XP update and the unlocked-level rows are persisted in one
     * database transaction, so XP can never be spent without the level being
     * saved (or vice versa).
     */
    fun unlockLevelsAndDeductXp(player: BattlePassPlayer, newlyUnlocked: List<Int>): Boolean {
        if (newlyUnlocked.isEmpty()) return true
        val set = unlockedLevelCache.getOrPut(player.uuid) { mutableSetOf() }
        set.addAll(newlyUnlocked)
        return try {
            sessionFactory.inTransaction { session ->
                val existing = session.find(BattlePassPlayerEntity::class.java, player.uuid.toString())
                if (existing != null) {
                    existing.xp = player.xp
                    session.merge(existing)
                } else {
                    session.persist(BattlePassPlayerEntity.fromModel(player))
                }
                newlyUnlocked.forEach { levelNumber ->
                    session.persist(PlayerUnlockedLevelEntity.fromModel(PlayerUnlockedLevel(player.uuid, levelNumber)))
                }
            }
            true
        } catch (e: Exception) {
            // Roll back the in-memory unlocked set so the live cache matches the
            // failed database write.
            set.removeAll(newlyUnlocked)
            false
        }
    }

    fun loadQuestProgress(uuid: UUID): MutableList<PlayerQuestProgress> {
        return questCache[uuid] ?: run {
            val entities = sessionFactory.fromTransaction { session ->
                session.createQuery(
                    "FROM PlayerQuestProgressEntity WHERE uuid = :uuid",
                    PlayerQuestProgressEntity::class.java
                ).setParameter("uuid", uuid.toString()).resultList
            }
            val quests = entities.map { it.toModel() }.toMutableList()
            questCache[uuid] = quests
            quests
        }
    }

    fun saveQuestProgress(uuid: UUID, progress: PlayerQuestProgress) {
        val list = questCache.getOrPut(uuid) { mutableListOf() }
        val index = list.indexOfFirst { it.questId == progress.questId }
        if (index >= 0) {
            list[index] = progress
        } else {
            list.add(progress)
        }
        sessionFactory.inTransaction { session ->
            val existing = session.find(
                PlayerQuestProgressEntity::class.java,
                "${uuid}-${progress.questId}"
            )
            if (existing != null) {
                existing.current = progress.current
                existing.completed = progress.completed
                session.merge(existing)
            } else {
                session.persist(PlayerQuestProgressEntity.fromModel(progress))
            }
        }
    }

    fun clearDailyQuests(uuid: UUID) {
        questCache.remove(uuid)
        sessionFactory.inTransaction { session ->
            session.createQuery("DELETE FROM PlayerQuestProgressEntity WHERE uuid = :uuid")
                .setParameter("uuid", uuid.toString())
                .executeUpdate()
        }
    }

    fun evictPlayer(uuid: UUID) {
        playerCache.remove(uuid)
        rewardCache.remove(uuid)
        premiumRewardCache.remove(uuid)
        unlockedLevelCache.remove(uuid)
        questCache.remove(uuid)
    }
}

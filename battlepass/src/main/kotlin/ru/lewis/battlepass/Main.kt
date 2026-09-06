package ru.lewis.battlepass

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin
import ru.lewis.battlepass.api.BattlePass
import ru.lewis.battlepass.api.BattlePassImpl
import ru.lewis.battlepass.listener.QuestListener
import ru.lewis.battlepass.listener.ServerLoadedListener
import ru.lewis.battlepass.placeholder.BattlePassPlaceholderExpansion
import ru.lewis.battlepass.service.BattlePassConfigService
import ru.lewis.battlepass.service.BattlePassService
import ru.lewis.battlepass.service.CommandService
import ru.lewis.point.api.PointAPI
import xyz.xenondevs.invui.InvUI

@Singleton
class Main @Inject constructor(
    private val plugin: Plugin,
    private val commandService: CommandService,
    private val serverLoadedListener: ServerLoadedListener,
    private val questListener: QuestListener,
    private val battlePassConfigService: BattlePassConfigService,
    private val battlePassService: BattlePassService,
    private val pointAPI: PointAPI,
    private val placeholderExpansion: BattlePassPlaceholderExpansion,
) {
    fun enable() {
        InvUI.getInstance().setPlugin(plugin)
        battlePassConfigService.load()
        commandService.register()

        registerEntities()
        registerListeners()
        battlePassService.startNotificationTask()
        registerPlaceholderAPI()

        BattlePass.init(BattlePassImpl(battlePassService, battlePassConfigService))
    }

    fun disable() {
        commandService.unregister()
        BattlePass.shutdown()
    }

    private fun registerPlaceholderAPI() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            placeholderExpansion.register()
            plugin.logger.info("PlaceholderAPI integration registered.")
        }
    }

    private fun registerListeners() {
        val pluginManager = plugin.server.pluginManager

        pluginManager.registerEvents(serverLoadedListener, plugin)
        pluginManager.registerEvents(questListener, plugin)
    }

    private fun registerEntities() {
        pointAPI.databaseService.registerEntity(ru.lewis.battlepass.model.BattlePassPlayerEntity::class.java)
        pointAPI.databaseService.registerEntity(ru.lewis.battlepass.model.PlayerRewardEntity::class.java)
        pointAPI.databaseService.registerEntity(ru.lewis.battlepass.model.PlayerUnlockedLevelEntity::class.java)
        pointAPI.databaseService.registerEntity(ru.lewis.battlepass.model.PlayerQuestProgressEntity::class.java)
    }
}

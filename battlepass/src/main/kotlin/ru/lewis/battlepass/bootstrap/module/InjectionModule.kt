package ru.lewis.battlepass.bootstrap.module

import com.google.inject.AbstractModule
import com.google.inject.Provides
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.plugin.Plugin
import ru.lewis.battlepass.commands.BattlePassAdminCommand
import ru.lewis.battlepass.commands.BattlePassCommand
import ru.lewis.battlepass.commands.BattlePassLevelCommand
import ru.lewis.battlepass.commands.BattlePassPremiumCommand
import ru.lewis.battlepass.commands.BattlePassRewardCommand
import ru.lewis.battlepass.listener.QuestListener
import ru.lewis.battlepass.listener.ServerLoadedListener
import ru.lewis.battlepass.menu.BattlePassMenu
import ru.lewis.battlepass.placeholder.BattlePassPlaceholderExpansion
import ru.lewis.battlepass.repository.BattlePassRepository
import ru.lewis.battlepass.service.BattlePassConfigService
import ru.lewis.battlepass.service.BattlePassService
import ru.lewis.battlepass.service.DailyQuestManager
import ru.lewis.point.api.PointAPI

class InjectionModule(
    private val plugin: Plugin
) : AbstractModule() {

    override fun configure() {
        bind(BattlePassRepository::class.java)
        bind(BattlePassConfigService::class.java)
        bind(DailyQuestManager::class.java)
        bind(BattlePassService::class.java)
        bind(BattlePassMenu::class.java)
        bind(BattlePassCommand::class.java)
        bind(BattlePassAdminCommand::class.java)
        bind(BattlePassLevelCommand::class.java)
        bind(BattlePassRewardCommand::class.java)
        bind(BattlePassPremiumCommand::class.java)
        bind(BattlePassPlaceholderExpansion::class.java)
        bind(QuestListener::class.java)
        bind(ServerLoadedListener::class.java)
    }

    @Provides
    fun providePlugin(): Plugin = plugin

    @Provides
    fun provideMiniMessage(): MiniMessage = MiniMessage.miniMessage()

    @Provides
    fun providePointAPI(): PointAPI = PointAPI.get()
}

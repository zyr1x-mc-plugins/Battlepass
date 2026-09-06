package ru.lewis.battlepass.service

import dev.rollczi.litecommands.LiteCommands
import dev.rollczi.litecommands.bukkit.LiteBukkitFactory
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.bukkit.command.CommandSender
import org.bukkit.plugin.Plugin
import ru.lewis.battlepass.commands.BattlePassAdminCommand
import ru.lewis.battlepass.commands.BattlePassCommand
import ru.lewis.battlepass.commands.BattlePassLevelCommand
import ru.lewis.battlepass.commands.BattlePassPremiumCommand
import ru.lewis.battlepass.commands.BattlePassRewardCommand

@Singleton
class CommandService @Inject constructor(
    private val plugin: Plugin,
    private val battlePassCommand: BattlePassCommand,
    private val battlePassAdminCommand: BattlePassAdminCommand,
    private val battlePassLevelCommand: BattlePassLevelCommand,
    private val battlePassRewardCommand: BattlePassRewardCommand,
    private val battlePassPremiumCommand: BattlePassPremiumCommand,
) {
    private lateinit var commands: LiteCommands<CommandSender>

    fun register() {
        commands = LiteBukkitFactory.builder(plugin.name, plugin)
            .commands(battlePassCommand)
            .commands(battlePassAdminCommand)
            .commands(battlePassLevelCommand)
            .commands(battlePassRewardCommand)
            .commands(battlePassPremiumCommand)
            .build()
    }

    fun unregister() {
        if (::commands.isInitialized) {
            commands.unregister()
        }
    }
}

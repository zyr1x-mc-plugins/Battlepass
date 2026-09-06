package ru.lewis.battlepass.commands

import dev.rollczi.litecommands.annotations.command.Command
import dev.rollczi.litecommands.annotations.context.Context
import dev.rollczi.litecommands.annotations.execute.Execute
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import ru.lewis.battlepass.menu.BattlePassMenu

@Singleton
@Command(name = "bp", aliases = ["battlepass"])
class BattlePassCommand @Inject constructor(
    private val battlePassMenu: BattlePassMenu,
) {

    @Execute
    fun execute(@Context sender: CommandSender) {
        when (sender) {
            is Player -> battlePassMenu.openMainMenu(sender)
            else -> sender.sendMessage("§cUse this command in-game.")
        }
    }
}

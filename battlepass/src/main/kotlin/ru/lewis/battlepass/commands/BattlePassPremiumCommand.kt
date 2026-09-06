package ru.lewis.battlepass.commands

import dev.rollczi.litecommands.annotations.argument.Arg
import dev.rollczi.litecommands.annotations.command.Command
import dev.rollczi.litecommands.annotations.context.Context
import dev.rollczi.litecommands.annotations.execute.Execute
import dev.rollczi.litecommands.annotations.permission.Permission
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import ru.lewis.battlepass.service.BattlePassService

@Singleton
@Command(name = "bp")
class BattlePassPremiumCommand @Inject constructor(
    private val battlePassService: BattlePassService,
) {

    @Execute(name = "premium give")
    @Permission("battlepass.admin.premium")
    fun give(
        @Context sender: CommandSender,
        @Arg("player") target: Player,
    ) {
        battlePassService.setPremium(target.uniqueId, true)
        sender.sendMessage("§aGranted Premium BattlePass to ${target.name}.")
        target.sendMessage("§aYou now have Premium BattlePass!")
    }

    @Execute(name = "premium remove")
    @Permission("battlepass.admin.premium")
    fun remove(
        @Context sender: CommandSender,
        @Arg("player") target: Player,
    ) {
        battlePassService.setPremium(target.uniqueId, false)
        sender.sendMessage("§aRemoved Premium BattlePass from ${target.name}.")
        target.sendMessage("§cYour Premium BattlePass has been removed.")
    }

    @Execute(name = "premium check")
    @Permission("battlepass.admin.premium")
    fun check(
        @Context sender: CommandSender,
        @Arg("player") target: Player,
    ) {
        val bpPlayer = battlePassService.getOrCreatePlayer(target.uniqueId)
        val status = if (bpPlayer.isPremium) "§aactive" else "§cinactive"
        sender.sendMessage("§ePremium for ${target.name}: $status")
    }
}

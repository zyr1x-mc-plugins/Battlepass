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
import ru.lewis.battlepass.listener.QuestListener
import ru.lewis.battlepass.service.BattlePassConfigService
import ru.lewis.battlepass.service.BattlePassService

@Singleton
@Command(name = "bp")
class BattlePassAdminCommand @Inject constructor(
    private val battlePassService: BattlePassService,
    private val configService: BattlePassConfigService,
    private val questListener: QuestListener,
) {

    @Execute(name = "admin")
    @Permission("battlepass.admin")
    fun admin(@Context sender: CommandSender) {
        sender.sendMessage("§6=== BattlePass Admin ===")
        sender.sendMessage("§e/bp admin reload §7- Reload config")
        sender.sendMessage("§e/bp admin info <player> §7- Player info")
        sender.sendMessage("§e/bp admin setxp <player> <amount> §7- Set XP")
        sender.sendMessage("§e/bp admin addxp <player> <amount> §7- Add XP")
    }

    @Execute(name = "reload")
    @Permission("battlepass.admin.reload")
    fun reloadRoot(@Context sender: CommandSender) {
        reloadInternal(sender)
    }

    @Execute(name = "admin reload")
    @Permission("battlepass.admin.reload")
    fun reload(@Context sender: CommandSender) {
        reloadInternal(sender)
    }

    private fun reloadInternal(sender: CommandSender) {
        configService.load()
        // QuestListener caches quest configs per Material/EntityType. Without
        // invalidating it here, edits to quests.yml (materials, new quest
        // types, etc.) silently stop matching for players until a full
        // server restart, even though the config itself reloaded fine.
        questListener.invalidateCache()
        sender.sendMessage("§aBattlePass config reloaded.")
    }

    @Execute(name = "admin info")
    @Permission("battlepass.admin.info")
    fun info(
        @Context sender: CommandSender,
        @Arg("player") target: Player,
    ) {
        val bpPlayer = battlePassService.getOrCreatePlayer(target.uniqueId)
        val premium = if (bpPlayer.isPremium) "§aYes" else "§cNo"
        val unclaimed = battlePassService.hasUnclaimedRewards(target.uniqueId)
        val unclaimedStr = if (unclaimed) "§eYes" else "§7None"

        sender.sendMessage("§6=== BattlePass Info: ${target.name} ===")
        sender.sendMessage("§eXP: §f${bpPlayer.xp}")
        sender.sendMessage("§ePremium: $premium")
        sender.sendMessage("§eUnclaimed rewards: $unclaimedStr")
    }

    @Execute(name = "admin setxp")
    @Permission("battlepass.admin.xp")
    fun setXp(
        @Context sender: CommandSender,
        @Arg("player") target: Player,
        @Arg("amount") amount: Long,
    ) {
        val opened = battlePassService.setXp(target, amount)
        sender.sendMessage("§aSet XP for ${target.name} to $amount ($opened level(s) opened).")
    }

    @Execute(name = "admin addxp")
    @Permission("battlepass.admin.xp")
    fun addXp(
        @Context sender: CommandSender,
        @Arg("player") target: Player,
        @Arg("amount") amount: Long,
    ) {
        battlePassService.addXp(target, amount)
        sender.sendMessage("§aAdded $amount XP to ${target.name}.")
    }
}
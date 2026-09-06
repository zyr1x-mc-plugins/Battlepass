package ru.lewis.battlepass.commands

import dev.rollczi.litecommands.annotations.argument.Arg
import dev.rollczi.litecommands.annotations.command.Command
import dev.rollczi.litecommands.annotations.context.Context
import dev.rollczi.litecommands.annotations.execute.Execute
import dev.rollczi.litecommands.annotations.join.Join
import dev.rollczi.litecommands.annotations.permission.Permission
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import ru.lewis.battlepass.config.type.ItemTemplate
import ru.lewis.battlepass.service.BattlePassConfigService
import ru.lewis.battlepass.service.BattlePassService

@Singleton
@Command(name = "bp")
class BattlePassRewardCommand @Inject constructor(
    private val battlePassService: BattlePassService,
    private val configService: BattlePassConfigService,
) {

    // ── Claim (level based) ──────────────────────────────────────────────

    @Execute(name = "reward claim")
    @Permission("battlepass.admin.reward")
    fun claim(
        @Context sender: CommandSender,
        @Arg("player") target: Player,
        @Arg("level") level: Int,
    ) {
        val result = battlePassService.claimReward(target, level)
        if (result) {
            sender.sendMessage("§aClaimed FREE reward of level $level for ${target.name}.")
        } else {
            sender.sendMessage("§cFailed to claim. Check that the level exists, is unlocked by XP and not claimed.")
        }
    }

    @Execute(name = "reward claimpremium")
    @Permission("battlepass.admin.reward")
    fun claimPremium(
        @Context sender: CommandSender,
        @Arg("player") target: Player,
        @Arg("level") level: Int,
    ) {
        val result = battlePassService.claimPremiumReward(target, level)
        if (result) {
            sender.sendMessage("§aClaimed PREMIUM reward of level $level for ${target.name}.")
        } else {
            sender.sendMessage("§cFailed to claim premium reward. Check level/XP/Premium.")
        }
    }

    // ── Editing ──────────────────────────────────────────────────────────

    private fun trackPremium(track: String?): Boolean? = when (track?.lowercase()) {
        "free" -> false
        "premium" -> true
        else -> null
    }

    private fun itemFromHand(player: Player): ItemTemplate? {
        val item = player.inventory.itemInMainHand
        if (item.type == Material.AIR) return null
        return ItemTemplate.fromItemStack(item)
    }

    @Execute(name = "admin reward item")
    @Permission("battlepass.admin.reward")
    fun addItem(
        @Context sender: CommandSender,
        @Arg("level") level: Int,
        @Arg("track") trackArg: String,
    ) {
        if (sender !is Player) {
            sender.sendMessage("§cUse this command in-game with the item in your hand.")
            return
        }
        val premium = trackPremium(trackArg) ?: run {
            sender.sendMessage("§cInvalid track. Use <free|premium>.")
            return
        }
        val template = itemFromHand(sender) ?: run {
            sender.sendMessage("§cHold an item in your hand first.")
            return
        }
        val added = configService.addRewardItem(level, premium, template)
        if (added) {
            val trackName = if (premium) "PREMIUM" else "FREE"
            sender.sendMessage("§aAdded item to Level $level $trackName rewards.")
        } else {
            sender.sendMessage("§cLevel $level does not exist.")
        }
    }

    @Execute(name = "admin reward display")
    @Permission("battlepass.admin.reward")
    fun addDisplay(
        @Context sender: CommandSender,
        @Arg("level") level: Int,
        @Arg("track") trackArg: String,
    ) {
        if (sender !is Player) {
            sender.sendMessage("§cUse this command in-game with the item in your hand.")
            return
        }
        val premium = trackPremium(trackArg) ?: run {
            sender.sendMessage("§cInvalid track. Use <free|premium>.")
            return
        }
        val template = itemFromHand(sender) ?: run {
            sender.sendMessage("§cHold an item in your hand first.")
            return
        }
        val added = configService.addDisplayItem(level, premium, template)
        if (added) {
            val trackName = if (premium) "PREMIUM" else "FREE"
            sender.sendMessage("§aAdded display item to Level $level $trackName (Preview only).")
        } else {
            sender.sendMessage("§cLevel $level does not exist.")
        }
    }

    @Execute(name = "admin reward command")
    @Permission("battlepass.admin.reward")
    fun addCommand(
        @Context sender: CommandSender,
        @Arg("level") level: Int,
        @Arg("track") trackArg: String,
        @Join("command") command: String,
    ) {
        val premium = trackPremium(trackArg) ?: run {
            sender.sendMessage("§cInvalid track. Use <free|premium>.")
            return
        }
        val sanitized = command.trim().removePrefix("/")
        if (sanitized.isEmpty()) {
            sender.sendMessage("§cCommand cannot be empty.")
            return
        }
        val added = configService.addCommand(level, premium, sanitized)
        if (added) {
            val trackName = if (premium) "PREMIUM" else "FREE"
            sender.sendMessage("§aAdded command to Level $level $trackName rewards.")
        } else {
            sender.sendMessage("§cLevel $level does not exist.")
        }
    }

    @Execute(name = "admin reward removeitem")
    @Permission("battlepass.admin.reward")
    fun removeItem(
        @Context sender: CommandSender,
        @Arg("level") level: Int,
        @Arg("track") trackArg: String,
        @Arg("index") index: Int,
    ) {
        val premium = trackPremium(trackArg) ?: run {
            sender.sendMessage("§cInvalid track. Use <free|premium>.")
            return
        }
        val removed = configService.removeRewardItem(level, premium, index)
        if (removed) {
            sender.sendMessage("§aRemoved item #$index from Level $level ${if (premium) "PREMIUM" else "FREE"}.")
        } else {
            sender.sendMessage("§cFailed to remove item. Check level exists and index is valid (1-based).")
        }
    }

    @Execute(name = "admin reward removedisplay")
    @Permission("battlepass.admin.reward")
    fun removeDisplay(
        @Context sender: CommandSender,
        @Arg("level") level: Int,
        @Arg("track") trackArg: String,
        @Arg("index") index: Int,
    ) {
        val premium = trackPremium(trackArg) ?: run {
            sender.sendMessage("§cInvalid track. Use <free|premium>.")
            return
        }
        val removed = configService.removeDisplayItem(level, premium, index)
        if (removed) {
            sender.sendMessage("§aRemoved display item #$index from Level $level ${if (premium) "PREMIUM" else "FREE"}.")
        } else {
            sender.sendMessage("§cFailed to remove display item. Check level exists and index is valid (1-based).")
        }
    }

    @Execute(name = "admin reward removecommand")
    @Permission("battlepass.admin.reward")
    fun removeCommand(
        @Context sender: CommandSender,
        @Arg("level") level: Int,
        @Arg("track") trackArg: String,
        @Arg("index") index: Int,
    ) {
        val premium = trackPremium(trackArg) ?: run {
            sender.sendMessage("§cInvalid track. Use <free|premium>.")
            return
        }
        val removed = configService.removeCommand(level, premium, index)
        if (removed) {
            sender.sendMessage("§aRemoved command #$index from Level $level ${if (premium) "PREMIUM" else "FREE"}.")
        } else {
            sender.sendMessage("§cFailed to remove command. Check level exists and index is valid (1-based).")
        }
    }
}

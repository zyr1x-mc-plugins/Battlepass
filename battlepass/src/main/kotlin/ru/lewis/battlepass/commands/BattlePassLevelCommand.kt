package ru.lewis.battlepass.commands

import dev.rollczi.litecommands.annotations.argument.Arg
import dev.rollczi.litecommands.annotations.command.Command
import dev.rollczi.litecommands.annotations.context.Context
import dev.rollczi.litecommands.annotations.execute.Execute
import dev.rollczi.litecommands.annotations.permission.Permission
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.bukkit.command.CommandSender
import ru.lewis.battlepass.service.BattlePassConfigService

@Singleton
@Command(name = "bp")
class BattlePassLevelCommand @Inject constructor(
    private val configService: BattlePassConfigService,
) {

    @Execute(name = "admin level create")
    @Permission("battlepass.admin.level")
    fun create(
        @Context sender: CommandSender,
        @Arg("level") level: Int,
        @Arg("required-xp") requiredXp: Long,
    ) {
        val created = configService.createLevel(level, requiredXp)
        if (created) {
            sender.sendMessage("§aLevel $level created (required XP: $requiredXp) → levels/level-$level.yml")
        } else {
            sender.sendMessage("§cLevel $level already exists.")
        }
    }

    @Execute(name = "admin level delete")
    @Permission("battlepass.admin.level")
    fun delete(
        @Context sender: CommandSender,
        @Arg("level") level: Int,
    ) {
        val deleted = configService.deleteLevel(level)
        if (deleted) {
            sender.sendMessage("§aLevel $level deleted (levels/level-$level.yml removed).")
        } else {
            sender.sendMessage("§cLevel $level does not exist or could not be deleted.")
        }
    }

    @Execute(name = "admin level setxp")
    @Permission("battlepass.admin.level")
    fun setXp(
        @Context sender: CommandSender,
        @Arg("level") level: Int,
        @Arg("xp") xp: Long,
    ) {
        val updated = configService.setLevelXp(level, xp)
        if (updated != null) {
            sender.sendMessage("§aLevel $level required XP set to $xp.")
        } else {
            sender.sendMessage("§cLevel $level does not exist.")
        }
    }

    @Execute(name = "admin level info")
    @Permission("battlepass.admin.level")
    fun info(
        @Context sender: CommandSender,
        @Arg("level") level: Int,
    ) {
        val levelConfig = configService.reloadLevel(level) ?: configService.levelConfigs[level]
        if (levelConfig == null) {
            sender.sendMessage("§cLevel $level does not exist.")
            return
        }
        val free = levelConfig.free
        val premium = levelConfig.premium
        sender.sendMessage("§6=== BattlePass Level ${levelConfig.level} ===")
        sender.sendMessage("§eRequired XP: §f${levelConfig.requiredXp}")
        sender.sendMessage("§6FREE:")
        sender.sendMessage("  §7Items: §f${free.rewards.items.size}")
        sender.sendMessage("  §7Commands: §f${free.rewards.commands.size}")
        sender.sendMessage("  §7Display items: §f${free.display.size}")
        sender.sendMessage("§6PREMIUM:")
        sender.sendMessage("  §7Items: §f${premium.rewards.items.size}")
        sender.sendMessage("  §7Commands: §f${premium.rewards.commands.size}")
        sender.sendMessage("  §7Display items: §f${premium.display.size}")
    }

    @Execute(name = "admin level list")
    @Permission("battlepass.admin.level")
    fun list(@Context sender: CommandSender) {
        val levels = configService.getSortedLevels()
        if (levels.isEmpty()) {
            sender.sendMessage("§7No levels configured.")
            return
        }
        sender.sendMessage("§6BattlePass Levels:")
        levels.forEach { levelConfig ->
            sender.sendMessage("§7#${levelConfig.level} §8— §e${levelConfig.requiredXp} XP")
        }
    }
}

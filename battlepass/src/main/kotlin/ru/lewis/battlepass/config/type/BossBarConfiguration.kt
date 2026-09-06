package ru.lewis.battlepass.config.type

import net.kyori.adventure.bossbar.BossBar
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import ru.lewis.battlepass.extensions.asMiniMessageComponent

@ConfigSerializable
data class BossBarConfiguration(
    val title: MiniMessageComponent = "<gold>До конца ивента: <time>".asMiniMessageComponent(),
    val color: BossBar.Color = BossBar.Color.YELLOW,
    val overlay: BossBar.Overlay = BossBar.Overlay.PROGRESS
)

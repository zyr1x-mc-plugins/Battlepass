package ru.lewis.battlepass.config.type

import net.kyori.adventure.title.Title
import org.bukkit.entity.Player
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import ru.lewis.battlepass.extensions.inTicks
import java.time.Duration

@ConfigSerializable
data class TitleConfiguration(
    val mainTitle: MiniMessageComponent,
    val subTitle: MiniMessageComponent,
    val fadeIn: Duration = Duration.ofMillis(500),
    val stayIt: Duration = Duration.ofSeconds(3),
    val fadeOut: Duration = Duration.ofMillis(500),
) {
    fun show(player: Player) {
        val fadeInTicks: Int = fadeIn.inTicks.toInt()
        val stayItTicks: Int = stayIt.inTicks.toInt()
        val fadeOutTicks: Int = fadeOut.inTicks.toInt()

        val mainTitleComponent = mainTitle.asComponent()
        val subTitleComponent = subTitle.asComponent()

        val title = Title.title(mainTitleComponent, subTitleComponent, fadeInTicks, stayItTicks, fadeOutTicks)

        player.showTitle(title)
    }
}

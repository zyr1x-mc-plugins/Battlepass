package ru.lewis.battlepass.extensions

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.Bukkit
import ru.lewis.battlepass.config.type.MiniMessageComponent
import ru.lewis.battlepass.config.type.SoundConfiguration
import ru.lewis.battlepass.config.type.TitleConfiguration

fun String.asMiniMessageComponent(): MiniMessageComponent {
    return MiniMessageComponent(this, this.parseMiniMessage())
}

fun Component.asMiniMessageComponent(): MiniMessageComponent {
    return MiniMessageComponent(
        MiniMessage.miniMessage().serialize(this)
            .removePrefix("<!italic><!underlined><!strikethrough><!bold><!obfuscated>"), this
    )
}

fun broadCast(miniMessageComponent: MiniMessageComponent) {
    Bukkit.getOnlinePlayers().forEach { player -> player.sendMessage(miniMessageComponent) }
}

fun broadCastTitle(titleConfiguration: TitleConfiguration) {
    Bukkit.getOnlinePlayers().forEach { player -> titleConfiguration.show(player)}
}

fun broadCastSound(soundConfiguration: SoundConfiguration) {
    Bukkit.getOnlinePlayers().forEach { player -> soundConfiguration.playSound(player) }
}

fun String.parseMiniMessage(vararg tagResolvers: TagResolver): Component =
    MiniMessage.miniMessage().deserialize(this, *tagResolvers)

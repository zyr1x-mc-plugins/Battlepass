package ru.lewis.battlepass.config.type

import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import org.bukkit.entity.Player
import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class SoundConfiguration(
    val sound: String = "minecraft:entity.warden.roar",
    val source: Sound.Source = Sound.Source.PLAYER,
    val volume: Float = 1.0f,
    val pitch: Float = 1.0f
) {
    fun playSound(player: Player) {
        player.playSound(
            Sound.sound(
                Key.key(sound),
                source,
                volume,
                pitch
            )
        )
    }
}

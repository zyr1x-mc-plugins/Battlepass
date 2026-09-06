package ru.lewis.battlepass.config.serializer

import net.kyori.adventure.sound.Sound
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.serialize.TypeSerializer
import java.lang.reflect.Type

class SoundSourceSerializer : TypeSerializer<Sound.Source> {

    override fun deserialize(type: Type, node: ConfigurationNode): Sound.Source? {
        val raw = node.string ?: return null

        return try {
            Sound.Source.valueOf(raw.uppercase())
        } catch (ex: IllegalArgumentException) {
            null
        }
    }

    override fun serialize(type: Type, obj: Sound.Source?, node: ConfigurationNode) {
        node.set(obj?.name)
    }
}

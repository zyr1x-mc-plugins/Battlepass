package ru.lewis.battlepass.config.serializer

import com.google.inject.Inject
import org.bukkit.potion.PotionType
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.kotlin.extensions.set
import org.spongepowered.configurate.serialize.TypeSerializer
import java.lang.reflect.Type

class PotionTypeSerializer @Inject constructor() : TypeSerializer<PotionType> {
    override fun deserialize(type: Type, node: ConfigurationNode): PotionType? {
        return node.string?.let { PotionType.valueOf(it) }
    }

    override fun serialize(type: Type, obj: PotionType?, node: ConfigurationNode) {
        node.set(String::class, obj?.name)
    }
}

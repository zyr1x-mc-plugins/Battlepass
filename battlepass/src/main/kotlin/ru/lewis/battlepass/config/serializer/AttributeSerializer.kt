package ru.lewis.battlepass.config.serializer

import com.google.inject.Inject
import org.bukkit.NamespacedKey
import org.bukkit.Registry
import org.bukkit.attribute.Attribute
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.kotlin.extensions.set
import org.spongepowered.configurate.serialize.TypeSerializer
import java.lang.reflect.Type

class AttributeSerializer @Inject constructor() : TypeSerializer<Attribute> {

    override fun deserialize(type: Type, node: ConfigurationNode): Attribute? {
        val raw = node.string ?: return null
        val key = if (raw.contains(":")) {
            val parts = raw.split(":", limit = 2)
            NamespacedKey(parts[0], parts[1])
        } else {
            NamespacedKey.minecraft(raw.lowercase())
        }
        return Registry.ATTRIBUTE.get(key)
    }

    override fun serialize(type: Type, obj: Attribute?, node: ConfigurationNode) {
        node.set(String::class, obj?.key?.toString())
    }
}

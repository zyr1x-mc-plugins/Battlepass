package ru.lewis.battlepass.config.serializer

import org.bukkit.NamespacedKey
import org.bukkit.Registry
import org.bukkit.enchantments.Enchantment
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.serialize.TypeSerializer
import java.lang.reflect.Type

class EnchantmentSerializer : TypeSerializer<Enchantment> {

    override fun deserialize(type: Type, node: ConfigurationNode): Enchantment? {
        val raw = node.string ?: return null
        val key = if (raw.contains(":")) {
            val parts = raw.split(":", limit = 2)
            NamespacedKey(parts[0], parts[1])
        } else {
            NamespacedKey.minecraft(raw.lowercase())
        }
        return Registry.ENCHANTMENT.get(key)
    }

    override fun serialize(type: Type, obj: Enchantment?, node: ConfigurationNode) {
        node.set(obj?.key?.toString())
    }
}

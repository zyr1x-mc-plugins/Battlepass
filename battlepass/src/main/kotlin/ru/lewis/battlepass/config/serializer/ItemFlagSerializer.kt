package ru.lewis.battlepass.config.serializer

import com.google.inject.Inject
import org.bukkit.inventory.ItemFlag
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.kotlin.extensions.set
import org.spongepowered.configurate.serialize.TypeSerializer
import java.lang.reflect.Type

class ItemFlagSerializer @Inject constructor() : TypeSerializer<ItemFlag> {
    override fun deserialize(type: Type, node: ConfigurationNode): ItemFlag? {
        return node.string?.let { ItemFlag.valueOf(it) }
    }

    override fun serialize(type: Type, obj: ItemFlag?, node: ConfigurationNode) {
        node.set(String::class, obj?.name)
    }
}

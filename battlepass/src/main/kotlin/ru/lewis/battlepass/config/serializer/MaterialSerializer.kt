package ru.lewis.battlepass.config.serializer

import com.google.inject.Inject
import org.bukkit.Material
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.kotlin.extensions.set
import org.spongepowered.configurate.serialize.TypeSerializer
import java.lang.reflect.Type

class MaterialSerializer @Inject constructor() : TypeSerializer<Material> {

    override fun deserialize(type: Type, node: ConfigurationNode): Material? {
        return node.string?.let { Material.getMaterial(it) }
    }

    override fun serialize(type: Type, obj: Material?, node: ConfigurationNode) {
        node.set(String::class, obj?.name)
    }
}

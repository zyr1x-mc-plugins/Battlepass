package ru.lewis.battlepass.config.serializer

import com.google.inject.Inject
import org.bukkit.entity.EntityType
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.kotlin.extensions.set
import org.spongepowered.configurate.serialize.TypeSerializer
import java.lang.reflect.Type

class EntityTypeSerializer @Inject constructor() : TypeSerializer<EntityType> {

    override fun deserialize(type: Type, node: ConfigurationNode): EntityType? {
        return node.string?.let { EntityType.valueOf(it.uppercase()) }
    }

    override fun serialize(type: Type, obj: EntityType?, node: ConfigurationNode) {
        node.set(String::class, obj?.name)
    }
}
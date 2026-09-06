package ru.lewis.battlepass.config.serializer

import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.serialize.TypeSerializer
import java.awt.Color
import java.lang.reflect.Type

class ColorSerializer : TypeSerializer<Color> {

    override fun deserialize(type: Type, node: ConfigurationNode): Color? {
        val red = node.node("red").getInt(Int.MAX_VALUE).takeIf { it != Int.MAX_VALUE } ?: return null
        val green = node.node("green").getInt(Int.MAX_VALUE).takeIf { it != Int.MAX_VALUE } ?: return null
        val blue = node.node("blue").getInt(Int.MAX_VALUE).takeIf { it != Int.MAX_VALUE } ?: return null

        return Color(red, green, blue)
    }

    override fun serialize(type: Type, obj: Color?, node: ConfigurationNode) {
        obj?.also {
            node.node("red").set(it.red)
            node.node("green").set(it.green)
            node.node("blue").set(it.blue)
        }
    }
}

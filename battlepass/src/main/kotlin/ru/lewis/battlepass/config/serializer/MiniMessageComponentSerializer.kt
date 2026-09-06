package ru.lewis.battlepass.config.serializer

import com.google.inject.Inject
import net.kyori.adventure.text.minimessage.MiniMessage
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.kotlin.extensions.set
import org.spongepowered.configurate.serialize.TypeSerializer
import ru.lewis.battlepass.config.type.MiniMessageComponent
import java.lang.reflect.Type

class MiniMessageComponentSerializer @Inject constructor(
    private val miniMessage: MiniMessage
) : TypeSerializer<MiniMessageComponent> {

    override fun deserialize(type: Type, node: ConfigurationNode): MiniMessageComponent? {
        return node.string?.let { MiniMessageComponent(it, miniMessage.deserialize(it)) }
    }

    override fun serialize(type: Type, obj: MiniMessageComponent?, node: ConfigurationNode) {
        node.set(String::class, obj?.rawString)
    }
}

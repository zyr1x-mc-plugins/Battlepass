package ru.lewis.battlepass.config.serializer

import net.kyori.adventure.bossbar.BossBar
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.serialize.TypeSerializer
import java.lang.reflect.Type

class BossBarColorSerializer : TypeSerializer<BossBar.Color> {

    override fun deserialize(
        type: Type,
        node: ConfigurationNode
    ): BossBar.Color? {
        val value = node.string ?: return null

        return runCatching {
            BossBar.Color.valueOf(value.uppercase())
        }.getOrNull()
    }

    override fun serialize(
        type: Type,
        obj: BossBar.Color?,
        node: ConfigurationNode
    ) {
        node.set(obj?.name)
    }
}

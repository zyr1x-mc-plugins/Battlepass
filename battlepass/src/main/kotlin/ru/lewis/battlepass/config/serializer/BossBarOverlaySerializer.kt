package ru.lewis.battlepass.config.serializer

import net.kyori.adventure.bossbar.BossBar
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.serialize.TypeSerializer
import java.lang.reflect.Type

class BossBarOverlaySerializer : TypeSerializer<BossBar.Overlay> {

    override fun deserialize(
        type: Type,
        node: ConfigurationNode
    ): BossBar.Overlay? {
        val value = node.string ?: return null

        return runCatching {
            BossBar.Overlay.valueOf(value.uppercase())
        }.getOrNull()
    }

    override fun serialize(
        type: Type,
        obj: BossBar.Overlay?,
        node: ConfigurationNode
    ) {
        node.set(obj?.name)
    }
}

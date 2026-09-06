package ru.lewis.battlepass.config.serializer

import com.google.inject.Inject
import net.kyori.adventure.util.Ticks
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.kotlin.extensions.get
import org.spongepowered.configurate.serialize.TypeSerializer
import java.lang.reflect.Type
import java.time.Duration

class PotionEffectSerializer @Inject constructor() : TypeSerializer<PotionEffect> {

    override fun deserialize(type: Type, node: ConfigurationNode): PotionEffect? {
        val duration = node.node("duration").get(Duration::class) ?: return null
        val amplifier = node.node("amplifier").getInt(1)
        val ambient = node.node("ambient").getBoolean(true)
        val particles = node.node("particles").getBoolean(true)
        val icon = node.node("icon").getBoolean(true)

        val ticks = (duration.toMillis() / Ticks.SINGLE_TICK_DURATION_MS).toInt()

        return PotionEffect(
            node.node("type").get(PotionEffectType::class)!!,
            ticks,
            amplifier,
            ambient,
            particles,
            icon
        )
    }

    override fun serialize(type: Type, obj: PotionEffect?, node: ConfigurationNode) {
        obj?.let {
            node.node("type").set(it.type.name)
            node.node("duration").set(Ticks.duration(it.duration.toLong()))
            node.node("amplifier").set(it.amplifier)
            node.node("ambient").set(it.isAmbient)
            node.node("particles").set(it.hasParticles())
            node.node("icon").set(it.hasIcon())
        } ?: node.raw(null)
    }
}

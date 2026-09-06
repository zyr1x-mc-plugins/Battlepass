package ru.lewis.battlepass.config.serializer

import com.google.inject.Inject
import org.bukkit.NamespacedKey
import org.bukkit.Registry
import org.bukkit.potion.PotionEffectType
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.serialize.TypeSerializer
import java.lang.reflect.Type

class PotionEffectTypeSerializer @Inject constructor() : TypeSerializer<PotionEffectType> {

    private val legacyMapping = mapOf(
        "INCREASE_DAMAGE" to "strength",
        "HARM" to "instant_damage",
        "HEAL" to "instant_health",
        "JUMP" to "jump_boost",
        "SLOW" to "slowness",
        "SPEED" to "speed",
        "FAST_DIGGING" to "haste",
        "SLOW_DIGGING" to "mining_fatigue",
        "DAMAGE_RESISTANCE" to "resistance",
        "CONFUSION" to "nausea",
        "WATER_BREATHING" to "water_breathing",
        "NIGHT_VISION" to "night_vision",
        "FIRE_RESISTANCE" to "fire_resistance",
        "INVISIBILITY" to "invisibility",
        "REGENERATION" to "regeneration",
        "WEAKNESS" to "weakness",
        "POISON" to "poison",
        "WITHER" to "wither",
        "ABSORPTION" to "absorption",
        "SATURATION" to "saturation",
        "HEALTH_BOOST" to "health_boost",
        "BLINDNESS" to "blindness",
        "HUNGER" to "hunger",
        "LEVITATION" to "levitation",
        "LUCK" to "luck",
        "UNLUCK" to "unluck",
        "SLOW_FALLING" to "slow_falling",
        "CONDUIT_POWER" to "conduit_power",
        "DOLPHINS_GRACE" to "dolphins_grace",
        "BAD_OMEN" to "bad_omen",
        "HERO_OF_THE_VILLAGE" to "hero_of_the_village",
        "DARKNESS" to "darkness",
        "GLOWING" to "glowing"
    )

    override fun deserialize(type: Type, node: ConfigurationNode): PotionEffectType? {
        val raw = node.string ?: return null

        val resolved = legacyMapping[raw.uppercase()] ?: raw

        val key = if (resolved.contains(":")) {
            val parts = resolved.split(":", limit = 2)
            NamespacedKey(parts[0], parts[1])
        } else {
            NamespacedKey.minecraft(resolved.lowercase())
        }

        return Registry.EFFECT.get(key)
            ?: throw IllegalArgumentException("Unknown potion effect type: $raw")
    }

    override fun serialize(type: Type, obj: PotionEffectType?, node: ConfigurationNode) {
        node.set(obj?.key?.toString())
    }
}

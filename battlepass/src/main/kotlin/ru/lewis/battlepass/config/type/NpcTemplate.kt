package ru.lewis.battlepass.config.type

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class NpcTemplate(
    val skin: String = "",
    val displayName: String = "",
    val deviation: Deviation = Deviation(5.0, 1.0, 5.0),
) {
    @ConfigSerializable
    data class Deviation(
        val x: Double,
        val y: Double,
        val z: Double,
    )
}

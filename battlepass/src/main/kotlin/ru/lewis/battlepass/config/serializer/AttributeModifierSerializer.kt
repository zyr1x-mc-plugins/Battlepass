package ru.lewis.battlepass.config.serializer

import org.bukkit.NamespacedKey
import org.bukkit.attribute.AttributeModifier
import org.bukkit.attribute.AttributeModifier.Operation
import org.bukkit.inventory.EquipmentSlotGroup
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.kotlin.extensions.get
import org.spongepowered.configurate.serialize.TypeSerializer
import java.lang.reflect.Type

class AttributeModifierSerializer : TypeSerializer<AttributeModifier> {

    override fun deserialize(type: Type, node: ConfigurationNode): AttributeModifier? {
        val keyStr = node.node("key").string ?: node.node("uuid").string ?: return null
        val key = if (keyStr.contains(":")) {
            val parts = keyStr.split(":", limit = 2)
            NamespacedKey(parts[0], parts[1])
        } else {
            NamespacedKey.minecraft(keyStr.lowercase().replace("-", "_").replace(" ", "_"))
        }
        val operation = node.node("operation").get(Operation::class) ?: return null
        val amount = node.node("amount").getDouble(Double.NaN).takeUnless { it.isNaN() } ?: return null
        val slotGroupStr = node.node("slot").string
        val slotGroup = slotGroupStr?.let { name ->
            when (name.lowercase()) {
                "any" -> EquipmentSlotGroup.ANY
                "mainhand" -> EquipmentSlotGroup.MAINHAND
                "offhand" -> EquipmentSlotGroup.OFFHAND
                "feet" -> EquipmentSlotGroup.FEET
                "legs" -> EquipmentSlotGroup.LEGS
                "chest" -> EquipmentSlotGroup.CHEST
                "head" -> EquipmentSlotGroup.HEAD
                "armor" -> EquipmentSlotGroup.ARMOR
                "hand" -> EquipmentSlotGroup.HAND
                "body" -> EquipmentSlotGroup.BODY
                else -> EquipmentSlotGroup.ANY
            }
        } ?: EquipmentSlotGroup.ANY

        return AttributeModifier(key, amount, operation, slotGroup)
    }

    override fun serialize(type: Type, obj: AttributeModifier?, node: ConfigurationNode) {
        if (obj == null) {
            node.raw(null)
            return
        }

        node.node("key").set(obj.key.toString())
        node.node("operation").set(obj.operation)
        node.node("amount").set(obj.amount)
        node.node("slot").set(obj.slotGroup.toString())
    }
}

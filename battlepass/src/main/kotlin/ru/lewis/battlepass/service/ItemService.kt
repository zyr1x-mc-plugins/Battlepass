package ru.lewis.battlepass.service

import jakarta.inject.Singleton
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

@Singleton
object ItemService {

    private const val NAMESPACE = "lewis"

    const val DISABLE_PLACE = "disable-place"

    private val disablePlaceKey = NamespacedKey(NAMESPACE, DISABLE_PLACE)

    fun disablePlace(itemStack: ItemStack) {
        setCustomString(itemStack, DISABLE_PLACE, "true")
    }

    fun isDisablePlace(itemStack: ItemStack): Boolean {
        return getCustomString(itemStack, DISABLE_PLACE)
            ?.equals("true", ignoreCase = true) == true
    }

    fun getCustomString(itemStack: ItemStack, key: String): String? {
        val meta = itemStack.itemMeta ?: return null

        return meta.persistentDataContainer.get(
            NamespacedKey(NAMESPACE, key),
            PersistentDataType.STRING
        )
    }

    fun setCustomString(
        itemStack: ItemStack,
        key: String,
        value: String
    ) {
        val meta = itemStack.itemMeta ?: return

        meta.persistentDataContainer.set(
            NamespacedKey(NAMESPACE, key.lowercase()),
            PersistentDataType.STRING,
            value
        )

        itemStack.itemMeta = meta
    }

    fun applyPersistentDataContainer(
        source: Map<String, String>,
        itemStack: ItemStack
    ) {
        val meta = itemStack.itemMeta ?: return

        source.forEach { (key, value) ->
            meta.persistentDataContainer.set(
                NamespacedKey(NAMESPACE, key.lowercase()),
                PersistentDataType.STRING,
                value
            )
        }

        itemStack.itemMeta = meta
    }
}

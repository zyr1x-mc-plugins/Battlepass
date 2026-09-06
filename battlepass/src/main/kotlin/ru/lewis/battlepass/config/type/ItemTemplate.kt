package ru.lewis.battlepass.config.type

import com.destroystokyo.paper.profile.ProfileProperty
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.EnchantmentStorageMeta
import org.bukkit.inventory.meta.LeatherArmorMeta
import org.bukkit.inventory.meta.PotionMeta
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.persistence.PersistentDataType
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionType
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import ru.lewis.battlepass.extensions.asMiniMessageComponent
import ru.lewis.battlepass.service.ItemService
import xyz.xenondevs.inventoryaccess.component.AdventureComponentWrapper
import xyz.xenondevs.invui.item.builder.AbstractItemBuilder
import xyz.xenondevs.invui.item.builder.ItemBuilder
import xyz.xenondevs.invui.item.builder.PotionBuilder
import java.awt.Color
import java.util.UUID

@ConfigSerializable
data class ItemTemplate(
    val type: Material = Material.STONE,
    val amount: Int? = null,
    val displayName: MiniMessageComponent? = null,
    val lore: List<MiniMessageComponent>? = null,
    val flags: Set<ItemFlag>? = null,
    val enchantments: Map<Enchantment, Int>? = null,
    val unbreakable: Boolean? = null,
    val potion: PotionTemplate? = null,
    val skullTexture: String? = null,
    val attributes: List<AttributeConfiguration>? = null,
    val customModelData: Int? = null,
    val persistentDataContainer: Map<String, String>? = null,
    val color: Color? = null,
) {
    @Transient
    private var cachedItem: ItemStack? = null

    @ConfigSerializable
    data class PotionTemplate(
        val type: PotionType,
        val color: Color?,
        val effects: List<PotionEffect>?
    )

    fun resolve(vararg tagResolvers: TagResolver): ItemTemplate {
        return copy(
            displayName = displayName?.resolve(*tagResolvers),
            lore = lore?.map { line ->
                (line as MiniMessageComponent?)?.resolve(*tagResolvers) ?: Component.empty().asMiniMessageComponent()
            }
        )
    }

    fun resolveLore(additionalLore: List<MiniMessageComponent>, vararg tagResolvers: TagResolver): ItemTemplate {
        val resolvedAdditionalLore = additionalLore.map { it.resolve(*tagResolvers) }
        val resolvedOriginalLore = (lore ?: emptyList()).map { it.resolve(*tagResolvers) }
        return copy(
            displayName = displayName?.resolve(*tagResolvers),
            lore = resolvedOriginalLore + resolvedAdditionalLore
        )
    }

    fun toCleanItem(): ItemStack {
        val item = ItemStack(type, amount ?: 1)
        val meta = item.itemMeta ?: return item

        displayName?.let { meta.displayName(it.asComponent()) }

        lore?.let { loreList ->
            meta.lore(loreList.map { it.asComponent() })
        }

        enchantments?.forEach { (enchantment, level) ->
            if (meta is EnchantmentStorageMeta) {
                meta.addStoredEnchant(enchantment, level, true)
            } else {
                meta.addEnchant(enchantment, level, true)
            }
        }

        flags?.forEach { meta.addItemFlags(it) }

        if (unbreakable == true) {
            meta.isUnbreakable = true
        }

        if (potion != null && meta is PotionMeta) {
            meta.basePotionType = potion.type
            potion.color?.let {
                meta.color = org.bukkit.Color.fromRGB(it.red, it.green, it.blue)
            }
            potion.effects?.forEach { effect ->
                meta.addCustomEffect(effect, true)
            }
        }

        customModelData?.let { meta.setCustomModelData(it) }

        attributes?.forEach {
            meta.addAttributeModifier(it.attribute, it.modifier)
        }

        if (color != null && meta is LeatherArmorMeta) {
            meta.setColor(org.bukkit.Color.fromRGB(color.red, color.green, color.blue))
        }

        item.itemMeta = meta

        if (skullTexture != null) {
            setSkullTexture(item, skullTexture)
        }

        return item
    }

    fun toItem(): ItemStack {
        cachedItem?.let { return it.clone() }
        val builder = createBuilder()

        builder.amount = amount ?: 1

        displayName?.also { builder.setDisplayName(AdventureComponentWrapper(it.asComponent())) }

        if (lore != null) {
            builder.setLore(lore.map { AdventureComponentWrapper(it.asComponent()) })
        }

        if (flags != null) {
            builder.setItemFlags(flags.toMutableList())
        }

        enchantments?.forEach { (enchantment, level) ->
            builder.addEnchantment(enchantment, level, true)
        }

        builder.setUnbreakable(unbreakable ?: false)

        if (potion != null && builder is PotionBuilder) {
            potion.color?.let { builder.setColor(it) }
            potion.effects?.forEach { builder.addEffect(it) }
        }

        customModelData?.let { builder.setCustomModelData(it) }

        val item = builder.get()

        if (potion != null) {
            val meta = item.itemMeta
            if (meta is PotionMeta) {
                meta.basePotionType = potion.type
                item.itemMeta = meta
            }
        }
        if (skullTexture != null) {
            setSkullTexture(item, skullTexture)
        }

        if (attributes != null) {
            item.itemMeta = item.itemMeta.apply {
                attributes.forEach {
                    item.itemMeta.addAttributeModifier(it.attribute, it.modifier)
                }
            }
        }

        if (persistentDataContainer != null) {
            ItemService.applyPersistentDataContainer(persistentDataContainer, item)
            println(persistentDataContainer)
        }

        if (color != null) {
            val meta = item.itemMeta
            if (meta is LeatherArmorMeta) {
                meta.setColor(org.bukkit.Color.fromRGB(color.red, color.green, color.blue))
                item.itemMeta = meta
            }
        }

        cachedItem = item
        return item.clone()
    }

    private fun createBuilder(): AbstractItemBuilder<*> {
        return when (type) {
            Material.POTION -> PotionBuilder(PotionBuilder.PotionType.NORMAL)
            Material.SPLASH_POTION -> PotionBuilder(PotionBuilder.PotionType.SPLASH)
            Material.LINGERING_POTION -> PotionBuilder(PotionBuilder.PotionType.LINGERING)
            Material.TIPPED_ARROW -> PotionBuilder(ItemStack(Material.TIPPED_ARROW))
            else -> ItemBuilder(type)
        }
    }

    private fun setSkullTexture(itemStack: ItemStack, skullTexture: String?) {
        if (skullTexture == null) return
        val itemMeta = itemStack.itemMeta

        if (itemMeta is SkullMeta) {
            val profile = Bukkit.createProfile(UUID.randomUUID())
            profile.setProperty(ProfileProperty("textures", skullTexture))
            itemMeta.playerProfile = profile
        }
        itemStack.setItemMeta(itemMeta)
    }

    companion object {

        fun fromItemStack(item: ItemStack): ItemTemplate {
            val meta = item.itemMeta
                ?: return ItemTemplate(
                    type = item.type,
                    amount = item.amount.takeIf { it != 1 }
                )

            val displayName = if (meta.hasDisplayName()) {
                meta.displayName()?.asMiniMessageComponent()
            } else null

            val lore = if (meta.hasLore()) {
                meta.lore()?.map { it.asMiniMessageComponent() }
            } else null

            val flags = meta.itemFlags.takeIf { it.isNotEmpty() }

            val enchantments = when {
                meta is EnchantmentStorageMeta && meta.hasStoredEnchants() ->
                    meta.storedEnchants.takeIf { it.isNotEmpty() }

                meta.hasEnchants() ->
                    meta.enchants.takeIf { it.isNotEmpty() }

                else -> null
            }

            val unbreakable = meta.isUnbreakable.takeIf { it }

            val potion = if (meta is PotionMeta) {
                PotionTemplate(
                    type = meta.basePotionType ?: PotionType.WATER,
                    color = meta.color?.let { Color(it.red, it.green, it.blue) },
                    effects = meta.customEffects.takeIf { it.isNotEmpty() }
                )
            } else null

            val skullTexture = if (meta is SkullMeta) {
                meta.playerProfile?.properties
                    ?.firstOrNull { it.name == "textures" }
                    ?.value
            } else null

            val attributes = if (meta.hasAttributeModifiers()) {
                meta.attributeModifiers?.entries()
                    ?.map { entry -> AttributeConfiguration(entry.key, entry.value) }
                    ?.takeIf { it.isNotEmpty() }
            } else null

            val customModelData = if (meta.hasCustomModelData()) {
                meta.customModelData
            } else null

            val persistentDataContainer = meta.persistentDataContainer.keys
                .associate { key ->
                    val value = meta.persistentDataContainer.get(key, PersistentDataType.STRING)
                    key.key to (value ?: "")
                }
                .takeIf { it.isNotEmpty() }

            val color = if (meta is LeatherArmorMeta) {
                Color(meta.color.red, meta.color.green, meta.color.blue)
            } else null

            return ItemTemplate(
                type = item.type,
                amount = item.amount.takeIf { it != 1 },
                displayName = displayName,
                lore = lore,
                flags = flags,
                enchantments = enchantments,
                unbreakable = unbreakable,
                potion = potion,
                skullTexture = skullTexture,
                attributes = attributes,
                customModelData = customModelData,
                persistentDataContainer = persistentDataContainer,
                color = color,
            )
        }
    }
}
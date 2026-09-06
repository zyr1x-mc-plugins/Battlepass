package ru.lewis.battlepass.config.type

import org.bukkit.entity.Player
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import ru.lewis.battlepass.extensions.asMiniMessageComponent
import xyz.xenondevs.inventoryaccess.component.AdventureComponentWrapper
import xyz.xenondevs.invui.gui.Gui
import xyz.xenondevs.invui.gui.PagedGui
import xyz.xenondevs.invui.gui.structure.Markers
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.item.ItemWrapper
import xyz.xenondevs.invui.window.Window
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@ConfigSerializable
open class MenuConfig(
    val title: MiniMessageComponent = "".asMiniMessageComponent(),
    val structure: List<String> = listOf(),
    val customItems: Map<Char, ItemTemplate> = mapOf(),
    val templates: Map<Char, ItemTemplate> = mapOf(),
)

@OptIn(ExperimentalContracts::class)
inline fun Window.Builder.Normal.Single.import(
    player: Player,
    config: MenuConfig,
    guiModifier: PagedGui.Builder<Item>.() -> Unit
): PagedGui<Item> {
    contract {
        callsInPlace(guiModifier, InvocationKind.EXACTLY_ONCE)
    }

    val gui = config.createPagedGui(player).apply(guiModifier).build()
    import(config, gui)
    return gui
}

@OptIn(ExperimentalContracts::class)
inline fun Window.Builder.Normal.Single.importNormal(
    config: MenuConfig,
    guiModifier: Gui.Builder.Normal.() -> Unit
): Gui {
    contract {
        callsInPlace(guiModifier, InvocationKind.EXACTLY_ONCE)
    }

    val gui = config.createGui().apply(guiModifier).build()
    import(config, gui)
    return gui
}

fun Window.Builder.Normal.Single.import(config: MenuConfig, gui: Gui) {
    setTitle(AdventureComponentWrapper(config.title.asComponent()))
    setGui(gui)
}

inline fun MenuConfig.createPagedGui(player: Player): PagedGui.Builder<Item> = PagedGui.items().apply {
    setStructure(*this@createPagedGui.structure.map { it.replace(" ", "") }.toTypedArray())
    addIngredient('.', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
    this@createPagedGui.customItems.forEach { (key, item) -> addIngredient(key, ItemWrapper(item.toItem())) }
    this@createPagedGui.templates.forEach { (key, item) -> addIngredient(key, ItemWrapper(item.toItem())) }
}

// Normal (non-paged) InvUI Gui built straight from the same MenuConfig. It
// keeps the shared structure/customItems/templates handling of the PagedGui
// variant, but does NOT register any content-list marker — all positions are
// fixed and individual slots are filled by the caller via Gui.setItem(...).
fun MenuConfig.createGui(): Gui.Builder.Normal = Gui.normal().apply {
    setStructure(*this@createGui.structure.map { it.replace(" ", "") }.toTypedArray())
    this@createGui.customItems.forEach { (key, item) -> addIngredient(key, ItemWrapper(item.toItem())) }
    this@createGui.templates.forEach { (key, item) -> addIngredient(key, ItemWrapper(item.toItem())) }
}

// Slot indices (row-major, space-stripped — exactly how InvUI maps the
// structure into the GUI) where the given layout char occurs. This is the
// single source of layout: a structure tells us where FREE ('F') and PREMIUM
// ('P') rewards live, and the number of 'F' positions defines the page size.
fun MenuConfig.slotIndices(ch: Char): List<Int> =
    structure.joinToString("").filter { it != ' ' }
        .mapIndexedNotNull { index, c -> if (c == ch) index else null }

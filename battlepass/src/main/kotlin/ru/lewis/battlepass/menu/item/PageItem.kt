package ru.lewis.battlepass.menu.item

import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.jetbrains.annotations.NotNull
import ru.lewis.battlepass.config.type.ItemTemplate
import xyz.xenondevs.invui.gui.PagedGui
import xyz.xenondevs.invui.item.ItemProvider
import xyz.xenondevs.invui.item.impl.controlitem.ControlItem

class PageItem(
    private val template: ItemTemplate,
    private val forward: Boolean,
) : ControlItem<PagedGui<*>>() {

    override fun getItemProvider(gui: PagedGui<*>): ItemProvider {
        return ItemProvider { template.toItem() }
    }

    override fun handleClick(@NotNull clickType: ClickType, @NotNull player: Player, @NotNull event: InventoryClickEvent) {
        if (clickType == ClickType.LEFT) {
            if (forward) gui.goForward() else gui.goBack()
        }
    }
}

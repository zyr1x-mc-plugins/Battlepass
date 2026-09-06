package ru.lewis.battlepass.menu.item

import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import ru.lewis.battlepass.config.type.ItemTemplate
import xyz.xenondevs.invui.gui.Gui
import xyz.xenondevs.invui.item.ItemProvider
import xyz.xenondevs.invui.item.impl.controlitem.ControlItem

class BackItem(
    private val template: ItemTemplate,
    private val onClick: (Player) -> Unit,
) : ControlItem<Gui>() {

    override fun getItemProvider(gui: Gui): ItemProvider {
        return ItemProvider { template.toItem() }
    }

    override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
        if (clickType == ClickType.LEFT) {
            onClick(player)
        }
    }
}

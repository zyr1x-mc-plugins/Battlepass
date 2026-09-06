package ru.lewis.battlepass.menu

import jakarta.inject.Inject
import jakarta.inject.Singleton
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import ru.lewis.battlepass.config.type.ItemTemplate
import ru.lewis.battlepass.config.type.MiniMessageComponent
import ru.lewis.battlepass.config.type.battlepass.BattlePassLevelConfig
import ru.lewis.battlepass.config.type.battlepass.BattlePassQuestConfig
import ru.lewis.battlepass.config.type.battlepass.LevelUnlockState
import ru.lewis.battlepass.config.type.battlepass.RewardClaimState
import ru.lewis.battlepass.config.type.import
import ru.lewis.battlepass.config.type.importNormal
import ru.lewis.battlepass.config.type.slotIndices
import ru.lewis.battlepass.extensions.asMiniMessageComponent
import ru.lewis.battlepass.menu.item.BackItem
import ru.lewis.battlepass.service.BattlePassConfigService
import ru.lewis.battlepass.service.BattlePassService
import ru.lewis.battlepass.service.DailyQuestManager
import xyz.xenondevs.inventoryaccess.component.AdventureComponentWrapper
import xyz.xenondevs.invui.gui.Gui
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.item.ItemProvider
import xyz.xenondevs.invui.item.builder.ItemBuilder
import xyz.xenondevs.invui.item.impl.AbstractItem
import xyz.xenondevs.invui.item.impl.SimpleItem
import xyz.xenondevs.invui.item.impl.controlitem.ControlItem
import xyz.xenondevs.invui.window.Window

@Singleton
class BattlePassMenu @Inject constructor(
    private val battlePassService: BattlePassService,
    private val configService: BattlePassConfigService,
    private val dailyQuestManager: DailyQuestManager,
) {

    fun openMainMenu(player: Player) {
        val menuConfig = configService.config.gui.mainMenu

        Window.single().apply {
            import(player, menuConfig) {
                addIngredient('R', RewardsItem(menuConfig.templates['R']!!))
                addIngredient('Q', QuestsItem(menuConfig.templates['Q']!!))
                addIngredient('P', PremiumItem(player, menuConfig.templates['P']!!))
            }
            open(player)
        }
    }

    fun openRewardsMenu(player: Player) {
        val menuConfig = configService.config.gui.rewardsMenu

        // A level opens automatically as soon as the player's XP covers its
        // cost (spending that XP), strictly in order. Run the sync before
        // rendering so every affordable reward is immediately claimable.
        battlePassService.autoUnlock(player)

        // F/P layout and page size come from the MenuConfig structure itself —
        // the structure is the single source of layout. No manual slot math and
        // no hardcoded 7/14: a row of N 'F' (and matching N 'P') defines the
        // page size automatically. FREE and PREMIUM rows stay strictly separate
        // containers, never mixed into a single content list.
        val freeSlots = menuConfig.slotIndices('F')
        val premiumSlots = menuConfig.slotIndices('P')
        require(freeSlots.size == premiumSlots.size) {
            "Rewards menu must declare the same number of 'F' and 'P' positions, " +
                "got ${freeSlots.size} F and ${premiumSlots.size} P"
        }
        val pageSize = freeSlots.size

        // Dynamic level count from the actually loaded level configs — never a
        // hardcoded number of levels.
        val sortedLevels = battlePassService.sortedLevels
        val totalLevels = sortedLevels.size
        val pageCount = if (pageSize == 0) 1 else (totalLevels + pageSize - 1) / pageSize

        // One dynamic item per level per track. FREE and PREMIUM are distinct
        // lists, so a FREE reward can never land in a PREMIUM slot and vice
        // versa; FREE/PREMIUM of the same index are vertically stacked because
        // the F and P rows share column positions.
        val freeItems = sortedLevels.map { RewardItem(player, it, RewardType.FREE, this@BattlePassMenu) }
        val premiumItems = sortedLevels.map { RewardItem(player, it, RewardType.PREMIUM, this@BattlePassMenu) }
        val emptySlot = SimpleItem(ItemStack(Material.AIR))

        // ONE shared pagination for the whole 14-slot grid (FREE + PREMIUM).
        val controller = RewardPageController(pageCount)

        lateinit var rewardsGui: Gui

        // Re-renders the visible FREE/PREMIUM slots for the current page and
        // blanks out reward positions that have no level on the last page.
        fun renderPage() {
            val gui = rewardsGui
            val from = controller.page * pageSize

            freeSlots.forEachIndexed { i, slot ->
                gui.setItem(slot, freeItems.getOrNull(from + i) ?: emptySlot)
            }
            premiumSlots.forEachIndexed { i, slot ->
                gui.setItem(slot, premiumItems.getOrNull(from + i) ?: emptySlot)
            }
        }

        Window.single().apply {
            val gui = importNormal(menuConfig) {
                addIngredient('<', RewardNavItem(menuConfig.templates['<']!!, forward = false, controller) { renderPage() })
                addIngredient('>', RewardNavItem(menuConfig.templates['>']!!, forward = true, controller) { renderPage() })
                addIngredient('B', BackItem(menuConfig.templates['B']!!) { openMainMenu(it) })
            }
            rewardsGui = gui
            renderPage()
            open(player)
        }
    }

    fun openRewardPreview(player: Player, level: BattlePassLevelConfig, isPremium: Boolean) {
        val menuConfig = configService.config.gui.rewardViewMenu

        // Each track (FREE/PREMIUM) has its own dedicated display list per
        // level. If an admin hasn't configured explicit display items for this
        // specific level+track yet, fall back to that same track's real
        // issuance items so Preview is never blank — this fallback is
        // visual-only and never touches real reward issuance, and FREE/PREMIUM
        // are still never mixed together.
        val track = if (isPremium) level.premium else level.free
        val previewItems = track.display.ifEmpty { track.rewards.items }

        Window.single().apply {
            val gui = import(player, menuConfig) {
                if (previewItems.isNotEmpty()) {
                    setContent(previewItems.map { SimpleItem(it.toItem()) })
                }
                addIngredient('B', BackItem(menuConfig.templates['B']!!) { openRewardsMenu(it) })
            }
            open(player)
        }
    }

    fun openQuestMenu(player: Player) {
        val menuConfig = configService.config.gui.questsMenu
        val questItemConfig = configService.config.gui.questItem
        val uuid = player.uniqueId

        if (battlePassService.getQuestProgress(uuid).isEmpty()) {
            battlePassService.checkAndResetDailyQuests(player)
        }
        val quests = battlePassService.getQuestProgress(uuid)

        Window.single().apply {
            import(player, menuConfig) {
                val questItems = if (quests.isEmpty()) {
                    listOf(
                        SimpleItem(
                            ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
                                .setDisplayName(
                                    AdventureComponentWrapper(
                                        configService.config.messages.noActiveQuests.asComponent()
                                    )
                                )
                                .get()
                        )
                    )
                } else {
                    quests.map { progress ->
                        // Resolve the exact variant that was rolled for this
                        // quest (not just the first config entry sharing its
                        // type) so the shown name/icon/amount always match
                        // progress.required.
                        val questConfig = dailyQuestManager.getQuestConfigForProgress(progress)

                        val statusRaw = if (progress.completed) {
                            questItemConfig.statusCompleted
                        } else {
                            questItemConfig.statusInProgress
                        }

                        // Resolved name comes from this quest's own config (or a
                        // readable fallback derived from its type) instead of
                        // the raw progress.questType string ("PLACE_BLOCK").
                        val questTypeName = questConfig?.resolvedDisplayName()
                            ?: BattlePassQuestConfig(type = progress.questType).resolvedDisplayName()

                        val tagResolvers = arrayOf(
                            Placeholder.component("quest_type", questTypeName.asComponent()),
                            Placeholder.unparsed("quest_xp", questConfig?.xp?.toString() ?: "0"),
                            Placeholder.unparsed("current", progress.current.toString()),
                            Placeholder.unparsed("required", progress.required.toString()),
                        )

                        val resolvedName = questItemConfig.displayName.resolve(*tagResolvers)
                        val resolvedStatus = statusRaw.resolve(*tagResolvers)
                        val resolvedLore =
                            listOf(resolvedStatus) + questItemConfig.lore.map { it.resolve(*tagResolvers) }

                        // Completed quests always show the global "done" icon;
                        // while in progress, prefer this quest's own configured
                        // icon (e.g. a dirt block for "place blocks") and fall
                        // back to the global in-progress icon if none is set.
                        val baseIcon = when {
                            progress.completed -> questItemConfig.completedIcon
                            questConfig?.icon != null -> questConfig.icon
                            else -> questItemConfig.inProgressIcon
                        }

                        val item = ItemBuilder(baseIcon.type)
                            .setDisplayName(AdventureComponentWrapper(resolvedName.asComponent()))
                            .setLore(resolvedLore.map { AdventureComponentWrapper(it.asComponent()) })
                            .get()

                        SimpleItem(item)
                    }
                }

                setContent(questItems)

                addIngredient('B', BackItem(menuConfig.templates['B']!!) { openMainMenu(it) })
            }
            open(player)
        }
    }

    fun handleRewardClick(
        player: Player,
        level: BattlePassLevelConfig,
        isPremium: Boolean,
    ) {
        val uuid = player.uniqueId
        if (isPremium) {
            val premiumState = battlePassService.getPremiumRewardState(uuid, level)
            when (premiumState) {
                RewardClaimState.PREMIUM_UNLOCKED -> {
                    if (battlePassService.claimPremiumReward(player, level.level)) {
                        player.sendMessage(configService.config.messages.rewardClaimed)
                    } else {
                        player.sendMessage(configService.config.messages.errorGivingReward)
                    }
                }

                RewardClaimState.PREMIUM_LOCKED -> {
                    val bpPlayer = battlePassService.getOrCreatePlayer(uuid)
                    if (!bpPlayer.isPremium) {
                        handlePremiumClick(player)
                    } else if (!battlePassService.isLevelUnlocked(uuid, level.level)) {
                        player.sendMessage(configService.config.messages.notEnoughXp)
                    } else {
                        player.sendMessage(configService.config.messages.premiumRequired)
                    }
                }

                RewardClaimState.PREMIUM_CLAIMED -> {
                    player.sendMessage(configService.config.messages.rewardAlreadyClaimed)
                }

                else -> {}
            }
        } else {
            val state = battlePassService.getFreeRewardState(uuid, level)
            when (state) {
                RewardClaimState.UNLOCKED -> {
                    if (battlePassService.claimReward(player, level.level)) {
                        player.sendMessage(configService.config.messages.rewardClaimed)
                    } else {
                        player.sendMessage(configService.config.messages.errorGivingReward)
                    }
                }

                RewardClaimState.LOCKED -> {
                    player.sendMessage(configService.config.messages.notEnoughXp)
                }

                RewardClaimState.CLAIMED -> {
                    player.sendMessage(configService.config.messages.rewardAlreadyClaimed)
                }

                else -> {}
            }
        }
    }

    // A FREE item renders ONLY its FREE state lore (LOCKED / AVAILABLE /
    // CLAIMED) and a PREMIUM item ONLY its PREMIUM state lore. This prevents
    // the "🔒 Premium" line from ever leaking onto a FREE slot — PREMIUM lore
    // is exclusive to RewardType.PREMIUM items.
    private fun getStateLore(state: RewardClaimState, premiumState: RewardClaimState, isPremium: Boolean): List<MiniMessageComponent> {
        val statusLore = configService.config.gui.statusLore
        if (!isPremium) {
            return when (state) {
                RewardClaimState.CLAIMED -> listOf(statusLore.claimed)
                RewardClaimState.UNLOCKED -> listOf(statusLore.unlocked)
                RewardClaimState.LOCKED -> listOf(statusLore.locked)
                else -> emptyList()
            }
        }
        return when (premiumState) {
            RewardClaimState.PREMIUM_CLAIMED -> listOf(statusLore.premiumClaimed)
            RewardClaimState.PREMIUM_UNLOCKED -> listOf(statusLore.premiumUnlocked)
            else -> listOf(statusLore.premiumLocked)
        }
    }

    private fun getLevelStateLore(state: LevelUnlockState): List<MiniMessageComponent> {
        val statusLore = configService.config.gui.statusLore
        return when (state) {
            LevelUnlockState.LOCKED_BY_PREVIOUS,
            LevelUnlockState.NOT_ENOUGH_XP,
            -> listOf(statusLore.locked)
            LevelUnlockState.AVAILABLE_TO_UNLOCK,
            LevelUnlockState.UNLOCKED,
            -> emptyList()
        }
    }

    fun handlePremiumClick(player: Player) {
        val uuid = player.uniqueId
        val bpPlayer = battlePassService.getOrCreatePlayer(uuid)
        val premiumConfig = configService.config.premium

        if (bpPlayer.isPremium) {
            player.sendMessage(premiumConfig.activeStatusMessage)
        } else {
            val command = premiumConfig.purchaseCommand
            val resolvedMessage = premiumConfig.inactiveStatusMessage.resolve(
                Placeholder.unparsed("command", command)
            )
            player.sendMessage(resolvedMessage)
        }
    }

    // ── Functional items ────────────────────────────────────────────────

    private inner class RewardsItem(
        private val template: ItemTemplate,
    ) : ControlItem<Gui>() {
        override fun getItemProvider(gui: Gui): ItemProvider {
            return ItemProvider { template.toItem() }
        }

        override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
            openRewardsMenu(player)
        }
    }

    private inner class QuestsItem(
        private val template: ItemTemplate,
    ) : ControlItem<Gui>() {
        override fun getItemProvider(gui: Gui): ItemProvider {
            return ItemProvider { template.toItem() }
        }

        override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
            openQuestMenu(player)
        }
    }

    // No separate premium submenu — the item itself in the main menu shows
    // subscription status, current XP and the player's stats straight from
    // the DB (claimed rewards, premium rewards, today's quests).
    private inner class PremiumItem(
        private val player: Player,
        private val template: ItemTemplate,
    ) : ControlItem<Gui>() {
        override fun getItemProvider(gui: Gui): ItemProvider {
            val uuid = player.uniqueId
            val bpPlayer = battlePassService.getOrCreatePlayer(uuid)
            val loreConfig = configService.config.premium.itemLore

            val totalRewards = battlePassService.sortedLevels.size
            val claimedFree =
                battlePassService.sortedLevels.count { battlePassService.hasClaimedReward(uuid, it.level) }
            val claimedPremium =
                battlePassService.sortedLevels.count { battlePassService.hasClaimedPremiumReward(uuid, it.level) }

            val quests = battlePassService.getQuestProgress(uuid)
            val completedQuests = quests.count { it.completed }

            val statusLine = if (bpPlayer.isPremium) loreConfig.activeStatus else loreConfig.inactiveStatus
            val xpLine = loreConfig.xp.resolve(Placeholder.unparsed("xp", bpPlayer.xp.toString()))
            val freeLine = loreConfig.freeRewardsClaimed.resolve(
                Placeholder.unparsed("claimed", claimedFree.toString()),
                Placeholder.unparsed("total", totalRewards.toString()),
            )
            val premiumLine = loreConfig.premiumRewardsClaimed.resolve(
                Placeholder.unparsed("claimed", claimedPremium.toString()),
                Placeholder.unparsed("total", totalRewards.toString()),
            )
            val questsLine = loreConfig.questsCompleted.resolve(
                Placeholder.unparsed("completed", completedQuests.toString()),
                Placeholder.unparsed("total", quests.size.toString()),
            )

            val lore = mutableListOf(statusLine, xpLine, freeLine, premiumLine, questsLine)
            if (!bpPlayer.isPremium) {
                lore.add(loreConfig.purchaseHint)
            }

            val icon = if (bpPlayer.isPremium) Material.NETHERITE_INGOT else Material.IRON_INGOT
            val finalItem = template.copy(type = icon, lore = lore)

            return ItemProvider { finalItem.toItem() }
        }

        override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
            handlePremiumClick(player)
        }
    }

    // Shared manual pagination state for the rewards menu. FREE and PREMIUM
    // always move together — there is exactly one page counter for the whole
    // FREE+PREMIUM grid.
    private class RewardPageController(pageCount: Int) {
        private var currentPage = 0
        private val size = maxOf(1, pageCount)

        val page: Int
            get() = currentPage

        val pageCount: Int
            get() = size

        fun goForward(): Boolean =
            if (currentPage < size - 1) { currentPage++; true } else false

        fun goBack(): Boolean =
            if (currentPage > 0) { currentPage--; true } else false
    }

    // '>' / '<' navigation buttons for a normal (non-paged) Gui. They move the
    // shared page counter and ask the caller to re-render the reward slots.
    private class RewardNavItem(
        private val template: ItemTemplate,
        private val forward: Boolean,
        private val controller: RewardPageController,
        private val onPageChange: () -> Unit,
    ) : AbstractItem() {
        override fun getItemProvider(): ItemProvider {
            return ItemProvider { template.toItem() }
        }

        override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
            if (clickType != ClickType.LEFT) return
            val changed = if (forward) controller.goForward() else controller.goBack()
            if (changed) onPageChange()
        }
    }

    // One reward slot renders either the FREE or the PREMIUM reward of a
    // level depending on its RewardType. It is a purely dynamic AbstractItem:
    // the 'F' and 'P' letters it is bound to in the config structure have no
    // item of their own — this item provides both the icon (by claim state)
    // and the click behaviour (LMB claim / RMB preview).
    private class RewardItem(
        private val player: Player,
        private val level: BattlePassLevelConfig,
        private val rewardType: RewardType,
        private val menu: BattlePassMenu,
    ) : AbstractItem() {

        override fun getItemProvider(): ItemProvider {
            val uuid = player.uniqueId
            val isPremium = rewardType == RewardType.PREMIUM
            val freeState = menu.battlePassService.getFreeRewardState(uuid, level)
            val premiumState = menu.battlePassService.getPremiumRewardState(uuid, level)
            val bpPlayer = menu.battlePassService.getOrCreatePlayer(uuid)

            val rewardIcons = menu.configService.config.gui.rewardIcons

            val tagResolvers = arrayOf(
                Placeholder.unparsed("number", level.level.toString()),
                Placeholder.unparsed("required_xp", level.requiredXp.toString()),
                Placeholder.unparsed("xp", bpPlayer.xp.toString()),
            )

            // Icons stay visible even when claimed — the claimed state just
            // changes the icon, it never removes the level from the GUI.
            //
            // The state/visual logic strictly depends on RewardType: a FREE
            // slot can only ever use FREE icons/lore and a FREE reward state,
            // a PREMIUM slot only PREMIUM icons/lore and a PREMIUM state.
            // PREMIUM_REQUIRED can never appear for a FREE item.
            val baseItem: ItemTemplate = if (isPremium) {
                val icons = rewardIcons.premium
                val xpUnlocked = menu.battlePassService.isLevelUnlocked(uuid, level.level)
                when {
                    premiumState == RewardClaimState.PREMIUM_CLAIMED -> icons.claimed
                    // Insufficient XP -> plain LOCKED, regardless of premium.
                    !xpUnlocked -> icons.locked
                    // Enough XP but no premium -> PREMIUM_REQUIRED visual.
                    !bpPlayer.isPremium -> icons.noPremium
                    premiumState == RewardClaimState.PREMIUM_UNLOCKED -> icons.available
                    else -> icons.locked
                }
            } else {
                val icons = rewardIcons.free
                when (freeState) {
                    RewardClaimState.CLAIMED -> icons.claimed
                    RewardClaimState.UNLOCKED -> icons.available
                    else -> icons.locked
                }
            }

            val statusLoreCfg = menu.configService.config.gui.statusLore
            val infoLore = listOf(
                statusLoreCfg.level.resolve(*tagResolvers),
                statusLoreCfg.requiredXp.resolve(*tagResolvers),
            )

            // A level opens automatically when its XP cost is covered. Until a
            // level is unlocked (LOCKED_BY_PREVIOUS / NOT_ENOUGH_XP) we show the
            // level status line; once it is UNLOCKED we show the reward status
            // line for this slot's track (FREE or PREMIUM only).
            val levelState = menu.battlePassService.getLevelState(uuid, level.level)
            val stateLore: List<MiniMessageComponent> = if (levelState == LevelUnlockState.UNLOCKED) {
                menu.getStateLore(freeState, premiumState, isPremium)
            } else {
                menu.getLevelStateLore(levelState)
            }
            val finalItem = baseItem.resolveLore(infoLore + stateLore, *tagResolvers)

            return ItemProvider { finalItem.toItem() }
        }

        override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
            val isPremium = rewardType == RewardType.PREMIUM
            when (clickType) {
                ClickType.LEFT -> {
                    menu.handleRewardClick(player, level, isPremium)
                    // Item stays in the GUI and just re-renders to CLAIMED.
                    notifyWindows()
                }
                ClickType.RIGHT -> menu.openRewardPreview(player, level, isPremium)
                else -> {}
            }
        }
    }

    private enum class RewardType {
        FREE, PREMIUM,
    }
}
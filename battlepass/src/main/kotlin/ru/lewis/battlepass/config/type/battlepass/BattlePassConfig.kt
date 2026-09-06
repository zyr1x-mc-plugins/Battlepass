package ru.lewis.battlepass.config.type.battlepass

import org.spongepowered.configurate.objectmapping.ConfigSerializable
import ru.lewis.battlepass.config.type.MenuConfig
import ru.lewis.battlepass.config.type.MiniMessageComponent
import ru.lewis.battlepass.config.type.TitleConfiguration
import ru.lewis.battlepass.extensions.asMiniMessageComponent

@ConfigSerializable
data class BattlePassConfig(
    val dailyQuests: DailyQuestSettings = DailyQuestSettings(),
    val premium: PremiumSettings = PremiumSettings(),
    val notifications: NotificationSettings = NotificationSettings(),
    val messages: MessageSettings = MessageSettings(),
    val titles: TitleSettings = TitleSettings(),
    val gui: GUISettings = GUISettings(),
    val placeholders: PlaceholderSettings = PlaceholderSettings(),
) {
    @ConfigSerializable
    data class DailyQuestSettings(
        val amount: Int = 5,
    )

    @ConfigSerializable
    data class PremiumSettings(
        val purchaseCommand: String = "",
        val activeStatusMessage: MiniMessageComponent = "<green>Premium активен".asMiniMessageComponent(),
        val inactiveStatusMessage: MiniMessageComponent = "<red>Premium неактивен. Купите: <click:run_command:'<command>'><gold>[Нажмите]</click>".asMiniMessageComponent(),
        val itemLore: PremiumItemLore = PremiumItemLore(),
    ) {
        // Lines shown directly on the "Premium Battle Pass" item in the main
        // menu — no separate premium submenu anymore, everything is in the
        // item's own lore, filled in with the player's real DB data.
        @ConfigSerializable
        data class PremiumItemLore(
            val activeStatus: MiniMessageComponent = "<green>Подписка: Активна".asMiniMessageComponent(),
            val inactiveStatus: MiniMessageComponent = "<red>Подписка: Не активна".asMiniMessageComponent(),
            val xp: MiniMessageComponent = "<gray>Опыт: <gold><xp> XP".asMiniMessageComponent(),
            val freeRewardsClaimed: MiniMessageComponent = "<gray>Награды (Free): <white><claimed>/<total>".asMiniMessageComponent(),
            val premiumRewardsClaimed: MiniMessageComponent = "<gray>Награды (Premium): <white><claimed>/<total>".asMiniMessageComponent(),
            val questsCompleted: MiniMessageComponent = "<gray>Квесты сегодня: <white><completed>/<total>".asMiniMessageComponent(),
            val purchaseHint: MiniMessageComponent = "<yellow>Нажмите, чтобы купить Premium".asMiniMessageComponent(),
        )
    }

    @ConfigSerializable
    data class NotificationSettings(
        val unclaimedRewards: UnclaimedRewardsSettings = UnclaimedRewardsSettings(),
    ) {
        @ConfigSerializable
        data class UnclaimedRewardsSettings(
            val enabled: Boolean = true,
            val interval: java.time.Duration = java.time.Duration.ofMinutes(15),
            val message: MiniMessageComponent = "<gold>Battle Pass <gray>У вас есть доступные награды!".asMiniMessageComponent(),
        )
    }

    @ConfigSerializable
    data class MessageSettings(
        val rewardClaimed: MiniMessageComponent = "<green>Награда получена!".asMiniMessageComponent(),
        val rewardAlreadyClaimed: MiniMessageComponent = "<red>Награда уже получена.".asMiniMessageComponent(),
        val notEnoughXp: MiniMessageComponent = "<red>Недостаточно XP для получения награды.".asMiniMessageComponent(),
        val previousRewardNotClaimed: MiniMessageComponent = "<red>Сначала получите предыдущую награду.".asMiniMessageComponent(),
        val premiumRequired: MiniMessageComponent = "<red>Для получения этой награды необходим Premium Battle Pass.".asMiniMessageComponent(),
        val questCompleted: MiniMessageComponent = "<green>Квест выполнен! +<xp> XP".asMiniMessageComponent(),
        val errorGivingReward: MiniMessageComponent = "<red>Произошла ошибка при выдаче награды.".asMiniMessageComponent(),
        val noActiveQuests: MiniMessageComponent = "<gray>У вас нет активных квестов. Квесты обновятся завтра.".asMiniMessageComponent(),
    )

    @ConfigSerializable
    data class TitleSettings(
        val questProgress50: TitleConfiguration = TitleConfiguration(
            mainTitle = "<gold>50%!".asMiniMessageComponent(),
            subTitle = "<gray>Вы почти выполнили квест".asMiniMessageComponent(),
        ),
        val questCompleted: TitleConfiguration = TitleConfiguration(
            mainTitle = "<green>Квест выполнен!".asMiniMessageComponent(),
            subTitle = "<gray>Вы получили <xp> Battle Pass XP".asMiniMessageComponent(),
        ),
    )

    @ConfigSerializable
    data class GUISettings(
        val mainMenu: MenuConfig = MenuConfig(
            title = "<gold>Battle Pass".asMiniMessageComponent(),
            structure = listOf(
                "_ _ _ _ _ _ _ _ _",
                "_ _ _ R Q P _ _ _",
                "_ _ _ _ _ _ _ _ _",
            ),
            templates = mapOf(
                'R' to ru.lewis.battlepass.config.type.ItemTemplate(
                    type = org.bukkit.Material.CHEST,
                    displayName = "<gold>Награды".asMiniMessageComponent(),
                    lore = listOf("<gray>Просмотреть награды Battle Pass".asMiniMessageComponent()),
                ),
                'Q' to ru.lewis.battlepass.config.type.ItemTemplate(
                    type = org.bukkit.Material.WRITABLE_BOOK,
                    displayName = "<gold>Ежедневные квесты".asMiniMessageComponent(),
                    lore = listOf("<gray>Просмотреть ежедневные квесты".asMiniMessageComponent()),
                ),
                'P' to ru.lewis.battlepass.config.type.ItemTemplate(
                    type = org.bukkit.Material.NETHERITE_INGOT,
                    displayName = "<gold>Premium Battle Pass".asMiniMessageComponent(),
                    lore = listOf("<gray>Статус Premium".asMiniMessageComponent()),
                ),
            ),
        ),
        val rewardsMenu: MenuConfig = MenuConfig(
            title = "<gold>Battle Pass - Награды".asMiniMessageComponent(),
            structure = listOf(
                "R R O O O O O R R",
                "R F F F F F F F R",
                "R x x x x x x x R",
                "R P P P P P P P R",
                "R R < O B O > R R",
            ),
            customItems = mapOf(
                'R' to ru.lewis.battlepass.config.type.ItemTemplate(
                    type = org.bukkit.Material.BLACK_STAINED_GLASS_PANE,
                    displayName = " ".asMiniMessageComponent(),
                ),
                'O' to ru.lewis.battlepass.config.type.ItemTemplate(
                    type = org.bukkit.Material.GRAY_STAINED_GLASS_PANE,
                    displayName = " ".asMiniMessageComponent(),
                ),
                'x' to ru.lewis.battlepass.config.type.ItemTemplate(
                    type = org.bukkit.Material.BLACK_STAINED_GLASS_PANE,
                    displayName = " ".asMiniMessageComponent(),
                ),
            ),
            templates = mapOf(
                '<' to ru.lewis.battlepass.config.type.ItemTemplate(
                    type = org.bukkit.Material.ARROW,
                    displayName = "<gray>Страница назад".asMiniMessageComponent(),
                ),
                '>' to ru.lewis.battlepass.config.type.ItemTemplate(
                    type = org.bukkit.Material.ARROW,
                    displayName = "<gray>Страница вперед".asMiniMessageComponent(),
                ),
                'B' to ru.lewis.battlepass.config.type.ItemTemplate(
                    type = org.bukkit.Material.ARROW,
                    displayName = "<red>Вернуться назад".asMiniMessageComponent(),
                    lore = listOf("<gray>Вернуться в главное меню".asMiniMessageComponent()),
                ),
            ),
        ),
        val rewardViewMenu: MenuConfig = MenuConfig(
            title = "<gold>Награда #<number>".asMiniMessageComponent(),
            structure = listOf(
                "x x x x x x x x x",
                ". . . . . . . . .",
                ". . . . . . . . .",
                ". . . . . . . . .",
                ". . . . . . . . .",
                "< x x x B x x x >",
            ),
            customItems = mapOf(
                'x' to ru.lewis.battlepass.config.type.ItemTemplate(
                    type = org.bukkit.Material.BLACK_STAINED_GLASS_PANE,
                    displayName = " ".asMiniMessageComponent(),
                ),
            ),
            templates = mapOf(
                '<' to ru.lewis.battlepass.config.type.ItemTemplate(
                    type = org.bukkit.Material.ARROW,
                    displayName = "<gray>Страница назад".asMiniMessageComponent(),
                ),
                '>' to ru.lewis.battlepass.config.type.ItemTemplate(
                    type = org.bukkit.Material.ARROW,
                    displayName = "<gray>Страница вперед".asMiniMessageComponent(),
                ),
                'B' to ru.lewis.battlepass.config.type.ItemTemplate(
                    type = org.bukkit.Material.ARROW,
                    displayName = "<red>Вернуться назад".asMiniMessageComponent(),
                    lore = listOf("<gray>Вернуться к наградам".asMiniMessageComponent()),
                ),
            ),
        ),
        val questsMenu: MenuConfig = MenuConfig(
            title = "<gold>Battle Pass - Ежедневные квесты".asMiniMessageComponent(),
            structure = listOf(
                "x x x x x x x x x",
                ". . . . . . . . .",
                "x x x x x x x x x",
                "x x x x x x x x x",
                "x x x x x x x x x",
                "< x x x B x x x >",
            ),
            customItems = mapOf(
                'x' to ru.lewis.battlepass.config.type.ItemTemplate(
                    type = org.bukkit.Material.BLACK_STAINED_GLASS_PANE,
                    displayName = " ".asMiniMessageComponent(),
                ),
            ),
            templates = mapOf(
                '<' to ru.lewis.battlepass.config.type.ItemTemplate(
                    type = org.bukkit.Material.ARROW,
                    displayName = "<gray>Страница назад".asMiniMessageComponent(),
                ),
                '>' to ru.lewis.battlepass.config.type.ItemTemplate(
                    type = org.bukkit.Material.ARROW,
                    displayName = "<gray>Страница вперед".asMiniMessageComponent(),
                ),
                'B' to ru.lewis.battlepass.config.type.ItemTemplate(
                    type = org.bukkit.Material.ARROW,
                    displayName = "<red>Вернуться назад".asMiniMessageComponent(),
                    lore = listOf("<gray>Вернуться в главное меню".asMiniMessageComponent()),
                ),
            ),
        ),
        val questItem: QuestItem = QuestItem(),
        val statusLore: StatusLore = StatusLore(),
        val rewardIcons: RewardIcons = RewardIcons(),
    )

    @ConfigSerializable
    data class QuestItem(
        val displayName: MiniMessageComponent = "<gold>Квест: <quest_type>".asMiniMessageComponent(),
        val lore: List<MiniMessageComponent> = listOf(
            "<gray>Награда: <gold><quest_xp> XP".asMiniMessageComponent(),
        ),
        val statusCompleted: MiniMessageComponent = "<green>✓ Выполнено".asMiniMessageComponent(),
        val statusInProgress: MiniMessageComponent = "<yellow><current> / <required>".asMiniMessageComponent(),
        val completedIcon: ru.lewis.battlepass.config.type.ItemTemplate = ru.lewis.battlepass.config.type.ItemTemplate(
            type = org.bukkit.Material.LIME_STAINED_GLASS_PANE,
            displayName = "<green>✓ Выполнено".asMiniMessageComponent(),
        ),
        val inProgressIcon: ru.lewis.battlepass.config.type.ItemTemplate = ru.lewis.battlepass.config.type.ItemTemplate(
            type = org.bukkit.Material.PAPER,
            displayName = "<gold>Квест".asMiniMessageComponent(),
        ),
    )

    @ConfigSerializable
    data class RewardIcons(
        val free: FreeIcons = FreeIcons(),
        val premium: PremiumIcons = PremiumIcons(),
    ) {
        @ConfigSerializable
        data class FreeIcons(
            val locked: ru.lewis.battlepass.config.type.ItemTemplate = ru.lewis.battlepass.config.type.ItemTemplate(
                type = org.bukkit.Material.GRAY_STAINED_GLASS_PANE,
                displayName = "<gray>Заблокировано".asMiniMessageComponent(),
            ),
            val available: ru.lewis.battlepass.config.type.ItemTemplate = ru.lewis.battlepass.config.type.ItemTemplate(
                type = org.bukkit.Material.LIME_STAINED_GLASS_PANE,
                displayName = "<gold>★ Доступно".asMiniMessageComponent(),
            ),
            val claimed: ru.lewis.battlepass.config.type.ItemTemplate = ru.lewis.battlepass.config.type.ItemTemplate(
                type = org.bukkit.Material.LIME_STAINED_GLASS_PANE,
                displayName = "<green>✓ Получено".asMiniMessageComponent(),
            ),
        )

        @ConfigSerializable
        data class PremiumIcons(
            val locked: ru.lewis.battlepass.config.type.ItemTemplate = ru.lewis.battlepass.config.type.ItemTemplate(
                type = org.bukkit.Material.GRAY_STAINED_GLASS_PANE,
                displayName = "<gray>Заблокировано".asMiniMessageComponent(),
            ),
            val available: ru.lewis.battlepass.config.type.ItemTemplate = ru.lewis.battlepass.config.type.ItemTemplate(
                type = org.bukkit.Material.CYAN_STAINED_GLASS_PANE,
                displayName = "<aqua>★ Premium доступно".asMiniMessageComponent(),
            ),
            val claimed: ru.lewis.battlepass.config.type.ItemTemplate = ru.lewis.battlepass.config.type.ItemTemplate(
                type = org.bukkit.Material.CYAN_STAINED_GLASS_PANE,
                displayName = "<aqua>✓ Premium получено".asMiniMessageComponent(),
            ),
            val noPremium: ru.lewis.battlepass.config.type.ItemTemplate = ru.lewis.battlepass.config.type.ItemTemplate(
                type = org.bukkit.Material.RED_STAINED_GLASS_PANE,
                displayName = "<red>🔒 Требуется Premium".asMiniMessageComponent(),
            ),
        )
    }

    @ConfigSerializable
    data class StatusLore(
        val level: MiniMessageComponent = "<gray>Уровень <yellow><number>".asMiniMessageComponent(),
        val requiredXp: MiniMessageComponent = "<gray>Требуется: <yellow><required_xp> XP".asMiniMessageComponent(),
        val claimed: MiniMessageComponent = "<green>✓ Получено".asMiniMessageComponent(),
        val unlocked: MiniMessageComponent = "<gold>★ Доступно (ЛКМ)".asMiniMessageComponent(),
        val locked: MiniMessageComponent = "<red>✗ Заблокировано".asMiniMessageComponent(),
        val premiumClaimed: MiniMessageComponent = "<aqua>✓ Premium получено".asMiniMessageComponent(),
        val premiumUnlocked: MiniMessageComponent = "<aqua>★ Premium доступно (ЛКМ)".asMiniMessageComponent(),
        val premiumLocked: MiniMessageComponent = "<gray>🔒 Premium".asMiniMessageComponent(),
    )

    @ConfigSerializable
    data class PlaceholderSettings(
        val premiumActive: String = "Активен",
        val premiumInactive: String = "Не активен",
        val progressFormat: String = "%percent%%",
        val rewardsProgressFormat: String = "%claimed%/%total%",
        val questsProgressFormat: String = "%completed%/%total%",
        val resetFormat: String = "HH:mm:ss",
        val noData: String = "-",
    )
}
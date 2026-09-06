package ru.lewis.battlepass.service

import com.google.inject.Inject
import com.google.inject.Singleton
import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.sound.Sound
import org.bukkit.Material
import org.bukkit.entity.EntityType
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemFlag
import org.bukkit.plugin.Plugin
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.potion.PotionType
import org.spongepowered.configurate.kotlin.extensions.get
import org.spongepowered.configurate.kotlin.extensions.set
import org.spongepowered.configurate.kotlin.objectMapperFactory
import org.spongepowered.configurate.yaml.NodeStyle
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import ru.lewis.battlepass.config.serializer.*
import ru.lewis.battlepass.config.type.ItemTemplate
import ru.lewis.battlepass.config.type.MiniMessageComponent
import ru.lewis.battlepass.config.type.battlepass.BattlePassConfig
import ru.lewis.battlepass.config.type.battlepass.BattlePassLevelConfig
import ru.lewis.battlepass.config.type.battlepass.BattlePassQuestConfig
import ru.lewis.battlepass.extensions.asMiniMessageComponent
import java.awt.Color
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.util.Comparator

@Singleton
class BattlePassConfigService @Inject constructor(
    private val plugin: Plugin,
    private val materialSerializer: MaterialSerializer,
    private val entityTypeSerializer: EntityTypeSerializer,
    private val miniMessageComponentSerializer: MiniMessageComponentSerializer,
    private val colorSerializer: ColorSerializer,
    private val potionEffectSerializer: PotionEffectSerializer,
    private val enchantmentSerializer: EnchantmentSerializer,
    private val attributeModifierSerializer: AttributeModifierSerializer,
    private val potionEffectTypeSerializer: PotionEffectTypeSerializer,
    private val itemFlagSerializer: ItemFlagSerializer,
    private val attributeSerializer: AttributeSerializer,
    private val potionTypeSerializer: PotionTypeSerializer,
    private val durationSerializer: DurationSerializer,
    private val soundSourceSerializer: SoundSourceSerializer,
    private val bossBarColorSerializer: BossBarColorSerializer,
    private val bossBarOverlaySerializer: BossBarOverlaySerializer,
) {
    private val dataFolder: Path get() = plugin.dataFolder.toPath()

    lateinit var config: BattlePassConfig
        private set

    val levelConfigs: MutableMap<Int, BattlePassLevelConfig> = mutableMapOf()
    val questConfigs: MutableList<BattlePassQuestConfig> = mutableListOf()

    fun load() {
        config = loadConfig("config")
        loadLevels()
        loadQuests()
    }

    private fun createLoaderBuilder(): YamlConfigurationLoader.Builder {
        return YamlConfigurationLoader.builder()
            .defaultOptions { options ->
                options.serializers { builder ->
                    builder
                        .register(PotionType::class.java, potionTypeSerializer)
                        .register(Attribute::class.java, attributeSerializer)
                        .register(ItemFlag::class.java, itemFlagSerializer)
                        .register(MiniMessageComponent::class.java, miniMessageComponentSerializer)
                        .register(Material::class.java, materialSerializer)
                        .register(org.bukkit.entity.EntityType::class.java, entityTypeSerializer)
                        .register(Color::class.java, colorSerializer)
                        .register(PotionEffect::class.java, potionEffectSerializer)
                        .register(Enchantment::class.java, enchantmentSerializer)
                        .register(Duration::class.java, durationSerializer)
                        .register(AttributeModifier::class.java, attributeModifierSerializer)
                        .register(PotionEffectType::class.java, potionEffectTypeSerializer)
                        .register(Sound.Source::class.java, soundSourceSerializer)
                        .register(BossBar.Color::class.java, bossBarColorSerializer)
                        .register(BossBar.Overlay::class.java, bossBarOverlaySerializer)
                        .registerAnnotatedObjects(objectMapperFactory())
                }
            }
            .indent(2)
            .nodeStyle(NodeStyle.BLOCK)
    }

    private inline fun <reified T : Any> YamlConfigurationLoader.getAndSave(): T {
        val node = this.load()
        var obj = node.get(T::class)
        if (obj == null) {
            plugin.logger.warning("Config ${T::class.simpleName} is empty, creating default...")
            obj = try {
                T::class.java.getDeclaredConstructor().newInstance()
            } catch (e: Exception) {
                throw IllegalStateException(
                    "Failed to create default config for ${T::class.simpleName}.", e
                )
            }
        }
        node.set(T::class, obj)
        this.save(node)
        return obj
    }

    private inline fun <reified T : Any> loadConfig(name: String, subfolder: String? = null): T {
        val path = if (subfolder != null) {
            dataFolder.resolve("$subfolder${File.separator}$name.yml")
        } else {
            dataFolder.resolve("$name.yml")
        }
        path.parent?.let { Files.createDirectories(it) }
        val loader = createLoaderBuilder().path(path).build()
        return try {
            loader.getAndSave()
        } catch (e: Exception) {
            plugin.logger.severe("Failed to load config: $name.yml — ${e.message}")
            throw e
        }
    }

    private fun levelsDir(): Path {
        val dir = dataFolder.resolve("levels")
        Files.createDirectories(dir)
        return dir
    }

    fun getLevelFile(level: Int): Path = levelsDir().resolve("level-$level.yml")

    fun getSortedLevels(): List<BattlePassLevelConfig> =
        levelConfigs.values.sortedBy { it.level }

    private fun loadLevels() {
        val levelsDir = levelsDir()
        levelConfigs.clear()
        val ymlFiles = Files.list(levelsDir)
            .filter { it.fileName.toString().endsWith(".yml") }
            .sorted(Comparator.comparingInt { path ->
                parseLevelNumber(path.fileName.toString()) ?: Int.MAX_VALUE
            })
            .toList()

        if (ymlFiles.isEmpty()) {
            createDefaultLevels()
            return
        }

        ymlFiles.forEach { path ->
            try {
                val loader = createLoaderBuilder().path(path).build()
                val node = loader.load()
                val levelConfig = node.get(BattlePassLevelConfig::class.java)
                if (levelConfig != null) {
                    levelConfigs[levelConfig.level] = levelConfig
                }
            } catch (e: Exception) {
                plugin.logger.warning("Failed to load level config: ${path.fileName} — ${e.message}")
            }
        }
    }

    private fun parseLevelNumber(fileName: String): Int? {
        val match = Regex("^level-(\\d+)\\.yml$").matchEntire(fileName) ?: return null
        return match.groupValues[1].toIntOrNull()
    }

    private fun createDefaultLevels() {
        val defaults = listOf(
            BattlePassLevelConfig(
                level = 1,
                requiredXp = 100,
                free = BattlePassLevelConfig.LevelTrack(
                    rewards = BattlePassLevelConfig.LevelTrack.RewardContent(
                        items = listOf(ItemTemplate(type = Material.DIAMOND)),
                    ),
                    display = listOf(
                        ItemTemplate(type = Material.DIAMOND, displayName = "<aqua>1x Алмаз".asMiniMessageComponent()),
                    ),
                ),
                premium = BattlePassLevelConfig.LevelTrack(
                    rewards = BattlePassLevelConfig.LevelTrack.RewardContent(
                        items = listOf(ItemTemplate(type = Material.DIAMOND, amount = 3)),
                    ),
                    display = listOf(
                        ItemTemplate(type = Material.DIAMOND, amount = 3, displayName = "<gold>3x Алмаз (Premium)".asMiniMessageComponent()),
                    ),
                ),
            ),
            BattlePassLevelConfig(
                level = 2,
                requiredXp = 250,
                free = BattlePassLevelConfig.LevelTrack(
                    rewards = BattlePassLevelConfig.LevelTrack.RewardContent(
                        items = listOf(ItemTemplate(type = Material.EMERALD, amount = 3)),
                    ),
                    display = listOf(
                        ItemTemplate(type = Material.EMERALD, amount = 3, displayName = "<aqua>3x Изумруд".asMiniMessageComponent()),
                    ),
                ),
                premium = BattlePassLevelConfig.LevelTrack(
                    rewards = BattlePassLevelConfig.LevelTrack.RewardContent(
                        items = listOf(ItemTemplate(type = Material.EMERALD, amount = 9)),
                    ),
                    display = listOf(
                        ItemTemplate(type = Material.EMERALD, amount = 9, displayName = "<gold>9x Изумруд (Premium)".asMiniMessageComponent()),
                    ),
                ),
            ),
            BattlePassLevelConfig(
                level = 3,
                requiredXp = 500,
                free = BattlePassLevelConfig.LevelTrack(
                    rewards = BattlePassLevelConfig.LevelTrack.RewardContent(
                        items = listOf(ItemTemplate(type = Material.GOLDEN_APPLE, amount = 3)),
                    ),
                    display = listOf(
                        ItemTemplate(type = Material.GOLDEN_APPLE, amount = 3, displayName = "<aqua>3x Золотое яблоко".asMiniMessageComponent()),
                    ),
                ),
                premium = BattlePassLevelConfig.LevelTrack(
                    rewards = BattlePassLevelConfig.LevelTrack.RewardContent(
                        items = listOf(ItemTemplate(type = Material.NETHERITE_INGOT, amount = 2)),
                        commands = listOf("give %player% diamond 16"),
                    ),
                    display = listOf(
                        ItemTemplate(type = Material.NETHERITE_INGOT, amount = 2, displayName = "<gold>2x Незеритовый слиток (Premium)".asMiniMessageComponent()),
                    ),
                ),
            ),
        )

        defaults.forEach { levelConfig ->
            saveLevel(levelConfig)
        }
        loadLevels()
    }

    /**
     * Persists the given level config back to its dedicated file
     * (levels/level-N.yml) and updates the in-memory map.
     */
    fun saveLevel(levelConfig: BattlePassLevelConfig): Boolean {
        return try {
            val path = getLevelFile(levelConfig.level)
            path.parent?.let { Files.createDirectories(it) }
            val loader = createLoaderBuilder().path(path).build()
            val node = loader.createNode()
            node.set(BattlePassLevelConfig::class.java, levelConfig)
            loader.save(node)
            levelConfigs[levelConfig.level] = levelConfig
            true
        } catch (e: Exception) {
            plugin.logger.severe("Failed to save level ${levelConfig.level} config — ${e.message}")
            false
        }
    }

    /**
     * Creates a new level config file. Returns false if the level already
     * exists (no overwrite).
     */
    fun createLevel(level: Int, requiredXp: Long): Boolean {
        if (levelConfigs.containsKey(level)) return false
        val created = BattlePassLevelConfig(level = level, requiredXp = requiredXp)
        val saved = saveLevel(created)
        if (saved) levelConfigs[level] = created
        return saved
    }

    /**
     * Deletes the level config file and removes it from memory.
     */
    fun deleteLevel(level: Int): Boolean {
        val path = getLevelFile(level)
        return try {
            val removed = Files.deleteIfExists(path)
            levelConfigs.remove(level)
            removed
        } catch (e: Exception) {
            plugin.logger.warning("Failed to delete level $level config — ${e.message}")
            false
        }
    }

    /**
     * Updates the required XP of an existing level and saves it. Returns null
     * if the level does not exist.
     */
    fun setLevelXp(level: Int, xp: Long): BattlePassLevelConfig? {
        val current = levelConfigs[level] ?: return null
        val updated = current.copy(requiredXp = xp)
        return if (saveLevel(updated)) updated else null
    }

    /**
     * Re-reads a single level from disk into memory. Useful after a command
     * mutates a level so the change applies without a full reload.
     */
    fun reloadLevel(level: Int): BattlePassLevelConfig? {
        val path = getLevelFile(level)
        if (!Files.exists(path)) return null
        return try {
            val loader = createLoaderBuilder().path(path).build()
            val node = loader.load()
            val levelConfig = node.get(BattlePassLevelConfig::class.java)
            if (levelConfig != null) levelConfigs[level] = levelConfig
            levelConfig
        } catch (e: Exception) {
            plugin.logger.warning("Failed to reload level $level config — ${e.message}")
            null
        }
    }

    // ── Level reward mutation helpers ────────────────────────────────────

    private fun track(levelConfig: BattlePassLevelConfig, premium: Boolean): BattlePassLevelConfig.LevelTrack =
        if (premium) levelConfig.premium else levelConfig.free

    fun addRewardItem(level: Int, premium: Boolean, item: ItemTemplate): Boolean {
        val current = levelConfigs[level] ?: return false
        val track = track(current, premium)
        val newTrack = track.copy(
            rewards = track.rewards.copy(items = track.rewards.items + item)
        )
        return saveLevel(if (premium) current.copy(premium = newTrack) else current.copy(free = newTrack))
    }

    fun addDisplayItem(level: Int, premium: Boolean, item: ItemTemplate): Boolean {
        val current = levelConfigs[level] ?: return false
        val track = track(current, premium)
        val newTrack = track.copy(display = track.display + item)
        return saveLevel(if (premium) current.copy(premium = newTrack) else current.copy(free = newTrack))
    }

    fun addCommand(level: Int, premium: Boolean, command: String): Boolean {
        val current = levelConfigs[level] ?: return false
        val track = track(current, premium)
        val newTrack = track.copy(
            rewards = track.rewards.copy(commands = track.rewards.commands + command)
        )
        return saveLevel(if (premium) current.copy(premium = newTrack) else current.copy(free = newTrack))
    }

    fun removeRewardItem(level: Int, premium: Boolean, index: Int): Boolean {
        val current = levelConfigs[level] ?: return false
        val track = track(current, premium)
        if (index < 1 || index > track.rewards.items.size) return false
        val newTrack = track.copy(
            rewards = track.rewards.copy(items = track.rewards.items.filterIndexed { i, _ -> i != index - 1 })
        )
        return saveLevel(if (premium) current.copy(premium = newTrack) else current.copy(free = newTrack))
    }

    fun removeDisplayItem(level: Int, premium: Boolean, index: Int): Boolean {
        val current = levelConfigs[level] ?: return false
        val track = track(current, premium)
        if (index < 1 || index > track.display.size) return false
        val newTrack = track.copy(display = track.display.filterIndexed { i, _ -> i != index - 1 })
        return saveLevel(if (premium) current.copy(premium = newTrack) else current.copy(free = newTrack))
    }

    fun removeCommand(level: Int, premium: Boolean, index: Int): Boolean {
        val current = levelConfigs[level] ?: return false
        val track = track(current, premium)
        if (index < 1 || index > track.rewards.commands.size) return false
        val newTrack = track.copy(
            rewards = track.rewards.copy(commands = track.rewards.commands.filterIndexed { i, _ -> i != index - 1 })
        )
        return saveLevel(if (premium) current.copy(premium = newTrack) else current.copy(free = newTrack))
    }

    private fun loadQuests() {
        val questsPath = dataFolder.resolve("quests.yml")
        Files.createDirectories(questsPath.parent)

        if (!Files.exists(questsPath)) {
            createDefaultQuests(questsPath)
            return
        }

        try {
            val loader = createLoaderBuilder().path(questsPath).build()
            val node = loader.load()
            val loaded = mutableListOf<BattlePassQuestConfig>()
            node.childrenList().forEach { child ->
                child.get(BattlePassQuestConfig::class.java)?.let { loaded.add(it) }
            }
            if (loaded.isNotEmpty()) {
                questConfigs.clear()
                questConfigs.addAll(loaded)
            }
        } catch (e: Exception) {
            plugin.logger.warning("Failed to load quest configs: ${e.message}")
        }
    }

    private fun createDefaultQuests(questsPath: Path) {
        val defaults = listOf(
            // ── BREAK_BLOCK: добыча руд и камня ──────────────────────────
            BattlePassQuestConfig(
                type = "BREAK_BLOCK", required = 32, xp = 10, materials = listOf(Material.STONE),
                displayName = "<#ffecde>Сломать x32 камня".asMiniMessageComponent(),
                icon = ItemTemplate(type = Material.STONE_PICKAXE),
            ),
            BattlePassQuestConfig(
                type = "BREAK_BLOCK", required = 48, xp = 20,
                materials = listOf(Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE),
                displayName = "<#ffecde>Добыть x48 угольной руды".asMiniMessageComponent(),
                icon = ItemTemplate(type = Material.COAL_ORE),
            ),
            BattlePassQuestConfig(
                type = "BREAK_BLOCK", required = 32, xp = 30,
                materials = listOf(Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE),
                displayName = "<#ffecde>Добыть x32 железной руды".asMiniMessageComponent(),
                icon = ItemTemplate(type = Material.IRON_ORE),
            ),
            BattlePassQuestConfig(
                type = "BREAK_BLOCK", required = 24, xp = 35,
                materials = listOf(Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE, Material.NETHER_GOLD_ORE),
                displayName = "<#ffecde>Добыть x24 золотой руды".asMiniMessageComponent(),
                icon = ItemTemplate(type = Material.GOLD_ORE),
            ),
            BattlePassQuestConfig(
                type = "BREAK_BLOCK", required = 48, xp = 20,
                materials = listOf(Material.COPPER_ORE, Material.DEEPSLATE_COPPER_ORE),
                displayName = "<#ffecde>Добыть x48 медной руды".asMiniMessageComponent(),
                icon = ItemTemplate(type = Material.COPPER_ORE),
            ),
            BattlePassQuestConfig(
                type = "BREAK_BLOCK", required = 32, xp = 35,
                materials = listOf(Material.REDSTONE_ORE, Material.DEEPSLATE_REDSTONE_ORE),
                displayName = "<#ffecde>Добыть x32 редстоун-руды".asMiniMessageComponent(),
                icon = ItemTemplate(type = Material.REDSTONE_ORE),
            ),
            BattlePassQuestConfig(
                type = "BREAK_BLOCK", required = 24, xp = 35,
                materials = listOf(Material.LAPIS_ORE, Material.DEEPSLATE_LAPIS_ORE),
                displayName = "<#ffecde>Добыть x24 лазуритовой руды".asMiniMessageComponent(),
                icon = ItemTemplate(type = Material.LAPIS_ORE),
            ),
            BattlePassQuestConfig(
                type = "BREAK_BLOCK", required = 16, xp = 60,
                materials = listOf(Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE),
                displayName = "<#ffecde>Добыть x16 алмазной руды".asMiniMessageComponent(),
                icon = ItemTemplate(type = Material.DIAMOND_ORE),
            ),
            BattlePassQuestConfig(
                type = "BREAK_BLOCK", required = 4, xp = 100,
                materials = listOf(Material.ANCIENT_DEBRIS),
                displayName = "<#ffecde>Добыть x4 древних обломков".asMiniMessageComponent(),
                icon = ItemTemplate(type = Material.ANCIENT_DEBRIS),
            ),
            BattlePassQuestConfig(
                type = "BREAK_BLOCK", required = 16, xp = 45, materials = listOf(Material.OBSIDIAN),
                displayName = "<#ffecde>Сломать x16 обсидиана".asMiniMessageComponent(),
                icon = ItemTemplate(type = Material.OBSIDIAN),
            ),

            // ── MINE_BLOCK: массовая добыча строительных блоков ──────────
            BattlePassQuestConfig(
                type = "MINE_BLOCK", required = 96, xp = 15, materials = listOf(Material.NETHERRACK),
                displayName = "<#ffecde>Сломать x96 адского камня".asMiniMessageComponent(),
                icon = ItemTemplate(type = Material.NETHERRACK),
            ),
            BattlePassQuestConfig(
                type = "MINE_BLOCK", required = 64, xp = 10, materials = listOf(Material.COBBLESTONE),
                displayName = "<#ffecde>Сломать x64 булыжника".asMiniMessageComponent(),
                icon = ItemTemplate(type = Material.COBBLESTONE),
            ),
            BattlePassQuestConfig(
                type = "MINE_BLOCK", required = 64, xp = 12, materials = listOf(Material.SAND),
                displayName = "<#ffecde>Сломать x64 песка".asMiniMessageComponent(),
                icon = ItemTemplate(type = Material.SAND),
            ),
            BattlePassQuestConfig(
                type = "MINE_BLOCK", required = 32, xp = 15, materials = listOf(Material.GRAVEL),
                displayName = "<#ffecde>Сломать x32 гравия".asMiniMessageComponent(),
                icon = ItemTemplate(type = Material.GRAVEL),
            ),
            BattlePassQuestConfig(
                type = "MINE_BLOCK", required = 128, xp = 25, materials = listOf(Material.DEEPSLATE),
                displayName = "<#ffecde>Сломать x128 глубокосланца".asMiniMessageComponent(),
                icon = ItemTemplate(type = Material.DEEPSLATE),
            ),
            BattlePassQuestConfig(
                type = "MINE_BLOCK", required = 32, xp = 30, materials = listOf(Material.NETHER_QUARTZ_ORE),
                displayName = "<#ffecde>Добыть x32 кварцевой руды".asMiniMessageComponent(),
                icon = ItemTemplate(type = Material.NETHER_QUARTZ_ORE),
            ),

            // ── PLACE_BLOCK: постройки и освещение ───────────────────────
            BattlePassQuestConfig(
                type = "PLACE_BLOCK", required = 32, xp = 12, materials = listOf(Material.COBBLESTONE),
                displayName = "<#ffecde>Поставить x32 булыжника".asMiniMessageComponent(),
                icon = ItemTemplate(type = Material.COBBLESTONE),
            ),
            BattlePassQuestConfig(
                type = "PLACE_BLOCK", required = 16, xp = 15, materials = listOf(Material.GLASS),
                displayName = "<#ffecde>Поставить x16 стекла".asMiniMessageComponent(),
                icon = ItemTemplate(type = Material.GLASS),
            ),
            BattlePassQuestConfig(
                type = "PLACE_BLOCK", required = 24, xp = 20, materials = listOf(Material.TORCH),
                displayName = "<#ffecde>Поставить x24 факела".asMiniMessageComponent(),
                icon = ItemTemplate(type = Material.TORCH),
            ),
            BattlePassQuestConfig(
                type = "PLACE_BLOCK", required = 32, xp = 15, materials = listOf(Material.OAK_LOG),
                displayName = "<#ffecde>Поставить x32 дубовых бревна".asMiniMessageComponent(),
                icon = ItemTemplate(type = Material.OAK_LOG),
            ),
            BattlePassQuestConfig(
                type = "PLACE_BLOCK", required = 24, xp = 18, materials = listOf(Material.STONE_BRICKS),
                displayName = "<#ffecde>Поставить x24 каменного кирпича".asMiniMessageComponent(),
                icon = ItemTemplate(type = Material.STONE_BRICKS),
            ),
            BattlePassQuestConfig(
                type = "PLACE_BLOCK", required = 32, xp = 18, materials = listOf(Material.BRICKS),
                displayName = "<#ffecde>Поставить x32 кирпича".asMiniMessageComponent(),
                icon = ItemTemplate(type = Material.BRICKS),
            ),

            // ── KILL_ENTITY: охота на мобов ──────────────────────────────
            BattlePassQuestConfig(
                type = "KILL_ENTITY", required = 15, xp = 15, entities = listOf(EntityType.ZOMBIE),
                displayName = "<#ffecde>Убить x15 зомби".asMiniMessageComponent(),
                icon = ItemTemplate(type = Material.ROTTEN_FLESH),
            ),
            BattlePassQuestConfig(
                type = "KILL_ENTITY", required = 12, xp = 20, entities = listOf(EntityType.SKELETON),
                displayName = "<#ffecde>Убить x12 скелетов".asMiniMessageComponent(),
                icon = ItemTemplate(type = Material.BONE),
            ),
            BattlePassQuestConfig(
                type = "KILL_ENTITY", required = 10, xp = 25, entities = listOf(EntityType.CREEPER),
                displayName = "<#ffecde>Убить x10 криперов".asMiniMessageComponent(),
                icon = ItemTemplate(type = Material.GUNPOWDER),
            ),
            BattlePassQuestConfig(
                type = "KILL_ENTITY", required = 12, xp = 15, entities = listOf(EntityType.SPIDER),
                displayName = "<#ffecde>Убить x12 пауков".asMiniMessageComponent(),
                icon = ItemTemplate(type = Material.STRING),
            ),
            BattlePassQuestConfig(
                type = "KILL_ENTITY", required = 5, xp = 35, entities = listOf(EntityType.ENDERMAN),
                displayName = "<#ffecde>Убить x5 эндерменов".asMiniMessageComponent(),
                icon = ItemTemplate(type = Material.ENDER_PEARL),
            ),
            BattlePassQuestConfig(
                type = "KILL_ENTITY", required = 3, xp = 45, entities = listOf(EntityType.WITCH),
                displayName = "<#ffecde>Убить x3 ведьм".asMiniMessageComponent(),
                icon = ItemTemplate(type = Material.GLASS_BOTTLE),
            ),
            BattlePassQuestConfig(
                type = "KILL_ENTITY", required = 20, xp = 25, entities = listOf(EntityType.SLIME),
                displayName = "<#ffecde>Убить x20 слизней".asMiniMessageComponent(),
                icon = ItemTemplate(type = Material.SLIME_BALL),
            ),
            BattlePassQuestConfig(
                type = "KILL_ENTITY", required = 15, xp = 35, entities = listOf(EntityType.DROWNED),
                displayName = "<#ffecde>Убить x15 утопленников".asMiniMessageComponent(),
                icon = ItemTemplate(type = Material.TRIDENT),
            ),
            BattlePassQuestConfig(
                type = "KILL_ENTITY", required = 8, xp = 40, entities = listOf(EntityType.PILLAGER),
                displayName = "<#ffecde>Убить x8 разбойников".asMiniMessageComponent(),
                icon = ItemTemplate(type = Material.CROSSBOW),
            ),
            BattlePassQuestConfig(
                type = "KILL_ENTITY", required = 5, xp = 40, entities = listOf(EntityType.GHAST),
                displayName = "<#ffecde>Убить x5 гастов".asMiniMessageComponent(),
                icon = ItemTemplate(type = Material.GHAST_TEAR),
            ),

            // ── FISH: рыбалка ────────────────────────────────────────────
            BattlePassQuestConfig(
                type = "FISH", required = 8, xp = 15, materials = listOf(Material.COD),
                displayName = "<#ffecde>Поймать x8 трески".asMiniMessageComponent(),
                icon = ItemTemplate(type = Material.COD),
            ),
            BattlePassQuestConfig(
                type = "FISH", required = 6, xp = 18, materials = listOf(Material.SALMON),
                displayName = "<#ffecde>Поймать x6 лосося".asMiniMessageComponent(),
                icon = ItemTemplate(type = Material.SALMON),
            ),
            BattlePassQuestConfig(
                type = "FISH", required = 3, xp = 25, materials = listOf(Material.PUFFERFISH),
                displayName = "<#ffecde>Поймать x3 иглобрюха".asMiniMessageComponent(),
                icon = ItemTemplate(type = Material.PUFFERFISH),
            ),
            BattlePassQuestConfig(
                type = "FISH", required = 5, xp = 25, materials = listOf(Material.TROPICAL_FISH),
                displayName = "<#ffecde>Поймать x5 тропических рыб".asMiniMessageComponent(),
                icon = ItemTemplate(type = Material.TROPICAL_FISH),
            ),
            BattlePassQuestConfig(
                type = "FISH", required = 20, xp = 20, materials = listOf(),
                displayName = "<#ffecde>Поймать x20 рыб".asMiniMessageComponent(),
                icon = ItemTemplate(type = Material.FISHING_ROD),
            ),
            BattlePassQuestConfig(
                type = "FISH", required = 2, xp = 30,
                materials = listOf(Material.BOW, Material.ENCHANTED_BOOK, Material.FISHING_ROD, Material.NAME_TAG, Material.NAUTILUS_SHELL),
                displayName = "<#ffecde>Выловить x2 сокровища".asMiniMessageComponent(),
                icon = ItemTemplate(type = Material.ENCHANTED_BOOK),
            ),

            // ── CRAFT_ITEM: крафт полезных предметов ─────────────────────
            BattlePassQuestConfig(
                type = "CRAFT_ITEM", required = 32, xp = 15, materials = listOf(Material.TORCH),
                displayName = "<#ffecde>Скрафтить x32 факела".asMiniMessageComponent(),
                icon = ItemTemplate(type = Material.TORCH),
            ),
            BattlePassQuestConfig(
                type = "CRAFT_ITEM", required = 16, xp = 12, materials = listOf(Material.BREAD),
                displayName = "<#ffecde>Скрафтить x16 хлеба".asMiniMessageComponent(),
                icon = ItemTemplate(type = Material.BREAD),
            ),
            BattlePassQuestConfig(
                type = "CRAFT_ITEM", required = 4, xp = 15, materials = listOf(Material.FURNACE),
                displayName = "<#ffecde>Скрафтить x4 печи".asMiniMessageComponent(),
                icon = ItemTemplate(type = Material.FURNACE),
            ),
            BattlePassQuestConfig(
                type = "CRAFT_ITEM", required = 4, xp = 18, materials = listOf(Material.BUCKET),
                displayName = "<#ffecde>Скрафтить x4 ведра".asMiniMessageComponent(),
                icon = ItemTemplate(type = Material.BUCKET),
            ),
            BattlePassQuestConfig(
                type = "CRAFT_ITEM", required = 3, xp = 20, materials = listOf(Material.SHIELD),
                displayName = "<#ffecde>Скрафтить x3 щита".asMiniMessageComponent(),
                icon = ItemTemplate(type = Material.SHIELD),
            ),
            BattlePassQuestConfig(
                type = "CRAFT_ITEM", required = 3, xp = 15, materials = listOf(Material.STONE_PICKAXE),
                displayName = "<#ffecde>Скрафтить x3 каменные кирки".asMiniMessageComponent(),
                icon = ItemTemplate(type = Material.STONE_PICKAXE),
            ),
            BattlePassQuestConfig(
                type = "CRAFT_ITEM", required = 2, xp = 25, materials = listOf(Material.IRON_PICKAXE),
                displayName = "<#ffecde>Скрафтить x2 железные кирки".asMiniMessageComponent(),
                icon = ItemTemplate(type = Material.IRON_PICKAXE),
            ),
            BattlePassQuestConfig(
                type = "CRAFT_ITEM", required = 2, xp = 25, materials = listOf(Material.IRON_SWORD),
                displayName = "<#ffecde>Скрафтить x2 железных меча".asMiniMessageComponent(),
                icon = ItemTemplate(type = Material.IRON_SWORD),
            ),
            BattlePassQuestConfig(
                type = "CRAFT_ITEM", required = 96, xp = 25, materials = listOf(Material.ARROW),
                displayName = "<#ffecde>Скрафтить x96 стрел".asMiniMessageComponent(),
                icon = ItemTemplate(type = Material.ARROW),
            ),
            BattlePassQuestConfig(
                type = "CRAFT_ITEM", required = 8, xp = 45, materials = listOf(Material.GOLDEN_APPLE),
                displayName = "<#ffecde>Скрафтить x8 золотых яблок".asMiniMessageComponent(),
                icon = ItemTemplate(type = Material.GOLDEN_APPLE),
            ),

            // ── KILL_PLAYER: PvP ─────────────────────────────────────────
            BattlePassQuestConfig(
                type = "KILL_PLAYER", required = 1, xp = 25,
                displayName = "<#ffecde>Убить x1 игрока".asMiniMessageComponent(),
                icon = ItemTemplate(type = Material.IRON_SWORD),
            ),
            BattlePassQuestConfig(
                type = "KILL_PLAYER", required = 3, xp = 60,
                displayName = "<#ffecde>Убить x3 игроков".asMiniMessageComponent(),
                icon = ItemTemplate(type = Material.DIAMOND_SWORD),
            ),
        )

        val loader = createLoaderBuilder().path(questsPath).build()
        val node = loader.createNode()
        defaults.forEach { questConfig ->
            node.appendListNode().set(BattlePassQuestConfig::class.java, questConfig)
        }
        loader.save(node)
        questConfigs.clear()
        questConfigs.addAll(defaults)
    }
}
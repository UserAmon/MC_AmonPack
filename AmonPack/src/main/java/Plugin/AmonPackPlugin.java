package Plugin;

import java.io.File;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

//import AvatarSystems.ForestMenager;
import RPG.Crafting.CraftingMenager;
import RPG.Crafting.Objects.Craftable_Tool;
import RPG.Gathering.CombatMenager;
import RPG.Gathering.FarmMenager;

import RPG.Levels.BendingTree.Levels_Bending;
import RPG.Levels.PlayerLevelMenager;

import Plugin.Listeners;
//import RPG.UnUsed.Menagerie.MMORPG.GuiMenu;

import Plugin.SimpleWorldGenerator;
import RPG.UnUsed.Menagerie.UpgradesMenager;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.projectkorra.projectkorra.ProjectKorra;

import com.projectkorra.projectkorra.storage.SQLite;
import Plugin.*;
import Abilities.Bending.AbilitiesListener;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.Element.ElementType;
import com.projectkorra.projectkorra.Element.SubElement;
import com.projectkorra.projectkorra.ability.CoreAbility;

import static Abilities.Bending.SoundAbility.StartDeafnessTimer;

public class AmonPackPlugin extends JavaPlugin {
	public static AmonPackPlugin plugin;
	public static boolean BuildingOnArenas;
	public static SQLite sqlite;
	private static Element BladesElement;
	private static Element SmokeElement;
	private static Element SoundElement;
	private java.io.File configFile;
	public FileConfiguration config;

	public static boolean ENABLE_BENDING_ABILITIES = true;
	public static boolean ENABLE_RPG_SYSTEMS = true;
	public static boolean ENABLE_SLOW_PROGRESSION = ENABLE_RPG_SYSTEMS;
	public static boolean ENABLE_DATABASE = true;
	public static boolean ENABLE_SKILL_TREE = ENABLE_RPG_SYSTEMS && ENABLE_DATABASE;
	public static boolean ENABLE_DUNGEONS = ENABLE_RPG_SYSTEMS && ENABLE_DATABASE;
	public static boolean ENABLE_RPG_GATHERING = ENABLE_RPG_SYSTEMS && ENABLE_DATABASE;
	public static boolean ENABLE_BOUNTIES = ENABLE_RPG_SYSTEMS && ENABLE_DATABASE;
	public static boolean ENABLE_PARTY = ENABLE_RPG_SYSTEMS && ENABLE_DATABASE;
	public static boolean ENABLE_WORLD_GEN = ENABLE_RPG_SYSTEMS && ENABLE_DATABASE;
	public static boolean ENABLE_ARMOR_EFFECTS = ENABLE_RPG_SYSTEMS && ENABLE_DATABASE;

	public static CustomContent.Pack.PackManager packManager;
	public static CustomContent.Items.CustomItemManager customItemManager;
	public static CustomContent.Blocks.CustomBlockManager customBlockManager;
	public static CustomContent.Bosses.BossManager bossManager;
	public static RPG.Progression.ProgressionManager progressionManager;
	public static RPG.Magic.manager.ManaManager manaManager;
	public static RPG.Magic.manager.SpellRegistry spellRegistry;
	public static CustomContent.Guns.GunManager gunManager;

	@Override
	public FileConfiguration getConfig() {
		if (config == null) {
			reloadConfig();
		}
		return config;
	}

	@Override
	public void reloadConfig() {
		if (configFile == null) {
			configFile = new java.io.File(getDataFolder(), "abilities_config.yml");
		}
		config = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(configFile);

		java.io.InputStream defConfigStream = getResource("abilities_config.yml");
		if (defConfigStream != null) {
			org.bukkit.configuration.file.YamlConfiguration defConfig = org.bukkit.configuration.file.YamlConfiguration
					.loadConfiguration(
							new java.io.InputStreamReader(defConfigStream, java.nio.charset.StandardCharsets.UTF_8));
			config.setDefaults(defConfig);
		}
	}

	@Override
	public void saveConfig() {
		if (config == null || configFile == null) {
			return;
		}
		try {
			getConfig().save(configFile);
		} catch (java.io.IOException ex) {
			getLogger().log(java.util.logging.Level.SEVERE, "Could not save config to " + configFile, ex);
		}
	}

	@Override
	public void saveDefaultConfig() {
		if (configFile == null) {
			configFile = new java.io.File(getDataFolder(), "abilities_config.yml");
		}
		if (!configFile.exists()) {
			saveResource("abilities_config.yml", false);
		}
	}

	public void reloadPluginConfigurations() {
		getLogger().info("Rozpoczynanie pełnego przeładowania i odświeżania wszystkich konfiguracji AmonPack...");

		// 1. Ponowne wczytanie plików konfiguracyjnych z dysku
		reloadConfig();

		AbilitiesConfigFile = new File(getDataFolder(), "abilities_config.yml");
		if (AbilitiesConfigFile.exists()) {
			AbilitiesConfig = YamlConfiguration.loadConfiguration(AbilitiesConfigFile);
		}

		File rpgFolder = new File(getDataFolder(), "RPG");
		SkillTreeFile = new File(rpgFolder, "skilltree.yml");
		if (SkillTreeFile.exists()) {
			setSkillTreeConfig(YamlConfiguration.loadConfiguration(SkillTreeFile));
		}

		LevelConfigFile = new File(rpgFolder, "Levels.yml");
		if (LevelConfigFile.exists()) {
			LevelConfig = YamlConfiguration.loadConfiguration(LevelConfigFile);
		}

		DungeonConfigFile = new File(getDataFolder(), "dungeons/dungeon_config.yml");
		if (DungeonConfigFile.exists()) {
			setDungeonConfig(YamlConfiguration.loadConfiguration(DungeonConfigFile));
		}

		// 2. Przeładowanie skryptów i konfiguracji umiejętności Magii (Bending)
		if (ENABLE_BENDING_ABILITIES) {
			Abilities.PK_Abilities.Earth.SandWave.loadConfig();
			Abilities.PK_Abilities.Earth.SandBreath.loadConfig();
		}

		// 3. Przeładowanie systemów Gathering/Crafting/Boss/Bounties w ConfigsMenager
		if (configs_menager != null) {
			configs_menager.LoadAllConfigs();
			configs_menager.ReloadMenagers();
		}
		if (ENABLE_RPG_GATHERING) {
			farmmenager = new FarmMenager();
			combatMenager = new CombatMenager();
		}

		// 4. Przeładowanie Drzewka Skilli (Levels_Bending) oraz Poziomów
		// (PlayerLevelMenager)
		if (ENABLE_SKILL_TREE) {
			if (levelsBending != null) {
				levelsBending.LoadData();
			}
			if (PlayerMenager != null) {
				PlayerLevelMenager.EnabledSkillTypes.clear();
				if (LevelConfig != null) {
					try {
						for (String key : LevelConfig.getStringList("AmonPack.Levels.Enabled")) {
							PlayerLevelMenager.EnabledSkillTypes
									.add(RPG.Levels.Objects.LevelSkill.SkillType.valueOf(key));
						}
					} catch (Exception e) {
						getLogger().warning("Błąd podczas odświeżania EnabledSkillTypes: " + e.getMessage());
					}
				}
				PlayerMenager.CreateInventories();
			}
			try {
				new UpgradesMenager();
			} catch (Exception e) {
				getLogger().warning("Błąd odświeżania UpgradesMenager: " + e.getMessage());
			}
		}

		// 5. Przeładowanie Dungeonów i Menagerie
		if (ENABLE_DUNGEONS) {
			MenagerieConfigFile = getMenagerieFiles();
			setDungeonsConfig(MenagerieConfigFile);
			RPG.Dungeons.DungBuildManager.init();
			if (RPG.Dungeons.DungeonManager.getInstance() != null) {
				RPG.Dungeons.DungeonManager.getInstance().loadTemplates();
			}
		}

		// 6. Przeładowanie Bounties & BossScrollManager
		if (ENABLE_BOUNTIES && bountiesMenager != null) {
			bountiesMenager.ReloadConfig();
		}
		if (BossScrollManager.getInstance() != null) {
			BossScrollManager.getInstance().reloadConfig();
		}

		// 7. Przeładowanie Slow Progression
		if (ENABLE_SLOW_PROGRESSION && RPG.Progression.ProgressionManager.getInstance() != null) {
			RPG.Progression.ProgressionManager.getInstance().reload();
		}

		// 8. Przeładowanie Magii (Zaklęcia, Koszty Many, Cooldowny, Ulepszenia)
		loadMagicConfig();

		getLogger().info("Pełny reload konfiguracji oraz instancji AmonPack zakończony sukcesem!");
	}

	static List<File> MenagerieConfigFile;
	// static File PvPFile;
	static File LevelConfigFile;
	static File AbilitiesConfigFile;
	static File SkillTreeFile;
	static File configpath;

	public static Levels_Bending levelsBending;
	public static FarmMenager farmmenager;
	public static CombatMenager combatMenager;
	public static RPG.Bounties.BountiesMenager bountiesMenager;
	private static ConfigsMenager configs_menager;
	private static PlayerLevelMenager PlayerMenager;
	private static List<FileConfiguration> MenagerieConfig = new ArrayList<>();
	private static FileConfiguration LevelConfig;
	private static FileConfiguration AbilitiesConfig;
	private static FileConfiguration SkillTreeConfig;
	private static FileConfiguration DungeonConfig;
	private static File DungeonConfigFile;
	private static NamespacedKey upgradeKey;

	@Override
	public void onEnable() {
		BuildingOnArenas = false;
		plugin = this;
		getLogger().info("AmonPack włączony [Bending: " + ENABLE_BENDING_ABILITIES +
				", SkillTree: " + ENABLE_SKILL_TREE +
				", Dungeons: " + ENABLE_DUNGEONS +
				", RPG: " + ENABLE_RPG_GATHERING +
				", Bounties: " + ENABLE_BOUNTIES +
				", Party: " + ENABLE_PARTY +
				", Database: " + ENABLE_DATABASE + "]");

		configpath = getDataFolder();

		// --- 0. ZAPISYWANIE WSZYSTKICH ZASOBÓW Z RESOURCES NA DYSK ---
		if (!getDataFolder().exists()) {
			getDataFolder().mkdirs();
		}
		String[] resourcesToSave = {
				"abilities_config.yml",
				"pack/pack_config.yml",
				"pack/custom_items.yml",
				"pack/custom_blocks.yml",
				"pack/custom_bosses.yml",
				"pack/magic_config.yml",
				"progression/slow_progression.yml",
				"RPG/Levels.yml",
				"RPG/skilltree.yml",
				"RPG/Crafting_Items.yml",
				"RPG/Bounties.yml",
				"RPG/BossConfig.yml",
				"dungeons/dung_build.yml",
				"dungeons/dungeon_config.yml",
				"dungeons/przykladowy_dungeon.yml",
				"dungeons/makapu_bandyci.yml",
				"dungeons/dokumentacja_dungeonow.yml"
		};
		for (String res : resourcesToSave) {
			try {
				File file = new File(getDataFolder(), res);
				if (!file.exists()) {
					if (res.contains("/")) {
						file.getParentFile().mkdirs();
					}
					saveResource(res, false);
				}
			} catch (Exception e) {
				getLogger().warning("Nie udalo sie zapisac domyslnego zasobu: " + res + " - " + e.getMessage());
			}
		}
		saveDefaultDungeonResources();

		// --- 0.1 Wczytanie konfiguracji poziomów przed utworzeniem bazy SQL ---
		File rpgDir = new File(getDataFolder(), "RPG");
		if (!rpgDir.exists()) {
			rpgDir.mkdirs();
		}
		LevelConfigFile = new File(rpgDir, "Levels.yml");
		if (!LevelConfigFile.exists()) {
			saveResource("RPG/Levels.yml", false);
		}
		LevelConfig = YamlConfiguration.loadConfiguration(LevelConfigFile);
		try {
			java.io.InputStream defLevelsStream = getResource("RPG/Levels.yml");
			if (defLevelsStream == null) {
				defLevelsStream = getResource("Levels.yml");
			}
			if (defLevelsStream != null) {
				java.io.Reader defReader = new java.io.InputStreamReader(defLevelsStream,
						java.nio.charset.StandardCharsets.UTF_8);
				YamlConfiguration defLevels = YamlConfiguration.loadConfiguration(defReader);
				LevelConfig.setDefaults(defLevels);
				if (!LevelConfig.contains("AmonPack.Levels.DUNGEON")) {
					if (defLevels.contains("AmonPack.Levels.DUNGEON")) {
						LevelConfig.set("AmonPack.Levels.DUNGEON", defLevels.get("AmonPack.Levels.DUNGEON"));
					}
					List<String> enabled = LevelConfig.getStringList("AmonPack.Levels.Enabled");
					if (!enabled.contains("DUNGEON")) {
						enabled.add("DUNGEON");
						LevelConfig.set("AmonPack.Levels.Enabled", enabled);
					}
					LevelConfig.save(LevelConfigFile);
				}
			}
		} catch (Exception ignored) {
		}

		// --- 2. BAZA DANYCH SQLITE ---
		if (ENABLE_DATABASE) {
			try {
				sqlConnection();
			} catch (Exception e) {
				getLogger().severe("Nie udalo sie polaczyc z SQLite: " + e.getMessage());
				e.printStackTrace();
			}
		}

		// --- 1. RUCHY MAGICZNE (BENDING ABILITIES) ---
		if (ENABLE_BENDING_ABILITIES) {
			SmokeElement = new SubElement("Smoke", Element.FIRE, ElementType.BENDING, ProjectKorra.plugin);
			SoundElement = new SubElement("Sound", Element.AIR, ElementType.BENDING, ProjectKorra.plugin);
			CoreAbility.registerPluginAbilities(this, "Abilities.PK_Abilities");
			createconf();

			AbilitiesConfigFile = new File(getDataFolder(), "abilities_config.yml");
			AbilitiesConfig = YamlConfiguration.loadConfiguration(AbilitiesConfigFile);
			java.io.InputStream defAbilitiesStream = getResource("abilities_config.yml");
			if (defAbilitiesStream != null) {
				org.bukkit.configuration.file.YamlConfiguration defConfig = org.bukkit.configuration.file.YamlConfiguration
						.loadConfiguration(new java.io.InputStreamReader(defAbilitiesStream,
								java.nio.charset.StandardCharsets.UTF_8));
				AbilitiesConfig.setDefaults(defConfig);
			}
			Abilities.PK_Abilities.Earth.SandWave.loadConfig();
			Abilities.PK_Abilities.Earth.SandBreath.loadConfig();
			this.getServer().getPluginManager().registerEvents(new AbilitiesListener(), this);
			Abilities.Bending.SpecialTriggerManager.registerAdvancementPacketListener();
			Abilities.PK_Abilities.Chi.ChiManager.init();
			try {
				StartDeafnessTimer();
			} catch (Exception e) {
				getLogger().warning("Nie udało się uruchomić StartDeafnessTimer: " + e.getMessage());
			}
		}

		// --- 3. RPG GATHERING & CRAFTING ---
		if (ENABLE_RPG_GATHERING) {
			configs_menager = new ConfigsMenager(getDataFolder());
			configs_menager.CreateMenagers();

			File craftingItemsFile = new File(rpgDir, "Crafting_Items.yml");
			if (!craftingItemsFile.exists()) {
				saveResource("RPG/Crafting_Items.yml", false);
			}
			farmmenager = new FarmMenager();
			combatMenager = new CombatMenager();
		}

		// --- 4. SKILL TREE & LEVELS ---
		if (ENABLE_SKILL_TREE) {
			SkillTreeFile = new File(rpgDir, "skilltree.yml");
			if (!SkillTreeFile.exists()) {
				saveResource("RPG/skilltree.yml", false);
			}
			setSkillTreeConfig(YamlConfiguration.loadConfiguration(SkillTreeFile));
			saveSkillTreeConfig();

			LevelConfigFile = new File(rpgDir, "Levels.yml");
			if (!LevelConfigFile.exists()) {
				saveResource("RPG/Levels.yml", false);
			}
			LevelConfig = YamlConfiguration.loadConfiguration(LevelConfigFile);

			levelsBending = new Levels_Bending();
			try {
				PlayerMenager = new PlayerLevelMenager();
				PlayerMenager.CreateInventories();
				new UpgradesMenager();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		// --- 5. DUNGEONS & MENAGERIE ---
		if (ENABLE_DUNGEONS) {
			MenagerieConfigFile = getMenagerieFiles();
			File dungeonsDir = new File(getDataFolder(), "dungeons");
			if (!dungeonsDir.exists()) {
				dungeonsDir.mkdirs();
			}
			File exampleDungeonFile = new File(dungeonsDir, "przykladowy_dungeon.yml");
			if (!exampleDungeonFile.exists()) {
				saveResource("dungeons/przykladowy_dungeon.yml", false);
			}
			File docsFile = new File(dungeonsDir, "dokumentacja_dungeonow.yml");
			if (!docsFile.exists()) {
				saveResource("dungeons/dokumentacja_dungeonow.yml", false);
			}

			setDungeonsConfig(MenagerieConfigFile);

			DungeonConfigFile = new File(getDataFolder(), "dungeons/dungeon_config.yml");
			if (!DungeonConfigFile.exists()) {
				saveResource("dungeons/dungeon_config.yml", false);
			}
			setDungeonConfig(YamlConfiguration.loadConfiguration(DungeonConfigFile));

			this.getServer().getPluginManager().registerEvents(new RPG.Dungeons.DungBuildManager(), this);
			RPG.Dungeons.DungBuildManager.init();
			try {
				new RPG.Dungeons.DungeonManager();
				this.getServer().getPluginManager().registerEvents(RPG.Dungeons.DungeonManager.getInstance(), this);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		// --- 6. BOUNTIES ---
		if (ENABLE_BOUNTIES) {
			bountiesMenager = new RPG.Bounties.BountiesMenager();
			this.getServer().getPluginManager().registerEvents(bountiesMenager, this);
		}

		// --- 7. WORLD GENERATOR ---
		if (ENABLE_WORLD_GEN) {
			SimpleWorldGenerator.loadAllWorlds();
		}

		SaveConfigs();

		upgradeKey = new NamespacedKey(this, "playerUpgrade");
		Commands cmdExecutor = new Commands();

		// Rejestracja komend sterowana flagami
		if (ENABLE_RPG_GATHERING) {
			if (this.getCommand("Craft") != null)
				this.getCommand("Craft").setExecutor(cmdExecutor);
		}
		if (ENABLE_SKILL_TREE) {
			if (this.getCommand("Level") != null)
				this.getCommand("Level").setExecutor(cmdExecutor);
			if (this.getCommand("SelectElement") != null)
				this.getCommand("SelectElement").setExecutor(cmdExecutor);
		}
		if (this.getCommand("ArenaBuilding") != null)
			this.getCommand("ArenaBuilding").setExecutor(cmdExecutor);
		if (ENABLE_DUNGEONS) {
			if (this.getCommand("Menagerie") != null)
				this.getCommand("Menagerie").setExecutor(cmdExecutor);
			if (this.getCommand("Dungeons") != null) {
				this.getCommand("Dungeons").setExecutor(cmdExecutor);
				this.getCommand("Dungeons").setTabCompleter(cmdExecutor);
			}
			if (this.getCommand("dungbuild") != null)
				this.getCommand("dungbuild").setExecutor(cmdExecutor);
		}
		if (this.getCommand("Reload") != null)
			this.getCommand("Reload").setExecutor(cmdExecutor);
		if (this.getCommand("amonpack") != null)
			this.getCommand("amonpack").setExecutor(cmdExecutor);
		if (ENABLE_BOUNTIES) {
			if (this.getCommand("Bounties") != null)
				this.getCommand("Bounties").setExecutor(cmdExecutor);
		}
		if (ENABLE_PARTY) {
			if (this.getCommand("party") != null) {
				this.getCommand("party").setExecutor(cmdExecutor);
				this.getCommand("party").setTabCompleter(cmdExecutor);
			}
			if (this.getCommand("p") != null)
				this.getCommand("p").setExecutor(cmdExecutor);
		}

		if (ENABLE_SKILL_TREE || ENABLE_RPG_GATHERING || ENABLE_DUNGEONS) {
			this.getServer().getPluginManager().registerEvents(new Listeners(), this);
			this.getServer().getPluginManager().registerEvents(new RPG.Crafting.BowMagicEffectsListener(), this);
		}

		if (ENABLE_ARMOR_EFFECTS) {
			new ArmorEffectsRunnable().runTaskTimer(this, 0, 20);
		}

		// --- 6. AUTORSKI RESOURCE PACK & CUSTOM CONTENT ---
		packManager = new CustomContent.Pack.PackManager();
		customItemManager = new CustomContent.Items.CustomItemManager(packManager);
		customBlockManager = new CustomContent.Blocks.CustomBlockManager(packManager, customItemManager);
		bossManager = new CustomContent.Bosses.BossManager(packManager);

		customItemManager.load();
		customBlockManager.load();
		bossManager.load();
		packManager.load();

		this.getServer().getPluginManager().registerEvents(new CustomContent.Pack.PackListener(packManager), this);
		this.getServer().getPluginManager()
				.registerEvents(new CustomContent.Items.CustomItemListener(customItemManager), this);
		this.getServer().getPluginManager().registerEvents(
				new CustomContent.Blocks.CustomBlockListener(customBlockManager, customItemManager), this);
		this.getServer().getPluginManager()
				.registerEvents(new CustomContent.Blocks.OreWorldGenerator(customBlockManager), this);
		this.getServer().getPluginManager()
				.registerEvents(new CustomContent.Bosses.BossListener(bossManager, customItemManager), this);
		this.getServer().getPluginManager().registerEvents(new CustomContent.Commands.DebugToolListener(packManager,
				customItemManager, customBlockManager, bossManager), this);

		if (this.getCommand("amon") != null) {
			CustomContent.Commands.AmonCommand amonCmd = new CustomContent.Commands.AmonCommand(packManager,
					customItemManager, customBlockManager, bossManager);
			this.getCommand("amon").setExecutor(amonCmd);
			this.getCommand("amon").setTabCompleter(
					new CustomContent.Commands.AmonTabCompleter(customItemManager, customBlockManager, bossManager));
		}

		// --- 6b. SYSTEM BRONI PALNEJ I RUSZNIKARNI ---
		CustomContent.Guns.GunConfigManager.getInstance();
		gunManager = new CustomContent.Guns.GunManager();
		this.getServer().getPluginManager().registerEvents(new CustomContent.Guns.GunListener(gunManager), this);
		this.getServer().getPluginManager().registerEvents(new CustomContent.Guns.GunsmithGui(), this);
		this.getServer().getPluginManager().registerEvents(new CustomContent.Guns.GunsmithManager(customBlockManager), this);

		// --- 7. SYSTEM MAGII I MANY ---
		spellRegistry = new RPG.Magic.manager.SpellRegistry();
		manaManager = new RPG.Magic.manager.ManaManager();
		manaManager.start();
		RPG.Magic.elements.ElementStatusManager.init();
		loadMagicConfig();
		this.getServer().getPluginManager().registerEvents(
				new RPG.Magic.listener.MagicItemListener(spellRegistry, manaManager, customItemManager), this);

		// --- 8. SYSTEM SLOW PROGRESSION ---
		if (ENABLE_SLOW_PROGRESSION) {
			progressionManager = new RPG.Progression.ProgressionManager();
			progressionManager.load();
		}

		System.out.println("Amonpack Załadowany!");
	}

	public static FileConfiguration magicConfig;
	private static File magicConfigFile;

	public void loadMagicConfig() {
		try {
			if (magicConfigFile == null) {
				magicConfigFile = new File(getDataFolder(), "pack/magic_config.yml");
			}
			if (!magicConfigFile.exists()) {
				magicConfigFile.getParentFile().mkdirs();
				saveResource("pack/magic_config.yml", false);
			}
			magicConfig = YamlConfiguration.loadConfiguration(magicConfigFile);
			if (spellRegistry != null) {
				spellRegistry.loadConfig(magicConfig);
			}
			getLogger().info("Pomyślnie załadowano konfigurację magii z pack/magic_config.yml!");
		} catch (Exception e) {
			getLogger().warning("Błąd podczas ładowania magic_config.yml: " + e.getMessage());
		}
	}

	@Override
	public void onDisable() {
		try {
			if (manaManager != null) {
				manaManager.stop();
			}
			if (ENABLE_SLOW_PROGRESSION && progressionManager != null) {
				progressionManager.unload();
			}
			if (bossManager != null) {
				bossManager.unload();
			}
			if (customBlockManager != null) {
				customBlockManager.unload();
			}
			if (packManager != null) {
				packManager.unload();
			}

			Abilities.PK_Abilities.Chi.ChiManager.stop();
			if (ENABLE_DUNGEONS && RPG.Dungeons.DungeonManager.getInstance() != null) {
				RPG.Dungeons.DungeonManager.getInstance().cleanupAll();
			}
			if (ENABLE_SKILL_TREE && PlayerMenager != null) {
				PlayerMenager.LoadIntoDatabase();
			}
			if (ENABLE_BOUNTIES && bountiesMenager != null) {
				bountiesMenager.SaveAll();
			}
			SaveConfigs();
			if (ENABLE_DATABASE && sqlite != null) {
				sqlite.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		getLogger().info("AmonPack wyłączony");
	}

	public static void setPlayerUpgrade(Player player, List<String> upgrades) {
		String upgradesString = String.join(",", upgrades);
		player.getPersistentDataContainer().set(upgradeKey, PersistentDataType.STRING, upgradesString);
	}

	public static List<String> getPlayerUpgrades(Player player) {
		String upgradesString = player.getPersistentDataContainer().get(upgradeKey, PersistentDataType.STRING);
		if (upgradesString != null && !upgradesString.isEmpty()) {
			return Arrays.asList(upgradesString.split(","));
		} else {
			return new ArrayList<>();
		}
	}

	public static SQLite mysqllite() {
		return sqlite;
	}

	public static void BuildingOff() {
		BuildingOnArenas = false;
	}

	public static void BuildingOn() {
		BuildingOnArenas = true;
	}

	public void createconf() {
		saveDefaultConfig();
		getConfig().options().copyDefaults(true);
		saveConfig();
	}

	public static void saveSkillTreeConfig() {
		try {
			if (getSkillTreeConfig() != null && SkillTreeFile != null) {
				getSkillTreeConfig().save(SkillTreeFile);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void sqlConnection() {
		sqlite = new SQLite(plugin.getLogger(), "AmonPackSQL.db", plugin.getDataFolder().getAbsolutePath());
		try {
			if (sqlite.open() != null) {
				getLogger().info("Baza danych połączona!");
			}
			ExecuteQuery("CREATE TABLE IF NOT EXISTS BendingTree (Player VARCHAR(50) PRIMARY KEY," +
					" AirPoints INT," +
					" FirePoints INT," +
					" WaterPoints INT," +
					" EarthPoints INT," +
					" ChiPoints INT," +
					" UnlockedAbilities TEXT," +
					" CurrentElement TEXT," +
					" AllElements TEXT," +
					" SwapAbility TEXT," +
					" DropAbility TEXT" +
					")");
			ensureColumnExists("BendingTree", "ChiPoints");
			ensureColumnExists("BendingTree", "SwapAbility");
			ensureColumnExists("BendingTree", "DropAbility");
			ExecuteQuery(
					"CREATE TABLE IF NOT EXISTS SpellTree (Player VARCHAR(50) PRIMARY KEY, SkillPoint INT, Path TEXT, Element TEXT, AllElements TEXT)");
			ExecuteQuery(
					"CREATE TABLE IF NOT EXISTS Reputation (Player VARCHAR(50) PRIMARY KEY, RepLvL1 INT, RepLvL2 INT, RepLvL3 INT, RepLvL4 INT, RepLvL5 INT, RepLvL6 INT, RepLvL7 INT)");
			ExecuteQuery(
					"CREATE TABLE IF NOT EXISTS Jobs (Player VARCHAR(50) PRIMARY KEY, Job1 INT, Job2 INT, Job3 INT, Job4 INT)");
			ExecuteQuery(
					"CREATE TABLE IF NOT EXISTS Parties (party_id VARCHAR(36) PRIMARY KEY, leader_uuid VARCHAR(36), friendly_fire INT)");
			ExecuteQuery(
					"CREATE TABLE IF NOT EXISTS PartyMembers (player_uuid VARCHAR(36) PRIMARY KEY, party_id VARCHAR(36))");
			ExecuteQuery("CREATE TABLE IF NOT EXISTS LevelGENERAL"
					+ " (Player VARCHAR(50) PRIMARY KEY, GeneralLevel DOUBLE, UsedRewards VARCHAR(100), UpgradePercent DOUBLE)");
			if (LevelConfig != null) {
				for (String key : LevelConfig.getStringList("AmonPack.Levels.Enabled")) {
					ExecuteQuery("CREATE TABLE IF NOT EXISTS Level" + key
							+ " (Player VARCHAR(50) PRIMARY KEY, GeneralLevel DOUBLE, UsedRewards VARCHAR(100),UpgradePercent DOUBLE)");
				}
			}
		} catch (Exception e) {
			plugin.getLogger().info(e.getMessage());
			getPluginLoader().disablePlugin(plugin);
		}
	}

	private void ensureColumnExists(String table, String column) {
		try {
			if (sqlite != null && sqlite.getConnection() != null) {
				Statement stmt = sqlite.getConnection().createStatement();
				ResultSet rs = stmt.executeQuery("PRAGMA table_info(" + table + ");");
				boolean exists = false;
				while (rs.next()) {
					if (column.equalsIgnoreCase(rs.getString("name"))) {
						exists = true;
						break;
					}
				}
				rs.close();
				stmt.close();
				if (!exists) {
					ExecuteQuery("ALTER TABLE " + table + " ADD COLUMN " + column + " TEXT;");
				}
			}
		} catch (Exception ignored) {
		}
	}

	public static void ExecuteQuery(String query) {
		try {
			if (sqlite != null && sqlite.getConnection() != null) {
				Statement stmt = sqlite.getConnection().createStatement();
				stmt.executeUpdate(query);
				stmt.close();
			}
		} catch (Exception var3) {
			PrintStream var10000 = System.err;
			String var10001 = var3.getClass().getName();
			var10000.println(var10001 + ": " + var3.getMessage());
			var3.printStackTrace();
		}
	}

	public static void SaveConfigs() {
		try {
			if (ENABLE_SKILL_TREE && LevelConfigFile != null && LevelConfig != null) {
				if (!LevelConfig.contains("AmonPack")) {
					LevelConfig.set("AmonPack.Levels.GENERAL.Gui.Place", 4);
					LevelConfig.set("AmonPack.Levels.GENERAL.Gui.Title", ChatColor.GOLD + "Poziom Ogólny: ");
					LevelConfig.set("AmonPack.Levels.MINING.Gui.Place", 20);
					LevelConfig.set("AmonPack.Levels.MINING.Gui.Title", ChatColor.GOLD + "Doświadczenie w Kopalni: ");
					LevelConfig.set("AmonPack.Levels.COMBAT.Gui.Place", 22);
					LevelConfig.set("AmonPack.Levels.COMBAT.Gui.Title",
							ChatColor.GOLD + "Doświadczenie w Strefie Walki: ");
					LevelConfig.set("AmonPack.Levels.DUNGEON.Gui.Place", 24);
					LevelConfig.set("AmonPack.Levels.DUNGEON.Gui.Title",
							ChatColor.GOLD + "Poziom Eksploracji Dungeonów: ");
				}
				LevelConfig.save(LevelConfigFile);
			}
			if (ENABLE_BENDING_ABILITIES && AbilitiesConfigFile != null && AbilitiesConfig != null) {
				AbilitiesConfig.save(AbilitiesConfigFile);
			}
		} catch (Exception e) {
			System.out.println("Błąd z konfigiem! " + e.getMessage());
		}
	}

	public static Element getBladesElement() {
		return BladesElement;
	}

	public static Element getSmokeElement() {
		return SmokeElement;
	}

	public static Element getSoundElement() {
		return SoundElement;
	}

	public static void reloadAllConfigs() {
		try {
			if (ENABLE_BENDING_ABILITIES) {
				plugin.reloadConfig();
				AbilitiesConfig = YamlConfiguration.loadConfiguration(new File(configpath, "abilities_config.yml"));
				java.io.InputStream defAbilitiesStream = plugin.getResource("abilities_config.yml");
				if (defAbilitiesStream != null) {
					org.bukkit.configuration.file.YamlConfiguration defConfig = org.bukkit.configuration.file.YamlConfiguration
							.loadConfiguration(new java.io.InputStreamReader(defAbilitiesStream,
									java.nio.charset.StandardCharsets.UTF_8));
					AbilitiesConfig.setDefaults(defConfig);
				}
				Abilities.PK_Abilities.Earth.SandWave.loadConfig();
				Abilities.PK_Abilities.Earth.SandBreath.loadConfig();
			}

			if (ENABLE_SKILL_TREE) {
				if (configpath != null) {
					File rpgDir = new File(configpath, "RPG");
					LevelConfig = YamlConfiguration.loadConfiguration(new File(rpgDir, "Levels.yml"));
					SkillTreeConfig = YamlConfiguration.loadConfiguration(new File(rpgDir, "skilltree.yml"));
				}
				if (levelsBending != null) {
					levelsBending.LoadData();
				}
			}

			if (ENABLE_DUNGEONS) {
				setDungeonsConfig(getMenagerieFilesReload());
				if (configpath != null) {
					DungeonConfig = YamlConfiguration
							.loadConfiguration(new File(configpath, "dungeons/dungeon_config.yml"));
				}
			}

			if (ENABLE_RPG_GATHERING) {
				if (farmmenager != null)
					farmmenager.ReloadConfig();
				if (combatMenager != null)
					combatMenager.ReloadConfig();
				if (configs_menager != null) {
					configs_menager.LoadAllConfigs();
					configs_menager.ReloadMenagers();
				}
			}

			System.out.println("pomyślnie zrobiono reload!");
		} catch (Exception e) {
			System.out.println("ERROR!!!  " + e);
			System.out.println("ERROR!!!  " + e.getMessage());
		}
	}

	public static List<FileConfiguration> GetMenagerieConfig() {
		return MenagerieConfig;
	}

	public static FileConfiguration getSkillTreeConfig() {
		return SkillTreeConfig;
	}

	public static FileConfiguration getDungeonConfig() {
		return DungeonConfig;
	}

	public static void setDungeonConfig(FileConfiguration DungeonConfig) {
		AmonPackPlugin.DungeonConfig = DungeonConfig;
	}

	public static void setDungeonsConfig(List<File> DungeonsConfig) {
		MenagerieConfig.clear();
		for (File f : DungeonsConfig) {
			MenagerieConfig.add(YamlConfiguration.loadConfiguration(f));
		}
	}

	public void setSkillTreeConfig(FileConfiguration SkillTreeConfig) {
		AmonPackPlugin.SkillTreeConfig = SkillTreeConfig;
	}

	/*
	 * public static FileConfiguration getPvPConfig() {
	 * return PvPConfig;
	 * }
	 * public void setPvPConfig(FileConfiguration PvPConfig) {
	 * AmonPackPlugin.PvPConfig = PvPConfig;
	 * }
	 */
	public List<File> getMenagerieFiles() {
		File menageriesFolder = new File(getDataFolder(), "Menageries");
		List<File> configs = new ArrayList<>();
		if (menageriesFolder.exists() && menageriesFolder.isDirectory()) {
			File[] files = menageriesFolder
					.listFiles((dir, name) -> name.startsWith("Menagerie") && name.endsWith(".yml"));
			if (files != null) {
				configs.addAll(Arrays.asList(files));
			}
		}
		return configs;
	}

	public static List<File> getMenagerieFilesReload() {
		File menageriesFolder = new File(configpath, "Menageries");
		List<File> configs = new ArrayList<>();
		if (menageriesFolder.exists() && menageriesFolder.isDirectory()) {
			File[] files = menageriesFolder
					.listFiles((dir, name) -> name.startsWith("Menagerie") && name.endsWith(".yml"));
			if (files != null) {
				configs.addAll(Arrays.asList(files));
			}
		}
		return configs;
	}

	public static PlayerLevelMenager getPlayerMenager() {
		return PlayerMenager;
	}

	public static FileConfiguration getLevelConfig() {
		return LevelConfig;
	}

	public static FileConfiguration getAbilitiesConfig() {
		if (AbilitiesConfig == null && plugin != null) {
			File f = new File(plugin.getDataFolder(), "abilities_config.yml");
			if (f.exists()) {
				AbilitiesConfig = YamlConfiguration.loadConfiguration(f);
			}
		}
		return AbilitiesConfig;
	}

	public static ConfigsMenager getConfigs_menager() {
		return configs_menager;
	}

	/*
	 * public static FileConfiguration getForestConfig() {
	 * return ForestConfig;
	 * }
	 */
	public static ItemStack FastEasyStack(Material mat, String name) {
		ItemStack TempItem = new ItemStack(mat);
		ItemMeta IMeta = TempItem.getItemMeta();
		IMeta.setDisplayName(name);
		TempItem.setItemMeta(IMeta);
		return TempItem;
	}

	public static ItemStack FastEasyStack(Material mat, String name, int modelil) {
		ItemStack TempItem = new ItemStack(mat);
		ItemMeta IMeta = TempItem.getItemMeta();
		IMeta.setCustomModelData(modelil);
		IMeta.setDisplayName(name);
		TempItem.setItemMeta(IMeta);
		return TempItem;
	}

	public static ItemStack FastEasyStackWithLore(Material mat, String name, List<String> lore) {
		ItemStack TempItem = new ItemStack(mat);
		ItemMeta IMeta = TempItem.getItemMeta();
		IMeta.setDisplayName(name);
		IMeta.setLore(lore);
		TempItem.setItemMeta(IMeta);
		return TempItem;
	}

	public static ItemStack FastEasyStackWithLoreModelData(Material mat, String name, List<String> lore,
			int modeldata) {
		ItemStack TempItem = new ItemStack(mat);
		ItemMeta IMeta = TempItem.getItemMeta();
		IMeta.setDisplayName(name);
		IMeta.setCustomModelData(modeldata);
		IMeta.setLore(lore);
		TempItem.setItemMeta(IMeta);
		return TempItem;
	}

	public static Element ElementBasedOnSubElement(Element SubElement) {
		for (Element element : Element.getAllElements()) {
			if (element.equals(SubElement)) {
				return element;
			}
			for (Element subele : Element.getSubElements(element)) {
				if (subele.equals(SubElement)) {
					return element;
				}
			}
		}
		return null;
	}

	private void saveDefaultDungeonResources() {
		File dungeonsDir = new File(getDataFolder(), "dungeons");
		if (!dungeonsDir.exists()) {
			dungeonsDir.mkdirs();
		}
		try {
			java.net.URL src = getFile().toURI().toURL();
			java.util.zip.ZipInputStream zip = new java.util.zip.ZipInputStream(src.openStream());
			java.util.zip.ZipEntry entry;
			while ((entry = zip.getNextEntry()) != null) {
				String name = entry.getName();
				if (name.startsWith("dungeons/") && !entry.isDirectory()
						&& (name.endsWith(".yml") || name.endsWith(".yaml"))) {
					File target = new File(getDataFolder(), name);
					if (!target.exists()) {
						saveResource(name, false);
						getLogger().info("[Dungeons] Zapisano domyślny plik lochu: " + name);
					}
				}
			}
			zip.close();
		} catch (Exception e) {
			getLogger().warning("Dynamiczne skanowanie folderu dungeons w JAR nie powiodło się: " + e.getMessage());
		}
	}

	public RPG.Magic.manager.SpellRegistry getSpellRegistry() {
		return spellRegistry;
	}

	public RPG.Magic.manager.ManaManager getManaManager() {
		return manaManager;
	}

}

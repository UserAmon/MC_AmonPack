package Plugin;

import java.io.File;
import java.io.PrintStream;
import java.sql.Connection;
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
	public static Plugin plugin;
	public static boolean BuildingOnArenas;
	public static SQLite sqlite;
	private static Element BladesElement;
	private static Element SmokeElement;
	private static Element SoundElement;
	private java.io.File configFile;
	public FileConfiguration config;

	public static boolean ENABLE_BENDING_ABILITIES = true;
	public static boolean ENABLE_SKILL_TREE, ENABLE_DUNGEONS, ENABLE_RPG_GATHERING, ENABLE_BOUNTIES, ENABLE_PARTY,
			ENABLE_DATABASE, ENABLE_WORLD_GEN, ENABLE_ARMOR_EFFECTS = true;

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
			"BossConfig.yml",
			"Bounties.yml",
			"Crafting_Items.yml",
			"dung_build.yml",
			"skilltree.yml",
			"Levels.yml",
			"dungeons/dungeon_config.yml",
			"dungeons/przykladowy_dungeon.yml",
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

			File craftingItemsFile = new File(getDataFolder(), "Crafting_Items.yml");
			if (!craftingItemsFile.exists()) {
				saveResource("Crafting_Items.yml", false);
			}
			farmmenager = new FarmMenager();
			combatMenager = new CombatMenager();
		}

		// --- 4. SKILL TREE & LEVELS ---
		if (ENABLE_SKILL_TREE) {
			SkillTreeFile = new File(getDataFolder(), "skilltree.yml");
			if (!SkillTreeFile.exists()) {
				saveResource("skilltree.yml", false);
			}
			setSkillTreeConfig(YamlConfiguration.loadConfiguration(SkillTreeFile));
			saveSkillTreeConfig();

			LevelConfigFile = new File(getDataFolder(), "Levels.yml");
			if (!LevelConfigFile.exists()) {
				saveResource("Levels.yml", false);
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
		}

		if (ENABLE_ARMOR_EFFECTS) {
			new ArmorEffectsRunnable().runTaskTimer(this, 0, 20);
		}

		System.out.println("Amonpack Załadowany!");
	}

	@Override
	public void onDisable() {
		try {
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
					" UnlockedAbilities TEXT," +
					" CurrentElement TEXT," +
					" AllElements TEXT," +
					" SwapAbility TEXT," +
					" DropAbility TEXT" +
					")");
			try { ExecuteQuery("ALTER TABLE BendingTree ADD COLUMN SwapAbility TEXT;"); } catch (Exception ignored) {}
			try { ExecuteQuery("ALTER TABLE BendingTree ADD COLUMN DropAbility TEXT;"); } catch (Exception ignored) {}
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
					LevelConfig = YamlConfiguration.loadConfiguration(new File(configpath, "Levels.yml"));
					SkillTreeConfig = YamlConfiguration.loadConfiguration(new File(configpath, "skilltree.yml"));
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

}

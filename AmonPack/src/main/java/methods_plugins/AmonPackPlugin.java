package methods_plugins;

import java.io.File;
import java.io.PrintStream;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import AvatarSystems.ForestMenager;
import AvatarSystems.PlayerLevelMenager;
import AvatarSystems.Util_Objects.LevelSkill;
import Mechanics.Listeners;
import Mechanics.MMORPG.GuiMenu;
import Mechanics.MMORPG.Puzzle;
import Mechanics.MMORPG.ReputationMenager;
import Mechanics.PVE.Menagerie.MenagerieMenager;
import Mechanics.PVE.Mining;
import Mechanics.PVE.SimpleWorldGenerator;
import Mechanics.PVP.newPvP;
import Mechanics.QuestItems;
import Mechanics.Skills.BendingGuiMenu;
import Mechanics.Skills.JobsMenager;
import Mechanics.Skills.UpgradesMenager;
import com.projectkorra.projectkorra.ProjectKorra;
import com.projectkorra.projectkorra.storage.SQLite;
import commands.*;
import methods_plugins.Abilities.AbilitiesListener;
import methods_plugins.Abilities.BladesAbility;
import methods_plugins.Abilities.SoundAbility;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.Element.ElementType;
import com.projectkorra.projectkorra.Element.SubElement;
import com.projectkorra.projectkorra.ability.CoreAbility;

import static methods_plugins.Abilities.SoundAbility.StartDeafnessTimer;


public class AmonPackPlugin extends JavaPlugin {
	public static Plugin plugin;
	JavaPlugin jp = this;
	public static boolean MiningAndGatheringOn;
	public static boolean BuildingOnArenas;
	public static boolean PvPEnabled;
	public static SQLite sqlite;
    private static Element BladesElement;
    private static Element SmokeElement;
	public FileConfiguration config = getConfig();
	static File newConfig;
	static List<File> MenagerieConfigFile;
	static File PvPFile;
	static File MinesConfigFile;
	static File LevelConfigFile;
	static File ForestConfigFile;
	static File AbilitiesConfigFile;
	static File SkillTreeFile;
	static File GuiFile;
	private static PlayerLevelMenager PlayerMenager;
	//static File WaveFile;
	private static FileConfiguration newConfigz;
	private static List<FileConfiguration> MenagerieConfig = new ArrayList<>();
	private static FileConfiguration PvPConfig;
	private static FileConfiguration ForestConfig;
	private static FileConfiguration LevelConfig;
	private static FileConfiguration AbilitiesConfig;
	private static FileConfiguration MinesConfig;
	private static FileConfiguration SkillTreeConfig;
	private static FileConfiguration GuiConfig;
	private static MenagerieMenager MenaMenager;
	//private static FileConfiguration WaveConfig;
	private static NamespacedKey upgradeKey;
	static File configpath;


    @Override
    public void onEnable() {
		MiningAndGatheringOn = true;
		BuildingOnArenas = false;
		PvPEnabled = true;
    	plugin = this;
        getLogger().info("AmonPack włączony");
        CoreAbility.registerPluginAbilities(jp, "abilities");
        BladesElement = new SubElement("Blades", Element.CHI, ElementType.BLOCKING, this);
        SmokeElement = new SubElement("Smoke", Element.FIRE, ElementType.BENDING, ProjectKorra.plugin);
		createconf();
		PvPFile = new File(getDataFolder(), "PvPConfig.yml");
		setPvPConfig(YamlConfiguration.loadConfiguration(PvPFile));
		newConfig = new File(getDataFolder(), "newconfig.yml");
		setNewConfigz(YamlConfiguration.loadConfiguration(newConfig));
		MenagerieConfigFile = getMenagerieFiles();
		MinesConfigFile = new File(getDataFolder(), "MinesConfigFile.yml");
		SkillTreeFile = new File(getDataFolder(), "SkillTreeConfig.yml");
		GuiFile = new File(getDataFolder(), "GuiConfig.yml");
		setDungeonsConfig(MenagerieConfigFile);
		configpath = getDataFolder();

		LevelConfigFile = new File(getDataFolder(), "Levels.yml");
		LevelConfig = YamlConfiguration.loadConfiguration(LevelConfigFile);

		ForestConfigFile = new File(getDataFolder() + File.separator + "RPG", "Forest.yml");
		ForestConfig = YamlConfiguration.loadConfiguration(ForestConfigFile);

		AbilitiesConfigFile = new File(getDataFolder(), "AbilitiesConfig.yml");
		AbilitiesConfig = YamlConfiguration.loadConfiguration(AbilitiesConfigFile);
		sqlConnection();
		setMinesConfigFile(YamlConfiguration.loadConfiguration(MinesConfigFile));
		setSkillTreeConfig(YamlConfiguration.loadConfiguration(SkillTreeFile));
		setGuiConfig(YamlConfiguration.loadConfiguration(GuiFile));
		SaveConfigs();
		savePvPConfig();
		saveNewConfig();
		saveSkillTreeConfig();
		saveGuiConfig();
		saveDungeonConfig();
		SimpleWorldGenerator.loadAllWorlds();
		saveMinesConfig();
        BladesAbility.CreateSwords();
		new QuestItems();
		upgradeKey = new NamespacedKey(this, "playerUpgrade");
        this.getCommand("MinGathOn").setExecutor(new Commands());
        this.getCommand("MinGathOff").setExecutor(new Commands());
        this.getCommand("QuestItems").setExecutor(new Commands());
        this.getCommand("Skills").setExecutor(new Commands());
        this.getCommand("Level").setExecutor(new Commands());
        this.getCommand("puzzle").setExecutor(new Puzzle());
        this.getCommand("Pomoc").setExecutor(new Commands());
		this.getCommand("ArenaBuilding").setExecutor(new Commands());
		this.getCommand("Menagerie").setExecutor(new Commands());
		this.getCommand("Menagerie").setTabCompleter(new CommandsTabMenager());
		this.getCommand("SpellTree").setExecutor(new Commands());
		this.getCommand("ArenaBuilding").setTabCompleter(new CommandsTabMenager());
		this.getCommand("PvP").setExecutor(new Commands());
		this.getCommand("Reload").setExecutor(new Commands());
		this.getServer().getPluginManager().registerEvents(new AbilitiesListener(), this);
		this.getServer().getPluginManager().registerEvents(new Puzzle(), this);
		this.getServer().getPluginManager().registerEvents(new Mining(), this);
		this.getServer().getPluginManager().registerEvents(new ReputationMenager(), this);
		this.getServer().getPluginManager().registerEvents(new GuiMenu(), this);
		this.getServer().getPluginManager().registerEvents(new Listeners(), this);
		new newPvP();
		try {
			StartDeafnessTimer();
			PlayerMenager = new PlayerLevelMenager();
			PlayerMenager.CreateInventories();
			new ForestMenager();
			new BendingGuiMenu();
			new JobsMenager();
			MenaMenager = new MenagerieMenager();
			new UpgradesMenager();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		System.out.println("Amonpack Załadowany");
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

    public static void off() {
		MiningAndGatheringOn = false;
		plugin.getLogger().info("Off");
    }
	public static void on() {
		MiningAndGatheringOn = true;
		plugin.getLogger().info("On");
	}
	public static void BuildingOff() {
		BuildingOnArenas = false;
	}
	public static void BuildingOn() {
		BuildingOnArenas = true;
	}
	public static void PvPoff() {
		PvPEnabled = false;
		plugin.getLogger().info("PvP-Off");
	}
	public static void PvPon() {
		PvPEnabled = true;
		plugin.getLogger().info("PvP-On");
	}

	public static void savePvPConfig(){
			try{
				if (!getPvPConfig().contains("AmonPack")) {
        			/*getPvPConfig()().set("AmonPack.PvP.Loc.X", -65);
        			getPvPConfig()().set("AmonPack.PvP.Loc.Y", -60);
        			getPvPConfig()().set("AmonPack.PvP.Loc.Z", 72);
        			getPvPConfig()().set("AmonPack.PvP.Loc.World", "PodZadaniowy");
        			getPvPConfig()().set("AmonPack.PvP.Loc.Radius", 20);*/

					getPvPConfig().set("AmonPack.PvP1.Loc.X", -1240);
					getPvPConfig().set("AmonPack.PvP1.Loc.Y", 70);
					getPvPConfig().set("AmonPack.PvP1.Loc.Z", 64);
					getPvPConfig().set("AmonPack.PvP1.Loc.Radius", 275);

					getPvPConfig().set("AmonPack.PvP.Loc.X", 64);
					getPvPConfig().set("AmonPack.PvP.Loc.Y", 70);
					getPvPConfig().set("AmonPack.PvP.Loc.Z", 64);
					getPvPConfig().set("AmonPack.PvP.Loc.World", "kojlerek");
					getPvPConfig().set("AmonPack.PvP.Loc.Radius", 320);
					getPvPConfig().set("AmonPack.PvP.FallingChest.FallPeriod", 45);
					getPvPConfig().set("AmonPack.PvP.FallingChest.Loot.Drobniak", 1);
					getPvPConfig().set("AmonPack.PvP.FallingChest.Loot.Ksymil", 1);

					getPvPConfig().set("AmonPack.PvP.FallingChest.Occurance.Combat.C1.Loot.Jadeit", 1);
					getPvPConfig().set("AmonPack.PvP.FallingChest.Occurance.Combat.C1.Loot.Bazalt", 1);
					getPvPConfig().set("AmonPack.PvP.FallingChest.Occurance.Combat.C1.EType", new String[]{"World_Żołnierz_Ognia_Mag_2","World_Żołnierz_Ognia_Wojownik_2"});
					getPvPConfig().set("AmonPack.PvP.FallingChest.Occurance.Combat.C1.EAmount", 2);

					getPvPConfig().set("AmonPack.PvP.FallingChest.Occurance.Combat.C2.Loot.Skyrim", 1);
					getPvPConfig().set("AmonPack.PvP.FallingChest.Occurance.Combat.C2.Loot.Meteoryt", 1);
					getPvPConfig().set("AmonPack.PvP.FallingChest.Occurance.Combat.C2.EType", new String[]{"World_Żołnierz_Ognia_Mag_2","World_Żołnierz_Ognia_Wojownik_2"});
					getPvPConfig().set("AmonPack.PvP.FallingChest.Occurance.Combat.C2.EAmount", 5);

					getPvPConfig().set("AmonPack.PvP.FallingChest.Occurance.Command.Co1.Loot.Celestyn", 1);
					getPvPConfig().set("AmonPack.PvP.FallingChest.Occurance.Command.Co1.Command", "say test to ejst komenda");

					getPvPConfig().set("AmonPack.PvP.FallingChest.Occurance.Parkour.P1.Loot.KwiatWisni", 1);
					getPvPConfig().set("AmonPack.PvP.FallingChest.Occurance.Parkour.P1.EndLoc.X",-19);
					getPvPConfig().set("AmonPack.PvP.FallingChest.Occurance.Parkour.P1.EndLoc.Y",-53);
					getPvPConfig().set("AmonPack.PvP.FallingChest.Occurance.Parkour.P1.EndLoc.Z",89);
					getPvPConfig().set("AmonPack.PvP.FallingChest.Occurance.Parkour.P1.StartLoc.X",-43);
					getPvPConfig().set("AmonPack.PvP.FallingChest.Occurance.Parkour.P1.StartLoc.Y",-59);
					getPvPConfig().set("AmonPack.PvP.FallingChest.Occurance.Parkour.P1.StartLoc.Z",88);


					getPvPConfig().set("AmonPack.PvP.Events.RandomSpawns.Period",30);
					getPvPConfig().set("AmonPack.PvP.Events.RandomSpawns.Spawn1.EType" , new String[]{"World_Żołnierz_Ognia_Mag_2","World_Żołnierz_Ognia_Wojownik_2"});
					getPvPConfig().set("AmonPack.PvP.Events.RandomSpawns.Spawn1.EAmount",3);
					getPvPConfig().set("AmonPack.PvP.Events.RandomSpawns.Spawn1.SpaAmount",4);

					getPvPConfig().set("AmonPack.PvP.Events.RaidBoss.Boss1.LocX",-74);
					getPvPConfig().set("AmonPack.PvP.Events.RaidBoss.Boss1.LocY",-39);
					getPvPConfig().set("AmonPack.PvP.Events.RaidBoss.Boss1.LocZ",62);
					getPvPConfig().set("AmonPack.PvP.Events.RaidBoss.Boss1.BossLocX",-74);
					getPvPConfig().set("AmonPack.PvP.Events.RaidBoss.Boss1.BossLocY",-39);
					getPvPConfig().set("AmonPack.PvP.Events.RaidBoss.Boss1.BossLocZ",62);
					getPvPConfig().set("AmonPack.PvP.Events.RaidBoss.Boss1.ArenaRadius",10);
					getPvPConfig().set("AmonPack.PvP.Events.RaidBoss.Boss1.ArenaHeight",6);
					getPvPConfig().set("AmonPack.PvP.Events.RaidBoss.Boss1.Loot.iron", 1);
					getPvPConfig().set("AmonPack.PvP.Events.RaidBoss.Boss1.BossName","Boss_WładcaOgnia_1");
				}
				getPvPConfig().save(PvPFile);
			}catch(Exception e){
				e.printStackTrace();
			}

	}

	public static void SaveConfigs(){
		try{
			if (!AbilitiesConfig.contains("AmonPack")) {
				LevelConfig.set("AmonPack.Earth.EarthHammer.Cooldown", 7000);
			}
			if (!ForestConfig.contains("AmonPack")) {
				ForestConfig.set("AmonPack.Forest.Forest1.World", "AvatarServGlownyNowy");
			}
			if (!LevelConfig.contains("AmonPack")) {
				LevelConfig.set("AmonPack.Levels.GENERAL.Gui.Place", 4);
				LevelConfig.set("AmonPack.Levels.GENERAL.Gui.Title", ChatColor.GOLD+"Poziom Ogólny: ");
				LevelConfig.set("AmonPack.Levels.MINING.Gui.Place", 20);
				LevelConfig.set("AmonPack.Levels.MINING.Gui.Title", ChatColor.GOLD+"Doświadczenie w Kopalni: ");
				LevelConfig.set("AmonPack.Levels.COMBAT.Gui.Place", 22);
				LevelConfig.set("AmonPack.Levels.COMBAT.Gui.Title", ChatColor.GOLD+"Doświadczenie w Strefie Walki: ");
			}
			LevelConfig.save(LevelConfigFile);
			AbilitiesConfig.save(AbilitiesConfigFile);
			ForestConfig.save(ForestConfigFile);
		}catch(Exception e){
			System.out.println("Błąd z konfigiem! "+e.getMessage());
		}}


	public static void saveMinesConfig(){
		try{
			if (!getMinesConfig().contains("AmonPack")) {
				getMinesConfig().set("AmonPack.MiningOresDrops.COAL_ORE", "coal");
				getMinesConfig().set("AmonPack.MiningOresDrops.IRON_ORE", "iron");
				getMinesConfig().set("AmonPack.MiningOresDrops.STONE", "cobblestone");
				getMinesConfig().set("AmonPack.MiningOresDrops.DEEPSLATE", "cobblestone");
				getMinesConfig().set("AmonPack.MiningOresDrops.GLOWSTONE", "Ksymil");
				getMinesConfig().set("AmonPack.MiningOresDrops.CRIMSON_HYPHAE", "Skyrim");
				getMinesConfig().set("AmonPack.MiningOresDrops.PRISMARINE", "Celestyn");
				getMinesConfig().set("AmonPack.MiningOresDrops.GRAVEL", "gravel");
				getMinesConfig().set("AmonPack.MiningOresDrops.SAND", "sand");
				getMinesConfig().set("AmonPack.MiningOresDrops.CLAY", "clayball");
				getMinesConfig().set("AmonPack.MiningOresDrops.DIRT", "dirt");

				/*getNewConfigz().set("AmonPack.Mining.Kopalnia1.X",2880);
				getNewConfigz().set("AmonPack.Mining.Kopalnia1.Y",40);
				getNewConfigz().set("AmonPack.Mining.Kopalnia1.Z", 566);
				getNewConfigz().set("AmonPack.Mining.Kopalnia1.World", "AvatarServGlownyNowy");*/
				getMinesConfig().set("AmonPack.Mining.Kopalnia1.X",-90);
				getMinesConfig().set("AmonPack.Mining.Kopalnia1.Y",-50);
				getMinesConfig().set("AmonPack.Mining.Kopalnia1.Z", -86);
				getMinesConfig().set("AmonPack.Mining.Kopalnia1.World", "PodZadaniowy");
				getMinesConfig().set("AmonPack.Mining.Kopalnia1.Y-Offset-Up", -40);
				getMinesConfig().set("AmonPack.Mining.Kopalnia1.Radius", 100);
				getMinesConfig().set("AmonPack.Mining.Kopalnia1.RestoreTime", 30);
				getMinesConfig().set("AmonPack.Mining.Kopalnia1.RevertBlock", "DEEPSLATE");
				getMinesConfig().set("AmonPack.Mining.Kopalnia1.Loot.Skyrim", 1);
				getMinesConfig().set("AmonPack.Mining.Kopalnia1.Ores.GLOWSTONE", 1);
				getMinesConfig().set("AmonPack.Mining.Kopalnia1.Ores.IRON_ORE", 4);
				getMinesConfig().set("AmonPack.Mining.Kopalnia1.Ores.COAL_ORE", 4);
			}
			getMinesConfig().save(MinesConfigFile);
		}catch(Exception e){
			e.printStackTrace();
		}}

	public static void saveDungeonConfig() {
		try {
			FileConfiguration config = GetMenagerieConfig().get(0);
			if (!config.contains("Menagerie.PróbaOgnia")) {
				config.set("Menagerie.PróbaOgnia.Center_Location", new int[]{0, 46, 20});
				config.set("Menagerie.PróbaOgnia.Base_World_Name", "MultiWorlds/MenageriaOgnia/MenageriaOgnia1");
				config.set("Menagerie.PróbaOgnia.Range_X", 200);
				config.set("Menagerie.PróbaOgnia.Range_Z", 200);
			}

			if (!config.contains("Menagerie.PróbaOgnia.Encounters.Encounter1")) {
				config.set("Menagerie.PróbaOgnia.Encounters.Encounter1.Spawn_Location", new int[]{-1, 47, 18});
				config.set("Menagerie.PróbaOgnia.Encounters.Encounter1.Doors_1", new int[]{0, 48, 21});
				config.set("Menagerie.PróbaOgnia.Encounters.Encounter1.Doors_2", new int[]{-2, 46, 21});
				config.set("Menagerie.PróbaOgnia.Encounters.Encounter1.Doors_Material", "BARRIER");

				String obj1Path = "Menagerie.PróbaOgnia.Encounters.Encounter1.Objectives.Obj1";
				if (!config.contains(obj1Path)) {
					config.set(obj1Path + ".Next_Objectives", new String[]{"Obj2"});
					config.set(obj1Path + ".Display_Title_Main", "Zaakceptuj");
					config.set(obj1Path + ".Display_Title_Sub", "Użyj darów");
					config.set(obj1Path + ".Effects.Effect1.Message", "Start!");
					config.set(obj1Path + ".Conditions.Condition1.AllPlayersReady", true);
				}

				String obj2Path = "Menagerie.PróbaOgnia.Encounters.Encounter1.Objectives.Obj2";
				if (!config.contains(obj2Path)) {
					config.set(obj2Path + ".Next_Objectives", new String[]{"Obj3"});
					config.set(obj2Path + ".Display_Title_Main", "Przebij się dalej");
					config.set(obj2Path + ".Display_Title_Sub", "Pokonaj Strażników");
					config.set(obj2Path + ".Effects.Effect1.Enemies.enemy1.Name", "WaveDefender_FireMage_1");
					config.set(obj2Path + ".Effects.Effect1.Enemies.enemy1.DisplayName", "&4&lMag Ognia");
					config.set(obj2Path + ".Effects.Effect1.Enemies.enemy1.Type", "HUSK");
					config.set(obj2Path + ".Effects.Effect1.Enemies.enemy1.SpawnLocation", new int[]{13, 46, 43});
					config.set(obj2Path + ".Effects.Effect1.Enemies.enemy1.SpawnLocationRange", 1);
					config.set(obj2Path + ".Effects.Effect1.Enemies.enemy1.SpawnChance", 100);
					config.set(obj2Path + ".Effects.Effect1.Enemies.enemy1.Amount", 2);
					config.set(obj2Path + ".Effects.Effect1.Enemies.enemy1.MaxLvl", 1);
					config.set(obj2Path + ".Conditions.Condition1.locationCondition.activationLoc", new int[]{-1, 46, 35});
					config.set(obj2Path + ".Conditions.Condition1.locationCondition.activationRange", 10.0);
				}

				String obj3Path = "Menagerie.PróbaOgnia.Encounters.Encounter1.Objectives.Obj3";
				if (!config.contains(obj3Path)) {
					config.set(obj3Path + ".Next_Objectives", new String[]{"Obj4"});
					config.set(obj3Path + ".Display_Title_Main", "Przebij się dalej");
					config.set(obj3Path + ".Display_Title_Sub", "Pokonaj kolejną fale");
					config.set(obj3Path + ".Effects.Effect1.Enemies.enemy1.Name", "WaveDefender_FireMage_1");
					config.set(obj3Path + ".Effects.Effect1.Enemies.enemy1.DisplayName", "&4&lMag Ognia");
					config.set(obj3Path + ".Effects.Effect1.Enemies.enemy1.Type", "HUSK");
					config.set(obj3Path + ".Effects.Effect1.Enemies.enemy1.SpawnLocation", new int[]{-15, 46, 49});
					config.set(obj3Path + ".Effects.Effect1.Enemies.enemy1.SpawnLocationRange", 1);
					config.set(obj3Path + ".Effects.Effect1.Enemies.enemy1.SpawnChance", 100);
					config.set(obj3Path + ".Effects.Effect1.Enemies.enemy1.Amount", 2);
					config.set(obj3Path + ".Effects.Effect1.Enemies.enemy1.MaxLvl", 1);
					config.set(obj3Path + ".Effects.Effect1.Enemies.enemy2.Name", "WaveDefender_FireSentinel_1");
					config.set(obj3Path + ".Effects.Effect1.Enemies.enemy2.DisplayName", "&4&lŻołnierz Ognia");
					config.set(obj3Path + ".Effects.Effect1.Enemies.enemy2.Type", "HUSK");
					config.set(obj3Path + ".Effects.Effect1.Enemies.enemy2.SpawnLocation", new int[]{13, 46, 43});
					config.set(obj3Path + ".Effects.Effect1.Enemies.enemy2.SpawnLocationRange", 1);
					config.set(obj3Path + ".Effects.Effect1.Enemies.enemy2.SpawnChance", 100);
					config.set(obj3Path + ".Effects.Effect1.Enemies.enemy2.Amount", 1);
					config.set(obj3Path + ".Effects.Effect1.Enemies.enemy2.MaxLvl", 1);
					config.set(obj3Path + ".Conditions.Condition1.killCondition.enemy1.Name", "WaveDefender_FireMage_1");
					config.set(obj3Path + ".Conditions.Condition1.killCondition.enemy1.DisplayName", "&4&lMag Ognia");
					config.set(obj3Path + ".Conditions.Condition1.killCondition.enemy1.Type", "HUSK");
					config.set(obj3Path + ".Conditions.Condition1.killCondition.enemy1.SpawnLocation", new int[]{13, 46, 43});
					config.set(obj3Path + ".Conditions.Condition1.killCondition.enemy1.SpawnLocationRange", 1);
					config.set(obj3Path + ".Conditions.Condition1.killCondition.enemy1.SpawnChance", 100);
					config.set(obj3Path + ".Conditions.Condition1.killCondition.enemy1.Amount", 2);
					config.set(obj3Path + ".Conditions.Condition1.killCondition.enemy1.MaxLvl", 1);
				}
/*
				String obj4Path = "Menagerie.PróbaOgnia.Encounters.Encounter1.Objectives.Obj4";
				if (!config.contains(obj4Path)) {
					config.set(obj4Path + ".Display_Title_Main", "Znajdź sposób na otwarcie wrót");
					config.set(obj4Path + ".Display_Title_Sub", "Poszukaj klucza");
					config.set(obj4Path + ".Effects.Effect1.Message", "Objective Completed! <3");
					config.set(obj4Path + ".Conditions.Condition1.killCondition.enemy1.Name", "WaveDefender_FireMage_1");
					config.set(obj4Path + ".Conditions.Condition1.killCondition.enemy1.DisplayName", "&4&lMag Ognia");
					config.set(obj4Path + ".Conditions.Condition1.killCondition.enemy1.Type", "HUSK");
					config.set(obj4Path + ".Conditions.Condition1.killCondition.enemy1.SpawnLocation", new int[]{-15, 46, 49});
					config.set(obj4Path + ".Conditions.Condition1.killCondition.enemy1.SpawnLocationRange", 1);
					config.set(obj4Path + ".Conditions.Condition1.killCondition.enemy1.SpawnChance", 100);
					config.set(obj4Path + ".Conditions.Condition1.killCondition.enemy1.Amount", 2);
					config.set(obj4Path + ".Conditions.Condition1.killCondition.enemy1.MaxLvl", 1);
					config.set(obj4Path + ".Conditions.Condition1.killCondition.enemy2.Name", "WaveDefender_FireSentinel_1");
					config.set(obj4Path + ".Conditions.Condition1.killCondition.enemy2.DisplayName", "&4&lŻołnierz Ognia");
					config.set(obj4Path + ".Conditions.Condition1.killCondition.enemy2.Type", "HUSK");
					config.set(obj4Path + ".Conditions.Condition1.killCondition.enemy2.SpawnLocation", new int[]{13, 46, 43});
					config.set(obj4Path + ".Conditions.Condition1.killCondition.enemy2.SpawnLocationRange", 1);
					config.set(obj4Path + ".Conditions.Condition1.killCondition.enemy2.SpawnChance", 100);
					config.set(obj4Path + ".Conditions.Condition1.killCondition.enemy2.Amount", 1);
					config.set(obj4Path + ".Conditions.Condition1.killCondition.enemy2.MaxLvl", 1);
				}*/
			}
			config.save(MenagerieConfigFile.get(0));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

/*
	public static void SaveWaveDefender(){
		try{
			if (!getWaveConfig().contains("WaveDefender")) {
			getWaveConfig().set("WaveDefender.WaveDefender_1.SpawnX", 0);
			getWaveConfig().set("WaveDefender.WaveDefender_1.SpawnY", 64);
			getWaveConfig().set("WaveDefender.WaveDefender_1.SpawnZ", 0);
			getWaveConfig().set("WaveDefender.WaveDefender_1.World", "Wave");
			getWaveConfig().set("WaveDefender.WaveDefender_1.Rollback", "say testwave");
			getWaveConfig().set("WaveDefender.WaveDefender_1.Range", 100);
			getWaveConfig().set("WaveDefender.WaveDefender_1.StartHP", 15);
			getWaveConfig().set("WaveDefender.WaveDefender_1.RestTimer", 7);
			getWaveConfig().set("WaveDefender.WaveDefender_1.DefendRadius", 3);
			getWaveConfig().set("WaveDefender.WaveDefender_1.Waves.1.AttackFromX", 9);
			getWaveConfig().set("WaveDefender.WaveDefender_1.Waves.1.AttackFromY", 64);
			getWaveConfig().set("WaveDefender.WaveDefender_1.Waves.1.AttackFromZ", 0);
			getWaveConfig().set("WaveDefender.WaveDefender_1.Waves.1.MobTypes", new HashMap<String, Integer>(){{put("TestMob",1);}});
			getWaveConfig().set("WaveDefender.WaveDefender_1.Waves.1.ExtraMobs", 0);
			getWaveConfig().set("WaveDefender.WaveDefender_1.Waves.2.AttackFromX", 0);
			getWaveConfig().set("WaveDefender.WaveDefender_1.Waves.2.AttackFromY", 64);
			getWaveConfig().set("WaveDefender.WaveDefender_1.Waves.2.AttackFromZ", -9);
			getWaveConfig().set("WaveDefender.WaveDefender_1.Waves.2.MobTypes", new HashMap<String, Integer>(){{put("TestMob",1);}});
			getWaveConfig().set("WaveDefender.WaveDefender_1.Waves.2.FriendlyMobTypes", new HashMap<String, Integer>(){{put("FabulaBandytaMagWody",1);}});
			getWaveConfig().set("WaveDefender.WaveDefender_1.Waves.2.AllowedEntityTypes", List.of("ZOMBIE"));
			getWaveConfig().set("WaveDefender.WaveDefender_1.Waves.2.ExtraMobs", 1);
			getWaveConfig().set("WaveDefender.WaveDefender_1.Waves.2.SpawnOffset", 1);
			getWaveConfig().set("WaveDefender.WaveDefender_1.Waves.3.1.AttackFromX", 9);
			getWaveConfig().set("WaveDefender.WaveDefender_1.Waves.3.1.AttackFromY", 64);
			getWaveConfig().set("WaveDefender.WaveDefender_1.Waves.3.1.AttackFromZ", 0);
			getWaveConfig().set("WaveDefender.WaveDefender_1.Waves.3.1.MobTypes", new HashMap<String, Integer>(){{put("TestMob",2);}});
			getWaveConfig().set("WaveDefender.WaveDefender_1.Waves.3.1.ExtraMobs", 0);
			getWaveConfig().set("WaveDefender.WaveDefender_1.Waves.3.2.AttackFromX", 0);
			getWaveConfig().set("WaveDefender.WaveDefender_1.Waves.3.2.AttackFromY", 64);
			getWaveConfig().set("WaveDefender.WaveDefender_1.Waves.3.2.AttackFromZ", -9);
			getWaveConfig().set("WaveDefender.WaveDefender_1.Waves.3.2.MobTypes", new HashMap<String, Integer>(){{put("TestMob",1);}});
			getWaveConfig().set("WaveDefender.WaveDefender_1.Waves.3.2.ExtraMobs", 2);
			}
			getWaveConfig().save(WaveFile);
		}catch(Exception e){
			e.printStackTrace();
		}}
*/
	public static void saveGuiConfig(){
		try{
			if (!getGuiConfig().contains("AmonPack")) {

				getGuiConfig().set("AmonPack.Gui.Help.0.Type", "HONEYCOMB");
				getGuiConfig().set("AmonPack.Gui.Help.0.Name", "&6&lPvP");
				getGuiConfig().set("AmonPack.Gui.Help.0.Lore", "&5Nakurwiasz się z graczami");

				getGuiConfig().set("AmonPack.Gui.ItemList.0.Type", "HONEYCOMB");
				getGuiConfig().set("AmonPack.Gui.ItemList.0.Name", "&6&lKsymil");
				getGuiConfig().set("AmonPack.Gui.ItemList.0.Source", "&5Materiał, Pozyswkiwane z Kopalni na spawnie");


				}
			getGuiConfig().save(GuiFile);
		}catch(Exception e){
			e.printStackTrace();
		}}

	public static void saveSkillTreeConfig(){
		try{
			if (!getSkillTreeConfig().contains("AmonPack")) {
				//getSkillTreeConfig().set("AmonPack.SpellTree.Abilities.Command", "say");
				/*

				getSkillTreeConfig().set("AmonPack.SpellTree.Abilities.Fire.PathDecoration", new Integer[]{10,11,12});
				getSkillTreeConfig().set("AmonPack.SpellTree.Abilities.Fire.FireBlast.Cost", 0);
				getSkillTreeConfig().set("AmonPack.SpellTree.Abilities.Fire.FireBlast.Place", 0);
				getSkillTreeConfig().set("AmonPack.SpellTree.Abilities.Fire.FireBlast.ReqAbilities", new String[]{});
				getSkillTreeConfig().set("AmonPack.SpellTree.Abilities.Fire.SmokeDaggers.Cost", 1);
				getSkillTreeConfig().set("AmonPack.SpellTree.Abilities.Fire.SmokeDaggers.Place", 1);
				getSkillTreeConfig().set("AmonPack.SpellTree.Abilities.Fire.SmokeDaggers.ReqAbilities", new String[]{"FireBlast"});
				getSkillTreeConfig().set("AmonPack.SpellTree.Abilities.Fire.FireBurst.Cost", 1);
				getSkillTreeConfig().set("AmonPack.SpellTree.Abilities.Fire.FireBurst.Place", 2);
				getSkillTreeConfig().set("AmonPack.SpellTree.Abilities.Fire.FireBurst.ReqAbilities", new String[]{"FireBlast"});
				getSkillTreeConfig().set("AmonPack.SpellTree.Abilities.Fire.SmokeSurge.Cost", 2);
				getSkillTreeConfig().set("AmonPack.SpellTree.Abilities.Fire.SmokeSurge.Place", 10);
				getSkillTreeConfig().set("AmonPack.SpellTree.Abilities.Fire.SmokeSurge.ReqAbilities", new String[]{"SmokeDaggers","FireBurst"});

				getSkillTreeConfig().set("AmonPack.SpellTree.Abilities.Air.PathDecoration", new Integer[]{10,11,12});
				getSkillTreeConfig().set("AmonPack.SpellTree.Abilities.Air.AirSwipe.Cost", 0);
				getSkillTreeConfig().set("AmonPack.SpellTree.Abilities.Air.AirSwipe.Place", 0);
				getSkillTreeConfig().set("AmonPack.SpellTree.Abilities.Air.AirSwipe.ReqAbilities", new String[]{});
				getSkillTreeConfig().set("AmonPack.SpellTree.Abilities.Air.AirBlast.Cost", 1);
				getSkillTreeConfig().set("AmonPack.SpellTree.Abilities.Air.AirBlast.Place", 1);
				getSkillTreeConfig().set("AmonPack.SpellTree.Abilities.Air.AirBlast.ReqAbilities", new String[]{"AirSwipe"});

				getSkillTreeConfig().set("AmonPack.SpellTree.Abilities.Water.PathDecoration", new Integer[]{10,11,12});
				getSkillTreeConfig().set("AmonPack.SpellTree.Abilities.Water.WaterManipulation.Cost", 0);
				getSkillTreeConfig().set("AmonPack.SpellTree.Abilities.Water.WaterManipulation.Place", 0);
				getSkillTreeConfig().set("AmonPack.SpellTree.Abilities.Water.WaterManipulation.ReqAbilities", new String[]{});

				getSkillTreeConfig().set("AmonPack.SpellTree.Abilities.Earth.PathDecoration", new Integer[]{10,11,12});
				getSkillTreeConfig().set("AmonPack.SpellTree.Abilities.Earth.EarthBlast.Cost", 0);
				getSkillTreeConfig().set("AmonPack.SpellTree.Abilities.Earth.EarthBlast.Place", 0);
				getSkillTreeConfig().set("AmonPack.SpellTree.Abilities.Earth.EarthBlast.ReqAbilities", new String[]{});
				*/

				//getSkillTreeConfig().set("AmonPack.SpellTree.Abilities.air", new String[]{"AirPressure","AirBlast","AirBurst"});
				//getSkillTreeConfig().set("AmonPack.SpellTree.Abilities.earth", new String[]{"SandBreath","EarthBlast",});
				//getSkillTreeConfig().set("AmonPack.SpellTree.Abilities.chi", new String[]{"QuickStrike","Counter",});
				//getSkillTreeConfig().set("AmonPack.SpellTree.Abilities.water", new String[]{"IceArch","Torrent",});
			}
			getSkillTreeConfig().save(SkillTreeFile);
		}catch(Exception e){
			e.printStackTrace();
		}}


    public static void saveNewConfig(){
    	try{
			if (!getNewConfigz().contains("AmonPack.Spawn")) {
				getNewConfigz().set("AmonPack.Spawn.X", 3085);
				getNewConfigz().set("AmonPack.Spawn.Y", 74);
				getNewConfigz().set("AmonPack.Spawn.Z", 414);
				getNewConfigz().set("AmonPack.Spawn.World", "AvatarServGlownyNowy");
			}

    	if (!getNewConfigz().contains("AmonPack")) {
       /* getNewConfigz().set("AmonPack.SpellTree.Abilities.Command", "say");
        getNewConfigz().set("AmonPack.SpellTree.Abilities.fire", new String[]{"FireBlast","SmokeDaggers",});
        getNewConfigz().set("AmonPack.SpellTree.Abilities.air", new String[]{"AirPressure","AirBlast","AirBurst"});
        getNewConfigz().set("AmonPack.SpellTree.Abilities.earth", new String[]{"SandBreath","EarthBlast",});
        getNewConfigz().set("AmonPack.SpellTree.Abilities.chi", new String[]{"QuickStrike","Counter",});
        getNewConfigz().set("AmonPack.SpellTree.Abilities.water", new String[]{"IceArch","Torrent",});
*/
        getNewConfigz().set("AmonPack.Puzzle.Puzzle1", new String[]{"Command:give ThisPlayer stone","IsRandom:true","BrokenChance:10","BrokenPlace:1","BrokenPlace:9","BrokenPlace:52","BrokenPlace:44","BateryPlace:4","CorePlace:0","CorePlace:31","CorePlace:53"});

		getNewConfigz().set("AmonPack.Gathering.Loc1.Options.X",2953);
		getNewConfigz().set("AmonPack.Gathering.Loc1.Options.Y",68);
		getNewConfigz().set("AmonPack.Gathering.Loc1.Options.Z", 696);
		getNewConfigz().set("AmonPack.Gathering.Loc1.Options.World", "AvatarServGlownyNowy");
		/*getNewConfigz().set("AmonPack.Gathering.Loc1.Options.X",-95);
		getNewConfigz().set("AmonPack.Gathering.Loc1.Options.Y",-57);
		getNewConfigz().set("AmonPack.Gathering.Loc1.Options.Z", -97);
		getNewConfigz().set("AmonPack.Gathering.Loc1.Options.World", "PodZadaniowy");*/
		getNewConfigz().set("AmonPack.Gathering.Loc1.Options.Radius", 75);
		getNewConfigz().set("AmonPack.Gathering.Loc1.Resource.FLOWERING_AZALEA_LEAVES.Loot", "KwiatWisni");
		getNewConfigz().set("AmonPack.Gathering.Loc1.Resource.FLOWERING_AZALEA_LEAVES.RestoreTime", 10);
		getNewConfigz().set("AmonPack.Gathering.Loc1.Resource.FLOWERING_AZALEA_LEAVES.ClickReq", 5);
		getNewConfigz().set("AmonPack.Gathering.Loc1.Resource.FLOWERING_AZALEA_LEAVES.DelocateTime", 15);
		getNewConfigz().set("AmonPack.Gathering.Loc1.Resource.OAK_LOG.ClickReq", 3);
		getNewConfigz().set("AmonPack.Gathering.Loc1.Resource.OAK_LOG.RestoreTime", 1);
		getNewConfigz().set("AmonPack.Gathering.Loc1.Resource.OAK_LOG.Loot", "oaklog");
		getNewConfigz().set("AmonPack.Gathering.Loc1.Resource.OAK_LOG.DelocateTime", 40);
		getNewConfigz().set("AmonPack.Gathering.Loc1.Resource.BIRCH_LOG.ClickReq", 3);
		getNewConfigz().set("AmonPack.Gathering.Loc1.Resource.BIRCH_LOG.RestoreTime", 1);
		getNewConfigz().set("AmonPack.Gathering.Loc1.Resource.BIRCH_LOG.Loot", "birchlog");
		getNewConfigz().set("AmonPack.Gathering.Loc1.Resource.BIRCH_LOG.DelocateTime", 40);

		getNewConfigz().set("AmonPack.Items.cobblestone.Type", "COBBLESTONE");
		getNewConfigz().set("AmonPack.Items.stone.Type", "STONE");
		getNewConfigz().set("AmonPack.Items.coal.Type", "COAL");
		getNewConfigz().set("AmonPack.Items.birchlog.Type", "BIRCH_LOG");
		getNewConfigz().set("AmonPack.Items.oaklog.Type", "OAK_LOG");
		getNewConfigz().set("AmonPack.Items.iron.Type", "RAW_IRON");
		getNewConfigz().set("AmonPack.Items.gravel.Type", "GRAVEL");
		getNewConfigz().set("AmonPack.Items.sand.Type", "SAND");
		getNewConfigz().set("AmonPack.Items.dirt.Type", "DIRT");
		getNewConfigz().set("AmonPack.Items.clayball.Type", "CLAY_BALL");
		getNewConfigz().set("AmonPack.Items.KwiatWisni.Type", "MAGENTA_DYE");
		getNewConfigz().set("AmonPack.Items.KwiatWisni.Name", "&d&lKwiat Wisni");
		getNewConfigz().set("AmonPack.Items.KwiatWisni.Enchantment.DURABILITY.EnchantmentLevel", 5);
		getNewConfigz().set("AmonPack.Items.KwiatWisni.Lore.Lore1", "&5Rzadki Minerał");
		getNewConfigz().set("AmonPack.Items.Ksymil.Type", "HONEYCOMB");
		getNewConfigz().set("AmonPack.Items.Ksymil.Name", "&6&lKsymil");
		getNewConfigz().set("AmonPack.Items.Ksymil.Enchantment.DURABILITY.EnchantmentLevel", 5);
		getNewConfigz().set("AmonPack.Items.Ksymil.Lore.Lore1", "&5Rzadki Minerał");
		getNewConfigz().set("AmonPack.Items.Celestyn.Type", "TUBE_CORAL");
		getNewConfigz().set("AmonPack.Items.Celestyn.Name", "&3&lCelestyn");
		getNewConfigz().set("AmonPack.Items.Celestyn.Enchantment.DURABILITY.EnchantmentLevel", 5);
		getNewConfigz().set("AmonPack.Items.Celestyn.Lore.Lore1", "&5Rzadki Minerał");
		getNewConfigz().set("AmonPack.Items.Skyrim.Type", "REDSTONE");
		getNewConfigz().set("AmonPack.Items.Skyrim.Name", "&4&lSkyrim");
		getNewConfigz().set("AmonPack.Items.Skyrim.Enchantment.DURABILITY.EnchantmentLevel", 5);
		getNewConfigz().set("AmonPack.Items.Skyrim.Lore.Lore1", "&5Rzadki Minerał");
		getNewConfigz().set("AmonPack.Items.Jadeit.Type", "SCUTE");
		getNewConfigz().set("AmonPack.Items.Jadeit.Name", "&a&lJadeit");
		getNewConfigz().set("AmonPack.Items.Jadeit.Enchantment.DURABILITY.EnchantmentLevel", 5);
		getNewConfigz().set("AmonPack.Items.Jadeit.Lore.Lore1", "&5Rzadki Minerał");
		getNewConfigz().set("AmonPack.Items.Bazalt.Type", "FLINT");
		getNewConfigz().set("AmonPack.Items.Bazalt.Name", "&8&lBazalt");
		getNewConfigz().set("AmonPack.Items.Bazalt.Enchantment.DURABILITY.EnchantmentLevel", 5);
		getNewConfigz().set("AmonPack.Items.Bazalt.Lore.Lore1", "&5Rzadki Minerał");
		getNewConfigz().set("AmonPack.Items.Meteoryt.Type", "FIRE_CHARGE");
		getNewConfigz().set("AmonPack.Items.Meteoryt.Name", "&4&lSztabka Meteorytu");
		getNewConfigz().set("AmonPack.Items.Meteoryt.Enchantment.PROTECTION_FIRE.EnchantmentLevel", 10);
		getNewConfigz().set("AmonPack.Items.Meteoryt.Lore.Lore1", "&5&nBardzo&5 rzadki Minerał");
		getNewConfigz().set("AmonPack.Items.Drobniak.Type", "IRON_NUGGET");
		getNewConfigz().set("AmonPack.Items.Drobniak.Name", "&7&lDrobniak");
		getNewConfigz().set("AmonPack.Items.Drobniak.Enchantment.DURABILITY.EnchantmentLevel", 3);
		getNewConfigz().set("AmonPack.Items.Drobniak.Lore.Lore1", "&fWaluta wykorzystywana w handlu na calym swiecie");
    	}
    	getNewConfigz().save(newConfig);
    	}catch(Exception e){
    	e.printStackTrace();
    	}}

    public void sqlConnection() {
    	sqlite = new SQLite(plugin.getLogger(), "AmonPackSQL.db", plugin.getDataFolder().getAbsolutePath());
    	try {
    	sqlite.open();
    	    } catch (Exception e) {
    	        plugin.getLogger().info(e.getMessage());
    	        getPluginLoader().disablePlugin(plugin);
    	    }
    	if (sqlite.open() != null) {
    		getLogger().info("Baza danych połączona!");
    	}
		ExecuteQuery("CREATE TABLE IF NOT EXISTS SpellTree (Player VARCHAR(50) PRIMARY KEY, SkillPoint INT, Path TEXT, Element TEXT, AllElements TEXT)");
		ExecuteQuery("CREATE TABLE IF NOT EXISTS Reputation (Player VARCHAR(50) PRIMARY KEY, RepLvL1 INT, RepLvL2 INT, RepLvL3 INT, RepLvL4 INT, RepLvL5 INT, RepLvL6 INT, RepLvL7 INT)");
		ExecuteQuery("CREATE TABLE IF NOT EXISTS Jobs (Player VARCHAR(50) PRIMARY KEY, Job1 INT, Job2 INT, Job3 INT, Job4 INT)");
		for(String key : LevelConfig.getStringList("AmonPack.Levels.Enabled")) {
			ExecuteQuery("CREATE TABLE IF NOT EXISTS Level"+key+" (Player VARCHAR(50) PRIMARY KEY, GeneralLevel DOUBLE, UsedRewards VARCHAR(100),UpgradePercent DOUBLE)");
		}
 	}

	public static void ExecuteQuery(String query) {
		try {
			Statement stmt = sqlite.getConnection().createStatement();
			stmt.executeUpdate(query);
			stmt.close();
		} catch (Exception var3) {
			PrintStream var10000 = System.err;
			String var10001 = var3.getClass().getName();
			var10000.println(var10001 + ": " + var3.getMessage());
			System.out.println("problem " + var3);
		}

	}

    @Override
    public void onDisable() {
        try {
            PlayerMenager.LoadIntoDatabase();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        sqlite.close();
        getLogger().info("AmonPack dezaktywowany");
    }
    
    public static Element getBladesElement() {
        return BladesElement;
    }
    public static Element getSmokeElement() {
        return SmokeElement;
    }

    public void createconf() {
    	 config.addDefault("AmonPack.Water.Ice.IcySpace.Cooldown", 12000);
    	 config.addDefault("AmonPack.Water.Ice.IcySpace.Range", 5);
         config.addDefault("AmonPack.Water.Ice.IcySpace.Duration", 10000);
         config.addDefault("AmonPack.Water.Ice.IcySpace.NightAugment.Cooldown", 5000);
         config.addDefault("AmonPack.Water.Ice.IcySpace.NightAugment.Range", 12);
         config.addDefault("AmonPack.Water.Ice.IcySpace.NightAugment.Duration", 60000);
         config.addDefault("AmonPack.Water.Ice.IcySpace.FullMoonAugment.Cooldown", 12000);
         config.addDefault("AmonPack.Water.Ice.IcySpace.FullMoonAugment.Range", 8);
         config.addDefault("AmonPack.Water.Ice.IcySpace.FullMoonAugment.Duration", 20000);
         config.addDefault("AmonPack.Water.Ice.IcySpace.1stPhaseDelay", 3);
         config.addDefault("AmonPack.Water.Ice.IcySpace.2ndPhaseDelay", 6);
         config.addDefault("AmonPack.Elemental.Water.DryGrassRevert", 15000);
         config.addDefault("AmonPack.Elemental.Water.DryGrassRange", 3);
  		config.addDefault("AmonPack.Elemental.Smoke.BlindnessDuration", 40);
  		config.addDefault("AmonPack.Elemental.Smoke.PoisonDuration", 40);
  		config.addDefault("AmonPack.Elemental.Smoke.PoisonPower", 1);
  		config.addDefault("AmonPack.Elemental.Smoke.SlowPower", 3);
  		config.addDefault("AmonPack.Elemental.Smoke.SlowDuration", 40);
  		config.addDefault("AmonPack.Elemental.Smoke.AffectUser", false);
         config.addDefault("AmonPack.Earth.Sand.SandWave.Cooldown", 6000);
         config.addDefault("AmonPack.Earth.Sand.SandWave.Range", 15);
         config.addDefault("AmonPack.Earth.Sand.SandWave.Duration", 4000);
         config.addDefault("AmonPack.Earth.Sand.SandWave.Size", 6);
         config.addDefault("AmonPack.Earth.Sand.SandWave.DeBuffPower", 2);
         config.addDefault("AmonPack.Earth.Sand.SandWave.DebuffDuration", 50);
         config.addDefault("AmonPack.Earth.Sand.SandWave.BurrowPower", 1);
         config.addDefault("AmonPack.Earth.Sand.SandBreath.Cooldown", 6000);
         config.addDefault("AmonPack.Earth.Sand.SandBreath.Range", 12);
         config.addDefault("AmonPack.Earth.Sand.SandBreath.Duration", 4000);
         config.addDefault("AmonPack.Earth.Sand.SandBreath.ChargeTime", 2000);
         config.addDefault("AmonPack.Earth.Sand.SandBreath.ChargedBreathBuff", true);
         config.addDefault("AmonPack.Earth.Sand.SandBreath.SpeedOnSand", 6);
         config.addDefault("AmonPack.Earth.Sand.SandBreath.SpeedOnEarth", 2);
         config.addDefault("AmonPack.Earth.Sand.SandBreath.DeBuffPower", 2);
         config.addDefault("AmonPack.Earth.Sand.SandBreath.DebuffDuration", 60);
         config.addDefault("AmonPack.Earth.Sand.SandBreath.Damage", 1);
         config.addDefault("AmonPack.Earth.Sand.SandBreath.DurationToUseBreath", 120);
         config.addDefault("AmonPack.Earth.Sand.SandBreath.CanDebuffEnemy", true);
 		 config.addDefault("AmonPack.Earth.Metal.SteelShackles.Range", 35);
 		 config.addDefault("AmonPack.Earth.Metal.SteelShackles.Hitbox", 3);
 		 config.addDefault("AmonPack.Earth.Metal.SteelShackles.DamageFirst", 2);
 		 config.addDefault("AmonPack.Earth.Metal.SteelShackles.DamageSecond", 1);
 		 config.addDefault("AmonPack.Earth.Metal.SteelShackles.StunRange", 15);
 		 config.addDefault("AmonPack.Earth.Metal.SteelShackles.StunDuration", 60);
 		 config.addDefault("AmonPack.Earth.Metal.SteelShackles.TimeToEscape", 60);
 		 config.addDefault("AmonPack.Earth.Metal.SteelShackles.DurabilityCost", 20);
 		 config.addDefault("AmonPack.Earth.Metal.SteelShackles.Cooldown", 7000);
 		 config.addDefault("AmonPack.Earth.Metal.MetalFlex.CooldownNormal", 10000);
 		 config.addDefault("AmonPack.Earth.Metal.MetalFlex.CooldownCrysis", 40000);
 		 config.addDefault("AmonPack.Earth.Metal.MetalFlex.SpeedPower", 3);
 		 config.addDefault("AmonPack.Earth.Metal.MetalFlex.CrysisDuration", 100);
 		 config.addDefault("AmonPack.Earth.Metal.MetalFlex.LowLevel", 5);
 		 config.addDefault("AmonPack.Earth.Metal.MetalCompress.CooldownMin", 4000);
 		 config.addDefault("AmonPack.Earth.Metal.MetalCompress.CooldownMax", 8000);
 		 config.addDefault("AmonPack.Earth.Metal.MetalCompress.Damage", 2);
 		 config.addDefault("AmonPack.Earth.Metal.MetalCompress.MaxChargeTime", 1700);
 		 config.addDefault("AmonPack.Earth.Metal.MetalCompress.Duration", 5);
 		 config.addDefault("AmonPack.Earth.Metal.MetalCompress.DurabilityCostMin", 10);
 		 config.addDefault("AmonPack.Earth.Metal.MetalCompress.DurabilityCostMax", 30);

		config.addDefault("AmonPack.Earth.EarthHammer.Cooldown", 7000);
		config.addDefault("AmonPack.Earth.EarthHammer.Damage", 1);
		config.addDefault("AmonPack.Earth.EarthHammer.ChargeTime", 2000);
		config.addDefault("AmonPack.Earth.EarthHammer.RevertTime", 10000);
		config.addDefault("AmonPack.Earth.EarthHammer.Range", 20);
		config.addDefault("AmonPack.Earth.EarthHammer.Radius", 2);

 		config.addDefault("AmonPack.Chi.Blades.Slash.Dmg-1", 1);
 		config.addDefault("AmonPack.Chi.Blades.Slash.Dmg-2", 1);
 		config.addDefault("AmonPack.Chi.Blades.Slash.Dmg-3", 2);
 		config.addDefault("AmonPack.Chi.Blades.Slash.Cooldown", 4000);
 		config.addDefault("AmonPack.Chi.Blades.Slash.SpeedPower", 3);
 		config.addDefault("AmonPack.Chi.Blades.Slash.SpeedDuration", 20);
 		config.addDefault("AmonPack.Chi.Blades.Slash.InvDuration", 20);
 		config.addDefault("AmonPack.Chi.Blades.Slash.EvadePower", 1);

 		config.addDefault("AmonPack.Chi.Blades.Pierce.SpeedPower", 3);
 		config.addDefault("AmonPack.Chi.Blades.Pierce.SpeedDuration", 60);
 		config.addDefault("AmonPack.Chi.Blades.Pierce.Dmg-1", 1);
 		config.addDefault("AmonPack.Chi.Blades.Pierce.Dmg-2", 3);
 		config.addDefault("AmonPack.Chi.Blades.Pierce.Cooldown", 4000);
 		config.addDefault("AmonPack.Chi.Blades.Pierce.DashPower", 2);

 		config.addDefault("AmonPack.Chi.Blades.Stab.Uses", 4);
 		config.addDefault("AmonPack.Chi.Blades.Stab.Dmg-Left", 2);
 		config.addDefault("AmonPack.Chi.Blades.Stab.Dmg-Right", 2);
 		config.addDefault("AmonPack.Chi.Blades.Stab.Cooldown", 4000);

 		config.addDefault("AmonPack.Chi.Blades.Counter.MaxHoldTime", 2500);
 		config.addDefault("AmonPack.Chi.Blades.Counter.EvadePower", 1);
 		config.addDefault("AmonPack.Chi.Blades.Counter.Cooldown", 4000);
 		config.addDefault("AmonPack.Chi.Blades.Counter.DashInAir", true);

 		config.addDefault("AmonPack.Air.AirPressure.MaxHoldTime", 4000);
 		config.addDefault("AmonPack.Air.AirPressure.Cooldown", 4000);
 		config.addDefault("AmonPack.Air.AirPressure.Dmg", 2);
 		config.addDefault("AmonPack.Air.AirPressure.Range-Sphere", 20);
 		config.addDefault("AmonPack.Air.AirPressure.Range-Pull", 4);
 		config.addDefault("AmonPack.Air.AirPressure.PushPower", 1.5);
 		config.addDefault("AmonPack.Air.AirPressure.MinHoldTime", 500);
 		config.addDefault("AmonPack.Air.AirPressure.CanControlSphere", false);

 		config.addDefault("AmonPack.Fire.Smoke.SmokeSurge.Cooldown", 4000);
 		config.addDefault("AmonPack.Fire.Smoke.SmokeSurge.Range", 30);
 		config.addDefault("AmonPack.Fire.Smoke.SmokeSurge.Dmg", 1);
 		config.addDefault("AmonPack.Fire.Smoke.SmokeSurge.SlowPower", 2);
 		config.addDefault("AmonPack.Fire.Smoke.SmokeSurge.SlowDuration", 40);
 		config.addDefault("AmonPack.Fire.Smoke.SmokeSurge.PoisonPower", 1);
 		config.addDefault("AmonPack.Fire.Smoke.SmokeSurge.PoisonDuration", 40);
 		config.addDefault("AmonPack.Fire.Smoke.SmokeSurge.BlindnessDuration", 40);
 		config.addDefault("AmonPack.Fire.Smoke.SmokeSurge.SmokeZoneDuration", 100);
 		config.addDefault("AmonPack.Fire.Smoke.SmokeSurge.SmokeZoneRange", 3);

 		config.addDefault("AmonPack.Fire.Smoke.SmokeDaggers.Cooldown", 4000);
 		config.addDefault("AmonPack.Fire.Smoke.SmokeDaggers.Range", 30);
 		config.addDefault("AmonPack.Fire.Smoke.SmokeDaggers.Dmg", 1);
 		config.addDefault("AmonPack.Fire.Smoke.SmokeDaggers.SlowPower", 2);
 		config.addDefault("AmonPack.Fire.Smoke.SmokeDaggers.SlowDuration", 40);
 		config.addDefault("AmonPack.Fire.Smoke.SmokeDaggers.PoisonPower", 1);
 		config.addDefault("AmonPack.Fire.Smoke.SmokeDaggers.PoisonDuration", 40);
 		config.addDefault("AmonPack.Fire.Smoke.SmokeDaggers.BlindnessDuration", 40);
 		config.addDefault("AmonPack.Fire.Smoke.SmokeDaggers.SmokeZoneDuration", 100);
 		config.addDefault("AmonPack.Fire.Smoke.SmokeDaggers.SmokeZoneRange", 2);

 		config.addDefault("AmonPack.Water.Ice.IceArch.Cooldown", 4000);
 		config.addDefault("AmonPack.Water.Ice.IceArch.Range", 20);
 		config.addDefault("AmonPack.Water.Ice.IceArch.ChargeTime", 1500);
        config.addDefault("AmonPack.Water.Ice.IceArch.Damage", 2);
 		config.addDefault("AmonPack.Water.Ice.IceArch.Arch-Width", 3);
 		config.addDefault("AmonPack.Water.Ice.IceArch.Arch-Duration", 5000);
        config.addDefault("AmonPack.Water.Ice.IceArch.Arch-Thickness", 2);
 		config.addDefault("AmonPack.Water.Ice.IceArch.CanFreeze", false);
 		config.addDefault("AmonPack.Water.Ice.IceArch.FreezeDuration", 2000);

 		config.addDefault("AmonPack.Water.Ice.IceThorn.FreezeDuration", 3000);
 		config.addDefault("AmonPack.Water.Ice.IceThorn.Cooldown", 7000);
 		config.addDefault("AmonPack.Water.Ice.IceThorn.Damage", 1);
 		config.addDefault("AmonPack.Water.Ice.IceThorn.ChargeTime", 2000);
 		config.addDefault("AmonPack.Water.Ice.IceThorn.RevertTime", 10000);
 		config.addDefault("AmonPack.Water.Ice.IceThorn.Range", 20);
 		config.addDefault("AmonPack.Water.Ice.IceThorn.Radius", 2);


 		config.addDefault("AmonPack.Water.Ice.IceArch.NightAugment.Cooldown", 4000);
 		config.addDefault("AmonPack.Water.Ice.IceArch.NightAugment.ChargeTime", 1000);
 		config.addDefault("AmonPack.Water.Ice.IceArch.NightAugment.Range", 30);
        config.addDefault("AmonPack.Water.Ice.IceArch.NightAugment.Damage", 3);
 		config.addDefault("AmonPack.Water.Ice.IceArch.NightAugment.Arch-Width", 4);
 		config.addDefault("AmonPack.Water.Ice.IceArch.NightAugment.Arch-Duration", 5000);
        config.addDefault("AmonPack.Water.Ice.IceArch.NightAugment.Arch-Thickness", 3);
 		config.addDefault("AmonPack.Water.Ice.IceArch.NightAugment.CanFreeze", false);
 		config.addDefault("AmonPack.Water.Ice.IceArch.NightAugment.FreezeDuration", 2000);

 		config.addDefault("AmonPack.Water.Ice.IceArch.FullMoonAugment.Cooldown", 1000);
 		config.addDefault("AmonPack.Water.Ice.IceArch.FullMoonAugment.ChargeTime", 500);
 		config.addDefault("AmonPack.Water.Ice.IceArch.FullMoonAugment.Range", 40);
        config.addDefault("AmonPack.Water.Ice.IceArch.FullMoonAugment.Damage", 5);
 		config.addDefault("AmonPack.Water.Ice.IceArch.FullMoonAugment.Arch-Width", 5);
 		config.addDefault("AmonPack.Water.Ice.IceArch.FullMoonAugment.Arch-Duration", 5000);
        config.addDefault("AmonPack.Water.Ice.IceArch.FullMoonAugment.Arch-Thickness", 4);
 		config.addDefault("AmonPack.Water.Ice.IceArch.FullMoonAugment.CanFreeze", false);
 		config.addDefault("AmonPack.Water.Ice.IceArch.FullMoonAugment.FreezeDuration", 2000);

		config.options().copyDefaults(true);
        saveConfig();
    }
	public static void reloadAllConfigs() {
		System.out.println("start test reload");
		LevelConfig = YamlConfiguration.loadConfiguration(new File(configpath, "Levels.yml"));
		AbilitiesConfig = YamlConfiguration.loadConfiguration(new File(configpath, "AbilitiesConfig.yml"));
		ForestConfig = YamlConfiguration.loadConfiguration(new File(configpath + File.separator + "RPG", "Forest.yml"));
		MinesConfig = YamlConfiguration.loadConfiguration(new File(configpath, "MinesConfigFile.yml"));
		MinesConfigFile = new File(configpath, "MinesConfigFile.yml");
		setMinesConfigFile(YamlConfiguration.loadConfiguration(MinesConfigFile));
		setDungeonsConfig(getMenagerieFilesReload());
		// = YamlConfiguration.loadConfiguration(new File(configpath, "PvPConfig.yml"));
		MenaMenager.ReloadMenageries();
		ForestMenager.LoadData();
		System.out.println("koniec test reload");
	}


	public static FileConfiguration getNewConfigz() {
		return newConfigz;
	}
	public static void setNewConfigz(FileConfiguration newConfigz) {
		AmonPackPlugin.newConfigz = newConfigz;
	}
	public static List<FileConfiguration> GetMenagerieConfig() {
		return MenagerieConfig;
	}
	public static FileConfiguration getMinesConfig() {
		return MinesConfig;
	}
	public static FileConfiguration getGuiConfig() {
		return GuiConfig;
	}
	/*public static FileConfiguration getWaveConfig() {
	//	return WaveConfig;
	}*/
	public static FileConfiguration getSkillTreeConfig() {
		return SkillTreeConfig;
	}
	public static void setDungeonsConfig(List<File> DungeonsConfig) {
		MenagerieConfig.clear();
		for(File f : DungeonsConfig){
			MenagerieConfig.add(YamlConfiguration.loadConfiguration(f));
		}
	}
	public static void setMinesConfigFile(FileConfiguration MinesConfig) {
		AmonPackPlugin.MinesConfig = MinesConfig;
	}
	public void setGuiConfig(FileConfiguration GuiConfig) {
		AmonPackPlugin.GuiConfig = GuiConfig;
	}
	/*public void setWaveConfig(FileConfiguration waveConfig) {
		AmonPackPlugin.WaveConfig = waveConfig;
	}*/
	public void setSkillTreeConfig(FileConfiguration SkillTreeConfig) {
		AmonPackPlugin.SkillTreeConfig = SkillTreeConfig;
	}
	public static FileConfiguration getPvPConfig() {
		return PvPConfig;
	}
	public void setPvPConfig(FileConfiguration PvPConfig) {
		AmonPackPlugin.PvPConfig =  PvPConfig;
	}
	public List<FileConfiguration> getMenagerieConfigs() {
		File dataFolder = getDataFolder();
		List<FileConfiguration> configs = new ArrayList<>();

		if (dataFolder.exists() && dataFolder.isDirectory()) {
			File[] files = dataFolder.listFiles((dir, name) -> name.startsWith("Menagerie") && name.endsWith(".yml"));

			if (files != null) {
				for (File file : files) {
					configs.add(YamlConfiguration.loadConfiguration(file));
				}
			}
		}
		return configs;
	}
	public List<File> getMenagerieFiles() {
		File menageriesFolder = new File(getDataFolder(), "Menageries");
		List<File> configs = new ArrayList<>();
		if (menageriesFolder.exists() && menageriesFolder.isDirectory()) {
			File[] files = menageriesFolder.listFiles((dir, name) -> name.startsWith("Menagerie") && name.endsWith(".yml"));
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
			File[] files = menageriesFolder.listFiles((dir, name) -> name.startsWith("Menagerie") && name.endsWith(".yml"));
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
	public static FileConfiguration getForestConfig() {
		return ForestConfig;
	}
}

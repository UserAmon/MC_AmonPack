package methods_plugins;

import AvatarSystems.Crafting.CraftingMenager;
import AvatarSystems.Gathering.ForestMenager;
import AvatarSystems.Gathering.MiningMenager;
import AvatarSystems.Perks.PerksMenager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class ConfigsMenager {

    private final File datafolder;
    private FileConfiguration crafting_Config;
    private FileConfiguration mining_Config;
    private FileConfiguration forest_Config;
    private FileConfiguration perks_Config;
    private FileConfiguration combat_Config;
    private File crafting_File;
    private File perks_File;
    private File mining_File;
    private File forest_File;
    private File combat_File;

    public CraftingMenager craftingMenager;
    public PerksMenager perks_menager;
    public MiningMenager mining_menager;
    public ForestMenager forest_menager;

    public ConfigsMenager(File datafolder) {
        System.out.println("Zaczynam ładowanie konfigów! " + datafolder);
        this.datafolder = datafolder;
        LoadAllConfigs();
        CheckAndLoadDefaults();
        SaveConfigs();
        System.out.println("Udało się! konfig!");
    }

    private void CheckAndLoadDefaults() {
        boolean changed = false;

        if (crafting_Config.getKeys(false).isEmpty()) {
            System.out.println("Crafting.yml jest pusty! Tworzenie wartości domyślnych...");
            addDefaultCraftingValues(crafting_Config);
            changed = true;
        }
        if (mining_Config.getKeys(false).isEmpty()) {
            System.out.println("Mining.yml jest pusty! Tworzenie wartości domyślnych...");
            addDefaultMiningValues(mining_Config);
            changed = true;
        }
        if (forest_Config.getKeys(false).isEmpty()) {
            System.out.println("Forest.yml jest pusty! Tworzenie wartości domyślnych...");
            addDefaultForestValues(forest_Config);
            changed = true;
        }
        if (combat_Config.getKeys(false).isEmpty()) {
            System.out.println("Combat.yml jest pusty! Tworzenie wartości domyślnych...");
            addDefaultCombatValues(combat_Config);
            changed = true;
        }
        /*
         * if (perks_Config.getKeys(false).isEmpty()) {
         * System.out.println("Perks.yml jest pusty! Tworzenie wartości domyślnych...");
         * addDefaultPerksValues(perks_Config);
         * changed = true;
         * }
         */

        if (changed) {
            SaveConfigs();
            ReloadMenagers();
        }
    }

    public void CreateMenagers() {
        craftingMenager = new CraftingMenager();
        mining_menager = new MiningMenager();
        forest_menager = new ForestMenager();
        // perks_menager = new PerksMenager();
    }

    public void ReloadMenagers() {
        if (craftingMenager != null)
            craftingMenager.ReloadConfig();
        if (mining_menager != null)
            mining_menager.ReloadConfig();
        if (forest_menager != null)
            forest_menager.ReloadConfig();
        if (AmonPackPlugin.combatMenager != null)
            AmonPackPlugin.combatMenager.ReloadConfig();
        // if (perks_menager != null) perks_menager.ReloadConfig();
    }

    public void LoadAllConfigs() {
        try {
            File rpgFolder = new File(datafolder, "RPG");

            if (!rpgFolder.exists())
                rpgFolder.mkdirs();

            crafting_File = new File(rpgFolder, "Crafting_Items.yml");
            mining_File = new File(rpgFolder, "Mining.yml");
            forest_File = new File(rpgFolder, "Forests.yml");
            combat_File = new File(rpgFolder, "Combat.yml");
            // perks_File = new File(rpgFolder, "Perks.yml");

            if (!crafting_File.exists())
                crafting_File.createNewFile();
            if (!mining_File.exists())
                mining_File.createNewFile();
            if (!forest_File.exists())
                forest_File.createNewFile();
            if (!combat_File.exists())
                combat_File.createNewFile();
            // if (!perks_File.exists()) perks_File.createNewFile();

            crafting_Config = YamlConfiguration.loadConfiguration(crafting_File);
            mining_Config = YamlConfiguration.loadConfiguration(mining_File);
            forest_Config = YamlConfiguration.loadConfiguration(forest_File);
            combat_Config = YamlConfiguration.loadConfiguration(combat_File);
            // perks_Config = YamlConfiguration.loadConfiguration(perks_File);

            System.out.println("pomyślnie zrobiono reload!");
        } catch (Exception e) {
            System.out.println("ERROR przy ładowaniu configów!!! " + e.getMessage());
        }
    }

    private void SaveConfigs() {
        try {
            crafting_Config.save(crafting_File);
            mining_Config.save(mining_File);
            forest_Config.save(forest_File);
            combat_Config.save(combat_File);
            // perks_Config.save(perks_File);
        } catch (IOException e) {
            System.out.println("Błąd z konfigiem! " + e.getMessage());
        }
    }

    public FileConfiguration getCrafting_Config() {
        return crafting_Config;
    }

    public FileConfiguration getPerks_Config() {
        return perks_Config;
    }

    public FileConfiguration getMining_Config() {
        return mining_Config;
    }

    public FileConfiguration getForest_Config() {
        return forest_Config;
    }

    public FileConfiguration getCombat_Config() {
        return combat_Config;
    }

    private void addDefaultPerksValues(FileConfiguration cfg) {
        cfg.set("perks.start.health_bonus", 4);
        cfg.set("perks.start.speed_bonus", 0.05);
    }

    private void addDefaultCombatValues(FileConfiguration cfg) {
        cfg.set("Combat.DefaultRegion.World", "world");
        cfg.set("Combat.DefaultRegion.Exp.ZOMBIE", 5);
        cfg.set("Combat.DefaultRegion.Exp.SKELETON", 5);
        cfg.set("Combat.DefaultRegion.Exp.SPIDER", 5);
    }

    private void addDefaultForestValues(FileConfiguration cfg) {
        String worldKey = "f1";
        cfg.set("AmonPack.Forest." + worldKey + ".World", "world");
        cfg.set("AmonPack.Forest." + worldKey + ".Exp.LEAVES", 0.3);
        cfg.set("AmonPack.Forest." + worldKey + ".Exp.LOG", 2.0);
        cfg.set("AmonPack.Forest." + worldKey + ".Exp.WOOD", 1.5);
        cfg.set("AmonPack.Forest." + worldKey + ".Exp.STRIPPED_WOOD", 1.8);
        cfg.set("AmonPack.Forest." + worldKey + ".Exp.STRIPPED_LOG", 2.0);

        cfg.set("AmonPack.LumberingBlocks", List.of(
                // LOGS
                "OAK_LOG", "SPRUCE_LOG", "BIRCH_LOG", "JUNGLE_LOG",
                "ACACIA_LOG", "DARK_OAK_LOG", "MANGROVE_LOG", "CHERRY_LOG", "BAMBOO_BLOCK",
                // WOOD
                "OAK_WOOD", "SPRUCE_WOOD", "BIRCH_WOOD", "JUNGLE_WOOD",
                "ACACIA_WOOD", "DARK_OAK_WOOD", "MANGROVE_WOOD", "CHERRY_WOOD",
                // STRIPPED LOGS
                "STRIPPED_OAK_LOG", "STRIPPED_SPRUCE_LOG", "STRIPPED_BIRCH_LOG", "STRIPPED_JUNGLE_LOG",
                "STRIPPED_ACACIA_LOG", "STRIPPED_DARK_OAK_LOG", "STRIPPED_MANGROVE_LOG", "STRIPPED_CHERRY_LOG",
                "STRIPPED_BAMBOO_BLOCK",
                // STRIPPED WOOD
                "STRIPPED_OAK_WOOD", "STRIPPED_SPRUCE_WOOD", "STRIPPED_BIRCH_WOOD", "STRIPPED_JUNGLE_WOOD",
                "STRIPPED_ACACIA_WOOD", "STRIPPED_DARK_OAK_WOOD", "STRIPPED_MANGROVE_WOOD", "STRIPPED_CHERRY_WOOD",
                // LEAVES
                "OAK_LEAVES", "SPRUCE_LEAVES", "BIRCH_LEAVES", "JUNGLE_LEAVES",
                "ACACIA_LEAVES", "DARK_OAK_LEAVES", "MANGROVE_LEAVES", "CHERRY_LEAVES",
                "AZALEA_LEAVES", "FLOWERING_AZALEA_LEAVES"));

    }

    private void addDefaultMiningValues(FileConfiguration cfg) {

        cfg.set("AmonPack.Mining.mine_1.World", "world");

        cfg.set("AmonPack.Mining.mine_1.Loot.COAL", 5);
        cfg.set("AmonPack.Mining.mine_1.Loot.IRON_INGOT", 5);
        cfg.set("AmonPack.Mining.mine_1.Loot.GOLD_INGOT", 5);
        cfg.set("AmonPack.Mining.mine_1.Loot.ia:meteor_shard", 2);
        cfg.set("AmonPack.Mining.mine_1.Loot.ia:basalt_shard", 5);

        cfg.set("AmonPack.Mining.mine_1.Exp.COAL_ORE", 1.5);
        cfg.set("AmonPack.Mining.mine_1.Exp.IRON_ORE", 3.0);
        cfg.set("AmonPack.Mining.mine_1.Exp.GOLD_ORE", 4.0);

        cfg.set("AmonPack.Mining.mine_1.ItemsAdderExp.ia:meteoryt_ore", 11.75);
        cfg.set("AmonPack.Mining.mine_1.ItemsAdderExp.ia:basalt_ore", 6.5);

        cfg.set("AmonPack.ItemsAdderMining.ia:meteoryt_ore", "ia:meteor_shard");
        cfg.set("AmonPack.ItemsAdderMining.ia:basalt_ore", "ia:basalt_shard");

        cfg.set("AmonPack.MiningBlocks", Arrays.asList(
                "COAL_ORE",
                "COPPER_ORE",
                "IRON_ORE",
                "GOLD_ORE",
                "REDSTONE_ORE",
                "EMERALD_ORE",
                "LAPIS_ORE",
                "DIAMOND_ORE",

                "GILDED_BLACKSTONE",
                "AMETHYST_CLUSTER",
                "BLOCK_OF_AMETHYST",
                "SMALL_AMETHYST_BUD",
                "MEDIUM_AMETHYST_BUD",
                "LARGE_AMETHYST_BUD",

                "STONE",
                "DEEPSLATE",
                "TUFF",
                "CALCITE",
                "BASALT",
                "NETHERRACK",
                "BLACKSTONE"));

        cfg.set("AmonPack.Farms.farm1.World", "world");
        cfg.set("AmonPack.Farms.farm1.Exp.WHEAT", 1.0);
        cfg.set("AmonPack.Farms.farm1.Exp.CARROTS", 1.0);
        cfg.set("AmonPack.Farms.farm1.Exp.POTATOES", 1.0);

        cfg.set("AmonPack.FarmingBlocks", Arrays.asList(
                "WHEAT", "WHEAT_SEEDS",
                "CARROTS",
                "POTATOES",
                "BEETROOTS",
                "MELON", "MELON_STEM",
                "PUMPKIN", "PUMPKIN_STEM",
                "SUGAR_CANE",
                "BAMBOO",
                "NETHER_WART",
                "COCOA",
                "KELP",
                "SEAGRASS",
                "SWEET_BERRY_BUSH"));

    }

    private void addDefaultCraftingValues(FileConfiguration cfg) {
        // Magic Effects
        cfg.set("MagicEffects.Monster_Hunter.Name", "§cŁowca Potworów");
        cfg.set("MagicEffects.Monster_Hunter.Lore.l1", "§7Zadaje dodatkowe obrażenia potworom.");
        cfg.set("MagicEffects.Monster_Hunter.IsMajor", false);
        cfg.set("MagicEffects.Monster_Hunter.Conditions.req1.Skill_Type", "COMBAT");
        cfg.set("MagicEffects.Monster_Hunter.Conditions.req1.Skill_Level", 1);
        cfg.set("MagicEffects.Monster_Hunter.Cost.c1.Material", "ROTTEN_FLESH");
        cfg.set("MagicEffects.Monster_Hunter.Cost.c1.Amount", 10);

        cfg.set("MagicEffects.Earth_Resolve_Dmg_Taking.Name", "§aDeterminacja Ziemi");
        cfg.set("MagicEffects.Earth_Resolve_Dmg_Taking.Lore.l1", "§7Redukuje obrażenia gdy jesteś blisko ziemi.");
        cfg.set("MagicEffects.Earth_Resolve_Dmg_Taking.IsMajor", true);
        cfg.set("MagicEffects.Earth_Resolve_Dmg_Taking.Conditions.req1.Skill_Type", "MINING");
        cfg.set("MagicEffects.Earth_Resolve_Dmg_Taking.Conditions.req1.Skill_Level", 5);
        cfg.set("MagicEffects.Earth_Resolve_Dmg_Taking.Cost.c1.Material", "COBBLESTONE");
        cfg.set("MagicEffects.Earth_Resolve_Dmg_Taking.Cost.c1.Amount", 64);

        cfg.set("MagicEffects.Earth_Damage_Boost_Absorb.Name", "§aWzmocnienie Absorpcji");
        cfg.set("MagicEffects.Earth_Damage_Boost_Absorb.Lore.l1",
                "§7Zwiększa obrażenia gdy posiadasz efekt absorpcji.");
        cfg.set("MagicEffects.Earth_Damage_Boost_Absorb.IsMajor", false);
        cfg.set("MagicEffects.Earth_Damage_Boost_Absorb.Conditions.req1.Skill_Type", "MINING");
        cfg.set("MagicEffects.Earth_Damage_Boost_Absorb.Conditions.req1.Skill_Level", 3);
        cfg.set("MagicEffects.Earth_Damage_Boost_Absorb.Cost.c1.Material", "GOLDEN_APPLE");
        cfg.set("MagicEffects.Earth_Damage_Boost_Absorb.Cost.c1.Amount", 1);

        cfg.set("MagicEffects.Earth_Damage_Boost_Hight.Name", "§aPrzewaga Wysokości");
        cfg.set("MagicEffects.Earth_Damage_Boost_Hight.Lore.l1", "§7Zadajesz więcej obrażeń będąc wyżej od celu.");
        cfg.set("MagicEffects.Earth_Damage_Boost_Hight.IsMajor", false);
        cfg.set("MagicEffects.Earth_Damage_Boost_Hight.Conditions.req1.Skill_Type", "MINING");
        cfg.set("MagicEffects.Earth_Damage_Boost_Hight.Conditions.req1.Skill_Level", 3);
        cfg.set("MagicEffects.Earth_Damage_Boost_Hight.Cost.c1.Material", "DIRT");
        cfg.set("MagicEffects.Earth_Damage_Boost_Hight.Cost.c1.Amount", 32);

        cfg.set("MagicEffects.Fire_Damage_Boost.Name", "§cOgniste Wzmocnienie");
        cfg.set("MagicEffects.Fire_Damage_Boost.Lore.l1", "§7Zadajesz więcej obrażeń płonącym celom.");
        cfg.set("MagicEffects.Fire_Damage_Boost.IsMajor", false);
        cfg.set("MagicEffects.Fire_Damage_Boost.Conditions.req1.Skill_Type", "COMBAT");
        cfg.set("MagicEffects.Fire_Damage_Boost.Conditions.req1.Skill_Level", 3);
        cfg.set("MagicEffects.Fire_Damage_Boost.Cost.c1.Material", "BLAZE_POWDER");
        cfg.set("MagicEffects.Fire_Damage_Boost.Cost.c1.Amount", 5);

        cfg.set("MagicEffects.Fire_Speed_Boost.Name", "§cOgnista Prędkość");
        cfg.set("MagicEffects.Fire_Speed_Boost.Lore.l1", "§7Otrzymujesz prędkość po uderzeniu płonącego celu.");
        cfg.set("MagicEffects.Fire_Speed_Boost.IsMajor", false);
        cfg.set("MagicEffects.Fire_Speed_Boost.Conditions.req1.Skill_Type", "COMBAT");
        cfg.set("MagicEffects.Fire_Speed_Boost.Conditions.req1.Skill_Level", 4);
        cfg.set("MagicEffects.Fire_Speed_Boost.Cost.c1.Material", "SUGAR");
        cfg.set("MagicEffects.Fire_Speed_Boost.Cost.c1.Amount", 10);

        cfg.set("MagicEffects.Knockback.Name", "§fOdrzut");
        cfg.set("MagicEffects.Knockback.Lore.l1", "§7Odrzuca wrogów przy uderzeniu.");
        cfg.set("MagicEffects.Knockback.IsMajor", false);
        cfg.set("MagicEffects.Knockback.Conditions.req1.Skill_Type", "COMBAT");
        cfg.set("MagicEffects.Knockback.Conditions.req1.Skill_Level", 2);
        cfg.set("MagicEffects.Knockback.Cost.c1.Material", "PISTON");
        cfg.set("MagicEffects.Knockback.Cost.c1.Amount", 1);

        cfg.set("MagicEffects.Burrow.Name", "§aZakopanie");
        cfg.set("MagicEffects.Burrow.Lore.l1", "§7Szansa na zakopanie wroga w ziemi.");
        cfg.set("MagicEffects.Burrow.IsMajor", true);
        cfg.set("MagicEffects.Burrow.Conditions.req1.Skill_Type", "MINING");
        cfg.set("MagicEffects.Burrow.Conditions.req1.Skill_Level", 6);
        cfg.set("MagicEffects.Burrow.Cost.c1.Material", "SAND");
        cfg.set("MagicEffects.Burrow.Cost.c1.Amount", 32);

        cfg.set("MagicEffects.Fire_Aspect.Name", "§cZaklęty Ogień");
        cfg.set("MagicEffects.Fire_Aspect.Lore.l1", "§7Podpala wrogów przy uderzeniu.");
        cfg.set("MagicEffects.Fire_Aspect.IsMajor", false);
        cfg.set("MagicEffects.Fire_Aspect.Conditions.req1.Skill_Type", "COMBAT");
        cfg.set("MagicEffects.Fire_Aspect.Conditions.req1.Skill_Level", 3);
        cfg.set("MagicEffects.Fire_Aspect.Cost.c1.Material", "FLINT_AND_STEEL");
        cfg.set("MagicEffects.Fire_Aspect.Cost.c1.Amount", 1);

        cfg.set("MagicEffects.Smoke_Aspect.Name", "§8Dymna Zasłona");
        cfg.set("MagicEffects.Smoke_Aspect.Lore.l1", "§7Tworzy dym wokół ciebie.");
        cfg.set("MagicEffects.Smoke_Aspect.IsMajor", false);
        cfg.set("MagicEffects.Smoke_Aspect.Conditions.req1.Skill_Type", "COMBAT");
        cfg.set("MagicEffects.Smoke_Aspect.Conditions.req1.Skill_Level", 2);
        cfg.set("MagicEffects.Smoke_Aspect.Cost.c1.Material", "COAL");
        cfg.set("MagicEffects.Smoke_Aspect.Cost.c1.Amount", 10);

        cfg.set("MagicEffects.Minor_Air_Sound_Damage_Buff.Name", "§fDźwiękowe Wzmocnienie");
        cfg.set("MagicEffects.Minor_Air_Sound_Damage_Buff.Lore.l1",
                "§7Zwiększa obrażenia od umiejętności dźwiękowych.");
        cfg.set("MagicEffects.Minor_Air_Sound_Damage_Buff.IsMajor", false);
        cfg.set("MagicEffects.Minor_Air_Sound_Damage_Buff.Conditions.req1.Skill_Type", "COMBAT");
        cfg.set("MagicEffects.Minor_Air_Sound_Damage_Buff.Conditions.req1.Skill_Level", 4);
        cfg.set("MagicEffects.Minor_Air_Sound_Damage_Buff.Cost.c1.Material", "NOTE_BLOCK");
        cfg.set("MagicEffects.Minor_Air_Sound_Damage_Buff.Cost.c1.Amount", 5);

        cfg.set("MagicEffects.Major_Air_Sound_Hit.Name", "§fUderzenie Dźwięku");
        cfg.set("MagicEffects.Major_Air_Sound_Hit.Lore.l1", "§7Wywołuje potężną falę dźwiękową przy uderzeniu.");
        cfg.set("MagicEffects.Major_Air_Sound_Hit.IsMajor", true);
        cfg.set("MagicEffects.Major_Air_Sound_Hit.Conditions.req1.Skill_Type", "COMBAT");
        cfg.set("MagicEffects.Major_Air_Sound_Hit.Conditions.req1.Skill_Level", 7);
        cfg.set("MagicEffects.Major_Air_Sound_Hit.Cost.c1.Material", "JUKEBOX");
        cfg.set("MagicEffects.Major_Air_Sound_Hit.Cost.c1.Amount", 1);

        cfg.set("MagicEffects.Earth_Hammer_Aspect.Name", "§aMłot Ziemi");
        cfg.set("MagicEffects.Earth_Hammer_Aspect.Lore.l1", "§7Twoje ataki wstrząsają ziemią.");
        cfg.set("MagicEffects.Earth_Hammer_Aspect.IsMajor", true);
        cfg.set("MagicEffects.Earth_Hammer_Aspect.Conditions.req1.Skill_Type", "MINING");
        cfg.set("MagicEffects.Earth_Hammer_Aspect.Conditions.req1.Skill_Level", 7);
        cfg.set("MagicEffects.Earth_Hammer_Aspect.Cost.c1.Material", "ANVIL");
        cfg.set("MagicEffects.Earth_Hammer_Aspect.Cost.c1.Amount", 1);

        cfg.set("MagicEffects.Minor_Water_Icy_Slowness_Hit.Name", "§bLodowe Spowolnienie");
        cfg.set("MagicEffects.Minor_Water_Icy_Slowness_Hit.Lore.l1", "§7Spowalnia wrogów przy uderzeniu.");
        cfg.set("MagicEffects.Minor_Water_Icy_Slowness_Hit.IsMajor", false);
        cfg.set("MagicEffects.Minor_Water_Icy_Slowness_Hit.Conditions.req1.Skill_Type", "COMBAT");
        cfg.set("MagicEffects.Minor_Water_Icy_Slowness_Hit.Conditions.req1.Skill_Level", 3);
        cfg.set("MagicEffects.Minor_Water_Icy_Slowness_Hit.Cost.c1.Material", "ICE");
        cfg.set("MagicEffects.Minor_Water_Icy_Slowness_Hit.Cost.c1.Amount", 10);

        cfg.set("MagicEffects.Air_Thrust.Name", "§fPowietrzne Pchnięcie");
        cfg.set("MagicEffects.Air_Thrust.Lore.l1", "§7Wyrzuca wrogów w powietrze.");
        cfg.set("MagicEffects.Air_Thrust.IsMajor", false);
        cfg.set("MagicEffects.Air_Thrust.Conditions.req1.Skill_Type", "COMBAT");
        cfg.set("MagicEffects.Air_Thrust.Conditions.req1.Skill_Level", 4);
        cfg.set("MagicEffects.Air_Thrust.Cost.c1.Material", "FEATHER");
        cfg.set("MagicEffects.Air_Thrust.Cost.c1.Amount", 10);

        cfg.set("MagicEffects.Air_Damage_Boost_Downward.Name", "§fPowietrzna Dominacja");
        cfg.set("MagicEffects.Air_Damage_Boost_Downward.Lore.l1", "§7Zadajesz więcej obrażeń spadając na wroga.");
        cfg.set("MagicEffects.Air_Damage_Boost_Downward.IsMajor", false);
        cfg.set("MagicEffects.Air_Damage_Boost_Downward.Conditions.req1.Skill_Type", "COMBAT");
        cfg.set("MagicEffects.Air_Damage_Boost_Downward.Conditions.req1.Skill_Level", 5);
        cfg.set("MagicEffects.Air_Damage_Boost_Downward.Cost.c1.Material", "PHANTOM_MEMBRANE");
        cfg.set("MagicEffects.Air_Damage_Boost_Downward.Cost.c1.Amount", 5);

        cfg.set("MagicEffects.Earth_1.Name", "§aZiemne Odłamki");
        cfg.set("MagicEffects.Earth_1.Lore.l1", "§7Przyzywa spadające bloki ziemi.");
        cfg.set("MagicEffects.Earth_1.IsMajor", false);
        cfg.set("MagicEffects.Earth_1.Conditions.req1.Skill_Type", "MINING");
        cfg.set("MagicEffects.Earth_1.Conditions.req1.Skill_Level", 4);
        cfg.set("MagicEffects.Earth_1.Cost.c1.Material", "DIRT");
        cfg.set("MagicEffects.Earth_1.Cost.c1.Amount", 16);

        cfg.set("MagicEffects.Ice_Thorn_Ability_Aspect.Name", "§bLodowy Kolec");
        cfg.set("MagicEffects.Ice_Thorn_Ability_Aspect.Lore.l1", "§7Wystrzeliwuje lodowe kolce.");
        cfg.set("MagicEffects.Ice_Thorn_Ability_Aspect.IsMajor", true);
        cfg.set("MagicEffects.Ice_Thorn_Ability_Aspect.Conditions.req1.Skill_Type", "COMBAT");
        cfg.set("MagicEffects.Ice_Thorn_Ability_Aspect.Conditions.req1.Skill_Level", 6);
        cfg.set("MagicEffects.Ice_Thorn_Ability_Aspect.Cost.c1.Material", "PACKED_ICE");
        cfg.set("MagicEffects.Ice_Thorn_Ability_Aspect.Cost.c1.Amount", 10);

        cfg.set("MagicEffects.Ice_Encase.Name", "§bZamrożenie");
        cfg.set("MagicEffects.Ice_Encase.Lore.l1", "§7Zamraża wroga w lodzie.");
        cfg.set("MagicEffects.Ice_Encase.IsMajor", true);
        cfg.set("MagicEffects.Ice_Encase.Conditions.req1.Skill_Type", "COMBAT");
        cfg.set("MagicEffects.Ice_Encase.Conditions.req1.Skill_Level", 8);
        cfg.set("MagicEffects.Ice_Encase.Cost.c1.Material", "BLUE_ICE");
        cfg.set("MagicEffects.Ice_Encase.Cost.c1.Amount", 5);

        cfg.set("MagicEffects.Lightning_Aspect.Name", "§ePiorun");
        cfg.set("MagicEffects.Lightning_Aspect.Lore.l1", "§7Uderza piorunem przy ataku.");
        cfg.set("MagicEffects.Lightning_Aspect.IsMajor", true);
        cfg.set("MagicEffects.Lightning_Aspect.Conditions.req1.Skill_Type", "COMBAT");
        cfg.set("MagicEffects.Lightning_Aspect.Conditions.req1.Skill_Level", 8);
        cfg.set("MagicEffects.Lightning_Aspect.Cost.c1.Material", "LIGHTNING_ROD");
        cfg.set("MagicEffects.Lightning_Aspect.Cost.c1.Amount", 1);

        cfg.set("MagicEffects.Looting.Name", "§eGrabież");
        cfg.set("MagicEffects.Looting.Lore.l1", "§7Zwiększa szansę na drop przedmiotów.");
        cfg.set("MagicEffects.Looting.IsMajor", false);
        cfg.set("MagicEffects.Looting.Conditions.req1.Skill_Type", "MINING");
        cfg.set("MagicEffects.Looting.Conditions.req1.Skill_Level", 5);
        cfg.set("MagicEffects.Looting.Cost.c1.Material", "EMERALD");
        cfg.set("MagicEffects.Looting.Cost.c1.Amount", 5);

        cfg.set("MagicEffects.Expierience.Name", "§aDoświadczenie");
        cfg.set("MagicEffects.Expierience.Lore.l1", "§7Zwiększa zdobywane doświadczenie.");
        cfg.set("MagicEffects.Expierience.IsMajor", false);
        cfg.set("MagicEffects.Expierience.Conditions.req1.Skill_Type", "MINING");
        cfg.set("MagicEffects.Expierience.Conditions.req1.Skill_Level", 3);
        cfg.set("MagicEffects.Expierience.Cost.c1.Material", "EXPERIENCE_BOTTLE");
        cfg.set("MagicEffects.Expierience.Cost.c1.Amount", 10);

        cfg.set("MagicEffects.Earth_Health_Boost.Name", "§aWitalność Ziemi");
        cfg.set("MagicEffects.Earth_Health_Boost.Lore.l1", "§7Otrzymujesz absorpcję po zabiciu wroga.");
        cfg.set("MagicEffects.Earth_Health_Boost.IsMajor", false);
        cfg.set("MagicEffects.Earth_Health_Boost.Conditions.req1.Skill_Type", "MINING");
        cfg.set("MagicEffects.Earth_Health_Boost.Conditions.req1.Skill_Level", 4);
        cfg.set("MagicEffects.Earth_Health_Boost.Cost.c1.Material", "APPLE");
        cfg.set("MagicEffects.Earth_Health_Boost.Cost.c1.Amount", 5);

        cfg.set("MagicEffects.Midas.Name", "§6Dotyk Midasa");
        cfg.set("MagicEffects.Midas.Lore.l1", "§7Szansa na zdobycie pieniędzy przy zabiciu.");
        cfg.set("MagicEffects.Midas.IsMajor", true);
        cfg.set("MagicEffects.Midas.Conditions.req1.Skill_Type", "MINING");
        cfg.set("MagicEffects.Midas.Conditions.req1.Skill_Level", 8);
        cfg.set("MagicEffects.Midas.Cost.c1.Material", "GOLD_INGOT");
        cfg.set("MagicEffects.Midas.Cost.c1.Amount", 20);

        cfg.set("MagicEffects.fire_burst.Name", "Ognisty Wybuch");
        cfg.set("MagicEffects.fire_burst.Lore.l1", "§cSilne rozgrzanie broni");
        cfg.set("MagicEffects.fire_burst.Lore.l2", "§6Szansa na podpalenie wroga");
        cfg.set("MagicEffects.fire_burst.IsMajor", true);
        cfg.set("MagicEffects.fire_burst.Conditions.req1.Skill_Type", "MINING");
        cfg.set("MagicEffects.fire_burst.Conditions.req1.Skill_Level", 2);
        cfg.set("MagicEffects.fire_burst.Cost.c1.Material", "BLAZE_POWDER");
        cfg.set("MagicEffects.fire_burst.Cost.c1.Amount", 5);

        cfg.set("MagicEffects.frost_touch.Name", "Mroźny Dotyk");
        cfg.set("MagicEffects.frost_touch.Lore.l1", "§bZamraża cel na chwilę");
        cfg.set("MagicEffects.frost_touch.IsMajor", false);
        cfg.set("MagicEffects.frost_touch.Conditions.req1.Skill_Type", "COMBAT");
        cfg.set("MagicEffects.frost_touch.Conditions.req1.Skill_Level", 2);
        cfg.set("MagicEffects.frost_touch.Cost.c1.Material", "SNOWBALL");
        cfg.set("MagicEffects.frost_touch.Cost.c1.Amount", 8);

        cfg.set("Craftable_Items_Molds.bamboo_staff.Material", "WOODEN_SWORD");
        cfg.set("Craftable_Items_Molds.bamboo_staff.Name", "§6Laska Bambusowa");
        cfg.set("Craftable_Items_Molds.bamboo_staff.Custom_Model_ID", 10003);
        cfg.set("Craftable_Items_Molds.bamboo_staff.Base_Damage", 3);
        cfg.set("Craftable_Items_Molds.bamboo_staff.Mold.Items_To_Craft.m1.Material", "BAMBOO");
        cfg.set("Craftable_Items_Molds.bamboo_staff.Mold.Items_To_Craft.m1.Amount", 4);
        cfg.set("Craftable_Items_Molds.bamboo_staff.Mold.AllowedMagicEffects",
                Arrays.asList("fire_burst", "frost_touch"));
        cfg.set("Craftable_Items_Molds.bamboo_staff.Item.Lore.l1", "§7Kute w starożytnej kuźni.");
        cfg.set("Craftable_Items_Molds.bamboo_staff.Item.Lore.l2", "§bMoc żywiołów zamknięta w ostrzu.");
        cfg.set("Craftable_Items_Molds.bamboo_staff.Item.Items_To_Craft.c1.Material", "STICK");
        cfg.set("Craftable_Items_Molds.bamboo_staff.Item.Items_To_Craft.c1.Amount", 1);

        cfg.set("Craftable_Items_Molds.earth_hammer.Material", "WOODEN_SWORD");
        cfg.set("Craftable_Items_Molds.earth_hammer.Name", "§aWachlarz Wojowniczek Kyoshi");
        cfg.set("Craftable_Items_Molds.earth_hammer.Custom_Model_ID", 10001);
        cfg.set("Craftable_Items_Molds.earth_hammer.Base_Damage", 2);
        cfg.set("Craftable_Items_Molds.earth_hammer.Mold.Items_To_Craft.m1.Material", "PAPER");
        cfg.set("Craftable_Items_Molds.earth_hammer.Mold.Items_To_Craft.m1.Amount", 8);
        cfg.set("Craftable_Items_Molds.earth_hammer.Mold.Items_To_Craft.m2.Material", "STICK");
        cfg.set("Craftable_Items_Molds.earth_hammer.Mold.Items_To_Craft.m2.Amount", 2);
        cfg.set("Craftable_Items_Molds.earth_hammer.Mold.AllowedMagicEffects", Arrays.asList());
        cfg.set("Craftable_Items_Molds.earth_hammer.Item.Lore.l1", "§aNiezwykle ciężki młot wykonany z twardej skały.");
        cfg.set("Craftable_Items_Molds.earth_hammer.Item.Items_To_Craft.c1.Material", "STICK");
        cfg.set("Craftable_Items_Molds.earth_hammer.Item.Items_To_Craft.c1.Amount", 2);

        // Craftable Items Defaults
        cfg.set("Craftable_Items.Test_Item.Material", "PAPER");
        cfg.set("Craftable_Items.Test_Item.Name", "§aTest Item");
        cfg.set("Craftable_Items.Test_Item.Custom_Model_ID", 1001);
        cfg.set("Craftable_Items.Test_Item.Mold.Items_To_Craft.m1.Material", "DIRT");
        cfg.set("Craftable_Items.Test_Item.Mold.Items_To_Craft.m1.Amount", 1);
        cfg.set("Craftable_Items.Test_Item.Mold.AllowedMagicEffects", Arrays.asList("Item_Effect_Test"));
        cfg.set("Craftable_Items.Test_Item.Item.Lore.l1", "§7A test item.");

        // Item Magic Effect Default
        cfg.set("MagicEffects.Item_Effect_Test.Name", "§bItem Effect");
        cfg.set("MagicEffects.Item_Effect_Test.Lore.l1", "§7Does something magical.");
        cfg.set("MagicEffects.Item_Effect_Test.IsMajor", false);
        cfg.set("MagicEffects.Item_Effect_Test.IsItemEffect", true);
        cfg.set("MagicEffects.Item_Effect_Test.Conditions.req1.Skill_Type", "MINING");
        cfg.set("MagicEffects.Item_Effect_Test.Conditions.req1.Skill_Level", 1);
        cfg.set("MagicEffects.Item_Effect_Test.Cost.c1.Material", "STONE");
        cfg.set("MagicEffects.Item_Effect_Test.Cost.c1.Amount", 1);
    }

}

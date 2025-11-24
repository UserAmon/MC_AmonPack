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
    private File crafting_File;
    private File perks_File;
    private File mining_File;
    private File forest_File;

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
/*
        if (perks_Config.getKeys(false).isEmpty()) {
            System.out.println("Perks.yml jest pusty! Tworzenie wartości domyślnych...");
            addDefaultPerksValues(perks_Config);
            changed = true;
        }*/

        if (changed) {
            SaveConfigs();
            ReloadMenagers();
        }
    }

    private void addDefaultCraftingValues(FileConfiguration cfg) {
        cfg.set("example_item.name", "Magic Sword");
        cfg.set("example_item.damage", 10);
        cfg.set("example_item.material", "DIAMOND_SWORD");
    }


    private void addDefaultPerksValues(FileConfiguration cfg) {
        cfg.set("perks.start.health_bonus", 4);
        cfg.set("perks.start.speed_bonus", 0.05);
    }

    public void CreateMenagers() {
        craftingMenager = new CraftingMenager();
        mining_menager = new MiningMenager();
        forest_menager = new ForestMenager();
        //perks_menager = new PerksMenager();
    }

    public void ReloadMenagers() {
        if (craftingMenager != null) craftingMenager.ReloadConfig();
        if (mining_menager != null) mining_menager.ReloadConfig();
        if (forest_menager != null) forest_menager.ReloadConfig();
        //if (perks_menager != null) perks_menager.ReloadConfig();
    }

    public void LoadAllConfigs() {
        try {
            File rpgFolder = new File(datafolder, "RPG");

            if (!rpgFolder.exists()) rpgFolder.mkdirs();

            crafting_File = new File(rpgFolder, "Crafting_Items.yml");
            mining_File = new File(rpgFolder, "Mining.yml");
            forest_File = new File(rpgFolder, "Forests.yml");
            //perks_File = new File(rpgFolder, "Perks.yml");

            if (!crafting_File.exists()) crafting_File.createNewFile();
            if (!mining_File.exists()) mining_File.createNewFile();
            if (!forest_File.exists()) forest_File.createNewFile();
            //if (!perks_File.exists()) perks_File.createNewFile();

            crafting_Config = YamlConfiguration.loadConfiguration(crafting_File);
            mining_Config = YamlConfiguration.loadConfiguration(mining_File);
            forest_Config = YamlConfiguration.loadConfiguration(forest_File);
            //perks_Config = YamlConfiguration.loadConfiguration(perks_File);

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
            //perks_Config.save(perks_File);
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
                "STRIPPED_ACACIA_LOG", "STRIPPED_DARK_OAK_LOG", "STRIPPED_MANGROVE_LOG", "STRIPPED_CHERRY_LOG", "STRIPPED_BAMBOO_BLOCK",
                // STRIPPED WOOD
                "STRIPPED_OAK_WOOD", "STRIPPED_SPRUCE_WOOD", "STRIPPED_BIRCH_WOOD", "STRIPPED_JUNGLE_WOOD",
                "STRIPPED_ACACIA_WOOD", "STRIPPED_DARK_OAK_WOOD", "STRIPPED_MANGROVE_WOOD", "STRIPPED_CHERRY_WOOD",
                // LEAVES
                "OAK_LEAVES", "SPRUCE_LEAVES", "BIRCH_LEAVES", "JUNGLE_LEAVES",
                "ACACIA_LEAVES", "DARK_OAK_LEAVES", "MANGROVE_LEAVES", "CHERRY_LEAVES",
                "AZALEA_LEAVES", "FLOWERING_AZALEA_LEAVES"
        ));


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
                "BLACKSTONE"
        ));

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
                "SWEET_BERRY_BUSH"
        ));

    }

}

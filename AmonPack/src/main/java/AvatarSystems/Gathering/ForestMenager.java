package AvatarSystems.Gathering;

import AvatarSystems.Gathering.Objects.Farm;
import AvatarSystems.Gathering.Objects.Forest;
import AvatarSystems.Gathering.Objects.Mine;
import AvatarSystems.Util_Objects.InventoryXHolder;
import AvatarSystems.Util_Objects.LevelSkill;
import AvatarSystems.Crafting.CraftingMenager;
import AvatarSystems.Util_Objects.PlayerLevel;
import AvatarSystems.Util_Objects.Resource;
import commands.Commands;
import methods_plugins.AmonPackPlugin;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

import static AvatarSystems.Gathering.MiningMenager.isNaturalBlock;
import static methods_plugins.Methods.getRandom;

public class ForestMenager {
    public static List<Forest> ForestWorlds = new ArrayList<>();
    static List<Material> ForestBlocks = new ArrayList<>();

    public ForestMenager() {
        ReloadConfig();
    }

    public void ReloadConfig() {
        ForestWorlds = new ArrayList<>();
        FileConfiguration Config = AmonPackPlugin.getConfigs_menager().getForest_Config();
        for (String key : Objects.requireNonNull(Config.getConfigurationSection("AmonPack.Forest")).getKeys(false)) {
            String World = Config.getString("AmonPack.Forest." + key + ".World");
            HashMap<Material, Double> ExpMap = new HashMap<>();
            if (Config.getConfigurationSection("AmonPack.Forest." + key + ".Exp") != null) {
                for (String ForestItem : Config.getConfigurationSection("AmonPack.Forest." + key + ".Exp")
                        .getKeys(false)) {
                    double expValue = Config.getDouble("AmonPack.Forest." + key + ".Exp." + ForestItem);
                    switch (ForestItem.toUpperCase()) {
                        case "LEAVES" -> {
                            List<Material> leaves = List.of(
                                    Material.OAK_LEAVES,
                                    Material.SPRUCE_LEAVES,
                                    Material.BIRCH_LEAVES,
                                    Material.JUNGLE_LEAVES,
                                    Material.ACACIA_LEAVES,
                                    Material.DARK_OAK_LEAVES,
                                    Material.MANGROVE_LEAVES,
                                    Material.CHERRY_LEAVES,
                                    Material.AZALEA_LEAVES,
                                    Material.FLOWERING_AZALEA_LEAVES);
                            leaves.forEach(mat -> ExpMap.put(mat, expValue));
                        }
                        case "LOG" -> {
                            List<Material> logs = List.of(
                                    Material.OAK_LOG,
                                    Material.SPRUCE_LOG,
                                    Material.BIRCH_LOG,
                                    Material.JUNGLE_LOG,
                                    Material.ACACIA_LOG,
                                    Material.DARK_OAK_LOG,
                                    Material.MANGROVE_LOG,
                                    Material.CHERRY_LOG,
                                    Material.BAMBOO_BLOCK);
                            logs.forEach(mat -> ExpMap.put(mat, expValue));
                        }
                        case "WOOD" -> {
                            List<Material> woods = List.of(
                                    Material.OAK_WOOD,
                                    Material.SPRUCE_WOOD,
                                    Material.BIRCH_WOOD,
                                    Material.JUNGLE_WOOD,
                                    Material.ACACIA_WOOD,
                                    Material.DARK_OAK_WOOD,
                                    Material.MANGROVE_WOOD,
                                    Material.CHERRY_WOOD);
                            woods.forEach(mat -> ExpMap.put(mat, expValue));
                        }
                        case "STRIPPED_WOOD" -> {
                            List<Material> strippedWoods = List.of(
                                    Material.STRIPPED_OAK_WOOD,
                                    Material.STRIPPED_SPRUCE_WOOD,
                                    Material.STRIPPED_BIRCH_WOOD,
                                    Material.STRIPPED_JUNGLE_WOOD,
                                    Material.STRIPPED_ACACIA_WOOD,
                                    Material.STRIPPED_DARK_OAK_WOOD,
                                    Material.STRIPPED_MANGROVE_WOOD,
                                    Material.STRIPPED_CHERRY_WOOD);
                            strippedWoods.forEach(mat -> ExpMap.put(mat, expValue));
                        }
                        case "STRIPPED_LOG" -> {
                            List<Material> strippedLogs = List.of(
                                    Material.STRIPPED_OAK_LOG,
                                    Material.STRIPPED_SPRUCE_LOG,
                                    Material.STRIPPED_BIRCH_LOG,
                                    Material.STRIPPED_JUNGLE_LOG,
                                    Material.STRIPPED_ACACIA_LOG,
                                    Material.STRIPPED_DARK_OAK_LOG,
                                    Material.STRIPPED_MANGROVE_LOG,
                                    Material.STRIPPED_CHERRY_LOG,
                                    Material.STRIPPED_BAMBOO_BLOCK);
                            strippedLogs.forEach(mat -> ExpMap.put(mat, expValue));
                        }
                    }
                }
                for (String ForestItem : Config.getConfigurationSection("AmonPack.Forest." + key + ".Exp")
                        .getKeys(false)) {
                    Material mat = Material.getMaterial(ForestItem.toUpperCase());
                    if (mat != null) {
                        double expValue = Config.getDouble("AmonPack.Forest." + key + ".Exp." + ForestItem);
                        ExpMap.put(mat, expValue);
                    }
                }
            }
            Location loc = new Location(Bukkit.getWorld(World), 0, 0, 0);
            Forest forest = new Forest(ExpMap, loc);
            ForestWorlds.add(forest);
        }
        for (String key : Config.getStringList("AmonPack.LumberingBlocks")) {
            ForestBlocks.add(Material.getMaterial(key));
        }
    }

    public static boolean PlayerBreakBlock(Player player, Block block) {
        return BreakBlockInternal(player, block,false);
    }

    private static boolean BreakBlockInternal(Player player, Block block, boolean Naturally) {
        if (isNaturalBlock(block)) {
            for (Forest forest : ForestWorlds) {
                if (block.getWorld().equals(forest.getLoc().getWorld())) {
                    if (ForestBlocks.contains(block.getType())) {
                        List<ItemStack> Drops = new ArrayList<>();
                        int SkillPoints = (int) forest.GetExpByMaterial(block.getType());
                        if(Naturally){
                            block.breakNaturally();
                        }else {
                            Drops.addAll(block.getDrops());
                            block.setType(Material.AIR);
                        }


                        double modifier = 1;
                        int extraLootChance = 0;

                        List<ItemStack> equipment = new ArrayList<>();
                        equipment.add(player.getInventory().getItemInMainHand());
                        for (ItemStack armor : player.getInventory().getArmorContents()) {
                            if (armor != null)
                                equipment.add(armor);
                        }

                        for (ItemStack item : equipment) {
                            if (CraftingMenager.HaveEffect(item, "Exp_Boost")
                                    || CraftingMenager.HaveEffect(item, "Experience")) {
                                modifier += 0.2;
                            }
                            if (CraftingMenager.HaveEffect(item, "Mining_Loot_Boost")) {
                                extraLootChance += 10;
                            }
                        }
                        if (extraLootChance > 0 && getRandom(0, 100) < extraLootChance) {
                            for (ItemStack item : Drops) {
                                player.getInventory().addItem(item);
                            }
                            AmonPackPlugin.getPlayerMenager().AddPoints(LevelSkill.SkillType.LUMBERING, player,
                                    SkillPoints);
                        }

                        for (ItemStack item : Drops) {
                            player.getInventory().addItem(item);
                        }

                        AmonPackPlugin.getPlayerMenager().AddPoints(LevelSkill.SkillType.LUMBERING, player,
                                (int) (SkillPoints * modifier));
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static void ChopTree(Player player, Block startBlock, int maxBlocks) {
        Set<Block> visited = new HashSet<>();
        Queue<Block> queue = new LinkedList<>();
        queue.add(startBlock);
        visited.add(startBlock);

        int chopped = 0;
        Material logType = startBlock.getType();

        while (!queue.isEmpty() && chopped < maxBlocks) {
            Block current = queue.poll();

            if (current.getType() == logType || current.getType().name().contains("LEAVES")) {
                if (BreakBlockInternal(player, current,true)) {
                    chopped++;
                } else {
                }
            } else {
                continue;
            }

            // Add neighbors
            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        if (x == 0 && y == 0 && z == 0)
                            continue;

                        Block relative = current.getRelative(x, y, z);
                        if (!visited.contains(relative)
                                && (relative.getType() == logType || relative.getType().name().contains("LEAVES"))) {
                            visited.add(relative);
                            queue.add(relative);
                        }
                    }
                }
            }
        }
    }

}
package RPG.Gathering;

import RPG.Gathering.Objects.Farm;
import RPG.Gathering.Objects.Forest;
import RPG.Gathering.Objects.Mine;
import RPG.Util.InventoryXHolder;
import RPG.Levels.Objects.LevelSkill;
import RPG.Crafting.CraftingMenager;
import RPG.Levels.Objects.PlayerLevel;
import RPG.Util.Resource;
import Plugin.Commands;
import Plugin.AmonPackPlugin;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import RPG.Progression.ProgressionManager;
import RPG.Progression.model.ObjectiveType;

import java.util.*;

import static RPG.Gathering.MiningMenager.isNaturalBlock;
import static Plugin.Methods.getRandom;

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

    public static boolean isAxe(ItemStack item) {
        if (item == null) return false;
        String name = item.getType().name();
        return name.endsWith("_AXE");
    }

    public static boolean isLog(Material mat) {
        if (mat == null) return false;
        String name = mat.name();
        return name.endsWith("_LOG") || name.endsWith("_WOOD") || name.endsWith("_STEM") || name.endsWith("_HYPHAE");
    }

    public static boolean isLeaves(Material mat) {
        if (mat == null) return false;
        String name = mat.name();
        return name.endsWith("_LEAVES") || name.endsWith("_WART_BLOCK") || mat == Material.SHROOMLIGHT || mat == Material.MANGROVE_ROOTS;
    }

    public static boolean isBuildingBlock(Material mat) {
        if (mat == null || mat.isAir()) return false;
        String name = mat.name();
        return name.endsWith("_PLANKS") || name.endsWith("_STAIRS") || name.endsWith("_SLAB") ||
                name.endsWith("_DOOR") || name.endsWith("_TRAPDOOR") || name.endsWith("_FENCE") ||
                name.endsWith("_FENCE_GATE") || name.endsWith("_GLASS") || name.endsWith("_GLASS_PANE") ||
                name.contains("STONE") || name.contains("BRICK") || name.contains("CONCRETE") ||
                name.contains("TERRACOTTA") || name.contains("WOOL") || name.contains("CARPET") ||
                mat == Material.CHEST || mat == Material.TRAPPED_CHEST || mat == Material.BARREL ||
                mat == Material.FURNACE || mat == Material.BLAST_FURNACE || mat == Material.SMOKER ||
                mat == Material.CRAFTING_TABLE || mat == Material.ANVIL || mat == Material.CHIPPED_ANVIL ||
                mat == Material.DAMAGED_ANVIL || mat == Material.ENCHANTING_TABLE || mat == Material.BOOKSHELF ||
                name.endsWith("_BED");
    }

    public static boolean isNaturalTree(Block startBlock) {
        if (startBlock == null || !isLog(startBlock.getType())) return false;
        if (!isNaturalBlock(startBlock)) return false;

        Set<Block> logs = new HashSet<>();
        Set<Block> leaves = new HashSet<>();
        Queue<Block> queue = new LinkedList<>();

        queue.add(startBlock);
        logs.add(startBlock);

        int maxLogs = 150;
        int highestY = startBlock.getY();

        while (!queue.isEmpty() && logs.size() <= maxLogs) {
            Block current = queue.poll();
            highestY = Math.max(highestY, current.getY());

            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        Block neighbor = current.getRelative(dx, dy, dz);
                        Material nType = neighbor.getType();

                        if (isBuildingBlock(nType)) {
                            return false; // Adjacent to player structure
                        }

                        if (isLog(nType) && !logs.contains(neighbor)) {
                            if (!isNaturalBlock(neighbor)) {
                                return false; // Contains player placed logs
                            }
                            logs.add(neighbor);
                            queue.add(neighbor);
                        } else if (isLeaves(nType)) {
                            leaves.add(neighbor);
                        }
                    }
                }
            }
        }

        // Tree must have leaves canopy near top
        if (leaves.size() < 3) {
            return false;
        }

        // Also check if leaves are positioned near the top of the trunk
        boolean hasTopLeaves = false;
        for (Block leaf : leaves) {
            if (leaf.getY() >= highestY - 2) {
                hasTopLeaves = true;
                break;
            }
        }

        return hasTopLeaves;
    }

    public static void onStartChopping(Player player, Block block) {
        if (player == null || block == null) return;
        if (isNaturalTree(block)) {
            // Apply slight mining fatigue to simulate heavier effort of chopping entire tree
            player.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 45, 0, false, false, false));
        }
    }

    public static boolean tryChopTreeAnimated(Player player, Block startBlock, ItemStack axe) {
        if (player == null || startBlock == null) return false;
        if (!isNaturalTree(startBlock)) return false;

        // Remove mining fatigue immediately on break
        player.removePotionEffect(PotionEffectType.MINING_FATIGUE);

        // Collect all tree blocks
        Set<Block> logs = new HashSet<>();
        Set<Block> leaves = new HashSet<>();
        Queue<Block> queue = new LinkedList<>();

        queue.add(startBlock);
        logs.add(startBlock);

        while (!queue.isEmpty() && logs.size() <= 200) {
            Block current = queue.poll();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        Block neighbor = current.getRelative(dx, dy, dz);
                        Material nType = neighbor.getType();

                        if (isLog(nType) && !logs.contains(neighbor)) {
                            logs.add(neighbor);
                            queue.add(neighbor);
                        } else if (isLeaves(nType)) {
                            leaves.add(neighbor);
                        }
                    }
                }
            }
        }

        List<Block> sortedLogs = new ArrayList<>(logs);
        sortedLogs.sort(Comparator.comparingInt(Block::getY));

        List<Block> sortedLeaves = new ArrayList<>(leaves);
        sortedLeaves.sort(Comparator.comparingInt(Block::getY));

        int totalLogs = sortedLogs.size();

        new BukkitRunnable() {
            int logIndex = 0;
            int leafIndex = 0;

            @Override
            public void run() {
                if (logIndex < sortedLogs.size()) {
                    int batch = Math.min(3, sortedLogs.size() - logIndex);
                    for (int b = 0; b < batch; b++) {
                        Block logBlock = sortedLogs.get(logIndex++);
                        if (isLog(logBlock.getType())) {
                            logBlock.getWorld().spawnParticle(Particle.BLOCK,
                                    logBlock.getLocation().add(0.5, 0.5, 0.5), 10, 0.25, 0.25, 0.25, logBlock.getBlockData());
                            logBlock.getWorld().playSound(logBlock.getLocation(), Sound.BLOCK_WOOD_BREAK, 0.8f, 1.0f);

                            BreakBlockInternal(player, logBlock, true);

                            // Quest progression update
                            if (ProgressionManager.getInstance() != null && ProgressionManager.getInstance().getProgressionService() != null) {
                                ProgressionManager.getInstance().getProgressionService().handleObjective(player, ObjectiveType.DESTROY_BLOCK, "WOOD", 1);
                                ProgressionManager.getInstance().getProgressionService().handleObjective(player, ObjectiveType.COLLECT_ITEM, "WOOD", 1);
                            }
                        }
                    }
                } else if (leafIndex < sortedLeaves.size()) {
                    int batch = Math.min(8, sortedLeaves.size() - leafIndex);
                    for (int b = 0; b < batch; b++) {
                        Block leafBlock = sortedLeaves.get(leafIndex++);
                        if (isLeaves(leafBlock.getType())) {
                            leafBlock.getWorld().spawnParticle(Particle.BLOCK,
                                    leafBlock.getLocation().add(0.5, 0.5, 0.5), 6, 0.2, 0.2, 0.2, leafBlock.getBlockData());
                            leafBlock.breakNaturally();
                        }
                    }
                } else {
                    // Finished
                    startBlock.getWorld().playSound(startBlock.getLocation(), Sound.BLOCK_GRASS_BREAK, 1.0f, 0.8f);
                    cancel();
                }
            }
        }.runTaskTimer(AmonPackPlugin.plugin, 0L, 1L);

        // Damage axe
        if (axe != null && axe.getItemMeta() instanceof org.bukkit.inventory.meta.Damageable dmgMeta) {
            int damageToApply = Math.max(1, totalLogs / 2);
            dmgMeta.setDamage(dmgMeta.getDamage() + damageToApply);
            axe.setItemMeta(dmgMeta);
        }

        return true;
    }

    public static boolean PlayerBreakBlock(Player player, Block block) {
        return BreakBlockInternal(player, block, false);
    }

    private static boolean BreakBlockInternal(Player player, Block block, boolean Naturally) {
        if (isNaturalBlock(block)) {
            for (Forest forest : ForestWorlds) {
                if (block.getWorld().equals(forest.getLoc().getWorld())) {
                    if (ForestBlocks.contains(block.getType())) {
                        List<ItemStack> Drops = new ArrayList<>();
                        int SkillPoints = (int) forest.GetExpByMaterial(block.getType());
                        if (Naturally) {
                            block.breakNaturally();
                        } else {
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
        tryChopTreeAnimated(player, startBlock, player.getInventory().getItemInMainHand());
    }
}
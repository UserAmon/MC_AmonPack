package RPG.Gathering;

import RPG.Crafting.CraftingMenager;
import RPG.Crafting.Objects.MagicEffects;
import RPG.Levels.Objects.LevelSkill;
import RPG.Gathering.Objects.Mine;
import com.projectkorra.projectkorra.util.ParticleEffect;

import CustomContent.Hooks.ItemsAdderHook;
import Plugin.AmonPackPlugin;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import RPG.Progression.ProgressionManager;
import RPG.Progression.model.ObjectiveType;

import java.util.*;

import static RPG.Gathering.FarmMenager.verticalPlants;
import static Plugin.Methods.getRandom;
import static org.bukkit.Material.matchMaterial;

public class MiningMenager {
    public static boolean MiningEnabled = true;
    public static List<String> AllowedWorlds = new ArrayList<>();
    public static List<Mine> MiningWorlds = new ArrayList<>();
    static List<Material> MiningOresDrops = new ArrayList<>();
    private static final Map<String, String> ItemsAdderMining = new HashMap<>();
    private static final Map<Location, Long> placedBlocks = new HashMap<>();

    public MiningMenager() {
        ReloadConfig();
        startCleanupTask();
    }

    public void ReloadConfig() {
        MiningWorlds = new ArrayList<>();
        MiningOresDrops.clear();
        placedBlocks.clear();
        AllowedWorlds = new ArrayList<>();
        FileConfiguration config = AmonPackPlugin.getConfigs_menager().getMining_Config();

        MiningEnabled = config.getBoolean("AmonPack.Mining.Enabled", true);
        AllowedWorlds = config.getStringList("AmonPack.Mining.Worlds");

        if (config.getConfigurationSection("AmonPack.Mining") != null) {
            for (String key : Objects.requireNonNull(config.getConfigurationSection("AmonPack.Mining")).getKeys(false)) {
                if (key.equalsIgnoreCase("Enabled") || key.equalsIgnoreCase("Worlds")) continue;
                String World = config.getString("AmonPack.Mining." + key + ".World");
                if (World == null) continue;
                HashMap<String, Integer> LChance = new HashMap<>();
                HashMap<Material, Double> ExpMap = new HashMap<>();
                HashMap<String, Double> IAExpMap = new HashMap<>();

                if (config.getConfigurationSection("AmonPack.Mining." + key + ".Loot") != null) {
                    for (String LootName : config.getConfigurationSection("AmonPack.Mining." + key + ".Loot")
                            .getKeys(false)) {
                        LChance.put(LootName, config.getInt("AmonPack.Mining." + key + ".Loot." + LootName));
                    }
                }

                if (config.getConfigurationSection("AmonPack.Mining." + key + ".Exp") != null) {
                    for (String OresName : config.getConfigurationSection("AmonPack.Mining." + key + ".Exp")
                            .getKeys(false)) {
                        ExpMap.put(Material.getMaterial(OresName),
                                config.getDouble("AmonPack.Mining." + key + ".Exp." + OresName));
                        if (OresName.endsWith("_ORE")) {
                            ExpMap.put(Material.getMaterial("DEEPSLATE_" + OresName),
                                    config.getDouble("AmonPack.Mining." + key + ".Exp." + OresName));
                        }
                    }
                }

                if (config.getConfigurationSection("AmonPack.Mining." + key + ".ItemsAdderExp") != null) {
                    for (String iaName : config.getConfigurationSection("AmonPack.Mining." + key + ".ItemsAdderExp")
                            .getKeys(false)) {
                        IAExpMap.put(iaName, config.getDouble("AmonPack.Mining." + key + ".ItemsAdderExp." + iaName));
                    }
                }

                org.bukkit.World bWorld = Bukkit.getWorld(World);
                if (bWorld != null) {
                    Location loc = new Location(bWorld, 0, 0, 0);
                    Mine mine = new Mine(loc, ExpMap, LChance, IAExpMap);
                    MiningWorlds.add(mine);
                }
            }
        }

        ItemsAdderMining.clear();
        if (config.getConfigurationSection("AmonPack.ItemsAdderMining") != null) {
            for (String blockId : config.getConfigurationSection("AmonPack.ItemsAdderMining").getKeys(false)) {
                String dropId = config.getString("AmonPack.ItemsAdderMining." + blockId);
                ItemsAdderMining.put(blockId, dropId);
            }
        }
        for (String key : config.getStringList("AmonPack.MiningBlocks")) {
            MiningOresDrops.add(Material.getMaterial(key));
            if (key.endsWith("_ORE")) {
                MiningOresDrops.add(Material.getMaterial("DEEPSLATE_" + key));
            }
        }
    }

    public static void PlayerPlaceBlock(Player player, Block block) {
        if (isNaturalBlock(block) && !block.isLiquid() && block.getType().isSolid()) {
            markBlockPlaced(block, 6000L);
            AmonPackPlugin.getPlayerMenager().AddPoints(LevelSkill.SkillType.BUILDING, player, 1);
        } else if (isNaturalBlock(block) && verticalPlants.contains(block.getType())) {
            markBlockPlaced(block, 2400);
        }
    }

    private static int IsMinable(Block block) {
        if (ItemsAdderHook.isAvailable()) {
            String iaId = ItemsAdderHook.getCustomBlockNamespacedId(block);
            if (iaId != null && ItemsAdderMining.containsKey(iaId)) {
                return 2;
            }
        }
        if (AmonPackPlugin.customBlockManager != null && AmonPackPlugin.customBlockManager.isCustomBlock(block)) {
            return 3;
        }
        if (MiningOresDrops.contains(block.getType())) {
            return 1;
        } else {
            return 0;
        }
    }

    public static boolean PlayerBreakBlock(Player player, Block block, int exp) {
        if (!MiningEnabled) return false;
        if (AllowedWorlds != null && !AllowedWorlds.isEmpty() && !AllowedWorlds.contains(block.getWorld().getName())) {
            return false;
        }
        return BreakBlockInternal(player, block, exp, true);
    }

    private static boolean BreakBlockInternal(Player player, Block block, int exp, boolean triggerAbilities) {
        if (!MiningEnabled) return false;
        if (AllowedWorlds != null && !AllowedWorlds.isEmpty() && !AllowedWorlds.contains(block.getWorld().getName())) {
            return false;
        }
        if (isNaturalBlock(block)) {
            for (Mine m : MiningWorlds) {
                if (block.getWorld().equals(m.getLoc().getWorld())) {
                    int Result = IsMinable(block);
                    if (Result > 0) {
                        List<ItemStack> Drops = new ArrayList<>();
                        int SkillPoints = 0;
                        boolean IsOre = false;
                        Material type = block.getType();
                        if (!MiningOresDrops.contains(block.getType())) {
                            IsOre=true;
                        }

                        if (Result == 1) {
                            Drops.addAll(block.getDrops());
                            SkillPoints = (int) m.GetExpByMaterial(block.getType());
                            block.setType(Material.AIR);
                        } else if (Result == 3) {
                            CustomContent.Blocks.CustomBlock cb = AmonPackPlugin.customBlockManager.removeBlock(block);
                            if (cb != null) {
                                SkillPoints = (int) cb.getExp();
                            }
                            block.setType(Material.AIR);
                        } else if (Result == 2) {
                            String iaId = ItemsAdderHook.getCustomBlockNamespacedId(block);
                            if (iaId != null) {
                                double iaExp = m.GetExpByIA(iaId);
                                exp = (int) (iaExp + 1);
                                SkillPoints = (int) iaExp;
                                String dropId = ItemsAdderMining.get(iaId);
                                ItemStack iaDrop = ItemsAdderHook.getItem(dropId);
                                if (iaDrop != null) {
                                    Drops.add(iaDrop);
                                }
                                ItemsAdderHook.removeCustomBlock(block);
                            }
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
                            if (CraftingMenager.HaveEffect(item, "Vein_Miner")&&IsOre) {
                                VeinMiner(player, block, type, 10);
                            }

                        }

                        for (Map.Entry<String, Integer> entry : m.getLootList().entrySet()) {
                            String lootItem = entry.getKey();
                            int chance = entry.getValue();
                            if (getRandom(1, 100) <= (chance + (extraLootChance / 2))) {
                                if (lootItem.startsWith("amonpack:")) {
                                    ItemStack cs = ItemsAdderHook.getItem(lootItem);
                                    if (cs == null) {
                                        cs = ItemsAdderHook.getItem(lootItem.substring(9));
                                    }
                                    if (cs != null) {
                                        player.getInventory().addItem(cs);
                                        if (ProgressionManager.getInstance() != null && ProgressionManager.getInstance().getProgressionService() != null) {
                                            ProgressionManager.getInstance().getProgressionService().handleObjective(player, ObjectiveType.COLLECT_ITEM, cs.getType().name(), cs.getAmount());
                                            ProgressionManager.getInstance().getProgressionService().handleObjective(player, ObjectiveType.COLLECT_ITEM, lootItem, cs.getAmount());
                                        }
                                    }
                                } else {
                                    Material mat = Material.getMaterial(lootItem);
                                    if (mat != null) {
                                        ItemStack lootStack = new ItemStack(mat);
                                        player.getInventory().addItem(lootStack);
                                        if (ProgressionManager.getInstance() != null && ProgressionManager.getInstance().getProgressionService() != null) {
                                            ProgressionManager.getInstance().getProgressionService().handleObjective(player, ObjectiveType.COLLECT_ITEM, mat.name(), 1);
                                        }
                                    }
                                }
                            }
                        }
                        for (ItemStack item : Drops) {
                            player.getInventory().addItem(item);
                        }

                        // Notify Progression Service of block destruction, depth, and collected items
                        if (ProgressionManager.getInstance() != null && ProgressionManager.getInstance().getProgressionService() != null) {
                            var ps = ProgressionManager.getInstance().getProgressionService();
                            ps.handleObjective(player, ObjectiveType.DESTROY_BLOCK, type.name(), 1);
                            if (type == Material.COBBLESTONE || type == Material.STONE || type == Material.DEEPSLATE) {
                                ps.handleObjective(player, ObjectiveType.DESTROY_BLOCK, "COBBLESTONE", 1);
                                ps.handleObjective(player, ObjectiveType.DESTROY_BLOCK, "STONE", 1);
                            }
                            if (type.name().endsWith("_ORE")) {
                                ps.handleObjective(player, ObjectiveType.DESTROY_BLOCK, "ORES", 1);
                                ps.handleObjective(player, ObjectiveType.DESTROY_BLOCK, "ORE", 1);
                            }
                            ps.handleObjective(player, ObjectiveType.MINE_TO_DEPTH, String.valueOf(block.getY()), 1);

                            for (ItemStack item : Drops) {
                                if (item != null && item.getType() != Material.AIR) {
                                    ps.handleObjective(player, ObjectiveType.COLLECT_ITEM, item.getType().name(), item.getAmount());
                                    if (item.getType() == Material.COAL || item.getType() == Material.CHARCOAL) {
                                        ps.handleObjective(player, ObjectiveType.COLLECT_ITEM, "COAL", item.getAmount());
                                    }
                                    if (item.getType() == Material.RAW_IRON || item.getType() == Material.IRON_INGOT) {
                                        ps.handleObjective(player, ObjectiveType.COLLECT_ITEM, "IRON_ORE", item.getAmount());
                                    }
                                    if (item.getType() == Material.RAW_COPPER || item.getType() == Material.COPPER_INGOT) {
                                        ps.handleObjective(player, ObjectiveType.COLLECT_ITEM, "COPPER_ORE", item.getAmount());
                                    }
                                    if (item.getType() == Material.RAW_GOLD || item.getType() == Material.GOLD_INGOT) {
                                        ps.handleObjective(player, ObjectiveType.COLLECT_ITEM, "GOLD_ORE", item.getAmount());
                                    }
                                    if (item.getType() == Material.DIAMOND) {
                                        ps.handleObjective(player, ObjectiveType.COLLECT_ITEM, "DIAMOND", item.getAmount());
                                    }
                                    if (item.getType() == Material.COBBLESTONE || item.getType() == Material.STONE) {
                                        ps.handleObjective(player, ObjectiveType.COLLECT_ITEM, "COBBLESTONE", item.getAmount());
                                        ps.handleObjective(player, ObjectiveType.COLLECT_ITEM, "STONE", item.getAmount());
                                    }
                                }
                            }
                        }

                        AmonPackPlugin.getPlayerMenager().AddPoints(LevelSkill.SkillType.MINING, player,
                                SkillPoints);
                        if (extraLootChance > 0 && getRandom(0, 100) < extraLootChance) {
                            System.out.println("Dodatkowy loot - "+extraLootChance);
                            for (ItemStack item : Drops) {
                                player.getInventory().addItem(item);
                            }
                            AmonPackPlugin.getPlayerMenager().AddPoints(LevelSkill.SkillType.MINING, player,
                                    SkillPoints);
                        }
                        if (exp < 1 && getRandom(0, 10) > 6)
                            exp += 1;
                        player.giveExp((int) ((exp) * modifier));
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static void VeinMiner(Player player, Block startBlock, Material type, int maxBlocks) {
        Set<Block> visited = new HashSet<>();
        Queue<Block> queue = new LinkedList<>();
        queue.add(startBlock);
        visited.add(startBlock);

        int mined = 0;

        while (!queue.isEmpty() && mined < maxBlocks) {
            Block current = queue.poll();

            if (current.getType() == type && isNaturalBlock(current)) {
                BreakBlockInternal(player, current, 0, false);
                mined++;
            } else {
                continue;
            }
            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        if (x == 0 && y == 0 && z == 0)
                            continue;

                        Block relative = current.getRelative(x, y, z);
                        if (!visited.contains(relative) && relative.getType() == type && isNaturalBlock(relative)) {
                            visited.add(relative);
                            queue.add(relative);
                        }
                    }
                }
            }
        }
    }

    public static void MineArea(Player player, Block center) {
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    Block b = center.getRelative(x, y, z);
                    if (b.getType() == Material.BEDROCK)
                        continue;
                    if (isNaturalBlock(b)) {
                        if (IsMinable(b) > 0) {
                            PlayerBreakBlock(player, b, 0);
                        } else {
                            b.breakNaturally(player.getInventory().getItemInMainHand());
                        }
                    }
                }
            }
        }
    }

    private void startCleanupTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                placedBlocks.entrySet().removeIf(entry -> entry.getValue() <= now);
            }
        }.runTaskTimer(AmonPackPlugin.plugin, 1200, 1200);
    }

    static boolean isNaturalBlock(Block block) {
        return !placedBlocks.containsKey(block.getLocation());
    }

    private static void markBlockPlaced(Block block, long time) {
        placedBlocks.put(block.getLocation(), System.currentTimeMillis() + time);
    }
}

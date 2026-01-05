package AvatarSystems.Gathering;

import AvatarSystems.Crafting.CraftingMenager;
import AvatarSystems.Util_Objects.LevelSkill;
import AvatarSystems.Gathering.Objects.Mine;
import com.projectkorra.projectkorra.util.ParticleEffect;

import dev.lone.itemsadder.api.CustomBlock;
import dev.lone.itemsadder.api.CustomStack;
import dev.lone.itemsadder.api.ItemsAdder;
import methods_plugins.AmonPackPlugin;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

import static AvatarSystems.Gathering.FarmMenager.verticalPlants;
import static methods_plugins.Methods.getRandom;
import static org.bukkit.Material.matchMaterial;

public class MiningMenager {
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
        FileConfiguration config = AmonPackPlugin.getConfigs_menager().getMining_Config();
        for (String key : Objects.requireNonNull(config.getConfigurationSection("AmonPack.Mining")).getKeys(false)) {
            String World = config.getString("AmonPack.Mining." + key + ".World");
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

            Location loc = new Location(Bukkit.getWorld(World), 0, 0, 0);
            Mine mine = new Mine(loc, ExpMap, LChance, IAExpMap);
            MiningWorlds.add(mine);
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
        CustomBlock customBlock = CustomBlock.byAlreadyPlaced(block);
        if (customBlock != null && ItemsAdderMining.containsKey(customBlock.getNamespacedID())) {
            return 2;
        } else if (MiningOresDrops.contains(block.getType())) {
            return 1;
        } else {
            return 0;
        }
    }

    public static boolean PlayerBreakBlock(Player player, Block block, int exp) {
        if (isNaturalBlock(block)) {
            for (Mine m : MiningWorlds) {
                if (block.getWorld().equals(m.getLoc().getWorld())) {
                    int Result = IsMinable(block);
                    if (Result > 0) {
                        CustomBlock customBlock = CustomBlock.byAlreadyPlaced(block);
                        if (Result == 1) {
                            for (ItemStack item : block.getDrops()) {
                                player.getInventory().addItem(item);
                            }
                            AmonPackPlugin.getPlayerMenager().AddPoints(LevelSkill.SkillType.MINING, player,
                                    (int) m.GetExpByMaterial(block.getType()));
                            block.setType(Material.AIR);
                        } else {
                            double iaExp = m.GetExpByIA(customBlock.getNamespacedID());
                            AmonPackPlugin.getPlayerMenager().AddPoints(LevelSkill.SkillType.MINING, player,
                                    (int) iaExp);
                            exp = (int) (iaExp + 1);
                            String dropId = ItemsAdderMining.get(customBlock.getNamespacedID());
                            CustomStack dropStack = CustomStack.getInstance(dropId);
                            if (dropStack != null) {
                                player.getInventory().addItem(dropStack.getItemStack());
                            }
                            customBlock.playBreakParticles();
                            customBlock.playBreakEffect();
                            customBlock.playBreakSound();
                            customBlock.remove();
                        }

                        for (Map.Entry<String, Integer> entry : m.getLootList().entrySet()) {
                            String lootItem = entry.getKey();
                            int chance = entry.getValue();
                            if (getRandom(1, 100) <= chance) {
                                if (lootItem.startsWith("amonpack:")) {
                                    String iaName = lootItem.substring(9);
                                    CustomStack cs = CustomStack.getInstance(iaName);
                                    if (cs != null) {
                                        player.getInventory().addItem(cs.getItemStack());
                                    }
                                } else {
                                    Material mat = Material.getMaterial(lootItem);
                                    if (mat != null) {
                                        player.getInventory().addItem(new ItemStack(mat));
                                    }
                                }
                            }
                        }

                        double modifier = 1;
                        int extraLootChance = 0;

                        // Check Armor & MainHand for Effects
                        List<ItemStack> equipment = new ArrayList<>();
                        equipment.add(player.getInventory().getItemInMainHand());
                        for (ItemStack armor : player.getInventory().getArmorContents()) {
                            if (armor != null)
                                equipment.add(armor);
                        }

                        for (ItemStack item : equipment) {
                            if (CraftingMenager.HaveEffect(item, "Exp_Boost")) {
                                modifier += 0.5; // 50% more XP per item
                            }
                            if (CraftingMenager.HaveEffect(item, "Mining_Loot_Boost")) {
                                extraLootChance += 10; // 10% extra loot chance per item
                            }
                        }

                        if (CraftingMenager.HaveEffect(player.getItemInUse(), "Expierience")) {
                            modifier += 1;
                        }
                        if (CraftingMenager.HaveEffect(player.getItemInUse(), "Looting")) {
                            extraLootChance += 30; // Original Looting effect
                        }

                        if (extraLootChance > 0 && getRandom(0, 100) < extraLootChance) {
                            if (customBlock != null) {
                                for (Object item : customBlock.getLoot()) {
                                    player.getInventory().addItem((ItemStack) item);
                                }
                                double iaExp = m.GetExpByIA(customBlock.getNamespacedID());
                                AmonPackPlugin.getPlayerMenager().AddPoints(LevelSkill.SkillType.MINING, player,
                                        (int) iaExp);
                            } else {
                                for (ItemStack item : block.getDrops()) {
                                    player.getInventory().addItem(item);
                                }
                                AmonPackPlugin.getPlayerMenager().AddPoints(LevelSkill.SkillType.MINING, player,
                                        (int) m.GetExpByMaterial(block.getType()));
                            }
                            modifier += 1; // Bonus XP for bonus loot
                        }
                        player.giveExp((int) ((1 + exp) * modifier));
                        return true;
                    }
                }
            }
        }
        return false;
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

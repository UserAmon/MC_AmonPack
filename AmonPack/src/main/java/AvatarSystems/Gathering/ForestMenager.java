package AvatarSystems.Gathering;

import AvatarSystems.Gathering.Objects.Farm;
import AvatarSystems.Gathering.Objects.Forest;
import AvatarSystems.Gathering.Objects.Mine;
import AvatarSystems.Util_Objects.InventoryXHolder;
import AvatarSystems.Util_Objects.LevelSkill;
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

    public void ReloadConfig(){
        ForestWorlds=new ArrayList<>();
        FileConfiguration Config = AmonPackPlugin.getForestConfig();
        for(String key : Objects.requireNonNull(Config.getConfigurationSection("AmonPack.Forest")).getKeys(false)) {
            String World = Config.getString("AmonPack.Forest." + key + ".World");
            HashMap<Material, Double> ExpMap = new HashMap<>();
            if (Config.getConfigurationSection("AmonPack.Forest."+key+".Exp") != null){
                for (String ForestItem : Config.getConfigurationSection("AmonPack.Forest." + key + ".Exp").getKeys(false)) {
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
                                    Material.FLOWERING_AZALEA_LEAVES
                            );
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
                                    Material.BAMBOO_BLOCK
                            );
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
                                    Material.CHERRY_WOOD
                            );
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
                                    Material.STRIPPED_CHERRY_WOOD
                            );
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
                                    Material.STRIPPED_BAMBOO_BLOCK
                            );
                            strippedLogs.forEach(mat -> ExpMap.put(mat, expValue));
                        }
                    }
                }
                for (String ForestItem : Config.getConfigurationSection("AmonPack.Forest." + key + ".Exp").getKeys(false)) {
                    Material mat = Material.getMaterial(ForestItem.toUpperCase());
                    if (mat != null) {
                        double expValue = Config.getDouble("AmonPack.Forest." + key + ".Exp." + ForestItem);
                        ExpMap.put(mat, expValue);
                    }
                }
            }
            Location loc = new Location(Bukkit.getWorld(World), 0, 0, 0);
            Forest forest = new Forest(ExpMap,loc);
            ForestWorlds.add(forest);
        }
        for(String key : Config.getStringList("AmonPack.LumberingBlocks")) {
                ForestBlocks.add(Material.getMaterial(key));
        }
    }

    public static boolean PlayerBreakBlock(Player player, Block block){
        if(isNaturalBlock(block)){
            for (Forest forest:ForestWorlds) {
                if (block.getWorld().equals(forest.getLoc().getWorld())) {
                    if(ForestBlocks.contains(block.getType())){
                        for (ItemStack item : block.getDrops()){
                            player.getInventory().addItem(item);
                        }
                        AmonPackPlugin.getPlayerMenager().AddPoints(LevelSkill.SkillType.LUMBERING,player, (int)forest.GetExpByMaterial(block.getType()));
                        block.setType(Material.AIR);
                        return true;
                    }}
            }}
        return false;
    }

}
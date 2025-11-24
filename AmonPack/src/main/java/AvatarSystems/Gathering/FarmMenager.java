package AvatarSystems.Gathering;

import AvatarSystems.Gathering.Objects.Farm;
import AvatarSystems.Gathering.Objects.Mine;
import AvatarSystems.Util_Objects.LevelSkill;
import commands.Commands;
import methods_plugins.AmonPackPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Ageable;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

import static AvatarSystems.Gathering.MiningMenager.isNaturalBlock;
import static methods_plugins.Methods.getRandom;

public class FarmMenager {
    public static List<Farm> FarmWorlds = new ArrayList<>();
    static List<Material> FarmBlocks = new ArrayList<>();
    static Set<Material> verticalPlants = Set.of(
            Material.BAMBOO,
            Material.SUGAR_CANE,
            Material.CACTUS,
            Material.KELP
    );
    static Set<Material> ageableCrops = Set.of(
            Material.WHEAT,
            Material.POTATOES,
            Material.CARROTS,
            Material.BEETROOTS,
            Material.NETHER_WART
    );

    public FarmMenager() {
        ReloadConfig();
    }
    public void ReloadConfig(){
        FarmBlocks=new ArrayList<>();
        FarmWorlds = new ArrayList<>();
        FileConfiguration config = AmonPackPlugin.getConfigs_menager().getMining_Config();
        for(String key : Objects.requireNonNull(config.getConfigurationSection("AmonPack.Farms")).getKeys(false)) {
            String World = config.getString("AmonPack.Farms." + key + ".World");
            HashMap<Material, Double> ExpMap = new HashMap<>();
            if (config.getConfigurationSection("AmonPack.Farms."+key+".Exp") != null){
                for(String FarmItem : config.getConfigurationSection("AmonPack.Farms."+key+".Exp").getKeys(false)) {
                    ExpMap.put(Material.getMaterial(FarmItem),config.getDouble("AmonPack.Farms." + key + ".Exp."+FarmItem));
                }}
            Location loc = new Location(Bukkit.getWorld(World), 0, 0, 0);
            Farm farm = new Farm(loc,ExpMap);
            FarmWorlds.add(farm);
        }
        for(String key : config.getStringList("AmonPack.FarmingBlocks")) {
            FarmBlocks.add(Material.getMaterial(key));
        }
    }
    public static boolean CheckFarmBlock(Block block, Player player, boolean IsRightClick) {
        Material type = block.getType();
        for (Farm farm : FarmWorlds) {
            if (!block.getWorld().equals(farm.getLoc().getWorld())) continue;
            if(IsRightClick){
                if (type == Material.SWEET_BERRY_BUSH && isNaturalBlock(block)) {
                    BlockState state = block.getState();
                    if (state.getBlockData() instanceof Ageable ageable) {
                        if (ageable.getAge() >= 2) {
                            ItemStack berries = new ItemStack(Material.SWEET_BERRIES, getRandom(2, 4));
                            Map<Integer, ItemStack> leftover = player.getInventory().addItem(berries);
                            leftover.values().forEach(i -> block.getWorld().dropItemNaturally(player.getLocation(), i));
                            ageable.setAge(1);
                            block.setBlockData(ageable);
                            AmonPackPlugin.getPlayerMenager().AddPoints(LevelSkill.SkillType.FARMING, player, (int) farm.GetExpByMaterial(type));
                            return true;
                        }
                    }
                }
            }else {
            if (verticalPlants.contains(type) && isNaturalBlock(block)) {
                    Block current = block;
                    List<Block> blocks = new ArrayList<>();
                    while (current.getType() == type) {
                        blocks.add(current);
                        current = current.getRelative(BlockFace.UP);
                    }
                    for (Block b : blocks) {
                        for (ItemStack item : b.getDrops()) {
                            Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
                            leftover.values().forEach(i -> block.getWorld().dropItemNaturally(player.getLocation(), i));
                        }
                        b.setType(Material.AIR);
                    }
                    AmonPackPlugin.getPlayerMenager().AddPoints(LevelSkill.SkillType.FARMING, player, 1 + blocks.size());
                    return true;
            }
            if (ageableCrops.contains(type)) {
                BlockState state = block.getState();
                if (state.getBlockData() instanceof Ageable ageable) {
                    if (ageable.getAge() < ageable.getMaximumAge()) {
                        return false;
                    }}
                for (ItemStack item : block.getDrops()) {
                    Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
                    leftover.values().forEach(i -> block.getWorld().dropItemNaturally(player.getLocation(), i));
                }
                block.setType(Material.AIR);
                AmonPackPlugin.getPlayerMenager().AddPoints(LevelSkill.SkillType.FARMING, player, (int) farm.GetExpByMaterial(type));
                return true;
            }
            if (FarmBlocks.contains(type) && isNaturalBlock(block)) {
                for (ItemStack item : block.getDrops()) {
                    Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
                    leftover.values().forEach(i -> block.getWorld().dropItemNaturally(player.getLocation(), i));
                }
                block.setType(Material.AIR);
                AmonPackPlugin.getPlayerMenager().AddPoints(LevelSkill.SkillType.FARMING, player, (int) farm.GetExpByMaterial(type));
                return true;
            }
        }}

        return false;
    }

}

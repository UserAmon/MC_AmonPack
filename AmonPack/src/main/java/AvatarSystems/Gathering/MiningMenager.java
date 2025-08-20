package AvatarSystems.Gathering;

import AvatarSystems.Util_Objects.LevelSkill;
import AvatarSystems.Gathering.Objects.Mine;
import commands.Commands;
import methods_plugins.AmonPackPlugin;
import org.bukkit.*;
import org.bukkit.block.Block;
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
    private static final Map<Location, Long> placedBlocks = new HashMap<>();


    public MiningMenager() {
        ReloadConfig();
        startCleanupTask();
    }
    public void ReloadConfig(){
        MiningWorlds=new ArrayList<>();
        for(String key : Objects.requireNonNull(AmonPackPlugin.getMinesConfig().getConfigurationSection("AmonPack.Mining")).getKeys(false)) {
            String World = AmonPackPlugin.getMinesConfig().getString("AmonPack.Mining." + key + ".World");
            HashMap<String, Integer> LChance = new HashMap<>();
            HashMap<Material, Double> ExpMap = new HashMap<>();
            if (AmonPackPlugin.getMinesConfig().getConfigurationSection("AmonPack.Mining."+key+".Loot") != null){
                for(String LootName : AmonPackPlugin.getMinesConfig().getConfigurationSection("AmonPack.Mining."+key+".Loot").getKeys(false)) {
                    for (int i = 0; i < AmonPackPlugin.getMinesConfig().getInt("AmonPack.Mining." + key + ".Loot."+LootName); i++) {
                        LChance.put(LootName,AmonPackPlugin.getMinesConfig().getInt("AmonPack.Mining." + key + ".Loot."+LootName));
                    }}}
            if (AmonPackPlugin.getMinesConfig().getConfigurationSection("AmonPack.Mining."+key+".Exp") != null){
                for(String OresName : AmonPackPlugin.getMinesConfig().getConfigurationSection("AmonPack.Mining."+key+".Exp").getKeys(false)) {
                    ExpMap.put(Material.getMaterial(OresName),AmonPackPlugin.getMinesConfig().getDouble("AmonPack.Mining." + key + ".Exp."+OresName));
                    if(OresName.endsWith("_ORE")){
                        ExpMap.put(Material.getMaterial("DEEPSLATE_"+OresName),AmonPackPlugin.getMinesConfig().getDouble("AmonPack.Mining." + key + ".Exp."+OresName));
                    }
                }}
            Location loc = new Location(Bukkit.getWorld(World), 0, 0, 0);
            Mine mine = new Mine(loc,ExpMap,LChance);
            MiningWorlds.add(mine);
        }
        for(String key : AmonPackPlugin.getMinesConfig().getStringList("AmonPack.MiningBlocks")) {
            MiningOresDrops.add(Material.getMaterial(key));
            if(key.endsWith("_ORE")) {
                MiningOresDrops.add(Material.getMaterial("DEEPSLATE_"+key));
            }
        }
    }

    public static void PlayerPlaceBlock(Player player, Block block) {
        if(isNaturalBlock(block) && !block.isLiquid() && block.getType().isSolid()){
            markBlockPlaced(block,6000L);
            AmonPackPlugin.getPlayerMenager().AddPoints(LevelSkill.SkillType.BUILDING,player, 1);
        }else
        if(isNaturalBlock(block) && verticalPlants.contains(block.getType())) {
            markBlockPlaced(block,2400);
        }
        }
    public static boolean PlayerBreakBlock(Player player, Block block){
        if(isNaturalBlock(block)){
        for (Mine m:MiningWorlds) {
            if (block.getWorld().equals(m.getLoc().getWorld())) {
                if(MiningOresDrops.contains(block.getType())){
                    for (ItemStack item : block.getDrops()){
                        player.getInventory().addItem(item);
                    }
                    int DropRandom = getRandom(0, 40);
                    if (DropRandom <= m.getLootList().size()) {
                        String st = getRandomKeyFromHashMap(m.getLootList());
                        player.getInventory().addItem(Commands.QuestItemConfig(st));
                    }
                    AmonPackPlugin.getPlayerMenager().AddPoints(LevelSkill.SkillType.MINING,player, (int)m.GetExpByMaterial(block.getType()));
                    block.setType(Material.AIR);
                    return true;
                }}
        }}
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
    private static <K, V> K getRandomKeyFromHashMap(Map<K, V> map) {
        Set<K> keySet = map.keySet();
        K[] keyArray = (K[]) keySet.toArray(new Object[0]);
        return keyArray[new Random().nextInt(keyArray.length)];
    }
    static boolean isNaturalBlock(Block block) {
        return !placedBlocks.containsKey(block.getLocation());
    }
    private static void markBlockPlaced(Block block, long time) {
        placedBlocks.put(block.getLocation(), System.currentTimeMillis() + time);
    }
}

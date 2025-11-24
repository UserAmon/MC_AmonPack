package AvatarSystems.Gathering;

import AvatarSystems.Crafting.CraftingMenager;
import AvatarSystems.Util_Objects.LevelSkill;
import AvatarSystems.Gathering.Objects.Mine;
import com.projectkorra.projectkorra.util.ParticleEffect;
import commands.Commands;
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
    public void ReloadConfig(){
        MiningWorlds=new ArrayList<>();
        MiningOresDrops.clear();
        placedBlocks.clear();
        FileConfiguration config = AmonPackPlugin.getConfigs_menager().getMining_Config();
        for(String key : Objects.requireNonNull(config.getConfigurationSection("AmonPack.Mining")).getKeys(false)) {
            String World = config.getString("AmonPack.Mining." + key + ".World");
            HashMap<String, Integer> LChance = new HashMap<>();
            HashMap<Material, Double> ExpMap = new HashMap<>();
            HashMap<String, Double> IAExpMap = new HashMap<>();

            if (config.getConfigurationSection("AmonPack.Mining."+key+".Loot") != null){
                for(String LootName : config.getConfigurationSection("AmonPack.Mining."+key+".Loot").getKeys(false)) {
                    for (int i = 0; i < config.getInt("AmonPack.Mining." + key + ".Loot."+LootName); i++) {
                        LChance.put(LootName,config.getInt("AmonPack.Mining." + key + ".Loot."+LootName));
                    }
                }
            }

            if (config.getConfigurationSection("AmonPack.Mining."+key+".Exp") != null){
                for(String OresName : config.getConfigurationSection("AmonPack.Mining."+key+".Exp").getKeys(false)) {
                    ExpMap.put(Material.getMaterial(OresName),config.getDouble("AmonPack.Mining." + key + ".Exp."+OresName));
                    if(OresName.endsWith("_ORE")){
                        ExpMap.put(Material.getMaterial("DEEPSLATE_"+OresName),config.getDouble("AmonPack.Mining." + key + ".Exp."+OresName));
                    }
                }
            }

            if (config.getConfigurationSection("AmonPack.Mining."+key+".ItemsAdderExp") != null){
                for(String iaName : config.getConfigurationSection("AmonPack.Mining."+key+".ItemsAdderExp").getKeys(false)) {
                    IAExpMap.put(iaName, config.getDouble("AmonPack.Mining." + key + ".ItemsAdderExp."+iaName));
                }
            }

            Location loc = new Location(Bukkit.getWorld(World), 0, 0, 0);
            Mine mine = new Mine(loc,ExpMap,LChance,IAExpMap);
            MiningWorlds.add(mine);
        }

        ItemsAdderMining.clear();
        if (config.getConfigurationSection("AmonPack.ItemsAdderMining") != null) {
            for (String blockId : config.getConfigurationSection("AmonPack.ItemsAdderMining").getKeys(false)) {
                String dropId = config.getString("AmonPack.ItemsAdderMining." + blockId);
                ItemsAdderMining.put(blockId, dropId);
            }
        }
        for(String key : config.getStringList("AmonPack.MiningBlocks")) {
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

    private static int IsMinable(Block block){
        CustomBlock customBlock = CustomBlock.byAlreadyPlaced(block);
        if(MiningOresDrops.contains(block.getType())) {
            return 1;
        }else
        if (customBlock != null && ItemsAdderMining.containsKey(customBlock.getNamespacedID())){
            return 2;
        }else{
            return 0;
        }
    }

    public static boolean PlayerBreakBlock(Player player, Block block, int exp){
        if(isNaturalBlock(block)){
        for (Mine m:MiningWorlds) {
            if (block.getWorld().equals(m.getLoc().getWorld())) {
                int Result = IsMinable(block);
                if(Result > 0){
                    CustomBlock customBlock = CustomBlock.byAlreadyPlaced(block);
                    if(Result == 1){
                        for (ItemStack item : block.getDrops()){
                            player.getInventory().addItem(item);

                        }
                        block.setType(Material.AIR);
                        AmonPackPlugin.getPlayerMenager().AddPoints(LevelSkill.SkillType.MINING,player, (int)m.GetExpByMaterial(block.getType()));
                    }else {
                        double iaExp = m.GetExpByIA(customBlock.getNamespacedID());
                        AmonPackPlugin.getPlayerMenager().AddPoints(LevelSkill.SkillType.MINING,player, (int)iaExp);
                        exp = (int) (iaExp+1);
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
                    int DropRandom = getRandom(0, 40);
                    if (DropRandom <= m.getLootList().size()) {
                        String st = getRandomKeyFromHashMap(m.getLootList());
                        player.getInventory().addItem(Commands.QuestItemConfig(st));
                    }

                    int modifier = 1;
                    if(CraftingMenager.HaveEffect(player.getItemInUse(),"Expierience")) {
                        modifier+=1;
                    }
                    if(CraftingMenager.HaveEffect(player.getItemInUse(),"Looting")) {
                        if(getRandom(0, 10)>3){
                            if(customBlock!=null){
                                for (Object item : customBlock.getLoot()){
                                    player.getInventory().addItem((ItemStack) item);
                                }
                                double iaExp = m.GetExpByIA(customBlock.getNamespacedID());
                                AmonPackPlugin.getPlayerMenager().AddPoints(LevelSkill.SkillType.MINING,player, (int)iaExp);
                            }else{
                                for (ItemStack item : block.getDrops()){
                                    player.getInventory().addItem(item);
                                }
                                AmonPackPlugin.getPlayerMenager().AddPoints(LevelSkill.SkillType.MINING,player, (int)m.GetExpByMaterial(block.getType()));
                            }
                            modifier+=1;
                        }
                    }
                    player.giveExp((1+exp)*modifier);
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

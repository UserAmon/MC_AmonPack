//package Mechanics.PVE;
//
//import AvatarSystems.Levels.PlayerLevelMenager;
//import AvatarSystems.Util_Objects.LevelSkill;
//import AvatarSystems.Gathering.Objects.Mine;
//import com.projectkorra.projectkorra.util.TempBlock;
//import commands.Commands;
//import methods_plugins.AmonPackPlugin;
//import org.bukkit.*;
//import org.bukkit.block.Block;
//import org.bukkit.enchantments.Enchantment;
//import org.bukkit.entity.Player;
//import org.bukkit.event.EventHandler;
//import org.bukkit.event.Listener;
//import org.bukkit.event.block.BlockBreakEvent;
//import org.bukkit.inventory.ItemStack;
//import org.bukkit.inventory.meta.ItemMeta;
//
//import java.util.*;
//
//import static methods_plugins.Methods.getRandom;
//import static org.bukkit.Material.getMaterial;
//import static org.bukkit.Material.matchMaterial;
//
//public class Mining implements Listener {
//    public static List<Mine> ListOfMines = new ArrayList<>();
//    HashMap<Material, String> MiningOresDrops = new HashMap<>();
//    public static ItemStack PickaxeTier3;
//    public void setMiningTools(){
//        PickaxeTier3 = new ItemStack(Material.GOLDEN_PICKAXE, 1);
//        ItemMeta PickaxeTier3Meta = PickaxeTier3.getItemMeta();
//        PickaxeTier3Meta.addEnchant(Enchantment.UNBREAKING, 10, true);
//        PickaxeTier3Meta.addEnchant(Enchantment.EFFICIENCY, 6, true);
//        PickaxeTier3Meta.setDisplayName("" + ChatColor.GRAY +ChatColor.BOLD + "Kilof Tier 3");
//        PickaxeTier3.setItemMeta(PickaxeTier3Meta);
//    }
//
//    public Mining() {
//        setMiningTools();
//        for(String key : Objects.requireNonNull(AmonPackPlugin.getMinesConfig().getConfigurationSection("AmonPack.Mining")).getKeys(false)) {
//            double X = AmonPackPlugin.getMinesConfig().getDouble("AmonPack.Mining." + key + ".X");
//            double Y = AmonPackPlugin.getMinesConfig().getDouble("AmonPack.Mining." + key + ".Y");
//            double Z = AmonPackPlugin.getMinesConfig().getDouble("AmonPack.Mining." + key + ".Z");
//            String World = AmonPackPlugin.getMinesConfig().getString("AmonPack.Mining." + key + ".World");
//            int YOffsetUp = AmonPackPlugin.getMinesConfig().getInt("AmonPack.Mining." + key + ".Y-Offset-Up");
//            int Radius = AmonPackPlugin.getMinesConfig().getInt("AmonPack.Mining." + key + ".Radius");
//            int RestoreTime = AmonPackPlugin.getMinesConfig().getInt("AmonPack.Mining." + key + ".RestoreTime");
//            Material b = matchMaterial(Objects.requireNonNull(AmonPackPlugin.getMinesConfig().getString("AmonPack.Mining." + key + ".RevertBlock")));
//            HashMap<String, Integer> LChance = new HashMap<>();
//            HashMap<Material, Integer> OChance = new HashMap<>();
//            HashMap<Material, Double> ExpMap = new HashMap<>();
//            if (AmonPackPlugin.getMinesConfig().getConfigurationSection("AmonPack.Mining."+key+".Loot") != null){
//            for(String LootName : AmonPackPlugin.getMinesConfig().getConfigurationSection("AmonPack.Mining."+key+".Loot").getKeys(false)) {
//                for (int i = 0; i < AmonPackPlugin.getMinesConfig().getInt("AmonPack.Mining." + key + ".Loot."+LootName); i++) {
//                    LChance.put(LootName,AmonPackPlugin.getMinesConfig().getInt("AmonPack.Mining." + key + ".Loot."+LootName));
//                }}}
//            if (AmonPackPlugin.getMinesConfig().getConfigurationSection("AmonPack.Mining."+key+".Loot") != null){
//                for(String OresName : AmonPackPlugin.getMinesConfig().getConfigurationSection("AmonPack.Mining."+key+".Ores").getKeys(false)) {
//                for (int i = 0; i < AmonPackPlugin.getMinesConfig().getInt("AmonPack.Mining." + key + ".Ores."+OresName); i++) {
//                    OChance.put(Material.getMaterial(OresName),AmonPackPlugin.getMinesConfig().getInt("AmonPack.Mining." + key + ".Ores."+OresName));
//                }}}
//            if (AmonPackPlugin.getMinesConfig().getConfigurationSection("AmonPack.Mining."+key+".Exp") != null){
//                for(String OresName : AmonPackPlugin.getMinesConfig().getConfigurationSection("AmonPack.Mining."+key+".Exp").getKeys(false)) {
//                    ExpMap.put(Material.getMaterial(OresName),AmonPackPlugin.getMinesConfig().getDouble("AmonPack.Mining." + key + ".Exp."+OresName));
//                }}
//            Location loc = new Location(Bukkit.getWorld(World), X, Y, Z);
//            Mine mine = new Mine(loc,YOffsetUp,Radius,RestoreTime,b,OChance,LChance,ExpMap);
//            ListOfMines.add(mine);
//        }
//        for(String key : AmonPackPlugin.getMinesConfig().getConfigurationSection("AmonPack.MiningOresDrops").getKeys(false)) {
//            MiningOresDrops.put(Material.getMaterial(key),AmonPackPlugin.getMinesConfig().getString("AmonPack.MiningOresDrops." + key));
//        }
//    }
//
//    public static <K, V> K getRandomKeyFromHashMap(Map<K, V> map) {
//        Set<K> keySet = map.keySet();
//        K[] keyArray = (K[]) keySet.toArray(new Object[0]);
//        return keyArray[new Random().nextInt(keyArray.length)];
//    }
//
//    public void MineBlocks(Material MinedMat, Player player, Mine m, Block b){
//        if (b.getType().equals(MinedMat)) {
//            player.getInventory().addItem(Commands.QuestItemConfig(MiningOresDrops.get(MinedMat)));
//            if (PlayerLevelMenager.GetSkillByPlayer(LevelSkill.SkillType.MINING,player)>getRandom(0, 100)) {
//                player.getInventory().addItem(Commands.QuestItemConfig(MiningOresDrops.get(MinedMat)));
//            }
//            int DropRandom = getRandom(0, 40);
//            int OreRandom = getRandom(0, 40);
//            if (DropRandom <= m.getLootList().size()) {
//                String st = getRandomKeyFromHashMap(m.getLootList());
//                player.sendMessage(ChatColor.GRAY + "Udało Ci się wydobyć " + Commands.QuestItemConfig(st).getItemMeta().getDisplayName());
//                player.getInventory().addItem(Commands.QuestItemConfig(st));
//            }
//            if (OreRandom > m.getOresList().size()) {
//                b.setType(m.getMainBlock());
//                TempBlock tb1 = new TempBlock(b, Material.AIR);
//                tb1.setRevertTime(m.getRestoreTime()* 1000L);
//            } else if (OreRandom <= m.getOresList().size()) {
//                Material mt2 = getRandomKeyFromHashMap(m.getOresList());
//                b.setType(mt2);
//                TempBlock tb1 = new TempBlock(b, Material.AIR);
//                tb1.setRevertTime(m.getRestoreTime()* 1000L);
//            }
//            AmonPackPlugin.getPlayerMenager().AddPoints(LevelSkill.SkillType.MINING,player, m.GetExpByMaterial(MinedMat),ChatColor.AQUA+"Exp:");
//        }
//    }
//
//    @EventHandler
//    public void onBlockBreak(BlockBreakEvent event) {
//        if (!AmonPackPlugin.BuildingOnArenas){
//            Player player = event.getPlayer();
//        Block b = event.getBlock();
//        for (Mine m:ListOfMines) {
//            if (b.getWorld().equals(m.getLoc().getWorld())){
//                if (b.getLocation().distance(m.getLoc())<=m.getRadius()){
//                    event.setCancelled(true);
//                if (b.getLocation().getY()<=(m.getLoc().getY()+m.getYOffsetUp())){
////                    if (player.getInventory().getItemInMainHand().isSimilar(PickaxeTier3)) {
////                        for (Block blocks : Methods.getBlocksInRadius(b.getLocation(), 1)) {
////                            for (Material MinedMat:MiningOresDrops.keySet()) {
////                                MineBlocks(MinedMat,player,m,blocks);
////                            }}}
//                for (Material MinedMat:MiningOresDrops.keySet()) {
//                    MineBlocks(MinedMat,player,m,b);
//                    }}}}
//        }}}
//}

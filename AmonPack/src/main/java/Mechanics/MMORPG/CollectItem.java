package Mechanics.MMORPG;

import commands.Commands;
import methods_plugins.AmonPackPlugin;
import methods_plugins.Methods;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

import static methods_plugins.Methods.getRandom;

public class CollectItem implements Listener {
    Inventory menu;
    BukkitRunnable task;
    ItemStack ProgressDone = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
    ItemStack ProgressLeft = new ItemStack(Material.RED_STAINED_GLASS_PANE);
    List<String> Resource = new ArrayList<String>();
    List<String> ResourceLoot = new ArrayList<String>();
    private final Map<Player, Integer> clickCounts;
    private final Map<Player, Block> clickedblock;
    List<Location> ResourceLoc = new ArrayList<Location>();
    List<String> ResourceLocNames = new ArrayList<String>();
    public CollectItem(Plugin plugin) {
        for(String LocNum : AmonPackPlugin.getNewConfigz().getConfigurationSection("AmonPack.Gathering").getKeys(false)) {
            for(String LocOpt : AmonPackPlugin.getNewConfigz().getConfigurationSection("AmonPack.Gathering."+LocNum).getKeys(false)) {
            if (!LocOpt.equals("Resource")){
                ResourceLoc.add(new Location(Bukkit.getWorld(AmonPackPlugin.getNewConfigz().getString("AmonPack.Gathering."+LocNum+".Options.World")),AmonPackPlugin.getNewConfigz().getDouble("AmonPack.Gathering."+LocNum+".Options.X"),AmonPackPlugin.getNewConfigz().getDouble("AmonPack.Gathering."+LocNum+".Options.Y"),AmonPackPlugin.getNewConfigz().getDouble("AmonPack.Gathering."+LocNum+".Options.Z")));
                ResourceLocNames.add(LocNum);
            }else{
                for(String key : AmonPackPlugin.getNewConfigz().getConfigurationSection("AmonPack.Gathering."+ LocNum +".Resource").getKeys(false)) {
                    Resource.add(key);
                    ResourceLoot.add(AmonPackPlugin.getNewConfigz().getString("AmonPack.Gathering."+ LocNum +".Resource."+key+".Loot"));
                }}}}
        this.clickCounts = new HashMap<>();
        this.clickedblock = new HashMap<>();
        ItemMeta ProgressDonemeta = ProgressDone.getItemMeta();
        ItemMeta ProgressLeftmeta = ProgressDone.getItemMeta();
        ProgressDonemeta.setDisplayName(ChatColor.GREEN +"Progress Done");
        ProgressLeftmeta.setDisplayName(ChatColor.RED +"Progress Left");
        ProgressDone.setItemMeta(ProgressDonemeta);
        ProgressLeft.setItemMeta(ProgressLeftmeta);
    }
    private void openMenu(Player player,int ReqClickAmount, ItemStack item, long delay) {
        menu = Bukkit.createInventory(player, 45);
        for (int i = 0; i < 9; i++) {
            if (clickCounts.get(player) != 0 && i < (9*clickCounts.get(player))/ReqClickAmount){
                menu.setItem(i, ProgressDone);
            }else{
                menu.setItem(i, ProgressLeft);
            }}
        menu.setItem(getRandom(9, 44), item);
        player.openInventory(menu);
        task = new BukkitRunnable() {
            @Override
            public void run() {
                if (player.getOpenInventory().getTopInventory().equals(menu)){
                    openMenu(player,ReqClickAmount,item,delay);
            }}};
        task.runTaskLater(AmonPackPlugin.plugin, delay);
    }
    private void addItemToInventory(Player player, int i) {
        for (int i2 = 0; i2 < ResourceLoc.size(); i2++) {
            if (player.getLocation().distance(ResourceLoc.get(0))<AmonPackPlugin.getNewConfigz().getInt("AmonPack.Gathering."+ResourceLocNames.get(i2)+".Options.Radius")) {
                player.getInventory().addItem(Commands.QuestItemConfig(AmonPackPlugin.getNewConfigz().getString("AmonPack.Gathering."+ ResourceLocNames.get(i2) +".Resource."+Resource.get(i)+".Loot")));
        }}
    }
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (AmonPackPlugin.MiningAndGatheringOn == true){
        Action action = event.getAction();
        Block block = event.getClickedBlock();
        Player player = event.getPlayer();
        if (action == Action.RIGHT_CLICK_BLOCK && block != null) {
            for (int i2 = 0; i2 < ResourceLoc.size(); i2++) {
                if (player.getWorld().equals(ResourceLoc.get(i2).getWorld()) && player.getLocation().distance(ResourceLoc.get(0))<AmonPackPlugin.getNewConfigz().getInt("AmonPack.Gathering."+ResourceLocNames.get(i2)+".Options.Radius")) {
                    for (int i = 0; i < Resource.size(); i++) {
                        if (block.getType() == Material.getMaterial(Resource.get(i))) {
                            event.setCancelled(true);
                            clickedblock.put(player,event.getClickedBlock());
                            clickCounts.put(player, 0);
                            if (task != null){
                                task.cancel();
                            }
                            if (isTreeWood(event.getClickedBlock().getType())){
                                if (isAxe(player.getInventory().getItemInMainHand())){
                                    TreeDecap(player,i2,i, new ItemStack(Material.getMaterial(Resource.get(i))));
                                }
                            }else {
                                if (block.getBlockData() instanceof Ageable) {
                                    if (((Ageable) block.getBlockData()).getAge() == 7) {
                                        int clicksreq = AmonPackPlugin.getNewConfigz().getInt("AmonPack.Gathering." + ResourceLocNames.get(i2) + ".Resource." + Resource.get(i) + ".ClickReq");
                                        openMenu(player, clicksreq, Commands.QuestItemConfig( AmonPackPlugin.getNewConfigz().getString("AmonPack.Gathering." + ResourceLocNames.get(i2) + ".Resource." + Resource.get(i) + ".Loot")), AmonPackPlugin.getNewConfigz().getInt("AmonPack.Gathering." + ResourceLocNames.get(i2) + ".Resource." + Resource.get(i) + ".DelocateTime"));
                                    }}else {
                                    int clicksreq = AmonPackPlugin.getNewConfigz().getInt("AmonPack.Gathering." + ResourceLocNames.get(i2) + ".Resource." + Resource.get(i) + ".ClickReq");
                                    openMenu(player, clicksreq, Commands.QuestItemConfig( AmonPackPlugin.getNewConfigz().getString("AmonPack.Gathering." + ResourceLocNames.get(i2) + ".Resource." + Resource.get(i) + ".Loot")), AmonPackPlugin.getNewConfigz().getInt("AmonPack.Gathering." + ResourceLocNames.get(i2) + ".Resource." + Resource.get(i) + ".DelocateTime"));
                                }}}}}}}}
    }
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (AmonPackPlugin.MiningAndGatheringOn == true){
        Player player = (Player) event.getWhoClicked();
        Inventory inventory = event.getClickedInventory();
        ItemStack clickedItem = event.getCurrentItem();
        if (event.getView().getTopInventory().equals(menu)) {
            event.setCancelled(true);
                    for (int i2 = 0; i2 < ResourceLoc.size(); i2++) {
                        if (player.getWorld().equals(ResourceLoc.get(i2).getWorld()) &&player.getLocation().distance(ResourceLoc.get(0))<AmonPackPlugin.getNewConfigz().getInt("AmonPack.Gathering."+ResourceLocNames.get(i2)+".Options.Radius")) {
                            for (int i = 0; i < Resource.size(); i++) {
                                if (clickedItem.getType() == Commands.QuestItemConfig(AmonPackPlugin.getNewConfigz().getString("AmonPack.Gathering."+ ResourceLocNames.get(i2) +".Resource."+Resource.get(i)+".Loot")).getType() && !isTreeWood(clickedItem.getType())) {
                                    clickCounts.put(player, clickCounts.get(player) + 1);
                                    task.cancel();
                                    task.run();
                                    int clicksreq = AmonPackPlugin.getNewConfigz().getInt("AmonPack.Gathering."+ ResourceLocNames.get(i2) +".Resource."+Resource.get(i)+".ClickReq");
                                    if (clickCounts.get(player) >= clicksreq) {
                                addItemToInventory(player, i);
                                player.closeInventory();
                                clickCounts.remove(player);
                                if (clickedblock.get(player) != null){
                                    Block block = clickedblock.get(player);
                                    if (block != null && block.getType() == Material.getMaterial(Resource.get(i))) {
                                        Particle particle = Particle.BLOCK_DUST;
                                        Object data = Bukkit.createBlockData(Material.getMaterial(Resource.get(i)));
                                        block.getWorld().spawnParticle(particle, block.getLocation().add(0,1,0), 30, data);
                                        block.getWorld().spawnParticle(particle, block.getLocation().add(0,0.5,0), 30, data);
                                        block.getWorld().spawnParticle(particle, block.getLocation(), 30, data);
                                        Material mat = Material.getMaterial(Resource.get(i));
                                        block.setType(Material.AIR);
                                        Bukkit.getScheduler().runTaskLater(AmonPackPlugin.plugin, () -> {
                                            block.setType(mat);
                                        }, AmonPackPlugin.getNewConfigz().getInt("AmonPack.Gathering."+ ResourceLocNames.get(i2) +".Resource."+Resource.get(i)+".RestoreTime")*20);
                                    }
                                    clickedblock.remove(player);
                                }}}
                                if (clickedItem.getType() == Commands.QuestItemConfig(AmonPackPlugin.getNewConfigz().getString("AmonPack.Gathering."+ ResourceLocNames.get(i2) +".Resource."+Resource.get(i)+".Loot")).getType() && isTreeWood(clickedItem.getType())) {
                                    int clickCount = clickCounts.get(player) + 1;
                                clickCounts.put(player, clickCount);
                                task.cancel();
                                task.run();
                                int clicksreq = AmonPackPlugin.getNewConfigz().getInt("AmonPack.Gathering."+ ResourceLocNames.get(i2) +".Resource."+Resource.get(i)+".ClickReq");
                                if (clickCount >= clicksreq && isTreeWood(clickedItem.getType())) {
                                    player.closeInventory();
                                    clickCounts.remove(player);
                                    int restore = AmonPackPlugin.getNewConfigz().getInt("AmonPack.Gathering."+ ResourceLocNames.get(i2) +".Resource."+Resource.get(i)+".RestoreTime");
                                    destroyTree(Methods.getTargetLocation(player,7), restore*20);
                                }}
                            }}}}}}

    public void TreeDecap(Player player, int i2, int i, ItemStack item) {
            int clicksreq = AmonPackPlugin.getNewConfigz().getInt("AmonPack.Gathering." + ResourceLocNames.get(i2) + ".Resource." + Resource.get(i) + ".ClickReq");
                openMenu(player, clicksreq, item, AmonPackPlugin.getNewConfigz().getInt("AmonPack.Gathering." + ResourceLocNames.get(i2) + ".Resource." + Resource.get(i) + ".DelocateTime"));
            }

    public void destroyTree(Location treeLocation, int i) {
        Block baseBlock = treeLocation.getBlock();
        if (!isTreeWood(baseBlock.getType())) {
            return;
        }
        destroyTreeRecursive(baseBlock, i);
    }

    private boolean isTreeWood(Material blockType) {
        return blockType == Material.OAK_LOG || blockType == Material.SPRUCE_LOG
                || blockType == Material.BIRCH_LOG || blockType == Material.JUNGLE_LOG
                || blockType == Material.ACACIA_LOG || blockType == Material.DARK_OAK_LOG;
    }

    private boolean isAxe(ItemStack item) {
        Material mat = item.getType();
        return mat == Material.WOODEN_AXE || mat == Material.STONE_AXE || mat == Material.IRON_AXE || mat == Material.GOLDEN_AXE || mat == Material.DIAMOND_AXE || mat == Material.NETHERITE_AXE;
    }


    private boolean isTreeLeaf(Block block) {
        Material blockType = block.getType();
        return blockType == Material.OAK_LEAVES || blockType == Material.SPRUCE_LEAVES
                || blockType == Material.BIRCH_LEAVES || blockType == Material.JUNGLE_LEAVES
                || blockType == Material.ACACIA_LEAVES || blockType == Material.DARK_OAK_LEAVES;
    }

    private void destroyTreeRecursive(Block block,int i) {
        if (isTreeWood(block.getType()) || isTreeLeaf(block)) {
            Material b = block.getType();
            block.breakNaturally();
            Bukkit.getScheduler().runTaskLater(AmonPackPlugin.plugin, new Runnable() {
                @Override
                public void run() {
                    block.setType(b);
                }
            }, i);
            block.breakNaturally();

            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        if (x != 0 || y != 0 || z != 0) {
                            Block adjacentBlock = block.getRelative(x, y, z);
                            destroyTreeRecursive(adjacentBlock,i);
                        }
                    }
                }
            }
        }
    }




}

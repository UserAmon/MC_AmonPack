package AvatarSystems.Util_Objects;

import AvatarSystems.PlayerLevelMenager;
import commands.Commands;
import dev.lone.itemsadder.api.FontImages.FontImageWrapper;
import dev.lone.itemsadder.api.FontImages.TexturedInventoryWrapper;
import methods_plugins.AmonPackPlugin;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

import static AvatarSystems.ForestMenager.ForestHolder;
import static methods_plugins.Methods.getRandom;

public class Forest {
    private Location CenterLocation;
    private double Range;
    private List<Resource> ListOfResources;
    private List<Material> Materials;
    private Map<Player, Integer> clickCounts;
    private Map<Player, Block> clickedblock;
    private Map<Player, BukkitRunnable> Tasks;

    public Forest(Location centerLocation, List<Resource> listOfResources, double range) {
        CenterLocation = centerLocation;
        ListOfResources = listOfResources;
        Range = range;
        Materials=new ArrayList<>();
        for (Resource resource : listOfResources){
            Materials.add(resource.getBlockName());
        }
        clickCounts=new HashMap<>();
        clickedblock=new HashMap<>();
        Tasks=new HashMap<>();

    }

    public void HandleForestInteract(Player player, Block block){
        Resource resource = GetResourceByMaterial(block.getType());
        if(isTreeWood(block.getType())){
            if(!isAxe(player.getInventory().getItemInMainHand())){
                player.sendMessage(ChatColor.RED +"Musisz uzyć siekiery, aby ściąć to drzewo!");
                return;
            }}
        clickedblock.put(player,block);
        clickCounts.put(player, 0);
        OpenClickMenu(player, resource);
    }
    public void HandleForestInvClick(Player player, ItemStack clickeditem){
        if (clickedblock.get(player) != null) {
            Block block = clickedblock.get(player);
            Resource resource = GetResourceByMaterial(block.getType());
            if(clickeditem.isSimilar(resource.getLootName())){
                clickCounts.put(player, clickCounts.get(player) + 1);
                if (clickCounts.get(player) >= resource.getClickRequired()) {
                    AmonPackPlugin.getPlayerMenager().AddPoints(LevelSkill.SkillType.FARMING,player, resource.getExp(),ChatColor.AQUA+"Exp:");
                    clickCounts.remove(player);
                    block.getWorld().spawnParticle(Particle.BLOCK, block.getLocation().add(0,1,0), 30, Bukkit.createBlockData(block.getType()));
                    block.getWorld().spawnParticle(Particle.BLOCK, block.getLocation().add(0,0.5,0), 30, Bukkit.createBlockData(block.getType()));
                    block.getWorld().spawnParticle(Particle.BLOCK, block.getLocation(), 30, Bukkit.createBlockData(block.getType()));
                    Material revertmat =block.getType();
                    if(isTreeWood(block.getType())){
                        destroyTree(block.getLocation(), (int) (resource.getRestoreTime()*20));
                    }else {
                        player.getInventory().addItem(resource.getLootName());
                        block.setType(Material.AIR);
                        Bukkit.getScheduler().runTaskLater(AmonPackPlugin.plugin, () -> {
                            block.setType(revertmat);
                        }, resource.getRestoreTime() * 20);
                    }
                    player.closeInventory();
                    clickedblock.remove(player);
                }else{
                    OpenClickMenu(player, resource);
                }
            }
        }
    }

    private void OpenClickMenu(Player player,Resource resource) {
        TexturedInventoryWrapper inventory = new TexturedInventoryWrapper(ForestHolder,
                ForestHolder.getSize(), ForestHolder.getTitle(), new FontImageWrapper("amon:first_gui")
        );
        Inventory inv = inventory.getInternal();
        for (int i = 0; i < 9; i++) {
            if (clickCounts.get(player) != 0 && i < (9*clickCounts.get(player))/resource.getClickRequired()){
                inv.setItem(i, ReturnItem().get(0));
            }else{
                inv.setItem(i, ReturnItem().get(1));
            }}
        inv.setItem(getRandom(9, 44), resource.getLootName());
        inventory.showInventory(player);
        if(Tasks.get(player)!=null){
            Tasks.get(player).cancel();
        }
            BukkitRunnable task = new BukkitRunnable() {
                @Override
                public void run() {
                    if (Objects.equals(player.getOpenInventory().getTopInventory().getHolder(), ForestHolder)){
                        OpenClickMenu(player,resource);
                    }}};
            task.runTaskLater(AmonPackPlugin.plugin, resource.getChangeLocationTimer());
            Tasks.put(player,task);

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
                        }}}}
        }}
    private List<ItemStack> ReturnItem(){
        List<ItemStack> ListOfItemStacks=new ArrayList<>();
        ItemStack ProgressDone=new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
        ItemMeta ProgressDoneMeta = ProgressDone.getItemMeta();
        ProgressDoneMeta.setDisplayName(ChatColor.GREEN +"Progress Done");
        ProgressDone.setItemMeta(ProgressDoneMeta);
        ListOfItemStacks.add(0,ProgressDone);
        ItemStack ProgressLeft=new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta ProgressLeftMeta = ProgressLeft.getItemMeta();
        ProgressLeftMeta.setDisplayName(ChatColor.RED +"Progress Left");
        ProgressLeft.setItemMeta(ProgressLeftMeta);
        ListOfItemStacks.add(1,ProgressLeft);
        return ListOfItemStacks;
    }
    public Location getCenterLocation() {
        return CenterLocation;
    }
    public List<Resource> getListOfResources() {
        return ListOfResources;
    }
    public double getRange() {
        return Range;
    }
    public List<Material> getMaterials() {
        return Materials;
    }
    public Resource GetResourceByMaterial(Material mat){
        Optional<Resource> Exist = ListOfResources.stream().filter(res -> res.getBlockName()==mat).findFirst();
        if(Exist.isPresent()){
            return Exist.get();
        }
        return null;
    }
}


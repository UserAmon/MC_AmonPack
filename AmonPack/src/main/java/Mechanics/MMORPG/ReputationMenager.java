package Mechanics.MMORPG;

import UtilObjects.MMORPG.Reputation;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ReputationMenager implements Listener {
    static ItemStack Background = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
    private static List<Reputation> RepList = new ArrayList<>();

    public ReputationMenager(){
        ItemMeta BackgroundMeta = Background.getItemMeta();
        BackgroundMeta.setDisplayName(ChatColor.BLACK+"");
        Background.setItemMeta(BackgroundMeta);
        RepList.add(new Reputation("Naród Ognia","RepLvL1","FIRE_CHARGE",10,1,"RED"));
        RepList.add(new Reputation("Naród Powietrza","RepLvL3","COBWEB",10,3,"GRAY"));
        RepList.add(new Reputation("Naród Ziemi","RepLvL5","DIRT",10,5,"GREEN"));
        RepList.add(new Reputation("Naród Wody","RepLvL7","WATER_BUCKET",10,7,"BLUE"));
    }
    public static void OpenRepGui(Player p){
        Inventory menu = Bukkit.createInventory(null, 54, "RepGui");
        for (int i = 0; i < menu.getSize(); i++) {
            menu.setItem(i,Background);
        }
        for (Reputation rep:RepList) {
            ItemStack RepItem = new ItemStack(Objects.requireNonNull(Material.getMaterial(rep.getMaterial())));
            ItemMeta RepItemMeta = RepItem.getItemMeta();
            RepItemMeta.setDisplayName(ChatColor.valueOf(rep.getColor()) + rep.getName());
            RepItem.setItemMeta(RepItemMeta);
            menu.setItem(rep.getPlaceInGui(),RepItem);
        }
        p.openInventory(menu);
    }
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() == null && event.getCurrentItem() != null) {
            if (event.getView().getTitle().equals("RepGui")) {
                event.setCancelled(true);
            }}
    }
}

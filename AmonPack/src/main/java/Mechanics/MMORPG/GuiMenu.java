package Mechanics.MMORPG;

import methods_plugins.AmonPackPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class GuiMenu implements Listener {
    private static Map<ItemStack, Integer> HelpItemList = new HashMap<>();
    private static Map<ItemStack, Integer> ItemListList = new HashMap<>();
    public GuiMenu(){
        for(String key : AmonPackPlugin.getGuiConfig().getConfigurationSection("AmonPack.Gui.Help").getKeys(false)) {
            String type = AmonPackPlugin.getGuiConfig().getString("AmonPack.Gui.Help." + key + ".Type");
            String name = AmonPackPlugin.getGuiConfig().getString("AmonPack.Gui.Help." + key + ".Name");
            String lore = AmonPackPlugin.getGuiConfig().getString("AmonPack.Gui.Help." + key + ".Lore");
            ItemStack item = new ItemStack(Objects.requireNonNull(Material.getMaterial(type)));
            ItemMeta itemmeta = item.getItemMeta();
            itemmeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            List<String> lorelist = new ArrayList<String>();
            lorelist.add(ChatColor.translateAlternateColorCodes('&', lore));
            itemmeta.setLore(lorelist);
            item.setItemMeta(itemmeta);
            HelpItemList.put(item, Integer.valueOf(key));
        }
        for(String key : AmonPackPlugin.getGuiConfig().getConfigurationSection("AmonPack.Gui.ItemList").getKeys(false)) {
            String type = AmonPackPlugin.getGuiConfig().getString("AmonPack.Gui.ItemList." + key + ".Type");
            String name = AmonPackPlugin.getGuiConfig().getString("AmonPack.Gui.ItemList." + key + ".Name");
            String lore = AmonPackPlugin.getGuiConfig().getString("AmonPack.Gui.ItemList." + key + ".Source");
            ItemStack item = new ItemStack(Objects.requireNonNull(Material.getMaterial(type)));
            ItemMeta itemmeta = item.getItemMeta();
            itemmeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            List<String> lorelist = new ArrayList<String>();
            lorelist.add(ChatColor.translateAlternateColorCodes('&', lore));
            itemmeta.setLore(lorelist);
            item.setItemMeta(itemmeta);
            ItemListList.put(item, Integer.valueOf(key));
        }
        }

    public static void OItemGui(Player player) {
        Inventory menu = Bukkit.createInventory(null, 54, "ItemGui");
        for (ItemStack IS:ItemListList.keySet()) {
            menu.setItem(ItemListList.get(IS), IS);
        }
        player.openInventory(menu);
    }
    public static void OHelpGui(Player player) {
        Inventory menu = Bukkit.createInventory(null, 54, "HelpGui");
        for (ItemStack IS:HelpItemList.keySet()) {
            menu.setItem(HelpItemList.get(IS), IS);
        }
        player.openInventory(menu);
    }

}

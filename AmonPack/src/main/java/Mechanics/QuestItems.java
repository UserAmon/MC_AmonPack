package Mechanics;

import methods_plugins.AmonPackPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class QuestItems {
    public static List<ItemStack>ListOfAllQuestItems = new ArrayList<>();
    public QuestItems() {
        for(String key : AmonPackPlugin.getNewConfigz().getConfigurationSection("AmonPack.Items").getKeys(false)) {
            if (AmonPackPlugin.getNewConfigz().getString("AmonPack.Items." + key + ".Name") != null) {
                String type = AmonPackPlugin.getNewConfigz().getString("AmonPack.Items." + key + ".Type");
                String name = ""+AmonPackPlugin.getNewConfigz().getString("AmonPack.Items." + key + ".Name").replace("&", "§");
                List<String> lorelist = new ArrayList<String>();
                if (AmonPackPlugin.getNewConfigz().getConfigurationSection("AmonPack.Items." + key + ".Lore") != null) {
                    for(String lores : AmonPackPlugin.getNewConfigz().getConfigurationSection("AmonPack.Items." + key + ".Lore").getKeys(false)) {
                        String lore = ""+AmonPackPlugin.getNewConfigz().getString("AmonPack.Items." + key + ".Lore." + lores).replace("&", "§");;
                        if (lore != null) {
                            lorelist.add(ChatColor.translateAlternateColorCodes('&', lore));
                        }}}
                ItemStack QuestItem = new ItemStack(Material.getMaterial(type), 1);
                ItemMeta QuestItemMeta = QuestItem.getItemMeta();
                if (AmonPackPlugin.getNewConfigz().getConfigurationSection("AmonPack.Items." + key + ".Enchantment") != null) {
                    for(String enchname : AmonPackPlugin.getNewConfigz().getConfigurationSection("AmonPack.Items." + key + ".Enchantment").getKeys(false)) {
                        int enchpower = AmonPackPlugin.getNewConfigz().getInt("AmonPack.Items." + key + ".Enchantment." + enchname + ".EnchantmentLevel");
                        QuestItemMeta.addEnchant(Enchantment.getByName(enchname), enchpower, true);
                    }}
                if (name != null) {
                    QuestItemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
                }
                if (lorelist != null) {
                    QuestItemMeta.setLore(lorelist);
                }
                QuestItem.setItemMeta(QuestItemMeta);
                ListOfAllQuestItems.add(QuestItem);
            }}
    }
}

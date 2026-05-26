package RPG.Dungeons;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DungeonInventoryBackup {

    /**
     * Saves the player's inventory and XP, then clears all non-crafted items.
     */
    public static void backupAndClearInventory(Player player, Plugin plugin) {
        if (player == null || !player.isOnline()) return;

        File backupsFolder = new File(plugin.getDataFolder(), "backups");
        if (!backupsFolder.exists()) {
            backupsFolder.mkdirs();
        }

        File file = new File(backupsFolder, player.getUniqueId().toString() + ".yml");
        YamlConfiguration config = new YamlConfiguration();

        // 1. Save standard inventories as serializable objects
        config.set("inventory.contents", player.getInventory().getContents());
        config.set("inventory.armor", player.getInventory().getArmorContents());
        config.set("inventory.extra", player.getInventory().getExtraContents());
        config.set("inventory.offhand", player.getInventory().getItemInOffHand());
        config.set("xp.level", player.getLevel());
        config.set("xp.exp", player.getExp());

        try {
            config.save(file);
            System.out.println("[Dungeons] Zapisano kopie ekwipunku dla gracza: " + player.getName());
        } catch (IOException e) {
            System.err.println("[Dungeons] Blad podczas zapisu ekwipunku dla gracza " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }

        // 2. Clear non-crafted items from the player
        clearNonCraftedItems(player);
    }

    /**
     * Restores the player's survival inventory and XP from their YAML backup,
     * merging any crafted items currently held with the restored items.
     */
    public static void restoreInventory(Player player, Plugin plugin) {
        if (player == null || !player.isOnline()) return;

        File backupsFolder = new File(plugin.getDataFolder(), "backups");
        File file = new File(backupsFolder, player.getUniqueId().toString() + ".yml");

        if (!file.exists()) {
            System.err.println("[Dungeons] Brak pliku kopii zapasowej dla gracza: " + player.getName());
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        // Save any crafted items they currently have in their dungeon inventory to merge them
        List<ItemStack> craftedItemsToKeep = new ArrayList<>();
        for (ItemStack item : player.getInventory().getContents()) {
            if (isCraftedItem(item)) {
                craftedItemsToKeep.add(item.clone());
            }
        }
        for (ItemStack item : player.getInventory().getArmorContents()) {
            if (isCraftedItem(item)) {
                craftedItemsToKeep.add(item.clone());
            }
        }
        ItemStack offHandItem = player.getInventory().getItemInOffHand();
        if (isCraftedItem(offHandItem)) {
            craftedItemsToKeep.add(offHandItem.clone());
        }

        // Clear their current dungeon items
        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);
        player.getInventory().setItemInOffHand(null);

        // Restore saved inventory
        try {
            // Restore XP
            player.setLevel(config.getInt("xp.level", 0));
            player.setExp((float) config.getDouble("xp.exp", 0.0));

            // Restore armor contents
            List<?> armorList = config.getList("inventory.armor");
            if (armorList != null) {
                player.getInventory().setArmorContents(armorList.toArray(new ItemStack[0]));
            }

            // Restore main contents
            List<?> mainList = config.getList("inventory.contents");
            if (mainList != null) {
                player.getInventory().setContents(mainList.toArray(new ItemStack[0]));
            }

            // Restore extra contents
            List<?> extraList = config.getList("inventory.extra");
            if (extraList != null) {
                player.getInventory().setExtraContents(extraList.toArray(new ItemStack[0]));
            }

            // Restore offhand
            ItemStack savedOffhand = config.getItemStack("inventory.offhand");
            if (savedOffhand != null) {
                player.getInventory().setItemInOffHand(savedOffhand);
            }

            // Merge back the crafted items (putting them in vacant slots)
            for (ItemStack crafted : craftedItemsToKeep) {
                player.getInventory().addItem(crafted);
            }

            System.out.println("[Dungeons] Przywrocono ekwipunek dla gracza: " + player.getName());
        } catch (Exception e) {
            System.err.println("[Dungeons] Blad podczas przywracania ekwipunku dla gracza " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }

        // Delete the backup file
        if (file.exists()) {
            file.delete();
        }
    }

    /**
     * Clears all non-crafted items from the player's inventory.
     */
    private static void clearNonCraftedItems(Player player) {
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item != null && !item.getType().isAir()) {
                if (!isCraftedItem(item)) {
                    player.getInventory().setItem(i, null);
                }
            }
        }

        ItemStack[] armor = player.getInventory().getArmorContents();
        for (int i = 0; i < armor.length; i++) {
            ItemStack item = armor[i];
            if (item != null && !item.getType().isAir()) {
                if (!isCraftedItem(item)) {
                    armor[i] = null;
                }
            }
        }
        player.getInventory().setArmorContents(armor);

        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (offhand != null && !offhand.getType().isAir()) {
            if (!isCraftedItem(offhand)) {
                player.getInventory().setItemInOffHand(null);
            }
        }
    }

    /**
     * Checks if an item is a crafted item by querying CraftingMenager.
     */
    public static boolean isCraftedItem(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        try {
            if (RPG.Crafting.CraftingMenager.getCraftedToolByItem(item) != null) return true;
            if (RPG.Crafting.CraftingMenager.getCraftableItemByItem(item) != null) return true;
            if (RPG.Crafting.CraftingMenager.IsArmor(item)) return true;
            if (RPG.Crafting.CraftingMenager.IsWeapon(item)) return true;
            if (RPG.Crafting.CraftingMenager.getItemMoldByItem(item) != null) return true;
        } catch (NoClassDefFoundError | Exception e) {
            // Handle safely in case classes are missing or not loaded
        }
        return false;
    }
}

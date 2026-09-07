package CustomContent.Hooks;

import RPG.Util.InventoryXHolder;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public class ItemsAdderHook {

    private static Boolean hasItemsAdder = null;

    public static boolean isAvailable() {
        if (hasItemsAdder == null) {
            try {
                Class.forName("dev.lone.itemsadder.api.CustomStack");
                hasItemsAdder = Bukkit.getPluginManager().getPlugin("ItemsAdder") != null;
            } catch (Throwable e) {
                hasItemsAdder = false;
            }
        }
        return Boolean.TRUE.equals(hasItemsAdder);
    }

    public static ItemStack getItem(String namespacedId) {
        if (!isAvailable()) return null;
        try {
            dev.lone.itemsadder.api.CustomStack cs = dev.lone.itemsadder.api.CustomStack.getInstance(namespacedId);
            return cs != null ? cs.getItemStack() : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static String getCustomBlockNamespacedId(Block block) {
        if (!isAvailable() || block == null) return null;
        try {
            dev.lone.itemsadder.api.CustomBlock cb = dev.lone.itemsadder.api.CustomBlock.byAlreadyPlaced(block);
            return cb != null ? cb.getNamespacedID() : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static boolean removeCustomBlock(Block block) {
        if (!isAvailable() || block == null) return false;
        try {
            dev.lone.itemsadder.api.CustomBlock cb = dev.lone.itemsadder.api.CustomBlock.byAlreadyPlaced(block);
            if (cb != null) {
                cb.playBreakParticles();
                cb.playBreakEffect();
                cb.playBreakSound();
                cb.remove();
                return true;
            }
        } catch (Throwable ignored) {}
        return false;
    }

    public static Inventory createTexturedInventory(InventoryHolder holder, int size, String title, String fontImage) {
        if (isAvailable()) {
            try {
                dev.lone.itemsadder.api.FontImages.TexturedInventoryWrapper wrapper =
                        new dev.lone.itemsadder.api.FontImages.TexturedInventoryWrapper(
                                holder, size, title, new dev.lone.itemsadder.api.FontImages.FontImageWrapper(fontImage));
                return wrapper.getInternal();
            } catch (Throwable ignored) {}
        }
        return Bukkit.createInventory(holder, size, title);
    }

    public static void showTexturedInventory(Player player, InventoryHolder holder, int size, String title, String fontImage, Inventory inv) {
        if (player == null) return;
        if (isAvailable()) {
            try {
                dev.lone.itemsadder.api.FontImages.TexturedInventoryWrapper wrapper =
                        new dev.lone.itemsadder.api.FontImages.TexturedInventoryWrapper(
                                holder, size, title, new dev.lone.itemsadder.api.FontImages.FontImageWrapper(fontImage));
                if (inv != null) {
                    wrapper.getInternal().setContents(inv.getContents());
                }
                wrapper.showInventory(player);
                return;
            } catch (Throwable ignored) {}
        }
        if (inv != null) {
            player.openInventory(inv);
        } else {
            player.openInventory(Bukkit.createInventory(holder, size, title));
        }
    }

    public static String getCustomFurnitureNamespacedId(Block block) {
        if (!isAvailable() || block == null) return null;
        try {
            dev.lone.itemsadder.api.CustomFurniture f = dev.lone.itemsadder.api.CustomFurniture.byAlreadySpawned(block);
            return f != null ? f.getNamespacedID() : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static String getCustomFurnitureNamespacedId(Entity entity) {
        if (!isAvailable() || entity == null) return null;
        try {
            dev.lone.itemsadder.api.CustomFurniture f = dev.lone.itemsadder.api.CustomFurniture.byAlreadySpawned(entity);
            return f != null ? f.getNamespacedID() : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static boolean isShelfOrFurniture(Block block) {
        if (block == null) return false;
        String furnitureId = getCustomFurnitureNamespacedId(block);
        if (furnitureId != null) {
            return true;
        }
        String blockId = getCustomBlockNamespacedId(block);
        if (blockId != null) {
            return isShelfId(blockId);
        }
        return false;
    }

    public static boolean isShelfOrFurniture(Entity entity) {
        if (entity == null) return false;
        String furnitureId = getCustomFurnitureNamespacedId(entity);
        if (furnitureId != null) {
            return true;
        }
        return false;
    }

    public static boolean isShelfId(String id) {
        if (id == null) return false;
        String lower = id.toLowerCase();
        return lower.contains("shelf") || lower.contains("polka") || lower.contains("polki")
                || lower.contains("rack") || lower.contains("stand") || lower.contains("display")
                || lower.contains("showcase") || lower.contains("wieszak") || lower.contains("furniture")
                || lower.contains("stojak") || lower.contains("gablota") || lower.contains("szafka")
                || lower.contains("holder") || lower.contains("board");
    }
}

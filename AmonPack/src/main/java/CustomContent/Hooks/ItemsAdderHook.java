package CustomContent.Hooks;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

public class ItemsAdderHook {

    private static Boolean hasItemsAdder = null;

    public static boolean isAvailable() {
        if (hasItemsAdder == null) {
            hasItemsAdder = Bukkit.getPluginManager().getPlugin("ItemsAdder") != null;
        }
        return hasItemsAdder;
    }

    public static ItemStack getItem(String namespacedId) {
        if (!isAvailable()) return null;
        try {
            return dev.lone.itemsadder.api.CustomStack.getInstance(namespacedId) != null
                    ? dev.lone.itemsadder.api.CustomStack.getInstance(namespacedId).getItemStack()
                    : null;
        } catch (Throwable ignored) {
            return null;
        }
    }
}

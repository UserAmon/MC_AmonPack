package AvatarSystems.Util_Objects;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class InventoryXHolder implements InventoryHolder {
    private final Inventory inventory;
    private int Size;
    private String Title;
    public InventoryXHolder(int size, String title) {
        this.inventory = Bukkit.createInventory(this, size, title);
        Size=size;
        Title=title;
    }
    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public int getSize() {
        return Size;
    }

    public String getTitle() {
        return Title;
    }
}
package Mechanics.PVE.Menagerie;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class CustomInventoryOwner implements InventoryHolder {
    private Inventory inventory;

    public CustomInventoryOwner() {
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }
}

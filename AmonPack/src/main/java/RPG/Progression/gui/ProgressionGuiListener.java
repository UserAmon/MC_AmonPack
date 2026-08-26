package RPG.Progression.gui;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;

public class ProgressionGuiListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory() == null) return;
        InventoryHolder holder = event.getInventory().getHolder();

        if (holder instanceof ProgressionMenuGui menu) {
            menu.handleClick(event);
        } else if (holder instanceof StageDetailGui detail) {
            detail.handleClick(event);
        } else if (holder instanceof ExplorationBiomesGui biomes) {
            biomes.handleClick(event);
        }
    }
}

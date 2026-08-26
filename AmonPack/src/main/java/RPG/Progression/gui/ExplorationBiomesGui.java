package RPG.Progression.gui;

import RPG.Progression.ProgressionManager;
import RPG.Progression.model.PlayerProgressionData;
import RPG.Progression.model.StageType;
import RPG.Progression.service.ExplorationService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class ExplorationBiomesGui implements InventoryHolder {

    private final Player player;
    private final PlayerProgressionData data;
    private final Inventory inventory;

    public ExplorationBiomesGui(Player player, PlayerProgressionData data) {
        this.player = player;
        this.data = data;
        this.inventory = Bukkit.createInventory(this, 54, ChatColor.DARK_GRAY + "✦ ATLAS ODKRYTYCH BIOMÓW ✦");
        buildGui();
    }

    private void buildGui() {
        inventory.clear();

        // Fill background
        ItemStack filler = ProgressionMenuGui.createItem(Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, filler);
        }

        // Header (Slot 4)
        StageType current = data.getCurrentStage();
        List<String> headerLore = Arrays.asList(
                "§7Atlas rejestruje każdy unikalny biom,",
                "§7do którego wkroczyłeś podczas swoich wypraw.",
                "",
                "§6Aktualny Etap: " + current.getDisplayName(),
                "§7Wszystkich odkrytych biomów: §a" + data.getDiscoveredBiomes().size()
        );
        inventory.setItem(4, ProgressionMenuGui.createItem(Material.MAP, "§3§lAtlas Biomów: §e" + player.getName(), headerLore));

        // Required biomes for current stage
        List<String> stageBiomes = ProgressionManager.getInstance().getStageService().getStageExplorationBiomes(current);
        int slot = 19;

        if (stageBiomes.isEmpty()) {
            inventory.setItem(22, ProgressionMenuGui.createItem(Material.COMPASS, "§7Brak specyficznych biomów w tym etapie", List.of("§8Eksploruj świat swobodnie!")));
        } else {
            for (String bName : stageBiomes) {
                if (slot > 34) break;
                boolean discovered = isBiomeDiscovered(bName);

                List<String> bLore = new ArrayList<>();
                bLore.add("§8Wymagany w etapie: " + current.getDisplayName());
                bLore.add("");
                if (discovered) {
                    bLore.add("§a§l✔ BIOM ODKRYTY");
                    bLore.add("§7Odwiedziłeś ten rejon!");
                } else {
                    bLore.add("§c§l✘ BIOM NIEODKRYTY");
                    bLore.add("§7Wyrusz na wyprawę, aby go odnaleźć.");
                }

                Material icon = discovered ? Material.LIME_DYE : Material.GRAY_DYE;
                inventory.setItem(slot++, ProgressionMenuGui.createItem(icon, (discovered ? "§a✔ " : "§c✘ ") + bName, bLore));
            }
        }

        // Back button (Slot 45)
        inventory.setItem(45, ProgressionMenuGui.createItem(Material.ARROW, "§e§l◀ Powrót", List.of("§7Kliknij, aby wrócić do głównego menu.")));
        // Close button (Slot 49)
        inventory.setItem(49, ProgressionMenuGui.createItem(Material.BARRIER, "§c§lZamknij", null));
    }

    private boolean isBiomeDiscovered(String searchName) {
        String clean = searchName.replace(" ", "_").toUpperCase(Locale.ROOT);
        for (String d : data.getDiscoveredBiomes()) {
            if (d.equalsIgnoreCase(clean) || d.contains(clean) || clean.contains(d)) {
                return true;
            }
        }
        return false;
    }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= 54) return;

        if (slot == 45) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            new ProgressionMenuGui(player, data).open();
            return;
        }

        if (slot == 49) {
            player.closeInventory();
            return;
        }
    }

    public void open() {
        player.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}

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
    private int page = 0;
    private static final int ITEMS_PER_PAGE = 18;

    public ExplorationBiomesGui(Player player, PlayerProgressionData data) {
        this(player, data, 0);
    }

    public ExplorationBiomesGui(Player player, PlayerProgressionData data, int page) {
        this.player = player;
        this.data = data;
        this.page = page;
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

        // 1. Wymagane biomy w aktualnym etapie (Sloty 10-16)
        List<String> stageBiomes = ProgressionManager.getInstance().getStageService().getStageExplorationBiomes(current);
        int reqSlot = 10;
        for (String bName : stageBiomes) {
            if (reqSlot > 16) break;
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
            inventory.setItem(reqSlot++, ProgressionMenuGui.createItem(icon, (discovered ? "§a✔ " : "§c✘ ") + bName, bLore));
        }

        // Divider row 3 (Sloty 18-26)
        ItemStack div = ProgressionMenuGui.createItem(Material.CYAN_STAINED_GLASS_PANE, "§3✦ Odkryte Krainy ✦", null);
        for (int i = 18; i < 27; i++) {
            inventory.setItem(i, div);
        }

        // 2. Pełna lista wszystkich odkrytych biomów gracza (Sloty 27-44 z paginacją)
        List<String> allDiscovered = new ArrayList<>(data.getDiscoveredBiomes());
        Collections.sort(allDiscovered);

        int totalPages = Math.max(1, (int) Math.ceil((double) allDiscovered.size() / ITEMS_PER_PAGE));
        if (page >= totalPages) page = totalPages - 1;
        if (page < 0) page = 0;

        int startIndex = page * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, allDiscovered.size());

        int discSlot = 27;
        for (int i = startIndex; i < endIndex; i++) {
            String bKey = allDiscovered.get(i);
            String formatted = ExplorationService.formatBiomeName(bKey);
            Material icon = getBiomeIcon(bKey);

            List<String> lore = List.of(
                    "§8Klucz biomu: " + bKey,
                    "",
                    "§a✔ Wpis w Atlasie Odkrywcy",
                    "§7Odwiedzony podczas przygód na serwerze."
            );
            inventory.setItem(discSlot++, ProgressionMenuGui.createItem(icon, "§b✦ " + formatted, lore));
        }

        // Pagination buttons
        if (page > 0) {
            inventory.setItem(48, ProgressionMenuGui.createItem(Material.ARROW, "§e◀ Poprzednia strona (" + page + "/" + totalPages + ")", null));
        }
        if (page < totalPages - 1) {
            inventory.setItem(50, ProgressionMenuGui.createItem(Material.ARROW, "§eNastępna strona ▶ (" + (page + 2) + "/" + totalPages + ")", null));
        }

        // Back button (Slot 45)
        inventory.setItem(45, ProgressionMenuGui.createItem(Material.ARROW, "§e§l◀ Powrót", List.of("§7Kliknij, aby wrócić do głównego menu.")));
        // Close button (Slot 49)
        inventory.setItem(49, ProgressionMenuGui.createItem(Material.BARRIER, "§c§lZamknij", null));
    }

    private Material getBiomeIcon(String biomeKey) {
        String k = biomeKey.toUpperCase(Locale.ROOT);
        if (k.contains("BIRCH")) return Material.BIRCH_SAPLING;
        if (k.contains("SPRUCE") || k.contains("TAIGA") || k.contains("PINE")) return Material.SPRUCE_SAPLING;
        if (k.contains("JUNGLE")) return Material.JUNGLE_SAPLING;
        if (k.contains("ACACIA") || k.contains("SAVANNA")) return Material.ACACIA_SAPLING;
        if (k.contains("DARK_FOREST") || k.contains("ROOFED")) return Material.DARK_OAK_SAPLING;
        if (k.contains("CHERRY")) return Material.CHERRY_SAPLING;
        if (k.contains("SWAMP") || k.contains("MANGROVE")) return Material.MANGROVE_PROPAGULE;
        if (k.contains("DESERT") || k.contains("BADLANDS") || k.contains("MESA")) return Material.DEAD_BUSH;
        if (k.contains("OCEAN") || k.contains("RIVER") || k.contains("BEACH")) return Material.WATER_BUCKET;
        if (k.contains("PEAKS") || k.contains("MOUNTAIN") || k.contains("SLOPES") || k.contains("HILLS")) return Material.POWDER_SNOW_BUCKET;
        if (k.contains("MEADOW") || k.contains("PLAINS") || k.contains("SUNFLOWER")) return Material.SUNFLOWER;
        if (k.contains("NETHER") || k.contains("CRIMSON") || k.contains("WARPED") || k.contains("BASALT") || k.contains("SOUL")) return Material.CRIMSON_FUNGUS;
        if (k.contains("END")) return Material.ENDER_PEARL;
        return Material.GRASS_BLOCK;
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

        if (slot == 48 && page > 0) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            new ExplorationBiomesGui(player, data, page - 1).open();
            return;
        }

        if (slot == 50) {
            List<String> allDiscovered = new ArrayList<>(data.getDiscoveredBiomes());
            int totalPages = Math.max(1, (int) Math.ceil((double) allDiscovered.size() / ITEMS_PER_PAGE));
            if (page < totalPages - 1) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                new ExplorationBiomesGui(player, data, page + 1).open();
                return;
            }
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

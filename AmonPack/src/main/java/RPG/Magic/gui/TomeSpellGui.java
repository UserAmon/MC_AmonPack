package RPG.Magic.gui;

import RPG.Magic.manager.MagicItemManager;
import RPG.Magic.manager.ManaManager;
import RPG.Magic.manager.SpellRegistry;
import RPG.Magic.model.Spell;
import RPG.Progression.gui.ProgressionMenuGui;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class TomeSpellGui implements InventoryHolder {

    private final Player player;
    private final ItemStack tomeItem;
    private final SpellRegistry spellRegistry;
    private final ManaManager manaManager;
    private final Inventory inventory;

    public TomeSpellGui(Player player, ItemStack tomeItem, SpellRegistry spellRegistry, ManaManager manaManager) {
        this.player = player;
        this.tomeItem = tomeItem;
        this.spellRegistry = spellRegistry;
        this.manaManager = manaManager;
        this.inventory = Bukkit.createInventory(this, 45, ChatColor.DARK_RED + "✦ TOM OGNIA: ZAKLĘCIA ✦");
        buildGui();
    }

    private void buildGui() {
        inventory.clear();

        ItemStack filler = ProgressionMenuGui.createItem(Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 45; i++) {
            inventory.setItem(i, filler);
        }

        int level = MagicItemManager.getTomeLevel(tomeItem);
        int kills = MagicItemManager.getTomeKills(tomeItem);
        String primaryId = MagicItemManager.getPrimarySpellId(tomeItem);
        String secondaryId = MagicItemManager.getSecondarySpellId(tomeItem);
        Set<String> upgrades = MagicItemManager.getUpgrades(tomeItem);

        Spell primary = spellRegistry.getSpell(primaryId);
        Spell secondary = spellRegistry.getSpell(secondaryId);

        // --- RZĄD 1 (Slot 4: Info Tomu) ---
        List<String> tomeLore = new ArrayList<>();
        tomeLore.add("§7Starożytna księga zawierająca pierwotne zaklęcia.");
        tomeLore.add("");
        tomeLore.add("§6✦ Poziom Tomu: §e" + level + " §7(Ulepsz w §5Ołtarzu Arkanów§7)");
        tomeLore.add("§c⚔ Zabójstwa Magią: §f" + kills);
        tomeLore.add("§b✦ Aktualna Mana: §f" + manaManager.getMana(player) + "/" + manaManager.getMaxMana(player) + " MP");
        tomeLore.add("§e✦ Aktywne Ulepszenia: §f" + (upgrades.isEmpty() ? "§7Brak" : upgrades.size()));
        inventory.setItem(4, ProgressionMenuGui.createItem(Material.WRITABLE_BOOK, "§c§l✦ TOM OGNIA ✦", tomeLore));

        // --- RZĄD 2: PUSTY (pozostają panele) ---

        // --- RZĄD 3: TYLKO DWIE RZECZY (Slot 20: LPM, Slot 24: Shift+LPM) ---
        // Slot 20: Główne Zaklęcie (LPM)
        List<String> primaryLore = new ArrayList<>();
        if (primary != null) {
            primaryLore.add("§8Przypisane Zaklęcie:");
            primaryLore.add("§c§l" + primary.getName());
            primaryLore.add("§b✦ Koszt: §f" + primary.getManaCost() + " MP §8| §eOdnowienie: §f" + String.format("%.1f", primary.getCooldownSeconds()) + "s");
            primaryLore.add("");
            primaryLore.add("§e✦ Kliknij, aby zmienić zaklęcie LPM!");
        } else {
            primaryLore.add("§7Brak przypisanego zaklęcia.");
            primaryLore.add("§e✦ Kliknij, aby wybrać zaklęcie!");
        }
        inventory.setItem(20, ProgressionMenuGui.createItem(Material.FIRE_CHARGE, "§c§l[ ✦ GŁÓWNE ZAKLĘCIE (LPM) ]", primaryLore));

        // Slot 24: Drugi Krąg (Shift+LPM)
        List<String> secondaryLore = new ArrayList<>();
        if (secondary != null && !secondaryId.equalsIgnoreCase("none")) {
            secondaryLore.add("§8Przypisane Zaklęcie:");
            secondaryLore.add("§6§l" + secondary.getName());
            secondaryLore.add("§b✦ Koszt: §f" + secondary.getManaCost() + " MP §8| §eOdnowienie: §f" + String.format("%.1f", secondary.getCooldownSeconds()) + "s");
            secondaryLore.add("");
            secondaryLore.add("§e✦ Kliknij, aby zmienić zaklęcie Drugiego Kręgu!");
        } else {
            secondaryLore.add("§7Brak przypisanego zaklęcia Drugiego Kręgu.");
            secondaryLore.add("§e✦ Kliknij, aby wybrać zaklęcie Shift+LPM!");
        }
        inventory.setItem(24, ProgressionMenuGui.createItem(Material.MAGMA_CREAM, "§6§l[ ✧ DRUGI KRĄG (SHIFT+LPM) ]", secondaryLore));

        // --- RZĄD 4: PUSTY (pozostają panele) ---

        // --- RZĄD 5: Zamknięcie (Slot 40) ---
        inventory.setItem(40, ProgressionMenuGui.createItem(Material.BARRIER, "§c§lZamknij", List.of("§7Kliknij, aby zamknąć menu.")));
    }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();

        if (slot == 40) {
            player.closeInventory();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.0f);
            return;
        }

        if (slot == 20) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
            player.openInventory(new PrimarySpellSelectGui(player, tomeItem, spellRegistry, manaManager).getInventory());
        } else if (slot == 24) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
            player.openInventory(new SecondarySpellSelectGui(player, tomeItem, spellRegistry, manaManager).getInventory());
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}

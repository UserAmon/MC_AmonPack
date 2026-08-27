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
        boolean isAir = MagicItemManager.isAirWand(tomeItem);
        this.inventory = Bukkit.createInventory(this, 45, isAir ? ChatColor.DARK_AQUA + "✦ RÓŻDŻKA FENÓW: ZAKLĘCIA ✦" : ChatColor.DARK_RED + "✦ TOM OGNIA: ZAKLĘCIA ✦");
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
        boolean isAir = MagicItemManager.isAirWand(tomeItem);
        boolean isWater = MagicItemManager.isWaterWand(tomeItem);
        boolean isStaff = MagicItemManager.isMagicStaff(tomeItem);

        Spell primary = spellRegistry.getSpell(primaryId);
        Spell secondary = spellRegistry.getSpell(secondaryId);

        // --- RZĄD 1 (Slot 4: Info Przedmiotu) ---
        List<String> tomeLore = new ArrayList<>();
        if (isStaff) {
            tomeLore.add("§7Mistyczna laska przewodząca pioruny i energię burzy.");
            tomeLore.add("");
            tomeLore.add("§6✦ Poziom Laski: §e" + level + " §7(Ulepsz w §5Ołtarzu Arkanów§7)");
            tomeLore.add("§c⚔ Zabójstwa Magią: §f" + kills);
            tomeLore.add("§b✦ Aktualna Mana: §f" + (int) manaManager.getMana(player) + "/" + (int) manaManager.getMaxMana(player) + " MP");
            tomeLore.add("§e✦ Aktywne Ulepszenia: §f" + (upgrades.isEmpty() ? "§7Brak" : upgrades.size()));
            inventory.setItem(4, ProgressionMenuGui.createItem(Material.LIGHTNING_ROD, "§e§l✦ LASKA BŁYSKAWIC ✦", tomeLore));
        } else if (isWater) {
            tomeLore.add("§7Różdżka ukształtowana z czystego oceanicznego kryształu.");
            tomeLore.add("");
            tomeLore.add("§6✦ Poziom Różdżki: §e" + level + " §7(Ulepsz w §5Ołtarzu Arkanów§7)");
            tomeLore.add("§c⚔ Zabójstwa Magią: §f" + kills);
            tomeLore.add("§b✦ Aktualna Mana: §f" + (int) manaManager.getMana(player) + "/" + (int) manaManager.getMaxMana(player) + " MP");
            tomeLore.add("§e✦ Aktywne Ulepszenia: §f" + (upgrades.isEmpty() ? "§7Brak" : upgrades.size()));
            inventory.setItem(4, ProgressionMenuGui.createItem(Material.PRISMARINE_SHARD, "§9§l✦ RÓŻDŻKA WODY ✦", tomeLore));
        } else if (isAir) {
            tomeLore.add("§7Mistyczna różdżka wiatru wykuta z esencji fenów.");
            tomeLore.add("");
            tomeLore.add("§6✦ Poziom Różdżki: §e" + level + " §7(Ulepsz w §5Ołtarzu Arkanów§7)");
            tomeLore.add("§c⚔ Zabójstwa Magią: §f" + kills);
            tomeLore.add("§b✦ Aktualna Mana: §f" + (int) manaManager.getMana(player) + "/" + (int) manaManager.getMaxMana(player) + " MP");
            tomeLore.add("§e✦ Aktywne Ulepszenia: §f" + (upgrades.isEmpty() ? "§7Brak" : upgrades.size()));
            inventory.setItem(4, ProgressionMenuGui.createItem(Material.FEATHER, "§b§l✦ RÓŻDŻKA FENÓW ✦", tomeLore));
        } else {
            tomeLore.add("§7Starożytna księga zawierająca pierwotne zaklęcia.");
            tomeLore.add("");
            tomeLore.add("§6✦ Poziom Tomu: §e" + level + " §7(Ulepsz w §5Ołtarzu Arkanów§7)");
            tomeLore.add("§c⚔ Zabójstwa Magią: §f" + kills);
            tomeLore.add("§b✦ Aktualna Mana: §f" + (int) manaManager.getMana(player) + "/" + (int) manaManager.getMaxMana(player) + " MP");
            tomeLore.add("§e✦ Aktywne Ulepszenia: §f" + (upgrades.isEmpty() ? "§7Brak" : upgrades.size()));
            inventory.setItem(4, ProgressionMenuGui.createItem(Material.WRITABLE_BOOK, "§c§l✦ TOM OGNIA ✦", tomeLore));
        }

        // --- RZĄD 2: PUSTY ---

        // --- RZĄD 3: TYLKO SLOTY ZAKLĘĆ ---
        if (isStaff || isAir || isWater) {
            // Różdżki i Laski mają tylko 1 slot na zaklęcie (Slot 22 - Środek)
            List<String> slotLore = new ArrayList<>();
            if (primary != null) {
                slotLore.add("§8Przypisane Zaklęcie:");
                slotLore.add("§b§l" + primary.getName());
                slotLore.add("§b✦ Koszt: §f" + primary.getManaCost() + " MP §8| §eOdnowienie: §f" + String.format("%.1f", primary.getCooldownSeconds()) + "s");
                slotLore.add("");
                slotLore.add("§e✦ Kliknij, aby zmienić zaklęcie!");
            } else {
                slotLore.add("§7Brak przypisanego zaklęcia.");
                slotLore.add("§e✦ Kliknij, aby wybrać zaklęcie!");
            }
            Material iconMat = isStaff ? Material.GLOWSTONE_DUST : (isWater ? Material.PRISMARINE_CRYSTALS : Material.FEATHER);
            String title = isStaff ? "§e§l[ ✦ ZAKLĘCIE ŁADOWANE (PPM) ]" : "§b§l[ ✦ PRZYPISANE ZAKLĘCIE ]";
            inventory.setItem(22, ProgressionMenuGui.createItem(iconMat, title, slotLore));
        } else {
            // Tomy mają 2 sloty: Slot 20 (LPM) oraz Slot 24 (Shift)
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

            List<String> secondaryLore = new ArrayList<>();
            if (secondary != null && !secondaryId.equalsIgnoreCase("none")) {
                secondaryLore.add("§8Przypisane Zaklęcie:");
                secondaryLore.add("§6§l" + secondary.getName());
                secondaryLore.add("§b✦ Koszt: §f" + secondary.getManaCost() + " MP §8| §eOdnowienie: §f" + String.format("%.1f", secondary.getCooldownSeconds()) + "s");
                secondaryLore.add("");
                secondaryLore.add("§e✦ Kliknij, aby zmienić tkany czar (Shift)!");
            } else {
                secondaryLore.add("§7Brak przypisanego tkanego czaru.");
                secondaryLore.add("§e✦ Kliknij, aby wybrać czar tkany Shiftem!");
            }
            inventory.setItem(24, ProgressionMenuGui.createItem(Material.MAGMA_CREAM, "§6§l[ ✧ TKANY CZAR (SHIFT) ]", secondaryLore));
        }

        // --- RZĄD 4: PUSTY ---

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

        boolean isStaff = MagicItemManager.isMagicStaff(tomeItem);
        boolean isWand = MagicItemManager.isMagicWand(tomeItem);

        if (isStaff || isWand) {
            if (slot == 22) {
                player.openInventory(new PrimarySpellSelectGui(player, tomeItem, spellRegistry, manaManager).getInventory());
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.0f);
            }
            return;
        }

        if (slot == 20) {
            player.openInventory(new PrimarySpellSelectGui(player, tomeItem, spellRegistry, manaManager).getInventory());
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
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

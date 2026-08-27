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

public class SecondarySpellSelectGui implements InventoryHolder {

    private final Player player;
    private final ItemStack tomeItem;
    private final SpellRegistry spellRegistry;
    private final ManaManager manaManager;
    private final Inventory inventory;

    public SecondarySpellSelectGui(Player player, ItemStack tomeItem, SpellRegistry spellRegistry, ManaManager manaManager) {
        this.player = player;
        this.tomeItem = tomeItem;
        this.spellRegistry = spellRegistry;
        this.manaManager = manaManager;
        this.inventory = Bukkit.createInventory(this, 27, ChatColor.GOLD + "✧ WYBÓR CZARU: SHIFT+LPM ✧");
        buildGui();
    }

    private void buildGui() {
        inventory.clear();
        ItemStack filler = ProgressionMenuGui.createItem(Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, filler);
        }

        int tomeLevel = MagicItemManager.getTomeLevel(tomeItem);
        String currentSecondary = MagicItemManager.getSecondarySpellId(tomeItem);
        boolean isAir = MagicItemManager.isAirWand(tomeItem);

        if (isAir) {
            // 1. Wir Powietrza (Level 2+) -> Slot 13
            inventory.setItem(13, createSpellItem("air_vortex", 2, tomeLevel, currentSecondary.equalsIgnoreCase("air_vortex"), Material.ELYTRA));
        } else {
            // 1. Fire Circle (Level 2+) -> Slot 12
            inventory.setItem(12, createSpellItem("fire_circle", 2, tomeLevel, currentSecondary.equalsIgnoreCase("fire_circle"), Material.MAGMA_CREAM));
            // 2. Barrage (Level 3+) -> Slot 14
            inventory.setItem(14, createSpellItem("barrage", 3, tomeLevel, currentSecondary.equalsIgnoreCase("barrage"), Material.BLAZE_ROD));
        }

        // Powrót -> Slot 18
        inventory.setItem(18, ProgressionMenuGui.createItem(Material.ARROW, "§e◀ Powrót do menu", List.of("§7Kliknij, aby wrócić.")));
    }

    private ItemStack createSpellItem(String spellId, int requiredLevel, int currentLevel, boolean isAssigned, Material activeMat) {
        Spell spell = spellRegistry.getSpell(spellId);
        if (spell == null) return ProgressionMenuGui.createItem(Material.BARRIER, "§cBrak zaklęcia", null);

        boolean unlocked = currentLevel >= requiredLevel;
        Material mat = unlocked ? activeMat : Material.GRAY_DYE;
        String name = (unlocked ? "§b§l" : "§7§l") + spell.getName();

        List<String> lore = new ArrayList<>();
        lore.add("§8Żywioł: §f" + spell.getElement().name());
        lore.add("§b✦ Koszt: §f" + spell.getManaCost() + " MP §8| §eOdnowienie: §f" + String.format("%.1f", spell.getCooldownSeconds()) + "s");
        lore.add("§8Wymagany Poziom Przedmiotu: §e" + requiredLevel);
        lore.add("");

        if (!unlocked) {
            lore.add("§c🔒 Zablokowane!");
            lore.add("§7Wymaga Poziomu §e" + requiredLevel + "§7.");
            lore.add("§7Ulepsz przedmiot w §5Ołtarzu Arkanów§7!");
        } else if (isAssigned) {
            lore.add("§a✔ Aktualnie przypisane do Drugiego Kręgu");
        } else {
            lore.add("§e✦ Kliknij, aby przypisać do Shift+LPM!");
        }

        return ProgressionMenuGui.createItem(mat, name, lore);
    }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();

        if (slot == 18) {
            player.openInventory(new TomeSpellGui(player, tomeItem, spellRegistry, manaManager).getInventory());
            return;
        }

        int tomeLevel = MagicItemManager.getTomeLevel(tomeItem);
        boolean isAir = MagicItemManager.isAirWand(tomeItem);

        if (isAir) {
            if (slot == 13) {
                if (tomeLevel >= 2) {
                    MagicItemManager.setSecondarySpellId(tomeItem, "air_vortex");
                    player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_GENERIC, 1.0f, 1.2f);
                    player.sendMessage("§aPrzypisano zaklęcie §bWir Powietrza §ado Drugiego Kręgu (Shift+LPM)!");
                    player.openInventory(new TomeSpellGui(player, tomeItem, spellRegistry, manaManager).getInventory());
                } else {
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    player.sendMessage("§cTo zaklęcie wymaga Poziomu II!");
                }
            }
            return;
        }

        if (slot == 12) {
            if (tomeLevel >= 2) {
                MagicItemManager.setSecondarySpellId(tomeItem, "fire_circle");
                player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_GENERIC, 1.0f, 1.2f);
                player.sendMessage("§aPrzypisano zaklęcie §6Fire Circle §ado Drugiego Kręgu (Shift+LPM)!");
                player.openInventory(new TomeSpellGui(player, tomeItem, spellRegistry, manaManager).getInventory());
            } else {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                player.sendMessage("§cTo zaklęcie wymaga Tomu na Poziomie II!");
            }
        } else if (slot == 14) {
            if (tomeLevel >= 3) {
                MagicItemManager.setSecondarySpellId(tomeItem, "barrage");
                player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_GENERIC, 1.0f, 1.2f);
                player.sendMessage("§aPrzypisano zaklęcie §cBarrage §ado Drugiego Kręgu (Shift+LPM)!");
                player.openInventory(new TomeSpellGui(player, tomeItem, spellRegistry, manaManager).getInventory());
            } else {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                player.sendMessage("§cTo zaklęcie wymaga Tomu na Poziomie III!");
            }
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}

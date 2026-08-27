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

public class PrimarySpellSelectGui implements InventoryHolder {

    private final Player player;
    private final ItemStack tomeItem;
    private final SpellRegistry spellRegistry;
    private final ManaManager manaManager;
    private final Inventory inventory;

    public PrimarySpellSelectGui(Player player, ItemStack tomeItem, SpellRegistry spellRegistry, ManaManager manaManager) {
        this.player = player;
        this.tomeItem = tomeItem;
        this.spellRegistry = spellRegistry;
        this.manaManager = manaManager;
        this.inventory = Bukkit.createInventory(this, 27, ChatColor.RED + "✦ WYBÓR CZARU: LPM ✦");
        buildGui();
    }

    private void buildGui() {
        inventory.clear();
        ItemStack filler = ProgressionMenuGui.createItem(Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, filler);
        }

        int tomeLevel = MagicItemManager.getTomeLevel(tomeItem);
        String currentPrimary = MagicItemManager.getPrimarySpellId(tomeItem);

        // 1. Fire Blast (Level 1+) -> Slot 11
        inventory.setItem(11, createSpellItem("fireblast", 1, tomeLevel, currentPrimary.equalsIgnoreCase("fireblast"), Material.FIRE_CHARGE));

        // 2. Blazing (Level 2+) -> Slot 13
        inventory.setItem(13, createSpellItem("blazing", 2, tomeLevel, currentPrimary.equalsIgnoreCase("blazing"), Material.BLAZE_POWDER));

        // 3. FlashPoint (Level 3+) -> Slot 15
        inventory.setItem(15, createSpellItem("flashpoint", 3, tomeLevel, currentPrimary.equalsIgnoreCase("flashpoint"), Material.LAVA_BUCKET));

        // Powrót -> Slot 18
        inventory.setItem(18, ProgressionMenuGui.createItem(Material.ARROW, "§e◀ Powrót do menu tomu", List.of("§7Kliknij, aby wrócić.")));
    }

    private ItemStack createSpellItem(String spellId, int requiredLevel, int currentLevel, boolean isAssigned, Material activeMat) {
        Spell spell = spellRegistry.getSpell(spellId);
        if (spell == null) return ProgressionMenuGui.createItem(Material.BARRIER, "§cBrak zaklęcia", null);

        boolean unlocked = currentLevel >= requiredLevel;
        Material mat = unlocked ? activeMat : Material.GRAY_DYE;
        String name = (unlocked ? "§c§l" : "§7§l") + spell.getName();

        List<String> lore = new ArrayList<>();
        lore.add("§8Żywioł: §c" + spell.getElement().name());
        lore.add("§b✦ Koszt: §f" + spell.getManaCost() + " MP §8| §eOdnowienie: §f" + String.format("%.1f", spell.getCooldownSeconds()) + "s");
        lore.add("§8Wymagany Poziom Tomu: §e" + requiredLevel);
        lore.add("");

        if (!unlocked) {
            lore.add("§c🔒 Zablokowane!");
            lore.add("§7Wymaga Tomu na Poziomie §e" + requiredLevel + "§7.");
            lore.add("§7Ulepsz tom w §5Ołtarzu Arkanów§7!");
        } else if (isAssigned) {
            lore.add("§a✔ Aktualnie przypisane do LPM");
        } else {
            lore.add("§e✦ Kliknij, aby przypisać do LPM!");
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

        if (slot == 11) {
            MagicItemManager.setPrimarySpellId(tomeItem, "fireblast");
            player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_GENERIC, 1.0f, 1.2f);
            player.sendMessage("§aPrzypisano zaklęcie §cFire Blast §ado LPM!");
            player.openInventory(new TomeSpellGui(player, tomeItem, spellRegistry, manaManager).getInventory());
        } else if (slot == 13) {
            if (tomeLevel >= 2) {
                MagicItemManager.setPrimarySpellId(tomeItem, "blazing");
                player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_GENERIC, 1.0f, 1.2f);
                player.sendMessage("§aPrzypisano zaklęcie §cBlazing §ado LPM!");
                player.openInventory(new TomeSpellGui(player, tomeItem, spellRegistry, manaManager).getInventory());
            } else {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                player.sendMessage("§cTo zaklęcie wymaga Tomu na Poziomie II!");
            }
        } else if (slot == 15) {
            if (tomeLevel >= 3) {
                MagicItemManager.setPrimarySpellId(tomeItem, "flashpoint");
                player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_GENERIC, 1.0f, 1.2f);
                player.sendMessage("§aPrzypisano zaklęcie §cFlashPoint §ado LPM!");
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

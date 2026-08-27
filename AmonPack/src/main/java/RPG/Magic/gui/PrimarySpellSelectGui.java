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
        boolean isAir = MagicItemManager.isAirWand(tomeItem);
        boolean isWater = MagicItemManager.isWaterWand(tomeItem);
        boolean isStaff = MagicItemManager.isMagicStaff(tomeItem);

        if (isStaff) {
            // Laska Błyskawic
            inventory.setItem(11, createSpellItem("lightning", 1, tomeLevel, currentPrimary.equalsIgnoreCase("lightning"), Material.GLOWSTONE_DUST));
            inventory.setItem(15, createSpellItem("chain_lightning", 2, tomeLevel, currentPrimary.equalsIgnoreCase("chain_lightning"), Material.NETHER_STAR));
        } else if (isWater) {
            // Różdżka Wody
            inventory.setItem(11, createSpellItem("splash", 1, tomeLevel, currentPrimary.equalsIgnoreCase("splash"), Material.PRISMARINE_CRYSTALS));
            inventory.setItem(13, createSpellItem("freeze", 2, tomeLevel, currentPrimary.equalsIgnoreCase("freeze"), Material.PACKED_ICE));
            inventory.setItem(15, createSpellItem("evaporate", 3, tomeLevel, currentPrimary.equalsIgnoreCase("evaporate"), Material.GLASS_BOTTLE));
        } else if (isAir) {
            // Różdżka Fenów
            inventory.setItem(11, createSpellItem("blow", 1, tomeLevel, currentPrimary.equalsIgnoreCase("blow"), Material.FEATHER));
            inventory.setItem(13, createSpellItem("airblade", 2, tomeLevel, currentPrimary.equalsIgnoreCase("airblade"), Material.IRON_SWORD));
            inventory.setItem(15, createSpellItem("air_vortex", 2, tomeLevel, currentPrimary.equalsIgnoreCase("air_vortex"), Material.ELYTRA));
        } else {
            // Tom Ognia
            inventory.setItem(11, createSpellItem("fireblast", 1, tomeLevel, currentPrimary.equalsIgnoreCase("fireblast"), Material.FIRE_CHARGE));
            inventory.setItem(13, createSpellItem("blazing", 2, tomeLevel, currentPrimary.equalsIgnoreCase("blazing"), Material.BLAZE_POWDER));
            inventory.setItem(15, createSpellItem("flashpoint", 3, tomeLevel, currentPrimary.equalsIgnoreCase("flashpoint"), Material.LAVA_BUCKET));
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
            lore.add("§a✔ Aktualnie przypisane do użycia");
        } else {
            lore.add("§e✦ Kliknij, aby przypisać to zaklęcie!");
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
        boolean isWater = MagicItemManager.isWaterWand(tomeItem);
        boolean isStaff = MagicItemManager.isMagicStaff(tomeItem);

        String chosenSpell = null;
        int reqLevel = 1;

        if (isStaff) {
            if (slot == 11) { chosenSpell = "lightning"; reqLevel = 1; }
            else if (slot == 15) { chosenSpell = "chain_lightning"; reqLevel = 2; }
        } else if (isWater) {
            if (slot == 11) { chosenSpell = "splash"; reqLevel = 1; }
            else if (slot == 13) { chosenSpell = "freeze"; reqLevel = 2; }
            else if (slot == 15) { chosenSpell = "evaporate"; reqLevel = 3; }
        } else if (isAir) {
            if (slot == 11) { chosenSpell = "blow"; reqLevel = 1; }
            else if (slot == 13) { chosenSpell = "airblade"; reqLevel = 2; }
            else if (slot == 15) { chosenSpell = "air_vortex"; reqLevel = 2; }
        } else {
            if (slot == 11) { chosenSpell = "fireblast"; reqLevel = 1; }
            else if (slot == 13) { chosenSpell = "blazing"; reqLevel = 2; }
            else if (slot == 15) { chosenSpell = "flashpoint"; reqLevel = 3; }
        }

        if (chosenSpell != null) {
            if (tomeLevel >= reqLevel) {
                MagicItemManager.setPrimarySpellId(tomeItem, chosenSpell);
                player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_GENERIC, 1.0f, 1.2f);
                Spell sp = spellRegistry.getSpell(chosenSpell);
                String spName = sp != null ? sp.getName() : chosenSpell;
                player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, 
                        net.md_5.bungee.api.chat.TextComponent.fromLegacyText("§a✔ Przypisano zaklęcie: §f" + spName));
                player.openInventory(new TomeSpellGui(player, tomeItem, spellRegistry, manaManager).getInventory());
            } else {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, 
                        net.md_5.bungee.api.chat.TextComponent.fromLegacyText("§c🔒 To zaklęcie wymaga Poziomu " + reqLevel + "!"));
            }
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpellSelectUpgradeGui implements InventoryHolder {

    private final Player player;
    private final ItemStack mainHandItem;
    private final SpellRegistry spellRegistry;
    private final ManaManager manaManager;
    private final Inventory inventory;
    private final Map<Integer, String> slotSpellMap = new HashMap<>();

    public SpellSelectUpgradeGui(Player player, ItemStack mainHandItem, SpellRegistry spellRegistry, ManaManager manaManager) {
        this.player = player;
        this.mainHandItem = mainHandItem;
        this.spellRegistry = spellRegistry;
        this.manaManager = manaManager;
        this.inventory = Bukkit.createInventory(this, 27, ChatColor.DARK_PURPLE + "✦ WYBIERZ CZAR DO ULEPSZENIA ✦");
        buildGui();
    }

    private void buildGui() {
        inventory.clear();
        slotSpellMap.clear();

        ItemStack filler = ProgressionMenuGui.createItem(Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, filler);
        }

        String itemId = MagicItemManager.getMagicItemId(mainHandItem);
        if (itemId == null) itemId = "tome_fire";
        int itemLevel = MagicItemManager.getTomeLevel(mainHandItem);

        switch (itemId) {
            case "wand_fen" -> {
                addSpellSlot(11, "blow", Material.FEATHER, "§f§lZaklęcie: Blow", 1, itemLevel);
                addSpellSlot(13, "airblade", Material.IRON_SWORD, "§b§lZaklęcie: Airblade", 2, itemLevel);
                addSpellSlot(15, "air_vortex", Material.ELYTRA, "§3§lZaklęcie: Wir Powietrza", 2, itemLevel);
            }
            case "staff_lightning" -> {
                addSpellSlot(12, "lightning", Material.LIGHTNING_ROD, "§e§lZaklęcie: Lightning", 1, itemLevel);
                addSpellSlot(14, "chain_lightning", Material.COPPER_INGOT, "§6§lZaklęcie: Chain Lightning", 2, itemLevel);
            }
            case "wand_water" -> {
                addSpellSlot(11, "splash", Material.PRISMARINE_SHARD, "§b§lZaklęcie: Splash", 1, itemLevel);
                addSpellSlot(13, "freeze", Material.ICE, "§9§lZaklęcie: Freeze", 2, itemLevel);
                addSpellSlot(15, "evaporate", Material.WATER_BUCKET, "§3§lZaklęcie: Evaporate", 3, itemLevel);
            }
            default -> {
                // Tom Ognia i domyślne tomy
                addSpellSlot(10, "fireblast", Material.FIRE_CHARGE, "§c§lZaklęcie: Fire Blast", 1, itemLevel);
                addSpellSlot(12, "blazing", Material.BLAZE_POWDER, "§6§lZaklęcie: Blazing", 2, itemLevel);
                addSpellSlot(14, "flashpoint", Material.LAVA_BUCKET, "§4§lZaklęcie: FlashPoint", 3, itemLevel);
                addSpellSlot(16, "fire_circle", Material.MAGMA_CREAM, "§e§lZaklęcie: Fire Circle", 2, itemLevel);
                addSpellSlot(22, "barrage", Material.BLAZE_ROD, "§d§lZaklęcie: Barrage", 3, itemLevel);
            }
        }

        // Slot 18: Powrót do Ołtarza
        inventory.setItem(18, ProgressionMenuGui.createItem(Material.ARROW, "§e◀ Powrót do Ołtarza Arkanów", List.of("§7Kliknij, aby wrócić do głównego menu ołtarza.")));
    }

    private void addSpellSlot(int slot, String spellId, Material mat, String name, int reqLevel, int curLevel) {
        Spell spell = spellRegistry.getSpell(spellId);
        List<String> lore = new ArrayList<>();
        if (spell != null) {
            lore.add("§7" + spell.getDescription());
            lore.add("");
            lore.add("§b✦ Koszt: §f" + (int)spell.getManaCost() + " MP §8| §eCooldown: §f" + spell.getCooldownSeconds() + "s");
        }

        if (curLevel >= reqLevel) {
            lore.add("");
            lore.add("§a✔ Zaklęcie odblokowane!");
            lore.add("§e✦ Kliknij, aby otworzyć drzewko ulepszeń tego czaru!");
            slotSpellMap.put(slot, spellId);
            inventory.setItem(slot, ProgressionMenuGui.createItem(mat, name, lore));
        } else {
            lore.add("");
            lore.add("§c✖ Zablokowane!");
            lore.add("§cWymaga " + (MagicItemManager.isAirWand(mainHandItem) ? "Różdżki Fenów" : "Tomu") + " Poziomu " + reqLevel + "!");
            inventory.setItem(slot, ProgressionMenuGui.createItem(Material.GRAY_DYE, "§8" + ChatColor.stripColor(name) + " §c(Zablokowane)", lore));
        }
    }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();

        if (slot == 18) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            player.openInventory(new ArcaneAltarGui(player, mainHandItem, spellRegistry, manaManager).getInventory());
            return;
        }

        String spellId = slotSpellMap.get(slot);
        if (spellId != null) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
            player.openInventory(new SingleSpellUpgradeGui(player, mainHandItem, spellId, spellRegistry, manaManager).getInventory());
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}

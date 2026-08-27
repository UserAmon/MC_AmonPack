package RPG.Magic.gui;

import RPG.Magic.manager.MagicItemManager;
import RPG.Magic.manager.ManaManager;
import RPG.Magic.manager.SpellRegistry;
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

public class SpellUpgradeTreeGui implements InventoryHolder {

    private final Player player;
    private final ItemStack tomeItem;
    private final SpellRegistry spellRegistry;
    private final ManaManager manaManager;
    private final Inventory inventory;

    public SpellUpgradeTreeGui(Player player, ItemStack tomeItem, SpellRegistry spellRegistry, ManaManager manaManager) {
        this.player = player;
        this.tomeItem = tomeItem;
        this.spellRegistry = spellRegistry;
        this.manaManager = manaManager;
        this.inventory = Bukkit.createInventory(this, 27, ChatColor.DARK_PURPLE + "✦ DRZEWKO ROZWOJU MAGII ✦");
        buildGui();
    }

    private void buildGui() {
        inventory.clear();
        ItemStack filler = ProgressionMenuGui.createItem(Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, filler);
        }

        // Slot 10: Efektywność Many (-10 MP) [Koszt: 5 EXP]
        inventory.setItem(10, createUpgradeItem("mana_reduction", "§b§lEfektywność Many", Material.POTION,
                "Zmniejsza zużycie many wszystkich czarów o 10 MP.", 5));

        // Slot 12: Szybki Rzut (-1.0s CD) [Koszt: 5 EXP]
        inventory.setItem(12, createUpgradeItem("cooldown_reduction", "§e§lSzybki Rzut", Material.CLOCK,
                "Skraca czas odnowienia (cooldown) wszystkich czarów o 1.0s.", 5));

        // Slot 14: Wielokrotny Pocisk (Multi-Cast) [Koszt: 8 EXP]
        inventory.setItem(14, createUpgradeItem("multi_cast", "§6§lWielokrotny Pocisk", Material.FIREWORK_STAR,
                "Fire Blast wystrzeliwuje 2 dodatkowe pociski w rozrzucie.", 8));

        // Slot 16: Ognisty Łańcuch (Chain Ricochet) [Koszt: 10 EXP]
        inventory.setItem(16, createUpgradeItem("fire_chain", "§4§lOgnisty Łańcuch (Fire Blast)", Material.BLAZE_ROD,
                "Trafienie wroga w Stanie Ognia sprawia, że Fire Blast przeskakuje do 3 kolejnych celów w promieniu 8 bloków!", 10));

        // Slot 18: Powrót
        inventory.setItem(18, ProgressionMenuGui.createItem(Material.ARROW, "§e◀ Powrót do Ołtarza Arkanów", List.of("§7Kliknij, aby wrócić.")));
    }

    private ItemStack createUpgradeItem(String upgradeId, String name, Material mat, String desc, int expCost) {
        boolean unlocked = MagicItemManager.hasUpgrade(tomeItem, upgradeId);
        List<String> lore = new ArrayList<>();
        lore.add("§7" + desc);
        lore.add("");
        lore.add("§eKoszt ulepszenia: §b" + expCost + " Poziomów EXP");
        lore.add("");

        if (unlocked) {
            lore.add("§a✔ ULEPSZENIE AKTYWNE");
            return ProgressionMenuGui.createItem(Material.ENCHANTED_BOOK, name + " §a[ODBLOKOWANE]", lore);
        } else {
            lore.add(player.getLevel() >= expCost ? "§a✦ Kliknij, aby odblokować to ulepszenie!" : "§c❌ Zbyt mało poziomów EXP!");
            return ProgressionMenuGui.createItem(mat, name, lore);
        }
    }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();

        if (slot == 18) {
            player.openInventory(new ArcaneAltarGui(player, tomeItem, spellRegistry, manaManager).getInventory());
            return;
        }

        if (slot == 10) {
            buyUpgrade("mana_reduction", "Efektywność Many", 5);
        } else if (slot == 12) {
            buyUpgrade("cooldown_reduction", "Szybki Rzut", 5);
        } else if (slot == 14) {
            buyUpgrade("multi_cast", "Wielokrotny Pocisk", 8);
        } else if (slot == 16) {
            buyUpgrade("fire_chain", "Ognisty Łańcuch", 10);
        }
    }

    private void buyUpgrade(String upgradeId, String name, int cost) {
        if (MagicItemManager.hasUpgrade(tomeItem, upgradeId)) {
            player.sendMessage("§eTo ulepszenie jest już odblokowane!");
            return;
        }

        if (player.getLevel() < cost) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            player.sendMessage("§cPotrzebujesz co najmniej " + cost + " poziomów doświadczenia!");
            return;
        }

        player.setLevel(player.getLevel() - cost);
        MagicItemManager.addUpgrade(tomeItem, upgradeId);

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.4f);
        player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.2f);
        player.sendMessage("§6§l✦ ODBLOKOWANO ULEPSZENIE: §a" + name + "§6!");

        buildGui();
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}

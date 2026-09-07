package RPG.BattleRoyale.Backpacks;

import Plugin.AmonPackPlugin;
import RPG.BattleRoyale.Loot.BattleRoyaleLootManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Zarządza systemem plecaków i fizycznego blokowania slotów gracza:
 * - Bez plecaka (Tier 0): dostępny tylko dolny pasek (Hotbar, 9 slotów: 0-8). Sloty 9-35 są zablokowane barierą.
 * - Plecak Poziom I (Tier 1): odblokowuje 1. rząd (sloty 9-17) -> łącznie 18 slotów ekwipunku.
 * - Plecak Poziom II (Tier 2): odblokowuje 2. rząd (sloty 18-26) -> łącznie 27 slotów ekwipunku.
 * - Plecak Poziom III (Tier 3): odblokowuje 3. rząd (sloty 27-35) -> łącznie 36 slotów ekwipunku (pełne EQ).
 *
 * Gracz zakłada lub ulepsza plecak klikając PPM, trzymając go w ręku.
 */
public class BackpackManager {

    public static final NamespacedKey KEY_BACKPACK_ID = new NamespacedKey(AmonPackPlugin.plugin, "br_backpack_id");
    public static final NamespacedKey KEY_BACKPACK_TIER = new NamespacedKey(AmonPackPlugin.plugin, "br_backpack_tier");
    public static final NamespacedKey KEY_LOCKED_SLOT = new NamespacedKey(AmonPackPlugin.plugin, "br_locked_slot");
    public static final NamespacedKey KEY_REQUIRED_TIER = new NamespacedKey(AmonPackPlugin.plugin, "br_required_tier");

    // Aktualnie założony poziom plecaka gracza (0 = bez plecaka, 1 = Tier I, 2 = Tier II, 3 = Tier III)
    private final Map<UUID, Integer> playerTiers = new ConcurrentHashMap<>();

    public int getPlayerTier(Player player) {
        if (player == null) return 0;
        return playerTiers.getOrDefault(player.getUniqueId(), 0);
    }

    public void setPlayerTier(Player player, int tier) {
        if (player == null) return;
        playerTiers.put(player.getUniqueId(), Math.max(0, Math.min(3, tier)));
    }

    /**
     * Inicjalizuje ekwipunek gracza na start meczu:
     * Poziom 0 (brak plecaka) -> sloty 0-8 (hotbar) wolne, sloty 9-35 zablokowane barierami.
     */
    public void initializePlayerInventory(Player player) {
        if (player == null || !player.isOnline()) return;
        setPlayerTier(player, 0);
        applySlotLocks(player, 0);
    }

    /**
     * Nakłada lub aktualizuje blokady slotów na podstawie poziomu plecaka.
     */
    public void applySlotLocks(Player player, int currentTier) {
        if (player == null || !player.isOnline()) return;
        Inventory inv = player.getInventory();

        // Rząd 1: sloty 9-17 (wymaga Tier 1)
        for (int slot = 9; slot <= 17; slot++) {
            if (currentTier >= 1) {
                if (isLockedSlotItem(inv.getItem(slot))) {
                    inv.setItem(slot, null);
                }
            } else {
                if (inv.getItem(slot) == null || isLockedSlotItem(inv.getItem(slot))) {
                    inv.setItem(slot, createLockedSlotItem(1));
                }
            }
        }

        // Rząd 2: sloty 18-26 (wymaga Tier 2)
        for (int slot = 18; slot <= 26; slot++) {
            if (currentTier >= 2) {
                if (isLockedSlotItem(inv.getItem(slot))) {
                    inv.setItem(slot, null);
                }
            } else {
                if (inv.getItem(slot) == null || isLockedSlotItem(inv.getItem(slot))) {
                    inv.setItem(slot, createLockedSlotItem(2));
                }
            }
        }

        // Rząd 3: sloty 27-35 (wymaga Tier 3)
        for (int slot = 27; slot <= 35; slot++) {
            if (currentTier >= 3) {
                if (isLockedSlotItem(inv.getItem(slot))) {
                    inv.setItem(slot, null);
                }
            } else {
                if (inv.getItem(slot) == null || isLockedSlotItem(inv.getItem(slot))) {
                    inv.setItem(slot, createLockedSlotItem(3));
                }
            }
        }
        player.updateInventory();
    }

    /**
     * Czyści wszystkie przedmioty blokujące sloty z ekwipunku gracza (np. po śmierci lub wyjściu z gry).
     */
    public void clearPlayerLockedSlots(Player player) {
        if (player == null || !player.isOnline()) return;
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (isLockedSlotItem(item)) {
                inv.setItem(i, null);
            }
        }
        playerTiers.remove(player.getUniqueId());
        player.updateInventory();
    }

    /**
     * Tworzy przedmiot placeholder blokujący dany slot.
     */
    public ItemStack createLockedSlotItem(int requiredTier) {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "✖ Zablokowany Slot");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Wymaga: " + ChatColor.YELLOW + "Plecak Poziom " + requiredTier + " (lub wyższy)");
            lore.add(ChatColor.DARK_GRAY + "Znajdź plecak w skrzyniach lub zabij zombie,");
            lore.add(ChatColor.DARK_GRAY + "a następnie kliknij PPM trzymając go w ręku!");
            meta.setLore(lore);
            meta.setCustomModelData(22099);

            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(KEY_LOCKED_SLOT, PersistentDataType.BYTE, (byte) 1);
            pdc.set(KEY_REQUIRED_TIER, PersistentDataType.INTEGER, requiredTier);
            item.setItemMeta(meta);
        }
        return item;
    }

    public boolean isLockedSlotItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(KEY_LOCKED_SLOT, PersistentDataType.BYTE);
    }

    /**
     * Zakłada / ulepsza plecak gracza po kliknięciu PPM.
     */
    public boolean equipBackpack(Player player, ItemStack itemInHand) {
        if (player == null || itemInHand == null || !isBackpack(itemInHand)) return false;

        int newTier = getBackpackTier(itemInHand);
        int currentTier = getPlayerTier(player);

        if (newTier <= currentTier) {
            player.sendMessage(ChatColor.RED + "[Plecak] Posiadasz już plecak poziomu " + currentTier + " lub lepszy!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return false;
        }

        // Zużycie przedmiotu plecaka z dłoni
        itemInHand.setAmount(itemInHand.getAmount() - 1);

        // Zwiększenie poziomu i odblokowanie slotów
        setPlayerTier(player, newTier);
        applySlotLocks(player, newTier);

        // Efekty dźwiękowe i wizualne
        player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1.0f, 1.0f);
        player.playSound(player.getLocation(), Sound.ITEM_BUNDLE_DROP_CONTENTS, 1.0f, 1.3f);
        player.getWorld().spawnParticle(org.bukkit.Particle.HAPPY_VILLAGER, player.getLocation().add(0, 1.0, 0), 15, 0.4, 0.5, 0.4, 0.1);

        int totalSlots = (newTier + 1) * 9;
        player.sendTitle(ChatColor.GREEN + "🎒 ZAŁOŻONO PLECAK!", ChatColor.YELLOW + "Poziom " + newTier + " (" + totalSlots + " slotów ekwipunku)", 10, 45, 15);
        player.sendMessage(ChatColor.GREEN + "[Plecak] Założyłeś Plecak Poziom " + newTier + "! Twój ekwipunek został rozszerzony do " + totalSlots + " slotów.");
        return true;
    }

    /**
     * Tworzy przedmiot plecaka o wybranym poziomie (1, 2 lub 3).
     */
    public ItemStack createBackpack(int tier) {
        tier = Math.max(1, Math.min(3, tier));
        UUID backpackId = UUID.randomUUID();

        Material mat = Material.CHEST_MINECART;
        String name;
        int totalUnlocked;

        switch (tier) {
            case 3:
                name = ChatColor.AQUA + "" + ChatColor.BOLD + "🎒 Plecak Przetrwania [Poziom III]";
                totalUnlocked = 36;
                break;
            case 2:
                name = ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "🎒 Plecak Taktyczny [Poziom II]";
                totalUnlocked = 27;
                break;
            case 1:
            default:
                name = ChatColor.GOLD + "" + ChatColor.BOLD + "🎒 Plecak Skautowy [Poziom I]";
                totalUnlocked = 18;
                break;
        }

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Pojemność całkowita: " + ChatColor.YELLOW + totalUnlocked + " slotów ekwipunku");
            lore.add(ChatColor.GRAY + "ID przedmiotu: " + ChatColor.DARK_GRAY + backpackId.toString().substring(0, 8));
            lore.add("");
            lore.add(ChatColor.YELLOW + "Kliknij PPM trzymając w dłoni, aby ZAŁOŻYĆ!");
            lore.add(ChatColor.GREEN + " • Odblokowuje " + (tier * 9) + " dodatkowych slotów w Twoim EQ.");
            meta.setLore(lore);

            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(KEY_BACKPACK_ID, PersistentDataType.STRING, backpackId.toString());
            pdc.set(KEY_BACKPACK_TIER, PersistentDataType.INTEGER, tier);

            item.setItemMeta(meta);
        }

        return item;
    }

    public boolean isBackpack(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        return pdc.has(KEY_BACKPACK_ID, PersistentDataType.STRING);
    }

    public int getBackpackTier(ItemStack item) {
        if (!isBackpack(item)) return 1;
        Integer tier = item.getItemMeta().getPersistentDataContainer().get(KEY_BACKPACK_TIER, PersistentDataType.INTEGER);
        return tier != null ? tier : 1;
    }

    /**
     * Blokuje klikanie w zablokowane sloty.
     */
    public void handleInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack current = event.getCurrentItem();
        ItemStack cursor = event.getCursor();

        if (isLockedSlotItem(current) || isLockedSlotItem(cursor)) {
            event.setCancelled(true);
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1.2f);
            return;
        }

        // Blokada zamiany klawiszem numerycznym (Hotbar button swap)
        if (event.getHotbarButton() >= 0) {
            int hotbarSlot = event.getHotbarButton();
            ItemStack hotbarItem = player.getInventory().getItem(hotbarSlot);
            if (isLockedSlotItem(hotbarItem) || isLockedSlotItem(current)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    /**
     * Blokada przeciągania na zablokowane sloty.
     */
    public void handleInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot < player.getInventory().getSize()) {
                ItemStack itemInSlot = player.getInventory().getItem(rawSlot);
                if (isLockedSlotItem(itemInSlot)) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    /**
     * Generuje plecak ze specjalnych zombie.
     */
    public ItemStack generateZombieBackpack(int tier, BattleRoyaleLootManager lootManager) {
        return createBackpack(tier);
    }

    public void cleanup() {
        playerTiers.clear();
    }
}

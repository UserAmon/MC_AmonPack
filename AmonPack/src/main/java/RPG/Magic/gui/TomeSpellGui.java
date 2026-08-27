package RPG.Magic.gui;

import Plugin.AmonPackPlugin;
import RPG.Magic.manager.ManaManager;
import RPG.Magic.manager.SpellRegistry;
import RPG.Magic.model.Spell;
import RPG.Magic.model.SpellElement;
import RPG.Progression.gui.ProgressionMenuGui;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class TomeSpellGui implements InventoryHolder {

    public static final NamespacedKey SPELL_PRIMARY_KEY = new NamespacedKey(AmonPackPlugin.plugin, "magic_primary_spell");
    public static final NamespacedKey SPELL_SECONDARY_KEY = new NamespacedKey(AmonPackPlugin.plugin, "magic_secondary_spell");

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
        this.inventory = Bukkit.createInventory(this, 27, ChatColor.DARK_RED + "✦ TOM OGNIA: ZAKLĘCIA ✦");
        buildGui();
    }

    private void buildGui() {
        inventory.clear();

        ItemStack filler = ProgressionMenuGui.createItem(Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, filler);
        }

        String primaryId = getPrimarySpellId(tomeItem);
        String secondaryId = getSecondarySpellId(tomeItem);

        Spell primary = spellRegistry.getSpell(primaryId);
        Spell secondary = spellRegistry.getSpell(secondaryId);

        // Slot 4: Tome Info
        List<String> tomeLore = List.of(
                "§7Potężna księga nasycona magią płomieni.",
                "",
                "§6Główne Zaklęcie (LPM): " + (primary != null ? primary.getName() : "§8[Brak]"),
                "§eDrugi Krąg (Shift+LPM): " + (secondary != null ? secondary.getName() : "§8[Brak]"),
                "",
                "§bTwoja Mana: §f" + (int) manaManager.getMana(player.getUniqueId()) + "/" + (int) manaManager.getMaxMana(player.getUniqueId()) + " MP"
        );
        inventory.setItem(4, ProgressionMenuGui.createItem(Material.BOOK, "§c§lTom Ognia", tomeLore));

        // Slot 11: Primary Spell Slot (LPM)
        List<String> primaryLore = new ArrayList<>();
        if (primary != null) {
            primaryLore.add("§7Aktualnie przypisane zaklęcie do §fLPM§7:");
            primaryLore.add(" " + primary.getName());
            primaryLore.add(" §7Koszt: §b" + primary.getManaCost() + " MP");
            primaryLore.add(" §7Cooldown: §e" + primary.getCooldownSeconds() + "s");
            primaryLore.add("");
            primaryLore.add("§7" + primary.getDescription());
        } else {
            primaryLore.add("§7Brak przypisanego czaru.");
            primaryLore.add("§eKliknij zaklęcie poniżej, aby przypisać!");
        }
        inventory.setItem(11, ProgressionMenuGui.createItem(Material.BLAZE_POWDER, "§6§l✦ Główne Zaklęcie (LPM)", primaryLore));

        // Slot 13: Secondary Spell Slot (Shift+LPM)
        List<String> secLore = new ArrayList<>();
        if (secondary != null) {
            secLore.add("§7Aktualnie przypisane zaklęcie do §fShift+LPM§7:");
            secLore.add(" " + secondary.getName());
            secLore.add(" §7Koszt: §b" + secondary.getManaCost() + " MP");
            secLore.add(" §7Cooldown: §e" + secondary.getCooldownSeconds() + "s");
            secLore.add("");
            secLore.add("§7" + secondary.getDescription());
        } else {
            secLore.add("§7Brak przypisanego czaru drugiego kręgu.");
            secLore.add("§eKliknij Shift + LPM na zaklęcie poniżej, aby przypisać!");
        }
        inventory.setItem(13, ProgressionMenuGui.createItem(Material.MAGMA_CREAM, "§e§l✧ Drugi Krąg (Shift+LPM)", secLore));

        // Slot 15: Spell Tree Info
        List<String> treeLore = List.of(
                "§7Kolejne potężniejsze zaklęcia",
                "§7będą odblokowywane w dedykowanym",
                "§7drzewku magii tego tomu!",
                "",
                "§aStatus: Aktywny Krąg Magii Ognia I"
        );
        inventory.setItem(15, ProgressionMenuGui.createItem(Material.FIRE_CHARGE, "§c§lDrzewko Magii Ognia", treeLore));

        // Available Fire Spells in bottom row (Slots 20-24)
        List<Spell> fireSpells = spellRegistry.getSpellsByElement(SpellElement.FIRE);
        int slot = 20;
        for (Spell s : fireSpells) {
            if (slot > 24) break;

            boolean isPrim = s.getId().equalsIgnoreCase(primaryId);
            boolean isSec = s.getId().equalsIgnoreCase(secondaryId);

            List<String> sLore = new ArrayList<>();
            sLore.add("§8Żywioł: " + s.getElement().getDisplayName());
            sLore.add("§7Koszt Many: §b" + s.getManaCost() + " MP");
            sLore.add("§7Czas Odnowienia: §e" + s.getCooldownSeconds() + "s");
            sLore.add("");
            sLore.add("§7" + s.getDescription());
            sLore.add("");
            if (isPrim) {
                sLore.add("§a§l✔ PRZYPISANO JAKO GŁÓWNY CZAR (LPM)");
            } else if (isSec) {
                sLore.add("§e§l✔ PRZYPISANO JAKO DRUGI KRĄG (Shift+LPM)");
            } else {
                sLore.add("§6LPM §7- Przypisz do głównego slotu");
                sLore.add("§eShift+LPM §7- Przypisz do drugiego kręgu");
            }

            Material icon = isPrim ? Material.FIRE_CHARGE : (isSec ? Material.MAGMA_CREAM : Material.BLAZE_POWDER);
            inventory.setItem(slot++, ProgressionMenuGui.createItem(icon, s.getName(), sLore));
        }

        // Close button (Slot 26)
        inventory.setItem(26, ProgressionMenuGui.createItem(Material.BARRIER, "§c§lZamknij", null));
    }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= 27) return;

        if (slot == 26) {
            player.closeInventory();
            return;
        }

        // Kliknięcie w zaklęcie (slot 20-24)
        if (slot >= 20 && slot <= 24) {
            List<Spell> fireSpells = spellRegistry.getSpellsByElement(SpellElement.FIRE);
            int index = slot - 20;
            if (index >= 0 && index < fireSpells.size()) {
                Spell selected = fireSpells.get(index);
                if (event.isShiftClick()) {
                    setSecondarySpellId(tomeItem, selected.getId());
                    player.sendMessage("§a[Tom Ognia] Przypisano czar §e" + selected.getName() + " §ado §fDrugiego Kręgu (Shift+LPM)§a!");
                } else {
                    setPrimarySpellId(tomeItem, selected.getId());
                    player.sendMessage("§a[Tom Ognia] Przypisano czar §e" + selected.getName() + " §ado §fGłównego Slotu (LPM)§a!");
                }
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.3f);
                buildGui();
            }
        }
    }

    public static String getPrimarySpellId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return "fireblast";
        String id = item.getItemMeta().getPersistentDataContainer().get(SPELL_PRIMARY_KEY, PersistentDataType.STRING);
        return id != null ? id : "fireblast";
    }

    public static void setPrimarySpellId(ItemStack item, String spellId) {
        if (item == null) return;
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(SPELL_PRIMARY_KEY, PersistentDataType.STRING, spellId);
            item.setItemMeta(meta);
        }
    }

    public static String getSecondarySpellId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(SPELL_SECONDARY_KEY, PersistentDataType.STRING);
    }

    public static void setSecondarySpellId(ItemStack item, String spellId) {
        if (item == null) return;
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(SPELL_SECONDARY_KEY, PersistentDataType.STRING, spellId);
            item.setItemMeta(meta);
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

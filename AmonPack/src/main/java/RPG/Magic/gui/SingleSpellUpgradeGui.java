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

import java.util.*;

public class SingleSpellUpgradeGui implements InventoryHolder {

    private final Player player;
    private final ItemStack mainHandItem;
    private final String spellId;
    private final SpellRegistry spellRegistry;
    private final ManaManager manaManager;
    private final Inventory inventory;

    private static class UpgradeDef {
        String key;
        String name;
        String desc;
        Material icon;
        int expCost;
        int slot;

        UpgradeDef(String key, String name, String desc, Material icon, int expCost, int slot) {
            this.key = key;
            this.name = name;
            this.desc = desc;
            this.icon = icon;
            this.expCost = expCost;
            this.slot = slot;
        }
    }

    private final Map<Integer, UpgradeDef> slotUpgradeMap = new HashMap<>();

    public SingleSpellUpgradeGui(Player player, ItemStack mainHandItem, String spellId, SpellRegistry spellRegistry, ManaManager manaManager) {
        this.player = player;
        this.mainHandItem = mainHandItem;
        this.spellId = spellId;
        this.spellRegistry = spellRegistry;
        this.manaManager = manaManager;

        Spell spell = spellRegistry.getSpell(spellId);
        String spellName = spell != null ? ChatColor.stripColor(spell.getName()) : spellId.toUpperCase();
        this.inventory = Bukkit.createInventory(this, 27, ChatColor.DARK_PURPLE + "✦ ROZWÓJ: " + spellName + " ✦");
        buildGui();
    }

    private void buildGui() {
        inventory.clear();
        slotUpgradeMap.clear();

        ItemStack filler = ProgressionMenuGui.createItem(Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, filler);
        }

        Spell spell = spellRegistry.getSpell(spellId);
        String spellName = spell != null ? spell.getName() : spellId;

        // Slot 4: Karta czaru
        inventory.setItem(4, ProgressionMenuGui.createItem(
                getSpellIcon(spellId),
                "§6§l✦ " + spellName + " ✦",
                List.of(
                        "§7" + (spell != null ? spell.getDescription() : ""),
                        "",
                        "§dWybierz pojedyncze ulepszenia poniżej, aby wzmocnić to zaklęcie!"
                )
        ));

        // Pobranie listy ulepszeń dla danego czaru
        List<UpgradeDef> upgrades = getUpgradesForSpell(spellId);
        for (UpgradeDef u : upgrades) {
            slotUpgradeMap.put(u.slot, u);
            boolean bought = MagicItemManager.hasUpgrade(mainHandItem, u.key);
            boolean isCreative = player.getGameMode() == org.bukkit.GameMode.CREATIVE;

            List<String> lore = new ArrayList<>();
            lore.add("§7" + u.desc);
            lore.add("");
            if (bought) {
                lore.add("§a✔ ZAKUPIONE I AKTYWNE");
                inventory.setItem(u.slot, ProgressionMenuGui.createItem(Material.EMERALD_BLOCK, "§a§l" + u.name, lore));
            } else {
                if (isCreative) {
                    lore.add("§a✔ [Tryb Kreatywny] Koszt EXP pomijany!");
                    lore.add("");
                    lore.add("§e✦ Kliknij, aby odblokować!");
                } else {
                    lore.add("§6✦ Koszt odblokowania: §e" + u.expCost + " Poziomów EXP");
                    lore.add("");
                    lore.add(player.getLevel() >= u.expCost ? "§e✦ Kliknij, aby odblokować!" : "§cNie masz wystarczająco dużo EXP!");
                }
                inventory.setItem(u.slot, ProgressionMenuGui.createItem(u.icon, "§e§l" + u.name, lore));
            }
        }

        // Slot 18: Powrót do wyboru zaklęć
        inventory.setItem(18, ProgressionMenuGui.createItem(Material.ARROW, "§e◀ Powrót do wyboru zaklęć", List.of("§7Kliknij, aby wrócić do listy czarów.")));
    }

    private UpgradeDef createUpgrade(String key, String defaultName, String defaultDesc, Material icon, int defaultExp, int slot) {
        org.bukkit.configuration.file.FileConfiguration cfg = Plugin.AmonPackPlugin.magicConfig;
        String name = defaultName;
        String desc = defaultDesc;
        int exp = defaultExp;

        if (cfg != null && cfg.contains("magic.upgrades." + key)) {
            name = cfg.getString("magic.upgrades." + key + ".name", defaultName);
            desc = cfg.getString("magic.upgrades." + key + ".desc", defaultDesc);
            exp = cfg.getInt("magic.upgrades." + key + ".cost_exp", defaultExp);

            if (cfg.contains("magic.upgrades." + key + ".mana_reduction")) {
                int manaRed = cfg.getInt("magic.upgrades." + key + ".mana_reduction");
                desc = "Zmniejsza koszt many o -" + manaRed + " MP.";
            } else if (cfg.contains("magic.upgrades." + key + ".cooldown_reduction")) {
                double cdRed = cfg.getDouble("magic.upgrades." + key + ".cooldown_reduction");
                desc = "Skraca czas odnowienia (cooldown) o -" + String.format(Locale.ROOT, "%.1f", cdRed) + "s.";
            }
        }
        return new UpgradeDef(key, name, desc, icon, exp, slot);
    }

    private List<UpgradeDef> getUpgradesForSpell(String id) {
        List<UpgradeDef> list = new ArrayList<>();
        switch (id.toLowerCase(Locale.ROOT)) {
            case "fireblast":
                list.add(createUpgrade("fireblast_mana", "Efektywność Many", "Zmniejsza koszt many o -10 MP.", Material.LAPIS_LAZULI, 5, 10));
                list.add(createUpgrade("fireblast_cd", "Szybki Rzut", "Skraca czas odnowienia o -1.0s.", Material.CLOCK, 5, 12));
                list.add(createUpgrade("fireblast_multi", "Wielokrotny Pocisk", "Wystrzeliwuje 2 dodatkowe kule ognia w rozrzucie.", Material.FIRE_CHARGE, 8, 14));
                list.add(createUpgrade("fireblast_chain", "Ognisty Łańcuch", "Trafienie wroga w Stanie Ognia rykoszetuje do 3 pobliskich celów.", Material.BLAZE_POWDER, 10, 16));
                break;
            case "blazing":
                list.add(createUpgrade("blazing_mana", "Efektywność Many", "Zmniejsza koszt many o -15 MP.", Material.LAPIS_LAZULI, 5, 11));
                list.add(createUpgrade("blazing_cd", "Szybki Rzut", "Skraca cooldown o -1.5s.", Material.CLOCK, 5, 13));
                list.add(createUpgrade("blazing_power", "Piekielna Fala", "Zwiększa obrażenia fali i wydłuża podpalenie do 8s.", Material.BLAZE_ROD, 8, 15));
                break;
            case "flashpoint":
                list.add(createUpgrade("flashpoint_mana", "Efektywność Many", "Zmniejsza koszt many o -20 MP.", Material.LAPIS_LAZULI, 8, 11));
                list.add(createUpgrade("flashpoint_cd", "Szybki Rzut", "Skraca cooldown o -2.0s.", Material.CLOCK, 8, 13));
                list.add(createUpgrade("flashpoint_radius", "Kataklizm", "Zwiększa promień eksplozji do 6 bloków i zadaje potężne obrażenia.", Material.TNT, 12, 15));
                break;
            case "fire_circle":
                list.add(createUpgrade("fire_circle_mana", "Efektywność Many", "Zmniejsza koszt many o -15 MP.", Material.LAPIS_LAZULI, 5, 11));
                list.add(createUpgrade("fire_circle_cd", "Szybki Rzut", "Skraca cooldown o -2.0s.", Material.CLOCK, 5, 13));
                list.add(createUpgrade("fire_circle_barrier", "Płomienna Tarcza", "Zwiększa siłę odepchnięcia i daje odporność na ogień na 5s.", Material.SHIELD, 8, 15));
                break;
            case "barrage":
                list.add(createUpgrade("barrage_mana", "Efektywność Many", "Zmniejsza koszt many o -20 MP.", Material.LAPIS_LAZULI, 8, 11));
                list.add(createUpgrade("barrage_cd", "Szybki Rzut", "Skraca cooldown o -2.0s.", Material.CLOCK, 8, 13));
                list.add(createUpgrade("barrage_count", "Deszcz Ognia", "Wystrzeliwuje 6 pocisków zamiast 4.", Material.MAGMA_CREAM, 12, 15));
                break;
            case "blow":
                list.add(createUpgrade("blow_mana", "Efektywność Many", "Zmniejsza koszt many o -10 MP.", Material.LAPIS_LAZULI, 5, 11));
                list.add(createUpgrade("blow_cd", "Szybki Rzut", "Skraca cooldown o -1.0s.", Material.CLOCK, 5, 13));
                list.add(createUpgrade("blow_power", "Poryw Fenów", "Zwiększa odrzut o +50% i zadaje dodatkowe obrażenia.", Material.FEATHER, 8, 15));
                break;
            case "airblade":
                list.add(createUpgrade("airblade_mana", "Efektywność Many", "Zmniejsza koszt many o -15 MP.", Material.LAPIS_LAZULI, 5, 11));
                list.add(createUpgrade("airblade_cd", "Szybki Rzut", "Skraca cooldown o -1.5s.", Material.CLOCK, 5, 13));
                list.add(createUpgrade("airblade_width", "Szerokie Cięcie", "Zwiększa szerokość cięcia wiatru i zadaje dodatkowe obrażenia.", Material.IRON_SWORD, 8, 15));
                break;
            case "air_vortex":
                list.add(createUpgrade("air_vortex_mana", "Efektywność Many", "Zmniejsza koszt many o -15 MP.", Material.LAPIS_LAZULI, 5, 11));
                list.add(createUpgrade("air_vortex_cd", "Szybki Rzut", "Skraca cooldown o -2.0s.", Material.CLOCK, 5, 13));
                list.add(createUpgrade("air_vortex_radius", "Cyklon Fenów", "Zwiększa promień zasysania do 12 bloków i czas trwania do 6s.", Material.ELYTRA, 10, 15));
                break;
            case "lightning":
                list.add(createUpgrade("lightning_mana", "Efektywność Many", "Zmniejsza koszt many o -10 MP.", Material.LAPIS_LAZULI, 5, 11));
                list.add(createUpgrade("lightning_cd", "Szybki Rzut", "Skraca czas odnowienia o -1.0s.", Material.CLOCK, 5, 13));
                list.add(createUpgrade("lightning_power", "Piorun Kulisty", "Zwiększa obrażenia pioruna i promień rażenia do 6 bloków.", Material.LIGHTNING_ROD, 8, 15));
                break;
            case "chain_lightning":
                list.add(createUpgrade("chain_lightning_mana", "Efektywność Many", "Zmniejsza koszt many o -15 MP.", Material.LAPIS_LAZULI, 6, 11));
                list.add(createUpgrade("chain_lightning_cd", "Szybki Rzut", "Skraca czas odnowienia o -1.5s.", Material.CLOCK, 6, 13));
                list.add(createUpgrade("chain_lightning_bounces", "Hiperprzewodność", "Zwiększa liczbę przeskoków do 6 i obrażenia wybuchu w wodzie.", Material.COPPER_INGOT, 10, 15));
                break;
            case "splash":
                list.add(createUpgrade("splash_mana", "Efektywność Many", "Zmniejsza koszt many o -10 MP.", Material.LAPIS_LAZULI, 5, 11));
                list.add(createUpgrade("splash_cd", "Szybki Rzut", "Skraca czas odnowienia o -0.8s.", Material.CLOCK, 5, 13));
                list.add(createUpgrade("splash_power", "Wodna Kula", "Zwiększa siłę odrzutu i obrażenia pocisku wodnego.", Material.PRISMARINE_SHARD, 8, 15));
                break;
            case "freeze":
                list.add(createUpgrade("freeze_mana", "Efektywność Many", "Zmniejsza koszt many o -15 MP.", Material.LAPIS_LAZULI, 5, 11));
                list.add(createUpgrade("freeze_cd", "Szybki Rzut", "Skraca czas odnowienia o -1.5s.", Material.CLOCK, 5, 13));
                list.add(createUpgrade("freeze_radius", "Lodowa Otchłań", "Zwiększa promień zamrażania do 6 bloków i obrażenia eksplozji lodu.", Material.ICE, 8, 15));
                break;
            case "evaporate":
                list.add(createUpgrade("evaporate_mana", "Efektywność Many", "Zmniejsza koszt many o -20 MP.", Material.LAPIS_LAZULI, 8, 11));
                list.add(createUpgrade("evaporate_cd", "Szybki Rzut", "Skraca czas odnowienia o -2.0s.", Material.CLOCK, 8, 13));
                list.add(createUpgrade("evaporate_power", "Wrząca Mgła", "Zadaje obrażenia wrzątku wszystkim wrogom w chmurze pary.", Material.DRAGON_BREATH, 10, 15));
                break;
            default:
                list.add(createUpgrade(id + "_mana", "Efektywność Many", "Zmniejsza koszt many tego czaru.", Material.LAPIS_LAZULI, 5, 11));
                list.add(createUpgrade(id + "_cd", "Szybki Rzut", "Skraca czas odnowienia tego czaru.", Material.CLOCK, 5, 13));
                break;
        }
        return list;
    }

    private Material getSpellIcon(String id) {
        switch (id.toLowerCase(Locale.ROOT)) {
            case "fireblast": return Material.FIRE_CHARGE;
            case "blazing": return Material.BLAZE_POWDER;
            case "flashpoint": return Material.LAVA_BUCKET;
            case "fire_circle": return Material.MAGMA_CREAM;
            case "barrage": return Material.BLAZE_ROD;
            case "blow": return Material.FEATHER;
            case "airblade": return Material.IRON_SWORD;
            case "air_vortex": return Material.ELYTRA;
            case "lightning": return Material.LIGHTNING_ROD;
            case "chain_lightning": return Material.COPPER_INGOT;
            case "splash": return Material.PRISMARINE_SHARD;
            case "freeze": return Material.ICE;
            case "evaporate": return Material.WATER_BUCKET;
            default: return Material.ENCHANTED_BOOK;
        }
    }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();

        if (slot == 18) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            player.openInventory(new SpellSelectUpgradeGui(player, mainHandItem, spellRegistry, manaManager).getInventory());
            return;
        }

        UpgradeDef upgrade = slotUpgradeMap.get(slot);
        if (upgrade == null) return;

        if (MagicItemManager.hasUpgrade(mainHandItem, upgrade.key)) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            player.sendMessage("§aPosiadasz już to ulepszenie!");
            return;
        }

        boolean isCreative = player.getGameMode() == org.bukkit.GameMode.CREATIVE;
        if (!isCreative && player.getLevel() < upgrade.expCost) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            player.sendMessage("§cPotrzebujesz §e" + upgrade.expCost + " Poziomów EXP§c, aby odblokować to ulepszenie!");
            return;
        }

        if (!isCreative) {
            player.setLevel(player.getLevel() - upgrade.expCost);
        }
        MagicItemManager.addUpgrade(mainHandItem, upgrade.key);

        player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.2f);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
        player.sendMessage("§6§l✦ ODBLOKOWANO ULEPSZENIE! §aPomyślnie zakupiono §e" + upgrade.name + " §adla zaklęcia §b" + spellId + "§a!");

        buildGui();
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}

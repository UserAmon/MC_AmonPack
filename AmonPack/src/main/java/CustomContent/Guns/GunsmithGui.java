package CustomContent.Guns;

import Plugin.AmonPackPlugin;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class GunsmithGui implements Listener {

    private static final String GUI_TITLE = "§8⚒ Warsztat Rusznikarski";
    private static final int SLOT_GUN = 13;
    private static final int SLOT_REPAIR = 29;
    private static final int SLOT_UPGRADE_LEVEL = 31;
    private static final int SLOT_MOD_RIFLING = 38;
    private static final int SLOT_MOD_LOCK = 39;
    private static final int SLOT_MOD_OPTIONAL = 40;
    private static final int SLOT_MOD_UNIQUE = 41;

    public static void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 45, GUI_TITLE);

        ItemStack filler = createItem(Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 45; i++) {
            inv.setItem(i, filler);
        }

        inv.setItem(SLOT_GUN, null);
        updateGuiButtons(inv, null);

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 0.8f, 1.2f);
    }

    private static void updateGuiButtons(Inventory inv, ItemStack gunItem) {
        GunData data = GunData.fromItemStack(gunItem);

        // 1. Przycisk Naprawy
        if (data != null) {
            int missingDur = data.getGunType().getMaxDurability() - data.getCurrentDurability();
            int ironCost = Math.max(1, (int) Math.ceil((double) missingDur / 60.0));
            if (missingDur <= 0) ironCost = 0;

            List<String> repLore = new ArrayList<>();
            repLore.add("§7Stan broni: " + (missingDur == 0 ? "§aIdealny (100%)" : "§cUszkodzona (-" + missingDur + " trwałości)"));
            repLore.add("§7Aktualna trwałość: §e" + data.getCurrentDurability() + "§7/" + data.getGunType().getMaxDurability());
            repLore.add("");
            if (ironCost > 0) {
                repLore.add("§6Wymagany materiał: §f" + ironCost + "x Sztabka Żelaza");
                repLore.add("§eKliknij, aby odnowić pełną wytrzymałość!");
            } else {
                repLore.add("§aBroń nie wymaga naprawy.");
            }
            inv.setItem(SLOT_REPAIR, createItem(Material.ANVIL, "§a§lNaprawa Broni", repLore));
        } else {
            inv.setItem(SLOT_REPAIR, createItem(Material.BARRIER, "§c§lWłóż broń palną", List.of("§7Umieść broń palną w gnieździe powyżej.")));
        }

        // 2. Przycisk Poziomu Broni (Tier) - ze wspomnieniem o celności!
        if (data != null) {
            List<String> lvlLore = new ArrayList<>();
            lvlLore.add("§7Aktualny poziom: §e" + data.getLevel() + "§7/5");
            lvlLore.add("§7Bonus do obrażeń: §c+" + String.format("%.1f", (data.getLevel() - 1) * 0.8) + " DMG");
            lvlLore.add("§7Bonus do celności: §a+" + ((data.getLevel() - 1) * 12) + "% Większa Celność / Mniejszy Rozrzut");
            lvlLore.add("");
            if (data.getLevel() < 5) {
                int costGold = data.getLevel() * 2;
                int costLvl = data.getLevel() * 3;
                lvlLore.add("§6Wymagania na Poziom " + (data.getLevel() + 1) + ":");
                lvlLore.add(" §f✦ " + costGold + "x Sztabka Złota");
                lvlLore.add(" §f✦ " + costLvl + " Poziomów Doświadczenia (EXP)");
                lvlLore.add(" §a✦ Zapewnia: §f+0.8 DMG oraz +12% Wyższą Celność!");
                lvlLore.add("");
                lvlLore.add("§eKliknij, aby ulepszyć broń!");
            } else {
                lvlLore.add("§aOsiągnięto maksymalny poziom mistrzowski!");
            }
            inv.setItem(SLOT_UPGRADE_LEVEL, createItem(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE, "§6§lUlepszenie Poziomu (Tier)", lvlLore));
        } else {
            inv.setItem(SLOT_UPGRADE_LEVEL, createItem(Material.BARRIER, "§c§lWłóż broń palną", List.of("§7Umieść broń palną w gnieździe powyżej.")));
        }

        // 3. Modyfikacje
        if (data != null) {
            // Slot 1: Gwintowana lufa
            List<String> rifLore = new ArrayList<>();
            rifLore.add("§7Zwiększa celność o §a+40%§7 oraz zasięg o §b+10m§7.");
            rifLore.add("§7Status: " + (data.hasRifling() ? "§aZainstalowana" : "§cBrak"));
            if (!data.hasRifling()) {
                rifLore.add("");
                rifLore.add("§6Koszt montażu: §f4x Żelazo, 2x Miedź");
                rifLore.add("§eKliknij, aby zamontować!");
            }
            inv.setItem(SLOT_MOD_RIFLING, createItem(Material.COPPER_INGOT, "§a§lGwintowana Lufa", rifLore));

            // Slot 2: Wzmocniony zamek
            List<String> lockLore = new ArrayList<>();
            lockLore.add("§7Skraca czas ładowania broni o §e-30%§7.");
            lockLore.add("§7Status: " + (data.hasReinforcedLock() ? "§aZainstalowany" : "§cBrak"));
            if (!data.hasReinforcedLock()) {
                lockLore.add("");
                lockLore.add("§6Koszt montażu: §f2x Krzemień, 2x Żelazo, 1x Redstone");
                lockLore.add("§eKliknij, aby zamontować!");
            }
            inv.setItem(SLOT_MOD_LOCK, createItem(Material.FLINT, "§e§lWzmocniony Zamek Skałkowy", lockLore));

            // Slot 3: Bagnet Myśliwski (dla Muszkietu) lub Amortyzator Odrzutu
            if (data.getGunType() == GunType.FLINTLOCK_MUSKET) {
                List<String> bayLore = new ArrayList<>();
                bayLore.add("§7Zadaje §c+7.0 obrażeń wręcz§7 przy bezpośrednim uderzeniu.");
                bayLore.add("§7Status: " + (data.hasBayonet() ? "§aZainstalowany" : "§cBrak"));
                if (!data.hasBayonet()) {
                    bayLore.add("");
                    bayLore.add("§6Koszt montażu: §f2x Żelazo, 1x Skóra");
                    bayLore.add("§eKliknij, aby zamontować!");
                }
                inv.setItem(SLOT_MOD_OPTIONAL, createItem(Material.IRON_SWORD, "§c§lBagnet Myśliwski", bayLore));
            } else {
                List<String> bayLore = new ArrayList<>();
                bayLore.add("§7Zmniejsza odrzut broni o §b-40%§7.");
                bayLore.add("§7Status: " + (data.hasBayonet() ? "§aZainstalowany" : "§cBrak"));
                if (!data.hasBayonet()) {
                    bayLore.add("");
                    bayLore.add("§6Koszt montażu: §f2x Skóra, 2x Miedź");
                    bayLore.add("§eKliknij, aby zamontować!");
                }
                inv.setItem(SLOT_MOD_OPTIONAL, createItem(Material.LEATHER, "§6§lErgonomiczne Łoże", bayLore));
            }

            // Slot 4: CZWARTY SLOT - UNIKALNY DLA KAŻDEJ BRONI!
            GunType gt = data.getGunType();
            if (gt == GunType.FLINTLOCK_PISTOL) {
                List<String> uLore = new ArrayList<>();
                uLore.add("§7Całkowicie usuwa karę §c-10% do prędkości ruchu§7 przy trzymaniu pistoletu.");
                uLore.add("§7Pozwala na pełną mobilność (100% prędkości w dłoni).");
                uLore.add("§7Status: " + (data.hasUniqueMod() ? "§aZainstalowana" : "§cBrak"));
                if (!data.hasUniqueMod()) {
                    uLore.add("");
                    uLore.add("§6Koszt montażu: §f4x Skóra, 2x Złoto");
                    uLore.add("§eKliknij, aby zamontować!");
                }
                inv.setItem(SLOT_MOD_UNIQUE, createItem(Material.FEATHER, "§a§l[Unikalne] Lekka Konstrukcja", uLore));
            } else if (gt == GunType.BLUNDERBUSS) {
                List<String> uLore = new ArrayList<>();
                uLore.add("§7Modyfikuje Garłacz na układ §6Dubeltówki (Double Barrel)§7!");
                uLore.add("§7Zwiększa pojemność do §e2 pocisków na raz§7 przed przeładowaniem.");
                uLore.add("§7Status: " + (data.hasUniqueMod() ? "§aZainstalowana" : "§cBrak"));
                if (!data.hasUniqueMod()) {
                    uLore.add("");
                    uLore.add("§6Koszt montażu: §f6x Żelazo, 4x Miedź");
                    uLore.add("§eKliknij, aby zamontować!");
                }
                inv.setItem(SLOT_MOD_UNIQUE, createItem(Material.CROSSBOW, "§6§l[Unikalne] Dubeltówka (Podwójna Lufa)", uLore));
            } else if (gt == GunType.FLINTLOCK_MUSKET) {
                List<String> uLore = new ArrayList<>();
                uLore.add("§7Potężny §b10x Zoom Optyczny§7 z siatką celowniczą i §4+25% obrażeń w głowę§7.");
                uLore.add("§7Status: " + (data.hasUniqueMod() ? "§aZainstalowana" : "§cBrak"));
                if (!data.hasUniqueMod()) {
                    uLore.add("");
                    uLore.add("§6Koszt montażu: §f3x Miedź, 2x Szkło, 1x Ametyst");
                    uLore.add("§eKliknij, aby zamontować!");
                }
                inv.setItem(SLOT_MOD_UNIQUE, createItem(Material.SPYGLASS, "§b§l[Unikalne] Luneta Optyczna 10x", uLore));
            } else if (gt == GunType.PEPPERBOX) {
                List<String> uLore = new ArrayList<>();
                uLore.add("§7Zwiększa pojemność bębna z 4 do §e5 komór (+1 pocisk)§7.");
                uLore.add("§7Zmniejsza odrzut gracza o §a-50%§7 i redukuje dym z lufy.");
                uLore.add("§7Status: " + (data.hasUniqueMod() ? "§aZainstalowany" : "§cBrak"));
                if (!data.hasUniqueMod()) {
                    uLore.add("");
                    uLore.add("§6Koszt montażu: §f4x Żelazo, 2x Miedź, 2x Redstone");
                    uLore.add("§eKliknij, aby zamontować!");
                }
                inv.setItem(SLOT_MOD_UNIQUE, createItem(Material.REPEATER, "§d§l[Unikalne] Kompensator i Bęben +1", uLore));
            }
        } else {
            inv.setItem(SLOT_MOD_RIFLING, createItem(Material.GRAY_DYE, "§8Modyfikacja Lufy", List.of("§7Włóż broń.")));
            inv.setItem(SLOT_MOD_LOCK, createItem(Material.GRAY_DYE, "§8Modyfikacja Zamka", List.of("§7Włóż broń.")));
            inv.setItem(SLOT_MOD_OPTIONAL, createItem(Material.GRAY_DYE, "§8Modyfikacja Dodatkowa", List.of("§7Włóż broń.")));
            inv.setItem(SLOT_MOD_UNIQUE, createItem(Material.GRAY_DYE, "§8Unikalne Ulepszenie", List.of("§7Włóż broń.")));
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(GUI_TITLE)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        int slot = event.getRawSlot();
        Inventory inv = event.getInventory();

        if (slot < 45 && slot != SLOT_GUN) {
            event.setCancelled(true);

            ItemStack gun = inv.getItem(SLOT_GUN);
            if (!GunData.isGun(gun)) return;
            GunData data = GunData.fromItemStack(gun);
            if (data == null) return;

            if (slot == SLOT_REPAIR) {
                handleRepair(player, gun, data, inv);
            } else if (slot == SLOT_UPGRADE_LEVEL) {
                handleUpgradeLevel(player, gun, data, inv);
            } else if (slot == SLOT_MOD_RIFLING) {
                handleModRifling(player, gun, data, inv);
            } else if (slot == SLOT_MOD_LOCK) {
                handleModLock(player, gun, data, inv);
            } else if (slot == SLOT_MOD_OPTIONAL) {
                handleModOptional(player, gun, data, inv);
            } else if (slot == SLOT_MOD_UNIQUE) {
                handleModUnique(player, gun, data, inv);
            }
            return;
        }

        Bukkit.getScheduler().runTaskLater(AmonPackPlugin.plugin, () -> {
            ItemStack currentGun = inv.getItem(SLOT_GUN);
            updateGuiButtons(inv, currentGun);
        }, 1L);
    }

    private void handleRepair(Player player, ItemStack gun, GunData data, Inventory inv) {
        int missingDur = data.getGunType().getMaxDurability() - data.getCurrentDurability();
        if (missingDur <= 0) {
            player.sendMessage("§aBroń jest już w nienagannym stanie!");
            return;
        }
        int ironCost = Math.max(1, (int) Math.ceil((double) missingDur / 60.0));
        if (player.getGameMode() != GameMode.CREATIVE && !player.getInventory().containsAtLeast(new ItemStack(Material.IRON_INGOT), ironCost)) {
            player.sendMessage("§c❌ Potrzebujesz " + ironCost + "x Sztabka Żelaza do naprawy!");
            player.playSound(player.getLocation(), Sound.BLOCK_CHEST_LOCKED, 1.0f, 1.5f);
            return;
        }
        if (player.getGameMode() != GameMode.CREATIVE) {
            player.getInventory().removeItem(new ItemStack(Material.IRON_INGOT, ironCost));
        }
        data.setCurrentDurability(data.getGunType().getMaxDurability());
        data.applyToItemStack(gun);
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0f, 1.0f);
        player.sendMessage("§a✔ Pomyślnie naprawiono broń do pełnej trwałości!");
        updateGuiButtons(inv, gun);
    }

    private void handleUpgradeLevel(Player player, ItemStack gun, GunData data, Inventory inv) {
        if (data.getLevel() >= 5) {
            player.sendMessage("§aBroń osiągnęła już maksymalny poziom mistrzowski!");
            return;
        }
        int costGold = data.getLevel() * 2;
        int costLvl = data.getLevel() * 3;

        if (player.getGameMode() != GameMode.CREATIVE) {
            if (!player.getInventory().containsAtLeast(new ItemStack(Material.GOLD_INGOT), costGold)) {
                player.sendMessage("§c❌ Potrzebujesz " + costGold + "x Sztabka Złota!");
                return;
            }
            if (player.getLevel() < costLvl) {
                player.sendMessage("§c❌ Potrzebujesz " + costLvl + " poziomów doświadczenia (EXP)!");
                return;
            }
            player.getInventory().removeItem(new ItemStack(Material.GOLD_INGOT, costGold));
            player.setLevel(player.getLevel() - costLvl);
        }

        data.setLevel(data.getLevel() + 1);
        data.applyToItemStack(gun);
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.2f);
        player.sendMessage("§6⭐ Ulepszono broń na Poziom " + data.getLevel() + "! (+0.8 DMG, +12% Celności)");
        updateGuiButtons(inv, gun);
    }

    private void handleModRifling(Player player, ItemStack gun, GunData data, Inventory inv) {
        if (data.hasRifling()) {
            player.sendMessage("§aGwintowana lufa jest już zamontowana!");
            return;
        }
        if (player.getGameMode() != GameMode.CREATIVE) {
            if (!player.getInventory().containsAtLeast(new ItemStack(Material.IRON_INGOT), 4) ||
                    !player.getInventory().containsAtLeast(new ItemStack(Material.COPPER_INGOT), 2)) {
                player.sendMessage("§c❌ Wymagane: 4x Żelazo, 2x Miedź!");
                return;
            }
            player.getInventory().removeItem(new ItemStack(Material.IRON_INGOT, 4), new ItemStack(Material.COPPER_INGOT, 2));
        }
        data.setRifling(true);
        data.applyToItemStack(gun);
        player.playSound(player.getLocation(), Sound.BLOCK_SMITHING_TABLE_USE, 1.0f, 1.2f);
        player.sendMessage("§a✔ Zamontowano Gwintowaną Lufę! (+40% celności, +10m zasięgu)");
        updateGuiButtons(inv, gun);
    }

    private void handleModLock(Player player, ItemStack gun, GunData data, Inventory inv) {
        if (data.hasReinforcedLock()) {
            player.sendMessage("§aWzmocniony zamek jest już zamontowany!");
            return;
        }
        if (player.getGameMode() != GameMode.CREATIVE) {
            if (!player.getInventory().containsAtLeast(new ItemStack(Material.FLINT), 2) ||
                    !player.getInventory().containsAtLeast(new ItemStack(Material.IRON_INGOT), 2) ||
                    !player.getInventory().containsAtLeast(new ItemStack(Material.REDSTONE), 1)) {
                player.sendMessage("§c❌ Wymagane: 2x Krzemień, 2x Żelazo, 1x Redstone!");
                return;
            }
            player.getInventory().removeItem(new ItemStack(Material.FLINT, 2), new ItemStack(Material.IRON_INGOT, 2), new ItemStack(Material.REDSTONE, 1));
        }
        data.setReinforcedLock(true);
        data.applyToItemStack(gun);
        player.playSound(player.getLocation(), Sound.BLOCK_SMITHING_TABLE_USE, 1.0f, 1.2f);
        player.sendMessage("§e✔ Zamontowano Wzmocniony Zamek Skałkowy! (-30% czasu ładowania)");
        updateGuiButtons(inv, gun);
    }

    private void handleModOptional(Player player, ItemStack gun, GunData data, Inventory inv) {
        if (data.hasBayonet()) {
            player.sendMessage("§aModyfikacja łoża/bagnetu jest już zamontowana!");
            return;
        }
        if (player.getGameMode() != GameMode.CREATIVE) {
            if (data.getGunType() == GunType.FLINTLOCK_MUSKET) {
                if (!player.getInventory().containsAtLeast(new ItemStack(Material.IRON_INGOT), 2) ||
                        !player.getInventory().containsAtLeast(new ItemStack(Material.LEATHER), 1)) {
                    player.sendMessage("§c❌ Wymagane: 2x Żelazo, 1x Skóra!");
                    return;
                }
                player.getInventory().removeItem(new ItemStack(Material.IRON_INGOT, 2), new ItemStack(Material.LEATHER, 1));
            } else {
                if (!player.getInventory().containsAtLeast(new ItemStack(Material.LEATHER), 2) ||
                        !player.getInventory().containsAtLeast(new ItemStack(Material.COPPER_INGOT), 2)) {
                    player.sendMessage("§c❌ Wymagane: 2x Skóra, 2x Miedź!");
                    return;
                }
                player.getInventory().removeItem(new ItemStack(Material.LEATHER, 2), new ItemStack(Material.COPPER_INGOT, 2));
            }
        }
        data.setBayonet(true);
        data.applyToItemStack(gun);
        player.playSound(player.getLocation(), Sound.BLOCK_SMITHING_TABLE_USE, 1.0f, 1.2f);
        player.sendMessage("§a✔ Zamontowano modyfikację!");
        updateGuiButtons(inv, gun);
    }

    private void handleModUnique(Player player, ItemStack gun, GunData data, Inventory inv) {
        if (data.hasUniqueMod()) {
            player.sendMessage("§aUnikalna modyfikacja jest już zamontowana!");
            return;
        }
        GunType gt = data.getGunType();
        if (player.getGameMode() != GameMode.CREATIVE) {
            if (gt == GunType.FLINTLOCK_PISTOL) {
                if (!player.getInventory().containsAtLeast(new ItemStack(Material.LEATHER), 4) ||
                        !player.getInventory().containsAtLeast(new ItemStack(Material.GOLD_INGOT), 2)) {
                    player.sendMessage("§c❌ Wymagane: 4x Skóra, 2x Złoto!");
                    return;
                }
                player.getInventory().removeItem(new ItemStack(Material.LEATHER, 4), new ItemStack(Material.GOLD_INGOT, 2));
            } else if (gt == GunType.BLUNDERBUSS) {
                if (!player.getInventory().containsAtLeast(new ItemStack(Material.IRON_INGOT), 6) ||
                        !player.getInventory().containsAtLeast(new ItemStack(Material.COPPER_INGOT), 4)) {
                    player.sendMessage("§c❌ Wymagane: 6x Żelazo, 4x Miedź!");
                    return;
                }
                player.getInventory().removeItem(new ItemStack(Material.IRON_INGOT, 6), new ItemStack(Material.COPPER_INGOT, 4));
            } else if (gt == GunType.FLINTLOCK_MUSKET) {
                if (!player.getInventory().containsAtLeast(new ItemStack(Material.COPPER_INGOT), 3) ||
                        !player.getInventory().containsAtLeast(new ItemStack(Material.GLASS), 2) ||
                        !player.getInventory().containsAtLeast(new ItemStack(Material.AMETHYST_SHARD), 1)) {
                    player.sendMessage("§c❌ Wymagane: 3x Miedź, 2x Szkło, 1x Odłamek Ametystu!");
                    return;
                }
                player.getInventory().removeItem(new ItemStack(Material.COPPER_INGOT, 3), new ItemStack(Material.GLASS, 2), new ItemStack(Material.AMETHYST_SHARD, 1));
            } else if (gt == GunType.PEPPERBOX) {
                if (!player.getInventory().containsAtLeast(new ItemStack(Material.IRON_INGOT), 4) ||
                        !player.getInventory().containsAtLeast(new ItemStack(Material.COPPER_INGOT), 2) ||
                        !player.getInventory().containsAtLeast(new ItemStack(Material.REDSTONE), 2)) {
                    player.sendMessage("§c❌ Wymagane: 4x Żelazo, 2x Miedź, 2x Redstone!");
                    return;
                }
                player.getInventory().removeItem(new ItemStack(Material.IRON_INGOT, 4), new ItemStack(Material.COPPER_INGOT, 2), new ItemStack(Material.REDSTONE, 2));
            }
        }
        data.setUniqueMod(true);
        data.applyToItemStack(gun);
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.4f);
        player.sendMessage("§6⭐ Zamontowano unikalną modyfikację rusznikarską!");
        updateGuiButtons(inv, gun);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!event.getView().getTitle().equals(GUI_TITLE)) return;
        Inventory inv = event.getInventory();
        ItemStack gun = inv.getItem(SLOT_GUN);
        if (gun != null && gun.getType() != Material.AIR) {
            inv.setItem(SLOT_GUN, null);
            if (event.getPlayer() instanceof Player p) {
                p.getInventory().addItem(gun).values().forEach(rem -> p.getWorld().dropItemNaturally(p.getLocation(), rem));
            }
        }
    }

    private static ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null) meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
}

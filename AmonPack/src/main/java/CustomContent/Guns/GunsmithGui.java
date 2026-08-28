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
    private static final int SLOT_MOD_SCOPE = 40;
    private static final int SLOT_MOD_BAYONET = 41;

    public static void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 45, GUI_TITLE);

        // Czarne tło
        ItemStack filler = createItem(Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 45; i++) {
            inv.setItem(i, filler);
        }

        // Puste gniazdo na broń
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
            inv.setItem(SLOT_REPAIR, createItem(Material.BARRIER, "§c§lWłóż broń palną", List.of("§7Umieść pistolet lub muszkiet w gnieździe powyżej.")));
        }

        // 2. Przycisk Poziomu Broni
        if (data != null) {
            List<String> lvlLore = new ArrayList<>();
            lvlLore.add("§7Aktualny poziom: §e" + data.getLevel() + "§7/5");
            lvlLore.add("§7Bonus do obrażeń: §c+" + String.format("%.1f", (data.getLevel() - 1) * 0.8) + " DMG");
            lvlLore.add("");
            if (data.getLevel() < 5) {
                int costGold = data.getLevel() * 2;
                int costLvl = data.getLevel() * 3;
                lvlLore.add("§6Wymagania ulepszenia na Poz. " + (data.getLevel() + 1) + ":");
                lvlLore.add(" §f✦ " + costGold + "x Sztabka Złota");
                lvlLore.add(" §f✦ " + costLvl + " Poziomów Doświadczenia (EXP)");
                lvlLore.add("");
                lvlLore.add("§eKliknij, aby podnieść poziom broni!");
            } else {
                lvlLore.add("§aOsiągnięto maksymalny poziom mistrzowski!");
            }
            inv.setItem(SLOT_UPGRADE_LEVEL, createItem(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE, "§6§lUlepszenie Poziomu (Tier)", lvlLore));
        } else {
            inv.setItem(SLOT_UPGRADE_LEVEL, createItem(Material.BARRIER, "§c§lWłóż broń palną", List.of("§7Umieść pistolet lub muszkiet w gnieździe powyżej.")));
        }

        // 3. Modyfikacje
        if (data != null) {
            // Gwintowana lufa
            List<String> rifLore = new ArrayList<>();
            rifLore.add("§7Zwiększa celność o §a+40%§7 oraz zasięg o §b+10m§7.");
            rifLore.add("§7Status: " + (data.hasRifling() ? "§aZainstalowana" : "§cBrak"));
            if (!data.hasRifling()) {
                rifLore.add("");
                rifLore.add("§6Koszt montażu: §f4x Żelazo, 2x Miedź");
                rifLore.add("§eKliknij, aby zamontować!");
            }
            inv.setItem(SLOT_MOD_RIFLING, createItem(Material.COPPER_INGOT, "§a§lGwintowana Lufa", rifLore));

            // Wzmocniony zamek
            List<String> lockLore = new ArrayList<>();
            lockLore.add("§7Skraca czas ładowania broni o §e-30%§7.");
            lockLore.add("§7Status: " + (data.hasReinforcedLock() ? "§aZainstalowany" : "§cBrak"));
            if (!data.hasReinforcedLock()) {
                lockLore.add("");
                lockLore.add("§6Koszt montażu: §f2x Krzemień, 2x Żelazo, 1x Redstone");
                lockLore.add("§eKliknij, aby zamontować!");
            }
            inv.setItem(SLOT_MOD_LOCK, createItem(Material.FLINT, "§e§lWzmocniony Zamek Skałkowy", lockLore));

            // Lunetka mosiężna
            List<String> scLore = new ArrayList<>();
            scLore.add("§7Zapewnia wyraźny zoom ADS oraz §4+25% obrażeń w głowę§7.");
            scLore.add("§7Status: " + (data.hasBrassScope() ? "§aZainstalowana" : "§cBrak"));
            if (!data.hasBrassScope()) {
                scLore.add("");
                scLore.add("§6Koszt montażu: §f2x Szkło, 3x Miedź");
                scLore.add("§eKliknij, aby zamontować!");
            }
            inv.setItem(SLOT_MOD_SCOPE, createItem(Material.SPYGLASS, "§b§lLunetka Mosiężna", scLore));

            // Bagnet
            if (data.getGunType().isSupportsBayonet()) {
                List<String> bayLore = new ArrayList<>();
                bayLore.add("§7Zadaje §c+7.0 obrażeń wręcz§7 przy bezpośrednim uderzeniu.");
                bayLore.add("§7Status: " + (data.hasBayonet() ? "§aZainstalowany" : "§cBrak"));
                if (!data.hasBayonet()) {
                    bayLore.add("");
                    bayLore.add("§6Koszt montażu: §f2x Żelazo, 1x Skóra");
                    bayLore.add("§eKliknij, aby zamontować!");
                }
                inv.setItem(SLOT_MOD_BAYONET, createItem(Material.IRON_SWORD, "§c§lBagnet Myśliwski", bayLore));
            } else {
                inv.setItem(SLOT_MOD_BAYONET, createItem(Material.BARRIER, "§8Niedostępne", List.of("§7Tylko Muszkiet wspiera montaż bagnetu.")));
            }
        } else {
            inv.setItem(SLOT_MOD_RIFLING, createItem(Material.GRAY_DYE, "§8Modyfikacja Lufy", List.of("§7Włóż broń.")));
            inv.setItem(SLOT_MOD_LOCK, createItem(Material.GRAY_DYE, "§8Modyfikacja Zamka", List.of("§7Włóż broń.")));
            inv.setItem(SLOT_MOD_SCOPE, createItem(Material.GRAY_DYE, "§8Modyfikacja Celownika", List.of("§7Włóż broń.")));
            inv.setItem(SLOT_MOD_BAYONET, createItem(Material.GRAY_DYE, "§8Modyfikacja Bagnetu", List.of("§7Włóż broń.")));
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(GUI_TITLE)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        int slot = event.getRawSlot();
        Inventory inv = event.getInventory();

        // Kliknięcie w sloty funkcyjne
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
            } else if (slot == SLOT_MOD_SCOPE) {
                handleModScope(player, gun, data, inv);
            } else if (slot == SLOT_MOD_BAYONET) {
                handleModBayonet(player, gun, data, inv);
            }
            return;
        }

        // Zmiana broni w slocie
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
        updateGuiButtons(inv, gun);

        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0f, 1.0f);
        player.sendMessage("§a✔ Pomyślnie naprawiono broń do pełnej trwałości!");
    }

    private void handleUpgradeLevel(Player player, ItemStack gun, GunData data, Inventory inv) {
        if (data.getLevel() >= 5) {
            player.sendMessage("§6Broń osiągnęła już maksymalny poziom (Tier 5)!");
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
                player.sendMessage("§c❌ Potrzebujesz " + costLvl + " Poziomów EXP!");
                return;
            }
            player.getInventory().removeItem(new ItemStack(Material.GOLD_INGOT, costGold));
            player.setLevel(player.getLevel() - costLvl);
        }

        data.setLevel(data.getLevel() + 1);
        data.applyToItemStack(gun);
        updateGuiButtons(inv, gun);

        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.4f);
        player.sendMessage("§6§l[Rusznikarnia] §eBroń została ulepszona na §aPoziom " + data.getLevel() + "§e!");
    }

    private void handleModRifling(Player player, ItemStack gun, GunData data, Inventory inv) {
        if (data.hasRifling()) return;
        if (player.getGameMode() != GameMode.CREATIVE) {
            if (!player.getInventory().containsAtLeast(new ItemStack(Material.IRON_INGOT), 4) ||
                    !player.getInventory().containsAtLeast(new ItemStack(Material.COPPER_INGOT), 2)) {
                player.sendMessage("§c❌ Wymagane materiały: 4x Żelazo, 2x Miedź!");
                return;
            }
            player.getInventory().removeItem(new ItemStack(Material.IRON_INGOT, 4), new ItemStack(Material.COPPER_INGOT, 2));
        }
        data.setRifling(true);
        data.applyToItemStack(gun);
        updateGuiButtons(inv, gun);
        player.playSound(player.getLocation(), Sound.BLOCK_SMITHING_TABLE_USE, 1.0f, 1.2f);
        player.sendMessage("§a✔ Zamontowano Gwintowaną Lufę!");
    }

    private void handleModLock(Player player, ItemStack gun, GunData data, Inventory inv) {
        if (data.hasReinforcedLock()) return;
        if (player.getGameMode() != GameMode.CREATIVE) {
            if (!player.getInventory().containsAtLeast(new ItemStack(Material.FLINT), 2) ||
                    !player.getInventory().containsAtLeast(new ItemStack(Material.IRON_INGOT), 2) ||
                    !player.getInventory().containsAtLeast(new ItemStack(Material.REDSTONE), 1)) {
                player.sendMessage("§c❌ Wymagane materiały: 2x Krzemień, 2x Żelazo, 1x Redstone!");
                return;
            }
            player.getInventory().removeItem(new ItemStack(Material.FLINT, 2), new ItemStack(Material.IRON_INGOT, 2), new ItemStack(Material.REDSTONE, 1));
        }
        data.setReinforcedLock(true);
        data.applyToItemStack(gun);
        updateGuiButtons(inv, gun);
        player.playSound(player.getLocation(), Sound.BLOCK_SMITHING_TABLE_USE, 1.0f, 1.2f);
        player.sendMessage("§a✔ Zamontowano Wzmocniony Zamek Skałkowy!");
    }

    private void handleModScope(Player player, ItemStack gun, GunData data, Inventory inv) {
        if (data.hasBrassScope()) return;
        if (player.getGameMode() != GameMode.CREATIVE) {
            if (!player.getInventory().containsAtLeast(new ItemStack(Material.GLASS), 2) ||
                    !player.getInventory().containsAtLeast(new ItemStack(Material.COPPER_INGOT), 3)) {
                player.sendMessage("§c❌ Wymagane materiały: 2x Szkło, 3x Miedź!");
                return;
            }
            player.getInventory().removeItem(new ItemStack(Material.GLASS, 2), new ItemStack(Material.COPPER_INGOT, 3));
        }
        data.setBrassScope(true);
        data.applyToItemStack(gun);
        updateGuiButtons(inv, gun);
        player.playSound(player.getLocation(), Sound.BLOCK_SMITHING_TABLE_USE, 1.0f, 1.2f);
        player.sendMessage("§a✔ Zamontowano Lunetkę Mosiężną!");
    }

    private void handleModBayonet(Player player, ItemStack gun, GunData data, Inventory inv) {
        if (data.hasBayonet() || !data.getGunType().isSupportsBayonet()) return;
        if (player.getGameMode() != GameMode.CREATIVE) {
            if (!player.getInventory().containsAtLeast(new ItemStack(Material.IRON_INGOT), 2) ||
                    !player.getInventory().containsAtLeast(new ItemStack(Material.LEATHER), 1)) {
                player.sendMessage("§c❌ Wymagane materiały: 2x Żelazo, 1x Skóra!");
                return;
            }
            player.getInventory().removeItem(new ItemStack(Material.IRON_INGOT, 2), new ItemStack(Material.LEATHER, 1));
        }
        data.setBayonet(true);
        data.applyToItemStack(gun);
        updateGuiButtons(inv, gun);
        player.playSound(player.getLocation(), Sound.BLOCK_SMITHING_TABLE_USE, 1.0f, 1.2f);
        player.sendMessage("§a✔ Zamontowano Bagnet Myśliwski!");
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!event.getView().getTitle().equals(GUI_TITLE)) return;
        if (event.getPlayer() instanceof Player player) {
            ItemStack gun = event.getInventory().getItem(SLOT_GUN);
            if (gun != null && gun.getType() != Material.AIR) {
                // Bezpieczny zwrot broni graczowi
                if (player.getInventory().firstEmpty() != -1) {
                    player.getInventory().addItem(gun);
                } else {
                    player.getWorld().dropItemNaturally(player.getLocation(), gun);
                }
            }
        }
    }

    private static ItemStack createItem(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null) meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
}

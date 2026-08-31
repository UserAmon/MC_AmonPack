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
    private static final int SLOT_MOD_UNIQUE_1 = 41;
    private static final int SLOT_MOD_UNIQUE_2 = 42;
    private static final int SLOT_MOD_UNIQUE_3 = 43;

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
            int maxDur = data.getMaxDurability();
            int missingDur = maxDur - data.getCurrentDurability();
            int ironCost = Math.max(1, (int) Math.ceil((double) missingDur / 60.0));
            if (missingDur <= 0) ironCost = 0;

            List<String> repLore = new ArrayList<>();
            repLore.add("§7Stan broni: " + (missingDur == 0 ? "§aIdealny (100%)" : "§cUszkodzona (-" + missingDur + " trwałości)"));
            repLore.add("§7Aktualna trwałość: §e" + data.getCurrentDurability() + "§7/" + maxDur);
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

        // 2. Przycisk Poziomu Broni (Tier)
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

        // 3. Modyfikacje Podstawowe (Sloty 38, 39, 40)
        if (data != null) {
            // Slot 38: Gwintowana lufa
            List<String> rifLore = new ArrayList<>();
            rifLore.add("§7Zwiększa celność o §a+30%§7 oraz zasięg o §b+10m§7.");
            rifLore.add("§7Status: " + (data.hasRifling() ? "§aZainstalowana" : "§cBrak"));
            if (!data.hasRifling()) {
                rifLore.add("");
                rifLore.add("§6Koszt montażu: §f4x Żelazo, 2x Miedź");
                rifLore.add("§eKliknij, aby zamontować!");
            }
            inv.setItem(SLOT_MOD_RIFLING, createItem(Material.COPPER_INGOT, "§a§lGwintowana Lufa", rifLore));

            // Slot 39: Wzmocniony zamek
            List<String> lockLore = new ArrayList<>();
            lockLore.add("§7Skraca czas ładowania broni o §e-30%§7.");
            lockLore.add("§7Status: " + (data.hasReinforcedLock() ? "§aZainstalowany" : "§cBrak"));
            if (!data.hasReinforcedLock()) {
                lockLore.add("");
                lockLore.add("§6Koszt montażu: §f2x Krzemień, 2x Żelazo, 1x Redstone");
                lockLore.add("§eKliknij, aby zamontować!");
            }
            inv.setItem(SLOT_MOD_LOCK, createItem(Material.FLINT, "§e§lWzmocniony Zamek Skałkowy", lockLore));

            // Slot 40: Bagnet Myśliwski (dla Muszkietu) lub Ergonomiczne Łoże
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

            // 4. Sloty 41, 42, 43: UNIKALNE ULEPSZENIA (Tylko 1 na broń!)
            GunType gt = data.getGunType();
            GunUniqueMod currentUMod = data.getUniqueMod();

            if (gt == GunType.FLINTLOCK_MUSKET) {
                // A: Stalker
                List<String> sLore = new ArrayList<>();
                sLore.add("§7Stanie w bezruchu w pobliżu liści/krzaków aktywuje §2Kamuflaż (Niewidzialność)§7.");
                sLore.add("§7Zapewnia §a+35% obrażeń krytycznych§7 przy strzale z ukrycia.");
                sLore.add("§7Status: " + (currentUMod == GunUniqueMod.MUSKET_STALKER ? "§a✔ Zainstalowane (Aktywne)" : "§cBrak"));
                if (currentUMod != GunUniqueMod.MUSKET_STALKER) {
                    sLore.add("");
                    sLore.add("§6Koszt wyboru: §f4x Liście Dębu, 2x Skóra, 2x Złoto");
                    sLore.add("§eKliknij, aby wybrać ulepszenie A!");
                }
                inv.setItem(SLOT_MOD_UNIQUE_1, createItem(Material.OAK_LEAVES, "§2§l[Unikalne A] Stalker", sLore));

                // B: Piechur
                List<String> pLore = new ArrayList<>();
                pLore.add("§7Skraca czas ładowania broni o §e-1.0s§7.");
                pLore.add("§7Zapewnia efekt §bSpeed§7 na czas trwania naciągania broni.");
                pLore.add("§7Daje §a25% szansy na darmowe załadowanie§7 (brak zużycia kuli).");
                pLore.add("§7Status: " + (currentUMod == GunUniqueMod.MUSKET_INFANTRYMAN ? "§a✔ Zainstalowane (Aktywne)" : "§cBrak"));
                if (currentUMod != GunUniqueMod.MUSKET_INFANTRYMAN) {
                    pLore.add("");
                    pLore.add("§6Koszt wyboru: §f4x Żelazo, 2x Skóra, 2x Pióro");
                    pLore.add("§eKliknij, aby wybrać ulepszenie B!");
                }
                inv.setItem(SLOT_MOD_UNIQUE_2, createItem(Material.FEATHER, "§e§l[Unikalne B] Piechur", pLore));
                inv.setItem(SLOT_MOD_UNIQUE_3, createItem(Material.BLACK_STAINED_GLASS_PANE, " ", null));

            } else if (gt == GunType.FLINTLOCK_PISTOL) {
                // A: Szybki i Wściekły
                List<String> fLore = new ArrayList<>();
                fLore.add("§7Całkowicie usuwa karę prędkości poruszania się (§a0% spowolnienia§7).");
                fLore.add("§7Zwiększa trwałość broni o §a+75§7, zasięg o §b+10m§7 oraz obrażenia o §c+2.0 DMG§7.");
                fLore.add("§7Status: " + (currentUMod == GunUniqueMod.PISTOL_FAST_AND_FURIOUS ? "§a✔ Zainstalowane (Aktywne)" : "§cBrak"));
                if (currentUMod != GunUniqueMod.PISTOL_FAST_AND_FURIOUS) {
                    fLore.add("");
                    fLore.add("§6Koszt wyboru: §f4x Skóra, 2x Złoto, 2x Żelazo");
                    fLore.add("§eKliknij, aby wybrać ulepszenie A!");
                }
                inv.setItem(SLOT_MOD_UNIQUE_1, createItem(Material.FEATHER, "§a§l[Unikalne A] Szybki i Wściekły", fLore));

                // B: Łowca Czarownic
                List<String> wLore = new ArrayList<>();
                wLore.add("§7Skraca czas ładowania o §e-0.5s§7 oraz daje §4+40% obrażeń krytycznych§7.");
                wLore.add("§7Zabójstwa upuszczają §e2x więcej EXP§7, §b2x więcej łupów§7 i dają 35% szansy na drop kul!");
                wLore.add("§7Status: " + (currentUMod == GunUniqueMod.PISTOL_WITCH_HUNTER ? "§a✔ Zainstalowane (Aktywne)" : "§cBrak"));
                if (currentUMod != GunUniqueMod.PISTOL_WITCH_HUNTER) {
                    wLore.add("");
                    wLore.add("§6Koszt wyboru: §f4x Złoto, 2x Lapis Lazuli, 2x Krzemień");
                    wLore.add("§eKliknij, aby wybrać ulepszenie B!");
                }
                inv.setItem(SLOT_MOD_UNIQUE_2, createItem(Material.WITHER_ROSE, "§5§l[Unikalne B] Łowca Czarownic", wLore));

                // C: Wsparcie Emocjonalne
                List<String> eLore = new ArrayList<>();
                eLore.add("§7Trafienie wrogiem nakłada debuff §dWyczerpanie (Exhaustion)§7 na 5s.");
                eLore.add("§7Cel jest spowolniony (Slowness II) i otrzymuje §c+30% większe obrażenia z pistoletów§7!");
                eLore.add("§7Status: " + (currentUMod == GunUniqueMod.PISTOL_EMOTIONAL_SUPPORT ? "§a✔ Zainstalowane (Aktywne)" : "§cBrak"));
                if (currentUMod != GunUniqueMod.PISTOL_EMOTIONAL_SUPPORT) {
                    eLore.add("");
                    eLore.add("§6Koszt wyboru: §f2x Ametyst, 4x Miedź, 2x Czerwony Barwnik");
                    eLore.add("§eKliknij, aby wybrać ulepszenie C!");
                }
                inv.setItem(SLOT_MOD_UNIQUE_3, createItem(Material.POPPY, "§d§l[Unikalne C] Wsparcie Emocjonalne", eLore));

            } else if (gt == GunType.BLUNDERBUSS) {
                // A: Dubeltówka
                List<String> dLore = new ArrayList<>();
                dLore.add("§7Podwójna lufa – pozwala na załadowanie §e2 pocisków naraz§7.");
                dLore.add("§7Status: " + (currentUMod == GunUniqueMod.SHOTGUN_DOUBLE_BARREL ? "§a✔ Zainstalowane (Aktywne)" : "§cBrak"));
                if (currentUMod != GunUniqueMod.SHOTGUN_DOUBLE_BARREL) {
                    dLore.add("");
                    dLore.add("§6Koszt wyboru: §f6x Żelazo, 4x Miedź");
                    dLore.add("§eKliknij, aby wybrać ulepszenie A!");
                }
                inv.setItem(SLOT_MOD_UNIQUE_1, createItem(Material.CROSSBOW, "§6§l[Unikalne A] Dubeltówka", dLore));

                // B: Demolka
                List<String> demoLore = new ArrayList<>();
                demoLore.add("§7Wystrzeliwuje potężną salwę §c+4 do +6 dodatkowego śrutu§7 (+1s czasu ładowania).");
                demoLore.add("§7Smoczy śrut ma większy zasięg (+8m) i podpala teren 3x3!");
                demoLore.add("§7Pocisk Brenek (Slug) zyskuje 4 rozproszone śruciny po bokach.");
                demoLore.add("§7Status: " + (currentUMod == GunUniqueMod.SHOTGUN_DEMOLITION ? "§a✔ Zainstalowane (Aktywne)" : "§cBrak"));
                if (currentUMod != GunUniqueMod.SHOTGUN_DEMOLITION) {
                    demoLore.add("");
                    demoLore.add("§6Koszt wyboru: §f4x Proch Strzelniczy, 6x Żelazo, 2x Miedź");
                    demoLore.add("§eKliknij, aby wybrać ulepszenie B!");
                }
                inv.setItem(SLOT_MOD_UNIQUE_2, createItem(Material.TNT, "§c§l[Unikalne B] Demolka", demoLore));
                inv.setItem(SLOT_MOD_UNIQUE_3, createItem(Material.BLACK_STAINED_GLASS_PANE, " ", null));

            } else if (gt == GunType.PEPPERBOX) {
                // A: Żołnierz Doskonały
                List<String> zLore = new ArrayList<>();
                zLore.add("§7Powiększa magazynek do §e5 komór (+1 pocisk)§7.");
                zLore.add("§7Zmniejsza odrzut gracza o §b-50%§7 i redukuje dym z lufy.");
                zLore.add("§7Status: " + (currentUMod == GunUniqueMod.PEPPERBOX_PERFECT_SOLDIER ? "§a✔ Zainstalowane (Aktywne)" : "§cBrak"));
                if (currentUMod != GunUniqueMod.PEPPERBOX_PERFECT_SOLDIER) {
                    zLore.add("");
                    zLore.add("§6Koszt wyboru: §f4x Żelazo, 2x Miedź, 2x Redstone");
                    zLore.add("§eKliknij, aby wybrać ulepszenie A!");
                }
                inv.setItem(SLOT_MOD_UNIQUE_1, createItem(Material.IRON_CHESTPLATE, "§b§l[Unikalne A] Żołnierz Doskonały", zLore));

                // B: Huragan
                List<String> hLore = new ArrayList<>();
                hLore.add("§7Zmniejsza magazynek do 3 komór (+0.8s czasu ładowania).");
                hLore.add("§7Zabójstwo w głowę §eBŁYSKAWICZNIE ładuje 1 nabój do komory§7!");
                hLore.add("§7Status: " + (currentUMod == GunUniqueMod.PEPPERBOX_HURRICANE ? "§a✔ Zainstalowane (Aktywne)" : "§cBrak"));
                if (currentUMod != GunUniqueMod.PEPPERBOX_HURRICANE) {
                    hLore.add("");
                    hLore.add("§6Koszt wyboru: §f4x Złoto, 2x Szmaragd, 2x Pióro");
                    hLore.add("§eKliknij, aby wybrać ulepszenie B!");
                }
                inv.setItem(SLOT_MOD_UNIQUE_2, createItem(Material.FEATHER, "§3§l[Unikalne B] Huragan", hLore));
                inv.setItem(SLOT_MOD_UNIQUE_3, createItem(Material.BLACK_STAINED_GLASS_PANE, " ", null));
            }
        } else {
            inv.setItem(SLOT_MOD_RIFLING, createItem(Material.BARRIER, "§c§lWłóż broń palną", List.of("§7Umieść broń palną w gnieździe powyżej.")));
            inv.setItem(SLOT_MOD_LOCK, createItem(Material.BARRIER, "§c§lWłóż broń palną", List.of("§7Umieść broń palną w gnieździe powyżej.")));
            inv.setItem(SLOT_MOD_OPTIONAL, createItem(Material.BARRIER, "§c§lWłóż broń palną", List.of("§7Umieść broń palną w gnieździe powyżej.")));
            inv.setItem(SLOT_MOD_UNIQUE_1, createItem(Material.BARRIER, "§c§lWłóż broń palną", List.of("§7Umieść broń palną w gnieździe powyżej.")));
            inv.setItem(SLOT_MOD_UNIQUE_2, createItem(Material.BARRIER, "§c§lWłóż broń palną", List.of("§7Umieść broń palną w gnieździe powyżej.")));
            inv.setItem(SLOT_MOD_UNIQUE_3, createItem(Material.BLACK_STAINED_GLASS_PANE, " ", null));
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(GUI_TITLE)) return;

        int slot = event.getRawSlot();
        Player player = (Player) event.getWhoClicked();

        if (slot >= 0 && slot < 45) {
            if (slot != SLOT_GUN) {
                event.setCancelled(true);
            }

            Inventory inv = event.getInventory();
            ItemStack gunItem = inv.getItem(SLOT_GUN);
            GunData data = GunData.fromItemStack(gunItem);

            if (data != null) {
                if (slot == SLOT_REPAIR) {
                    handleRepair(player, inv, gunItem, data);
                } else if (slot == SLOT_UPGRADE_LEVEL) {
                    handleLevelUpgrade(player, inv, gunItem, data);
                } else if (slot == SLOT_MOD_RIFLING) {
                    handleModRifling(player, inv, gunItem, data);
                } else if (slot == SLOT_MOD_LOCK) {
                    handleModLock(player, inv, gunItem, data);
                } else if (slot == SLOT_MOD_OPTIONAL) {
                    handleModOptional(player, inv, gunItem, data);
                } else if (slot == SLOT_MOD_UNIQUE_1) {
                    handleUniqueModChoice(player, inv, gunItem, data, 1);
                } else if (slot == SLOT_MOD_UNIQUE_2) {
                    handleUniqueModChoice(player, inv, gunItem, data, 2);
                } else if (slot == SLOT_MOD_UNIQUE_3) {
                    handleUniqueModChoice(player, inv, gunItem, data, 3);
                }
            }
        }

        Bukkit.getScheduler().runTaskLater(AmonPackPlugin.plugin, () -> {
            if (event.getView().getTitle().equals(GUI_TITLE)) {
                updateGuiButtons(event.getInventory(), event.getInventory().getItem(SLOT_GUN));
            }
        }, 1L);
    }

    private void handleRepair(Player player, Inventory inv, ItemStack gunItem, GunData data) {
        int maxDur = data.getMaxDurability();
        int missingDur = maxDur - data.getCurrentDurability();
        if (missingDur <= 0) {
            player.sendMessage("§a[Rusznikarz] Ta broń nie wymaga naprawy.");
            return;
        }

        int ironCost = Math.max(1, (int) Math.ceil((double) missingDur / 60.0));
        if (player.getGameMode() == GameMode.CREATIVE || consumeItem(player, Material.IRON_INGOT, ironCost)) {
            data.setCurrentDurability(maxDur);
            data.applyToItemStack(gunItem);
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0f, 1.0f);
            player.sendMessage("§a[Rusznikarz] Broń została całkowicie odnowiona!");
        } else {
            player.sendMessage("§c[Rusznikarz] Brak materiałów! Potrzebujesz: " + ironCost + "x Sztabka Żelaza.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
    }

    private void handleLevelUpgrade(Player player, Inventory inv, ItemStack gunItem, GunData data) {
        if (data.getLevel() >= 5) {
            player.sendMessage("§a[Rusznikarz] Broń osiągnęła już maksymalny poziom mistrzowski!");
            return;
        }

        int costGold = data.getLevel() * 2;
        int costLvl = data.getLevel() * 3;

        if (player.getGameMode() == GameMode.CREATIVE || (player.getLevel() >= costLvl && hasItem(player, Material.GOLD_INGOT, costGold))) {
            if (player.getGameMode() != GameMode.CREATIVE) {
                consumeItem(player, Material.GOLD_INGOT, costGold);
                player.setLevel(player.getLevel() - costLvl);
            }
            data.setLevel(data.getLevel() + 1);
            data.applyToItemStack(gunItem);
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.2f);
            player.sendMessage("§6[Rusznikarz] §aPomyślnie ulepszono broń na Poziom " + data.getLevel() + "!");
        } else {
            player.sendMessage("§c[Rusznikarz] Wymagane: " + costGold + "x Złoto oraz " + costLvl + " poziomów EXP!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
    }

    private void handleModRifling(Player player, Inventory inv, ItemStack gunItem, GunData data) {
        if (data.hasRifling()) {
            player.sendMessage("§a[Rusznikarz] Gwintowana lufa jest już zamontowana!");
            return;
        }
        if (player.getGameMode() == GameMode.CREATIVE || (hasItem(player, Material.IRON_INGOT, 4) && hasItem(player, Material.COPPER_INGOT, 2))) {
            if (player.getGameMode() != GameMode.CREATIVE) {
                consumeItem(player, Material.IRON_INGOT, 4);
                consumeItem(player, Material.COPPER_INGOT, 2);
            }
            data.setRifling(true);
            data.applyToItemStack(gunItem);
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0f, 1.2f);
            player.sendMessage("§a[Rusznikarz] Zamontowano Gwintowaną Lufę (+30% celności, +10m zasięgu)!");
        } else {
            player.sendMessage("§c[Rusznikarz] Brak materiałów: 4x Żelazo, 2x Miedź.");
        }
    }

    private void handleModLock(Player player, Inventory inv, ItemStack gunItem, GunData data) {
        if (data.hasReinforcedLock()) {
            player.sendMessage("§a[Rusznikarz] Wzmocniony zamek jest już zamontowany!");
            return;
        }
        if (player.getGameMode() == GameMode.CREATIVE || (hasItem(player, Material.FLINT, 2) && hasItem(player, Material.IRON_INGOT, 2) && hasItem(player, Material.REDSTONE, 1))) {
            if (player.getGameMode() != GameMode.CREATIVE) {
                consumeItem(player, Material.FLINT, 2);
                consumeItem(player, Material.IRON_INGOT, 2);
                consumeItem(player, Material.REDSTONE, 1);
            }
            data.setReinforcedLock(true);
            data.applyToItemStack(gunItem);
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0f, 1.2f);
            player.sendMessage("§a[Rusznikarz] Zamontowano Wzmocniony Zamek (-30% czasu ładowania)!");
        } else {
            player.sendMessage("§c[Rusznikarz] Brak materiałów: 2x Krzemień, 2x Żelazo, 1x Redstone.");
        }
    }

    private void handleModOptional(Player player, Inventory inv, ItemStack gunItem, GunData data) {
        if (data.hasBayonet()) {
            player.sendMessage("§a[Rusznikarz] To ulepszenie jest już zainstalowane!");
            return;
        }

        if (data.getGunType() == GunType.FLINTLOCK_MUSKET) {
            if (player.getGameMode() == GameMode.CREATIVE || (hasItem(player, Material.IRON_INGOT, 2) && hasItem(player, Material.LEATHER, 1))) {
                if (player.getGameMode() != GameMode.CREATIVE) {
                    consumeItem(player, Material.IRON_INGOT, 2);
                    consumeItem(player, Material.LEATHER, 1);
                }
                data.setBayonet(true);
                data.applyToItemStack(gunItem);
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0f, 1.2f);
                player.sendMessage("§a[Rusznikarz] Zamontowano Bagnet Myśliwski (+7.0 DMG wręcz)!");
            } else {
                player.sendMessage("§c[Rusznikarz] Brak materiałów: 2x Żelazo, 1x Skóra.");
            }
        } else {
            if (player.getGameMode() == GameMode.CREATIVE || (hasItem(player, Material.LEATHER, 2) && hasItem(player, Material.COPPER_INGOT, 2))) {
                if (player.getGameMode() != GameMode.CREATIVE) {
                    consumeItem(player, Material.LEATHER, 2);
                    consumeItem(player, Material.COPPER_INGOT, 2);
                }
                data.setBayonet(true);
                data.applyToItemStack(gunItem);
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0f, 1.2f);
                player.sendMessage("§a[Rusznikarz] Zamontowano Ergonomiczne Łoże (-40% odrzutu)!");
            } else {
                player.sendMessage("§c[Rusznikarz] Brak materiałów: 2x Skóra, 2x Miedź.");
            }
        }
    }

    private void handleUniqueModChoice(Player player, Inventory inv, ItemStack gunItem, GunData data, int optionIndex) {
        GunType gt = data.getGunType();
        GunUniqueMod targetMod = GunUniqueMod.NONE;

        if (gt == GunType.FLINTLOCK_MUSKET) {
            if (optionIndex == 1) targetMod = GunUniqueMod.MUSKET_STALKER;
            else if (optionIndex == 2) targetMod = GunUniqueMod.MUSKET_INFANTRYMAN;
        } else if (gt == GunType.FLINTLOCK_PISTOL) {
            if (optionIndex == 1) targetMod = GunUniqueMod.PISTOL_FAST_AND_FURIOUS;
            else if (optionIndex == 2) targetMod = GunUniqueMod.PISTOL_WITCH_HUNTER;
            else if (optionIndex == 3) targetMod = GunUniqueMod.PISTOL_EMOTIONAL_SUPPORT;
        } else if (gt == GunType.BLUNDERBUSS) {
            if (optionIndex == 1) targetMod = GunUniqueMod.SHOTGUN_DOUBLE_BARREL;
            else if (optionIndex == 2) targetMod = GunUniqueMod.SHOTGUN_DEMOLITION;
        } else if (gt == GunType.PEPPERBOX) {
            if (optionIndex == 1) targetMod = GunUniqueMod.PEPPERBOX_PERFECT_SOLDIER;
            else if (optionIndex == 2) targetMod = GunUniqueMod.PEPPERBOX_HURRICANE;
        }

        if (targetMod == GunUniqueMod.NONE) return;

        if (data.getUniqueMod() == targetMod) {
            player.sendMessage("§a[Rusznikarz] To unikalne ulepszenie (" + targetMod.getDisplayName() + "§a) jest już zainstalowane na Twojej broni!");
            return;
        }

        boolean canCraft = false;

        if (player.getGameMode() == GameMode.CREATIVE) {
            canCraft = true;
        } else {
            switch (targetMod) {
                case MUSKET_STALKER:
                    if (hasItem(player, Material.OAK_LEAVES, 4) && hasItem(player, Material.LEATHER, 2) && hasItem(player, Material.GOLD_INGOT, 2)) {
                        consumeItem(player, Material.OAK_LEAVES, 4);
                        consumeItem(player, Material.LEATHER, 2);
                        consumeItem(player, Material.GOLD_INGOT, 2);
                        canCraft = true;
                    }
                    break;
                case MUSKET_INFANTRYMAN:
                    if (hasItem(player, Material.IRON_INGOT, 4) && hasItem(player, Material.LEATHER, 2) && hasItem(player, Material.FEATHER, 2)) {
                        consumeItem(player, Material.IRON_INGOT, 4);
                        consumeItem(player, Material.LEATHER, 2);
                        consumeItem(player, Material.FEATHER, 2);
                        canCraft = true;
                    }
                    break;
                case PISTOL_FAST_AND_FURIOUS:
                    if (hasItem(player, Material.LEATHER, 4) && hasItem(player, Material.GOLD_INGOT, 2) && hasItem(player, Material.IRON_INGOT, 2)) {
                        consumeItem(player, Material.LEATHER, 4);
                        consumeItem(player, Material.GOLD_INGOT, 2);
                        consumeItem(player, Material.IRON_INGOT, 2);
                        canCraft = true;
                    }
                    break;
                case PISTOL_WITCH_HUNTER:
                    if (hasItem(player, Material.GOLD_INGOT, 4) && hasItem(player, Material.LAPIS_LAZULI, 2) && hasItem(player, Material.FLINT, 2)) {
                        consumeItem(player, Material.GOLD_INGOT, 4);
                        consumeItem(player, Material.LAPIS_LAZULI, 2);
                        consumeItem(player, Material.FLINT, 2);
                        canCraft = true;
                    }
                    break;
                case PISTOL_EMOTIONAL_SUPPORT:
                    if (hasItem(player, Material.AMETHYST_SHARD, 2) && hasItem(player, Material.COPPER_INGOT, 4) && hasItem(player, Material.RED_DYE, 2)) {
                        consumeItem(player, Material.AMETHYST_SHARD, 2);
                        consumeItem(player, Material.COPPER_INGOT, 4);
                        consumeItem(player, Material.RED_DYE, 2);
                        canCraft = true;
                    }
                    break;
                case SHOTGUN_DOUBLE_BARREL:
                    if (hasItem(player, Material.IRON_INGOT, 6) && hasItem(player, Material.COPPER_INGOT, 4)) {
                        consumeItem(player, Material.IRON_INGOT, 6);
                        consumeItem(player, Material.COPPER_INGOT, 4);
                        canCraft = true;
                    }
                    break;
                case SHOTGUN_DEMOLITION:
                    if (hasItem(player, Material.GUNPOWDER, 4) && hasItem(player, Material.IRON_INGOT, 6) && hasItem(player, Material.COPPER_INGOT, 2)) {
                        consumeItem(player, Material.GUNPOWDER, 4);
                        consumeItem(player, Material.IRON_INGOT, 6);
                        consumeItem(player, Material.COPPER_INGOT, 2);
                        canCraft = true;
                    }
                    break;
                case PEPPERBOX_PERFECT_SOLDIER:
                    if (hasItem(player, Material.IRON_INGOT, 4) && hasItem(player, Material.COPPER_INGOT, 2) && hasItem(player, Material.REDSTONE, 2)) {
                        consumeItem(player, Material.IRON_INGOT, 4);
                        consumeItem(player, Material.COPPER_INGOT, 2);
                        consumeItem(player, Material.REDSTONE, 2);
                        canCraft = true;
                    }
                    break;
                case PEPPERBOX_HURRICANE:
                    if (hasItem(player, Material.GOLD_INGOT, 4) && hasItem(player, Material.EMERALD, 2) && hasItem(player, Material.FEATHER, 2)) {
                        consumeItem(player, Material.GOLD_INGOT, 4);
                        consumeItem(player, Material.EMERALD, 2);
                        consumeItem(player, Material.FEATHER, 2);
                        canCraft = true;
                    }
                    break;
                default:
                    break;
            }
        }

        if (canCraft) {
            data.setUniqueMod(targetMod);
            data.applyToItemStack(gunItem);
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0f, 1.4f);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.8f);
            player.sendMessage("§a[Rusznikarz] Pomyślnie zamontowano unikalne ulepszenie: " + targetMod.getDisplayName() + "§a!");
        } else {
            player.sendMessage("§c[Rusznikarz] Brak wymaganych materiałów na wybrane ulepszenie!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!event.getView().getTitle().equals(GUI_TITLE)) return;

        Inventory inv = event.getInventory();
        ItemStack gunItem = inv.getItem(SLOT_GUN);
        if (gunItem != null && gunItem.getType() != Material.AIR) {
            Player player = (Player) event.getPlayer();
            player.getInventory().addItem(gunItem).forEach((index, item) -> player.getWorld().dropItem(player.getLocation(), item));
            inv.setItem(SLOT_GUN, null);
        }
    }

    private static ItemStack createItem(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null) {
                meta.setLore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private static boolean hasItem(Player player, Material mat, int amount) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == mat) {
                count += item.getAmount();
                if (count >= amount) return true;
            }
        }
        return false;
    }

    private static boolean consumeItem(Player player, Material mat, int amount) {
        if (!hasItem(player, mat, amount)) return false;
        int remaining = amount;
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && item.getType() == mat) {
                int take = Math.min(item.getAmount(), remaining);
                item.setAmount(item.getAmount() - take);
                if (item.getAmount() <= 0) {
                    player.getInventory().setItem(i, null);
                }
                remaining -= take;
                if (remaining <= 0) break;
            }
        }
        return true;
    }
}

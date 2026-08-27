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

public class ArcaneAltarGui implements InventoryHolder {

    private final Player player;
    private final ItemStack mainHandItem;
    private final SpellRegistry spellRegistry;
    private final ManaManager manaManager;
    private final Inventory inventory;

    public ArcaneAltarGui(Player player, ItemStack mainHandItem, SpellRegistry spellRegistry, ManaManager manaManager) {
        this.player = player;
        this.mainHandItem = mainHandItem;
        this.spellRegistry = spellRegistry;
        this.manaManager = manaManager;
        this.inventory = Bukkit.createInventory(this, 27, ChatColor.DARK_PURPLE + "✦ OŁTARZ ARKANÓW ✦");
        buildGui();
    }

    private void buildGui() {
        inventory.clear();
        ItemStack filler = ProgressionMenuGui.createItem(Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, filler);
        }

        boolean isTome = isMagicItem(mainHandItem);
        boolean isAir = MagicItemManager.isAirWand(mainHandItem);

        // Slot 4: Info o trzymanym przedmiocie
        if (isTome) {
            int level = MagicItemManager.getTomeLevel(mainHandItem);
            int kills = MagicItemManager.getTomeKills(mainHandItem);
            List<String> lore = List.of(
                    "§7Aktualnie trzymany przedmiot magiczny:",
                    "§c§l" + (mainHandItem.hasItemMeta() && mainHandItem.getItemMeta().hasDisplayName() ? mainHandItem.getItemMeta().getDisplayName() : (isAir ? "Różdżka Fenów" : "Tom Magii")),
                    "",
                    "§6✦ Poziom: §e" + level + " §8| §c⚔ Zabójstwa: §f" + kills,
                    "§a✔ Gotowy do ulepszenia lub rozwoju czarów!"
            );
            inventory.setItem(4, ProgressionMenuGui.createItem(mainHandItem.getType(), "§5§l✦ UMIESZCZONY PRZEDMIOT ✦", lore));
        } else {
            List<String> lore = List.of(
                    "§cTrzymaj w głównej dłoni magiczny przedmiot (np. Tom Ognia lub Różdżkę Fenów),",
                    "§caby móc korzystać z funkcji Ołtarza Arkanów!"
            );
            inventory.setItem(4, ProgressionMenuGui.createItem(Material.BARRIER, "§c§lBrak Magicznego Przedmiotu", lore));
        }

        // Slot 11: [ ULEPSZ PRZEDMIOT ]
        if (isTome) {
            int level = MagicItemManager.getTomeLevel(mainHandItem);
            int kills = MagicItemManager.getTomeKills(mainHandItem);
            List<String> upgradeLore = new ArrayList<>();

            if (isAir) {
                if (level == 1) {
                    upgradeLore.add("§7Awansuj różdżkę na §bPoziom II (Władca Wichrów)§7.");
                    upgradeLore.add("§7Odblokowuje zaklęcia: §fAirblade §7oraz §bWir Powietrza§7.");
                    upgradeLore.add("");
                    upgradeLore.add("§eWymagania awansu:");
                    upgradeLore.add((kills >= 5 ? "§a✔" : "§c❌") + " §7Wymagane zabójstwa magią: §f" + kills + "/5");
                    upgradeLore.add((player.getInventory().containsAtLeast(new ItemStack(Material.ROTTEN_FLESH), 10) ? "§a✔" : "§c❌") + " §710x Zgniłe Mięso");
                    upgradeLore.add((player.getInventory().containsAtLeast(new ItemStack(Material.FEATHER), 4) ? "§a✔" : "§c❌") + " §74x Pióro");
                    upgradeLore.add((player.getLevel() >= 5 ? "§a✔" : "§c❌") + " §75 Poziomów Doświadczenia (EXP)");
                    upgradeLore.add("");
                    boolean canUpgrade = kills >= 5 && player.getInventory().containsAtLeast(new ItemStack(Material.ROTTEN_FLESH), 10) && player.getInventory().containsAtLeast(new ItemStack(Material.FEATHER), 4) && player.getLevel() >= 5;
                    if (canUpgrade) {
                        upgradeLore.add("§a✦ Kliknij, aby ulepszyć różdżkę do Poziomu II!");
                    } else {
                        upgradeLore.add("§c❌ Nie spełniasz wszystkich wymagań!");
                    }
                    inventory.setItem(11, ProgressionMenuGui.createItem(Material.ANVIL, "§b§l[ ✦ ULEPSZ RÓŻDŻKĘ -> POZIOM II ]", upgradeLore));
                } else {
                    upgradeLore.add("§aTen przedmiot osiągnął już maksymalny poziom mistrzostwa!");
                    inventory.setItem(11, ProgressionMenuGui.createItem(Material.NETHER_STAR, "§a§l[ ✦ MAKSYMALNY POZIOM ✦ ]", upgradeLore));
                }
            } else {
                if (level == 1) {
                    upgradeLore.add("§7Awansuj swój tom na §6Poziom II (Adept Ognia)§7.");
                    upgradeLore.add("§7Odblokowuje zaklęcia: §cBlazing §7oraz §6Fire Circle§7.");
                    upgradeLore.add("");
                    upgradeLore.add("§eWymagania awansu:");
                    upgradeLore.add((kills >= 5 ? "§a✔" : "§c❌") + " §7Wymagane zabójstwa magią: §f" + kills + "/5");
                    upgradeLore.add((player.getInventory().containsAtLeast(new ItemStack(Material.BLAZE_POWDER), 2) ? "§a✔" : "§c❌") + " §72x Płomienny Proszek (Blaze Powder)");
                    upgradeLore.add((player.getInventory().containsAtLeast(new ItemStack(Material.FIRE_CHARGE), 1) ? "§a✔" : "§c❌") + " §71x Ognista Kula (Fire Charge)");
                    upgradeLore.add((player.getLevel() >= 5 ? "§a✔" : "§c❌") + " §75 Poziomów Doświadczenia (EXP)");
                    upgradeLore.add("");
                    boolean canUpgrade = kills >= 5 && player.getInventory().containsAtLeast(new ItemStack(Material.BLAZE_POWDER), 2) && player.getInventory().containsAtLeast(new ItemStack(Material.FIRE_CHARGE), 1) && player.getLevel() >= 5;
                    if (canUpgrade) {
                        upgradeLore.add("§a✦ Kliknij, aby ulepszyć tom do Poziomu II!");
                    } else {
                        upgradeLore.add("§c❌ Nie spełniasz wszystkich wymagań!");
                    }
                    inventory.setItem(11, ProgressionMenuGui.createItem(Material.ANVIL, "§6§l[ ✦ ULEPSZ PRZEDMIOT -> POZIOM II ]", upgradeLore));
                } else if (level == 2) {
                    upgradeLore.add("§7Awansuj swój tom na §6Poziom III (Mistrz Płomieni)§7.");
                    upgradeLore.add("§7Odblokowuje zaklęcia: §4FlashPoint §7oraz §cBarrage§7.");
                    upgradeLore.add("");
                    upgradeLore.add("§eWymagania awansu:");
                    upgradeLore.add((kills >= 15 ? "§a✔" : "§c❌") + " §7Wymagane zabójstwa magią: §f" + kills + "/15");
                    upgradeLore.add((player.getInventory().containsAtLeast(new ItemStack(Material.BLAZE_ROD), 4) ? "§a✔" : "§c❌") + " §74x Płomienna Różdżka (Blaze Rod)");
                    upgradeLore.add((player.getInventory().containsAtLeast(new ItemStack(Material.MAGMA_BLOCK), 2) ? "§a✔" : "§c❌") + " §72x Blok Magmy");
                    upgradeLore.add((player.getLevel() >= 10 ? "§a✔" : "§c❌") + " §710 Poziomów Doświadczenia (EXP)");
                    upgradeLore.add("");
                    boolean canUpgrade = kills >= 15 && player.getInventory().containsAtLeast(new ItemStack(Material.BLAZE_ROD), 4) && player.getInventory().containsAtLeast(new ItemStack(Material.MAGMA_BLOCK), 2) && player.getLevel() >= 10;
                    if (canUpgrade) {
                        upgradeLore.add("§a✦ Kliknij, aby ulepszyć tom do Poziomu III!");
                    } else {
                        upgradeLore.add("§c❌ Nie spełniasz wszystkich wymagań!");
                    }
                    inventory.setItem(11, ProgressionMenuGui.createItem(Material.NETHER_STAR, "§6§l[ ✦ ULEPSZ PRZEDMIOT -> POZIOM III ]", upgradeLore));
                } else {
                    upgradeLore.add("§aTen przedmiot osiągnął już maksymalny poziom mistrzostwa!");
                    inventory.setItem(11, ProgressionMenuGui.createItem(Material.NETHER_STAR, "§a§l[ ✦ MAKSYMALNY POZIOM ✦ ]", upgradeLore));
                }
            }
        } else {
            inventory.setItem(11, ProgressionMenuGui.createItem(Material.GRAY_DYE, "§7[ Ulepsz Przedmiot - Wymagany Magiczny Przedmiot ]", List.of("§7Włóż lub trzymaj przedmiot w dłoni.")));
        }

        // Slot 15: [ ROZWIJAJ MAGIĘ ]
        if (isTome) {
            List<String> treeLore = List.of(
                    "§7Otwiera drzewko ulepszeń zaklęć tego przedmiotu.",
                    "§7Pozwala na zakup trwałych ulepszeń:",
                    "§b✦ Mniejszy koszt many (-10 MP)",
                    "§e✦ Mniejszy cooldown (-1.0s)",
                    "§6✦ Wielokrotny pocisk (Multi-Cast)",
                    "§4✦ Ognisty Łańcuch (Rykoszet na Stan Ognia)",
                    "",
                    "§d✦ Kliknij, aby otworzyć Drzewko Magii!"
            );
            inventory.setItem(15, ProgressionMenuGui.createItem(Material.ENCHANTING_TABLE, "§d§l[ ✧ ROZWIJAJ MAGIĘ ✧ ]", treeLore));
        } else {
            inventory.setItem(15, ProgressionMenuGui.createItem(Material.GRAY_DYE, "§7[ Rozwijaj Magię - Wymagany Magiczny Przedmiot ]", List.of("§7Włóż lub trzymaj przedmiot w dłoni.")));
        }

        // Slot 22: Zamknij
        inventory.setItem(22, ProgressionMenuGui.createItem(Material.BARRIER, "§c§lZamknij", List.of("§7Kliknij, aby zamknąć ołtarz.")));
    }

    private boolean isMagicItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        if (MagicItemManager.isAirWand(item)) return true;
        if (item.getType() == Material.BOOK || item.getType() == Material.ENCHANTED_BOOK) return true;
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName() && (item.getItemMeta().getDisplayName().toLowerCase().contains("tom") || item.getItemMeta().getDisplayName().toLowerCase().contains("tome") || item.getItemMeta().getDisplayName().toLowerCase().contains("różdżka") || item.getItemMeta().getDisplayName().toLowerCase().contains("fen"))) return true;
        if (item.hasItemMeta() && item.getItemMeta().hasCustomModelData() && (item.getItemMeta().getCustomModelData() == 20001 || item.getItemMeta().getCustomModelData() == 20002)) return true;
        return false;
    }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();

        if (slot == 22) {
            player.closeInventory();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.0f);
            return;
        }

        if (!isMagicItem(mainHandItem)) return;

        if (slot == 11) {
            int level = MagicItemManager.getTomeLevel(mainHandItem);
            int kills = MagicItemManager.getTomeKills(mainHandItem);
            boolean isAir = MagicItemManager.isAirWand(mainHandItem);

            if (isAir) {
                if (level == 1) {
                    boolean hasItems = player.getInventory().containsAtLeast(new ItemStack(Material.ROTTEN_FLESH), 10) && player.getInventory().containsAtLeast(new ItemStack(Material.FEATHER), 4);
                    boolean hasExp = player.getLevel() >= 5;
                    if (kills >= 5 && hasItems && hasExp) {
                        player.getInventory().removeItem(new ItemStack(Material.ROTTEN_FLESH, 10));
                        player.getInventory().removeItem(new ItemStack(Material.FEATHER, 4));
                        player.setLevel(player.getLevel() - 5);

                        MagicItemManager.setTomeLevel(mainHandItem, 2);

                        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.2f);
                        player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.0f);
                        player.sendMessage("§6§l✦ AWANS! §aTwoja Różdżka Fenów została ulepszona na §bPoziom II (Władca Wichrów)§a!");
                        player.sendMessage("§dOdblokowano nowe zaklęcia: §fAirblade §doraz §bWir Powietrza§d!");

                        buildGui();
                    } else {
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                        player.sendMessage("§cNie spełniasz wymagań do ulepszenia różdżki!");
                    }
                }
                return;
            }

            if (level == 1) {
                boolean hasItems = player.getInventory().containsAtLeast(new ItemStack(Material.BLAZE_POWDER), 2) && player.getInventory().containsAtLeast(new ItemStack(Material.FIRE_CHARGE), 1);
                boolean hasExp = player.getLevel() >= 5;
                if (kills >= 5 && hasItems && hasExp) {
                    player.getInventory().removeItem(new ItemStack(Material.BLAZE_POWDER, 2));
                    player.getInventory().removeItem(new ItemStack(Material.FIRE_CHARGE, 1));
                    player.setLevel(player.getLevel() - 5);

                    MagicItemManager.setTomeLevel(mainHandItem, 2);

                    player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.2f);
                    player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.0f);
                    player.sendMessage("§6§l✦ AWANS! §aTwój Tom Ognia został ulepszony na §ePoziom II (Adept Ognia)§a!");
                    player.sendMessage("§dOdblokowano nowe zaklęcia: §cBlazing §doraz §6Fire Circle§d!");

                    buildGui();
                } else {
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    player.sendMessage("§cNie spełniasz wymagań do ulepszenia tomu!");
                }
            } else if (level == 2) {
                boolean hasItems = player.getInventory().containsAtLeast(new ItemStack(Material.BLAZE_ROD), 4) && player.getInventory().containsAtLeast(new ItemStack(Material.MAGMA_BLOCK), 2);
                boolean hasExp = player.getLevel() >= 10;
                if (kills >= 15 && hasItems && hasExp) {
                    player.getInventory().removeItem(new ItemStack(Material.BLAZE_ROD, 4));
                    player.getInventory().removeItem(new ItemStack(Material.MAGMA_BLOCK, 2));
                    player.setLevel(player.getLevel() - 10);

                    MagicItemManager.setTomeLevel(mainHandItem, 3);

                    player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                    player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.0f);
                    player.sendMessage("§6§l✦ MISTRZOSTWO! §aTwój Tom Ognia osiągnął §4Poziom III (Mistrz Płomieni)§a!");
                    player.sendMessage("§dOdblokowano arcymagiczne zaklęcia: §4FlashPoint §doraz §cBarrage§d!");

                    buildGui();
                } else {
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    player.sendMessage("§cNie spełniasz wymagań do ulepszenia tomu!");
                }
            }
        } else if (slot == 15) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
            player.openInventory(new SpellUpgradeTreeGui(player, mainHandItem, spellRegistry, manaManager).getInventory());
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}

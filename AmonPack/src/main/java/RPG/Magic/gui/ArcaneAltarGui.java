package RPG.Magic.gui;

import Plugin.AmonPackPlugin;
import RPG.Crafting.CraftingMenager;
import RPG.Magic.manager.MagicItemManager;
import RPG.Magic.manager.ManaManager;
import RPG.Magic.manager.SpellRegistry;
import RPG.Progression.gui.ProgressionMenuGui;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

        boolean isMagic = MagicItemManager.isMagicItem(mainHandItem);
        String itemId = MagicItemManager.getMagicItemId(mainHandItem);

        // Slot 4: Info o trzymanym przedmiocie
        if (isMagic && itemId != null) {
            int level = MagicItemManager.getTomeLevel(mainHandItem);
            int kills = MagicItemManager.getTomeKills(mainHandItem);
            String displayName = (mainHandItem.hasItemMeta() && mainHandItem.getItemMeta().hasDisplayName()) ?
                    mainHandItem.getItemMeta().getDisplayName() : formatItemName(itemId);

            List<String> lore = List.of(
                    "§7Aktualnie umieszczony w ołtarzu:",
                    displayName,
                    "",
                    "§6✦ Poziom: §e" + level + " §8| §c⚔ Zabójstwa: §f" + kills,
                    "§a✔ Gotowy do awansu lub ulepszania zaklęć!"
            );
            inventory.setItem(4, ProgressionMenuGui.createItem(mainHandItem.getType(), "§5§l✦ UMIESZCZONY PRZEDMIOT ✦", lore));
        } else {
            List<String> lore = List.of(
                    "§cTrzymaj w głównej dłoni magiczny przedmiot (np. Tom Ognia, Różdżkę lub Laskę),",
                    "§caby móc korzystać z funkcji Ołtarza Arkanów!"
            );
            inventory.setItem(4, ProgressionMenuGui.createItem(Material.BARRIER, "§c§lBrak Magicznego Przedmiotu", lore));
        }

        // Slot 11: [ ULEPSZ PRZEDMIOT ]
        if (isMagic && itemId != null) {
            int currentLevel = MagicItemManager.getTomeLevel(mainHandItem);
            int kills = MagicItemManager.getTomeKills(mainHandItem);
            int nextLevel = currentLevel + 1;

            FileConfiguration cfg = AmonPackPlugin.magicConfig;
            String levelPath = "magic.items." + itemId + ".levels." + nextLevel;

            if (cfg != null && cfg.contains(levelPath)) {
                String nextName = cfg.getString(levelPath + ".name", "Poziom " + nextLevel);
                int reqKills = cfg.getInt(levelPath + ".req_kills", 0);
                int reqExp = cfg.getInt(levelPath + ".req_exp_levels", 0);
                List<ItemStack> reqItems = loadRequiredItems(cfg, levelPath + ".req_items");

                List<String> unlockedPrimary = cfg.getStringList(levelPath + ".unlocked_primary");
                List<String> unlockedSecondary = cfg.getStringList(levelPath + ".unlocked_secondary");

                List<String> upgradeLore = new ArrayList<>();
                upgradeLore.add("§7Awansuj przedmiot na " + nextName + "§7.");
                if (!unlockedPrimary.isEmpty() || !unlockedSecondary.isEmpty()) {
                    List<String> names = new ArrayList<>();
                    for (String s : unlockedPrimary) names.add("§f" + MagicItemManager.formatSpellName(s));
                    for (String s : unlockedSecondary) names.add("§b" + MagicItemManager.formatSpellName(s));
                    upgradeLore.add("§7Odblokowuje zaklęcia: " + String.join("§7, ", names));
                }
                upgradeLore.add("");
                upgradeLore.add("§eWymagania awansu:");

                boolean allReqsMet = true;
                boolean isCreative = player.getGameMode() == org.bukkit.GameMode.CREATIVE;

                if (isCreative) {
                    upgradeLore.add("§a✔ [Tryb Kreatywny] Wszystkie wymagania są pomijane!");
                } else {
                    if (reqKills > 0) {
                        boolean killsMet = kills >= reqKills;
                        if (!killsMet) allReqsMet = false;
                        upgradeLore.add((killsMet ? "§a✔" : "§c❌") + " §7Wymagane zabójstwa magią: §f" + kills + "/" + reqKills);
                    }

                    for (ItemStack req : reqItems) {
                        int hasAmount = CraftingMenager.countMatchingItems(player, req);
                        boolean itemMet = hasAmount >= req.getAmount();
                        if (!itemMet) allReqsMet = false;
                        upgradeLore.add((itemMet ? "§a✔" : "§c❌") + " §7" + req.getAmount() + "x " + formatMaterialName(req.getType()));
                    }

                    if (reqExp > 0) {
                        boolean expMet = player.getLevel() >= reqExp;
                        if (!expMet) allReqsMet = false;
                        upgradeLore.add((expMet ? "§a✔" : "§c❌") + " §7" + reqExp + " Poziomów Doświadczenia (EXP)");
                    }
                }

                upgradeLore.add("");
                if (allReqsMet || isCreative) {
                    upgradeLore.add("§a✦ Kliknij, aby ulepszyć przedmiot do Poziomu " + nextLevel + "!");
                } else {
                    upgradeLore.add("§c❌ Nie spełniasz wszystkich wymagań!");
                }

                inventory.setItem(11, ProgressionMenuGui.createItem(Material.ANVIL, "§6§l[ ✦ ULEPSZ PRZEDMIOT -> POZIOM " + nextLevel + " ]", upgradeLore));
            } else {
                List<String> maxLore = List.of("§aTen przedmiot osiągnął już maksymalny poziom mistrzostwa!");
                inventory.setItem(11, ProgressionMenuGui.createItem(Material.NETHER_STAR, "§a§l[ ✦ MAKSYMALNY POZIOM ✦ ]", maxLore));
            }
        } else {
            inventory.setItem(11, ProgressionMenuGui.createItem(Material.GRAY_DYE, "§7[ Ulepsz Przedmiot - Wymagany Magiczny Przedmiot ]", List.of("§7Włóż lub trzymaj przedmiot w dłoni.")));
        }

        // Slot 15: [ ROZWIJAJ MAGIĘ ]
        if (isMagic && itemId != null) {
            List<String> treeLore = List.of(
                    "§7Otwiera drzewko ulepszeń zaklęć tego przedmiotu.",
                    "§7Pozwala na zakup trwałych ulepszeń:",
                    "§b✦ Mniejszy koszt many",
                    "§e✦ Krótszy czas odnowienia (cooldown)",
                    "§6✦ Zwiększone obrażenia i dodatkowe efekty czarów",
                    "",
                    "§d✦ Kliknij, aby otworzyć Drzewko Zaklęć!"
            );
            inventory.setItem(15, ProgressionMenuGui.createItem(Material.ENCHANTING_TABLE, "§d§l[ ✧ ROZWIJAJ ZAKLĘCIA ✧ ]", treeLore));
        } else {
            inventory.setItem(15, ProgressionMenuGui.createItem(Material.GRAY_DYE, "§7[ Rozwijaj Zaklęcia - Wymagany Magiczny Przedmiot ]", List.of("§7Włóż lub trzymaj przedmiot w dłoni.")));
        }

        // Slot 22: Zamknij
        inventory.setItem(22, ProgressionMenuGui.createItem(Material.BARRIER, "§c§lZamknij", List.of("§7Kliknij, aby zamknąć ołtarz.")));
    }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();

        if (slot == 22) {
            player.closeInventory();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.0f);
            return;
        }

        if (!MagicItemManager.isMagicItem(mainHandItem)) return;
        String itemId = MagicItemManager.getMagicItemId(mainHandItem);
        if (itemId == null) return;

        if (slot == 11) {
            int currentLevel = MagicItemManager.getTomeLevel(mainHandItem);
            int kills = MagicItemManager.getTomeKills(mainHandItem);
            int nextLevel = currentLevel + 1;

            FileConfiguration cfg = AmonPackPlugin.magicConfig;
            String levelPath = "magic.items." + itemId + ".levels." + nextLevel;

            if (cfg != null && cfg.contains(levelPath)) {
                String nextName = cfg.getString(levelPath + ".name", "Poziom " + nextLevel);
                int reqKills = cfg.getInt(levelPath + ".req_kills", 0);
                int reqExp = cfg.getInt(levelPath + ".req_exp_levels", 0);
                List<ItemStack> reqItems = loadRequiredItems(cfg, levelPath + ".req_items");

                boolean isCreative = player.getGameMode() == org.bukkit.GameMode.CREATIVE;
                boolean allReqsMet = isCreative;

                if (!isCreative) {
                    allReqsMet = true;
                    if (kills < reqKills) allReqsMet = false;
                    if (player.getLevel() < reqExp) allReqsMet = false;
                    for (ItemStack req : reqItems) {
                        if (CraftingMenager.countMatchingItems(player, req) < req.getAmount()) {
                            allReqsMet = false;
                            break;
                        }
                    }
                }

                if (allReqsMet) {
                    if (!isCreative) {
                        CraftingMenager.HaveItems(player, true, reqItems);
                        if (reqExp > 0) {
                            player.setLevel(player.getLevel() - reqExp);
                        }
                    }

                    MagicItemManager.setTomeLevel(mainHandItem, nextLevel);

                    player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.2f);
                    player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.0f);
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
                    player.sendMessage("§6§l✦ AWANS PRZEDMIOTU! §aTwój magiczny przedmiot awansował na " + nextName + "§a!");

                    buildGui();
                } else {
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    player.sendMessage("§cNie spełniasz wszystkich wymagań do ulepszenia tego przedmiotu!");
                }
            }
        } else if (slot == 15) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
            player.openInventory(new SpellSelectUpgradeGui(player, mainHandItem, spellRegistry, manaManager).getInventory());
        }
    }

    private List<ItemStack> loadRequiredItems(FileConfiguration cfg, String path) {
        List<ItemStack> items = new ArrayList<>();
        if (!cfg.contains(path)) return items;

        List<Map<?, ?>> list = cfg.getMapList(path);
        for (Map<?, ?> entry : list) {
            Object matObj = entry.get("material");
            Object amountObj = entry.get("amount");
            if (matObj != null && amountObj != null) {
                Material mat = Material.matchMaterial(matObj.toString());
                int amount = ((Number) amountObj).intValue();
                if (mat != null && amount > 0) {
                    items.add(new ItemStack(mat, amount));
                }
            }
        }
        return items;
    }

    private String formatItemName(String itemId) {
        return switch (itemId.toLowerCase()) {
            case "tome_fire" -> "§c§lTom Ognia";
            case "wand_fen" -> "§b§lRóżdżka Fenów";
            case "staff_lightning" -> "§e§lLaska Błyskawic";
            case "wand_water" -> "§9§lRóżdżka Wody";
            default -> "§d§lPrzedmiot Magiczny";
        };
    }

    private String formatMaterialName(Material mat) {
        return switch (mat) {
            case BLAZE_POWDER -> "Płomienny Proszek (Blaze Powder)";
            case FIRE_CHARGE -> "Ognista Kula (Fire Charge)";
            case BLAZE_ROD -> "Płomienna Różdżka (Blaze Rod)";
            case MAGMA_BLOCK -> "Blok Magmy";
            case FEATHER -> "Pióro";
            case PHANTOM_MEMBRANE -> "Błona Fantoma";
            case COPPER_INGOT -> "Sztabka Miedzi";
            case LIGHTNING_ROD -> "Piorunochron";
            case PRISMARINE_SHARD -> "Odłamek Pryzmarynu";
            case ICE -> "Lód";
            case HEART_OF_THE_SEA -> "Serce Oceanu";
            case PRISMARINE_CRYSTALS -> "Kryształy Pryzmarynu";
            default -> mat.name().replace("_", " ").toLowerCase();
        };
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}

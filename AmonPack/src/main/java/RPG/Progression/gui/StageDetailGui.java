package RPG.Progression.gui;

import RPG.Progression.ProgressionManager;
import RPG.Progression.model.PlayerProgressionData;
import RPG.Progression.model.Quest;
import RPG.Progression.model.QuestCategory;
import RPG.Progression.model.StageType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class StageDetailGui implements InventoryHolder {

    private final Player player;
    private final PlayerProgressionData data;
    private final StageType stage;
    private final Inventory inventory;
    private int page = 0;
    private final List<Quest> allQuests;

    public StageDetailGui(Player player, PlayerProgressionData data, StageType stage) {
        this.player = player;
        this.data = data;
        this.stage = stage;
        this.allQuests = new ArrayList<>(ProgressionManager.getInstance().getQuestRegistry().getQuestsForStage(stage));
        this.inventory = Bukkit.createInventory(this, 54, ChatColor.DARK_GRAY + "Zadania: " + stage.getDisplayName());
        buildGui();
    }

    private void buildGui() {
        inventory.clear();

        // Fill background borders
        ItemStack filler = ProgressionMenuGui.createItem(Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 9; i++) inventory.setItem(i, filler);
        for (int i = 45; i < 54; i++) inventory.setItem(i, filler);

        // Header Info (Slot 4)
        int completedReq = ProgressionManager.getInstance().getStageService().getCompletedRequiredQuestsCount(data, stage);
        int totalReq = ProgressionManager.getInstance().getStageService().getRequiredQuestsCount(stage);
        double pct = totalReq > 0 ? ((double) completedReq / totalReq) * 100.0 : 100.0;

        List<String> infoLore = Arrays.asList(
                stage.getDescription(),
                "",
                "§7Ukończone wymagane zadania: §e" + completedReq + "§7/§e" + totalReq + " §8(" + String.format("%.0f", pct) + "%)",
                "§f" + ProgressionMenuGui.buildProgressBar(completedReq, totalReq, 10),
                "",
                "§7Aby przejść do kolejnego etapu, musisz",
                "§7ukończyć wszystkie zadania oznaczone jako §c[WYMAGANE]§7."
        );
        inventory.setItem(4, ProgressionMenuGui.createItem(stage.getIconMaterial(), stage.getDisplayName(), infoLore));

        // Back button (Slot 45)
        inventory.setItem(45, ProgressionMenuGui.createItem(Material.ARROW, "§e§l◀ Powrót do Etapów", List.of("§7Kliknij, aby wrócić do menu głównego.")));

        // Navigation (Slots 48 and 50)
        int maxPerPage = 28;
        int totalPages = (int) Math.ceil((double) allQuests.size() / maxPerPage);
        if (totalPages == 0) totalPages = 1;

        if (page > 0) {
            inventory.setItem(48, ProgressionMenuGui.createItem(Material.PAPER, "§e◀ Poprzednia strona (" + page + ")", null));
        }
        if (page + 1 < totalPages) {
            inventory.setItem(50, ProgressionMenuGui.createItem(Material.PAPER, "§eNastępna strona (" + (page + 2) + ") ▶", null));
        }

        // Close button (Slot 49)
        inventory.setItem(49, ProgressionMenuGui.createItem(Material.BARRIER, "§c§lZamknij", null));

        // Quests Grid (Slots 10-16, 19-25, 28-34, 37-43)
        int[] questSlots = {
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34,
                37, 38, 39, 40, 41, 42, 43
        };

        int startIndex = page * maxPerPage;
        int endIndex = Math.min(startIndex + maxPerPage, allQuests.size());

        for (int i = startIndex; i < endIndex; i++) {
            Quest quest = allQuests.get(i);
            int slot = questSlots[i - startIndex];

            boolean completed = data.isQuestCompleted(quest.getId());
            boolean unlocked = ProgressionManager.getInstance().getProgressionService().isQuestUnlocked(data, quest);
            int prog = data.getProgress(quest.getId());
            int req = quest.getRequiredAmount();

            List<String> lore = new ArrayList<>();
            lore.add("§8Kategoria: " + quest.getCategory().getDisplayName());
            lore.add("");

            if (completed) {
                if (quest.getDescription() != null && !quest.getDescription().isEmpty()) {
                    lore.add("§7" + quest.getDescription());
                    lore.add("");
                }
                lore.add("§7Cel: §f" + quest.getTitle());
                lore.add("§a§l✔ UKOŃCZONE");
            } else if (unlocked) {
                if (quest.getDescription() != null && !quest.getDescription().isEmpty()) {
                    lore.add("§7" + quest.getDescription());
                    lore.add("");
                }
                lore.add("§7Cel: §f" + quest.getTitle());
                lore.add("§7Wymagana ilość: §e" + req);
                lore.add("");
                int displayProg = Math.min(prog, req);
                lore.add("§e⏳ Status: §f" + displayProg + "§7/§e" + req);
                lore.add("§f" + ProgressionMenuGui.buildProgressBar(displayProg, req, 8));
            } else {
                lore.add("§8🔒 §cTo zadanie jest jeszcze nieodkryte!");
                lore.add("§7Ukończ wcześniejsze cele, aby je odblokować.");
                lore.add("");
                lore.add("§7Wymaga wcześniejszego ukończenia:");
                for (String pId : quest.getPrerequisites()) {
                    Quest pQuest = ProgressionManager.getInstance().getQuestRegistry().getQuest(pId);
                    String pName = pQuest != null ? pQuest.getTitle() : pId;
                    boolean pDone = data.isQuestCompleted(pId);
                    lore.add(" " + (pDone ? "§a✔ " : "§c✖ ") + "§7" + pName);
                }
            }

            // Rewards (only visible if unlocked or completed)
            if ((unlocked || completed) && !quest.getReward().isEmpty()) {
                lore.add("");
                lore.add("§6§lNagrody:");
                if (quest.getReward().getMoney() > 0) {
                    lore.add(" §e• §f+" + quest.getReward().getMoney() + "$");
                }
                if (quest.getReward().getPlayerExp() > 0) {
                    lore.add(" §a• §f+" + quest.getReward().getPlayerExp() + " EXP");
                }
                for (Map.Entry<String, Integer> itm : quest.getReward().getItems().entrySet()) {
                    lore.add(" §b• §f" + itm.getKey() + " x" + itm.getValue());
                }
                for (Map.Entry<RPG.Levels.Objects.LevelSkill.SkillType, Double> sk : quest.getReward().getSkillExp().entrySet()) {
                    lore.add(" §d• §f+" + sk.getValue() + " EXP (" + sk.getKey().name() + ")");
                }
            }

            Material iconMat;
            String questTitle;
            if (completed) {
                iconMat = Material.LIME_DYE;
                questTitle = "§a✔ " + quest.getTitle();
            } else if (unlocked) {
                iconMat = (quest.getIcon() != null ? quest.getIcon() : Material.PAPER);
                questTitle = "§e" + quest.getTitle();
            } else {
                iconMat = Material.GRAY_DYE;
                questTitle = "§8🔒 ??? §8(Nieodkryte Zadanie)";
            }

            ItemStack questItem = ProgressionMenuGui.createItem(iconMat, questTitle, lore);

            if (completed) {
                ItemMeta meta = questItem.getItemMeta();
                if (meta != null) {
                    meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                    questItem.setItemMeta(meta);
                }
            }

            inventory.setItem(slot, questItem);
        }
    }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= 54) return;

        if (slot == 45) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            new ProgressionMenuGui(player, data).open();
            return;
        }

        if (slot == 49) {
            player.closeInventory();
            return;
        }

        if (slot == 48 && page > 0) {
            page--;
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            buildGui();
            return;
        }

        int maxPerPage = 28;
        int totalPages = (int) Math.ceil((double) allQuests.size() / maxPerPage);
        if (slot == 50 && page + 1 < totalPages) {
            page++;
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            buildGui();
            return;
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

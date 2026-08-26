package RPG.Progression.gui;

import RPG.Progression.ProgressionManager;
import RPG.Progression.model.PlayerProgressionData;
import RPG.Progression.model.Quest;
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

public class ProgressionMenuGui implements InventoryHolder {

    private final Player player;
    private final PlayerProgressionData data;
    private final Inventory inventory;

    public ProgressionMenuGui(Player player, PlayerProgressionData data) {
        this.player = player;
        this.data = data;
        this.inventory = Bukkit.createInventory(this, 54, ChatColor.DARK_GRAY + "✦ GŁÓWNA PROGRESJA RPG ✦");
        buildGui();
    }

    private void buildGui() {
        // Fill background
        ItemStack filler = createItem(Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, filler);
        }

        // Header Info (Slot 4)
        StageType current = data.getCurrentStage();
        int completedReq = ProgressionManager.getInstance().getStageService().getCompletedRequiredQuestsCount(data, current);
        int totalReq = ProgressionManager.getInstance().getStageService().getRequiredQuestsCount(current);
        double pct = totalReq > 0 ? ((double) completedReq / totalReq) * 100.0 : 100.0;
        String progressBar = buildProgressBar(completedReq, totalReq, 10);

        List<String> headerLore = Arrays.asList(
                "§7Twój aktualny poziom rozwoju w świecie gry.",
                "",
                "§6Aktualny Etap: " + current.getDisplayName(),
                "§7Postęp wymaganych zadań: §e" + completedReq + "§7/§e" + totalReq + " §8(" + String.format("%.0f", pct) + "%)",
                "§f" + progressBar,
                "",
                "§7Kliknij dowolny etap poniżej, aby",
                "§7zobaczyć listę przypisanych do niego zadań!"
        );
        inventory.setItem(4, createItem(Material.NETHER_STAR, "§6§lTwoja Progresja: §e" + player.getName(), headerLore));

        // Biomes Explorer Icon (Slot 8)
        List<String> biomesLore = Arrays.asList(
                "§7Zobacz listę odkrytych biomów w świecie gry",
                "§7oraz biomy wymagane do odblokowania kolejnych etapów.",
                "",
                "§7Odkrytych biomów łącznie: §a" + data.getDiscoveredBiomes().size(),
                "",
                "§eKliknij, aby otworzyć atlas biomów!"
        );
        inventory.setItem(8, createItem(Material.MAP, "§3§lAtlas Odkrytych Biomów", biomesLore));

        // 6 Stages Layout (Slots: 20, 21, 22, 23, 24, 25)
        int[] stageSlots = {20, 21, 22, 23, 24, 25};
        StageType[] stages = {StageType.WOODEN, StageType.STONE, StageType.IRON, StageType.DIAMOND, StageType.NETHER, StageType.END};

        for (int i = 0; i < stages.length; i++) {
            StageType stage = stages[i];
            int slot = stageSlots[i];

            boolean isCurrent = (stage == current);
            boolean isCompleted = (current.getOrder() > stage.getOrder());
            boolean isLocked = (stage.getOrder() > current.getOrder());

            List<String> lore = new ArrayList<>();
            lore.add(stage.getDescription());
            lore.add("");

            int stageReqTotal = ProgressionManager.getInstance().getStageService().getRequiredQuestsCount(stage);
            int stageReqDone = ProgressionManager.getInstance().getStageService().getCompletedRequiredQuestsCount(data, stage);

            if (isCompleted) {
                lore.add("§a✔ ETAP UKOŃCZONY");
                lore.add("§7Wymagane zadania: §a" + stageReqDone + "§7/§a" + stageReqTotal);
                lore.add("");
                lore.add("§eKliknij, aby przejrzeć zadania z tego etapu!");
            } else if (isCurrent) {
                lore.add("§e✦ AKTUALNIE ROZWIJANY ETAP ✦");
                lore.add("§7Postęp: §e" + stageReqDone + "§7/§e" + stageReqTotal + " zadań");
                lore.add("§f" + buildProgressBar(stageReqDone, stageReqTotal, 10));
                lore.add("");
                lore.add("§eKliknij, aby otworzyć zadania tego etapu!");
            } else {
                lore.add("§c🔒 ETAP ZABLOKOWANY");
                lore.add("§7Wymaga ukończenia: " + stage.getPrevious().getDisplayName());
                lore.add("");
                lore.add("§8Kliknij, aby podejrzeć przyszłe cele.");
            }

            ItemStack stageItem = createItem(stage.getIconMaterial(), stage.getDisplayName(), lore);
            if (isCurrent || isCompleted) {
                ItemMeta meta = stageItem.getItemMeta();
                if (meta != null) {
                    meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                    stageItem.setItemMeta(meta);
                }
            }
            inventory.setItem(slot, stageItem);
        }

        // Close button (Slot 49)
        inventory.setItem(49, createItem(Material.BARRIER, "§c§lZamknij Menu", List.of("§7Kliknij, aby wyjść z menu progresji.")));
    }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= 54) return;

        if (slot == 49) {
            player.closeInventory();
            return;
        }

        if (slot == 8) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            new ExplorationBiomesGui(player, data).open();
            return;
        }

        int[] stageSlots = {20, 21, 22, 23, 24, 25};
        StageType[] stages = {StageType.WOODEN, StageType.STONE, StageType.IRON, StageType.DIAMOND, StageType.NETHER, StageType.END};

        for (int i = 0; i < stageSlots.length; i++) {
            if (slot == stageSlots[i]) {
                StageType selected = stages[i];
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                new StageDetailGui(player, data, selected).open();
                return;
            }
        }
    }

    public void open() {
        player.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public static String buildProgressBar(int current, int max, int length) {
        if (max <= 0) return "§a[■■■■■■■■■■]";
        int completedBars = (int) Math.round(((double) current / max) * length);
        completedBars = Math.max(0, Math.min(length, completedBars));
        int remainingBars = length - completedBars;

        StringBuilder sb = new StringBuilder("§a[");
        for (int i = 0; i < completedBars; i++) {
            sb.append("■");
        }
        sb.append("§7");
        for (int i = 0; i < remainingBars; i++) {
            sb.append("□");
        }
        sb.append("§a]");
        return sb.toString();
    }

    public static ItemStack createItem(Material mat, String name, List<String> lore) {
        ItemStack stack = new ItemStack(mat != null ? mat : Material.STONE);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            if (name != null) meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            if (lore != null) {
                List<String> colored = new ArrayList<>();
                for (String l : lore) {
                    colored.add(ChatColor.translateAlternateColorCodes('&', l));
                }
                meta.setLore(colored);
            }
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_DESTROYS);
            stack.setItemMeta(meta);
        }
        return stack;
    }
}

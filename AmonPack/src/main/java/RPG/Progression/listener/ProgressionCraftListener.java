package RPG.Progression.listener;

import RPG.Progression.model.ObjectiveType;
import RPG.Progression.model.PlayerProgressionData;
import RPG.Progression.service.ProgressionService;
import RPG.Progression.service.RestrictionService;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;

public class ProgressionCraftListener implements Listener {

    private final ProgressionService progressionService;
    private final RestrictionService restrictionService;

    public ProgressionCraftListener(ProgressionService progressionService, RestrictionService restrictionService) {
        this.progressionService = progressionService;
        this.restrictionService = restrictionService;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCraftItem(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack result = event.getRecipe().getResult();
        if (result == null || result.getType() == Material.AIR) return;

        PlayerProgressionData data = progressionService.getPlayerData(player);
        if (data == null) return;

        // 1. Content restriction check
        if (!restrictionService.isAllowedItem(player, data, result)) {
            event.setCancelled(true);
            return;
        }

        // Calculate amount crafted
        int amount = result.getAmount();
        if (event.isShiftClick()) {
            amount = Math.max(1, amount); // shift click estimate
        }

        Material mat = result.getType();
        String matName = mat.name();

        // 2. Trigger CRAFT_ITEM objective
        progressionService.handleObjective(player, ObjectiveType.CRAFT_ITEM, matName, amount);

        // Armor set check
        checkArmorSetCraft(player, matName);
    }

    private void checkArmorSetCraft(Player player, String craftedMatName) {
        // If player crafted a piece, check if player owns or has full armor set
        if (craftedMatName.startsWith("LEATHER_")) {
            if (hasFullArmorSet(player, "LEATHER")) {
                progressionService.handleObjective(player, ObjectiveType.CRAFT_ITEM, "ARMOR_SET_LEATHER", 1);
            }
        } else if (craftedMatName.startsWith("IRON_")) {
            if (hasFullArmorSet(player, "IRON")) {
                progressionService.handleObjective(player, ObjectiveType.CRAFT_ITEM, "ARMOR_SET_IRON", 1);
            }
        } else if (craftedMatName.startsWith("DIAMOND_")) {
            if (hasFullArmorSet(player, "DIAMOND")) {
                progressionService.handleObjective(player, ObjectiveType.CRAFT_ITEM, "ARMOR_SET_DIAMOND", 1);
            }
        } else if (craftedMatName.startsWith("NETHERITE_")) {
            if (hasFullArmorSet(player, "NETHERITE")) {
                progressionService.handleObjective(player, ObjectiveType.CRAFT_ITEM, "ARMOR_SET_NETHERITE", 1);
            }
        }
    }

    private boolean hasFullArmorSet(Player player, String prefix) {
        ItemStack helmet = player.getInventory().getHelmet();
        ItemStack chest = player.getInventory().getChestplate();
        ItemStack legs = player.getInventory().getLeggings();
        ItemStack boots = player.getInventory().getBoots();

        return helmet != null && helmet.getType().name().startsWith(prefix)
                && chest != null && chest.getType().name().startsWith(prefix)
                && legs != null && legs.getType().name().startsWith(prefix)
                && boots != null && boots.getType().name().startsWith(prefix);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onFurnaceExtract(FurnaceExtractEvent event) {
        Player player = event.getPlayer();
        Material mat = event.getItemType();
        int amount = event.getItemAmount();

        progressionService.handleObjective(player, ObjectiveType.COOK_ITEM, mat.name(), amount);
        progressionService.handleObjective(player, ObjectiveType.SMELT_ITEM, mat.name(), amount);
        progressionService.handleObjective(player, ObjectiveType.COLLECT_ITEM, mat.name(), amount);

        if (mat == Material.CHARCOAL || mat == Material.COAL) {
            progressionService.handleObjective(player, ObjectiveType.COLLECT_ITEM, "COAL", amount);
            progressionService.handleObjective(player, ObjectiveType.COLLECT_ITEM, "CHARCOAL", amount);
            progressionService.handleObjective(player, ObjectiveType.SMELT_ITEM, "COAL", amount);
            progressionService.handleObjective(player, ObjectiveType.SMELT_ITEM, "CHARCOAL", amount);
        }

        if (mat == Material.COOKED_BEEF || mat == Material.COOKED_PORKCHOP || mat == Material.COOKED_CHICKEN
                || mat == Material.COOKED_MUTTON || mat == Material.COOKED_SALMON || mat == Material.COOKED_COD) {
            progressionService.handleObjective(player, ObjectiveType.COOK_ITEM, "FOOD", amount);
            progressionService.handleObjective(player, ObjectiveType.COOK_ITEM, "MEAT", amount);
            progressionService.handleObjective(player, ObjectiveType.COLLECT_ITEM, "FOOD", amount);
            progressionService.handleObjective(player, ObjectiveType.COLLECT_ITEM, "MEAT", amount);
        }

        if (mat == Material.IRON_INGOT || mat == Material.GOLD_INGOT || mat == Material.COPPER_INGOT) {
            progressionService.handleObjective(player, ObjectiveType.SMELT_ITEM, "ORES", amount);
            progressionService.handleObjective(player, ObjectiveType.COLLECT_ITEM, "INGOT", amount);
        }
    }
}

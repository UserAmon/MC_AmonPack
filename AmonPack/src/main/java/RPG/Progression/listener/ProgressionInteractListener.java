package RPG.Progression.listener;

import CustomContent.Items.CustomItemManager;
import Plugin.AmonPackPlugin;
import RPG.Progression.model.ObjectiveType;
import RPG.Progression.model.PlayerProgressionData;
import RPG.Progression.service.ProgressionService;
import RPG.Progression.service.RestrictionService;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;

public class ProgressionInteractListener implements Listener {

    private final ProgressionService progressionService;
    private final RestrictionService restrictionService;

    public ProgressionInteractListener(ProgressionService progressionService, RestrictionService restrictionService) {
        this.progressionService = progressionService;
        this.restrictionService = restrictionService;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        Block block = event.getClickedBlock();

        PlayerProgressionData data = progressionService.getPlayerData(player);
        if (data == null) return;

        // 1. Content restriction check on held item
        if (item != null && item.getType() != Material.AIR) {
            if (!restrictionService.isAllowedItem(player, data, item)) {
                event.setCancelled(true);
                return;
            }
        }

        // 2. Bone meal use on crops
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && block != null && item != null) {
            if (item.getType() == Material.BONE_MEAL) {
                Material bType = block.getType();
                if (bType == Material.WHEAT || bType == Material.CARROTS || bType == Material.POTATOES
                        || bType == Material.BEETROOTS || bType.name().endsWith("_SAPLING")) {
                    progressionService.handleObjective(player, ObjectiveType.USE_ITEM, "BONE_MEAL", 1);
                }
            }

            // Hoe on dirt -> prepare farmland
            if (item.getType().name().endsWith("_HOE")) {
                if (block.getType() == Material.DIRT || block.getType() == Material.GRASS_BLOCK) {
                    progressionService.handleObjective(player, ObjectiveType.USE_ITEM, "HOE", 1);
                    progressionService.handleObjective(player, ObjectiveType.PLACE_BLOCK, "FARMLAND", 1);
                }
            }
        }

        // 3. Block interaction objectives
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && block != null) {
            Material bType = block.getType();
            if (bType == Material.COMPOSTER) {
                progressionService.handleObjective(player, ObjectiveType.USE_BLOCK, "COMPOSTER", 1);
            } else if (bType == Material.CAMPFIRE || bType == Material.SOUL_CAMPFIRE) {
                progressionService.handleObjective(player, ObjectiveType.USE_BLOCK, "CAMPFIRE", 1);
            } else if (bType == Material.FURNACE || bType == Material.BLAST_FURNACE || bType == Material.SMOKER) {
                progressionService.handleObjective(player, ObjectiveType.USE_BLOCK, "FURNACE", 1);
            } else if (bType == Material.CRAFTING_TABLE) {
                progressionService.handleObjective(player, ObjectiveType.USE_BLOCK, "CRAFTING_TABLE", 1);
            } else if (bType == Material.CHEST || bType == Material.TRAPPED_CHEST || bType == Material.BARREL) {
                progressionService.handleObjective(player, ObjectiveType.USE_BLOCK, "CHEST", 1);
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBreed(EntityBreedEvent event) {
        if (!(event.getBreeder() instanceof Player player)) return;

        Entity entity = event.getEntity();
        String typeName = entity.getType().name();

        progressionService.handleObjective(player, ObjectiveType.BREED_ENTITY, typeName, 1);
        progressionService.handleObjective(player, ObjectiveType.BREED_ENTITY, "ANIMALS", 1);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
            Player player = event.getPlayer();
            progressionService.handleObjective(player, ObjectiveType.CATCH_FISH, "FISH", 1);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        Material mat = item.getType();

        progressionService.handleObjective(player, ObjectiveType.USE_ITEM, mat.name(), 1);
        if (mat == Material.BREAD) {
            progressionService.handleObjective(player, ObjectiveType.USE_ITEM, "BREAD", 1);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        ItemStack stack = event.getItem().getItemStack();
        Material mat = stack.getType();
        int amount = stack.getAmount();

        // 1. Custom Item Pickup
        if (AmonPackPlugin.customItemManager != null) {
            String customId = AmonPackPlugin.customItemManager.getCustomItemId(stack);
            if (customId != null) {
                progressionService.handleObjective(player, ObjectiveType.COLLECT_ITEM, customId, amount);
                progressionService.handleObjective(player, ObjectiveType.COLLECT_ITEM, "custom:" + customId, amount);
            }
        }

        // 2. Vanilla Item Pickup
        progressionService.handleObjective(player, ObjectiveType.COLLECT_ITEM, mat.name(), amount);

        if (mat.name().endsWith("_LOG") || mat.name().endsWith("_WOOD")) {
            progressionService.handleObjective(player, ObjectiveType.COLLECT_ITEM, "WOOD", amount);
        }
        if (mat == Material.WHEAT_SEEDS || mat == Material.PUMPKIN_SEEDS || mat == Material.MELON_SEEDS || mat == Material.BEETROOT_SEEDS) {
            progressionService.handleObjective(player, ObjectiveType.COLLECT_ITEM, "SEEDS", amount);
        }
        if (mat == Material.COAL || mat == Material.CHARCOAL) {
            progressionService.handleObjective(player, ObjectiveType.COLLECT_ITEM, "COAL", amount);
        }
        if (mat == Material.RAW_IRON || mat == Material.IRON_ORE || mat == Material.DEEPSLATE_IRON_ORE) {
            progressionService.handleObjective(player, ObjectiveType.COLLECT_ITEM, "IRON_ORE", amount);
        }
        if (mat == Material.DIAMOND || mat == Material.DIAMOND_ORE || mat == Material.DEEPSLATE_DIAMOND_ORE) {
            progressionService.handleObjective(player, ObjectiveType.COLLECT_ITEM, "DIAMOND", amount);
        }
    }
}

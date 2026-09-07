package RPG.Progression.listener;

import RPG.Progression.model.ObjectiveType;
import RPG.Progression.model.PlayerProgressionData;
import RPG.Progression.service.ProgressionService;
import RPG.Progression.service.RestrictionService;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public class ProgressionBlockListener implements Listener {

    private final ProgressionService progressionService;
    private final RestrictionService restrictionService;

    public ProgressionBlockListener(ProgressionService progressionService, RestrictionService restrictionService) {
        this.progressionService = progressionService;
        this.restrictionService = restrictionService;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Material mat = block.getType();

        PlayerProgressionData data = progressionService.getPlayerData(player);
        if (data == null) return;

        // 1. Content restriction check
        if (!restrictionService.isAllowedBlock(player, data, mat)) {
            event.setCancelled(true);
            return;
        }

        // 2. Crop harvest check
        if (block.getBlockData() instanceof Ageable ageable) {
            if (ageable.getAge() >= ageable.getMaximumAge()) {
                progressionService.handleObjective(player, ObjectiveType.HARVEST_CROP, mat.name(), 1);
            }
        }

        // 3. Destroy block objective
        progressionService.handleObjective(player, ObjectiveType.DESTROY_BLOCK, mat.name(), 1);

        // 4. Mining depth objective
        progressionService.handleObjective(player, ObjectiveType.MINE_TO_DEPTH, String.valueOf(block.getY()), 1);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Material mat = block.getType();

        PlayerProgressionData data = progressionService.getPlayerData(player);
        if (data == null) return;

        // Content restriction check
        if (!restrictionService.isAllowedBlock(player, data, mat)) {
            event.setCancelled(true);
            return;
        }

        // Place block objective
        progressionService.handleObjective(player, ObjectiveType.PLACE_BLOCK, mat.name(), 1);

        // Plant crop objective
        if (mat == Material.WHEAT || mat == Material.CARROTS || mat == Material.POTATOES || mat == Material.BEETROOTS) {
            progressionService.handleObjective(player, ObjectiveType.PLANT_CROP, mat.name(), 1);
        }
        if (mat.name().endsWith("_SAPLING")) {
            progressionService.handleObjective(player, ObjectiveType.PLANT_CROP, mat.name(), 1);
        }
    }
}

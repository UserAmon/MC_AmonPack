package RPG.Progression.listener;

import RPG.Progression.model.PlayerProgressionData;
import RPG.Progression.service.ExplorationService;
import RPG.Progression.service.ProgressionService;
import RPG.Progression.service.RestrictionService;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class ProgressionExplorationListener implements Listener {

    private final ProgressionService progressionService;
    private final ExplorationService explorationService;
    private final RestrictionService restrictionService;

    public ProgressionExplorationListener(ProgressionService progressionService, ExplorationService explorationService, RestrictionService restrictionService) {
        this.progressionService = progressionService;
        this.explorationService = explorationService;
        this.restrictionService = restrictionService;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;
        if (from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY() && from.getBlockZ() == to.getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        PlayerProgressionData data = progressionService.getPlayerData(player);
        if (data == null) return;

        explorationService.checkPlayerLocation(player, data, progressionService);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPortal(PlayerPortalEvent event) {
        Player player = event.getPlayer();
        Location to = event.getTo();
        if (to == null || to.getWorld() == null) return;

        PlayerProgressionData data = progressionService.getPlayerData(player);
        if (data == null) return;

        World.Environment targetEnv = to.getWorld().getEnvironment();
        if (!restrictionService.isAllowedDimension(player, data, targetEnv)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.NETHER_PORTAL
                || event.getCause() == PlayerTeleportEvent.TeleportCause.END_PORTAL
                || event.getCause() == PlayerTeleportEvent.TeleportCause.END_GATEWAY) {
            Player player = event.getPlayer();
            Location to = event.getTo();
            if (to == null || to.getWorld() == null) return;

            PlayerProgressionData data = progressionService.getPlayerData(player);
            if (data == null) return;

            World.Environment targetEnv = to.getWorld().getEnvironment();
            if (!restrictionService.isAllowedDimension(player, data, targetEnv)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        progressionService.loadPlayer(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player p = event.getPlayer();
        explorationService.removePlayer(p.getUniqueId());
        progressionService.unloadPlayer(p);
    }
}

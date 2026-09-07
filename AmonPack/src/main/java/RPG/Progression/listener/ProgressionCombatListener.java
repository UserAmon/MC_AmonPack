package RPG.Progression.listener;

import RPG.Progression.model.ObjectiveType;
import RPG.Progression.service.ProgressionService;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wither;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;

public class ProgressionCombatListener implements Listener {

    private final ProgressionService progressionService;

    public ProgressionCombatListener(ProgressionService progressionService) {
        this.progressionService = progressionService;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer == null) return;

        String typeName = victim.getType().name();

        // 1. Kill entity objective
        progressionService.handleObjective(killer, ObjectiveType.KILL_ENTITY, typeName, 1);

        if (victim instanceof Monster) {
            progressionService.handleObjective(killer, ObjectiveType.KILL_ENTITY, "HOSTILE", 1);
            progressionService.handleObjective(killer, ObjectiveType.KILL_ENTITY, "AGGRESSIVE_MOBS", 1);
        }

        // MythicMobs and Custom Name check
        if (victim.getCustomName() != null) {
            String stripped = org.bukkit.ChatColor.stripColor(victim.getCustomName()).trim();
            progressionService.handleObjective(killer, ObjectiveType.KILL_ENTITY, stripped, 1);
            progressionService.handleObjective(killer, ObjectiveType.DEFEAT_BOSS, stripped, 1);
        }
        if (victim.hasMetadata("MythicMob")) {
            for (org.bukkit.metadata.MetadataValue mv : victim.getMetadata("MythicMob")) {
                try {
                    Object mobInst = mv.value();
                    if (mobInst != null) {
                        String internalName = null;
                        try {
                            Object mobType = mobInst.getClass().getMethod("getType").invoke(mobInst);
                            if (mobType != null) {
                                internalName = (String) mobType.getClass().getMethod("getInternalName").invoke(mobType);
                            }
                        } catch (Throwable ignored) {
                            internalName = (String) mobInst.getClass().getMethod("getMobType").invoke(mobInst);
                        }
                        if (internalName != null) {
                            progressionService.handleObjective(killer, ObjectiveType.KILL_ENTITY, internalName, 1);
                            progressionService.handleObjective(killer, ObjectiveType.DEFEAT_BOSS, internalName, 1);
                        }
                    }
                } catch (Throwable ignored) {}
            }
        }

        // 2. Boss defeat check for vanilla bosses
        if (victim instanceof EnderDragon) {
            progressionService.handleObjective(killer, ObjectiveType.DEFEAT_BOSS, "ENDER_DRAGON", 1);
            progressionService.handleObjective(killer, ObjectiveType.DEFEAT_BOSS, "DRAGON", 1);
        } else if (victim instanceof Wither) {
            progressionService.handleObjective(killer, ObjectiveType.DEFEAT_BOSS, "WITHER", 1);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onExpChange(PlayerExpChangeEvent event) {
        Player player = event.getPlayer();
        int amount = event.getAmount();
        if (amount > 0) {
            progressionService.handleObjective(player, ObjectiveType.GAIN_EXPERIENCE, "EXP", amount);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onBedLeave(PlayerBedLeaveEvent event) {
        Player player = event.getPlayer();
        long time = player.getWorld().getTime();
        // If time is morning (0 - 1000 ticks)
        if (time < 1000 || time > 23000) {
            progressionService.handleObjective(player, ObjectiveType.SURVIVE_NIGHT, "NIGHT", 1);
            progressionService.handleObjective(player, ObjectiveType.USE_BLOCK, "BED", 1);
        }
    }
}

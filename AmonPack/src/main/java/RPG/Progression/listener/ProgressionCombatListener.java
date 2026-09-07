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
        if (killer == null && victim.getLastDamageCause() instanceof org.bukkit.event.entity.EntityDamageByEntityEvent edbe) {
            if (edbe.getDamager() instanceof Player p) {
                killer = p;
            } else if (edbe.getDamager() instanceof org.bukkit.entity.Projectile proj && proj.getShooter() instanceof Player p) {
                killer = p;
            }
        }
        if (killer == null) return;

        String typeName = victim.getType().name();

        // 1. Kill entity objective
        progressionService.handleObjective(killer, ObjectiveType.KILL_ENTITY, typeName, 1);

        if (victim instanceof Monster) {
            progressionService.handleObjective(killer, ObjectiveType.KILL_ENTITY, "HOSTILE", 1);
            progressionService.handleObjective(killer, ObjectiveType.KILL_ENTITY, "AGGRESSIVE_MOBS", 1);
        }

        // Custom Name check
        if (victim.getCustomName() != null) {
            String stripped = org.bukkit.ChatColor.stripColor(victim.getCustomName()).trim();
            progressionService.handleObjective(killer, ObjectiveType.KILL_ENTITY, stripped, 1);
            progressionService.handleObjective(killer, ObjectiveType.DEFEAT_BOSS, stripped, 1);
        }

        // MythicMobs check (v5, v4, and metadata fallback)
        String mythicInternalName = getMythicMobInternalName(victim);
        if (mythicInternalName != null) {
            progressionService.handleObjective(killer, ObjectiveType.KILL_ENTITY, mythicInternalName, 1);
            progressionService.handleObjective(killer, ObjectiveType.DEFEAT_BOSS, mythicInternalName, 1);
        }

        // 2. Boss defeat check for vanilla bosses
        if (victim instanceof EnderDragon) {
            progressionService.handleObjective(killer, ObjectiveType.DEFEAT_BOSS, "ENDER_DRAGON", 1);
            progressionService.handleObjective(killer, ObjectiveType.DEFEAT_BOSS, "DRAGON", 1);
        } else if (victim instanceof Wither) {
            progressionService.handleObjective(killer, ObjectiveType.DEFEAT_BOSS, "WITHER", 1);
        }
    }

    private String getMythicMobInternalName(LivingEntity entity) {
        if (entity == null) return null;

        // 1. Try MythicMobs v5 (io.lumine.mythic.bukkit.MythicBukkit)
        try {
            Class<?> mythicBukkitClass = Class.forName("io.lumine.mythic.bukkit.MythicBukkit");
            Object inst = mythicBukkitClass.getMethod("inst").invoke(null);
            if (inst != null) {
                // Try APIHelper first
                try {
                    Object apiHelper = inst.getClass().getMethod("getAPIHelper").invoke(inst);
                    if (apiHelper != null) {
                        Object activeMob = apiHelper.getClass().getMethod("getMythicMobInstance", org.bukkit.entity.Entity.class).invoke(apiHelper, entity);
                        if (activeMob != null) {
                            Object mobType = activeMob.getClass().getMethod("getType").invoke(activeMob);
                            if (mobType != null) {
                                return (String) mobType.getClass().getMethod("getInternalName").invoke(mobType);
                            }
                        }
                    }
                } catch (Throwable ignored) {}

                // Try MobManager
                try {
                    Object mobManager = inst.getClass().getMethod("getMobManager").invoke(inst);
                    if (mobManager != null) {
                        Object opt = mobManager.getClass().getMethod("getActiveMob", java.util.UUID.class).invoke(mobManager, entity.getUniqueId());
                        if (opt instanceof java.util.Optional<?> optional && optional.isPresent()) {
                            Object activeMob = optional.get();
                            Object mobType = activeMob.getClass().getMethod("getType").invoke(activeMob);
                            if (mobType != null) {
                                return (String) mobType.getClass().getMethod("getInternalName").invoke(mobType);
                            }
                        }
                    }
                } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}

        // 2. Try MythicMobs v4 (io.lumine.xikage.mythicmobs.MythicMobs)
        try {
            Class<?> mythicMobsClass = Class.forName("io.lumine.xikage.mythicmobs.MythicMobs");
            Object inst = mythicMobsClass.getMethod("inst").invoke(null);
            if (inst != null) {
                Object apiHelper = inst.getClass().getMethod("getAPIHelper").invoke(inst);
                if (apiHelper != null) {
                    Object activeMob = apiHelper.getClass().getMethod("getMythicMobInstance", org.bukkit.entity.Entity.class).invoke(apiHelper, entity);
                    if (activeMob != null) {
                        Object mobType = activeMob.getClass().getMethod("getType").invoke(activeMob);
                        if (mobType != null) {
                            return (String) mobType.getClass().getMethod("getInternalName").invoke(mobType);
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}

        // 3. Fallback to metadata check
        if (entity.hasMetadata("MythicMob")) {
            for (org.bukkit.metadata.MetadataValue mv : entity.getMetadata("MythicMob")) {
                try {
                    Object mobInst = mv.value();
                    if (mobInst != null) {
                        try {
                            Object mobType = mobInst.getClass().getMethod("getType").invoke(mobInst);
                            if (mobType != null) {
                                return (String) mobType.getClass().getMethod("getInternalName").invoke(mobType);
                            }
                        } catch (Throwable ignored) {
                            return (String) mobInst.getClass().getMethod("getMobType").invoke(mobInst);
                        }
                    }
                } catch (Throwable ignored) {}
            }
        }

        return null;
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

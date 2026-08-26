package CustomContent.Bosses;

import CustomContent.Items.CustomItemManager;
import RPG.Progression.event.CustomBossDefeatEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class BossListener implements Listener {

    private final BossManager bossManager;
    private final CustomItemManager itemManager;
    private final Random random = new Random();

    public BossListener(BossManager bossManager, CustomItemManager itemManager) {
        this.bossManager = bossManager;
        this.itemManager = itemManager;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBossDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof LivingEntity victim) {
            ActiveBossInstance boss = bossManager.getActiveBoss(victim);
            if (boss != null) {
                // Record damager player
                if (event.getDamager() instanceof Player player) {
                    boss.addDamager(player.getUniqueId());
                } else if (event.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Player shooter) {
                    boss.addDamager(shooter.getUniqueId());
                }

                // Trigger on hit skills
                for (CustomBoss.BossSkill skill : boss.getTemplate().getSkills()) {
                    if ("ON_HIT".equalsIgnoreCase(skill.trigger)) {
                        if (random.nextDouble() <= 0.35) {
                            if (skill.announcement != null && !skill.announcement.isEmpty()) {
                                Bukkit.broadcastMessage(skill.announcement);
                            }
                            BossSkillExecutor.executeSkill(boss.getEntity(), skill.ability, skill.range);
                        }
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBossDeath(EntityDeathEvent event) {
        ActiveBossInstance boss = bossManager.getActiveBoss(event.getEntity());
        if (boss == null) return;

        event.getDrops().clear();
        event.setDroppedExp(boss.getTemplate().getExpDrop());

        Location loc = event.getEntity().getLocation();
        loc.getWorld().playSound(loc, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);

        Player killer = event.getEntity().getKiller();
        String killerName = killer != null ? killer.getName() : "Bohaterów";

        Bukkit.broadcastMessage("§6§l[AmonPack] §eBoss " + boss.getTemplate().getDisplayName() + " §6został pokonany przez §a" + killerName + "§6!");

        // Collect all participants (damagers + nearby players within range)
        Set<Player> participants = new HashSet<>();
        if (killer != null) {
            participants.add(killer);
        }
        for (UUID uuid : boss.getDamagers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                participants.add(p);
            }
        }
        double range = Math.max(35.0, boss.getTemplate().getFollowRange());
        for (Player p : loc.getWorld().getPlayers()) {
            if (p.getLocation().distance(loc) <= range) {
                participants.add(p);
            }
        }

        // Fire progression custom boss defeat event
        Bukkit.getPluginManager().callEvent(new CustomBossDefeatEvent(boss.getTemplate(), (Mob) event.getEntity(), killer, participants));

        // Dropy
        for (CustomBoss.BossDrop drop : boss.getTemplate().getDrops()) {
            if (drop.customItemId != null && random.nextDouble() <= drop.chance) {
                ItemStack stack = itemManager.createItemStack(drop.customItemId);
                if (stack != null) {
                    int count = drop.min;
                    if (drop.max > drop.min) {
                        count += random.nextInt(drop.max - drop.min + 1);
                    }
                    stack.setAmount(count);
                    loc.getWorld().dropItemNaturally(loc, stack);
                }
            }
        }

        bossManager.removeActiveBoss(event.getEntity().getUniqueId());
    }
}

package CustomContent.Bosses;

import CustomContent.Items.CustomItemManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

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

package Abilities.PK_Abilities.Chi;

import Plugin.AmonPackPlugin;
import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.ChiAbility;
import com.projectkorra.projectkorra.util.DamageHandler;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class PointBlank extends ChiAbility implements AddonAbility {

    private long cooldown;
    private long duration;
    private double range;
    private long chargeTimeMs;
    private double heavyDamage;
    private long chiBlockDuration;

    private long startTime;
    private LivingEntity targetEnemy;
    private int chargeTicks = 0;
    private int requiredChargeTicks;
    private boolean isMarked = false;

    public PointBlank(Player player) {
        super(player);

        if (hasAbility(player, PointBlank.class)) {
            return;
        }
        if (bPlayer.isOnCooldown(this) || !bPlayer.canBend(this)) {
            return;
        }

        loadConfig();

        this.startTime = System.currentTimeMillis();
        this.requiredChargeTicks = (int) (chargeTimeMs / 50L);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.5f);
        start();
    }

    private void loadConfig() {
        this.cooldown = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Chi.PointBlank.Cooldown", 12000L);
        this.duration = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Chi.PointBlank.Duration", 8000L);
        this.range = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Chi.PointBlank.Range", 8.0);
        this.chargeTimeMs = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Chi.PointBlank.ChargeTime", 2500L);
        this.heavyDamage = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Chi.PointBlank.HeavyDamage", 8.0);
        this.chiBlockDuration = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Chi.PointBlank.ChiBlockDuration", 5000L);
    }

    @Override
    public void progress() {
        if (player == null || !player.isOnline() || player.isDead()) {
            finishSkill();
            return;
        }

        long elapsed = System.currentTimeMillis() - startTime;
        if (elapsed >= duration) {
            finishSkill();
            return;
        }

        // Target scanning
        LivingEntity closest = findClosestEnemy();
        if (closest == null) {
            resetCharge();
            return;
        }

        if (targetEnemy != null && !targetEnemy.getUniqueId().equals(closest.getUniqueId())) {
            resetCharge();
        }

        targetEnemy = closest;

        // Out of range check -> reset charge!
        if (player.getLocation().distance(targetEnemy.getLocation()) > range) {
            resetCharge();
            return;
        }

        if (!isMarked) {
            chargeTicks++;
            renderChargingRing(targetEnemy.getLocation(), chargeTicks, requiredChargeTicks);

            if (chargeTicks >= requiredChargeTicks) {
                isMarked = true;
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.8f);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 2.0f);
            }
        } else {
            // Render marked target point - VISIBLE ONLY TO CASTER PLAYER
            Location targetPoint = targetEnemy.getLocation().add(0, 1.2, 0);

            // User-only particle rendering
            player.spawnParticle(Particle.END_ROD, targetPoint, 2, 0.05, 0.05, 0.05, 0.01);
            Particle.DustOptions goldDust = new Particle.DustOptions(Color.fromRGB(255, 215, 0), 1.2f);
            player.spawnParticle(Particle.DUST, targetPoint, 4, 0.1, 0.1, 0.1, 0, goldDust);
        }
    }

    private void resetCharge() {
        targetEnemy = null;
        chargeTicks = 0;
        isMarked = false;
    }

    private LivingEntity findClosestEnemy() {
        LivingEntity closest = null;
        double minDistance = range;
        for (Entity e : GeneralMethods.getEntitiesAroundPoint(player.getLocation(), range)) {
            if (e instanceof LivingEntity le && e.getEntityId() != player.getEntityId()) {
                double dist = player.getLocation().distance(le.getLocation());
                if (dist < minDistance) {
                    minDistance = dist;
                    closest = le;
                }
            }
        }
        return closest;
    }

    private void renderChargingRing(Location enemyLoc, int current, int total) {
        double ratio = (double) current / (double) total;
        double maxCirclePoints = 16;
        int pointsToRender = (int) Math.ceil(ratio * maxCirclePoints);
        double ringRadius = 0.8;

        Location feet = enemyLoc.clone().add(0, 0.1, 0);
        for (int i = 0; i < pointsToRender; i++) {
            double angle = (2 * Math.PI / maxCirclePoints) * i;
            double x = ringRadius * Math.cos(angle);
            double z = ringRadius * Math.sin(angle);
            Location pt = feet.clone().add(x, 0, z);

            Particle.DustOptions dust = new Particle.DustOptions(Color.fromRGB(255, (int) (100 + (155 * ratio)), 0), 0.8f);
            feet.getWorld().spawnParticle(Particle.DUST, pt, 1, 0, 0, 0, 0, dust);
        }
    }

    public void onHitTarget(LivingEntity victim) {
        if (!isMarked || targetEnemy == null || victim == null) return;
        if (!victim.getUniqueId().equals(targetEnemy.getUniqueId())) return;

        DamageHandler.damageEntity(victim, heavyDamage, this);

        if (victim instanceof Player targetPlayer) {
            BendingPlayer targetBPlayer = BendingPlayer.getBendingPlayer(targetPlayer);
            if (targetBPlayer != null) {
                targetBPlayer.blockChi();
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (targetBPlayer.isChiBlocked()) {
                            targetBPlayer.unblockChi();
                        }
                    }
                }.runTaskLater(AmonPackPlugin.plugin, Math.max(1L, chiBlockDuration / 50L));
            }
        }

        Location hitLoc = victim.getLocation().add(0, 1.2, 0);
        hitLoc.getWorld().playSound(hitLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.5f);
        hitLoc.getWorld().playSound(hitLoc, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.2f, 0.6f);

        hitLoc.getWorld().spawnParticle(Particle.EXPLOSION, hitLoc, 1, 0, 0, 0, 0);
        hitLoc.getWorld().spawnParticle(Particle.CRIT, hitLoc, 25, 0.4, 0.4, 0.4, 0.2);

        finishSkill();
    }

    private void finishSkill() {
        if (bPlayer != null) {
            bPlayer.addCooldown(this, cooldown);
        }
        remove();
    }

    @Override
    public long getCooldown() {
        return cooldown;
    }

    @Override
    public Location getLocation() {
        return player != null ? player.getLocation() : null;
    }

    @Override
    public String getName() {
        return "PointBlank";
    }

    @Override
    public boolean isSneakAbility() {
        return false;
    }

    @Override
    public boolean isHarmlessAbility() {
        return false;
    }

    @Override
    public String getAuthor() {
        return "AmonPack";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public void load() {
    }

    @Override
    public void stop() {
        finishSkill();
    }

    @Override
    public String getDescription() {
        return "Aktywuje ruch PointBlank. Podczas trwania ładuje oznaczenie celu pod nogami wroga w zasięgu. Po naładowaniu pokazuje punkt czuły wroga (widoczny tylko dla Ciebie). Celne uderzenie w ten punkt zadaje potężne obrażenia i blokuje chi.";
    }

    @Override
    public String getInstructions() {
        return "Naciśnij LPM aby aktywować PointBlank. Utrzymuj dystans od wroga aż załaduje się punkt, a następnie zaatakuj go!";
    }
}

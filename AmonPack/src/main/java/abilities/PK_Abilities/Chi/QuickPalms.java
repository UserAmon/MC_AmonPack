package Abilities.PK_Abilities.Chi;

import Plugin.AmonPackPlugin;
import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.ChiAbility;
import com.projectkorra.projectkorra.util.DamageHandler;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class QuickPalms extends ChiAbility implements AddonAbility {

    private enum Mode { NONE, SHIFT_STANCE, TARGET_STRIKING }

    private Mode mode;
    private long cooldown;
    private double range;
    private long stanceDuration;
    private double initialDamage;
    private int pointCount;
    private double strikeDamage;

    private long startTime;
    private LivingEntity targetEntity;
    private List<Location> targetPoints = new ArrayList<>();
    private Random random = new Random();

    public QuickPalms(Player player) {
        super(player);

        if (hasAbility(player, QuickPalms.class)) {
            return;
        }
        if (bPlayer.isOnCooldown(this) || !bPlayer.canBend(this)) {
            return;
        }

        loadConfig();

        if (player.isSneaking()) {
            LivingEntity enemy = findNearestEnemy(player, range);
            if (enemy == null) {
                return;
            }
            this.mode = Mode.SHIFT_STANCE;
            this.startTime = System.currentTimeMillis();
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.4f);
            start();
        }
    }

    private void loadConfig() {
        this.cooldown = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Chi.QuickPalms.Cooldown", 8000L);
        this.range = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Chi.QuickPalms.Range", 6.0);
        this.stanceDuration = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Chi.QuickPalms.StanceDuration", 5000L);
        this.initialDamage = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Chi.QuickPalms.InitialDamage", 2.5);
        this.pointCount = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Chi.QuickPalms.PointCount", 3);
        this.strikeDamage = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Chi.QuickPalms.StrikeDamage", 2.0);
    }

    @Override
    public void progress() {
        if (player == null || !player.isOnline() || player.isDead()) {
            remove();
            return;
        }

        if (mode == Mode.SHIFT_STANCE) {
            long elapsed = System.currentTimeMillis() - startTime;
            if (elapsed >= stanceDuration || !player.isSneaking()) {
                bPlayer.addCooldown(this, cooldown);
                remove();
                return;
            }

            // Render rotating Chi rings in front of camera
            Location eye = player.getEyeLocation();
            Vector forward = eye.getDirection().normalize();
            Location center = eye.clone().add(forward.multiply(1.2));

            double time = System.currentTimeMillis() / 150.0;
            for (int i = 0; i < 6; i++) {
                double angle = time + (i * Math.PI / 3);
                double x = 0.5 * Math.cos(angle);
                double y = 0.5 * Math.sin(angle);
                Vector right = new Vector(-forward.getZ(), 0, forward.getX()).normalize();
                if (right.lengthSquared() < 0.01) right = new Vector(1, 0, 0);
                Vector up = right.clone().crossProduct(forward).normalize();

                Location pLoc = center.clone().add(right.multiply(x)).add(up.multiply(y));
                Particle.DustOptions chiColor = new Particle.DustOptions(Color.fromRGB(240, 240, 255), 0.8f);
                player.getWorld().spawnParticle(Particle.DUST, pLoc, 1, 0, 0, 0, 0, chiColor);
            }

            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText("§b✦ QUICK PALMS STANCE ✦"));

        } else if (mode == Mode.TARGET_STRIKING) {
            if (targetEntity == null || targetEntity.isDead() || !targetEntity.isValid()) {
                bPlayer.addCooldown(this, cooldown);
                remove();
                return;
            }

            if (System.currentTimeMillis() - startTime > 10000L) { // 10s window to strike points
                bPlayer.addCooldown(this, cooldown);
                remove();
                return;
            }

            // Render target points ONLY to the victim player
            if (targetEntity instanceof Player victimPlayer) {
                Location vBase = victimPlayer.getLocation();
                for (Location pt : targetPoints) {
                    Location curPt = vBase.clone().add(pt.getX(), pt.getY(), pt.getZ());
                    victimPlayer.spawnParticle(Particle.CRIT, curPt, 2, 0.05, 0.05, 0.05, 0.02);
                    Particle.DustOptions ptColor = new Particle.DustOptions(Color.fromRGB(255, 100, 100), 1.0f);
                    victimPlayer.spawnParticle(Particle.DUST, curPt, 2, 0.05, 0.05, 0.05, 0, ptColor);
                }
            }
        }
    }

    public boolean isShiftStanceActive() {
        return mode == Mode.SHIFT_STANCE;
    }

    public void onDodgeDamage(Entity damager) {
        if (mode != Mode.SHIFT_STANCE) return;

        if (damager != null) {
            Location behind = damager.getLocation().subtract(damager.getLocation().getDirection().normalize().multiply(1.5));
            behind.setDirection(damager.getLocation().getDirection());
            player.teleport(behind);

            player.getWorld().playSound(behind, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.2f, 1.8f);
            player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, behind.clone().add(0, 1.0, 0), 2, 0.2, 0.2, 0.2, 0);
        }

        bPlayer.addCooldown(this, cooldown);
        remove();
    }

    public void onHitEntity(LivingEntity victim) {
        if (mode == Mode.NONE) {
            // First hit - initialize striking mode
            this.mode = Mode.TARGET_STRIKING;
            this.targetEntity = victim;
            this.startTime = System.currentTimeMillis();

            DamageHandler.damageEntity(victim, initialDamage, this);
            apply1SecChiBlockAndSlow(victim);

            // Generate points with relative offsets
            targetPoints.clear();
            for (int i = 0; i < pointCount; i++) {
                double offX = (random.nextDouble() - 0.5) * 0.8;
                double offY = 0.4 + (random.nextDouble() * 1.0); // chest / neck / waist
                double offZ = (random.nextDouble() - 0.5) * 0.8;
                targetPoints.add(new Location(null, offX, offY, offZ));
            }

            player.getWorld().playSound(victim.getLocation(), Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 1.0f, 1.4f);

        } else if (mode == Mode.TARGET_STRIKING && victim.getEntityId() == targetEntity.getEntityId()) {
            DamageHandler.damageEntity(victim, strikeDamage, this);
            apply1SecChiBlockAndSlow(victim);

            if (!targetPoints.isEmpty()) {
                targetPoints.remove(0); // Remove one struck point
            }

            player.getWorld().playSound(victim.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.8f);

            if (targetPoints.isEmpty()) {
                // Final Strike — Knock caster BACKWARD away from victim!
                Vector backVec = player.getLocation().toVector().subtract(victim.getLocation().toVector()).setY(0);
                if (backVec.lengthSquared() < 0.01) {
                    backVec = player.getLocation().getDirection().multiply(-1.0);
                }
                backVec.normalize().multiply(1.4).setY(0.4);
                player.setVelocity(backVec);

                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.6f, 1.6f);
                bPlayer.addCooldown(this, cooldown);
                remove();
            }
        }
    }

    private void apply1SecChiBlockAndSlow(LivingEntity victim) {
        victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20, 1, false, false));

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
                }.runTaskLater(AmonPackPlugin.plugin, 20L); // 1 second = 20 ticks
            }
        }
    }

    private LivingEntity findNearestEnemy(Player player, double range) {
        LivingEntity nearest = null;
        double bestDist = range * range;
        for (Entity e : GeneralMethods.getEntitiesAroundPoint(player.getLocation(), range)) {
            if (e instanceof LivingEntity le && e.getEntityId() != player.getEntityId()) {
                double dist = player.getLocation().distanceSquared(le.getLocation());
                if (dist < bestDist) {
                    bestDist = dist;
                    nearest = le;
                }
            }
        }
        return nearest;
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
        return "QuickPalms";
    }

    @Override
    public boolean isSneakAbility() {
        return true;
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
        remove();
    }

    @Override
    public String getDescription() {
        return "Pozwala kontrować obrażenia przenosząc się za plecy wroga (Shift) oraz uderzać w słabe punkty na ciele wroga nakładając 1s chi-blocka i odrzucając się w tył po ostatnim ciosie (LPM).";
    }

    @Override
    public String getInstructions() {
        return "Shift: aktywuj kontrę w pobliżu wroga. LPM: uderzaj we wroga i jego słabe punkty!";
    }
}

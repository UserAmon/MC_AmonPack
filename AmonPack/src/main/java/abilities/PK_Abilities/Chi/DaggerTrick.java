package Abilities.PK_Abilities.Chi;

import Abilities.Util_Objects.AbilityProjectile;
import Abilities.Util_Objects.BetterParticles;
import Plugin.AmonPackPlugin;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.ChiAbility;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.ParticleEffect;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class DaggerTrick extends ChiAbility implements AddonAbility {

    private long cooldown;
    private double backwardForce;
    private double upForce;
    private int maxArrowClicks;
    private int arrowCount;
    private int arrowRange;
    private double arrowDamage;
    private double arrowSpeed;
    private double chiCost;

    private int slot;
    private int clicksUsed = 0;
    private long lastClickTime = 0;
    private final List<AbilityProjectile> projectiles = new ArrayList<>();
    private boolean launched = false;
    private boolean cooldownApplied = false;

    public DaggerTrick(Player player) {
        super(player);

        if (hasAbility(player, DaggerTrick.class)) {
            return;
        }
        if (bPlayer.isOnCooldown(this) || !bPlayer.canBend(this)) {
            return;
        }

        loadConfig();

        if (!ChiManager.consumeChi(player, chiCost)) {
            return;
        }

        this.slot = player.getInventory().getHeldItemSlot();

        performBackwardJump();
        start();
    }

    private void loadConfig() {
        this.cooldown = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Chi.DaggerTrick.Cooldown", 5000L);
        this.backwardForce = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Chi.DaggerTrick.BackwardForce", 1.2);
        this.upForce = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Chi.DaggerTrick.UpForce", 0.6);
        this.maxArrowClicks = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Chi.DaggerTrick.MaxArrowClicks", 3);
        this.arrowCount = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Chi.DaggerTrick.ArrowCount", 3);
        this.arrowRange = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Chi.DaggerTrick.ArrowRange", 25);
        this.arrowDamage = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Chi.DaggerTrick.ArrowDamage", 2.5);
        this.arrowSpeed = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Chi.DaggerTrick.ArrowSpeed", 1.0);
        this.chiCost = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Chi.DaggerTrick.ChiCost", 40.0);
    }

    private void performBackwardJump() {
        Vector dir = player.getLocation().getDirection().setY(0).normalize();
        Vector backVel = dir.multiply(-backwardForce).setY(upForce);
        player.setVelocity(backVel);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.4f);
        player.getWorld().spawnParticle(Particle.CRIT, player.getLocation(), 15, 0.3, 0.3, 0.3, 0.1);
        launched = true;
    }

    @Override
    public void progress() {
        if (player == null || !player.isOnline() || player.isDead()) {
            projectiles.clear();
            remove();
            return;
        }

        // Advance and process existing projectiles
        if (!projectiles.isEmpty()) {
            List<AbilityProjectile> copy = new ArrayList<>(projectiles);
            for (AbilityProjectile proj : copy) {
                Location loc = proj.Advance();
                if (loc == null) {
                    projectiles.remove(proj);
                    continue;
                }

                loc.getWorld().spawnParticle(Particle.CRIT, loc, 2, 0.05, 0.05, 0.05, 0.02);
                loc.getWorld().spawnParticle(Particle.SMOKE, loc, 1, 0.02, 0.02, 0.02, 0.01);

                boolean hit = false;
                for (Entity entity : GeneralMethods.getEntitiesAroundPoint(loc, 1.2)) {
                    if (entity instanceof LivingEntity && entity.getUniqueId() != player.getUniqueId()) {
                        DamageHandler.damageEntity(entity, arrowDamage, this);
                        loc.getWorld().playSound(loc, Sound.ENTITY_ITEM_BREAK, 0.8f, 1.5f);
                        projectiles.remove(proj);
                        hit = true;
                        break;
                    }
                }

                if (!hit) {
                    if (loc.distance(proj.getOrigin()) > arrowRange || loc.getBlock().getType().isSolid()) {
                        projectiles.remove(proj);
                    }
                }
            }
        }

        // Check if player changed slot or grounded
        if (launched) {
            if (player.getInventory().getHeldItemSlot() != slot || isGrounded()) {
                applyCooldown();
                launched = false; // Player can no longer fire arrows, but flying projectiles continue!
            }
        }

        // If jump has ended and all projectiles finished their journey, remove ability instance
        if (!launched && projectiles.isEmpty()) {
            remove();
        }
    }

    private boolean isGrounded() {
        Location loc = player.getLocation();
        return (player.getFallDistance() > 0.05 || player.getVelocity().getY() < 0)
                && (loc.clone().add(0, -0.1, 0).getBlock().getType().isSolid() || player.isOnGround());
    }

    public void onLeftClick() {
        if (!launched || clicksUsed >= maxArrowClicks) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastClickTime < 200) {
            return;
        }
        lastClickTime = now;
        clicksUsed++;

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0f, 1.2f);

        Location eye = player.getEyeLocation();
        Vector forward = eye.getDirection().normalize();

        List<BetterParticles> particles = new ArrayList<>();
        particles.add(new BetterParticles(3, ParticleEffect.CRIT, 0.1, 0.02, 0.1));

        int count = Math.max(1, arrowCount);
        for (int i = 0; i < count; i++) {
            double angleDeg = (count == 1) ? 0 : -45.0 + (i * 90.0 / (count - 1));
            Vector dir = rotateY(forward.clone(), angleDeg).multiply(arrowSpeed);

            Location startLoc = eye.clone();
            projectiles.add(new AbilityProjectile(dir, startLoc, startLoc.clone(), particles, 1));
        }
    }

    private Vector rotateY(Vector vector, double angleDegrees) {
        double angle = Math.toRadians(angleDegrees);
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double x = vector.getX() * cos - vector.getZ() * sin;
        double z = vector.getX() * sin + vector.getZ() * cos;
        return new Vector(x, vector.getY(), z).normalize();
    }

    private void applyCooldown() {
        if (!cooldownApplied && bPlayer != null) {
            bPlayer.addCooldown(this, cooldown);
            cooldownApplied = true;
        }
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
        return "DaggerTrick";
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
        return "1.1";
    }

    @Override
    public void load() {
    }

    @Override
    public void stop() {
        projectiles.clear();
        remove();
    }

    @Override
    public String getDescription() {
        return "Wyskakujesz w powietrze w tył (Shift). Podczas lotu klikaj LPM, aby wystrzeliwać wachlarze sztyletów/strzał w stronę wrogów.";
    }

    @Override
    public String getInstructions() {
        return "Kucnij (Shift), aby wyskoczyć do tyłu, a następnie klikaj LPM w powietrzu, aby strzelać!";
    }
}

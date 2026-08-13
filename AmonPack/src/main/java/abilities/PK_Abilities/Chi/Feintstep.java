package Abilities.PK_Abilities.Chi;

import Plugin.AmonPackPlugin;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.ChiAbility;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class Feintstep extends ChiAbility implements AddonAbility {

    private enum State { CHARGING, STANCE }

    private State state;
    private long chargeTime;
    private long duration;
    private long cooldown;
    private int maxDodges;
    private int speedAmplifier;
    private int jumpAmplifier;
    private double dashForce;

    private long startTime;
    private int dodgesLeft;
    private boolean activeStance = false;

    public Feintstep(Player player) {
        super(player);

        if (hasAbility(player, Feintstep.class)) {
            return;
        }
        if (bPlayer.isOnCooldown(this) || !bPlayer.canBend(this)) {
            return;
        }

        loadConfig();

        this.state = State.CHARGING;
        this.startTime = System.currentTimeMillis();
        this.dodgesLeft = maxDodges;

        start();
    }

    private void loadConfig() {
        this.chargeTime = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Chi.Feintstep.ChargeTime", 1500L);
        this.duration = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Chi.Feintstep.Duration", 10000L);
        this.cooldown = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Chi.Feintstep.Cooldown", 12000L);
        this.maxDodges = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Chi.Feintstep.MaxDodges", 3);
        this.speedAmplifier = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Chi.Feintstep.SpeedAmplifier", 1);
        this.jumpAmplifier = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Chi.Feintstep.JumpAmplifier", 1);
        this.dashForce = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Chi.Feintstep.DashForce", 1.2);
    }

    @Override
    public void progress() {
        if (player == null || !player.isOnline() || player.isDead()) {
            finishSkill();
            return;
        }

        if (state == State.CHARGING) {
            if (!player.isSneaking()) {
                finishSkill();
                return;
            }

            long elapsed = System.currentTimeMillis() - startTime;
            player.getWorld().spawnParticle(Particle.CRIT, player.getLocation().add(0, 0.5, 0), 2, 0.2, 0.2, 0.2, 0.05);

            if (elapsed >= chargeTime) {
                state = State.STANCE;
                activeStance = true;
                startTime = System.currentTimeMillis();
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.6f);
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.5f);
            }
        } else if (state == State.STANCE) {
            long elapsed = System.currentTimeMillis() - startTime;
            if (elapsed >= duration || dodgesLeft <= 0) {
                finishSkill();
                return;
            }

            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 10, speedAmplifier, false, false));
            player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 10, jumpAmplifier, false, false));

            player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation().add(0, 0.2, 0), 1, 0.2, 0.1, 0.2, 0.01);
        }
    }

    public boolean isStanceActive() {
        return activeStance && state == State.STANCE && dodgesLeft > 0;
    }

    public void onDodge(Entity attacker) {
        if (!isStanceActive()) return;

        dodgesLeft--;

        Location loc = player.getLocation();
        loc.getWorld().playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.2f, 1.8f);
        loc.getWorld().playSound(loc, Sound.ENTITY_BAT_TAKEOFF, 1.0f, 1.4f);

        loc.getWorld().spawnParticle(Particle.SWEEP_ATTACK, loc.clone().add(0, 1.0, 0), 2, 0.2, 0.2, 0.2, 0.05);
        loc.getWorld().spawnParticle(Particle.CLOUD, loc.clone().add(0, 0.5, 0), 12, 0.4, 0.4, 0.4, 0.1);
        loc.getWorld().spawnParticle(Particle.CRIT, loc.clone().add(0, 1.0, 0), 15, 0.3, 0.5, 0.3, 0.2);

        Vector dashVel;
        if (attacker != null) {
            Vector toAttacker = attacker.getLocation().toVector().subtract(loc.toVector()).normalize();
            Vector side = toAttacker.clone().crossProduct(new Vector(0, 1, 0)).normalize();
            if (Math.random() < 0.5) side.multiply(-1);
            dashVel = side.multiply(dashForce * 0.8).add(toAttacker.multiply(dashForce * 0.4)).setY(0.35);
        } else {
            Vector back = loc.getDirection().multiply(-dashForce).setY(0.35);
            dashVel = back;
        }

        player.setVelocity(dashVel);

        if (dodgesLeft <= 0) {
            finishSkill();
        }
    }

    private void finishSkill() {
        if (activeStance) {
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
        return "Feintstep";
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
        finishSkill();
    }

    @Override
    public String getDescription() {
        return "Ładuje stan akrobaty. Podczas trwania pozwala na uniknięcie określonej liczby instancji obrażeń, wykonując zwinne odskoki i nadając przyspieszenie.";
    }

    @Override
    public String getInstructions() {
        return "Przytrzymaj Shift aby naładować Feintstep, a następnie puść aby wejść w stan akrobaty!";
    }
}

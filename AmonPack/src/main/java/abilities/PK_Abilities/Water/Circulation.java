package Abilities.PK_Abilities.Water;

import Plugin.AmonPackPlugin;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.BloodAbility;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Random;

public class Circulation extends BloodAbility implements AddonAbility {

    private enum State {
        CHARGING, FULLY_CHARGED, CONTROLLED
    }

    private State state;
    private long cooldown;
    private double range;
    private long chargeTime;
    private int walkDurationTicks;

    private long startTime;
    private LivingEntity target;
    private Random random = new Random();

    public Circulation(Player player) {
        super(player);

        if (hasAbility(player, Circulation.class)) {
            return;
        }
        if (bPlayer.isOnCooldown(this) || !bPlayer.canBend(this)) {
            return;
        }

        loadConfig();

        LivingEntity enemy = findTarget(player, range);
        if (enemy == null) {
            return;
        }

        this.target = enemy;
        this.startTime = System.currentTimeMillis();
        this.state = State.CHARGING;

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_SPLASH_POTION_BREAK, 0.8f, 0.8f);
        start();
    }

    private void loadConfig() {
        this.cooldown = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Water.Circulation.Cooldown", 12000L);
        this.range = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Water.Circulation.Range", 15.0);
        this.chargeTime = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Water.Circulation.ChargeTime", 1500L);
        this.walkDurationTicks = AmonPackPlugin.getAbilitiesConfig()
                .getInt("AmonPack.Water.Circulation.WalkDurationTicks", 80);
    }

    @Override
    public void progress() {
        if (player == null || !player.isOnline() || player.isDead()) {
            remove();
            return;
        }

        if (state == State.CHARGING || state == State.FULLY_CHARGED) {
            if (!player.isSneaking()) {
                if (state == State.FULLY_CHARGED) {
                    executeCirculationControl();
                    return;
                } else {
                    remove();
                    return;
                }
            }

            if (target == null || target.isDead() || target.getLocation().distance(player.getLocation()) > range) {
                remove();
                return;
            }

            long elapsed = System.currentTimeMillis() - startTime;
            if (elapsed >= chargeTime) {
                if (state != State.FULLY_CHARGED) {
                    state = State.FULLY_CHARGED;
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
                    player.playSound(player.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, 1.0f, 1.4f);
                    Particle.DustOptions brightRed = new Particle.DustOptions(Color.fromRGB(255, 0, 0), 1.5f);
                    player.getWorld().spawnParticle(Particle.DUST, target.getEyeLocation(), 12, 0.3, 0.3, 0.3, 0,
                            brightRed);
                    player.getWorld().spawnParticle(Particle.CRIT, target.getEyeLocation(), 6, 0.3, 0.3, 0.3, 0.1);
                }
                Particle.DustOptions darkRed = new Particle.DustOptions(Color.fromRGB(220, 0, 0), 1.2f);
                player.getWorld().spawnParticle(Particle.DUST, target.getEyeLocation(), 5, 0.2, 0.2, 0.2, 0, darkRed);
                player.getWorld().spawnParticle(Particle.CRIT, target.getEyeLocation(), 2, 0.2, 0.2, 0.2, 0.05);
            } else {
                Particle.DustOptions bloodRed = new Particle.DustOptions(Color.fromRGB(130, 0, 0), 0.7f);
                player.getWorld().spawnParticle(Particle.DUST, target.getEyeLocation(), 2, 0.1, 0.1, 0.1, 0, bloodRed);
            }
        }
    }

    private void executeCirculationControl() {
        state = State.CONTROLLED;
        bPlayer.addCooldown(this, cooldown);

        applyForcedWalk(target, walkDurationTicks, true);

        if (VeinFlowManager.isActive(player)) {
            double aoeRadius = AmonPackPlugin.getAbilitiesConfig()
                    .getDouble("AmonPack.Water.VeinFlow.CirculationAoeRadius", 10.0);
            for (Entity entity : GeneralMethods.getEntitiesAroundPoint(player.getLocation(), aoeRadius)) {
                if (entity instanceof LivingEntity le && !entity.getUniqueId().equals(player.getUniqueId())
                        && !entity.getUniqueId().equals(target.getUniqueId())) {
                    applyForcedWalk(le, walkDurationTicks, false);
                }
            }
        }
    }

    private void applyForcedWalk(LivingEntity victim, int durationTicks, boolean isPrimary) {
        if (victim == null || victim.isDead())
            return;

        player.getWorld().playSound(victim.getLocation(), Sound.ENTITY_SPLASH_POTION_BREAK, 1.2f, 0.6f);
        player.getWorld().playSound(victim.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.5f, 1.6f);

        new BukkitRunnable() {
            private int ticks = 0;
            private float victimYaw = victim.getLocation().getYaw();

            @Override
            public void run() {
                ticks++;
                if (ticks > durationTicks || victim.isDead() || !victim.isValid()) {
                    if (isPrimary) {
                        remove();
                    }
                    cancel();
                    return;
                }

                // Every 10 ticks pick a random new walking yaw direction
                if (ticks % 10 == 0) {
                    victimYaw = random.nextFloat() * 360.0f - 180.0f;
                }

                Location loc = victim.getLocation();
                loc.setYaw(victimYaw);
                loc.setPitch(0.0f);

                Vector walkDir = loc.getDirection().normalize().multiply(0.28).setY(victim.getVelocity().getY());
                victim.teleport(loc);
                victim.setVelocity(walkDir);

                Particle.DustOptions bloodRed = new Particle.DustOptions(Color.fromRGB(160, 0, 0), 0.8f);
                victim.getWorld().spawnParticle(Particle.DUST, victim.getEyeLocation(), 3, 0.15, 0.15, 0.15, 0,
                        bloodRed);
            }
        }.runTaskTimer(AmonPackPlugin.plugin, 0L, 1L);
    }

    private LivingEntity findTarget(Player player, double range) {
        Location eye = player.getEyeLocation();
        Vector direction = eye.getDirection().normalize();
        for (double step = 0.0; step <= range; step += 0.5) {
            Location point = eye.clone().add(direction.clone().multiply(step));
            for (Entity entity : GeneralMethods.getEntitiesAroundPoint(point, 1.2)) {
                if (entity instanceof LivingEntity le && !entity.getUniqueId().equals(player.getUniqueId())) {
                    if (player.hasLineOfSight(entity)) {
                        return le;
                    }
                }
            }
        }
        return null;
    }

    @Override
    public long getCooldown() {
        return cooldown;
    }

    @Override
    public Location getLocation() {
        return target != null ? target.getLocation() : player.getLocation();
    }

    @Override
    public String getName() {
        return "Circulation";
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
        return "Przejmuje krążenie wroga poprzez ładowanie shiftem i zmusza go do losowego chodzenia.";
    }

    @Override
    public String getInstructions() {
        return "Przytrzymaj Shift patrząc na wroga aby go naładować, a następnie puść Shift!";
    }
}

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

    private enum State { CHARGING, FULLY_CHARGED, CONTROLLED }

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
        this.walkDurationTicks = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Water.Circulation.WalkDurationTicks", 80);
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
                state = State.FULLY_CHARGED;
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText("§aCirculation gotowy! Puść SHIFT, aby zmusić wroga do marszu!"));
                Particle.DustOptions darkRed = new Particle.DustOptions(Color.fromRGB(180, 0, 0), 1.0f);
                player.getWorld().spawnParticle(Particle.DUST, target.getEyeLocation(), 4, 0.2, 0.2, 0.2, 0, darkRed);
            } else {
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText("§c[Circulation] Przejmowanie krążenia wroga..."));
                Particle.DustOptions bloodRed = new Particle.DustOptions(Color.fromRGB(130, 0, 0), 0.7f);
                player.getWorld().spawnParticle(Particle.DUST, target.getEyeLocation(), 2, 0.1, 0.1, 0.1, 0, bloodRed);
            }
        }
    }

    private void executeCirculationControl() {
        state = State.CONTROLLED;
        bPlayer.addCooldown(this, cooldown);

        player.getWorld().playSound(target.getLocation(), Sound.ENTITY_SPLASH_POTION_BREAK, 1.2f, 0.6f);
        player.getWorld().playSound(target.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.5f, 1.6f);

        // Force victim to walk around like an NPC/bot for walkDurationTicks
        new BukkitRunnable() {
            private int ticks = 0;
            private float targetYaw = target.getLocation().getYaw();

            @Override
            public void run() {
                ticks++;
                if (ticks > walkDurationTicks || target.isDead() || !target.isValid()) {
                    remove();
                    cancel();
                    return;
                }

                // Every 10 ticks pick a random new walking yaw direction
                if (ticks % 10 == 0) {
                    targetYaw = random.nextFloat() * 360.0f - 180.0f;
                }

                Location loc = target.getLocation();
                loc.setYaw(targetYaw);
                loc.setPitch(0.0f);

                Vector walkDir = loc.getDirection().normalize().multiply(0.28).setY(target.getVelocity().getY());
                target.teleport(loc);
                target.setVelocity(walkDir);

                Particle.DustOptions bloodRed = new Particle.DustOptions(Color.fromRGB(160, 0, 0), 0.8f);
                target.getWorld().spawnParticle(Particle.DUST, target.getEyeLocation(), 3, 0.15, 0.15, 0.15, 0, bloodRed);
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
        return "Przejmuje krążenie wroga poprzez ładowanie shiftem i zmusza go do losowego chodzenia jak bot/NPC.";
    }

    @Override
    public String getInstructions() {
        return "Przytrzymaj Shift patrząc na wroga aby go naładować, a następnie puść Shift!";
    }
}

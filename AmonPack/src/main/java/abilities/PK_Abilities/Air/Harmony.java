package Abilities.PK_Abilities.Air;

import Abilities.Bending.SoundAbility;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.util.DamageHandler;
import Plugin.AmonPackPlugin;
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

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Harmony extends SoundAbility implements AddonAbility {

    private long rhythmInterval;
    private long inputWindow;
    private double selfPenaltyStacks;
    private int slowDuration;
    private int nauseaDuration;
    private double projectileSpeed;
    private double stackCountPerHit;
    private double damage;
    private long cooldown;

    private boolean loopActive = false;
    private long lastCueTime = 0;
    private boolean waitingForClick = false;
    private boolean clickedInWindow = false;

    public Harmony(Player player) {
        super(player);

        if (!bPlayer.canBend(this)) {
            return;
        }

        this.rhythmInterval = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Air.Sound.Harmony.RhythmInterval",
                1000);
        this.inputWindow = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Air.Sound.Harmony.InputWindow", 500);
        this.selfPenaltyStacks = AmonPackPlugin.getAbilitiesConfig()
                .getDouble("AmonPack.Air.Sound.Harmony.SelfPenaltyStacks", 4.0);
        this.slowDuration = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Air.Sound.Harmony.SlowDuration", 60);
        this.nauseaDuration = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Air.Sound.Harmony.NauseaDuration",
                100);
        this.projectileSpeed = AmonPackPlugin.getAbilitiesConfig()
                .getDouble("AmonPack.Air.Sound.Harmony.ProjectileSpeed", 1.2);
        this.stackCountPerHit = AmonPackPlugin.getAbilitiesConfig()
                .getDouble("AmonPack.Air.Sound.Harmony.StackCountPerHit", 3.0);
        this.damage = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Air.Sound.Harmony.Damage", 3.0);
        this.cooldown = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Air.Sound.Harmony.Cooldown", 7000);

        this.loopActive = true;
        startRhythmLoop();
        fireSoundProjectile();

        start();
    }

    @Override
    public void progress() {
        if (player == null || !player.isOnline() || player.isDead()) {
            remove();
            return;
        }
    }

    private void startRhythmLoop() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!loopActive || player == null || !player.isOnline()) {
                    cancel();
                    return;
                }

                // Play audible cue signal
                lastCueTime = System.currentTimeMillis();
                waitingForClick = true;
                clickedInWindow = false;
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.5f);

                // Wait for input window expiry
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (!loopActive)
                            return;
                        if (waitingForClick && !clickedInWindow) {
                            applySelfPenalty();
                        }
                    }
                }.runTaskLater(AmonPackPlugin.plugin, (inputWindow / 50L) + 2L);

            }
        }.runTaskTimer(AmonPackPlugin.plugin, rhythmInterval / 50L, rhythmInterval / 50L);
    }

    public void onLeftClick() {
        if (!loopActive)
            return;

        long now = System.currentTimeMillis();
        long elapsed = now - lastCueTime;

        if (waitingForClick && elapsed >= 0 && elapsed <= inputWindow) {
            // Perfect rhythm match!
            clickedInWindow = true;
            waitingForClick = false;
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 2.0f);
            fireSoundProjectile();
        } else {
            // Failed timing (too early or too late)
            applySelfPenalty();
        }
    }

    private void applySelfPenalty() {
        loopActive = false;
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);

        SoundAbility.HandleDamage(player, player, selfPenaltyStacks);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, slowDuration, 1));
        player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, nauseaDuration, 1));

        bPlayer.addCooldown(this, cooldown);
        remove();
    }

    private void fireSoundProjectile() {
        Location eye = player.getEyeLocation();
        Vector dir = eye.getDirection().normalize().multiply(projectileSpeed);

        new BukkitRunnable() {
            private Location loc = eye.clone().add(dir.clone().multiply(1.2));
            private int ticks = 0;
            private double spinAngle = 0;
            private final Set<UUID> hitSet = new HashSet<>();

            @Override
            public void run() {
                ticks++;
                if (ticks > 35 || player == null || !player.isOnline()) {
                    cancel();
                    return;
                }

                spinAngle += 0.4;
                loc.add(dir);

                // Spinning sound note particle pattern
                for (int i = 0; i < 3; i++) {
                    double a = spinAngle + (i * (2 * Math.PI / 3));
                    Location pt = loc.clone().add(Math.cos(a) * 0.4, Math.sin(a) * 0.4, Math.sin(a) * 0.4);
                    loc.getWorld().spawnParticle(Particle.NOTE, pt, 1, 0, 0, 0, 0);
                }
                loc.getWorld().spawnParticle(Particle.SCULK_CHARGE_POP, loc, 3, 0.1, 0.1, 0.1, 0.02);

                for (Entity e : GeneralMethods.getEntitiesAroundPoint(loc, 1.8)) {
                    if (e instanceof LivingEntity le && e.getEntityId() != player.getEntityId()
                            && !hitSet.contains(e.getUniqueId())) {
                        hitSet.add(e.getUniqueId());
                        DamageHandler.damageEntity(le, damage, Harmony.this);
                        SoundAbility.HandleDamage(player, le, stackCountPerHit);
                        cancel();
                        return;
                    }
                }

                if (loc.getBlock().getType().isSolid()) {
                    cancel();
                }
            }
        }.runTaskTimer(AmonPackPlugin.plugin, 0L, 1L);
    }

    @Override
    public void remove() {
        super.remove();
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
        return "Harmony";
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
    }

    @Override
    public String getDescription() {
        return "Wypuszcza pocisk wibracji powietrza zadający obrażenia i nakładający poziom dźwięku na wroga. Można użyć wielokrotnie pod warunkiem zachowania idealnego rytmu.";
    }

    @Override
    public String getInstructions() {
        return "Użyj LPM aby wystrzelić pocisk. Powtórz kliknięcie w odpowiednim momencie, aby wystrzelić kolejny pocisk.";
    }
}

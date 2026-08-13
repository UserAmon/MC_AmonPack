package Abilities.PK_Abilities.Water;

import Plugin.AmonPackPlugin;
import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.BloodAbility;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Random;

public class VeinFlow extends BloodAbility implements AddonAbility {

    private long durationMs;
    private long cooldown;
    private double detectionDistance;
    private double stunChance;
    private long stunDurationMs;
    private long addedCooldownMs;

    private double chargeTimeMultiplier;
    private double damageMultiplier;
    private double healingMultiplier;

    private long startTime;
    private boolean alive = true;
    private Random random = new Random();
    private BossBar bossBar;

    public VeinFlow(Player player) {
        super(player);

        if (bPlayer.isOnCooldown(this) || !bPlayer.canBend(this)) {
            return;
        }

        if (VeinFlowManager.isActive(player)) {
            VeinFlow current = VeinFlowManager.getStance(player);
            if (current != null) {
                current.remove();
            }
            return;
        }

        loadConfig();

        this.startTime = System.currentTimeMillis();
        this.bossBar = Bukkit.createBossBar("§c🩸 VEIN FLOW STANCE 🩸", BarColor.RED, BarStyle.SOLID);
        this.bossBar.addPlayer(player);
        this.bossBar.setVisible(true);

        VeinFlowManager.registerStance(player, this);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_SPLASH_POTION_BREAK, 1.2f, 0.6f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.5f, 1.8f);

        start();
    }

    private void loadConfig() {
        this.cooldown = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Water.VeinFlow.Cooldown", 30000L);
        this.durationMs = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Water.VeinFlow.Duration", 15L) * 1000L;
        this.detectionDistance = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Water.VeinFlow.DetectionDistance", 15.0);
        this.stunChance = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Water.VeinFlow.StunChance", 0.35);
        this.stunDurationMs = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Water.VeinFlow.StunDurationMs", 1500L);
        this.addedCooldownMs = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Water.VeinFlow.AddedCooldownMs", 4000L);

        this.chargeTimeMultiplier = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Water.VeinFlow.Boost.ChargeTimeMultiplier", 0.6);
        this.damageMultiplier = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Water.VeinFlow.Boost.DamageMultiplier", 1.5);
        this.healingMultiplier = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Water.VeinFlow.Boost.HealingMultiplier", 1.5);
    }

    @Override
    public void progress() {
        if (player == null || !player.isOnline() || player.isDead()) {
            remove();
            return;
        }

        long elapsed = System.currentTimeMillis() - startTime;
        if (elapsed >= durationMs) {
            remove();
            return;
        }

        // Feet blood particles (low at feet level so vision is clear)
        Location feet = player.getLocation().add(0, 0.05, 0);
        Particle.DustOptions darkRed = new Particle.DustOptions(Color.fromRGB(150, 0, 0), 0.7f);
        player.getWorld().spawnParticle(Particle.DUST, feet, 3, 0.2, 0.02, 0.2, 0, darkRed);

        // Render ground blood trail to all enemies in range
        renderGroundBloodTrail();

        if (bossBar != null) {
            double pct = 1.0 - ((double) elapsed / durationMs);
            bossBar.setProgress(Math.max(0.0, Math.min(1.0, pct)));
        }
    }

    private void renderGroundBloodTrail() {
        Location start = player.getLocation();
        Particle.DustOptions trailRed = new Particle.DustOptions(Color.fromRGB(180, 0, 0), 0.8f);

        for (Entity e : GeneralMethods.getEntitiesAroundPoint(player.getLocation(), detectionDistance)) {
            if (e instanceof LivingEntity le && e.getEntityId() != player.getEntityId()) {
                Location target = le.getLocation();
                Vector dir = target.toVector().subtract(start.toVector()).setY(0);
                double dist = dir.length();
                if (dist > 0.5) {
                    dir.normalize();
                    for (double d = 0.5; d < dist; d += 0.8) {
                        Location pt = start.clone().add(dir.clone().multiply(d));
                        pt.setY(pt.getBlock().getY() + 0.05);
                        player.getWorld().spawnParticle(Particle.DUST, pt, 1, 0.02, 0, 0.02, 0, trailRed);
                    }
                }
            }
        }
    }

    public void tryApplyAttackBuffs(LivingEntity victim) {
        if (!alive || victim == null) return;
        if (player.getLocation().distance(victim.getLocation()) > detectionDistance) return;

        if (random.nextDouble() <= stunChance) {
            // Stun: Freeze movement AND camera rotation
            applyStun(victim, stunDurationMs);

            // Add Cooldown to victim's selected magic ability
            if (victim instanceof Player targetPlayer) {
                BendingPlayer targetBPlayer = BendingPlayer.getBendingPlayer(targetPlayer);
                if (targetBPlayer != null && targetBPlayer.getBoundAbilityName() != null) {
                    String selectedAbi = targetBPlayer.getBoundAbilityName();
                    if (!selectedAbi.isEmpty()) {
                        long curCd = 0;
                        if (targetBPlayer.isOnCooldown(selectedAbi) && targetBPlayer.getCooldowns().containsKey(selectedAbi)) {
                            curCd = targetBPlayer.getCooldowns().get(selectedAbi).getCooldown() - System.currentTimeMillis();
                        }
                        targetBPlayer.addCooldown(selectedAbi, Math.max(0, curCd) + addedCooldownMs);
                    }
                }
            }

            // Blood burst particles on victim
            Location vLoc = victim.getLocation().add(0, 1.0, 0);
            Particle.DustOptions bloodRed = new Particle.DustOptions(Color.fromRGB(160, 0, 0), 1.2f);
            vLoc.getWorld().spawnParticle(Particle.DUST, vLoc, 20, 0.4, 0.5, 0.4, 0, bloodRed);
            vLoc.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, vLoc, 6, 0.2, 0.3, 0.2, 0.1);
            vLoc.getWorld().playSound(vLoc, Sound.ENTITY_SPLASH_POTION_BREAK, 1.0f, 0.7f);
        }
    }

    private void applyStun(LivingEntity victim, long durationMs) {
        final Location lockLoc = victim.getLocation().clone();
        final int totalTicks = (int) (durationMs / 50L);

        new BukkitRunnable() {
            private int ticks = 0;

            @Override
            public void run() {
                ticks++;
                if (ticks > totalTicks || victim.isDead() || !victim.isValid()) {
                    cancel();
                    return;
                }

                // Lock location AND camera rotation
                victim.teleport(lockLoc);
                victim.setVelocity(new Vector(0, 0, 0));

                Particle.DustOptions bloodRed = new Particle.DustOptions(Color.fromRGB(160, 0, 0), 0.8f);
                lockLoc.getWorld().spawnParticle(Particle.DUST, lockLoc.clone().add(0, 1.0, 0), 3, 0.2, 0.3, 0.2, 0, bloodRed);
            }
        }.runTaskTimer(AmonPackPlugin.plugin, 0L, 1L);
    }

    public boolean isAlive() {
        return alive;
    }

    public double getChargeTimeMultiplier() {
        return chargeTimeMultiplier;
    }

    public double getDamageMultiplier() {
        return damageMultiplier;
    }

    public double getHealingMultiplier() {
        return healingMultiplier;
    }

    @Override
    public void remove() {
        if (!alive) return;
        alive = false;
        if (bossBar != null) {
            bossBar.removeAll();
            bossBar.setVisible(false);
        }
        VeinFlowManager.unregisterStance(player);
        bPlayer.addCooldown(this, cooldown);
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
        return "VeinFlow";
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
        remove();
    }

    @Override
    public String getDescription() {
        return "Aktywuje stan przepływu krwi. Wskazuje pod nogami krwisty ślad do wrogów, daje szansę na ogłuszenie i zablokowanie wybranego skilla wroga przy ataku oraz potęguje umiejętności krwi i leczenia.";
    }

    @Override
    public String getInstructions() {
        return "Kliknij LPM aby włączyć/wyłączyć stan VeinFlow.";
    }
}

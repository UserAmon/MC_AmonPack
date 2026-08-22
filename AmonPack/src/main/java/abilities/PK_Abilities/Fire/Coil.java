package Abilities.PK_Abilities.Fire;

import Abilities.Bending.SmokeAbility;
import Abilities.Util_Objects.SmokeSource;
import RPG.Levels.BendingTree.PlayerBendingBranch;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.FireAbility;
import com.projectkorra.projectkorra.ability.LightningAbility;
import com.projectkorra.projectkorra.util.DamageHandler;
import Plugin.AmonPackPlugin;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class Coil extends LightningAbility implements AddonAbility {

    private enum State {
        CHARGING, FIRED
    }

    private State state;
    private long startTime;
    private int maxRings;
    private long chargeIntervalPerRing;
    private int ringCount = 0;
    private double projectileSpeed;
    private double randomnessFactor;
    private int lightningVisualInterval;
    private double smokeRadius;
    private double aoeDamage;
    private double aoeKnockback;
    private double damagePerProjectile;
    private long cooldown;

    private boolean hasThunderMark;
    private long thunderDelayMs;
    private double thunderRadius;
    private double thunderDamage;

    private double ringAngle = 0;

    public Coil(Player player) {
        super(player);

        if (!bPlayer.canBend(this)) {
            return;
        }

        this.maxRings = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Fire.Coil.MaxRings", 3);
        this.chargeIntervalPerRing = AmonPackPlugin.getAbilitiesConfig()
                .getLong("AmonPack.Fire.Coil.ChargeIntervalPerRing", 1000);
        this.projectileSpeed = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Fire.Coil.ProjectileSpeed", 0.7);
        this.randomnessFactor = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Fire.Coil.RandomnessFactor",
                0.45);
        this.lightningVisualInterval = AmonPackPlugin.getAbilitiesConfig()
                .getInt("AmonPack.Fire.Coil.LightningVisualInterval", 6);
        this.smokeRadius = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Fire.Coil.SmokeRadius", 5.0);
        this.aoeDamage = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Fire.Coil.AoEDamage", 8.0);
        this.aoeKnockback = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Fire.Coil.AoEKnockback", 1.2);
        this.damagePerProjectile = AmonPackPlugin.getAbilitiesConfig()
                .getDouble("AmonPack.Fire.Coil.DamagePerProjectile", 4.5);
        this.cooldown = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Fire.Coil.Cooldown", 8000);

        PlayerBendingBranch branch = (AmonPackPlugin.levelsBending != null)
                ? AmonPackPlugin.levelsBending.GetBranchByPlayerName(player.getName())
                : null;
        this.hasThunderMark = (branch != null && (branch.hasUpgrade("CoilThunder") || branch.hasUpgrade("CoilStorm") || branch.hasUpgrade("CoilSmite")));
        this.thunderDelayMs = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Fire.Coil.ThunderDelayMs", 3000L);
        this.thunderRadius = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Fire.Coil.ThunderRadius", 5.0);
        this.thunderDamage = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Fire.Coil.ThunderDamage", 6.5);

        if (FirelordStanceManager.isActive(player)) {
            this.chargeIntervalPerRing = Math.max(100L, this.chargeIntervalPerRing / 2);
        }
        this.chargeIntervalPerRing = Math.max(1L, this.chargeIntervalPerRing);

        this.state = State.CHARGING;
        this.startTime = System.currentTimeMillis();

        start();
    }

    @Override
    public void progress() {
        if (player == null || !player.isOnline() || player.isDead()) {
            remove();
            return;
        }

        if (state == State.CHARGING) {
            if (!player.isSneaking()) {
                fire();
                return;
            }

            long elapsed = System.currentTimeMillis() - startTime;
            ringCount = Math.min(maxRings, (int) (elapsed / chargeIntervalPerRing) + 1);

            renderTideLockStyleSlowRings();

            if (elapsed % 600 < 50) {
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_COPPER_BREAK, 0.4f,
                        1.0f + (ringCount * 0.2f));
            }
        }
    }

    private void renderTideLockStyleSlowRings() {
        ringAngle += 0.08;
        Location center = player.getLocation().add(0, 1.0, 0);
        boolean isFirelord = FirelordStanceManager.isActive(player);

        for (int r = 0; r < ringCount; r++) {
            double radius = 1.4 + (r * 0.5);
            double yOffset = (r - 1) * 0.15;

            int points = 16;
            for (int i = 0; i < points; i++) {
                double angle = (2 * Math.PI / points) * i + (r % 2 == 0 ? ringAngle : -ringAngle);
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                Location pt = center.clone().add(x, yOffset + Math.sin(angle) * 0.2, z);

                if (isFirelord) {
                    player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, pt, 4, 0.02, 0.02, 0.02, 0.05);
                    player.getWorld().spawnParticle(Particle.DUST, pt, 1, 0, 0, 0, 0,
                            new org.bukkit.Particle.DustOptions(org.bukkit.Color.fromRGB(180, 220, 255), 1.0f));
                } else {
                    player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, pt, 1, 0.01, 0.01, 0.01, 0.01);
                    if (i % 4 == 0) {
                        player.getWorld().spawnParticle(Particle.FIREWORK, pt, 1, 0.01, 0.01, 0.01, 0.01);
                    }
                }
            }
        }
    }

    private void fire() {
        state = State.FIRED;
        Location eye = player.getEyeLocation();
        Vector baseDir = eye.getDirection().setY(0.02).normalize();

        player.getWorld().playSound(eye, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.8f, 1.6f);

        boolean isFirelord = FirelordStanceManager.isActive(player);
        FirelordStance stance = FirelordStanceManager.getStance(player);

        double actualSpeed = isFirelord ? projectileSpeed * stance.getSpeedMultiplier() : projectileSpeed;
        double actualDamage = isFirelord ? damagePerProjectile * stance.getDamageMultiplier() : damagePerProjectile;
        double actualSmokeRadius = isFirelord ? smokeRadius * stance.getRangeMultiplier() : smokeRadius;
        long actualCooldown = isFirelord ? (long) (cooldown * stance.getCooldownMultiplier()) : cooldown;

        for (int r = 0; r < ringCount; r++) {
            Vector spreadDir = (r == 0) ? baseDir.clone().multiply(actualSpeed) : baseDir.clone().add(new Vector(
                    (Math.random() - 0.5) * 0.8,
                    0.02,
                    (Math.random() - 0.5) * 0.8)).normalize().multiply(actualSpeed);

            spawnGroundLightningProjectile(eye.clone(), spreadDir, actualDamage, actualSmokeRadius, r == 0);
        }

        bPlayer.addCooldown(this, actualCooldown);
        remove();
    }

    private void spawnGroundLightningProjectile(Location startLoc, Vector initialVel, double currentDamage,
            double currentSmokeRadius, boolean isMain) {
        Abilities.Util_Objects.LightningBolt bolt = new Abilities.Util_Objects.LightningBolt(
                player, this, startLoc, initialVel.normalize(), currentDamage, 25.0, 0, false);

        Set<UUID> hitEnemies = new HashSet<>();

        new BukkitRunnable() {
            private int ticks = 0;

            @Override
            public void run() {
                ticks++;
                if (ticks > 50 || player == null || !player.isOnline() || bolt.isDead()) {
                    cancel();
                    return;
                }

                Location currentLoc = bolt.getLocation();

                if (ticks % lightningVisualInterval == 0) {
                    currentLoc.getWorld().strikeLightningEffect(currentLoc);
                }

                SmokeSource nearSource = SmokeAbility.UseSmokeSource(player, currentSmokeRadius);
                if (nearSource != null || checkSmokeSourcesNear(currentLoc, currentSmokeRadius)) {
                    triggerSmokeElectrification(currentLoc, currentSmokeRadius);
                    cancel();
                    return;
                }

                if (hasThunderMark) {
                    for (Entity e : GeneralMethods.getEntitiesAroundPoint(currentLoc, 1.6)) {
                        if (e instanceof LivingEntity le && e.getEntityId() != player.getEntityId()) {
                            if (!hitEnemies.contains(le.getUniqueId())) {
                                hitEnemies.add(le.getUniqueId());
                                triggerEnemyThunderMark(le.getLocation().clone(), thunderDelayMs, thunderRadius, thunderDamage);
                            }
                        }
                    }
                }

                List<Abilities.Util_Objects.LightningBolt> branches = bolt.progress();
                if (branches != null && !branches.isEmpty()) {
                    for (Abilities.Util_Objects.LightningBolt b : branches) {
                        b.progress();
                    }
                }

                if (currentLoc.getBlock().getType().isSolid()) {
                    cancel();
                }
            }
        }.runTaskTimer(AmonPackPlugin.plugin, 0L, 1L);
    }

    private void triggerEnemyThunderMark(Location markLoc, long delayMs, double radius, double dmg) {
        int totalTicks = Math.max(20, (int) (delayMs / 50L));
        Location center = markLoc.clone().add(0, 0.1, 0);

        for (int y = 2; y >= -3; y--) {
            Block b = center.clone().add(0, y, 0).getBlock();
            if (b.getType().isSolid()) {
                center.setY(b.getY() + 1.05);
                break;
            }
        }

        center.getWorld().playSound(center, Sound.BLOCK_BEACON_AMBIENT, 1.2f, 1.8f);

        new BukkitRunnable() {
            int t = 0;
            int lastSegments = 0;

            @Override
            public void run() {
                t++;
                if (t >= totalTicks) {
                    center.getWorld().playSound(center, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.5f, 0.9f);
                    center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 1.0f);
                    center.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, center, 2, 0.5, 0.5, 0.5, 0.0);
                    center.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, center, 40, radius * 0.5, 1.0, radius * 0.5, 0.2);

                    center.getWorld().strikeLightningEffect(center);
                    center.getWorld().strikeLightningEffect(center.clone().add(radius * 0.5, 0, 0));
                    center.getWorld().strikeLightningEffect(center.clone().add(-radius * 0.5, 0, 0));
                    center.getWorld().strikeLightningEffect(center.clone().add(0, 0, radius * 0.5));
                    center.getWorld().strikeLightningEffect(center.clone().add(0, 0, -radius * 0.5));

                    boolean isFirelord = FirelordStanceManager.isActive(player);
                    FirelordStance stance = FirelordStanceManager.getStance(player);
                    double actualDmg = isFirelord ? dmg * stance.getDamageMultiplier() : dmg;

                    for (Entity e : GeneralMethods.getEntitiesAroundPoint(center, radius)) {
                        if (e instanceof LivingEntity le && e.getEntityId() != player.getEntityId()) {
                            DamageHandler.damageEntity(le, actualDmg, Coil.this);
                            Vector push = le.getLocation().toVector().subtract(center.toVector()).normalize().multiply(1.2).setY(0.4);
                            le.setVelocity(push);
                        }
                    }
                    cancel();
                    return;
                }

                double progress = (double) t / totalTicks;
                int currentSegments = Math.min(12, (int) (progress * 13.0));

                if (currentSegments > lastSegments) {
                    lastSegments = currentSegments;
                    center.getWorld().playSound(center, Sound.BLOCK_NOTE_BLOCK_HAT, 0.8f, 1.0f + (currentSegments * 0.09f));
                    center.getWorld().playSound(center, Sound.BLOCK_COPPER_BREAK, 0.5f, 1.2f + (currentSegments * 0.06f));
                }

                int maxAngleDeg = currentSegments * 30;
                for (int deg = 0; deg <= maxAngleDeg; deg += 6) {
                    double rad = Math.toRadians(deg);
                    double x = Math.cos(rad) * radius;
                    double z = Math.sin(rad) * radius;
                    Location pLoc = center.clone().add(x, 0, z);

                    if (deg % 30 == 0) {
                        center.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, pLoc, 1, 0.02, 0.02, 0.02, 0.01);
                    }
                    center.getWorld().spawnParticle(Particle.DUST, pLoc, 1, 0, 0, 0, 0,
                            new Particle.DustOptions(org.bukkit.Color.fromRGB(160, 225, 255), 0.75f));
                }

                if (t % 4 == 0) {
                    double headRad = Math.toRadians(maxAngleDeg);
                    Location headLoc = center.clone().add(Math.cos(headRad) * radius, 0, Math.sin(headRad) * radius);
                    center.getWorld().spawnParticle(Particle.FIREWORK, headLoc, 2, 0.05, 0.05, 0.05, 0.02);
                }
            }
        }.runTaskTimer(AmonPackPlugin.plugin, 0L, 1L);
    }

    private boolean checkSmokeSourcesNear(Location targetLoc, double currentSmokeRadius) {
        SmokeSource source = SmokeAbility.UseSmokeSource(player, currentSmokeRadius);
        if (source != null) {
            SmokeAbility.DeleteSource(source);
            return true;
        }
        return false;
    }

    private void triggerSmokeElectrification(Location loc, double currentSmokeRadius) {
        loc.getWorld().playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.2f, 0.8f);
        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.2f);

        loc.getWorld().strikeLightningEffect(loc);
        loc.getWorld().strikeLightningEffect(loc.clone().add(1.5, 0, 1.5));
        loc.getWorld().strikeLightningEffect(loc.clone().add(-1.5, 0, -1.5));

        loc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, loc, 2, 0.5, 0.5, 0.5, 0.0);
        loc.getWorld().spawnParticle(Particle.LARGE_SMOKE, loc, 30, 1.5, 1.5, 1.5, 0.1);
        loc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, loc, 40, 2.0, 2.0, 2.0, 0.2);

        boolean isFirelord = FirelordStanceManager.isActive(player);
        FirelordStance stance = FirelordStanceManager.getStance(player);
        double actualAoEDamage = isFirelord ? aoeDamage * stance.getDamageMultiplier() : aoeDamage;

        for (Entity e : GeneralMethods.getEntitiesAroundPoint(loc, currentSmokeRadius)) {
            if (e instanceof LivingEntity le && e.getEntityId() != player.getEntityId()) {
                DamageHandler.damageEntity(le, actualAoEDamage, this);
                Vector push = le.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(aoeKnockback)
                        .setY(0.4);
                le.setVelocity(push);
            }
        }
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
        return "Coil";
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
        return "1.2";
    }

    @Override
    public void load() {
    }

    @Override
    public void stop() {
    }

	@Override
	public String getDescription() {
		return "Formuje wokół gracza cewki piorunowe generujące coraz silniejsze pierścienie elektryczne, a po puszczeniu wystrzeliwuje salwę błyskawic.";
	}

	@Override
	public String getInstructions() {
		return "Przytrzymaj SHIFT aby ładować kolejne pierścienie cewki, a następnie puść SHIFT aby wyładować energię.";
	}
}

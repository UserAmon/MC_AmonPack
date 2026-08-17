package Abilities.PK_Abilities.Fire;

import Abilities.Util_Objects.LightningBolt;
import Plugin.AmonPackPlugin;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.LightningAbility;
import com.projectkorra.projectkorra.util.DamageHandler;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ArcBlast extends LightningAbility implements AddonAbility {

    private enum State {
        CHARGING, FULLY_CHARGED, FIRING
    }

    private State state;
    private long cooldown;
    private double baseDamage;
    private double baseRange;
    private double baseSpeed;
    private int chainCount;
    private double chainRange;
    private double waterExtraDamage;
    private int burstCount;
    private int burstDelayTicks;
    private long fullyChargedStartTime = 0;

    private List<Vector> pointOffsets = new ArrayList<>();
    private boolean[] collectedPoints;
    private int collectedCount = 0;
    private int totalPointsRequired = 4;
    private double startY;

    private List<ArcBlastProjectile> projectiles = new ArrayList<>();
    private Random random = new Random();

    public ArcBlast(Player player) {
        super(player);

        if (hasAbility(player, ArcBlast.class)) {
            return;
        }
        if (bPlayer.isOnCooldown(this) || !bPlayer.canBend(this)) {
            return;
        }

        loadConfig();

        boolean isFirelord = FirelordStanceManager.isActive(player);
        this.totalPointsRequired = isFirelord ? 6 : 4;
        this.collectedPoints = new boolean[totalPointsRequired];
        this.startY = player.getLocation().getY();
        this.state = State.CHARGING;

        initTargetPoints();
        start();
    }

    private void loadConfig() {
        this.cooldown = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Fire.ArcBlast.Cooldown", 8000L);
        this.baseDamage = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Fire.ArcBlast.Damage", 4.5);
        this.baseRange = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Fire.ArcBlast.Range", 30.0);
        this.baseSpeed = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Fire.ArcBlast.Speed", 1.2);
        this.chainCount = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Fire.ArcBlast.ChainCount", 3);
        this.chainRange = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Fire.ArcBlast.ChainRange", 6.0);
        this.waterExtraDamage = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Fire.ArcBlast.WaterExtraDamage",
                4.0);
        this.burstCount = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Fire.ArcBlast.BurstCount", 4);
        this.burstDelayTicks = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Fire.ArcBlast.BurstDelayTicks", 3);
    }

    public boolean isFullyCharged() {
        return state == State.FULLY_CHARGED;
    }

    public void onClick() {
        if (state == State.FULLY_CHARGED) {
            fire();
        }
    }

    private void initTargetPoints() {
        Location eyeLoc = player.getEyeLocation();
        Vector forward = eyeLoc.getDirection().normalize();
        Vector right = new Vector(-forward.getZ(), 0, forward.getX()).normalize();
        if (right.lengthSquared() < 0.01) {
            right = new Vector(1, 0, 0);
        }
        Vector up = right.clone().crossProduct(forward).normalize();

        pointOffsets.clear();

        for (int i = 0; i < totalPointsRequired; i++) {
            double distance = 3.0 + (random.nextDouble() * 3.0);
            double offsetX = (random.nextDouble() - 0.5) * 4.5;
            double offsetY = (random.nextDouble() - 0.5) * 3.0;

            Vector pt = forward.clone().multiply(distance)
                    .add(right.clone().multiply(offsetX))
                    .add(up.clone().multiply(offsetY));
            pointOffsets.add(pt);
        }
    }

    @Override
    public void progress() {
        if (player == null || player.isDead() || !player.isOnline()) {
            remove();
            return;
        }

        if (state == State.CHARGING) {
            if (!player.isSneaking()) {
                remove();
                return;
            }

            if (FirelordStanceManager.isActive(player)) {
                Location loc = player.getLocation();
                if (loc.getY() < startY + 2.0) {
                    player.setVelocity(new Vector(player.getVelocity().getX(), 0.12, player.getVelocity().getZ()));
                } else {
                    player.setVelocity(new Vector(player.getVelocity().getX(), 0.01, player.getVelocity().getZ()));
                }
                player.getWorld().spawnParticle(Particle.FLAME, loc.clone().add(0, 0.1, 0), 3, 0.2, 0.1, 0.2, 0.02);
                player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, loc.clone().add(0, 0.2, 0), 3, 0.2, 0.2, 0.2,
                        0.05);
            }

            checkHoverCollection();

            Location eyeLoc = player.getEyeLocation();
            for (int i = 0; i < totalPointsRequired; i++) {
                if (!collectedPoints[i]) {
                    Location ptLoc = eyeLoc.clone().add(pointOffsets.get(i));
                    player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, ptLoc, 2, 0.1, 0.1, 0.1, 0.05);
                    Particle.DustOptions dust = new Particle.DustOptions(Color.fromRGB(180, 220, 255), 0.8f);
                    player.getWorld().spawnParticle(Particle.DUST, ptLoc, 1, 0, 0, 0, 0, dust);
                }
            }

            String bar = "§4[ArcBlast]&c Zbieranie punktów: §e" + collectedCount + "/" + totalPointsRequired;
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(bar));
        } else if (state == State.FULLY_CHARGED) {
            if (System.currentTimeMillis() - fullyChargedStartTime > 12000L) {
                remove();
                return;
            }

            Particle.DustOptions dust = new Particle.DustOptions(Color.fromRGB(135, 206, 250), 1.2f);
            player.getWorld().spawnParticle(Particle.DUST,
                    player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(1.5)), 5, 0.2, 0.2,
                    0.2, 0, dust);
            player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK,
                    player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(1.5)), 3, 0.15, 0.15, 0.15, 0.05);
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                    TextComponent.fromLegacyText("§aArcBlast gotowy! Kliknij LPM, aby wystrzelić!"));
        } else if (state == State.FIRING) {
            if (projectiles.isEmpty()) {
                remove();
                return;
            }
            List<ArcBlastProjectile> copy = new ArrayList<>(projectiles);
            for (ArcBlastProjectile proj : copy) {
                proj.progress();
                if (proj.isDead()) {
                    projectiles.remove(proj);
                }
            }
        }
    }

    private void checkHoverCollection() {
        Location eyeLoc = player.getEyeLocation();
        Vector lookDir = eyeLoc.getDirection().normalize();

        for (int i = 0; i < totalPointsRequired; i++) {
            if (!collectedPoints[i]) {
                Location targetLoc = eyeLoc.clone().add(pointOffsets.get(i));
                Vector toTarget = targetLoc.clone().subtract(eyeLoc).toVector().normalize();
                double angle = Math.toDegrees(lookDir.angle(toTarget));

                if (angle < 8.0) {
                    collectedPoints[i] = true;
                    collectedCount++;
                    player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.8f,
                            1.5f + (collectedCount * 0.1f));

                    if (collectedCount >= totalPointsRequired) {
                        state = State.FULLY_CHARGED;
                        fullyChargedStartTime = System.currentTimeMillis();
                        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.8f);
                    }
                }
            }
        }
    }

    public void fire() {
        if (state == State.FIRING) return;
        state = State.FIRING;

        boolean isFirelord = FirelordStanceManager.isActive(player);
        FirelordStance stance = FirelordStanceManager.getStance(player);

        double actualDamage = isFirelord ? baseDamage * stance.getDamageMultiplier() : baseDamage;
        double actualRange = isFirelord ? baseRange * stance.getRangeMultiplier() : baseRange;
        double actualSpeed = isFirelord ? baseSpeed * stance.getSpeedMultiplier() : baseSpeed;
        long actualCooldown = isFirelord ? (long) (cooldown * stance.getCooldownMultiplier()) : cooldown;

        bPlayer.addCooldown(this, actualCooldown);

        fireWave(actualDamage, actualRange, actualSpeed, isFirelord);

        if (burstCount > 0) {
            new BukkitRunnable() {
                private int count = 0;

                @Override
                public void run() {
                    if (player == null || !player.isOnline() || player.isDead()) {
                        cancel();
                        return;
                    }
                    count++;
                    fireSingleBurst(actualDamage * 0.8, actualRange, actualSpeed * 1.1);
                    if (count >= burstCount) {
                        cancel();
                    }
                }
            }.runTaskTimer(AmonPackPlugin.plugin, (long) burstDelayTicks, (long) burstDelayTicks);
        }
    }

    private void fireWave(double actualDamage, double actualRange, double actualSpeed, boolean isFirelord) {
        Location eye = player.getEyeLocation();
        Vector mainDir = eye.getDirection().normalize();

        projectiles.add(new ArcBlastProjectile(player, this, eye.clone(), mainDir.clone(), actualDamage, actualRange,
                actualSpeed, chainCount, false));

        if (isFirelord) {
            Vector right = mainDir.clone().crossProduct(new Vector(0, 1, 0)).normalize();
            if (right.lengthSquared() < 0.01)
                right = new Vector(1, 0, 0);

            Vector leftDir = mainDir.clone().add(right.clone().multiply(-0.35)).normalize();
            Vector rightDir = mainDir.clone().add(right.clone().multiply(0.35)).normalize();

            projectiles.add(new ArcBlastProjectile(player, this, eye.clone().add(right.clone().multiply(-0.5)), leftDir,
                    actualDamage * 0.7, actualRange * 0.8, actualSpeed * 0.9, Math.max(1, chainCount - 1), true));
            projectiles.add(new ArcBlastProjectile(player, this, eye.clone().add(right.clone().multiply(0.5)), rightDir,
                    actualDamage * 0.7, actualRange * 0.8, actualSpeed * 0.9, Math.max(1, chainCount - 1), true));
        }

        player.getWorld().playSound(eye, Sound.ITEM_TRIDENT_THUNDER, 1.2f, 1.2f);
    }

    private void fireSingleBurst(double dmg, double rng, double spd) {
        Location eye = player.getEyeLocation();
        Vector dir = eye.getDirection().normalize();
        projectiles.add(new ArcBlastProjectile(player, this, eye.clone(), dir.clone(), dmg, rng, spd, 1, false));
        player.getWorld().playSound(eye, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.8f, 1.8f);
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
        return "ArcBlast";
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
        return "Gromadzi energię piorunów unosząc się w powietrzu i kalibrując celownik na punkty. Wypuszcza długi naprowadzany promień pioruna przeskakujący na kolejne cele i rażący wodę.";
    }

    @Override
    public String getInstructions() {
        return "Przytrzymaj Shift aby unieść się i najechać celownikiem na punkty, a następnie puść Shift aby wystrzelić!";
    }

    private class ArcBlastProjectile {
        private Player player;
        private ArcBlast ability;
        private Location loc;
        private Vector dir;
        private double damage;
        private double maxDistance;
        private double speed;
        private int chainsRemaining;
        private boolean isSideBolt;

        private double distanceTraveled = 0;
        private boolean dead = false;
        private List<LivingEntity> hitEntities = new ArrayList<>();

        public ArcBlastProjectile(Player player, ArcBlast ability, Location loc, Vector dir, double damage,
                double maxDistance, double speed, int chainsRemaining, boolean isSideBolt) {
            this.player = player;
            this.ability = ability;
            this.loc = loc.clone();
            this.dir = dir.normalize();
            this.damage = damage;
            this.maxDistance = maxDistance;
            this.speed = speed;
            this.chainsRemaining = chainsRemaining;
            this.isSideBolt = isSideBolt;
        }

        public void progress() {
            if (dead)
                return;

            if (distanceTraveled >= maxDistance) {
                dead = true;
                return;
            }

            // Steer toward player view direction
            Vector playerLook = player.getEyeLocation().getDirection().normalize();
            double steerFactor = isSideBolt ? 0.08 : 0.15;
            dir = dir.clone().multiply(1.0 - steerFactor).add(playerLook.multiply(steerFactor)).normalize();

            // Add slight lightning randomness jitter
            dir.add(new Vector((random.nextDouble() - 0.5) * 0.12, (random.nextDouble() - 0.5) * 0.12,
                    (random.nextDouble() - 0.5) * 0.12)).normalize();

            RayTraceResult rtr = loc.getWorld().rayTraceBlocks(loc, dir, speed, FluidCollisionMode.NEVER, true);
            if (rtr != null && rtr.getHitBlock() != null) {
                Block b = rtr.getHitBlock();
                if (GeneralMethods.isSolid(b)) {
                    dead = true;
                    return;
                }
            } else {
                loc.add(dir.clone().multiply(speed));
            }

            distanceTraveled += speed;

            // Renders crackling lightning trail
            loc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, loc, 4, 0.15, 0.15, 0.15, 0.08);
            Particle.DustOptions dust = new Particle.DustOptions(Color.fromRGB(180, 230, 255), 1.0f);
            loc.getWorld().spawnParticle(Particle.DUST, loc, 2, 0.1, 0.1, 0.1, 0, dust);

            // Water interaction check
            boolean inWater = (loc.getBlock().getType() == Material.WATER);
            if (inWater) {
                triggerWaterDischarge(loc);
            }

            // Hit & chaining collision
            for (Entity entity : GeneralMethods.getEntitiesAroundPoint(loc, 1.4)) {
                if (entity instanceof LivingEntity target && entity.getEntityId() != player.getEntityId()
                        && !hitEntities.contains(target)) {
                    hitEntities.add(target);

                    double actualDamage = damage;
                    if (target.getLocation().getBlock().getType() == Material.WATER || target.isInWater()) {
                        actualDamage += waterExtraDamage;
                        triggerWaterDischarge(target.getLocation());
                    }

                    DamageHandler.damageEntity(target, actualDamage, ability);
                    target.getWorld().strikeLightningEffect(target.getLocation());
                    target.getWorld().playSound(target.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.0f, 1.2f);

                    if (chainsRemaining > 0) {
                        LivingEntity next = findNearestTarget(target.getLocation(), chainRange);
                        if (next != null) {
                            dir = GeneralMethods.getDirection(target.getLocation(), next.getLocation()).normalize();
                            loc = target.getLocation().clone().add(0, 0.5, 0);
                            chainsRemaining--;
                            return;
                        }
                    }
                    dead = true;
                    return;
                }
            }
        }

        private void triggerWaterDischarge(Location waterLoc) {
            waterLoc.getWorld().playSound(waterLoc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.8f, 1.4f);
            waterLoc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, waterLoc, 20, 1.5, 1.5, 1.5, 0.2);

            for (int i = 0; i < 4; i++) {
                Vector randDir = new Vector((random.nextDouble() - 0.5), 0.1, (random.nextDouble() - 0.5)).normalize();
                LightningBolt subBolt = new LightningBolt(player, ability, waterLoc, randDir, damage * 0.5, 8.0, 0,
                        false);
                subBolt.progress();
            }
        }

        private LivingEntity findNearestTarget(Location center, double radius) {
            LivingEntity nearest = null;
            double bestDist = radius * radius;
            for (Entity entity : GeneralMethods.getEntitiesAroundPoint(center, radius)) {
                if (entity instanceof LivingEntity target && entity.getEntityId() != player.getEntityId()
                        && !hitEntities.contains(target)) {
                    double dist = center.distanceSquared(target.getLocation());
                    if (dist < bestDist) {
                        bestDist = dist;
                        nearest = target;
                    }
                }
            }
            return nearest;
        }

        public boolean isDead() {
            return dead;
        }
    }
}

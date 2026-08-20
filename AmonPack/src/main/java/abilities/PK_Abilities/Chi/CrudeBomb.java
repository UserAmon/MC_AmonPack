package Abilities.PK_Abilities.Chi;

import Plugin.AmonPackPlugin;
import RPG.Levels.BendingTree.PlayerBendingBranch;
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
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CrudeBomb extends ChiAbility implements AddonAbility {

    public enum State { THROWN, SMOKE_CLOUD }

    private static final Map<UUID, Long> MULTI_THROWS = new ConcurrentHashMap<>();

    private State state;
    private long cooldown;
    private double throwForce;
    private double gravity;
    private double bounceDamping;
    private long fuseTimeMs;
    private double explosionRadius;
    private double explosionDamage;
    private double knockbackForce;
    private double smokeRadius;
    private long smokeDurationMs;
    private double tempMaxChiBonus;
    private double tempRegenBonus;
    private double chiCost;

    private boolean hasDouble = false;
    private boolean hasThickSmoke = false;
    private boolean hasFlashbang = false;

    private Location location;
    private Vector velocity;
    private long launchTime;
    private long smokeStartTime;
    private Location smokeCenter;
    private final Random random = new Random();

    public CrudeBomb(Player player) {
        super(player);

        if (bPlayer.isOnCooldown(this) || !bPlayer.canBend(this)) {
            return;
        }

        loadConfig();

        PlayerBendingBranch branch = (AmonPackPlugin.levelsBending != null) ? AmonPackPlugin.levelsBending.GetBranchByPlayerName(player.getName()) : null;
        if (branch != null) {
            hasDouble = branch.hasUpgrade("CrudeBombDouble");
            hasThickSmoke = branch.hasUpgrade("CrudeBombThickSmoke");
            hasFlashbang = branch.hasUpgrade("CrudeBombFlashbang");
        }

        if (hasThickSmoke) {
            this.smokeRadius = 7.0;
            this.smokeDurationMs = 14000L;
        }

        if (hasFlashbang) {
            this.chiCost = this.chiCost * 0.5;
        }

        if (!ChiManager.consumeChi(player, chiCost)) {
            return;
        }

        UUID uuid = player.getUniqueId();
        if (hasDouble) {
            Long lastThrow = MULTI_THROWS.get(uuid);
            if (lastThrow != null && (System.currentTimeMillis() - lastThrow <= 3000L)) {
                // Second throw in window!
                MULTI_THROWS.remove(uuid);
                bPlayer.addCooldown(this, cooldown);
            } else {
                // First throw in window!
                MULTI_THROWS.put(uuid, System.currentTimeMillis());
                AmonPackPlugin.plugin.getServer().getScheduler().runTaskLater(AmonPackPlugin.plugin, () -> {
                    Long t = MULTI_THROWS.remove(uuid);
                    if (t != null && bPlayer != null && !bPlayer.isOnCooldown(this)) {
                        bPlayer.addCooldown(this, cooldown);
                    }
                }, 60L); // 3 seconds window
            }
        } else {
            bPlayer.addCooldown(this, cooldown);
        }

        this.state = State.THROWN;
        this.launchTime = System.currentTimeMillis();
        this.location = player.getEyeLocation().clone();

        Vector dir = player.getEyeLocation().getDirection().normalize();
        this.velocity = dir.multiply(throwForce).add(new Vector(0, 0.2, 0));

        player.getWorld().playSound(location, Sound.ENTITY_SNOWBALL_THROW, 1.0f, 0.8f);
        player.getWorld().playSound(location, Sound.ENTITY_TNT_PRIMED, 0.8f, 1.4f);

        start();
    }

    private void loadConfig() {
        this.cooldown = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Chi.CrudeBomb.Cooldown", 8000L);
        this.throwForce = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Chi.CrudeBomb.ThrowForce", 1.3);
        this.gravity = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Chi.CrudeBomb.Gravity", 0.05);
        this.bounceDamping = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Chi.CrudeBomb.BounceDamping", 0.6);
        this.fuseTimeMs = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Chi.CrudeBomb.FuseTimeMs", 2000L);
        this.explosionRadius = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Chi.CrudeBomb.ExplosionRadius", 4.0);
        this.explosionDamage = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Chi.CrudeBomb.ExplosionDamage", 3.5);
        this.knockbackForce = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Chi.CrudeBomb.KnockbackForce", 1.0);
        this.smokeRadius = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Chi.CrudeBomb.SmokeRadius", 4.5);
        this.smokeDurationMs = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Chi.CrudeBomb.SmokeDurationMs", 7000L);
        this.tempMaxChiBonus = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Chi.CrudeBomb.TempMaxChiBonus", 50.0);
        this.tempRegenBonus = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Chi.CrudeBomb.TempRegenBonus", 5.0);
        this.chiCost = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Chi.CrudeBomb.ChiCost", 40.0);
    }

    @Override
    public void progress() {
        if (player == null || !player.isOnline()) {
            remove();
            return;
        }

        if (state == State.THROWN) {
            progressThrown();
        } else if (state == State.SMOKE_CLOUD) {
            progressSmokeCloud();
        }
    }

    private void progressThrown() {
        long elapsed = System.currentTimeMillis() - launchTime;

        if (elapsed >= fuseTimeMs) {
            explode();
            return;
        }

        // Apply gravity
        velocity.add(new Vector(0, -gravity, 0));

        // Raytrace for block collision
        double speed = velocity.length();
        if (speed > 0.001) {
            RayTraceResult result = location.getWorld().rayTraceBlocks(location, velocity.clone().normalize(), speed,
                    org.bukkit.FluidCollisionMode.NEVER, true);

            if (result != null && result.getHitBlock() != null) {
                Block hitBlock = result.getHitBlock();
                if (hitBlock.getType().isSolid()) {
                    BlockFace face = result.getHitBlockFace();
                    if (face != null) {
                        Vector normal = new Vector(face.getModX(), face.getModY(), face.getModZ());
                        double dot = velocity.dot(normal);
                        velocity.subtract(normal.multiply(2 * dot)).multiply(bounceDamping);

                        location.getWorld().playSound(location, Sound.BLOCK_ANVIL_LAND, 0.5f, 1.8f);
                        location.getWorld().playSound(location, Sound.BLOCK_STONE_HIT, 0.8f, 1.2f);
                    }
                    if (result.getHitPosition() != null) {
                        location = result.getHitPosition().toLocation(location.getWorld());
                    }
                }
            } else {
                location.add(velocity);
            }
        }

        // Particles for thrown grenade & fuse
        Particle.DustOptions bombCore = new Particle.DustOptions(Color.fromRGB(40, 40, 40), 1.2f);
        location.getWorld().spawnParticle(Particle.DUST, location, 2, 0.04, 0.04, 0.04, 0, bombCore);
        location.getWorld().spawnParticle(Particle.SMALL_FLAME, location.clone().add(0, 0.12, 0), 1, 0.02, 0.02, 0.02, 0.01);
        location.getWorld().spawnParticle(Particle.SMOKE, location, 1, 0.03, 0.03, 0.03, 0.01);

        if (random.nextInt(3) == 0) {
            location.getWorld().playSound(location, Sound.BLOCK_FIRE_AMBIENT, 0.4f, 1.8f);
        }
    }

    private void explode() {
        this.state = State.SMOKE_CLOUD;
        this.smokeStartTime = System.currentTimeMillis();
        this.smokeCenter = location.clone();

        // Explosion sound & visuals
        location.getWorld().playSound(location, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.1f);
        location.getWorld().playSound(location, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 0.8f, 0.6f);
        location.getWorld().spawnParticle(Particle.EXPLOSION, location, 1, 0.2, 0.2, 0.2, 0);
        location.getWorld().spawnParticle(Particle.LAVA, location, 4, 0.2, 0.2, 0.2, 0.05);
        location.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, location, 8, 0.3, 0.3, 0.3, 0.02);

        if (hasFlashbang) {
            location.getWorld().spawnParticle(Particle.FLASH, location, 2, 0.5, 0.5, 0.5, 0);
            location.getWorld().playSound(location, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 1.5f, 0.5f);
            for (Entity e : GeneralMethods.getEntitiesAroundPoint(location, 8.0)) {
                if (e instanceof LivingEntity victim && e.getEntityId() != player.getEntityId()) {
                    victim.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 80, 0, false, true));
                    victim.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 100, 0, false, true));
                }
            }
        }

        // Damage & knockback entities in explosion radius
        for (Entity e : GeneralMethods.getEntitiesAroundPoint(location, explosionRadius)) {
            if (e instanceof LivingEntity victim && e.getEntityId() != player.getEntityId()) {
                DamageHandler.damageEntity(victim, explosionDamage, this);

                Vector kb = victim.getLocation().toVector().subtract(location.toVector()).normalize().multiply(knockbackForce).setY(0.35);
                victim.setVelocity(kb);
            }
        }
    }

    private void progressSmokeCloud() {
        long elapsed = System.currentTimeMillis() - smokeStartTime;

        if (elapsed >= smokeDurationMs) {
            remove();
            return;
        }

        // Render subtle smoke cloud
        int particleCount = 4;
        for (int i = 0; i < particleCount; i++) {
            double u = random.nextDouble();
            double v = random.nextDouble();
            double theta = u * 2.0 * Math.PI;
            double phi = Math.acos(2.0 * v - 1.0);
            double r = Math.cbrt(random.nextDouble()) * smokeRadius;
            double sinPhi = Math.sin(phi);
            double x = r * sinPhi * Math.cos(theta);
            double y = (r * Math.cos(phi)) * 0.6 + 0.4;
            double z = r * sinPhi * Math.sin(theta);

            Location pLoc = smokeCenter.clone().add(x, y, z);
            pLoc.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, pLoc, 1, 0.01, 0.01, 0.01, 0.005);
        }

        // Apply effects to entities inside smoke cloud
        for (Entity e : GeneralMethods.getEntitiesAroundPoint(smokeCenter, smokeRadius)) {
            if (e instanceof LivingEntity victim) {
                if (victim.getEntityId() != player.getEntityId()) {
                    // Enemies get blinded and slowed
                    victim.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 0, false, false));
                    victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 0, false, false));
                }
            }
        }

        // Caster benefits while inside smoke cloud
        if (player.getLocation().distance(smokeCenter) <= smokeRadius) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 30, 0, false, false, false));
            ChiManager.addTempMaxChi(player, "CrudeBomb_Smoke", tempMaxChiBonus, 30L);
            ChiManager.addTempRegen(player, "CrudeBomb_Smoke", tempRegenBonus, 30L);
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                    TextComponent.fromLegacyText(String.format("§8☁ §7Ukryty w dymie §a(+%.0f Max Chi, +%.1f Chi/s)", tempMaxChiBonus, tempRegenBonus)));
        }
    }

    @Override
    public long getCooldown() {
        return cooldown;
    }

    @Override
    public Location getLocation() {
        return location != null ? location : (player != null ? player.getLocation() : null);
    }

    @Override
    public String getName() {
        return "CrudeBomb";
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
        return "Rzucasz granatem (LPM), który odbija się od ścian i wybucha po 2 sekundach zadając obrażenia i odrzucając cele. Po wybuchu pozostawia chmurę dymu, w której user jest niewidzialny, zyskuje +50 tymczasowego Max Chi oraz +5 Chi/s regeneracji, a wrogowie są oślepieni.";
    }

    @Override
    public String getInstructions() {
        return "Kliknij LPM, aby rzucić granatem dymnym!";
    }
}

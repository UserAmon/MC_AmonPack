package Abilities.PK_Abilities.Water;

import Abilities.Bending.SpecialTriggerable;
import Abilities.Bending.SpecialTriggerManager;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.WaterAbility;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.ParticleEffect;
import com.projectkorra.projectkorra.util.TempBlock;
import Plugin.AmonPackPlugin;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
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

import java.util.HashSet;
import java.util.Set;

public class VineWave extends WaterAbility implements AddonAbility, SpecialTriggerable {

    private int state = 0; // 0 = Tkanie wzoru V, 1 = Ładowanie lodu w obu dłoniach, 2 = Wystrzelenie fali pnączy, 3 = Koniec
    private int weaveTicks = 0;
    private final int maxWeaveTicks = 50; // 2.5s na tkanie

    private int iceChargeTicks = 0;
    private final int maxIceChargeTicks = 20; // 1s ładowania lodu

    private Location waveLoc;
    private Vector waveDir;
    private double waveDistance = 0.0;
    private double maxRange;
    private double speed;
    private double damage;
    private double knockback;
    private long cooldown;
    private int ricochets = 0;
    private final int maxRicochets = 3;

    private int currentCheckPoint = 0; // 0: Góra lewo, 1: Dół środek, 2: Góra prawo
    private final Set<LivingEntity> hitEntities = new HashSet<>();

    public VineWave(Player player) {
        super(player);

        if (bPlayer.isOnCooldown(this) || !bPlayer.canBendIgnoreBinds(this)) {
            return;
        }

        this.damage = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Water.VineWave.Damage", 5.5);
        this.maxRange = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Water.VineWave.MaxRange", 25.0);
        this.speed = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Water.VineWave.Speed", 1.1);
        this.knockback = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Water.VineWave.Knockback", 0.6);
        this.cooldown = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Water.VineWave.Cooldown", 7000);

        SpecialTriggerManager.registerActiveSpecial(player);
        start();
    }

    @Override
    public void progress() {
        if (player == null || player.isDead() || !player.isOnline()) {
            finish();
            return;
        }

        // --- FAZA 1: Tkanie wzoru litery "V" w powietrzu przed graczką/graczem ---
        if (state == 0) {
            weaveTicks++;

            Location centerAnchor = player.getEyeLocation().add(player.getEyeLocation().getDirection().clone().normalize().multiply(2.2));
            Vector right = player.getEyeLocation().getDirection().clone().crossProduct(new Vector(0, 1, 0)).normalize();
            Vector up = new Vector(0, 1, 0);

            // Punkty wzoru V (Góra Lewo -> Dół Środek -> Góra Prawo)
            Location pt1 = centerAnchor.clone().add(right.clone().multiply(-0.7)).add(up.clone().multiply(0.5));
            Location pt2 = centerAnchor.clone().add(up.clone().multiply(-0.4));
            Location pt3 = centerAnchor.clone().add(right.clone().multiply(0.7)).add(up.clone().multiply(0.5));

            // Rysowanie cząsteczek wzoru V
            displayVLine(pt1, pt2);
            displayVLine(pt2, pt3);

            // Śledzenie celu kamery gracza
            Location eye = player.getEyeLocation();
            if (currentCheckPoint == 0 && eye.distanceSquared(pt1) < 2.5) {
                currentCheckPoint = 1;
                player.getWorld().playSound(pt1, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6f, 1.2f);
            } else if (currentCheckPoint == 1 && eye.distanceSquared(pt2) < 2.5) {
                currentCheckPoint = 2;
                player.getWorld().playSound(pt2, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6f, 1.4f);
            } else if (currentCheckPoint == 2 && eye.distanceSquared(pt3) < 2.5) {
                currentCheckPoint = 3;
                player.getWorld().playSound(pt3, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.6f);
            }

            if (currentCheckPoint == 3 || weaveTicks >= maxWeaveTicks) {
                state = 1;
                iceChargeTicks = 0;
            }
        }
        // --- FAZA 2: 1 Sekunda ładowania cząsteczek lodu w obu rękach ---
        else if (state == 1) {
            iceChargeTicks++;

            Location leftHand = getHandLocation(-0.45);
            Location rightHand = getHandLocation(0.45);

            ParticleEffect.CRIT_MAGIC.display(leftHand, 4, 0.05, 0.05, 0.05, 0.01);
            ParticleEffect.CRIT_MAGIC.display(rightHand, 4, 0.05, 0.05, 0.05, 0.01);
            ParticleEffect.WATER_DROP.display(leftHand, 3, 0.05, 0.05, 0.05, 0.01);
            ParticleEffect.WATER_DROP.display(rightHand, 3, 0.05, 0.05, 0.05, 0.01);

            if (iceChargeTicks % 6 == 0) {
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 0.6f, 1.4f);
            }

            if (iceChargeTicks >= maxIceChargeTicks) {
                state = 2;
                waveLoc = player.getLocation().clone().add(0, 0.2, 0);
                waveDir = player.getEyeLocation().getDirection().clone().setY(0).normalize();
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.2f, 0.8f);
            }
        }
        // --- FAZA 3: Wystrzelenie fali pnączy (Podskakiwanie w pionie + Rykoszet poziomy od ścian) ---
        else if (state == 2) {
            waveDistance += speed;
            if (waveDistance >= maxRange || ricochets > maxRicochets) {
                finish();
                return;
            }

            // Fizyka podskakiwania (Parabola odbijająca się pionowo co 3.0 bloki)
            double bounceCycle = (waveDistance % 3.0) / 3.0;
            double yBounce = Math.abs(Math.sin(bounceCycle * Math.PI)) * 1.4;

            Vector stepVelocity = waveDir.clone().multiply(speed);

            // Rykoszet od ścian (Pojedyńczy promieniorozchodzący się Poziomo jak w EarthDisc)
            RayTraceResult rayTrace = waveLoc.getWorld().rayTraceBlocks(waveLoc, stepVelocity, speed, FluidCollisionMode.NEVER, true);
            if (rayTrace != null && rayTrace.getHitBlock() != null && rayTrace.getHitBlock().getType().isSolid()) {
                BlockFace face = rayTrace.getHitBlockFace();
                if (face != null && face != BlockFace.UP && face != BlockFace.DOWN) {
                    Vector normal = new Vector(face.getModX(), 0, face.getModZ()).normalize();
                    double dot = waveDir.dot(normal);
                    waveDir.subtract(normal.multiply(2 * dot)).normalize();

                    ricochets++;
                    player.getWorld().playSound(waveLoc, Sound.BLOCK_GRASS_BREAK, 1.0f, 1.5f);
                    ParticleEffect.VILLAGER_HAPPY.display(waveLoc, 12, 0.3, 0.3, 0.3, 0.1);
                }
            }

            waveLoc.add(waveDir.clone().multiply(speed));
            Location currentPoint = waveLoc.clone().add(0, yBounce, 0);

            // Efekty cząsteczkowe pnączy i wody
            ParticleEffect.VILLAGER_HAPPY.display(currentPoint, 5, 0.15, 0.15, 0.15, 0.03);
            ParticleEffect.WATER_SPLASH.display(currentPoint, 5, 0.15, 0.15, 0.15, 0.03);
            ParticleEffect.SLIME.display(currentPoint, 3, 0.1, 0.1, 0.1, 0.02);

            Block b = currentPoint.getBlock();
            if (b.getType() == Material.AIR) {
                new TempBlock(b, Material.OAK_LEAVES).setRevertTime(220);
            }

            // Zadawanie obrażeń i efekt spowolnienia
            for (Entity entity : GeneralMethods.getEntitiesAroundPoint(currentPoint, 1.5)) {
                if (entity instanceof LivingEntity && !entity.getUniqueId().equals(player.getUniqueId())) {
                    LivingEntity target = (LivingEntity) entity;
                    if (!hitEntities.contains(target)) {
                        hitEntities.add(target);
                        DamageHandler.damageEntity(target, damage, this);
                        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1));
                        target.setVelocity(waveDir.clone().multiply(knockback).setY(0.25));
                        player.getWorld().playSound(target.getLocation(), Sound.BLOCK_GRASS_STEP, 1.0f, 1.2f);
                    }
                }
            }
        }
    }

    private void displayVLine(Location start, Location end) {
        int points = 6;
        for (int i = 0; i <= points; i++) {
            double ratio = (double) i / points;
            Location loc = start.clone().add(end.clone().subtract(start).toVector().multiply(ratio));
            ParticleEffect.VILLAGER_HAPPY.display(loc, 1, 0.02, 0.02, 0.02, 0.01);
            ParticleEffect.WATER_WAKE.display(loc, 1, 0.02, 0.02, 0.02, 0.01);
        }
    }

    private Location getHandLocation(double sideOffset) {
        Location base = player.getLocation().clone().add(0, 1.1, 0);
        Vector forward = player.getLocation().getDirection().clone().setY(0).normalize();
        Vector right = forward.clone().crossProduct(new Vector(0, 1, 0)).normalize();
        return base.add(forward.multiply(0.3)).add(right.multiply(sideOffset));
    }

    private void finish() {
        SpecialTriggerManager.unregisterActiveSpecial(player);
        bPlayer.addCooldown(this);
        remove();
    }

    @Override
    public TriggerType getSupportedTriggerType() {
        return TriggerType.BOTH;
    }

    @Override
    public long getCooldown() {
        return cooldown;
    }

    @Override
    public Location getLocation() {
        return waveLoc != null ? waveLoc : (player != null ? player.getLocation() : null);
    }

    @Override
    public String getName() {
        return "VineWave";
    }

    @Override
    public boolean isHarmlessAbility() {
        return false;
    }

    @Override
    public boolean isSneakAbility() {
        return false;
    }

    @Override
    public String getAuthor() {
        return "Amon";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public void load() {}

    @Override
    public void stop() {}
}

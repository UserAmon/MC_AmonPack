package abilities;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.WaterAbility;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.ParticleEffect;
import com.projectkorra.projectkorra.util.TempBlock;

import abilities.Util_Objects.BetterParticles;
import methods_plugins.Methods;

public class TideLock extends WaterAbility implements AddonAbility {

    private enum State {
        IDLE, CHARGING, LAUNCHED, MARKING, LOCKING
    }

    private State state;
    private long startTime;
    private long chargeTime = 2000;
    private long markDuration = 5000;
    private long lockDuration = 3000;
    private long cooldown = 6000;
    private double damage = 2.0;
    private double burstDamage = 3.0;
    private double range = 25;
    private double speed = 1;

    private List<Location> waterRing;
    private LivingEntity markedTarget;
    private long markStartTime;
    private long lockStartTime;

    // Projectile Variables
    private int slot;
    private Location projectileLoc;
    private Vector projectileDir;
    private boolean isReturning;

    public TideLock(Player player) {
        super(player);
        if (bPlayer.isOnCooldown(this)) {
            return;
        }
        if (!bPlayer.canBend(this)) {
            return;
        }

        this.slot = player.getInventory().getHeldItemSlot();
        this.state = State.IDLE;
        this.waterRing = new ArrayList<>();
        start();
    }

    @Override
    public void progress() {
        if (player.isDead() || !player.isOnline()) {
            remove();
            return;
        }

        if (player.getInventory().getHeldItemSlot() != slot) {
            remove();
            return;
        }

        switch (state) {
            case IDLE:
                if (player.isSneaking()) {
                    state = State.CHARGING;
                    startTime = System.currentTimeMillis();
                    Location target = Methods.getTargetLocation(player, 10);
                    if (isWaterbendable(target.getBlock()) || isWaterbendable(player.getLocation().getBlock())) {
                        waterRing.add(target);
                    } else {
                        remove();
                    }
                } else {
                    remove();
                }
                break;

            case CHARGING:
                if (!player.isSneaking()) {
                    if (System.currentTimeMillis() - startTime > 1000) {
                        fire();
                    } else {
                        remove();
                    }
                } else {
                    waterRing = Methods.BendableBlocksAnimation(waterRing, player.getLocation(), Material.WATER, 1);
                    if (waterRing.isEmpty()) {
                        Animation(player.getEyeLocation(),2.5, Material.WATER);
                    }
                }
                break;

            case LAUNCHED:
                if (projectileLoc == null) {
                    remove();
                    return;
                }

                if (player.isSneaking()) {
                    isReturning = true;
                    projectileDir = GeneralMethods.getDirection(projectileLoc, player.getEyeLocation()).normalize();
                } else if (isReturning) {
                    remove();
                    return;
                }

                projectileLoc.add(projectileDir.clone().multiply(speed));

                if (isReturning) {
                    new BetterParticles(5, ParticleEffect.WATER_WAKE, 0.2, 0.2, 0.2).Display(projectileLoc);
                } else {
                    new BetterParticles(3, ParticleEffect.WATER_WAKE, 0, 0, 0).Display(projectileLoc);
                }

                TempBlock tb2 = new TempBlock(projectileLoc.getBlock(), Material.WATER);
                tb2.setRevertTime(200);

                for (Entity entity : GeneralMethods.getEntitiesAroundPoint(projectileLoc, isReturning ? 2.0 : 1.5)) {
                    if (entity instanceof LivingEntity && !entity.getUniqueId().equals(player.getUniqueId())) {
                        DamageHandler.damageEntity(entity, damage, TideLock.this);
                        markTarget((LivingEntity) entity);
                        return;
                    }
                }

                if (isReturning && projectileLoc.distanceSquared(player.getEyeLocation()) < 2) {
                    state = State.CHARGING;
                    startTime = System.currentTimeMillis() - chargeTime;
                    waterRing.clear();
                    waterRing.add(projectileLoc.clone());
                    projectileLoc = null;
                    isReturning = false;
                    return;
                }

                if (projectileLoc.getBlock().getType().isSolid()) {
                    remove();
                    return;
                }

                if (projectileLoc.distanceSquared(player.getEyeLocation()) > range * range) {
                    remove();
                    return;
                }
                break;

            case MARKING:
                if (System.currentTimeMillis() - markStartTime > markDuration) {
                    remove();
                }
                if (markedTarget != null && !markedTarget.isDead()) {
                    if (player.isSneaking()) {
                        startLocking();
                    }
                    player.getWorld().spawnParticle(Particle.DRIPPING_WATER, markedTarget.getLocation(), 3,
                            0.2, 0.2, 0.2, 0);
                    Animation(markedTarget.getEyeLocation(),2, Material.WATER);
                    markedTarget.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20, 2, false, false));
                } else {
                    remove();
                }
                break;

            case LOCKING:
                if (markedTarget == null || markedTarget.isDead()) {
                    remove();
                    return;
                }
                if(!player.isSneaking()){
                    remove();
                    return;
                }
                if (System.currentTimeMillis() - lockStartTime > lockDuration) {
                    burst();
                    return;
                }
                if (Math.random() < 0.25) {
                    Methods.spawnFallingBlocks(markedTarget.getLocation(), Material.ICE, 1, 0.5, null);
                }
                int amplifier = (int) (1+((System.currentTimeMillis() - lockStartTime) / 1000));
                    Animation(markedTarget.getEyeLocation(),(1.25+((double) lockDuration /1000))-amplifier, Material.ICE);
                markedTarget.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20, amplifier, false, false));
                for (Block b : GeneralMethods.getBlocksAroundPoint(markedTarget.getLocation(), 3)) {
                    if (isWater(b)) {
                        new TempBlock(b, Material.ICE).setRevertTime(200);
                    }
                }
                break;
        }
    }

    private void Animation(Location location, double radius, Material mat){
        {
            double time = System.currentTimeMillis() / 1000.0;

            double angle1 = time * 4;
            double x1 = radius * Math.cos(angle1);
            double z1 = radius * Math.sin(angle1);

            TempBlock tb2 = new TempBlock(location.clone().add(x1, -0.5, z1).getBlock(), mat);
            tb2.setRevertTime(100);

            double angle2 = time * 4 + Math.PI;
            double x2 = radius * Math.cos(angle2);
            double z2 = radius * Math.sin(angle2);

            TempBlock tb1 = new TempBlock(location.clone().add(x2, -0.5, z2).getBlock(), mat);
            tb1.setRevertTime(100);
        }
    }

    private void fire() {
        state = State.LAUNCHED;
        projectileLoc = player.getEyeLocation();
        projectileDir = player.getLocation().getDirection();
        isReturning = false;
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_SPLASH, 1f, 1f);
    }

    private void markTarget(LivingEntity target) {
        state = State.MARKING;
        markedTarget = target;
        markStartTime = System.currentTimeMillis();
    }

    public void startLocking() {
        if (state != State.MARKING || markedTarget == null)
            return;
        state = State.LOCKING;
        lockStartTime = System.currentTimeMillis();
        bPlayer.addCooldown(this);
    }

    private void burst() {
        if (markedTarget != null) {
            DamageHandler.damageEntity(markedTarget, burstDamage, this);

            Methods.FreezeTarget(markedTarget.getLocation(), 1, 2, 2, 2000, Material.ICE);

            markedTarget.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 5, false, false)); // Freeze
            player.getWorld().spawnParticle(Particle.SNOWFLAKE, markedTarget.getLocation(), 50, 1, 1, 1, 0.5);
            player.playSound(markedTarget.getLocation(), Sound.BLOCK_GLASS_BREAK, 1f, 1f);
        }
        remove();
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
        return player.getLocation();
    }

    @Override
    public String getName() {
        return "TideLock";
    }

    @Override
    public boolean isHarmlessAbility() {
        return false;
    }

    @Override
    public boolean isSneakAbility() {
        return true;
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
}

package Abilities.PK_Abilities.Water;

import Plugin.AmonPackPlugin;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.IceAbility;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.TempBlock;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class FrostGrip extends IceAbility implements AddonAbility {

    private enum State { ABSORBING, READY, LAUNCHED }

    private State state;
    private long cooldown;
    private double damage;
    private double sourceRange;
    private int slowDuration;
    private int slowAmplifier;
    private int noJumpDurationTicks;

    private Block sourceBlock;
    private Location floatLoc;
    private TempBlock floatingTempBlock;
    private boolean launched = false;

    public FrostGrip(Player player) {
        super(player);

        if (hasAbility(player, FrostGrip.class)) {
            return;
        }
        if (bPlayer.isOnCooldown(this) || !bPlayer.canBend(this)) {
            return;
        }

        loadConfig();

        Block target = player.getTargetBlockExact((int) sourceRange, org.bukkit.FluidCollisionMode.ALWAYS);
        if (target == null || !(target.getType() == Material.WATER || target.getType() == Material.ICE || target.getType() == Material.PACKED_ICE || isWaterbendable(target))) {
            if (player.getLocation().getBlock().getType() == Material.WATER) {
                target = player.getLocation().getBlock();
            } else {
                return;
            }
        }

        this.sourceBlock = target;
        this.floatLoc = target.getLocation().add(0.5, 0.5, 0.5);
        this.state = State.ABSORBING;

        player.getWorld().playSound(floatLoc, Sound.ITEM_BUCKET_EMPTY, 0.8f, 1.4f);
        start();
    }

    private void loadConfig() {
        this.cooldown = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Water.Ice.FrostGrip.Cooldown", 7000L);
        this.damage = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Water.Ice.FrostGrip.Damage", 4.5);
        this.sourceRange = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Water.Ice.FrostGrip.SourceRange", 15.0);
        this.slowDuration = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Water.Ice.FrostGrip.SlowDuration", 60);
        this.slowAmplifier = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Water.Ice.FrostGrip.SlowAmplifier", 2);
        this.noJumpDurationTicks = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Water.Ice.FrostGrip.NoJumpDurationTicks", 60);
    }

    @Override
    public void progress() {
        if (player == null || !player.isOnline() || player.isDead()) {
            cleanupTempBlock();
            remove();
            return;
        }

        if (state == State.ABSORBING || state == State.READY) {
            if (!player.isSneaking()) {
                if (state == State.READY) {
                    launch();
                    return;
                } else {
                    cleanupTempBlock();
                    remove();
                    return;
                }
            }

            Location targetFloatLoc = player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(2.5));

            if (state == State.ABSORBING) {
                floatLoc.add(targetFloatLoc.clone().subtract(floatLoc).toVector().multiply(0.25));
                floatLoc.getWorld().spawnParticle(Particle.SPLASH, floatLoc, 4, 0.1, 0.1, 0.1, 0.05);

                if (floatLoc.distance(targetFloatLoc) < 0.6) {
                    state = State.READY;
                    player.getWorld().playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 0.8f, 1.5f);
                }
            } else if (state == State.READY) {
                floatLoc = targetFloatLoc.clone();
                floatLoc.getWorld().spawnParticle(Particle.SNOWFLAKE, floatLoc, 2, 0.2, 0.2, 0.2, 0.01);
            }

            updateFloatingBlock(floatLoc);
        }
    }

    private void updateFloatingBlock(Location loc) {
        Block b = loc.getBlock();
        if (floatingTempBlock == null || !floatingTempBlock.getBlock().getLocation().equals(b.getLocation())) {
            cleanupTempBlock();
            if (b.getType() == Material.AIR) {
                floatingTempBlock = new TempBlock(b, Material.ICE.createBlockData(), 200);
            }
        }
    }

    private void cleanupTempBlock() {
        if (floatingTempBlock != null) {
            floatingTempBlock.revertBlock();
            floatingTempBlock = null;
        }
    }

    private void launch() {
        state = State.LAUNCHED;
        cleanupTempBlock();

        Location eye = player.getEyeLocation();
        Vector launchDir = eye.getDirection().normalize();

        player.getWorld().playSound(floatLoc, Sound.ENTITY_SNOWBALL_THROW, 1.0f, 0.8f);

        new BukkitRunnable() {
            private Location cur = floatLoc.clone();
            private int ticks = 0;
            private TempBlock projTempBlock;

            @Override
            public void run() {
                ticks++;
                if (projTempBlock != null) {
                    projTempBlock.revertBlock();
                    projTempBlock = null;
                }

                if (ticks > 30 || player == null || !player.isOnline() || cur.getBlock().getType().isSolid()) {
                    bPlayer.addCooldown(FrostGrip.this, cooldown);
                    remove();
                    cancel();
                    return;
                }

                cur.add(launchDir.clone().multiply(1.2));
                cur.getWorld().spawnParticle(Particle.SNOWFLAKE, cur, 6, 0.2, 0.2, 0.2, 0.05);

                Block b = cur.getBlock();
                if (b.getType() == Material.AIR) {
                    projTempBlock = new TempBlock(b, Material.ICE.createBlockData(), 100);
                }

                for (Entity entity : GeneralMethods.getEntitiesAroundPoint(cur, 1.5)) {
                    if (entity instanceof LivingEntity target && entity.getEntityId() != player.getEntityId()) {
                        if (projTempBlock != null) projTempBlock.revertBlock();
                        applyFreezeEffects(target);
                        bPlayer.addCooldown(FrostGrip.this, cooldown);
                        remove();
                        cancel();
                        return;
                    }
                }
            }
        }.runTaskTimer(AmonPackPlugin.plugin, 0L, 1L);
    }

    private void applyFreezeEffects(LivingEntity target) {
        DamageHandler.damageEntity(target, damage, this);
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, slowDuration, slowAmplifier, false, false));
        // Jump lock effect (JUMP_BOOST level 200 completely disables spacebar jump in Minecraft)
        target.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, noJumpDurationTicks, 200, false, false));

        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.2f, 0.8f);
        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_SNOW_BREAK, 1.5f, 0.6f);
        target.getWorld().spawnParticle(Particle.ITEM_SNOWBALL, target.getLocation().add(0, 0.5, 0), 25, 0.4, 0.4, 0.4, 0.1, new ItemStack(Material.SNOWBALL));
        target.getWorld().spawnParticle(Particle.BLOCK, target.getLocation().add(0, 0.2, 0), 30, 0.5, 0.2, 0.5, 0.1, Material.ICE.createBlockData());

        // Freeze feet with TempBlock ICE
        Block feetBlock = target.getLocation().getBlock();
        if (feetBlock.getType() == Material.AIR) {
            new TempBlock(feetBlock, Material.ICE.createBlockData(), noJumpDurationTicks * 50L);
        }

        // Continuous no-jump velocity clamp & ice particle aura
        new BukkitRunnable() {
            private int t = 0;

            @Override
            public void run() {
                t++;
                if (t > noJumpDurationTicks || target.isDead() || !target.isValid()) {
                    cancel();
                    return;
                }

                if (target.getLocation().add(0, -0.1, 0).getBlock().getType() == Material.AIR || target.getVelocity().getY() > 0) {
                    Vector v = target.getVelocity();
                    v.setY(-0.5);
                    target.setVelocity(v);
                }

                target.getWorld().spawnParticle(Particle.SNOWFLAKE, target.getLocation().add(0, 0.2, 0), 5, 0.3, 0.2, 0.3, 0.02);
                target.getWorld().spawnParticle(Particle.BLOCK, target.getLocation().add(0, 0.1, 0), 3, 0.2, 0.1, 0.2, 0.01, Material.ICE.createBlockData());
            }
        }.runTaskTimer(AmonPackPlugin.plugin, 0L, 1L);

        // Rotating Ice Ring of unpickable item drops
        spawnRotatingIceRing(target);
    }

    private void spawnRotatingIceRing(LivingEntity target) {
        List<Item> iceItems = new ArrayList<>();
        Location center = target.getLocation();

        for (int i = 0; i < 5; i++) {
            Item item = center.getWorld().dropItem(center.clone().add(0, 0.5, 0), new ItemStack(Material.ICE));
            item.setPickupDelay(Integer.MAX_VALUE);
            item.setGravity(false);
            iceItems.add(item);
        }

        new BukkitRunnable() {
            private int ticks = 0;

            @Override
            public void run() {
                ticks++;
                if (ticks > 60 || target.isDead() || !target.isValid()) {
                    for (Item item : iceItems) {
                        if (item.isValid()) item.remove();
                    }
                    cancel();
                    return;
                }

                Location currentCenter = target.getLocation().add(0, 0.5, 0);
                double angleStep = (2 * Math.PI) / iceItems.size();
                double radius = 1.0;

                for (int i = 0; i < iceItems.size(); i++) {
                    Item item = iceItems.get(i);
                    if (item.isValid()) {
                        double angle = (ticks * 0.15) + (i * angleStep);
                        double x = radius * Math.cos(angle);
                        double z = radius * Math.sin(angle);
                        Location itemLoc = currentCenter.clone().add(x, Math.sin(ticks * 0.2) * 0.2, z);
                        item.teleport(itemLoc);
                    }
                }
            }
        }.runTaskTimer(AmonPackPlugin.plugin, 0L, 1L);
    }

    @Override
    public void remove() {
        cleanupTempBlock();
        super.remove();
    }

    @Override
    public long getCooldown() {
        return cooldown;
    }

    @Override
    public Location getLocation() {
        return floatLoc;
    }

    @Override
    public String getName() {
        return "FrostGrip";
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
        cleanupTempBlock();
        remove();
    }

    @Override
    public String getDescription() {
        return "Absorbuje blok wody/lodu shiftem, przetrzymując go w powietrzu przed graczym. Puszczenie shifta rzuca nim w cel, nakładając zamrożenie, uniemożliwiając skakanie oraz tworząc obracający się pierścień z lodu.";
    }

    @Override
    public String getInstructions() {
        return "Przytrzymaj Shift na wodzie/lodzie aby przyciągnąć blok, a następnie puść Shift aby nim rzucić!";
    }
}

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
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;

public class Hoarfrost extends WaterAbility implements AddonAbility, SpecialTriggerable {

    private int durationTicks = 0;
    private final int maxDurationTicks = 100;

    private int stacks = 0;
    private final int maxStacks = 3;
    private long lastStackTime = 0L;

    private double damagePerSpear;
    private double knockback;
    private long cooldown;

    private boolean isFiring = false;

    public Hoarfrost(Player player) {
        super(player);

        Hoarfrost activeInstance = getAbility(player, Hoarfrost.class);
        if (activeInstance != null) {
            activeInstance.tryAddStack();
            return;
        }

        if (bPlayer.isOnCooldown(this) || !bPlayer.canBendIgnoreBinds(this)) {
            return;
        }

        Block targetWaterBlock = getWaterSourceBlock(player, 20);
        if (targetWaterBlock == null) {
            return;
        }

        this.damagePerSpear = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Water.Hoarfrost.DamagePerSpear", 4.5);
        this.knockback = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Water.Hoarfrost.Knockback", 0.65);
        this.cooldown = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Water.Hoarfrost.Cooldown", 6000);

        SpecialTriggerManager.registerActiveSpecial(player);
        this.stacks = 1;
        this.lastStackTime = System.currentTimeMillis();
        pullWaterFromSource(targetWaterBlock.getLocation());

        start();
    }

    public void tryAddStack() {
        if (isFiring || stacks >= maxStacks) return;

        // 500ms cooldown na zebranie kolejnego stacka
        if (System.currentTimeMillis() - lastStackTime < 500L) {
            return;
        }

        Block targetWaterBlock = getWaterSourceBlock(player, 20);
        if (targetWaterBlock != null) {
            stacks++;
            lastStackTime = System.currentTimeMillis();
            durationTicks = 0;
            pullWaterFromSource(targetWaterBlock.getLocation());
            player.getWorld().playSound(player.getLocation(), Sound.ITEM_BOTTLE_FILL, 0.8f, 1.4f);
        }
    }

    @Override
    public void progress() {
        if (player == null || player.isDead() || !player.isOnline()) {
            finish();
            return;
        }

        if (isFiring) return;

        durationTicks++;
        if (durationTicks >= maxDurationTicks) {
            finish();
            return;
        }

        SpecialTriggerManager.applySoftCooldownToToolbar(player);

        player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                new TextComponent(ChatColor.AQUA + "✦ HOARFROST STACKI: " + ChatColor.WHITE + stacks + " / 3 " + ChatColor.YELLOW + "[SHIFT - Wystrzał]"));

        // Orbitowanie włóczni lodu wokół gracza z duration 50ms
        displayOrbitingIceSpears();

        if (player.isSneaking()) {
            fireIceSpears();
        }
    }

    private void displayOrbitingIceSpears() {
        Location center = player.getLocation().add(0, 1.1, 0);
        for (int s = 0; s < stacks; s++) {
            double angle = (durationTicks * 0.25) + (s * (2 * Math.PI / stacks));
            double x = 1.5 * Math.cos(angle);
            double z = 1.5 * Math.sin(angle);
            Location orbLoc = center.clone().add(x, 0, z);

            Block b = orbLoc.getBlock();
            if (b.getType() == Material.AIR) {
                TempBlock tb = new TempBlock(b, Material.PACKED_ICE);
                tb.setRevertTime(50L);
            }
            ParticleEffect.SNOW_SHOVEL.display(orbLoc, 2, 0.05, 0.05, 0.05, 0.01);
        }
    }

    private void fireIceSpears() {
        isFiring = true;
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.2f, 0.7f);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0f, 1.3f);

        Location eye = player.getEyeLocation();
        Vector baseDir = eye.getDirection().clone().normalize();

        // Wystrzeliwanie długich na 3 bloki włóczni lodu (TempBlocki 100ms duration) z grawitacją
        for (int i = 0; i < stacks; i++) {
            double angleOffset = (i - (stacks - 1) / 2.0) * 0.22;
            Vector spreadDir = baseDir.clone().add(new Vector(angleOffset, 0.05, 0.0)).normalize().multiply(1.3);

            Location startSpawn = eye.clone().add(spreadDir.clone().normalize().multiply(1.2));

            new BukkitRunnable() {
                private Location currLoc = startSpawn.clone();
                private Vector vel = spreadDir.clone();
                private int ticks = 0;
                private Set<LivingEntity> hitEntities = new HashSet<>();

                @Override
                public void run() {
                    ticks++;
                    if (ticks > 40 || currLoc.getBlock().getType().isSolid()) {
                        explodeSpearHit(currLoc, vel.clone().normalize());
                        this.cancel();
                        return;
                    }

                    // Grawitacja działająca na włócznię lodu
                    vel.add(new Vector(0, -0.035, 0));
                    currLoc.add(vel);

                    Vector dir = vel.clone().normalize();
                    // Tworzenie długiej na 3 bloki włóczni z TempBlocków (100ms duration)
                    for (int seg = 0; seg < 3; seg++) {
                        Location segLoc = currLoc.clone().subtract(dir.clone().multiply(seg * 0.8));
                        Block b = segLoc.getBlock();
                        if (b.getType() == Material.AIR) {
                            TempBlock tb = new TempBlock(b, Material.PACKED_ICE);
                            tb.setRevertTime(100L);
                        }
                        ParticleEffect.SNOW_SHOVEL.display(segLoc, 2, 0.05, 0.05, 0.05, 0.01);
                    }

                    for (Entity entity : GeneralMethods.getEntitiesAroundPoint(currLoc, 1.6)) {
                        if (entity instanceof LivingEntity && !entity.getUniqueId().equals(player.getUniqueId())) {
                            LivingEntity target = (LivingEntity) entity;
                            if (!hitEntities.contains(target)) {
                                hitEntities.add(target);
                                DamageHandler.damageEntity(target, damagePerSpear, Hoarfrost.this);
                                target.setVelocity(dir.clone().multiply(knockback).setY(0.25));
                                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1, false, false));
                            }
                            explodeSpearHit(currLoc, dir);
                            this.cancel();
                            return;
                        }
                    }
                }
            }.runTaskTimer(AmonPackPlugin.plugin, i * 2L, 1L);
        }

        finish();
    }

    private void explodeSpearHit(Location hitLoc, Vector dir) {
        ParticleEffect.WATER_DROP.display(hitLoc, 12, 0.3, 0.3, 0.3, 0.1);
        ParticleEffect.CRIT_MAGIC.display(hitLoc, 12, 0.3, 0.3, 0.3, 0.1);
        ParticleEffect.BLOCK_CRACK.display(hitLoc, 20, 0.4, 0.4, 0.4, 0.1, Material.ICE.createBlockData());
        hitLoc.getWorld().playSound(hitLoc, Sound.BLOCK_GLASS_BREAK, 0.9f, 1.4f);
    }

    private void pullWaterFromSource(Location sourceLoc) {
        new BukkitRunnable() {
            private Location current = sourceLoc.clone();
            @Override
            public void run() {
                if (!player.isOnline() || current.distanceSquared(player.getLocation()) < 2.0) {
                    this.cancel();
                    return;
                }
                Vector dir = player.getLocation().add(0, 1, 0).toVector().subtract(current.toVector()).normalize();
                current.add(dir.multiply(0.8));
                ParticleEffect.WATER_WAKE.display(current, 3, 0.08, 0.08, 0.08, 0.02);
                ParticleEffect.WATER_SPLASH.display(current, 3, 0.08, 0.08, 0.08, 0.02);

                Block b = current.getBlock();
                if (b.getType() == Material.AIR) {
                    new TempBlock(b, Material.WATER).setRevertTime(120);
                }
            }
        }.runTaskTimer(AmonPackPlugin.plugin, 0, 1);
    }

    private Block getWaterSourceBlock(Player player, int range) {
        Block target = player.getTargetBlockExact(range);
        if (target != null && (isWater(target) || isIce(target))) {
            return target;
        }
        for (Block b : player.getLineOfSight(null, range)) {
            if (isWater(b) || isIce(b)) {
                return b;
            }
        }
        return null;
    }

    private void finish() {
        SpecialTriggerManager.unregisterActiveSpecial(player);
        bPlayer.addCooldown(this);
        remove();
    }

    @Override
    public TriggerType getSupportedTriggerType() {
        return TriggerType.SWAP;
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
        return "Hoarfrost";
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
        return "2.0";
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
        return "Przyciąga lód ze źródła wody. Wzmocnienie tworzy orbitujące włócznie lodu (50ms TempBlock), a wystrzał naciśnięciem SHIFT tworzy długie na 3 bloki (100ms TempBlock) zakrzywione grawitacyjnie włócznie lodu.";
    }

    @Override
    public String getInstructions() {
        return "Spójrz na wodę/lód i naciśnij F (SWAP), aby zebrać włócznię (500ms CD). Kucnij (SHIFT), aby wystrzelić 3-blokowe włócznie lodu!";
    }
}

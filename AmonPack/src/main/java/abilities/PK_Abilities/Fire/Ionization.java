package Abilities.PK_Abilities.Fire;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.LightningAbility;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.ParticleEffect;

import Abilities.Util_Objects.AbilityProjectile;
import Abilities.Util_Objects.BetterParticles;
import Plugin.AmonPackPlugin;
import Plugin.Methods;
import Abilities.Util_Objects.LightningBolt;

public class Ionization extends LightningAbility implements AddonAbility {

    private enum State {
        CHARGING, CHARGED, FIRING
    }

    private State state;
    private long startTime;
    private long chargeTime = 3500;
    private long chargedWindow = 2000;
    private long cooldown = 6000;
    private long failCooldown = 3000;
    private double selfDamage = 2.0;
    private int slot;

    public Ionization(Player player) {
        super(player);
        if (bPlayer.isOnCooldown(this)) {
            return;
        }
        if (!bPlayer.canBend(this)) {
            return;
        }

        this.state = State.CHARGING;
        this.startTime = System.currentTimeMillis();
        this.slot = player.getInventory().getHeldItemSlot();

        start();
    }

    @Override
    public void progress() {
        if (player.isDead() || !player.isOnline()) {
            remove();
            return;
        }

        if (player.getInventory().getHeldItemSlot() != slot) {
            fail("Slot changed!");
            return;
        }

        switch (state) {
            case CHARGING:
                if (System.currentTimeMillis() - startTime >= chargeTime) {
                    state = State.CHARGED;
                    startTime = System.currentTimeMillis();
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f);
                } else {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20, 2, false, false));
                    if (player.getLocation().getBlock().getType() == Material.WATER) {
                        Methods.LightningProjectile(player.getLocation().clone(), player);
                        Methods.LightningProjectile(player.getLocation().clone(), player);
                        fail("Got Wet!");
                    }
                    player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, player.getLocation().add(0, 1, 0), 2, 0.3,
                            0.5, 0.3, 0.05);
                }
                break;

            case CHARGED:
                if (System.currentTimeMillis() - startTime >= chargedWindow) {
                    fail("Too late!");
                } else {
                    Particle.DustOptions dustOptions = new Particle.DustOptions(Color.fromRGB(135, 206, 250), 1);
                    player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0, 1, 0), 5, 0.5,
                            0.5, 0.5, 0, dustOptions);
                    if (new Random().nextInt(10) == 0) {
                        player.playSound(player.getLocation(), Sound.ENTITY_TNT_PRIMED, 0.1f, 2f);
                    }
                }
                break;
            case FIRING:
                break;
        }
    }

    public void onClick() {
        if (state == State.CHARGING) {
            fail("Too early!");
        } else if (state == State.CHARGED) {
            fire();
        }
    }

    private void fire() {
        state = State.FIRING;
        player.playSound(player.getLocation(), Sound.ITEM_TRIDENT_THUNDER, 1f, 1f);

        Location origin = player.getEyeLocation().clone().add(0, 0.5, 0);
        Vector direction = player.getLocation().getDirection();

        List<LightningBolt> bolts = new ArrayList<>();
        bolts.add(new LightningBolt(player, this, origin, direction, 4, 60, 5, true));

        RPG.Levels.BendingTree.PlayerBendingBranch branch = AmonPackPlugin.levelsBending.GetBranchByPlayerName(player.getName());
        boolean hasStatic = (branch != null && branch.hasUpgrade("Static"));

        if (hasStatic) {
            Vector leftDir = rotateY(direction, 20);
            Vector rightDir = rotateY(direction, -20);
            bolts.add(new LightningBolt(player, this, origin, leftDir, 2.0, 42, 3, false));
            bolts.add(new LightningBolt(player, this, origin, rightDir, 2.0, 42, 3, false));
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                bolts.removeIf(LightningBolt::isDead);
                if (bolts.isEmpty()) {
                    this.cancel();
                    return;
                }
                List<LightningBolt> nextBranches = new ArrayList<>();
                for (LightningBolt bolt : bolts) {
                    List<LightningBolt> branches = bolt.progress();
                    if (branches != null) {
                        nextBranches.addAll(branches);
                    }
                }
                bolts.addAll(nextBranches);
            }
        }.runTaskTimer(AmonPackPlugin.plugin, 0, 1);

        bPlayer.addCooldown(this, cooldown);
        remove();
    }

    private Vector rotateY(Vector vector, double angleDegrees) {
        double angle = Math.toRadians(angleDegrees);
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double x = vector.getX() * cos - vector.getZ() * sin;
        double z = vector.getX() * sin + vector.getZ() * cos;
        return new Vector(x, vector.getY(), z).normalize();
    }

    private void fail(String reason) {
        player.getWorld().strikeLightningEffect(player.getLocation());
        DamageHandler.damageEntity(player, selfDamage, this);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 2, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20, 2, false, false));
        bPlayer.addCooldown(this, failCooldown);
        remove();
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
        return "Ionization";
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
        return "Ionizes the air around you, charging your body and discharging electric sparks at enemies. Be careful - if you take damage, touch water or miss relase-windows - you will be shocked!";
    }

    @Override
    public String getInstructions() {
        return "Left-click to start ionizating the air around you! After ability is charged, left-click to release it and destroy your enemies!";
    }

}
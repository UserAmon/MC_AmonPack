package abilities;

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

import abilities.Util_Objects.AbilityProjectile;
import abilities.Util_Objects.BetterParticles;
import methods_plugins.AmonPackPlugin;
import methods_plugins.Methods;

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
                    startTime = System.currentTimeMillis(); // Reset timer for charged window
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f);
                } else {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,20,2,false,false));
                    if(player.getLocation().getBlock().getType()==Material.WATER){
                        Methods.LightningProjectile(player.getLocation().clone(), player);
                        Methods.LightningProjectile(player.getLocation().clone(), player);
                        fail("Got Wet!");
                    }
                    if (new Random().nextInt(2) == 0) {
                        Particle.DustOptions dustOptions = new Particle.DustOptions(Color.fromRGB(135, 206, 250), 1);
                        player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0, 1, 0), 5, 0.5,
                                0.5, 0.5, 0, dustOptions);
                        if (new Random().nextInt(10) == 0) {
                            player.playSound(player.getLocation(), Sound.ENTITY_TNT_PRIMED, 0.1f, 2f);
                        }
                    }
                }
                break;

            case CHARGED:
                if (System.currentTimeMillis() - startTime >= chargedWindow) {
                    fail("Too late!");
                } else {
                    player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, player.getLocation().add(0, 1, 0), 2, 0.3,
                            0.5, 0.3, 0.05);
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

        Location origin = player.getEyeLocation().clone().add(0,0.5,0);
        Vector direction = player.getLocation().getDirection();

        List<BetterParticles> particles = new ArrayList<>();
        particles.add(new BetterParticles(3, ParticleEffect.REDSTONE, 0.3, 0.01, 0.15, Color.fromRGB(0, 153, 255)));
        particles.add(new BetterParticles(3, ParticleEffect.REDSTONE, 0.3, 0.01, 0.15, Color.fromRGB(0, 255, 235)));
        particles.add(new BetterParticles(2, ParticleEffect.CRIT_MAGIC, 0.3, 0.01, 0.15));

        AbilityProjectile mainBolt = new AbilityProjectile(direction, origin.clone(), origin.clone(), particles, 1.5);

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks > 40) { // Max duration
                    this.cancel();
                    return;
                }
                Location loc;
                    loc = mainBolt.LightningAdvance().clone();
                for (Entity entity : GeneralMethods.getEntitiesAroundPoint(loc, 1.5)) {
                    if (entity instanceof LivingEntity && !entity.getUniqueId().equals(player.getUniqueId())) {
                        DamageHandler.damageEntity(entity, 4, Ionization.this);
                        Methods.LightningProjectile(loc.clone(), player);
                    }
                }
                if (ticks % 3 == 0) {
                    Methods.LightningProjectile(loc.clone(), player);
                }
                if (loc.getBlock().getType().isSolid()) {
                    this.cancel();
                }
                if(loc.getBlock().getType()== Material.WATER || loc.getBlock().getType()== Material.ICE){
                    Methods.LightningProjectile(loc.clone(), player);
                    for (Entity entity : GeneralMethods.getEntitiesAroundPoint(loc, 3)) {
                        if (entity instanceof LivingEntity) {
                            Methods.LightningProjectile(entity.getLocation().clone(), player);
                        }
                    }
                    ticks+=4;
                }
                ticks++;
            }
        }.runTaskTimer(AmonPackPlugin.plugin, 0, 1);

        bPlayer.addCooldown(this, cooldown);
        remove();
    }

    private void fail(String reason) {
        // Visual Lightning Strike
        player.getWorld().strikeLightningEffect(player.getLocation());
        DamageHandler.damageEntity(player, selfDamage, this);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,40,2,false,false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS,20,2,false,false));
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
        return "Charge up a powerful lightning attack. Click to start charging, wait for the signal, then click again to fire. Don't mess up!";
    }

    @Override
    public String getInstructions() {
        return "Left-Click to charge. Left-Click again when charged.";
    }
}

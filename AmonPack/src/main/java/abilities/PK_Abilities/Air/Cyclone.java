package Abilities.PK_Abilities.Air;

import Plugin.AmonPackPlugin;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.AirAbility;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.ParticleEffect;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Random;

public class Cyclone extends AirAbility implements AddonAbility {

    private long cooldown;
    private double damage;
    private double range;
    private double speed;
    private long holdDuration;

    public Cyclone(Player player) {
        super(player);

        if (bPlayer.isOnCooldown(this) || !bPlayer.canBend(this)) {
            return;
        }

        this.cooldown = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Air.Cyclone.Cooldown", 7000L);
        this.damage = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Air.Cyclone.Damage", 4.0);
        this.range = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Air.Cyclone.Range", 20.0);
        this.speed = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Air.Cyclone.Speed", 1.0);
        this.holdDuration = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Air.Cyclone.HoldDuration", 3000L);

        bPlayer.addCooldown(this, cooldown);
        start();
        launchCyclone();
    }

    private void launchCyclone() {
        final Location origin = player.getEyeLocation();
        Vector initDir = player.getLocation().getDirection().setY(0).normalize();
        if (initDir.lengthSquared() < 0.01) initDir = new Vector(1, 0, 0);
        final Vector dir = initDir;

        player.getWorld().playSound(origin, Sound.ENTITY_HORSE_JUMP, 1.0f, 1.6f);
        player.getWorld().playSound(origin, Sound.ITEM_TRIDENT_RIPTIDE_1, 0.8f, 1.2f);

        new BukkitRunnable() {
            private Location currLoc = origin.clone();
            private double traveled = 0;
            private int ticks = 0;
            private LivingEntity trappedTarget = null;
            private int trappedTicks = 0;
            private Random rand = new Random();

            @Override
            public void run() {
                ticks++;

                if (trappedTarget != null) {
                    trappedTicks++;
                    if (!trappedTarget.isValid() || trappedTarget.isDead() || trappedTicks >= (holdDuration / 50)) {
                        // Wyrzucenie celu i usunięcie cyklonu
                        Vector throwVec = new Vector((rand.nextDouble() - 0.5) * 0.8, 0.4, (rand.nextDouble() - 0.5) * 0.8);
                        trappedTarget.setVelocity(throwVec);
                        DamageHandler.damageEntity(trappedTarget, damage, Cyclone.this);
                        currLoc.getWorld().playSound(currLoc, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 0.8f, 1.5f);
                        remove();
                        this.cancel();
                        return;
                    }

                    // Poruszanie i obracanie uwięzionego celu w powietrzu wewnątrz małego tornada
                    Location vortexCenter = currLoc.clone().add(0, 1.5, 0);
                    vortexCenter.add((rand.nextDouble() - 0.5) * 0.4, 0, (rand.nextDouble() - 0.5) * 0.4);

                    trappedTarget.teleport(vortexCenter);
                    trappedTarget.setRotation(trappedTarget.getLocation().getYaw() + 25.0f, (float) (Math.sin(ticks * 0.5) * 20.0));
                    trappedTarget.setVelocity(new Vector(0, 0.02, 0));

                    renderTornadoParticles(currLoc, ticks);
                    return;
                }

                traveled += speed;
                currLoc.add(dir.clone().multiply(speed));

                // Śledzenie podłoża
                org.bukkit.block.Block b = currLoc.getBlock();
                if (b.getType().isSolid()) {
                    currLoc.setY(currLoc.getY() + 1.0);
                } else if (!b.getRelative(0, -1, 0).getType().isSolid() && b.getRelative(0, -2, 0).getType().isSolid()) {
                    currLoc.setY(currLoc.getY() - 1.0);
                }

                renderTornadoParticles(currLoc, ticks);

                if (traveled >= range || currLoc.getBlock().getType().isSolid()) {
                    remove();
                    this.cancel();
                    return;
                }

                // Sprawdzanie trafienia przeciwnika
                for (Entity entity : GeneralMethods.getEntitiesAroundPoint(currLoc, 1.8)) {
                    if (entity instanceof LivingEntity && entity.getUniqueId() != player.getUniqueId()) {
                        trappedTarget = (LivingEntity) entity;
                        currLoc.getWorld().playSound(currLoc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.2f, 1.5f);
                        break;
                    }
                }
            }
        }.runTaskTimer(AmonPackPlugin.plugin, 0L, 1L);
    }

    private void renderTornadoParticles(Location baseLoc, int ticks) {
        // Małe tornado: 3 pierścienie rosnące w górę (wysokość 3 bloki, szerokość do 1.5 bloku)
        double[] ringHeights = {0.2, 1.2, 2.5};
        double[] ringRadii = {0.5, 1.0, 1.5};

        for (int r = 0; r < 3; r++) {
            double height = ringHeights[r];
            double radius = ringRadii[r];
            int points = 8 + (r * 4);

            for (int i = 0; i < points; i++) {
                double angle = (ticks * 0.35) + (2 * Math.PI * i / points) + (r * 0.5);
                double x = radius * Math.cos(angle);
                double z = radius * Math.sin(angle);
                Location pLoc = baseLoc.clone().add(x, height, z);

                ParticleEffect.CLOUD.display(pLoc, 1, 0.02, 0.02, 0.02, 0.01);
                pLoc.getWorld().spawnParticle(Particle.DUST, pLoc, 1, 0, 0, 0, 0, new Particle.DustOptions(Color.WHITE, 0.9f));

                if (r == 2 && Math.random() < 0.2) {
                    ParticleEffect.SWEEP_ATTACK.display(pLoc, 1, 0, 0, 0, 0);
                }
            }
        }

        if (ticks % 4 == 0) {
            baseLoc.getWorld().playSound(baseLoc, Sound.ENTITY_HORSE_JUMP, 0.5f, 1.8f);
        }
    }

    @Override
    public void progress() {
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
        return "Cyclone";
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
    public boolean isHarmlessAbility() {
        return false;
    }

    @Override
    public boolean isSneakAbility() {
        return false;
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
        return "Wystrzeliwuje małe tornado (3m wysokości, 1.5m szerokości) poruszające się naprzód. Przechwytuje wroga, odrywa go od ziemi, trzyma w wirowym powietrzu i obraca w powietrzu.";
    }

    @Override
    public String getInstructions() {
        return "Naciśnij slot z Cyclone, aby wystrzelić tornado!";
    }
}

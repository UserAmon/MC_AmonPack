package Abilities.PK_Abilities.Water;

import Plugin.AmonPackPlugin;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.PlantAbility;
import com.projectkorra.projectkorra.ability.WaterAbility;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.ParticleEffect;
import com.projectkorra.projectkorra.util.TempBlock;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class Blossom extends WaterAbility implements AddonAbility {

    private enum State {
        CHARGING, RELEASED
    }

    private State state;
    private long startTime;
    private long chargeTime;
    private double damage;
    private double range;
    private double speed;
    private long cooldown;

    private int ticksCharging = 0;
    private Random random = new Random();

    public Blossom(Player player) {
        super(player);

        if (bPlayer.isOnCooldown(this) || !bPlayer.canBend(this)) {
            return;
        }

        this.chargeTime = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Water.Plant.Blossom.ChargeTime", 2000L);
        this.damage = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Water.Plant.Blossom.Damage", 4.0);
        this.range = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Water.Plant.Blossom.Range", 18.0);
        this.speed = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Water.Plant.Blossom.Speed", 1.2);
        this.cooldown = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Water.Plant.Blossom.Cooldown", 6000L);

        this.state = State.CHARGING;
        this.startTime = System.currentTimeMillis();

        start();
    }

    @Override
    public void progress() {
        if (player == null || player.isDead() || !player.isOnline()) {
            remove();
            return;
        }

        if (state == State.CHARGING) {
            ticksCharging++;

            if (!player.isSneaking()) {
                if (System.currentTimeMillis() - startTime >= chargeTime) {
                    launchWave();
                } else {
                    remove();
                }
                return;
            }

            player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                    new TextComponent(System.currentTimeMillis() - startTime >= chargeTime
                            ? "§a§l[BLOSSOM NAŁADOWANE] Puść SHIFT, aby wystrzelić falę!"
                            : "§e[Blossom] Ładowanie kukurydzy i kwiatów..."));

            // Efekty V-shape w stożku przed graczem na ziemi
            if (ticksCharging % 3 == 0) {
                renderVConetherraform();
            }
        }
    }

    private void renderVConetherraform() {
        Location base = player.getLocation();
        Vector dir = base.getDirection().setY(0).normalize();
        if (dir.lengthSquared() < 0.01) dir = new Vector(1, 0, 0);

        Vector right = dir.clone().crossProduct(new Vector(0, 1, 0)).normalize();

        double maxDist = 6.0;
        for (double d = 1.0; d <= maxDist; d += 1.0) {
            double halfSpread = d * 0.55; // Stożek V-Shape
            for (double s = -halfSpread; s <= halfSpread; s += 0.8) {
                Location spot = base.clone().add(dir.clone().multiply(d)).add(right.clone().multiply(s));
                Block ground = getGroundBlock(spot);
                if (ground != null) {
                    Block above = ground.getRelative(0, 1, 0);

                    // Przekształcanie kamienia w ziemię, ziemi w trawę
                    if (ground.getType() == Material.STONE || ground.getType() == Material.COBBLESTONE || ground.getType() == Material.DEEPSLATE) {
                        new TempBlock(ground, Material.DIRT).setRevertTime(8000L);
                    } else if (ground.getType() == Material.DIRT || ground.getType() == Material.COARSE_DIRT || ground.getType() == Material.PODZOL) {
                        new TempBlock(ground, Material.GRASS_BLOCK).setRevertTime(8000L);
                    }

                    if (above.getType() == Material.AIR && !TempBlock.isTempBlock(above) && random.nextDouble() < 0.25) {
                        Material mat = random.nextBoolean() ? Material.SHORT_GRASS : (random.nextBoolean() ? Material.DANDELION : Material.POPPY);
                        new TempBlock(above, mat).setRevertTime(8000L);
                    }

                    ParticleEffect.COMPOSTER.display(above.getLocation().add(0.5, 0.2, 0.5), 1, 0.2, 0.2, 0.2, 0.01);
                }
            }
        }

        if (ticksCharging % 6 == 0) {
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_GRASS_STEP, 0.8f, 1.2f);
        }
    }

    private void launchWave() {
        state = State.RELEASED;
        bPlayer.addCooldown(this, cooldown);

        final Location origin = player.getLocation().clone();
        Vector initWaveDir = player.getLocation().getDirection().setY(0).normalize();
        if (initWaveDir.lengthSquared() < 0.01) initWaveDir = new Vector(1, 0, 0);
        final Vector waveDir = initWaveDir;
        final Vector rightVec = waveDir.clone().crossProduct(new Vector(0, 1, 0)).normalize();

        player.getWorld().playSound(origin, Sound.BLOCK_GRASS_BREAK, 1.2f, 0.8f);
        player.getWorld().playSound(origin, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.2f);

        Material[] waveFlowers = {
            Material.POPPY, Material.DANDELION, Material.BLUE_ORCHID,
            Material.ALLIUM, Material.AZURE_BLUET, Material.SHORT_GRASS, Material.FERN
        };

        new BukkitRunnable() {
            private Location currLoc = origin.clone();
            private double traveled = 0;
            private Set<LivingEntity> hitTargets = new HashSet<>();

            @Override
            public void run() {
                traveled += speed;
                currLoc.add(waveDir.clone().multiply(speed));

                Block ground = getGroundBlock(currLoc);
                if (ground == null || traveled >= range) {
                    remove();
                    this.cancel();
                    return;
                }

                Location centerWaveLoc = ground.getLocation().add(0.5, 1.0, 0.5);

                // Tworzenie fali kwiatów wysokości 1 bloku w szerokości 3 bloków
                for (double offset = -1.2; offset <= 1.2; offset += 1.2) {
                    Location waveSpot = centerWaveLoc.clone().add(rightVec.clone().multiply(offset));
                    Block wGround = getGroundBlock(waveSpot);
                    if (wGround != null) {
                        Block wAbove = wGround.getRelative(0, 1, 0);
                        if (wAbove.getType() == Material.AIR) {
                            Material flowerMat = waveFlowers[random.nextInt(waveFlowers.length)];
                            new TempBlock(wAbove, flowerMat).setRevertTime(1500L); // 1.5s fala kwiatów
                        }

                        // Strzelanie pnączami na boki
                        Location vineSpot = wAbove.getLocation().add(rightVec.clone().multiply(offset > 0 ? 1.5 : -1.5));
                        ParticleEffect.COMPOSTER.display(vineSpot, 3, 0.3, 0.3, 0.3, 0.05);
                        ParticleEffect.BLOCK_CRACK.display(waveSpot, 3, 0.2, 0.2, 0.2, 0.05, Material.OAK_LEAVES.createBlockData());
                    }
                }

                // Zostawianie dzikich kwiatów na ziemi po fali (wildflowers)
                Block trailAbove = ground.getRelative(0, 1, 0);
                if (trailAbove.getType() == Material.AIR && !TempBlock.isTempBlock(trailAbove)) {
                    Material trailMat = waveFlowers[random.nextInt(waveFlowers.length)];
                    new TempBlock(trailAbove, trailMat).setRevertTime(8000L);
                }

                // Podrzucanie wrogów w powietrze i zadawanie obrażeń
                for (Entity entity : GeneralMethods.getEntitiesAroundPoint(centerWaveLoc, 2.2)) {
                    if (entity instanceof LivingEntity && entity.getUniqueId() != player.getUniqueId()) {
                        LivingEntity target = (LivingEntity) entity;
                        if (!hitTargets.contains(target)) {
                            hitTargets.add(target);
                            DamageHandler.damageEntity(target, damage, Blossom.this);
                            Vector popUp = waveDir.clone().multiply(0.4).setY(0.65);
                            target.setVelocity(popUp);
                        }
                    }
                }

                if (traveled % 4 == 0) {
                    centerWaveLoc.getWorld().playSound(centerWaveLoc, Sound.BLOCK_GRASS_BREAK, 0.8f, 1.2f);
                }
            }
        }.runTaskTimer(AmonPackPlugin.plugin, 0L, 1L);
    }

    private Block getGroundBlock(Location loc) {
        Block b = loc.getBlock();
        for (int y = 2; y >= -3; y--) {
            Block check = b.getRelative(0, y, 0);
            if (check.getType().isSolid()) {
                return check;
            }
        }
        return null;
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
        return "Blossom";
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
        return true;
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
        return "Ładuje falę trawy i kwiatów w stożku V przed graczem. Po zwolnieniu SHIFT fala kwiatów wystrzeliwuje na wprost, strzela pnączami na boki i zostawia wildflowers.";
    }

    @Override
    public String getInstructions() {
        return "Przytrzymaj SHIFT, aby naładować falę kwiatów, i puść SHIFT, aby ją wystrzelić!";
    }
}

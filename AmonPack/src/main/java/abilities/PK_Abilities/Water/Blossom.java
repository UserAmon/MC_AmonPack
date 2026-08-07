package Abilities.PK_Abilities.Water;

import Plugin.AmonPackPlugin;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
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

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class Blossom extends WaterAbility implements AddonAbility {

    private double damage;
    private double range;
    private double speed;
    private long cooldown;
    private Random random = new Random();

    public Blossom(Player player) {
        super(player);

        if (bPlayer.isOnCooldown(this) || !bPlayer.canBend(this)) {
            return;
        }

        this.damage = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Water.Plant.Blossom.Damage", 4.0);
        this.range = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Water.Plant.Blossom.Range", 18.0);
        this.speed = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Water.Plant.Blossom.Speed", 1.2);
        this.cooldown = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Water.Plant.Blossom.Cooldown", 6000L);

        bPlayer.addCooldown(this, cooldown);
        start();
        launchWave();
    }

    private void launchWave() {
        final Location origin = player.getLocation().clone();
        Vector initWaveDir = player.getLocation().getDirection().setY(0).normalize();
        if (initWaveDir.lengthSquared() < 0.01) initWaveDir = new Vector(1, 0, 0);
        final Vector waveDir = initWaveDir;
        final Vector rightVec = waveDir.clone().crossProduct(new Vector(0, 1, 0)).normalize();

        player.getWorld().playSound(origin, Sound.BLOCK_GRASS_BREAK, 1.2f, 0.8f);
        player.getWorld().playSound(origin, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.2f);

        Material[] waveFlowers = {
            Material.POPPY, Material.DANDELION, Material.BLUE_ORCHID,
            Material.ALLIUM, Material.AZURE_BLUET, Material.SHORT_GRASS, Material.FERN, Material.PITCHER_PLANT
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

                // Fala kwiatów (1 blok wysokości) na szerokość 3 bloków
                for (double offset = -1.2; offset <= 1.2; offset += 1.2) {
                    Location waveSpot = centerWaveLoc.clone().add(rightVec.clone().multiply(offset));
                    Block wGround = getGroundBlock(waveSpot);
                    if (wGround != null) {
                        // Terraformowanie terenu: stone -> dirt, dirt -> grass_block
                        if (wGround.getType() == Material.STONE || wGround.getType() == Material.COBBLESTONE || wGround.getType() == Material.DEEPSLATE) {
                            new TempBlock(wGround, Material.DIRT).setRevertTime(8000L);
                        } else if (wGround.getType() == Material.DIRT || wGround.getType() == Material.COARSE_DIRT || wGround.getType() == Material.PODZOL) {
                            new TempBlock(wGround, Material.GRASS_BLOCK).setRevertTime(8000L);
                        }

                        Block wAbove = wGround.getRelative(0, 1, 0);
                        if (wAbove.getType() == Material.AIR && !TempBlock.isTempBlock(wAbove)) {
                            Material flowerMat = waveFlowers[random.nextInt(waveFlowers.length)];
                            new TempBlock(wAbove, flowerMat).setRevertTime(2000L);
                        }

                        // Efekty cząsteczek na boki
                        Location vineSpot = wAbove.getLocation().add(rightVec.clone().multiply(offset > 0 ? 1.5 : -1.5));
                        ParticleEffect.COMPOSTER.display(vineSpot, 3, 0.3, 0.3, 0.3, 0.05);
                        ParticleEffect.BLOCK_CRACK.display(waveSpot, 3, 0.2, 0.2, 0.2, 0.05, Material.OAK_LEAVES.createBlockData());
                    }
                }

                // Pozostawianie dzikich kwiatów i krzewów (WILDFLOWERS) na blokach trawy po przejściu fali
                if (ground.getType() == Material.GRASS_BLOCK || ground.getType() == Material.DIRT) {
                    Block trailAbove = ground.getRelative(0, 1, 0);
                    if (trailAbove.getType() == Material.AIR && !TempBlock.isTempBlock(trailAbove)) {
                        Material trailMat = waveFlowers[random.nextInt(waveFlowers.length)];
                        new TempBlock(trailAbove, trailMat).setRevertTime(8000L);
                    }
                }

                // Podrzucanie i obrażenia wrogów
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

                if ((int) traveled % 4 == 0) {
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
        return "Blossom";
    }

    @Override
    public String getAuthor() {
        return "AmonPack";
    }

    @Override
    public String getVersion() {
        return "2.0";
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
        return "Wystrzeliwuje natychmiastową falę kwiatów i trawy (LPM). Fala zamienia kamień w ziemię, a ziemię w trawę, strzela na boki pnączami i zostawia dzikie kwiaty (wildflowers) na ziemi.";
    }

    @Override
    public String getInstructions() {
        return "Naciśnij LPM, aby natychmiast wystrzelić falę kwiatów!";
    }
}

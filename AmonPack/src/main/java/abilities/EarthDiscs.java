package abilities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.projectkorra.projectkorra.util.TempBlock;
import methods_plugins.Methods;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.EarthAbility;

import abilities.Util_Objects.EarthDisc;

public class EarthDiscs extends EarthAbility implements AddonAbility {

    private enum State {
        BENDABLE, CHARGING, ARMED
    }

    private State state;
    private long startTime;
    private long chargeTime = 2000;
    private long armedDuration = 10000;
    private long cooldown = 6000;
    private int ammo = 2;
    private long interval;
    private double radius = 1.75;
    private List<Location> NearBlocks;

    public EarthDiscs(Player player) {
        super(player);
        if (bPlayer.isOnCooldown(this)) {
            return;
        }
        if (!bPlayer.canBend(this)) {
            return;
        }

        if (hasAbility(player, EarthDiscs.class)) {
            return;
        }
        interval = 0;
        state = State.BENDABLE;
        List<Location> shuffledList = new ArrayList<>();
        for (Block b : GeneralMethods.getBlocksAroundPoint(player.getLocation(), 10)) {
            if (b.getLocation().getY() <= player.getLocation().getY() + 1
                    && b.getLocation().distance(player.getLocation()) > 7 && EarthAbility.isEarthbendable(player, b)) {
                shuffledList.add(b.getLocation());
            }
        }
        if (shuffledList.size() > 4) {
            Collections.shuffle(shuffledList);
            NearBlocks = shuffledList.subList(0, 4);
            for (Location loc : NearBlocks) {
                TempBlock tb1 = new TempBlock(loc.getBlock(), Material.AIR);
                tb1.setRevertTime(7000);
                loc.setY(loc.getY() + 2);
            }
            start();
        }
        this.startTime = System.currentTimeMillis();
        start();
    }

    @Override
    public void progress() {
        if (player.isDead() || !player.isOnline()) {
            remove();
            return;
        }

        switch (state) {
            case BENDABLE:
                if (player.isSneaking()) {
                    interval++;
                    if (interval >= 2) {
                        interval = 0;
                        NearBlocks = Methods.BendableBlocksAnimation(NearBlocks, player.getLocation().clone(),
                                Material.STONE, 0.8);
                        if (NearBlocks.isEmpty() || NearBlocks.size() < 2) {
                            state = State.ARMED;
                        }
                    }
                } else {
                    bPlayer.addCooldown(this);
                    remove();
                    return;
                }
                break;

            case CHARGING:

                if (!player.isSneaking()) {
                    remove();
                    return;
                }
                if (System.currentTimeMillis() - startTime > chargeTime) {
                    state = State.ARMED;
                    startTime = System.currentTimeMillis();
                    player.playSound(player.getLocation(), Sound.BLOCK_STONE_BREAK, 1f, 0.5f);
                } else {
                    Location eye = player.getEyeLocation();
                    double time = System.currentTimeMillis() / 1000.0;

                    double angle1 = time * 4;
                    double x1 = radius * Math.cos(angle1);
                    double z1 = radius * Math.sin(angle1);

                    TempBlock tb2 = new TempBlock(eye.clone().add(x1, 0, z1).getBlock(), Material.DIRT);
                    tb2.setRevertTime(100);
                    double angle2 = time * 4 + Math.PI;
                    double x2 = radius * Math.cos(angle2);
                    double z2 = radius * Math.sin(angle2);
                    TempBlock tb1 = new TempBlock(eye.clone().add(x2, 0, z2).getBlock(), Material.DIRT);
                    tb1.setRevertTime(100);
                }
                break;

            case ARMED:
                if (System.currentTimeMillis() - startTime > armedDuration && ammo == 2) {
                    remove();
                    return;
                }

                if (ammo <= 0) {
                    remove();
                    return;
                }

                if (ammo > 0) {
                    Location eye = player.getEyeLocation();
                    double time = System.currentTimeMillis() / 1000.0;

                    if (ammo >= 1) {
                        double angle1 = time * 4;
                        double x1 = radius * Math.cos(angle1);
                        double z1 = radius * Math.sin(angle1);
                        EarthDisc.displayParticle(eye.clone().add(x1, -0.7, z1));
                    }
                    if (ammo >= 2) {
                        double angle2 = time * 4 + Math.PI;
                        double x2 = radius * Math.cos(angle2);
                        double z2 = radius * Math.sin(angle2);
                        EarthDisc.displayParticle(eye.clone().add(x2, -0.7, z2));
                    }
                }
                break;
        }
    }


    public void onClick() {
        if (state == State.ARMED && ammo > 0) {
            ammo--;
            Location spawn = player.getEyeLocation().clone().add(player.getEyeLocation().getDirection().multiply(1));

            new EarthDisc(player, spawn, player.getLocation().getDirection(), 4, 1, true);

            player.playSound(player.getLocation(), Sound.ENTITY_SNOWBALL_THROW, 1f, 0.5f);

            if (ammo == 0) {
                bPlayer.addCooldown(this);
            }
        }
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
        return "EarthDiscs";
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

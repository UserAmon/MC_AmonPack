package abilities;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.EarthAbility;
import com.projectkorra.projectkorra.util.TempBlock;

import abilities.Util_Objects.EarthDisc;
import methods_plugins.Methods;

public class DiscHurl extends EarthAbility implements AddonAbility {

    private enum State {
        SELECTING, TRAVELING, CHARGED
    }

    private State state;
    private List<Location> selectedBlocks;
    private long cooldown = 3000;
    private double damage = 3;
    private double speed = 0.8;
    private int interval = 0;

    public DiscHurl(Player player) {
        super(player);
        if (bPlayer.isOnCooldown(this)) {
            return;
        }
        Location TargetedBlock = Methods.getTargetLocation(player,15);
        if (isEarthbendable(player, TargetedBlock.getBlock())) {
            selectedBlocks = new ArrayList<>();
            selectedBlocks.add(TargetedBlock);
            new TempBlock(TargetedBlock.getBlock(), Material.AIR).setRevertTime(5000);
            state = State.SELECTING;
            start();
        }
    }

    @Override
    public void progress() {
        if (player.isDead() || !player.isOnline()) {
            remove();
            return;
        }

        switch (state) {
            case SELECTING:
                if (!player.isSneaking()) {
                    remove();
                    return;
                }
                state = State.TRAVELING;
                break;

            case TRAVELING:
                if (!player.isSneaking()) {
                    remove();
                    return;
                }

                interval++;
                if (interval >= 2) {
                    interval = 0;
                    selectedBlocks = Methods.BendableBlocksAnimation(selectedBlocks, player.getLocation().clone(),
                            Material.STONE, 0.8);

                    if (selectedBlocks.isEmpty() || selectedBlocks.get(0).distance(player.getLocation()) < 2) {
                        state = State.CHARGED;
                        player.playSound(player.getLocation(), Sound.BLOCK_GRAVEL_BREAK, 1f, 1f);
                    }
                }
                break;

            case CHARGED:
                if (!player.isSneaking()) {
                    shoot();
                    return;
                }
                EarthDisc.displayParticle(player.getEyeLocation().clone().add(0,-0.7,0).add(player.getEyeLocation().getDirection().multiply(1.5)));
                break;
        }
    }

    private void shoot() {
        Location spawn = player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(1.5));
        new EarthDisc(player, spawn, player.getLocation().getDirection(), damage, speed, false);
        player.playSound(player.getLocation(), Sound.ENTITY_GHAST_SHOOT, 0.5f, 1.5f);
        bPlayer.addCooldown(this);
        remove();
    }

    public static void onLeftClick(Player player) {
        EarthDisc.redirectNearby(player, 4);
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
        return "DiscHurl";
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

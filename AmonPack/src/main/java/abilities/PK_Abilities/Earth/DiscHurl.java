package Abilities.PK_Abilities.Earth;

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

import Abilities.Util_Objects.EarthDisc;
import Plugin.Methods;
import Plugin.AmonPackPlugin;

public class DiscHurl extends EarthAbility implements AddonAbility {

    private enum State {
        SELECTING, TRAVELING, CHARGED
    }

    private State state;
    private List<Location> selectedBlocks;
    private long cooldown;
    private double damage;
    private double speed;
    private double sourceRange;
    private long sourceRevertTime;
    private int interval = 0;
    private Material sourceMaterial;
    private boolean canRedirect;
    private int maxBounces;
    private double range;

    public DiscHurl(Player player) {
        super(player);
        this.cooldown = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Earth.DiscHurl.Cooldown", 3000);
        this.damage = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Earth.DiscHurl.Damage", 3.0);
        this.speed = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Earth.DiscHurl.Speed", 0.8);
        this.sourceRange = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Earth.DiscHurl.SourceRange", 15.0);
        this.sourceRevertTime = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Earth.DiscHurl.SourceRevertTime", 5000);
        this.canRedirect = AmonPackPlugin.getAbilitiesConfig().getBoolean("AmonPack.Earth.DiscHurl.CanRedirect", true);
        this.maxBounces = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Earth.DiscHurl.MaxBounces", 3);
        this.range = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Earth.DiscHurl.Range", 30.0);

        if (bPlayer.isOnCooldown(this)) {
            return;
        }
        Location TargetedBlock = Methods.getTargetLocation(player, sourceRange);
        if (isEarthbendable(player, TargetedBlock.getBlock())) {
            this.sourceMaterial = TargetedBlock.getBlock().getType();
            selectedBlocks = new ArrayList<>();
            selectedBlocks.add(TargetedBlock);
            new TempBlock(TargetedBlock.getBlock(), Material.AIR).setRevertTime(sourceRevertTime);
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
                            sourceMaterial, 0.8);

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
                EarthDisc.displayParticle(player.getEyeLocation().clone().add(0, -0.7, 0)
                        .add(player.getEyeLocation().getDirection().multiply(1.5)), sourceMaterial);
                break;
        }
    }

    private void shoot() {
        Location spawn = player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(1.5));
        EarthDisc disc = new EarthDisc(player, spawn, player.getLocation().getDirection(), damage, speed, false, sourceMaterial, canRedirect, maxBounces, range);
        disc.setAbility(this);
        player.playSound(player.getLocation(), Sound.ENTITY_GHAST_SHOOT, 0.5f, 1.5f);
        bPlayer.addCooldown(this);
        remove();
    }

    public static void onLeftClick(Player player) {
        double redirectRange = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Earth.DiscHurl.RedirectRange", 4.0);
        EarthDisc.redirectNearby(player, redirectRange);
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

    @Override
    public String getDescription() {
        return "Launches a solid earth disc at high speed towards your target. Maybe there is a way this ability works with arleady existing Earth Discs...";
    }

    @Override
    public String getInstructions() {
        return "Hold shift on bendable earth block to charge an earth disc. Release to hurl it.";
    }

}
package Abilities.PK_Abilities.Earth;

import Plugin.AmonPackPlugin;
import Plugin.Methods;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.EarthAbility;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.TempBlock;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class Baricade extends EarthAbility implements AddonAbility {

    private enum State {
        CHARGING, READY, WALL_TRAVELING, STANDING
    }

    private State state;
    private long startTime;
    private int slot;

    // Config variables
    private long cooldown;
    private double circleDistance;
    private double circleRadius;
    private int wallWidth;
    private int wallHeight;
    private double wallSpeed;
    private double wallDistance;
    private Material wallMaterial;
    private int crumbleFallingBlocks;
    private long standingDuration;
    private double wallDamage;
    private double wallPush;

    // Tracing mechanics
    private Location center;
    private Vector dir0;
    private Vector right;
    private Vector up;
    private double targetAngle = 0.0;
    private int soundTicks = 0;

    // Traveling wall mechanics
    private Location startLoc;
    private Vector wallDir;
    private double distanceTraveled = 0.0;
    private double prevCenterY;
    private List<TempBlock> activeBlocks = new ArrayList<>();
    private long standingStartTime = 0;

    public Baricade(Player player) {
        super(player);
        loadConfig();

        if (bPlayer.isOnCooldown(this)) {
            return;
        }
        if (!bPlayer.canBend(this)) {
            return;
        }

        // Requirements: stand on the ground, near earthbendable blocks
        if (!player.isOnGround() && player.getLocation().getBlock().getRelative(BlockFace.DOWN).getType().isAir()) {
            return;
        }
        if (!isNearEarthBlock()) {
            return;
        }

        this.slot = player.getInventory().getHeldItemSlot();
        this.state = State.CHARGING;
        this.startTime = System.currentTimeMillis();

        // Lock initial forward vector and compute right/up orthogonal vectors
        Location eye = player.getEyeLocation();
        this.dir0 = eye.getDirection().normalize();

        this.right = this.dir0.clone().crossProduct(new Vector(0, 1, 0)).normalize();
        if (this.right.lengthSquared() < 0.01) {
            this.right = new Vector(1, 0, 0);
        }
        this.up = this.right.clone().crossProduct(this.dir0).normalize();

        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_GRINDSTONE_USE, 1f, 0.9f);
        start();
    }

    private Location getCenterLoc() {
        Location eye = player.getEyeLocation();
        return eye.clone().add(this.dir0.clone().multiply(circleDistance));
    }

    private void loadConfig() {
        this.cooldown = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Earth.Baricade.Cooldown", 8000);
        this.circleDistance = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Earth.Baricade.CircleDistance", 2.0);
        this.circleRadius = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Earth.Baricade.CircleRadius", 0.45);
        this.wallWidth = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Earth.Baricade.WallWidth", 3);
        this.wallHeight = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Earth.Baricade.WallHeight", 3);
        this.wallSpeed = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Earth.Baricade.WallSpeed", 0.4);
        this.wallDistance = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Earth.Baricade.WallDistance", 12.0);
        
        String matStr = AmonPackPlugin.getAbilitiesConfig().getString("AmonPack.Earth.Baricade.WallMaterial", "STONE");
        try {
            this.wallMaterial = Material.valueOf(matStr.toUpperCase());
        } catch (Exception e) {
            this.wallMaterial = Material.STONE;
        }
        
        this.crumbleFallingBlocks = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Earth.Baricade.CrumbleFallingBlocks", 8);
        this.standingDuration = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Earth.Baricade.StandingDuration", 5000);
        this.wallDamage = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Earth.Baricade.WallDamage", 1.0);
        this.wallPush = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Earth.Baricade.WallPush", 0.45);
    }

    private boolean isNearEarthBlock() {
        Location loc = player.getLocation();
        for (int x = -3; x <= 3; x++) {
            for (int y = -2; y <= 1; y++) {
                for (int z = -3; z <= 3; z++) {
                    Block b = loc.clone().add(x, y, z).getBlock();
                    if (EarthAbility.isEarthbendable(player, b)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public void progress() {
        if (player.isDead() || !player.isOnline()) {
            remove();
            return;
        }

        if ((state == State.CHARGING || state == State.READY) && player.getInventory().getHeldItemSlot() != slot) {
            remove();
            return;
        }

        // Manual crumble: if active/standing and sneak-clicking while holding Baricade slot
        if ((state == State.WALL_TRAVELING || state == State.STANDING) 
                && player.isSneaking() 
                && "Baricade".equalsIgnoreCase(bPlayer.getBoundAbilityName())) {
            crumble();
            return;
        }

        switch (state) {
            case CHARGING:
                if (!player.isSneaking()) {
                    remove();
                    return;
                }
                drawTracingCircle();
                checkAlignment();

                if (targetAngle >= 360.0) {
                    state = State.READY;
                    player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1.2f);
                }
                break;

            case READY:
                // Tracing complete. Action bar instructions
                player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, 
                        net.md_5.bungee.api.chat.TextComponent.fromLegacyText(ChatColor.GREEN + "§lBARICADE READY - RELEASE SHIFT / BARYKADA GOTOWA - PUŚĆ SHIFT"));
                
                // Pulsing success particles following look direction
                Location targetLoc = player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(circleDistance));
                player.spawnParticle(org.bukkit.Particle.BLOCK, targetLoc, 1, 0.1, 0.1, 0.1, 0.0, org.bukkit.Material.STONE.createBlockData());

                if (!player.isSneaking()) {
                    triggerWall();
                }
                break;

            case WALL_TRAVELING:
                progressWall();
                break;

            case STANDING:
                if (System.currentTimeMillis() - standingStartTime > standingDuration) {
                    crumble();
                }
                break;
        }
    }

    private void drawTracingCircle() {
        Location curCenter = getCenterLoc();
        
        org.bukkit.Particle.DustOptions dustNeutral = new org.bukkit.Particle.DustOptions(org.bukkit.Color.fromRGB(150, 110, 80), 0.5f);
        org.bukkit.Particle.DustOptions dustComplete = new org.bukkit.Particle.DustOptions(org.bukkit.Color.fromRGB(60, 180, 60), 0.5f);
        org.bukkit.Particle.DustOptions dustActive = new org.bukkit.Particle.DustOptions(org.bukkit.Color.fromRGB(255, 50, 50), 0.8f);

        // Render neutral path and completed path
        for (double a = 0; a < 360.0; a += 20.0) {
            double rad = Math.toRadians(a);
            Vector offset = up.clone().multiply(circleRadius * Math.cos(rad)).add(right.clone().multiply(-circleRadius * Math.sin(rad)));
            Location pLoc = curCenter.clone().add(offset);
            
            if (a < targetAngle) {
                player.spawnParticle(org.bukkit.Particle.DUST, pLoc, 1, 0, 0, 0, 0, dustComplete);
            } else {
                player.spawnParticle(org.bukkit.Particle.DUST, pLoc, 1, 0, 0, 0, 0, dustNeutral);
            }
        }

        // Draw active tracking target
        double targetRad = Math.toRadians(targetAngle);
        Vector activeOffset = up.clone().multiply(circleRadius * Math.cos(targetRad)).add(right.clone().multiply(-circleRadius * Math.sin(targetRad)));
        Location activeLoc = curCenter.clone().add(activeOffset);
        player.spawnParticle(org.bukkit.Particle.DUST, activeLoc, 1, 0, 0, 0, 0, dustActive);
    }

    private void checkAlignment() {
        double targetRad = Math.toRadians(targetAngle);
        Vector activeOffset = up.clone().multiply(circleRadius * Math.cos(targetRad)).add(right.clone().multiply(-circleRadius * Math.sin(targetRad)));
        Location activeLoc = getCenterLoc().clone().add(activeOffset);

        Vector toParticle = activeLoc.toVector().subtract(player.getEyeLocation().toVector()).normalize();
        double dot = player.getEyeLocation().getDirection().normalize().dot(toParticle);

        if (dot > 0.94) {
            targetAngle += 12.0; // complete in 30 ticks (1.5s) if tracked perfectly
            soundTicks++;
            if (soundTicks % 3 == 0) {
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_GRAVEL_BREAK, 0.4f, 1.2f);
            }
        }
    }

    private void triggerWall() {
        this.state = State.WALL_TRAVELING;
        bPlayer.addCooldown(this);

        this.startLoc = player.getLocation().clone().add(player.getLocation().getDirection().setY(0).normalize().multiply(1.5));
        this.wallDir = player.getLocation().getDirection().setY(0).normalize();
        if (this.wallDir.lengthSquared() < 0.01) {
            this.wallDir = new Vector(1, 0, 0);
        }

        this.prevCenterY = getGroundY(this.startLoc);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_STONE_BREAK, 1.2f, 0.7f);
    }

    private void progressWall() {
        distanceTraveled += wallSpeed;
        if (distanceTraveled > wallDistance) {
            this.state = State.STANDING;
            this.standingStartTime = System.currentTimeMillis();
            return;
        }

        Location currentLoc = startLoc.clone().add(wallDir.clone().multiply(distanceTraveled));
        double colY = getGroundY(currentLoc);

        // Check for major sudden elevation changes (cliff collision)
        if (Math.abs(colY - prevCenterY) > 2.2) {
            crumble();
            return;
        }
        prevCenterY = colY;

        // Revert previous blocks
        revertWallBlocks();

        // Calculate and create new wall blocks
        Vector rightVec = wallDir.clone().crossProduct(new Vector(0, 1, 0)).normalize();
        
        for (int w = -wallWidth / 2; w <= wallWidth / 2; w++) {
            Location colLoc = currentLoc.clone().add(rightVec.clone().multiply(w));
            double colGroundY = getGroundY(colLoc);
            
            for (int h = 0; h < wallHeight; h++) {
                Location blockLoc = colLoc.clone();
                blockLoc.setY(colGroundY + h);
                Block b = blockLoc.getBlock();
                
                // Obstacle collision check: crumble if hit solid structure
                if (b.getType().isSolid() && !b.isPassable() && !TempBlock.isTempBlock(b)) {
                    crumble();
                    return;
                }

                if (isTransparent(b) || b.isPassable()) {
                    TempBlock tb = new TempBlock(b, wallMaterial);
                    tb.setRevertTime(standingDuration + 10000); // Safety timeout fallback
                    activeBlocks.add(tb);
                }
            }
        }

        // Push entities and deal damage
        for (TempBlock tb : activeBlocks) {
            Location loc = tb.getLocation().add(0.5, 0.5, 0.5);
            for (Entity entity : GeneralMethods.getEntitiesAroundPoint(loc, 1.2)) {
                if (entity instanceof LivingEntity && !entity.getUniqueId().equals(player.getUniqueId())) {
                    LivingEntity le = (LivingEntity) entity;
                    Vector push = wallDir.clone().multiply(wallPush).setY(0.25);
                    le.setVelocity(push);
                    DamageHandler.damageEntity(le, wallDamage, this);
                }
            }
        }
    }

    private double getGroundY(Location loc) {
        Location check = loc.clone();
        for (int dy = 2; dy >= -4; dy--) {
            Block b = check.clone().add(0, dy, 0).getBlock();
            if (b.getType().isSolid() && !TempBlock.isTempBlock(b)) {
                return b.getY() + 1.0;
            }
        }
        return loc.getY();
    }

    private void revertWallBlocks() {
        if (activeBlocks == null) {
            return;
        }
        for (TempBlock tb : activeBlocks) {
            tb.revertBlock();
        }
        activeBlocks.clear();
    }

    private void crumble() {
        Location crumbleLoc = startLoc.clone().add(wallDir.clone().multiply(distanceTraveled));
        revertWallBlocks();
        Methods.spawnFallingBlocks(crumbleLoc.add(0, 1, 0), wallMaterial, crumbleFallingBlocks, 1.5, player);
        crumbleLoc.getWorld().playSound(crumbleLoc, Sound.BLOCK_STONE_BREAK, 1.5f, 0.8f);
        remove();
    }

    @Override
    public void stop() {
        revertWallBlocks();
        super.remove();
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
        return "Baricade";
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
    public void load() {}

    @Override
    public String getDescription() {
        return "Raises a 3x3 earth wall that travels forward, adjusting to terrain and crumbling at obstacles.";
    }

    @Override
    public String getInstructions() {
        return "Hold Sneak on the ground near earth blocks. Trace the circle with your crosshair. Release Shift to fire.";
    }
}

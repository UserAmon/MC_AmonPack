package abilities;

import dev.lone.itemsadder.api.ItemsAdder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.FireAbility;
import com.projectkorra.projectkorra.util.DamageHandler;

public class FlameShot extends FireAbility implements AddonAbility {

    private Location location;
    private long lastFireTime;
    private int chargesLeft;

    // Configurable variables
    private double damage = 3.0;
    private double speed = 1.2;
    private double range = 30;
    private long cooldown = 7000;
    private long internalCooldown = 1000;
    private int maxCharges = 2;
    private double gravity = 0.05;

    private class Projectile {
        Location loc;
        Vector dir;
        Location startLoc;

        public Projectile(Location loc, Vector dir) {
            this.loc = loc;
            this.dir = dir;
            this.startLoc = loc.clone();
        }
    }

    private java.util.List<Projectile> projectiles = new java.util.ArrayList<>();

    public FlameShot(Player player) {
        super(player);
        if (bPlayer.isOnCooldown(this)) {
            return;
        }
        if (!bPlayer.canBend(this)) {
            return;
        }

        if (hasAbility(player, FlameShot.class)) {
            FlameShot existing = getAbility(player, FlameShot.class);
            if (System.currentTimeMillis() - existing.lastFireTime < internalCooldown) {
                return;
            }
            existing.fire();
            return;
        }

        this.chargesLeft = maxCharges;
        start();
        fire();
    }

    private void fire() {
        this.lastFireTime = System.currentTimeMillis();
        this.chargesLeft--;

        projectiles.add(new Projectile(player.getEyeLocation(), player.getLocation().getDirection().normalize()));

        Bukkit.dispatchCommand(
                Bukkit.getConsoleSender(),
                "iaemote roll " + player.getName()
        );
        if (chargesLeft <= 0) {
            bPlayer.addCooldown(this);
        }
    }

    @Override
    public void progress() {
        if (player.isDead() || !player.isOnline()) {
            remove();
            return;
        }

        if (projectiles.isEmpty() && chargesLeft <= 0) {
            remove();
            return;
        }

        if (projectiles.isEmpty() && System.currentTimeMillis() - lastFireTime > 5000) {
            bPlayer.addCooldown(this);
            remove();
            return;
        }

        java.util.Iterator<Projectile> it = projectiles.iterator();
        while (it.hasNext()) {
            Projectile p = it.next();

            if (p.loc.distanceSquared(p.startLoc) > range * range || !isTransparent(p.loc.getBlock())) {
                it.remove();
                continue;
            }

            p.dir.setY(p.dir.getY() - gravity);
            p.loc.add(p.dir.clone().multiply(speed));

            player.getWorld().spawnParticle(Particle.FLAME, p.loc, 5, 0.1, 0.1, 0.1, 0.02);
            player.getWorld().playSound(p.loc, Sound.ENTITY_BLAZE_SHOOT, 0.5f, 1f);

            boolean hit = false;
            for (Entity entity : GeneralMethods.getEntitiesAroundPoint(p.loc, 1.5)) {
                if (entity instanceof LivingEntity && entity.getUniqueId() != player.getUniqueId()) {
                    DamageHandler.damageEntity(entity, damage, this);
                    entity.setFireTicks(60);
                    hit = true;
                    break;
                }
            }

            if (hit) {
                it.remove();
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
        return "FlameShot";
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
        remove();
    }
}

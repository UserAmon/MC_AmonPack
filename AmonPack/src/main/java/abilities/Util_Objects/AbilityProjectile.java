package abilities.Util_Objects;

import com.projectkorra.projectkorra.util.ParticleEffect;
import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Random;


public class AbilityProjectile {
    private Location location;
    private Vector direction;
    private Location origin;
    private List<BetterParticles> Particles;
    private double speed;
    private boolean Echo;

    public AbilityProjectile(Vector direction, Location location, Location origin, List<BetterParticles> particles, double speed) {
        this.direction = direction;
        this.location = location;
        this.origin = origin;
        Particles = particles;
        this.speed = speed;
    }

    public Location Advance(){
        for (BetterParticles particle : Particles){
            particle.Display(location);
        }
        location.add(direction).multiply(speed);
        return location;
    }
    public Location LightningAdvance() {
        Random random = new Random();

        double offsetX = ((random.nextDouble() * 2) - 1);
        double offsetZ = ((random.nextDouble() * 2) - 1);

        Vector randomOffset = new Vector(offsetX, -0.01, offsetZ);

        Vector forward = direction.clone().normalize();

        Vector move = forward.add(randomOffset.multiply(1.4));

        for (BetterParticles particle : Particles) {
            particle.DisplayDustOption(location);
        }

        location.add(move);
        return location;
    }

    public Location Revert(){
        for (BetterParticles particle : Particles){
            particle.Display(location);
        }
        location.subtract(direction).multiply(speed);
        return location;
    }
    public Location Advance(Vector dir){
        for (BetterParticles particle : Particles){
            particle.Display(location);
        }
        location.add(dir).multiply(speed);
        return location;
    }
    public Location Advance(double x, double y, double z){
        for (BetterParticles particle : Particles){
            particle.Display(location);
        }
        location.add(x,y,z).multiply(speed);
        return location;
    }

    public Location getOrigin() {
        return origin;
    }

    public Location getLocation() {
        return location;
    }

    public void UpdateLocation(Location location) {
        this.location = location;
    }

    public Vector getDirection() {
        return direction;
    }

    public boolean isEcho() {
        return Echo;
    }

    public void setEcho(boolean echo) {
        Echo = echo;
    }
}

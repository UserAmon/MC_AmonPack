package Abilities.Util_Objects;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Particle.DustOptions;

public class BetterParticles {
    private Particle effect;
    private int amount;
    private double range;
    private double Yrange;
    private double speed;
    private Color color;

    public BetterParticles(int amount, Particle effect, double range, double speed, double yrange) {
        this(amount, effect, range, speed, yrange, null);
    }

    public BetterParticles(int amount, Particle effect, double range, double speed, double yrange, Color color) {
        this.amount = amount;
        this.effect = effect;
        this.range = range;
        this.speed = speed;
        this.Yrange = yrange;
        this.color = color;
    }

    public void Display(Location location) {
        if (location == null || location.getWorld() == null) return;
        if (effect == Particle.DUST && color != null) {
            DustOptions dust = new DustOptions(color, 1.0f);
            location.getWorld().spawnParticle(Particle.DUST, location, amount, range, Yrange, range, speed, dust);
        } else if (effect == Particle.NOTE) {
            location.getWorld().spawnParticle(Particle.NOTE, location, amount, range, Yrange, range, speed);
        } else {
            location.getWorld().spawnParticle(effect, location, amount, range, Yrange, range, speed);
        }
    }

    public void DisplayDustOption(Location location) {
        if (location == null || location.getWorld() == null) return;
        if (color != null) {
            DustOptions dust = new DustOptions(color, 1.0f);
            location.getWorld().spawnParticle(Particle.DUST, location, amount, range, Yrange, range, speed, dust);
        } else {
            location.getWorld().spawnParticle(effect, location, amount, range, Yrange, range, speed);
        }
    }
}

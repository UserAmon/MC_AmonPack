package RPG.BattleRoyale.Blood;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.UUID;

/**
 * Reprezentuje pojedynczy punkt śladu krwi na podłożu.
 * Ślady posiadają ograniczony czas życia oraz intensywność / świeżość,
 * którą zombie potrafią analizować i podążać tropem ofiary.
 */
public class BloodTrail {

    private final UUID id;
    private final World world;
    private final Location location;
    private final double initialIntensity;
    private final long creationTime;
    private final long lifetimeMs;
    private final UUID sourcePlayerId;

    public BloodTrail(Location location, double initialIntensity, long lifetimeMs, UUID sourcePlayerId) {
        this.id = UUID.randomUUID();
        this.world = location.getWorld();
        this.location = location.clone();
        this.initialIntensity = Math.max(0.1, Math.min(1.0, initialIntensity));
        this.creationTime = System.currentTimeMillis();
        this.lifetimeMs = lifetimeMs;
        this.sourcePlayerId = sourcePlayerId;
    }

    public double getFreshness(long now) {
        long age = now - creationTime;
        if (age >= lifetimeMs) return 0.0;
        return (1.0 - ((double) age / lifetimeMs)) * initialIntensity;
    }

    public boolean isExpired(long now) {
        return (now - creationTime) >= lifetimeMs;
    }

    public UUID getId() { return id; }
    public World getWorld() { return world; }
    public Location getLocation() { return location.clone(); }
    public long getCreationTime() { return creationTime; }
    public long getLifetimeMs() { return lifetimeMs; }
    public UUID getSourcePlayerId() { return sourcePlayerId; }
}

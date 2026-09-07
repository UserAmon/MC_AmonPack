package RPG.BattleRoyale.Noise;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;

import java.util.UUID;

/**
 * Reprezentuje pojedyncze zdarzenie hałasu na mapie Battle Royale.
 * Zombie słyszące ten dźwięk nie otrzymują natychmiast pozycji gracza (brak wallhacka),
 * a jedynie zapamiętują lokalizację ostatniego źródła hałasu i kierują się tam, by zbadać sprawę.
 */
public class NoiseEvent {

    private final UUID id;
    private final World world;
    private final Location location;
    private final double intensity; // 0.0 - 100.0
    private final double radius;    // promień słyszalności w blokach
    private final long timestamp;
    private final String sourceType;
    private final UUID sourceEntityId;
    private final long expirationTime;

    public NoiseEvent(Location location, double intensity, double radius, String sourceType, Entity sourceEntity, long lifetimeMs) {
        this.id = UUID.randomUUID();
        this.world = location.getWorld();
        this.location = location.clone();
        this.intensity = Math.max(0.0, Math.min(100.0, intensity));
        this.radius = radius;
        this.timestamp = System.currentTimeMillis();
        this.sourceType = sourceType != null ? sourceType : "UNKNOWN";
        this.sourceEntityId = sourceEntity != null ? sourceEntity.getUniqueId() : null;
        this.expirationTime = this.timestamp + lifetimeMs;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expirationTime;
    }

    public UUID getId() { return id; }
    public World getWorld() { return world; }
    public Location getLocation() { return location.clone(); }
    public double getIntensity() { return intensity; }
    public double getRadius() { return radius; }
    public long getTimestamp() { return timestamp; }
    public String getSourceType() { return sourceType; }
    public UUID getSourceEntityId() { return sourceEntityId; }
    public long getExpirationTime() { return expirationTime; }
}

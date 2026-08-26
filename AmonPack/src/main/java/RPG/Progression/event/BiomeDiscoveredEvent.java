package RPG.Progression.event;

import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class BiomeDiscoveredEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final String biomeName;
    private final Biome biome;

    public BiomeDiscoveredEvent(Player player, String biomeName, Biome biome) {
        this.player = player;
        this.biomeName = biomeName;
        this.biome = biome;
    }

    public Player getPlayer() {
        return player;
    }

    public String getBiomeName() {
        return biomeName;
    }

    public Biome getBiome() {
        return biome;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}

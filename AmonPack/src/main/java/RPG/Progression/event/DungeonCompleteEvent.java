package RPG.Progression.event;

import RPG.Dungeons.Dungeon;
import RPG.Dungeons.DungeonInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.Collections;
import java.util.List;

public class DungeonCompleteEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    private final DungeonInstance dungeonInstance;
    private final Dungeon dungeonTemplate;
    private final List<Player> players;

    public DungeonCompleteEvent(DungeonInstance dungeonInstance, Dungeon dungeonTemplate, List<Player> players) {
        this.dungeonInstance = dungeonInstance;
        this.dungeonTemplate = dungeonTemplate;
        this.players = players;
    }

    public DungeonInstance getDungeonInstance() {
        return dungeonInstance;
    }

    public Dungeon getDungeonTemplate() {
        return dungeonTemplate;
    }

    public List<Player> getPlayers() {
        return Collections.unmodifiableList(players);
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}

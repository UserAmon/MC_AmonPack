package RPG.Progression.event;

import CustomContent.Bosses.CustomBoss;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class CustomBossDefeatEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    private final CustomBoss bossTemplate;
    private final Mob entity;
    private final Player killer;
    private final Set<Player> participants;

    public CustomBossDefeatEvent(CustomBoss bossTemplate, Mob entity, Player killer, Set<Player> participants) {
        this.bossTemplate = bossTemplate;
        this.entity = entity;
        this.killer = killer;
        this.participants = participants != null ? participants : new HashSet<>();
    }

    public CustomBoss getBossTemplate() {
        return bossTemplate;
    }

    public Mob getEntity() {
        return entity;
    }

    public Player getKiller() {
        return killer;
    }

    public Set<Player> getParticipants() {
        return Collections.unmodifiableSet(participants);
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}

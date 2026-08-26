package RPG.Progression.event;

import RPG.Progression.model.StageType;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class StageUnlockEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final StageType previousStage;
    private final StageType newStage;

    public StageUnlockEvent(Player player, StageType previousStage, StageType newStage) {
        this.player = player;
        this.previousStage = previousStage;
        this.newStage = newStage;
    }

    public Player getPlayer() {
        return player;
    }

    public StageType getPreviousStage() {
        return previousStage;
    }

    public StageType getNewStage() {
        return newStage;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}

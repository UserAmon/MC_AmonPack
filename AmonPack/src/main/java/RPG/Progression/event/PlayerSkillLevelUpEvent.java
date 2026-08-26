package RPG.Progression.event;

import RPG.Levels.Objects.LevelSkill;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PlayerSkillLevelUpEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final LevelSkill.SkillType skillType;
    private final int newLevel;

    public PlayerSkillLevelUpEvent(Player player, LevelSkill.SkillType skillType, int newLevel) {
        this.player = player;
        this.skillType = skillType;
        this.newLevel = newLevel;
    }

    public Player getPlayer() {
        return player;
    }

    public LevelSkill.SkillType getSkillType() {
        return skillType;
    }

    public int getNewLevel() {
        return newLevel;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}

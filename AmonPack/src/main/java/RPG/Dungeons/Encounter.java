package RPG.Dungeons;

import java.util.ArrayList;
import java.util.List;

public class Encounter {
    private final String id;
    private final String description;
    private final List<DungeonCondition> conditions;
    private final List<DungeonEffect> effects;
    private final List<String> nextEncounters;
    private final List<String> exclude;
    private final int reqClears;
    private final String encAfterClears;

    public Encounter(String id, String description, List<DungeonCondition> conditions, List<DungeonEffect> effects, List<String> nextEncounters, List<String> exclude, int reqClears, String encAfterClears) {
        this.id = id;
        this.description = description;
        this.conditions = conditions != null ? conditions : new ArrayList<>();
        this.effects = effects != null ? effects : new ArrayList<>();
        this.nextEncounters = nextEncounters != null ? nextEncounters : new ArrayList<>();
        this.exclude = exclude != null ? exclude : new ArrayList<>();
        this.reqClears = reqClears;
        this.encAfterClears = encAfterClears;
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public List<DungeonCondition> getConditions() {
        return conditions;
    }

    public List<DungeonEffect> getEffects() {
        return effects;
    }

    public List<String> getNextEncounters() {
        return nextEncounters;
    }

    public List<String> getExclude() {
        return exclude;
    }

    public int getReqClears() {
        return reqClears;
    }

    public String getEncAfterClears() {
        return encAfterClears;
    }
}

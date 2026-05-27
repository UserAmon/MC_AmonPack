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
    private final String title;
    private final List<String> pool;
    private final List<List<String>> poolLists;

    public Encounter(String id, String description, List<DungeonCondition> conditions, List<DungeonEffect> effects, List<String> nextEncounters, List<String> exclude, int reqClears, String encAfterClears, String title, List<String> pool, List<List<String>> poolLists) {
        this.id = id;
        this.description = description;
        this.conditions = conditions != null ? conditions : new ArrayList<>();
        this.effects = effects != null ? effects : new ArrayList<>();
        this.nextEncounters = nextEncounters != null ? nextEncounters : new ArrayList<>();
        this.exclude = exclude != null ? exclude : new ArrayList<>();
        this.reqClears = reqClears;
        this.encAfterClears = encAfterClears;
        this.title = title;
        this.pool = pool != null ? pool : new ArrayList<>();
        this.poolLists = poolLists != null ? poolLists : new ArrayList<>();
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

    public String getTitle() {
        return title;
    }

    public List<String> getPool() {
        return pool;
    }

    public List<List<String>> getPoolLists() {
        return poolLists;
    }
}

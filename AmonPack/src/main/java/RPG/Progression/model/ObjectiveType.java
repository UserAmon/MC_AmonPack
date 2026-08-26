package RPG.Progression.model;

public enum ObjectiveType {
    DESTROY_BLOCK("destroy_block"),
    COLLECT_ITEM("collect_item"),
    CRAFT_ITEM("craft_item"),
    PLACE_BLOCK("place_block"),
    USE_ITEM("use_item"),
    USE_BLOCK("use_block"),
    HARVEST_CROP("harvest_crop"),
    PLANT_CROP("plant_crop"),
    BREED_ENTITY("breed_entity"),
    CATCH_FISH("catch_fish"),
    COOK_ITEM("cook_item"),
    SMELT_ITEM("smelt_item"),
    KILL_ENTITY("kill_entity"),
    SURVIVE_NIGHT("survive_night"),
    DISCOVER_BIOME("discover_biome"),
    DISCOVER_STRUCTURE("discover_structure"),
    TRAVEL_DISTANCE("travel_distance"),
    MINE_TO_DEPTH("mine_to_depth"),
    GAIN_EXPERIENCE("gain_experience"),
    COMPLETE_DUNGEON("complete_dungeon"),
    DEFEAT_BOSS("defeat_boss"),
    REACH_SKILL_LEVEL("reach_skill_level"),
    UNLOCK_KNOWLEDGE("unlock_knowledge"),
    CUSTOM_OBJECTIVE("custom_objective");

    private final String key;

    ObjectiveType(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public static ObjectiveType fromKey(String key) {
        if (key == null) return CUSTOM_OBJECTIVE;
        for (ObjectiveType type : values()) {
            if (type.name().equalsIgnoreCase(key) || type.key.equalsIgnoreCase(key)) {
                return type;
            }
        }
        return CUSTOM_OBJECTIVE;
    }
}

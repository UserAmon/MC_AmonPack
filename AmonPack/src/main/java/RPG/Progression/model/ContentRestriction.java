package RPG.Progression.model;

import org.bukkit.Material;
import org.bukkit.World;

import java.util.*;

public class ContentRestriction {

    private final Map<Material, StageType> itemUsageRestrictions = new EnumMap<>(Material.class);
    private final Map<Material, StageType> blockBreakRestrictions = new EnumMap<>(Material.class);
    private final Map<String, StageType> customItemRestrictions = new HashMap<>();
    private final Map<String, StageType> craftableMoldRestrictions = new HashMap<>();
    private final Map<World.Environment, StageType> dimensionRestrictions = new EnumMap<>(World.Environment.class);

    public ContentRestriction() {
    }

    public void addItemRestriction(Material material, StageType minStage) {
        if (material != null && minStage != null) {
            itemUsageRestrictions.put(material, minStage);
        }
    }

    public void addBlockRestriction(Material material, StageType minStage) {
        if (material != null && minStage != null) {
            blockBreakRestrictions.put(material, minStage);
        }
    }

    public void addCustomItemRestriction(String customItemId, StageType minStage) {
        if (customItemId != null && minStage != null) {
            customItemRestrictions.put(customItemId.toLowerCase(Locale.ROOT), minStage);
        }
    }

    public void addCraftableMoldRestriction(String moldId, StageType minStage) {
        if (moldId != null && minStage != null) {
            craftableMoldRestrictions.put(moldId.toLowerCase(Locale.ROOT), minStage);
        }
    }

    public void addDimensionRestriction(World.Environment env, StageType minStage) {
        if (env != null && minStage != null) {
            dimensionRestrictions.put(env, minStage);
        }
    }

    public StageType getRequiredStageForItem(Material material) {
        return itemUsageRestrictions.get(material);
    }

    public StageType getRequiredStageForBlock(Material material) {
        return blockBreakRestrictions.get(material);
    }

    public StageType getRequiredStageForCustomItem(String customItemId) {
        if (customItemId == null) return null;
        return customItemRestrictions.get(customItemId.toLowerCase(Locale.ROOT));
    }

    public StageType getRequiredStageForCraftableMold(String moldId) {
        if (moldId == null) return null;
        return craftableMoldRestrictions.get(moldId.toLowerCase(Locale.ROOT));
    }

    public StageType getRequiredStageForDimension(World.Environment env) {
        return dimensionRestrictions.get(env);
    }

    public void clear() {
        itemUsageRestrictions.clear();
        blockBreakRestrictions.clear();
        customItemRestrictions.clear();
        craftableMoldRestrictions.clear();
        dimensionRestrictions.clear();
    }
}

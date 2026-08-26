package RPG.Progression.model;

import org.bukkit.ChatColor;
import org.bukkit.Material;

public enum QuestCategory {
    CORE("CORE", "&e&lPodstawy Przetrwania", Material.CRAFTING_TABLE, 201),
    FARMING_HERBALISM("FARMING_HERBALISM", "&a&lRolnictwo i Ziołolecznictwo", Material.WHEAT, 202),
    COOKING_STORAGE_SURVIVAL("COOKING_STORAGE_SURVIVAL", "&6&lKuchnia i Przetrwanie", Material.FURNACE, 203),
    ANIMALS_FISHING("ANIMALS_FISHING", "&b&lHodowla i Rybołówstwo", Material.FISHING_ROD, 204),
    MINING("MINING", "&7&lGórnictwo i Złoża", Material.IRON_PICKAXE, 205),
    BUILDING_BASE("BUILDING_BASE", "&d&lBudowa i Rozwój Bazy", Material.BRICKS, 206),
    EXPLORATION("EXPLORATION", "&3&lEksploracja Świata", Material.COMPASS, 207),
    COMBAT("COMBAT", "&c&lWalka i Pancerz", Material.IRON_SWORD, 208),
    DUNGEONS("DUNGEONS", "&5&lLochy i Podziemia", Material.CHISELED_STONE_BRICKS, 209),
    BOSSES("BOSSES", "&4&lStarożytni Bossowie", Material.WITHER_SKELETON_SKULL, 210);

    private final String id;
    private final String displayName;
    private final Material iconMaterial;
    private final int customModelData;

    QuestCategory(String id, String displayName, Material iconMaterial, int customModelData) {
        this.id = id;
        this.displayName = displayName;
        this.iconMaterial = iconMaterial;
        this.customModelData = customModelData;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return ChatColor.translateAlternateColorCodes('&', displayName);
    }

    public Material getIconMaterial() {
        return iconMaterial;
    }

    public int getCustomModelData() {
        return customModelData;
    }

    public static QuestCategory fromName(String name) {
        if (name == null) return CORE;
        for (QuestCategory cat : values()) {
            if (cat.name().equalsIgnoreCase(name) || cat.id.equalsIgnoreCase(name)) {
                return cat;
            }
        }
        return CORE;
    }
}

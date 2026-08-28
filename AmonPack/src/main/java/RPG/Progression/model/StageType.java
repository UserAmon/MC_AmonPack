package RPG.Progression.model;

import org.bukkit.ChatColor;
import org.bukkit.Material;

import java.util.Arrays;
import java.util.List;

public enum StageType {
    WOODEN(0, "WOODEN", "&6&lEtap Drewniany", ChatColor.GOLD, Material.OAK_LOG, 101, "&7Początki przetrwania, podstawowe narzędzia i schronienie."),
    STONE(1, "STONE", "&7&lEtap Kamienny", ChatColor.GRAY, Material.STONE_PICKAXE, 102, "&7Rozwój osady, kopalnia i podstawowa eksploracja."),
    COPPER(2, "COPPER", "&6&lEtap Miedziany", ChatColor.GOLD, Material.COPPER_INGOT, 107, "&7Obróbka miedzi, elektryczność i pierwsze zaawansowane stopy."),
    IRON(3, "IRON", "&f&lEtap Żelazny", ChatColor.WHITE, Material.IRON_CHESTPLATE, 103, "&7Zaawansowane rzemiosło, pierwsze lochy i bossowie."),
    DIAMOND(4, "DIAMOND", "&b&lEtap Diamentowy", ChatColor.AQUA, Material.DIAMOND_SWORD, 104, "&7Pełne RPG, trudne dungeony i przygotowanie do Netheru."),
    NETHER(5, "NETHER", "&c&lEtap Netheru", ChatColor.RED, Material.NETHERITE_INGOT, 105, "&7Podbój wymiaru ognia, starożytne szczątki i infernalne potęgi."),
    END(6, "END", "&d&lEtap Endu", ChatColor.LIGHT_PURPLE, Material.DRAGON_EGG, 106, "&7Ostateczna granica, potęga Kresu i Smoczy Władca.");

    private final int order;
    private final String id;
    private final String displayName;
    private final ChatColor color;
    private final Material iconMaterial;
    private final int customModelData;
    private final String description;

    StageType(int order, String id, String displayName, ChatColor color, Material iconMaterial, int customModelData, String description) {
        this.order = order;
        this.id = id;
        this.displayName = displayName;
        this.color = color;
        this.iconMaterial = iconMaterial;
        this.customModelData = customModelData;
        this.description = description;
    }

    public int getOrder() {
        return order;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return ChatColor.translateAlternateColorCodes('&', displayName);
    }

    public ChatColor getColor() {
        return color;
    }

    public Material getIconMaterial() {
        return iconMaterial;
    }

    public int getCustomModelData() {
        return customModelData;
    }

    public String getDescription() {
        return ChatColor.translateAlternateColorCodes('&', description);
    }

    public StageType getNext() {
        int nextOrder = this.order + 1;
        for (StageType stage : values()) {
            if (stage.order == nextOrder) {
                return stage;
            }
        }
        return null;
    }

    public StageType getPrevious() {
        int prevOrder = this.order - 1;
        for (StageType stage : values()) {
            if (stage.order == prevOrder) {
                return stage;
            }
        }
        return null;
    }

    public boolean isAtLeast(StageType required) {
        if (required == null) return true;
        return this.order >= required.order;
    }

    public static StageType fromName(String name) {
        if (name == null) return WOODEN;
        for (StageType stage : values()) {
            if (stage.name().equalsIgnoreCase(name) || stage.id.equalsIgnoreCase(name)) {
                return stage;
            }
        }
        return WOODEN;
    }

    public static List<StageType> getOrderedStages() {
        return Arrays.asList(values());
    }
}

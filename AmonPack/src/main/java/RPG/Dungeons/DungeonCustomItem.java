package RPG.Dungeons;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import Plugin.AmonPackPlugin;
import java.util.ArrayList;
import java.util.List;

public class DungeonCustomItem {
    private final String id;
    private final Material material;
    private final String name;
    private final List<String> description;
    private final String type;
    private final int customModelId;

    public DungeonCustomItem(String id, Material material, String name, List<String> description, String type, int customModelId) {
        this.id = id;
        this.material = material;
        this.name = name;
        this.description = description != null ? description : new ArrayList<>();
        this.type = type;
        this.customModelId = customModelId;
    }

    public String getId() {
        return id;
    }

    public Material getMaterial() {
        return material;
    }

    public String getName() {
        return name;
    }

    public List<String> getDescription() {
        return description;
    }

    public String getType() {
        return type;
    }

    public int getCustomModelId() {
        return customModelId;
    }

    public ItemStack toItemStack() {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', name));
            if (!description.isEmpty()) {
                List<String> coloredLore = new ArrayList<>();
                for (String line : description) {
                    coloredLore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', line));
                }
                meta.setLore(coloredLore);
            }
            if (customModelId > 0) {
                meta.setCustomModelData(customModelId);
            }
            meta.getPersistentDataContainer().set(
                new NamespacedKey(AmonPackPlugin.plugin, "dungeon_item_id"),
                PersistentDataType.STRING,
                id
            );
            item.setItemMeta(meta);
        }
        return item;
    }

    private String useMode = "PPM";
    private String effectType = "";
    private int duration = 0;
    private double effectValue = 0.0;

    public String getUseMode() {
        return useMode;
    }

    public void setUseMode(String useMode) {
        this.useMode = useMode;
    }

    public String getEffectType() {
        return effectType;
    }

    public void setEffectType(String effectType) {
        this.effectType = effectType;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public double getEffectValue() {
        return effectValue;
    }

    public void setEffectValue(double effectValue) {
        this.effectValue = effectValue;
    }
}

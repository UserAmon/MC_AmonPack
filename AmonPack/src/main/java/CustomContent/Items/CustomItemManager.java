package CustomContent.Items;

import CustomContent.Pack.PackManager;
import Plugin.AmonPackPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.util.*;

public class CustomItemManager {

    public static final NamespacedKey ITEM_KEY = new NamespacedKey(AmonPackPlugin.plugin, "custom_item_id");

    private final Map<String, CustomItem> items = new LinkedHashMap<>();
    private final PackManager packManager;

    public CustomItemManager(PackManager packManager) {
        this.packManager = packManager;
    }

    public void load() {
        items.clear();
        File file = new File(AmonPackPlugin.plugin.getDataFolder(), "custom_items.yml");
        if (!file.exists()) {
            AmonPackPlugin.plugin.saveResource("custom_items.yml", false);
        }

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        if (cfg.getConfigurationSection("items") != null) {
            for (String key : cfg.getConfigurationSection("items").getKeys(false)) {
                String path = "items." + key;
                String name = ChatColor.translateAlternateColorCodes('&', cfg.getString(path + ".display_name", key));
                String matStr = cfg.getString(path + ".base_material", "DIAMOND_SWORD");
                Material mat = Material.matchMaterial(matStr);
                if (mat == null) mat = Material.DIAMOND_SWORD;

                int cmd = cfg.getInt(path + ".custom_model_data", 10001);
                CustomItem item = new CustomItem(key, name, mat, cmd);
                item.setUnbreakable(cfg.getBoolean(path + ".unbreakable", true));

                List<String> rawLore = cfg.getStringList(path + ".lore");
                List<String> lore = new ArrayList<>();
                for (String l : rawLore) {
                    lore.add(ChatColor.translateAlternateColorCodes('&', l));
                }
                item.setLore(lore);

                // Stats
                item.setAttackDamage(cfg.getDouble(path + ".stats.attack_damage", 0.0));
                item.setAttackSpeed(cfg.getDouble(path + ".stats.attack_speed", 0.0));
                item.setMovementSpeed(cfg.getDouble(path + ".stats.movement_speed", 0.0));
                item.setArmor(cfg.getDouble(path + ".stats.armor", 0.0));
                item.setArmorToughness(cfg.getDouble(path + ".stats.armor_toughness", 0.0));

                // Effects
                item.setOnHitEffectType(cfg.getString(path + ".on_hit_effects.type", null));
                item.setEffectChance(cfg.getDouble(path + ".on_hit_effects.chance", 0.0));
                item.setEffectDuration(cfg.getInt(path + ".on_hit_effects.duration", 0));
                item.setEffectValue(cfg.getDouble(path + ".on_hit_effects.value", 0.0));

                items.put(key.toLowerCase(Locale.ROOT), item);

                // Rejestracja w paczce modeli
                String modelPath = cfg.getString(path + ".model", "item/" + key);
                if (!modelPath.startsWith("amonpack:")) {
                    modelPath = "amonpack:" + modelPath;
                }
                packManager.registerModelOverride(mat.name(), cmd, modelPath);
            }
        }
    }

    public ItemStack createItemStack(String itemId) {
        CustomItem customItem = items.get(itemId.toLowerCase(Locale.ROOT));
        if (customItem == null) return null;

        ItemStack stack = new ItemStack(customItem.getBaseMaterial());
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(customItem.getDisplayName());
            meta.setCustomModelData(customItem.getCustomModelData());
            meta.setUnbreakable(customItem.isUnbreakable());

            if (!customItem.getLore().isEmpty()) {
                meta.setLore(customItem.getLore());
            }

            // Statystyki / AttributeModifiers
            EquipmentSlot slot = EquipmentSlot.HAND;
            String matName = customItem.getBaseMaterial().name();
            if (matName.endsWith("_HELMET")) slot = EquipmentSlot.HEAD;
            else if (matName.endsWith("_CHESTPLATE")) slot = EquipmentSlot.CHEST;
            else if (matName.endsWith("_LEGGINGS")) slot = EquipmentSlot.LEGS;
            else if (matName.endsWith("_BOOTS")) slot = EquipmentSlot.FEET;

            if (customItem.getAttackDamage() > 0) {
                meta.addAttributeModifier(Attribute.ATTACK_DAMAGE, new AttributeModifier(UUID.randomUUID(), "generic.attack_damage", customItem.getAttackDamage(), AttributeModifier.Operation.ADD_NUMBER, slot));
            }
            if (customItem.getAttackSpeed() > 0) {
                meta.addAttributeModifier(Attribute.ATTACK_SPEED, new AttributeModifier(UUID.randomUUID(), "generic.attack_speed", customItem.getAttackSpeed() - 4.0, AttributeModifier.Operation.ADD_NUMBER, slot));
            }
            if (customItem.getArmor() > 0) {
                meta.addAttributeModifier(Attribute.ARMOR, new AttributeModifier(UUID.randomUUID(), "generic.armor", customItem.getArmor(), AttributeModifier.Operation.ADD_NUMBER, slot));
            }
            if (customItem.getArmorToughness() > 0) {
                meta.addAttributeModifier(Attribute.ARMOR_TOUGHNESS, new AttributeModifier(UUID.randomUUID(), "generic.armor_toughness", customItem.getArmorToughness(), AttributeModifier.Operation.ADD_NUMBER, slot));
            }
            if (customItem.getMovementSpeed() > 0) {
                meta.addAttributeModifier(Attribute.MOVEMENT_SPEED, new AttributeModifier(UUID.randomUUID(), "generic.movement_speed", customItem.getMovementSpeed(), AttributeModifier.Operation.ADD_NUMBER, slot));
            }

            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE);
            meta.getPersistentDataContainer().set(ITEM_KEY, PersistentDataType.STRING, customItem.getId());
            stack.setItemMeta(meta);
        }
        return stack;
    }

    public CustomItem getCustomItem(String itemId) {
        return items.get(itemId.toLowerCase(Locale.ROOT));
    }

    public String getCustomItemId(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return null;
        return stack.getItemMeta().getPersistentDataContainer().get(ITEM_KEY, PersistentDataType.STRING);
    }

    public Map<String, CustomItem> getAllItems() {
        return Collections.unmodifiableMap(items);
    }
}

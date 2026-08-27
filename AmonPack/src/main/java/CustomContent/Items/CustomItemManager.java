package CustomContent.Items;

import CustomContent.Pack.PackManager;
import Plugin.AmonPackPlugin;
import org.bukkit.Bukkit;
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
        File packFolder = new File(AmonPackPlugin.plugin.getDataFolder(), "pack");
        if (!packFolder.exists()) packFolder.mkdirs();

        File file = new File(packFolder, "custom_items.yml");
        if (!file.exists()) {
            AmonPackPlugin.plugin.saveResource("pack/custom_items.yml", false);
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

        // Gwarancja obecności Magicznego Stołu Warsztatowego
        if (!items.containsKey("magic_crafting_table")) {
            CustomItem tableItem = new CustomItem("magic_crafting_table", "§d§lMagiczny Stół Warsztatowy", Material.NOTE_BLOCK, 30002);
            tableItem.setUnbreakable(true);
            tableItem.setLore(List.of(
                    "§7Nasycony mistyczną energią stół rzemieślniczy.",
                    "§7Pozwala na wykuwanie form, pancerzy i broni.",
                    "",
                    "§e✦ Kliknij PPM po postawieniu, aby otworzyć magiczne rzemiosło!"
            ));
            items.put("magic_crafting_table", tableItem);
            packManager.registerModelOverride("note_block", 30002, "amonpack:block/magic_crafting_table");
        }

        // Gwarancja obecności Tomu Ognia
        if (!items.containsKey("tome_fire")) {
            CustomItem tome = new CustomItem("tome_fire", "§c§lTom Ognia", Material.BOOK, 20001);
            tome.setUnbreakable(true);
            tome.setLore(List.of(
                    "§7Starożytna księga zawierająca pierwotne zaklęcia płomieni.",
                    "",
                    "§6✦ LPM: §fWystrzelenie aktywnego czaru §7(np. Fireblast)",
                    "§e✦ Shift + LPM: §fRzucenie zaklęcia drugiego kręgu",
                    "§d✦ PPM: §fOtwórz menu zaklęć i przypisz czary",
                    "",
                    "§b✦ Koszt Fireblast: §f40 MP §8| §eCooldown: §f4.0s"
            ));
            items.put("tome_fire", tome);
            packManager.registerModelOverride("book", 20001, "amonpack:magic/tome_fire");
            packManager.registerModelOverride("enchanted_book", 20001, "amonpack:magic/tome_fire");
        }

        // Rejestracja broni
        registerDefaultItem("boomerang", "§b§lBumerang", Material.WOODEN_SWORD, 10000, "amonpack:weapons/boomerang");
        registerDefaultItem("wachlarz", "§d§lŻelazny Wachlarz", Material.WOODEN_SWORD, 10001, "amonpack:weapons/wachlarz");
        registerDefaultItem("laska_aanga", "§e§lLaska Aanga", Material.WOODEN_SWORD, 10002, "amonpack:weapons/laska_aanga");
        registerDefaultItem("bambus", "§a§lKij Bambusowy", Material.WOODEN_SWORD, 10003, "amonpack:weapons/bambus");
        registerDefaultItem("earth_hammer", "§6§lMłot Ziemi", Material.WOODEN_SWORD, 10004, "amonpack:weapons/earth_hammer");
        registerDefaultItem("msokka", "§9§lKosmiczny Miecz Sokki", Material.WOODEN_SWORD, 10005, "amonpack:weapons/msokka");
        registerDefaultItem("wlocznia_ognia", "§c§lWłócznia Ognia", Material.WOODEN_SWORD, 10006, "amonpack:weapons/wlocznia_ognia");
        registerDefaultItem("sztylet", "§8§lSztylet Cienia", Material.WOODEN_SWORD, 10007, "amonpack:weapons/sztylet");

        // Rejestracja rzemiosła
        registerDefaultItem("mold_empty", "§7§lPusta Forma", Material.PAPER, 10001, "amonpack:crafting/mold_empty");
        registerDefaultItem("mold_full", "§6§lWypełniona Forma", Material.PAPER, 10002, "amonpack:crafting/mold_full");
        registerDefaultItem("meteor_shard", "§4§lOdłamek Meteorytu", Material.PAPER, 10003, "amonpack:crafting/meteor_shard");
        registerDefaultItem("basalt_shard", "§8§lOdłamek Bazaltu", Material.PAPER, 10004, "amonpack:crafting/basalt_shard");
        registerDefaultItem("firescroll", "§c§lZwój Płomieni", Material.PAPER, 10005, "amonpack:crafting/firescroll");

        // Rejestracja bloków
        registerDefaultItem("meteoryt_ore", "§4§lRuda Meteorytu", Material.NOTE_BLOCK, 30001, "amonpack:block/meteoryt_ore");
        registerDefaultItem("basalt_ore", "§8§lRuda Bazaltu", Material.NOTE_BLOCK, 30003, "amonpack:block/basalt_ore");
        registerDefaultItem("arcane_altar", "§5§lOłtarz Arkanów", Material.NOTE_BLOCK, 30004, "amonpack:block/arcane_altar");

        registerRecipes();
    }

    private void registerDefaultItem(String id, String name, Material mat, int cmd, String modelPath) {
        if (!items.containsKey(id)) {
            CustomItem ci = new CustomItem(id, name, mat, cmd);
            ci.setModel(modelPath);
            ci.setUnbreakable(true);
            items.put(id, ci);
        }
        packManager.registerModelOverride(mat.name(), cmd, modelPath);
    }

    public void registerRecipes() {
        NamespacedKey tableRecipeKey = new NamespacedKey(AmonPackPlugin.plugin, "magic_crafting_table_recipe");
        NamespacedKey altarRecipeKey = new NamespacedKey(AmonPackPlugin.plugin, "arcane_altar_recipe");
        try {
            Bukkit.removeRecipe(tableRecipeKey);
            Bukkit.removeRecipe(altarRecipeKey);
        } catch (Throwable ignored) {}

        ItemStack tableItem = createItemStack("magic_crafting_table");
        if (tableItem == null) {
            tableItem = createDefaultMagicCraftingTable();
        }

        try {
            org.bukkit.inventory.ShapedRecipe recipe = new org.bukkit.inventory.ShapedRecipe(tableRecipeKey, tableItem);
            recipe.shape("PPP", "CCC", "CCC");
            recipe.setIngredient('P', Material.PAPER);
            recipe.setIngredient('C', Material.COBBLESTONE);
            Bukkit.addRecipe(recipe);
        } catch (Throwable t) {
            Bukkit.getLogger().warning("[AmonPack] Błąd rejestracji receptury magic_crafting_table: " + t.getMessage());
        }

        ItemStack altarItem = createItemStack("arcane_altar");
        if (altarItem == null) {
            altarItem = new ItemStack(Material.NOTE_BLOCK);
            ItemMeta meta = altarItem.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§5§lOłtarz Arkanów");
                meta.setCustomModelData(30004);
                meta.setLore(List.of(
                        "§7Mistyczny ołtarz służący do ulepszania magicznych ksiąg i rozwijania zaklęć.",
                        "",
                        "§e✦ Kliknij PPM po postawieniu, aby ulepszać tomy i magię!"
                ));
                altarItem.setItemMeta(meta);
            }
        }

        try {
            org.bukkit.inventory.ShapedRecipe altarRecipe = new org.bukkit.inventory.ShapedRecipe(altarRecipeKey, altarItem);
            altarRecipe.shape("DBD", "OPO", "OOO");
            altarRecipe.setIngredient('D', Material.DIAMOND);
            altarRecipe.setIngredient('B', Material.BOOK);
            altarRecipe.setIngredient('P', Material.BLAZE_POWDER);
            altarRecipe.setIngredient('O', Material.OBSIDIAN);
            Bukkit.addRecipe(altarRecipe);
        } catch (Throwable t) {
            Bukkit.getLogger().warning("[AmonPack] Błąd rejestracji receptury arcane_altar: " + t.getMessage());
        }
    }

    public ItemStack createDefaultMagicCraftingTable() {
        ItemStack stack = new ItemStack(Material.NOTE_BLOCK);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§d§lMagiczny Stół Warsztatowy");
            meta.setCustomModelData(30002);
            meta.setUnbreakable(true);
            meta.setLore(List.of(
                    "§7Nasycony mistyczną energią stół rzemieślniczy.",
                    "§7Pozwala na wykuwanie form, pancerzy i broni.",
                    "",
                    "§e✦ Kliknij PPM po postawieniu, aby otworzyć magiczne rzemiosło!"
            ));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE);
            meta.getPersistentDataContainer().set(ITEM_KEY, PersistentDataType.STRING, "magic_crafting_table");
            stack.setItemMeta(meta);
        }
        return stack;
    }

    public ItemStack createDefaultTomeFire() {
        ItemStack stack = new ItemStack(Material.BOOK);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§c§lTom Ognia");
            meta.setCustomModelData(20001);
            meta.setUnbreakable(true);
            meta.setLore(List.of(
                    "§7Starożytna księga zawierająca pierwotne zaklęcia płomieni.",
                    "",
                    "§6✦ LPM: §fWystrzelenie aktywnego czaru §7(np. Fireblast)",
                    "§e✦ Shift + LPM: §fRzucenie zaklęcia drugiego kręgu",
                    "§d✦ PPM: §fOtwórz menu zaklęć i przypisz czary",
                    "",
                    "§b✦ Koszt Fireblast: §f40 MP §8| §eCooldown: §f4.0s"
            ));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE);
            meta.getPersistentDataContainer().set(ITEM_KEY, PersistentDataType.STRING, "tome_fire");
            stack.setItemMeta(meta);
        }
        return stack;
    }

    public ItemStack createItemStack(String itemId) {
        if (itemId == null) return null;
        if (itemId.equalsIgnoreCase("magic_crafting_table") && !items.containsKey("magic_crafting_table")) {
            return createDefaultMagicCraftingTable();
        }
        if (itemId.equalsIgnoreCase("tome_fire") && !items.containsKey("tome_fire")) {
            return createDefaultTomeFire();
        }
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
        String id = stack.getItemMeta().getPersistentDataContainer().get(ITEM_KEY, PersistentDataType.STRING);
        if (id != null && !id.isEmpty()) return id;

        var meta = stack.getItemMeta();
        if (meta.hasCustomModelData()) {
            int cmd = meta.getCustomModelData();
            if (cmd == 30001) return "meteoryt_ore";
            if (cmd == 30002) return "magic_crafting_table";
            if (cmd == 30003) return "basalt_ore";
            if (cmd == 30004) return "arcane_altar";
            if (cmd == 20001) return "tome_fire";
            if (cmd == 20002) return "wand_fen";
            if (cmd == 10020) return "bone_sword";
            if (cmd == 10021) return "custom_bow";
            if (cmd == 10014) return "meteor_pickaxe";
            if (cmd == 10002) return "meteor_axe";
        }

        if (meta.hasDisplayName()) {
            String name = ChatColor.stripColor(meta.getDisplayName()).toLowerCase();
            if (name.contains("magiczny stół") || name.contains("magic crafting")) return "magic_crafting_table";
            if (name.contains("ołtarz arkanów") || name.contains("arcane altar")) return "arcane_altar";
            if (name.contains("ruda meteorytu")) return "meteoryt_ore";
            if (name.contains("ruda bazaltu")) return "basalt_ore";
            if (name.contains("tom ognia")) return "tome_fire";
            if (name.contains("różdżka") || name.contains("fen") || name.contains("wand")) return "wand_fen";
            if (name.contains("kościany miecz")) return "bone_sword";
            if (name.contains("długi łuk") || name.contains("custom bow")) return "custom_bow";
            if (name.contains("meteorytowy kilof")) return "meteor_pickaxe";
            if (name.contains("meteorytowy topór")) return "meteor_axe";
        }
        return null;
    }

    public Map<String, CustomItem> getAllItems() {
        return Collections.unmodifiableMap(items);
    }
}

package AvatarSystems.Crafting;

import AvatarSystems.Crafting.Objects.*;
import AvatarSystems.Util_Objects.InventoryXHolder;
import AvatarSystems.Util_Objects.LevelSkill;
import dev.lone.itemsadder.api.FontImages.FontImageWrapper;
import dev.lone.itemsadder.api.FontImages.TexturedInventoryWrapper;
import methods_plugins.AmonPackPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static methods_plugins.AmonPackPlugin.FastEasyStack;
import static methods_plugins.AmonPackPlugin.FastEasyStackWithLoreModelData;

public class CraftingMenager {
    //listy broni, scrolli, pancerzy.
    public static List<CraftedWeapon> AllCraftableWeapons = new ArrayList<>();
    public static List<Craftable_Tool> AllTools = new ArrayList<>();
    public static List<Craftable_Armor> AllArmor = new ArrayList<>();
    //AllMolds słuzy do szukania i łączenia itemów na podstawie mold_id
    public static List<ItemMold> AllMolds = new ArrayList<>();
    public static List<MagicEffects> AllMagicEffects = new ArrayList<>();
    public static InventoryXHolder CraftingGui;
    public static InventoryXHolder EffectsGui;
    public static InventoryXHolder CategoryGui;

    public CraftingMenager() {
        ReloadConfig();
        CreateInventories();
    }

    public static void OpenMoldCategory(Player player){
        Inventory inv = Bukkit.createInventory(CategoryGui, CategoryGui.getSize(), CategoryGui.getTitle());

        ItemStack Weapon= FastEasyStackWithLoreModelData(Material.PAPER,
                ChatColor.DARK_AQUA +" Bronie",
                List.of(ChatColor.DARK_GREEN+"Kliknij mnie aby otworzyć menu z wyborem Broni"),
                1001);

        ItemStack Tool= FastEasyStackWithLoreModelData(Material.PAPER,
                ChatColor.DARK_RED +" Narzędzia",
                List.of(ChatColor.DARK_GREEN+"Kliknij mnie aby otworzyć menu z wyborem Narzędzi"),
                1001);

        ItemStack Armor= FastEasyStackWithLoreModelData(Material.PAPER,
                ChatColor.DARK_BLUE +" Zbroje",
                List.of(ChatColor.DARK_GREEN+"Kliknij mnie aby otworzyć menu z wyborem Zbroi"),
                1001);

        inv.setItem(1, Weapon);
        inv.setItem(4, Tool);
        inv.setItem(7, Armor);

        player.openInventory(inv);
    }

    public static void OpenMoldCrafting(Player player, int Category){
        TexturedInventoryWrapper inventory = new TexturedInventoryWrapper(CraftingGui,
                CraftingGui.getSize(), CraftingGui.getTitle(), new FontImageWrapper("amonpack:bending_abilities_list")
        );
        List<ItemMold> ChosenMolds = new ArrayList<>();
        switch (Category){
            case 0:
                ChosenMolds.addAll(AllCraftableWeapons);
                break;
            case 1:
                ChosenMolds.addAll(AllArmor);
                break;
            case 2:
                ChosenMolds.addAll(AllTools);
                break;
        }
        Inventory inv = inventory.getInternal();
        for (ItemMold mold : ChosenMolds) {
            ItemStack stack = mold.toItemStack();
            ItemMeta meta = stack.getItemMeta();
            List<String> lore = new ArrayList<>(mold.getItemLore());
            lore.add("§7Potrzebne materiały:");
            for (ItemStack req : mold.getItemsRequiredToShapeMold()) {
                if(req.getItemMeta()!=null && !req.getItemMeta().getItemName().isEmpty()){
                    lore.add("§8- §f" + req.getAmount() + "x " + req.getItemMeta().getItemName());
                }else{
                    lore.add("§8- §f" + req.getAmount() + "x " + req.getType().name());
                }
            }
            meta.setLore(lore);
            stack.setItemMeta(meta);
            inv.addItem(stack);
        }
        inventory.showInventory(player);
    }

    public static void OpenMagicEffectsGui(Player player, ItemStack item, ItemStack clickeditem){
        TexturedInventoryWrapper inventory = new TexturedInventoryWrapper(EffectsGui,
                EffectsGui.getSize(), EffectsGui.getTitle(), new FontImageWrapper("amonpack:bending_abilities_list")
        );
        ItemMold moldItem = getItemMoldByItem(item);
        Inventory inv = inventory.getInternal();

        for (MagicEffects effect : moldItem.getAllowedMagicEffects()){
            ItemStack effectItem = new ItemStack(Material.BOOK);
            List<String> Lore=new ArrayList<>();
            if(effect.isMajorRune()){
                effectItem.setType(Material.ENCHANTED_BOOK);
                Lore.add("§3§lWiększa Runa");
            }
            ItemMeta meta = effectItem.getItemMeta();
            meta.setDisplayName(effect.getDisplayName());
            try {
                if(effect.getLoreDescription()!=null){
                    Lore.addAll(effect.getLoreDescription());
                }
            } catch (Exception e) {
                System.out.println("catch z lore "+e);
            }
            if (!effect.getConditions().isEmpty() && effect.getConditions()!=null) {
                Lore.add("§7Wymagania umiejętności:");
                for (MagicEffectsConditions cond : effect.getConditions()) {
                    if(cond.isSkillRequired()){
                        Lore.add("§8- §f" + cond.getType() + " §7poziom §f" + cond.getRequiredSkillLevel());
                    }
                }
            }
            if (!effect.getCost().isEmpty() && effect.getCost()!=null) {
                Lore.add("§7Potrzebne materiały:");
                for (ItemStack req : effect.getCost()) {
                    if(req.getItemMeta()!=null && !req.getItemMeta().getItemName().isEmpty()){
                        Lore.add("§8- §f" + req.getAmount() + "x " + req.getItemMeta().getItemName());
                    }else{
                        Lore.add("§8- §f" + req.getAmount() + "x " + req.getType().name());
                    }
                }
            }
            Lore.add("§3Kliknij, aby dodać efekt");
            meta.setLore(Lore);
            effectItem.setItemMeta(meta);
            inv.addItem(effectItem);
        }
        MagicEffects EffectToAplly;
        if(clickeditem != null){
            String effectName = ChatColor.stripColor(clickeditem.getItemMeta().getDisplayName());
            MagicEffects effect = GetMagicEfectByDisplayName(effectName);
            EffectToAplly = effect;
        } else {
            EffectToAplly = null;
        }
        List<MagicEffects> ExistingEffects = new ArrayList<>();
        List<String> EffectsLore = new ArrayList<>();
        NamespacedKey key = new NamespacedKey(AmonPackPlugin.plugin, "magic_effects");
        String data = Objects.requireNonNull(item.getItemMeta()).getPersistentDataContainer().get(key, PersistentDataType.STRING);
        if (data != null && !data.isEmpty()) {
            ExistingEffects.addAll(MagicEffects.deserializeList(data));
        }
        if(EffectToAplly!=null && ExistingEffects.stream().filter(ef->ef.getName().equalsIgnoreCase(EffectToAplly.getName())).findFirst().isEmpty()){
            ExistingEffects.add(EffectToAplly);
            player.sendMessage( ChatColor.AQUA+ChatColor.BOLD.toString()+"Pomyślnie dodano atrybut do formy: "+EffectToAplly.getDisplayName());
        }
        if(!ExistingEffects.isEmpty())
            EffectsLore.add("§9Zaklęte Runy:");
        for (MagicEffects effects : ExistingEffects){
            EffectsLore.add("§8- " + effects.getDisplayName());
        }
        ItemStack preview = moldItem.addEffectsToItem(moldItem.to_Empty_Mold_ItemStack(), ExistingEffects);
        ItemMeta imeta= preview.getItemMeta();
        List<String> newLore = new ArrayList<>();
        if(imeta!=null && imeta.getLore()!=null) newLore.addAll(imeta.getLore());
        newLore.addAll(EffectsLore);
        imeta.setLore(newLore);
        preview.setItemMeta(imeta);
        inv.setItem(53,preview);
        inventory.showInventory(player);
    }


    public void ReloadConfig(){
        AllCraftableWeapons.clear();
        AllMolds.clear();
        AllTools.clear();
        AllMagicEffects.clear();
        FileConfiguration Config = AmonPackPlugin.getConfigs_menager().getCrafting_Config();
        System.out.println("Reload Craftowania");
        try {
            for (String effectId : Objects.requireNonNull(Config.getConfigurationSection("MagicEffects")).getKeys(false)) {
                String name = Config.getString("MagicEffects." + effectId + ".Name");
                List<String> lore = new ArrayList<>();
                if (Config.getConfigurationSection("MagicEffects." + effectId + ".Lore") != null) {
                    for (String key : Config.getConfigurationSection("MagicEffects." + effectId + ".Lore").getKeys(false)) {
                        String loreLine = Config.getString("MagicEffects." + effectId + ".Lore." + key);
                        if (loreLine != null) {
                            lore.add(loreLine);
                        }
                    }
                }
                boolean IsMajor = Config.getBoolean("MagicEffects." + effectId + ".IsMajor");
                List<MagicEffectsConditions> conditions = new ArrayList<>();
                if (Config.getConfigurationSection("MagicEffects." + effectId + ".Conditions") != null) {
                    for (String conditionsname : Config.getConfigurationSection("MagicEffects." + effectId + ".Conditions").getKeys(false)) {
                        String path = "MagicEffects." + effectId + ".Conditions." + conditionsname;
                        String skillType = Config.getString(path + ".Skill_Type");
                        int skillLevel = Config.getInt(path + ".Skill_Level");
                        conditions.add(new MagicEffectsConditions(
                                skillLevel,
                                LevelSkill.SkillType.valueOf(skillType)
                        ));
                    }
                }
                List<ItemStack> cost = new ArrayList<>();
                if (Config.getConfigurationSection("MagicEffects." + effectId + ".Cost") != null) {
                    for (String costKey : Config.getConfigurationSection("MagicEffects." + effectId + ".Cost").getKeys(false)) {
                        String path = "MagicEffects." + effectId + ".Cost." + costKey;
                        String matName = Config.getString(path + ".Material");
                        int amount = Config.getInt(path + ".Amount");

                        Material mat = Material.getMaterial(matName);
                        if (mat != null) {
                            cost.add(new ItemStack(mat, amount));
                        } else {
                            dev.lone.itemsadder.api.CustomStack custom = dev.lone.itemsadder.api.CustomStack.getInstance(matName);
                            if (custom != null) {
                                ItemStack iaItem = custom.getItemStack().clone();
                                iaItem.setAmount(amount);
                                cost.add(iaItem);
                            } else {
                                System.out.println("⚠ Nieprawidłowy materiał (ani vanilla, ani ItemsAdder) w MagicEffects." + effectId + ".Cost." + costKey + ": " + matName);
                            }
                        }
                    }
                }

                AllMagicEffects.add(new MagicEffects(conditions, cost, name, lore, effectId, IsMajor));
            }

        }catch (Exception e){
            System.out.println(" bład przy ładowaniu magic effects - "+e);
        }
        try {
            int IdCounter = 10;
            for(String WeaponName : Objects.requireNonNull(Config.getConfigurationSection("Craftable_Items_Molds")).getKeys(false)) {
                String path = "Craftable_Items_Molds."+WeaponName+".";
                Material material = Material.getMaterial(Objects.requireNonNull(Config.getString(path + "Material")));
                String DisplayName = Objects.requireNonNull(Config.getString(path + "Name"));
                Integer CustomModelId = Config.getInt(path + "Custom_Model_ID");
                int BaseDmg = Config.getInt(path + "Base_Damage");

                String MoldPath = path+"Mold.";
                List<ItemStack> ItemToShapeMold = new ArrayList<>();
                for(String MoldCraftItem : Objects.requireNonNull(Config.getConfigurationSection(MoldPath+"Items_To_Craft")).getKeys(false)) {
                    String matName = Config.getString(MoldPath + "Items_To_Craft."+MoldCraftItem+".Material");
                    int amount = Config.getInt(MoldPath + "Items_To_Craft."+MoldCraftItem+".Amount");

                    Material mat = Material.getMaterial(matName);
                    if (mat != null) {
                        ItemToShapeMold.add(new ItemStack(mat, amount));
                    } else {
                        dev.lone.itemsadder.api.CustomStack custom = dev.lone.itemsadder.api.CustomStack.getInstance(matName);
                        if (custom != null) {
                            ItemStack iaItem = custom.getItemStack().clone();
                            iaItem.setAmount(amount);
                            ItemToShapeMold.add(iaItem);

                        } else {
                            System.out.println("⚠ Nieprawidłowy materiał w Items_To_Craft: " + matName);
                        }
                    }

                }

                List<MagicEffects> AllowedEffects = new ArrayList<>();
                if (!Config.getStringList(MoldPath + "AllowedMagicEffects").isEmpty()) {
                    for (String effectId : Config.getStringList(MoldPath + "AllowedMagicEffects")) {
                        for (MagicEffects effect : AllMagicEffects) {
                            if (effect.getName().equalsIgnoreCase(effectId)) {
                                AllowedEffects.add(effect);
                                break;
                            }
                        }
                    }
                }else {
                    AllowedEffects.addAll(AllMagicEffects);
                }

                String ItemPath = path + "Item.";
                List<String> ItemLoreList = new ArrayList<>();
                for(String key : Objects.requireNonNull(Config.getConfigurationSection(ItemPath + "Lore")).getKeys(false)) {
                    String loreLine = Config.getString(ItemPath + "Lore." + key);
                    if(loreLine != null) {
                        ItemLoreList.add(loreLine);
                    }
                }
                List<ItemStack> ItemToCraftItem = new ArrayList<>();
                for(String CraftItem : Objects.requireNonNull(Config.getConfigurationSection(ItemPath+"Items_To_Craft")).getKeys(false)) {
                    Material ItemMaterial = Material.getMaterial(Objects.requireNonNull(Config.getString(ItemPath + "Items_To_Craft."+CraftItem+".Material")));
                    int amount = Config.getInt(ItemPath + "Items_To_Craft."+CraftItem+".Amount");
                    ItemToCraftItem.add(new ItemStack(ItemMaterial,amount));
                }
                CraftedWeapon w = new CraftedWeapon(""+IdCounter,ItemToShapeMold,DisplayName,material,ItemLoreList,CustomModelId,AllowedEffects,BaseDmg);
                AllCraftableWeapons.add(w);
                IdCounter++;
            }
        } catch (Exception e) {
            System.out.println("error w reloadu craftingu "+e);
        }

        AllMolds.addAll(AllCraftableWeapons);
        Craftable_Tool tool1 = new Craftable_Tool("100",new ArrayList<>(),ChatColor.DARK_GREEN+"Kilof z Meteorytu", Material.NETHERITE_PICKAXE,new ArrayList<>(),10000,new ArrayList<>());
        Craftable_Armor armor_1 = new Craftable_Armor("200",new ArrayList<>(),ChatColor.DARK_GREEN+"Kryształowy Hełm z Omashu", Material.IRON_HELMET,new ArrayList<>(),0,new ArrayList<>(),1.5);
        AllTools.add(tool1);
        AllArmor.add(armor_1);
        AllMolds.addAll(AllTools);
        AllMolds.addAll(AllArmor);
    }
    public static boolean IsWeapon(ItemStack item){
        if(getCraftedWeaponByItem(item)!=null){
            return true;
        }
        return false;
    }
    public static boolean IsArmor(ItemStack item){
        if(GetCraftedArmorByItem(item)!=null){
            return true;
        }
        return false;
    }
    public static CraftedWeapon getCraftedWeaponByItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        NamespacedKey key = new NamespacedKey(AmonPackPlugin.plugin, "weapon_id");
        String id = item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
        if (id == null) return null;

        for (CraftedWeapon weapon : AllCraftableWeapons) {
            if (weapon.getWeaponID().equals(id)) return weapon;
        }
        return null;
    }
    public static Craftable_Tool getCraftedToolByItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        NamespacedKey key = new NamespacedKey(AmonPackPlugin.plugin, "weapon_id");
        String id = item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
        if (id == null) return null;

        for (Craftable_Tool tool : AllTools) {
            if (tool.getWeaponID().equals(id)) return tool;
        }
        return null;
    }
    public static Craftable_Armor GetCraftedArmorByItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        NamespacedKey key = new NamespacedKey(AmonPackPlugin.plugin, "weapon_id");
        String id = item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
        if (id == null) return null;

        for (Craftable_Armor tool : AllArmor) {
            if (tool.getWeaponID().equals(id)) return tool;
        }
        return null;
    }
    public static ItemMold getItemMoldByItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        NamespacedKey key = new NamespacedKey(AmonPackPlugin.plugin, "weapon_id");
        String id = item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
        if (id == null) return null;

        for (ItemMold weapon : AllMolds) {
            if (weapon.getWeaponID().equals(id)) return weapon;
        }
        return null;
    }
    public void CreateInventories(){
        CraftingGui=new InventoryXHolder(54,"");
        EffectsGui=new InventoryXHolder(54,"");
        CategoryGui=new InventoryXHolder(9,"");
    }
    public static MagicEffects GetMagicEfectByDisplayName(String name){
        return AllMagicEffects.stream().filter(magicEffects -> ChatColor.stripColor(magicEffects.getDisplayName()).equalsIgnoreCase(name)).findFirst().orElse(null);
    }
    public static MagicEffects GetMagicEfectByName(String name){
        return AllMagicEffects.stream().filter(magicEffects -> magicEffects.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
    }
    public static boolean HaveMajor(ItemStack item){
        ItemMeta meta = item.getItemMeta();
        NamespacedKey key = new NamespacedKey(AmonPackPlugin.plugin, "magic_effects");
        String data = meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        if (data != null && !data.isEmpty()) {
            for (MagicEffects effects : MagicEffects.deserializeList(data)){
                if(effects.isMajorRune())return true;
            }
        }
        return false;
    }
    public static List<MagicEffects> ShowEffects(ItemStack item){
        List<MagicEffects> effekty = new ArrayList<>();
        if(item!=null){
            ItemMeta meta = item.getItemMeta();
            NamespacedKey key = new NamespacedKey(AmonPackPlugin.plugin, "magic_effects");
            String data = meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
            if (data != null && !data.isEmpty()) {
                effekty = MagicEffects.deserializeList(data);
            }}
        return effekty;
    }
    public static boolean HaveEffect(ItemStack item, String effectname){
        if(item!=null){
        ItemMeta meta = item.getItemMeta();
        NamespacedKey key = new NamespacedKey(AmonPackPlugin.plugin, "magic_effects");
        String data = meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        if (data != null && !data.isEmpty()) {
            MagicEffects effect= MagicEffects.deserializeList(data).stream().filter(effects -> effects.getName().equalsIgnoreCase(effectname)).findFirst().orElse(null);
            return effect != null;
        }}
        return false;
    }
    public static List<MagicEffects> getEffectsFromItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return new ArrayList<>();
        }

        ItemMeta meta = item.getItemMeta();
        NamespacedKey key = new NamespacedKey(AmonPackPlugin.plugin, "magic_effects");

        String data = meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        if (data == null || data.isEmpty()) {
            return new ArrayList<>();
        }

        try {
            return MagicEffects.deserializeList(data); // korzystasz z tego co już masz
        } catch (Exception e) {
            System.out.println("❌ Błąd przy odczytywaniu efektów z itemu: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    public static boolean HaveItems(Player p, Boolean Delete, List<ItemStack> itemToDelete) {
        boolean hasAll = true;
        for (ItemStack required : itemToDelete) {
            if (!p.getInventory().containsAtLeast(required, required.getAmount())) {
                hasAll = false;
                break;
            }
        }
            if (Delete && hasAll) {
                for (ItemStack required : itemToDelete) {
                    int amountToRemove = required.getAmount();
                    for (ItemStack content : p.getInventory().getContents()) {
                        if (content == null) continue;
                        if (content.getType() == required.getType()) {
                            boolean sameMeta = true;
                            if (required.hasItemMeta()) {
                                ItemMeta reqMeta = required.getItemMeta();
                                ItemMeta contMeta = content.getItemMeta();
                                if (reqMeta.hasCustomModelData() && contMeta.hasCustomModelData()) {
                                    if (reqMeta.getCustomModelData() != contMeta.getCustomModelData()) {
                                        sameMeta = false;
                                    }
                                }
                            }
                            if (sameMeta) {
                                int contentAmount = content.getAmount();
                                if (contentAmount > amountToRemove) {
                                    content.setAmount(contentAmount - amountToRemove);
                                    break;
                                } else {
                                    amountToRemove -= contentAmount;
                                    content.setAmount(0);
                                }
                            }
                        }
                    }
                }
                p.updateInventory();
            }
        return hasAll;

    }



}

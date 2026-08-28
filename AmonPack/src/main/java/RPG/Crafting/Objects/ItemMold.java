package RPG.Crafting.Objects;

import com.projectkorra.projectkorra.ability.Ability;
import com.projectkorra.projectkorra.ability.CoreAbility;
import Plugin.AmonPackPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ItemMold {
    private String ItemName;
    private final List<ItemStack> ItemsRequiredToShapeMold;
    private List<String> ItemLore;
    private Material ItemMaterial;
    private Integer CustomModelID;
    private final String weaponID;
    private List<MagicEffects> AllowedMagicEffects = new ArrayList<>();
    private ItemType TypeOfMold;

    public enum ItemType {
        WEAPON,
        TOOL,
        ARMOR,
        ITEM
    }

    public ItemMold(String weaponID, List<ItemStack> itemsRequiredToShapeMold, String itemName, Material itemMaterial,
            List<String> itemLore, Integer customModelID, List<MagicEffects> allowedMagicEffects, ItemType i_type) {
        this.weaponID = weaponID;
        ItemsRequiredToShapeMold = itemsRequiredToShapeMold;
        ItemName = itemName;
        ItemMaterial = itemMaterial;
        ItemLore = itemLore;
        CustomModelID = customModelID;
        AllowedMagicEffects = allowedMagicEffects;
        TypeOfMold = i_type;
    }

    public void Craft(Player player, List<MagicEffects> ListOfEffects, ItemStack mold, boolean CraftIntoItem,
            double damage) {
        if (mold == null || !mold.hasItemMeta())
            return;
        List<MagicEffects> ExistingEffects = new ArrayList<>();
        List<String> EffectsLore = new ArrayList<>(ItemLore);
        NamespacedKey key = new NamespacedKey(AmonPackPlugin.plugin, "magic_effects");
        String data = Objects.requireNonNull(mold.getItemMeta()).getPersistentDataContainer().get(key,
                PersistentDataType.STRING);
        if (data != null && !data.isEmpty()) {
            ExistingEffects.addAll(MagicEffects.deserializeList(data));
        }
        if (mold.getType() == Material.PAPER) {
            player.getInventory().remove(mold);
        }

        ExistingEffects.addAll(ListOfEffects);
        if (!ExistingEffects.isEmpty()) {
            EffectsLore.add("§9Wykute Runy:");
            for (MagicEffects effects : ExistingEffects) {
                EffectsLore.add("§8- " + effects.getDisplayName());
            }
        }
        ItemStack NewMold;
        if (CraftIntoItem) {
            NewMold = addEffectsToItem(toItemStack(), ExistingEffects);
        } else {
            NewMold = addEffectsToItem(to_Empty_Mold_ItemStack(), ExistingEffects);
        }
        ItemMeta MoldMeta = NewMold.getItemMeta();
        assert MoldMeta != null;
        if (TypeOfMold == ItemType.WEAPON) {
            EffectsLore.add("");
            EffectsLore.add("§9§lBazowe obrażenia: " + damage);
        }
        if (TypeOfMold == ItemType.ARMOR) {
            if (this instanceof Craftable_Armor ca) {
                EffectsLore = new ArrayList<>(ca.getFormattedArmorLore());
                if (!ExistingEffects.isEmpty()) {
                    EffectsLore.add("");
                    EffectsLore.add("§9Wykute Runy:");
                    for (MagicEffects effects : ExistingEffects) {
                        EffectsLore.add("§8- " + effects.getDisplayName());
                    }
                }
            } else {
                EffectsLore.add("");
                EffectsLore.add("§9§lBazowa obrona: " + damage);
            }
        }
        MoldMeta.setLore(EffectsLore);
        NewMold.setItemMeta(MoldMeta);

        // Jeśli wytworzono przedmiot magiczny (Tomy, Różdżki, Laski), inicjalizujemy dane magii
        if (CraftIntoItem && TypeOfMold == ItemType.ITEM && (RPG.Magic.manager.MagicItemManager.isMagicItem(NewMold) || RPG.Crafting.CraftingMenager.isMagicMold(this))) {
            RPG.Magic.manager.MagicItemManager.initMagicItem(NewMold, weaponID.toLowerCase(java.util.Locale.ROOT));
        }

        // Jeśli wytworzono broń palną, inicjalizujemy dane broni i lore
        if (CraftIntoItem) {
            CustomContent.Guns.GunType gt = CustomContent.Guns.GunType.fromId(weaponID);
            if (gt != null) {
                CustomContent.Guns.GunData gd = new CustomContent.Guns.GunData(gt);
                gd.applyToItemStack(NewMold);
            }
        }

        player.getInventory().addItem(NewMold);

        // Powiadomienie systemu progresji o wytworzeniu przedmiotu
        if (CraftIntoItem && RPG.Progression.ProgressionManager.getInstance() != null) {
            var service = RPG.Progression.ProgressionManager.getInstance().getProgressionService();
            if (service != null) {
                service.handleObjective(player, RPG.Progression.model.ObjectiveType.CRAFT_ITEM, weaponID, 1);
                service.handleObjective(player, RPG.Progression.model.ObjectiveType.CRAFT_ITEM, weaponID.toLowerCase(java.util.Locale.ROOT), 1);
                service.handleObjective(player, RPG.Progression.model.ObjectiveType.CRAFT_ITEM, ItemMaterial.name(), 1);
                if (AmonPackPlugin.customItemManager != null) {
                    String customId = AmonPackPlugin.customItemManager.getCustomItemId(NewMold);
                    if (customId != null) {
                        service.handleObjective(player, RPG.Progression.model.ObjectiveType.CRAFT_ITEM, customId, 1);
                        service.handleObjective(player, RPG.Progression.model.ObjectiveType.CRAFT_ITEM, "custom:" + customId, 1);
                    }
                }
            }
        }
    }

    public ItemStack addEffectsToItem(ItemStack item, List<MagicEffects> effects) {
        ItemMeta meta = item.getItemMeta();
        NamespacedKey key = new NamespacedKey(AmonPackPlugin.plugin, "magic_effects");
        String serialized = MagicEffects.serializeList(effects);
        assert meta != null;
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, serialized);
        meta.getPersistentDataContainer().set(
                new NamespacedKey(AmonPackPlugin.plugin, "weapon_id"),
                PersistentDataType.STRING, weaponID

        );
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack toItemStack() {
        ItemStack item = new ItemStack(ItemMaterial);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (ItemName != null)
                meta.setDisplayName(ItemName);
            if (ItemLore != null && !ItemLore.isEmpty())
                meta.setLore(ItemLore);
            if (CustomModelID != null)
                meta.setCustomModelData(CustomModelID);
            meta.getPersistentDataContainer().set(
                    new NamespacedKey(AmonPackPlugin.plugin, "weapon_id"),
                    PersistentDataType.STRING,
                    weaponID);
            item.setItemMeta(meta);
        }
        return item;
    }

    public ItemStack to_Empty_Mold_ItemStack() {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.setCustomModelData(10007);
        if (meta != null) {
            if (ItemName != null)
                meta.setDisplayName(ChatColor.DARK_AQUA + "Forma: " + ChatColor.GOLD + ChatColor.BOLD + ItemName);
            meta.getPersistentDataContainer().set(
                    new NamespacedKey(AmonPackPlugin.plugin, "weapon_id"),
                    PersistentDataType.STRING,
                    weaponID);
            item.setItemMeta(meta);
        }
        return item;
    }

    public void ExecuteOnKillingByPlayer(Entity victim, ItemStack item, Player player, List<ItemStack> drops, int exp){
        ItemMeta meta = item.getItemMeta();
        NamespacedKey key = new NamespacedKey(AmonPackPlugin.plugin, "magic_effects");
        String data = meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        if (data != null && !data.isEmpty() && victim!=player) {
            for (MagicEffects effects : MagicEffects.deserializeList(data)){
                effects.ExecuteOnKilling(victim, player,drops, exp);
            }
        }
    }

    public String getWeaponID() {
        return weaponID;
    }

    public String getItemName() {
        return ItemName;
    }

    public Material getItemMaterial() {
        return ItemMaterial;
    }

    public Integer getCustomModelID() {
        return CustomModelID;
    }

    public List<ItemStack> getItemsRequiredToShapeMold() {
        return ItemsRequiredToShapeMold;
    }

    public List<String> getItemLore() {
        return ItemLore;
    }

    public List<MagicEffects> getAllowedMagicEffects() {
        return AllowedMagicEffects;
    }

    public ItemType getTypeOfMold() {
        return TypeOfMold;
    }

    private String requiredStage = null;
    private String requiredObjective = null;

    public String getRequiredStage() {
        return requiredStage;
    }

    public void setRequiredStage(String requiredStage) {
        this.requiredStage = requiredStage;
    }

    public String getRequiredObjective() {
        return requiredObjective;
    }

    public void setRequiredObjective(String requiredObjective) {
        this.requiredObjective = requiredObjective;
    }

    public boolean isUnlockedFor(Player player) {
        if (player == null) return true;
        if (player.getGameMode() == org.bukkit.GameMode.CREATIVE) return true;
        if (!Plugin.AmonPackPlugin.ENABLE_SLOW_PROGRESSION) return true;
        if (RPG.Progression.ProgressionManager.getInstance() == null || RPG.Progression.ProgressionManager.getInstance().getProgressionService() == null) return true;

        RPG.Progression.model.PlayerProgressionData data = RPG.Progression.ProgressionManager.getInstance().getProgressionService().getPlayerData(player);
        if (data == null) return true;

        if (requiredStage != null && !requiredStage.trim().isEmpty()) {
            RPG.Progression.model.StageType reqStage = RPG.Progression.model.StageType.fromName(requiredStage.trim());
            if (reqStage != null && !data.getCurrentStage().isAtLeast(reqStage)) {
                return false;
            }
        }

        if (requiredObjective != null && !requiredObjective.trim().isEmpty()) {
            if (!data.isQuestCompleted(requiredObjective.trim())) {
                return false;
            }
        }

        return true;
    }
}

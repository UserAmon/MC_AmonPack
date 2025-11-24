package AvatarSystems.Crafting.Objects;

import com.projectkorra.projectkorra.ability.Ability;
import com.projectkorra.projectkorra.ability.CoreAbility;
import methods_plugins.AmonPackPlugin;
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
        if (mold == null || !mold.hasItemMeta() || mold.getType() != Material.PAPER)
            return;
        List<MagicEffects> ExistingEffects = new ArrayList<>();
        List<String> EffectsLore = new ArrayList<>(ItemLore);
        NamespacedKey key = new NamespacedKey(AmonPackPlugin.plugin, "magic_effects");
        String data = Objects.requireNonNull(mold.getItemMeta()).getPersistentDataContainer().get(key,
                PersistentDataType.STRING);
        if (data != null && !data.isEmpty()) {
            ExistingEffects.addAll(MagicEffects.deserializeList(data));
        }
        player.getInventory().remove(mold);

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
            EffectsLore.add("");
            EffectsLore.add("§9§lBazowe obrona: " + damage);
        }
        MoldMeta.setLore(EffectsLore);
        NewMold.setItemMeta(MoldMeta);
        player.getInventory().addItem(NewMold);
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
        meta.setCustomModelData(10095);
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

    public String getWeaponID() {
        return weaponID;
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
}

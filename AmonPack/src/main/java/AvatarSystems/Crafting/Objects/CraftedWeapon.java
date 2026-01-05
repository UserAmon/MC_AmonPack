package AvatarSystems.Crafting.Objects;

import methods_plugins.AmonPackPlugin;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;


import java.util.List;
import java.util.UUID;

public class CraftedWeapon extends ItemMold {
    private double Damage;
    private boolean IsRange;

    public CraftedWeapon(String weaponID, List<ItemStack> itemsRequiredToShapeMold, String itemName, Material itemMaterial, List<String> itemLore, Integer customModelID, List<MagicEffects> allowedMagicEffects, double damage) {
        super(weaponID, itemsRequiredToShapeMold, itemName, itemMaterial, itemLore, customModelID, allowedMagicEffects,ItemType.WEAPON);
        Damage = damage;
        IsRange = false;
    }

    public double getDamage() {
        return Damage;
    }
    public boolean isRange() {
        return IsRange;
    }

    public double ExecuteEffectsOnHitByPlayer(Entity victim, ItemStack item, Player player){
        double Damage = 0;
        ItemMeta meta = item.getItemMeta();
        NamespacedKey key = new NamespacedKey(AmonPackPlugin.plugin, "magic_effects");
        String data = meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        if (data != null && !data.isEmpty()) {
            for (MagicEffects effects : MagicEffects.deserializeList(data)){
                Damage=Damage+effects.ExecuteOnHit(victim,player);
            }
        }
        return Damage;
    }
}

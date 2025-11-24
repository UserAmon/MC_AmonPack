package AvatarSystems.Crafting.Objects;

import methods_plugins.AmonPackPlugin;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class Craftable_Armor extends ItemMold {
    private double DmgReduction;

    public Craftable_Armor(String weaponID, List<ItemStack> itemsRequiredToShapeMold, String itemName, Material itemMaterial, List<String> itemLore, Integer customModelID, List<MagicEffects> allowedMagicEffects, double dmgReduction) {
        super(weaponID, itemsRequiredToShapeMold, itemName, itemMaterial, itemLore, customModelID, allowedMagicEffects, ItemType.ARMOR);
        DmgReduction = dmgReduction;
    }

    public double getDmgReduction() {
        return DmgReduction;
    }
    public double ExecutePlayerGetDamaged(Entity victim, ItemStack item, Player player){
        double DamageTaken = 0;
        ItemMeta meta = item.getItemMeta();
        NamespacedKey key = new NamespacedKey(AmonPackPlugin.plugin, "magic_effects");
        String data = meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        if (data != null && !data.isEmpty()) {
            for (MagicEffects effects : MagicEffects.deserializeList(data)){
                DamageTaken=DamageTaken+effects.ExecuteOnTakinHit(victim,player);
            }
        }
        return DamageTaken;
    }
}

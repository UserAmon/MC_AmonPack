package AvatarSystems.Crafting.Objects;

import methods_plugins.AmonPackPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

public class Craftable_Item extends ItemMold {

    public Craftable_Item(String weaponID, List<ItemStack> itemsRequiredToShapeMold, String itemName,
            Material itemMaterial, List<String> itemLore, Integer customModelID,
            List<MagicEffects> allowedMagicEffects) {
        super(weaponID, itemsRequiredToShapeMold, itemName, itemMaterial, itemLore, customModelID, allowedMagicEffects,
                ItemType.ITEM);
    }

    public void Use(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
        }

        List<MagicEffects> itemEffects = AvatarSystems.Crafting.CraftingMenager.getEffectsFromItem(item);
        if (itemEffects.isEmpty()) {
            return;
        }

        for (MagicEffects effect : itemEffects) {
            if (effect.isItemEffect()) {
                effect.ExecuteOnUse(player);
                player.sendMessage(ChatColor.GREEN + "Użyłeś przedmiotu: " + getDisplayName());
            }
        }
    }

    private String getDisplayName() {
        return super.toItemStack().getItemMeta().getDisplayName();
    }
}

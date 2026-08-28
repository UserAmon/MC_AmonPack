package RPG.Crafting.Objects;

import Plugin.AmonPackPlugin;
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
        if (item == null || item.getType() == Material.AIR) return;

        // Przedmioty magiczne (Tomy, Różdżki, Laski) nigdy nie są niszczone przy kliknięciu
        if (RPG.Magic.manager.MagicItemManager.isMagicItem(item)) {
            return;
        }

        List<MagicEffects> itemEffects = RPG.Crafting.CraftingMenager.getEffectsFromItem(item);
        if (itemEffects == null || itemEffects.isEmpty()) {
            return;
        }

        boolean hasItemEffect = false;
        for (MagicEffects effect : itemEffects) {
            if (effect != null && effect.isItemEffect()) {
                hasItemEffect = true;
                effect.ExecuteOnUse(player);
                player.sendMessage(ChatColor.GREEN + "Użyłeś przedmiotu: " + getDisplayName());
            }
        }

        // Usuwamy tylko jeśli przedmiot faktycznie wykonał efekt zużywalny
        if (hasItemEffect) {
            if (item.getAmount() > 1) {
                item.setAmount(item.getAmount() - 1);
            } else {
                player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
            }
        }
    }

    public String getDisplayName() {
        return super.toItemStack().getItemMeta().getDisplayName();
    }
}

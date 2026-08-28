package RPG.Crafting.Objects;

import Plugin.AmonPackPlugin;
import RPG.Crafting.CraftingMenager;
import RPG.Magic.model.SpellElement;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class CustomArmorManager {

    public static double getPlayerTotalArmor(Player player) {
        if (player == null) return 0.0;
        double total = 0.0;
        for (ItemStack item : player.getInventory().getArmorContents()) {
            if (item != null && item.hasItemMeta()) {
                if (CraftingMenager.IsArmor(item)) {
                    Craftable_Armor armor = CraftingMenager.GetCraftedArmorByItem(item);
                    if (armor != null) {
                        total += armor.getArmorValue();
                    }
                }
            }
        }
        return total;
    }

    public static double getPlayerManaReductionPercent(Player player, SpellElement element) {
        if (player == null) return 0.0;
        double total = 0.0;
        for (ItemStack item : player.getInventory().getArmorContents()) {
            if (item != null && item.hasItemMeta()) {
                if (CraftingMenager.IsArmor(item)) {
                    Craftable_Armor armor = CraftingMenager.GetCraftedArmorByItem(item);
                    if (armor != null && armor.appliesToElement(armor.getManaElement(), element)) {
                        total += armor.getManaReductionPercent();
                    }
                }
            }
        }
        return total;
    }

    public static double getPlayerManaReductionFlat(Player player, SpellElement element) {
        if (player == null) return 0.0;
        double total = 0.0;
        for (ItemStack item : player.getInventory().getArmorContents()) {
            if (item != null && item.hasItemMeta()) {
                if (CraftingMenager.IsArmor(item)) {
                    Craftable_Armor armor = CraftingMenager.GetCraftedArmorByItem(item);
                    if (armor != null && armor.appliesToElement(armor.getManaElement(), element)) {
                        total += armor.getManaReductionFlat();
                    }
                }
            }
        }
        return total;
    }

    public static double getPlayerCooldownReductionPercent(Player player, SpellElement element) {
        if (player == null) return 0.0;
        double total = 0.0;
        for (ItemStack item : player.getInventory().getArmorContents()) {
            if (item != null && item.hasItemMeta()) {
                if (CraftingMenager.IsArmor(item)) {
                    Craftable_Armor armor = CraftingMenager.GetCraftedArmorByItem(item);
                    if (armor != null && armor.appliesToElement(armor.getCdElement(), element)) {
                        total += armor.getCdReductionPercent();
                    }
                }
            }
        }
        return total;
    }

    public static double getPlayerCooldownReductionFlat(Player player, SpellElement element) {
        if (player == null) return 0.0;
        double total = 0.0;
        for (ItemStack item : player.getInventory().getArmorContents()) {
            if (item != null && item.hasItemMeta()) {
                if (CraftingMenager.IsArmor(item)) {
                    Craftable_Armor armor = CraftingMenager.GetCraftedArmorByItem(item);
                    if (armor != null && armor.appliesToElement(armor.getCdElement(), element)) {
                        total += armor.getCdReductionFlat();
                    }
                }
            }
        }
        return total;
    }

    public static double getPlayerSpeedIncreasePercent(Player player) {
        if (player == null) return 0.0;
        double total = 0.0;
        for (ItemStack item : player.getInventory().getArmorContents()) {
            if (item != null && item.hasItemMeta()) {
                if (CraftingMenager.IsArmor(item)) {
                    Craftable_Armor armor = CraftingMenager.GetCraftedArmorByItem(item);
                    if (armor != null) {
                        total += armor.getSpeedIncreasePercent();
                    }
                }
            }
        }
        return total;
    }

    public static void updatePlayerSpeed(Player player) {
        if (player == null || !player.isOnline()) return;
        double bonus = getPlayerSpeedIncreasePercent(player);
        float targetSpeed = (float) Math.min(1.0f, Math.max(0.05f, 0.2f * (1.0 + bonus)));
        if (Math.abs(player.getWalkSpeed() - targetSpeed) > 0.001f) {
            player.setWalkSpeed(targetSpeed);
        }
    }

    public static double calculateReducedDamage(Player player, double incomingDamage, boolean ignoreArmor) {
        if (ignoreArmor) {
            return Math.max(1.0, incomingDamage);
        }
        double totalArmor = getPlayerTotalArmor(player);
        double reduction = totalArmor / 3.0;
        return Math.max(1.0, incomingDamage - reduction);
    }
}

package Mechanics;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import AvatarSystems.Crafting.CraftingMenager;

public class ArmorEffectsRunnable extends BukkitRunnable {

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getFireTicks() > 10) {
                boolean hasPhoenixHeart = false;
                for (ItemStack item : player.getInventory().getArmorContents()) {
                    if (item != null && CraftingMenager.IsArmor(item)) {
                        if (CraftingMenager.HaveEffect(item, "PhoenixHeart")) {
                            hasPhoenixHeart = true;
                            break;
                        }
                    }
                }

                if (hasPhoenixHeart) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20, 2, false, false));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 20, 1, false, false));
                }
            }
        }
    }
}

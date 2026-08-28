package CustomContent.Guns;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;

public class GunListener implements Listener {

    private final GunManager gunManager;

    public GunListener(GunManager gunManager) {
        this.gunManager = gunManager;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        ItemStack mainHand = player.getInventory().getItemInMainHand();

        if (GunData.isGun(mainHand)) {
            // Blokada przeniesienia broni do drugiej ręki i wywołanie reloadu klawiszem F
            event.setCancelled(true);
            gunManager.startReload(player, mainHand);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!GunData.isGun(item)) return;

        Action action = event.getAction();
        if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
            // LPM = Wystrzał
            event.setCancelled(true);
            gunManager.handleLeftClick(player, item);
        } else if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            // PPM = Celowanie (ADS Zoom)
            gunManager.handleRightClick(player, item);
        }
    }

    @EventHandler
    public void onItemHeldChange(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        gunManager.stopAiming(player);
        gunManager.cancelReload(player);
    }

    @EventHandler
    public void onDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (GunData.isGun(event.getItemDrop().getItemStack())) {
            gunManager.stopAiming(player);
            gunManager.cancelReload(player);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onMeleeAttackWithBayonet(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            ItemStack item = player.getInventory().getItemInMainHand();
            if (GunData.isGun(item)) {
                GunData data = GunData.fromItemStack(item);
                if (data != null && data.hasBayonet()) {
                    // Dodatkowe obrażenia wręcz od bagnetu
                    event.setDamage(event.getDamage() + 7.0);
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.2f);
                }
            }
        }
    }
}

package CustomContent.Guns;

import Plugin.AmonPackPlugin;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CrossbowMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class GunListener implements Listener {

    private static final NamespacedKey KEY_GHOST_ARROW = new NamespacedKey(AmonPackPlugin.plugin, "ghost_arrow_flintlock");
    private final GunManager gunManager;

    public GunListener(GunManager gunManager) {
        this.gunManager = gunManager;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        ItemStack mainHand = player.getInventory().getItemInMainHand();

        if (GunData.isGun(mainHand)) {
            // Blokada przeniesienia broni do lewej ręki
            event.setCancelled(true);
            GunData data = GunData.fromItemStack(mainHand);
            if (data != null && data.getCurrentAmmo() < data.getMaxAmmoCapacity()) {
                gunManager.startReload(player, mainHand);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!GunData.isGun(item)) return;
        GunData data = GunData.fromItemStack(item);
        if (data == null) return;

        Action action = event.getAction();

        if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
            // LPM: Celowanie (ADS) lub szybki podgląd
            if (player.isSneaking()) {
                if (gunManager.isAiming(player)) {
                    gunManager.stopAiming(player);
                } else {
                    gunManager.startAiming(player, item);
                }
            } else {
                gunManager.handleLeftClick(player, item);
            }
        } else if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            // PPM: Ładowanie kuszy (jeśli rozładowana) lub celowanie
            if (data.getCurrentAmmo() <= 0) {
                AmmoType ammo = gunManager.findHighestPriorityAmmo(player, data.getGunType());
                if (ammo == null && player.getGameMode() != org.bukkit.GameMode.CREATIVE) {
                    event.setCancelled(true);
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§c❌ Brak kul w ekwipunku! Wymagana: " + data.getGunType().getRequiredAmmoType().getDisplayName()));
                    player.playSound(player.getLocation(), Sound.BLOCK_DISPENSER_FAIL, 1.0f, 1.5f);
                    return;
                }

                if (!gunManager.isReloading(player)) {
                    gunManager.startReload(player, item);
                }

                // Dodanie tymczasowej strzały do naciągnięcia kuszy
                if (!player.getInventory().contains(Material.ARROW) && player.getGameMode() != org.bukkit.GameMode.CREATIVE) {
                    ItemStack ghostArrow = new ItemStack(Material.ARROW, 1);
                    ItemMeta arrowMeta = ghostArrow.getItemMeta();
                    if (arrowMeta != null) {
                        arrowMeta.getPersistentDataContainer().set(KEY_GHOST_ARROW, PersistentDataType.BYTE, (byte) 1);
                        arrowMeta.setDisplayName("§8[Kula Pistoletowa]");
                        ghostArrow.setItemMeta(arrowMeta);
                    }
                    player.getInventory().addItem(ghostArrow);
                }
            } else {
                if (player.isSneaking() && !gunManager.isAiming(player)) {
                    gunManager.startAiming(player, item);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onShootBow(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        ItemStack bow = event.getBow();

        if (GunData.isGun(bow)) {
            event.setCancelled(true);
            if (event.getProjectile() != null) {
                event.getProjectile().remove();
            }

            cleanGhostArrows(player);

            GunData data = GunData.fromItemStack(bow);
            if (data != null && data.getCurrentAmmo() > 0) {
                gunManager.fireGun(player, bow, data);
            }
        }
    }

    private void cleanGhostArrows(Player player) {
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack != null && stack.getType() == Material.ARROW && stack.hasItemMeta()) {
                if (stack.getItemMeta().getPersistentDataContainer().has(KEY_GHOST_ARROW, PersistentDataType.BYTE)) {
                    player.getInventory().setItem(i, null);
                }
            }
        }
    }

    @EventHandler
    public void onItemHeldChange(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        gunManager.stopAiming(player);
        gunManager.cancelReload(player);
        cleanGhostArrows(player);
    }

    @EventHandler
    public void onDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        cleanGhostArrows(player);
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
                    event.setDamage(event.getDamage() + 7.0);
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.2f);
                }
            }
        }
    }
}

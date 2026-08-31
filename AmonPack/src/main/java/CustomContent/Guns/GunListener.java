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
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

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
            // Jeśli broń jest naładowana i gotowa do strzału:
            if (data.getCurrentAmmo() > 0) {
                if (player.isSneaking() && !gunManager.isAiming(player)) {
                    gunManager.startAiming(player, item);
                    return;
                }
                // Natychmiastowy wystrzał przy kliknięciu PPM (np. szybka seria z Pieprzniczki / Dubeltówki)
                gunManager.fireGun(player, item, data);
                return;
            }

            // Jeśli broń jest rozładowana – rozpoczynamy ładowanie
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

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDeath(org.bukkit.event.entity.EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        ItemStack hand = killer.getInventory().getItemInMainHand();
        if (!GunData.isGun(hand)) return;

        GunData data = GunData.fromItemStack(hand);
        if (data == null) return;

        // Pistolet: Łowca Czarownic - 2x EXP, podwójny drop i szansa na amunicję
        if (data.getUniqueMod() == GunUniqueMod.PISTOL_WITCH_HUNTER) {
            double expMult = GunConfigManager.getInstance().getUniqueDouble("pistol_witch_hunter", "exp_multiplier", 2.0);
            event.setDroppedExp((int) Math.round(event.getDroppedExp() * expMult));

            List<ItemStack> extraDrops = new ArrayList<>();
            for (ItemStack drop : event.getDrops()) {
                if (drop != null) extraDrops.add(drop.clone());
            }
            event.getDrops().addAll(extraDrops);

            double ammoChance = GunConfigManager.getInstance().getUniqueDouble("pistol_witch_hunter", "ammo_drop_chance", 0.35);
            if (Math.random() < ammoChance) {
                ItemStack ammoStack = new ItemStack(Material.IRON_NUGGET, 1 + (int) (Math.random() * 2));
                ItemMeta meta = ammoStack.getItemMeta();
                if (meta != null) {
                    meta.setCustomModelData(10060);
                    meta.setDisplayName("§fOłowiana Kula Muszkietowa");
                    ammoStack.setItemMeta(meta);
                }
                event.getDrops().add(ammoStack);
                killer.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§5✦ §l[Łowca Czarownic] §aZdobyto dodatkowy łup i amunicję!"));
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDamageBreakStalker(org.bukkit.event.entity.EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            ItemStack hand = player.getInventory().getItemInMainHand();
            if (GunData.isGun(hand)) {
                GunData data = GunData.fromItemStack(hand);
                if (data != null && data.getUniqueMod() == GunUniqueMod.MUSKET_STALKER) {
                    player.removePotionEffect(org.bukkit.potion.PotionEffectType.INVISIBILITY);
                }
            }
        }
    }
}

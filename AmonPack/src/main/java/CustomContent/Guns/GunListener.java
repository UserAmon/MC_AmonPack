package CustomContent.Guns;

import Plugin.AmonPackPlugin;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import CustomContent.Hooks.ItemsAdderHook;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.RayTraceResult;

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
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

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
            // Jeśli gracz klika na półkę (shelf/furniture) – nie strzelaj, pozwól odłożyć broń na półkę
            if (isShelfInteraction(player, event.getClickedBlock())) {
                if (player.getGameMode() == GameMode.CREATIVE) {
                    final ItemStack copy = item.clone();
                    final int heldSlot = player.getInventory().getHeldItemSlot();
                    Bukkit.getScheduler().runTask(AmonPackPlugin.plugin, () -> {
                        ItemStack current = player.getInventory().getItem(heldSlot);
                        if (current == null || current.getType() == Material.AIR || !GunData.isGun(current)) {
                            player.getInventory().setItem(heldSlot, copy);
                            player.updateInventory();
                        }
                    });
                    Bukkit.getScheduler().runTaskLater(AmonPackPlugin.plugin, () -> {
                        ItemStack current = player.getInventory().getItem(heldSlot);
                        if (current == null || current.getType() == Material.AIR || !GunData.isGun(current)) {
                            player.getInventory().setItem(heldSlot, copy);
                            player.updateInventory();
                        }
                    }, 2L);
                }
                return;
            }

            long now = System.currentTimeMillis();
            long last = gunManager.getLastShotTime(player.getUniqueId());
            long minInterval = gunManager.getCooldownMs(data.getGunType());
            if (now - last < minInterval || player.hasCooldown(item.getType())) {
                event.setCancelled(true);
                return;
            }

            // Jeśli broń jest naładowana i gotowa do strzału:
            if (data.getCurrentAmmo() > 0) {
                event.setCancelled(true);
                if (player.isSneaking() && !gunManager.isAiming(player)) {
                    gunManager.startAiming(player, item);
                    return;
                }
                // Natychmiastowy wystrzał przy kliknięciu PPM
                gunManager.fireGun(player, item, data);
                ItemStack updatedHand = player.getInventory().getItemInMainHand();
                if (GunData.isGun(updatedHand)) {
                    player.getInventory().setItemInMainHand(updatedHand);
                    player.updateInventory();
                }
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
            event.setConsumeItem(false);
            if (event.getProjectile() != null) {
                event.getProjectile().remove();
            }

            cleanGhostArrows(player);
        }
    }

    public static boolean isGhostArrow(ItemStack item) {
        if (item == null || item.getType() != Material.ARROW || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(KEY_GHOST_ARROW, PersistentDataType.BYTE);
    }

    public static int countRealArrows(Player player) {
        int count = 0;
        for (ItemStack is : player.getInventory().getContents()) {
            if (is != null && is.getType() == Material.ARROW) {
                if (!isGhostArrow(is)) {
                    count += is.getAmount();
                }
            }
        }
        return count;
    }

    public static void cleanGhostArrows(Player player) {
        boolean cleaned = false;
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (isGhostArrow(stack)) {
                player.getInventory().setItem(i, null);
                cleaned = true;
            }
        }
        if (cleaned) {
            player.updateInventory();
        }
    }

    public static boolean isShelfInteraction(Player player, Block clickedBlock) {
        if (clickedBlock != null && isShelfBlock(clickedBlock)) {
            return true;
        }

        // Sprawdzenie celowania wzrokiem w mebel/półkę w zasięgu interakcji (do 4.5 bloku)
        RayTraceResult rayTrace = player.getWorld().rayTrace(
                player.getEyeLocation(),
                player.getEyeLocation().getDirection(),
                4.5,
                FluidCollisionMode.NEVER,
                true,
                0.3,
                e -> !e.equals(player)
        );
        if (rayTrace != null) {
            if (rayTrace.getHitBlock() != null && isShelfBlock(rayTrace.getHitBlock())) {
                return true;
            }
            if (rayTrace.getHitEntity() != null && isShelfEntity(rayTrace.getHitEntity())) {
                return true;
            }
        }
        return false;
    }

    public static boolean isShelfBlock(Block block) {
        if (block == null) return false;
        Material mat = block.getType();
        if (mat == Material.CHISELED_BOOKSHELF || mat == Material.BOOKSHELF || mat.name().contains("SHELF")) {
            return true;
        }
        return ItemsAdderHook.isShelfOrFurniture(block);
    }

    public static boolean isShelfEntity(Entity entity) {
        if (entity == null) return false;
        if (entity instanceof ItemFrame || entity instanceof ArmorStand
                || entity.getType().name().contains("FRAME")
                || entity.getType().name().contains("STAND")
                || entity.getType().name().contains("INTERACTION")
                || entity.getType().name().contains("DISPLAY")) {
            return true;
        }
        return ItemsAdderHook.isShelfOrFurniture(entity);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        handleShelfEntityInteraction(event.getPlayer(), event.getRightClicked(), event.getHand());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
        handleShelfEntityInteraction(event.getPlayer(), event.getRightClicked(), event.getHand());
    }

    private void handleShelfEntityInteraction(Player player, Entity entity, EquipmentSlot hand) {
        if (hand != EquipmentSlot.HAND) return;
        if (player.getGameMode() != GameMode.CREATIVE) return;
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!GunData.isGun(item)) return;

        if (isShelfEntity(entity)) {
            final ItemStack copy = item.clone();
            final int heldSlot = player.getInventory().getHeldItemSlot();
            Bukkit.getScheduler().runTask(AmonPackPlugin.plugin, () -> {
                ItemStack current = player.getInventory().getItem(heldSlot);
                if (current == null || current.getType() == Material.AIR || !GunData.isGun(current)) {
                    player.getInventory().setItem(heldSlot, copy);
                    player.updateInventory();
                }
            });
            Bukkit.getScheduler().runTaskLater(AmonPackPlugin.plugin, () -> {
                ItemStack current = player.getInventory().getItem(heldSlot);
                if (current == null || current.getType() == Material.AIR || !GunData.isGun(current)) {
                    player.getInventory().setItem(heldSlot, copy);
                    player.updateInventory();
                }
            }, 2L);
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

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDeath(org.bukkit.event.entity.EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        ItemStack hand = killer.getInventory().getItemInMainHand();
        if (!GunData.isGun(hand)) return;

        GunData data = GunData.fromItemStack(hand);
        if (data == null) return;

        // Strzelba: Ergonomiczne Łoże - zabójstwa dają graczowi efekt "Fachu" na 5s (-0.5s przeładowania strzelb)
        if (data.getGunType() == GunType.BLUNDERBUSS && data.hasBayonet()) {
            gunManager.grantShotgunFach(killer);
            killer.playSound(killer.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.6f);
            killer.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§e⚙ §l[FACH] §aSzybkie przeładowanie strzelb (-0.5s) przez 5s!"));
        }

        // Pistolet: Punisher - zabójstwa dają graczowi efekt Speed I na 3 sekundy
        if (data.getGunType() == GunType.FLINTLOCK_PISTOL && data.getUniqueMod() == GunUniqueMod.PISTOL_PUNISHER) {
            killer.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SPEED, 60, 0, false, false, true));
            killer.playSound(killer.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 1.8f);
            killer.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§c⚡ §l[PUNISHER] §bZabójstwo! Zwiększona prędkość ruchu na 3s!"));
        }

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

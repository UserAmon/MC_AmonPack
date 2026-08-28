package CustomContent.Guns;

import Plugin.AmonPackPlugin;
import RPG.Progression.ProgressionManager;
import RPG.Progression.model.ObjectiveType;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GunManager {

    private final Set<UUID> aimingPlayers = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Map<UUID, BukkitTask> activeReloads = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastShotTime = new ConcurrentHashMap<>();
    private final Random random = new Random();

    public void handleRightClick(Player player, ItemStack gunItem) {
        if (!GunData.isGun(gunItem)) return;
        // Trzymanie PPM = Mocny Zoom ADS
        startAiming(player, gunItem);
    }

    public void handleLeftClick(Player player, ItemStack gunItem) {
        if (!GunData.isGun(gunItem)) return;
        GunData data = GunData.fromItemStack(gunItem);
        if (data == null) return;

        // Sprawdzenie cooldownu strzału (0.25s - 0.45s)
        long now = System.currentTimeMillis();
        long last = lastShotTime.getOrDefault(player.getUniqueId(), 0L);
        long cooldownMs = data.getGunType() == GunType.PEPPERBOX ? 250L : 450L;
        if (now - last < cooldownMs) {
            return;
        }

        // Sprawdzenie czy gracz właśnie przeładowuje
        if (isReloading(player)) {
            player.sendMessage("§cBroń jest w trakcie przeładowywania!");
            return;
        }

        // Sprawdzenie trwałości
        if (data.getCurrentDurability() <= 0) {
            player.playSound(player.getLocation(), Sound.ITEM_SHIELD_BREAK, 1.0f, 0.8f);
            player.sendMessage("§c❌ Ta broń jest zniszczona! Napraw ją w Stole Rusznikarskim.");
            return;
        }

        // Sprawdzenie amunicji w komorze
        if (data.getCurrentAmmo() <= 0) {
            player.playSound(player.getLocation(), Sound.BLOCK_DISPENSER_FAIL, 1.0f, 1.4f);
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§c⚠ Brak kul w komorze! Naciśnij [F] aby załadować."));
            // Automatyczny start reloadu
            startReload(player, gunItem);
            return;
        }

        lastShotTime.put(player.getUniqueId(), now);
        fireGun(player, gunItem, data);
    }

    private void fireGun(Player player, ItemStack gunItem, GunData data) {
        Location eyeLoc = player.getEyeLocation();
        Vector dir = eyeLoc.getDirection().normalize();
        World world = player.getWorld();

        AmmoType ammo = data.getLoadedAmmoType();
        if (ammo == null) {
            ammo = data.getGunType().getRequiredAmmoType();
        }

        boolean isDragon = (ammo == AmmoType.DRAGON_CARTRIDGE);
        boolean isSlug = (ammo == AmmoType.SLUG_CARTRIDGE);

        // 1. Dźwięki i dym wystrzału z lufy
        Location muzzleLoc = eyeLoc.clone().add(dir.clone().multiply(0.8)).add(0, -0.15, 0);

        float soundPitch = data.getGunType() == GunType.BLUNDERBUSS ? (isSlug ? 0.7f : 0.9f) :
                data.getGunType() == GunType.FLINTLOCK_MUSKET ? 1.1f : 1.4f;

        world.playSound(eyeLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, soundPitch);
        world.playSound(eyeLoc, Sound.ITEM_FLINTANDSTEEL_USE, 1.0f, 0.6f);
        if (isSlug) {
            world.playSound(eyeLoc, Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 1.0f, 0.8f);
        }

        world.spawnParticle(Particle.FLAME, muzzleLoc, isDragon ? 25 : 12, 0.15, 0.15, 0.15, 0.05);
        world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, muzzleLoc, isSlug ? 28 : 18, 0.25, 0.25, 0.25, 0.05);
        world.spawnParticle(Particle.SMOKE, muzzleLoc, 25, 0.3, 0.3, 0.3, 0.08);

        // 2. Liczba pocisków
        // Garłacz strzela 8 śrucinami tylko gdy załadowany jest śrut lub smoczy oddech; slug to 1 potężny pocisk
        int bulletCount = (data.getGunType() == GunType.BLUNDERBUSS && !isSlug) ? 8 : 1;
        boolean isAiming = isAiming(player);
        double baseSpread = data.getSpread();

        if (isAiming) baseSpread *= 0.30; // 70% redukcja rozrzutu przy ADS!
        if (player.isSprinting()) baseSpread *= 1.60;
        if (player.isSneaking()) baseSpread *= 0.75;

        for (int i = 0; i < bulletCount; i++) {
            Vector spreadDir = dir.clone();
            if (baseSpread > 0.001) {
                spreadDir.add(new Vector(
                        (random.nextDouble() - 0.5) * baseSpread,
                        (random.nextDouble() - 0.5) * baseSpread,
                        (random.nextDouble() - 0.5) * baseSpread
                )).normalize();
            }
            simulateBullet(player, eyeLoc, spreadDir, data, ammo);
        }

        // 3. Odrzut kamery i fizyczny odrzut gracza w tył
        applyRecoil(player, data.getGunType(), isSlug);

        // 4. Zużycie amunicji i trwałości
        data.setCurrentAmmo(data.getCurrentAmmo() - 1);
        if (player.getGameMode() != GameMode.CREATIVE) {
            data.setCurrentDurability(data.getCurrentDurability() - 1);
        }
        data.applyToItemStack(gunItem);

        // 5. Powiadomienie progresji
        if (ProgressionManager.getInstance() != null && ProgressionManager.getInstance().getProgressionService() != null) {
            ProgressionManager.getInstance().getProgressionService().handleObjective(player, ObjectiveType.USE_ITEM, data.getGunType().getId(), 1);
        }
    }

    private void simulateBullet(Player shooter, Location startLoc, Vector initialVelocity, GunData data, AmmoType ammo) {
        World world = startLoc.getWorld();
        double speed = (ammo == AmmoType.SLUG_CARTRIDGE) ? 3.4 : 3.0; // 60-68 m/s
        Vector velocity = initialVelocity.clone().multiply(speed);
        Location currentLoc = startLoc.clone();
        final double maxRange = data.getEffectiveRange();
        final boolean isDragon = (ammo == AmmoType.DRAGON_CARTRIDGE);
        final boolean isSlug = (ammo == AmmoType.SLUG_CARTRIDGE);

        new BukkitRunnable() {
            private double distanceTraveled = 0.0;

            @Override
            public void run() {
                for (int step = 0; step < 3; step++) {
                    Vector stepMovement = velocity.clone().multiply(0.333);
                    RayTraceResult hit = world.rayTrace(currentLoc, stepMovement.normalize(), stepMovement.length(),
                            FluidCollisionMode.NEVER, true, 0.35, entity -> entity != shooter && entity instanceof LivingEntity);

                    if (hit != null) {
                        Location hitPos = hit.getHitPosition().toLocation(world);
                        if (hit.getHitEntity() instanceof LivingEntity victim) {
                            handleHitEntity(shooter, victim, hitPos, data, ammo);
                        } else if (hit.getHitBlock() != null) {
                            handleHitBlock(hitPos, isDragon);
                        }
                        cancel();
                        return;
                    }

                    currentLoc.add(stepMovement);
                    distanceTraveled += stepMovement.length();

                    // Efekty smugi pocisku
                    if (isDragon) {
                        world.spawnParticle(Particle.FLAME, currentLoc, 2, 0.02, 0.02, 0.02, 0.01);
                        world.spawnParticle(Particle.SMOKE, currentLoc, 1, 0, 0, 0, 0);
                    } else if (isSlug) {
                        world.spawnParticle(Particle.CRIT, currentLoc, 2, 0.03, 0.03, 0.03, 0.02);
                        world.spawnParticle(Particle.SMOKE, currentLoc, 1, 0, 0, 0, 0);
                    } else {
                        world.spawnParticle(Particle.CRIT, currentLoc, 1, 0, 0, 0, 0);
                    }

                    // Grawitacja
                    velocity.subtract(new Vector(0, 0.012, 0));

                    if (distanceTraveled >= maxRange || currentLoc.getY() < -64 || currentLoc.getY() > 320) {
                        cancel();
                        return;
                    }
                }
            }
        }.runTaskTimer(AmonPackPlugin.plugin, 1L, 1L);
    }

    private void handleHitEntity(Player shooter, LivingEntity victim, Location hitLoc, GunData data, AmmoType ammo) {
        World world = victim.getWorld();
        boolean isHeadshot = hitLoc.getY() >= (victim.getEyeLocation().getY() - 0.25);
        boolean isDragon = (ammo == AmmoType.DRAGON_CARTRIDGE);
        boolean isSlug = (ammo == AmmoType.SLUG_CARTRIDGE);

        double damage = data.getDamage();
        if (isDragon) {
            damage += 3.0; // Bonus ognia
        }

        if (isHeadshot) {
            damage *= data.getHeadshotMultiplier();
            world.playSound(hitLoc, Sound.ENTITY_ARROW_HIT_PLAYER, 1.2f, 1.6f);
            world.playSound(hitLoc, Sound.BLOCK_ANVIL_LAND, 0.6f, 1.8f);
            world.spawnParticle(Particle.CRIT, hitLoc, 15, 0.2, 0.2, 0.2, 0.2);
            shooter.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§c🎯 §lTRAFIENIE W GŁOWĘ! §e(" + String.format(Locale.ROOT, "%.1f", damage) + " DMG)"));
        } else {
            world.playSound(hitLoc, Sound.ENTITY_ARROW_HIT, 1.0f, 1.2f);
            world.spawnParticle(Particle.DAMAGE_INDICATOR, hitLoc, 6, 0.2, 0.2, 0.2, 0.1);
        }

        // Zadanie obrażeń
        victim.damage(damage, shooter);

        // Odrzut dla Garłacza (POZIOMY BEZ PODRZUCANIA W GÓRĘ)
        if (data.getGunType() == GunType.BLUNDERBUSS) {
            Vector kb = victim.getLocation().toVector().subtract(shooter.getLocation().toVector());
            kb.setY(0);
            double kbStrength = isSlug ? 1.25 : 0.85;
            if (kb.lengthSquared() > 0.001) {
                kb.normalize().multiply(kbStrength);
            }
            victim.setVelocity(new Vector(kb.getX(), 0.0, kb.getZ()));
        }

        // Efekt smoczego oddechu (Podpalenie na 6 sekund i rozbłysk płomieni)
        if (isDragon) {
            victim.setFireTicks(120);
            world.spawnParticle(Particle.LAVA, hitLoc, 10, 0.3, 0.3, 0.3, 0.1);
            world.spawnParticle(Particle.FLAME, hitLoc, 15, 0.4, 0.3, 0.4, 0.05);
        }

        // Zaliczenie headshota do progresji
        if (isHeadshot && ProgressionManager.getInstance() != null && ProgressionManager.getInstance().getProgressionService() != null) {
            ProgressionManager.getInstance().getProgressionService().handleObjective(shooter, ObjectiveType.KILL_ENTITY, "HEADSHOT", 1);
        }
    }

    private void handleHitBlock(Location hitLoc, boolean isDragon) {
        World world = hitLoc.getWorld();
        world.playSound(hitLoc, Sound.BLOCK_STONE_HIT, 1.0f, 1.2f);
        world.spawnParticle(Particle.BLOCK, hitLoc, 15, 0.2, 0.2, 0.2, 0.1, Material.STONE.createBlockData());

        if (isDragon) {
            world.spawnParticle(Particle.FLAME, hitLoc, 16, 0.4, 0.2, 0.4, 0.05);
            if (hitLoc.getBlock().getType() == Material.AIR) {
                hitLoc.getBlock().setType(Material.FIRE);
            }
        }
    }

    private void applyRecoil(Player player, GunType type, boolean isSlug) {
        float pitchKick = (type == GunType.BLUNDERBUSS) ? (isSlug ? -6.0f : -5.0f) :
                type == GunType.FLINTLOCK_MUSKET ? -3.8f : -2.2f;
        float yawKick = (random.nextFloat() - 0.5f) * 1.6f;

        Location loc = player.getLocation();
        loc.setPitch(Math.max(-90.0f, loc.getPitch() + pitchKick));
        loc.setYaw(loc.getYaw() + yawKick);
        player.teleport(loc);

        // Odrzut fizyczny gracza w tył (zauważalny dla strzelby)
        double backForce = (type == GunType.BLUNDERBUSS) ? (isSlug ? -0.36 : -0.25) :
                type == GunType.FLINTLOCK_MUSKET ? -0.14 : -0.09;

        Vector back = player.getLocation().getDirection().setY(0).normalize().multiply(backForce);
        player.setVelocity(player.getVelocity().add(back));
    }

    public void startReload(Player player, ItemStack gunItem) {
        if (!GunData.isGun(gunItem)) return;
        GunData data = GunData.fromItemStack(gunItem);
        if (data == null) return;

        if (data.getCurrentAmmo() >= data.getGunType().getMaxAmmo()) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§aBroń jest już w pełni załadowana!"));
            return;
        }

        if (isReloading(player)) {
            return;
        }

        // Wyszukanie amunicji o najwyższym priorytecie (najniższy numer slotu w Hotbarze 0..8, potem EQ 9..35, potem Offhand)
        AmmoType selectedAmmo = findHighestPriorityAmmo(player, data.getGunType());
        if (selectedAmmo == null && player.getGameMode() != GameMode.CREATIVE) {
            player.playSound(player.getLocation(), Sound.BLOCK_CHEST_LOCKED, 1.0f, 1.5f);
            player.sendMessage("§c❌ Brak kompatybilnej amunicji w Twoim ekwipunku!");
            return;
        }
        if (selectedAmmo == null) {
            selectedAmmo = data.getGunType().getRequiredAmmoType();
        }

        final AmmoType chosenAmmo = selectedAmmo;
        data.setLoadedAmmoType(chosenAmmo);

        int totalTicks = data.getReloadTicks();
        UUID uuid = player.getUniqueId();

        BukkitTask task = new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                // Anulowanie jeśli gracz zmienił trzymany przedmiot lub zamknął grę
                ItemStack currentHand = player.getInventory().getItemInMainHand();
                if (!GunData.isGun(currentHand) || !player.isOnline()) {
                    cancelReload(player);
                    return;
                }

                tick++;
                double progress = (double) tick / totalTicks;
                int barBlocks = (int) (progress * 10);
                StringBuilder bar = new StringBuilder("§e[");
                for (int b = 0; b < 10; b++) {
                    if (b < barBlocks) bar.append("§a■");
                    else bar.append("§7□");
                }
                bar.append("§e] §fŁadowanie: ").append(chosenAmmo.getDisplayName());
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(bar.toString()));

                // Dźwięki fazowe
                if (tick == 1) {
                    player.playSound(player.getLocation(), Sound.BLOCK_SAND_PLACE, 1.0f, 1.1f);
                } else if (tick == totalTicks / 2) {
                    player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_IRON, 1.0f, 1.2f);
                } else if (tick == (int) (totalTicks * 0.85)) {
                    player.playSound(player.getLocation(), Sound.BLOCK_LEVER_CLICK, 1.0f, 1.4f);
                }

                if (tick >= totalTicks) {
                    finishReload(player, currentHand, data, chosenAmmo);
                    cancel();
                }
            }
        }.runTaskTimer(AmonPackPlugin.plugin, 1L, 1L);

        activeReloads.put(uuid, task);
    }

    private void finishReload(Player player, ItemStack gunItem, GunData data, AmmoType ammoToLoad) {
        activeReloads.remove(player.getUniqueId());

        if (player.getGameMode() != GameMode.CREATIVE) {
            int needed = data.getGunType().getMaxAmmo() - data.getCurrentAmmo();
            int consumed = consumeAmmoByPriority(player, ammoToLoad, needed);
            if (consumed <= 0) return;
            data.setCurrentAmmo(data.getCurrentAmmo() + consumed);
        } else {
            data.setCurrentAmmo(data.getGunType().getMaxAmmo());
        }

        data.setLoadedAmmoType(ammoToLoad);
        data.applyToItemStack(gunItem);

        player.playSound(player.getLocation(), Sound.ITEM_FLINTANDSTEEL_USE, 1.0f, 1.4f);
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§a✔ Załadowano " + ammoToLoad.getDisplayName() + " §f(" + data.getCurrentAmmo() + "/" + data.getGunType().getMaxAmmo() + ")"));
    }

    public boolean isReloading(Player player) {
        return activeReloads.containsKey(player.getUniqueId());
    }

    public void cancelReload(Player player) {
        BukkitTask task = activeReloads.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§cPrzeładowanie przerwane!"));
        }
    }

    public void startAiming(Player player, ItemStack gunItem) {
        aimingPlayers.add(player.getUniqueId());
        GunData data = GunData.fromItemStack(gunItem);
        // Slowness V (amplifier 4) lub z lunetą Slowness VIII (amplifier 7) dla BARDZO MOCNEGO ZOOMA!
        int amplifier = (data != null && data.hasBrassScope()) ? 7 : 4;
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 14, amplifier, false, false, false));
    }

    public void stopAiming(Player player) {
        if (aimingPlayers.remove(player.getUniqueId())) {
            player.removePotionEffect(PotionEffectType.SLOWNESS);
        }
    }

    public boolean isAiming(Player player) {
        return aimingPlayers.contains(player.getUniqueId());
    }

    public AmmoType findHighestPriorityAmmo(Player player, GunType gunType) {
        // 1. Hotbar (sloty 0..8)
        for (int slot = 0; slot <= 8; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            AmmoType at = getAmmoTypeFromStack(stack);
            if (at != null && gunType.isCompatibleAmmo(at)) {
                return at;
            }
        }
        // 2. Główny ekwipunek (sloty 9..35)
        for (int slot = 9; slot <= 35; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            AmmoType at = getAmmoTypeFromStack(stack);
            if (at != null && gunType.isCompatibleAmmo(at)) {
                return at;
            }
        }
        // 3. Druga ręka (Offhand)
        ItemStack offhand = player.getInventory().getItemInOffHand();
        AmmoType atOff = getAmmoTypeFromStack(offhand);
        if (atOff != null && gunType.isCompatibleAmmo(atOff)) {
            return atOff;
        }

        return null;
    }

    public int consumeAmmoByPriority(Player player, AmmoType type, int maxToConsume) {
        int remaining = maxToConsume;
        // Hotbar 0..8
        for (int slot = 0; slot <= 8; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (getAmmoTypeFromStack(stack) == type) {
                int take = Math.min(stack.getAmount(), remaining);
                stack.setAmount(stack.getAmount() - take);
                if (stack.getAmount() <= 0) player.getInventory().setItem(slot, null);
                remaining -= take;
                if (remaining <= 0) return maxToConsume;
            }
        }
        // Inventory 9..35
        for (int slot = 9; slot <= 35; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (getAmmoTypeFromStack(stack) == type) {
                int take = Math.min(stack.getAmount(), remaining);
                stack.setAmount(stack.getAmount() - take);
                if (stack.getAmount() <= 0) player.getInventory().setItem(slot, null);
                remaining -= take;
                if (remaining <= 0) return maxToConsume;
            }
        }
        // Offhand
        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (getAmmoTypeFromStack(offhand) == type) {
            int take = Math.min(offhand.getAmount(), remaining);
            offhand.setAmount(offhand.getAmount() - take);
            if (offhand.getAmount() <= 0) player.getInventory().setItemInOffHand(null);
            remaining -= take;
        }
        return maxToConsume - remaining;
    }

    public AmmoType getAmmoTypeFromStack(ItemStack stack) {
        if (stack == null || stack.getType() != Material.IRON_NUGGET || !stack.hasItemMeta()) return null;
        ItemMeta meta = stack.getItemMeta();
        if (meta.hasCustomModelData()) {
            int cmd = meta.getCustomModelData();
            if (cmd == AmmoType.LEAD_BULLET.getCustomModelData()) return AmmoType.LEAD_BULLET;
            if (cmd == AmmoType.SCATTER_SHOT.getCustomModelData()) return AmmoType.SCATTER_SHOT;
            if (cmd == AmmoType.DRAGON_CARTRIDGE.getCustomModelData()) return AmmoType.DRAGON_CARTRIDGE;
            if (cmd == AmmoType.SLUG_CARTRIDGE.getCustomModelData()) return AmmoType.SLUG_CARTRIDGE;
        }
        if (meta.hasDisplayName()) {
            String name = meta.getDisplayName().toLowerCase(Locale.ROOT);
            if (name.contains("slug") || name.contains("brenek")) return AmmoType.SLUG_CARTRIDGE;
            if (name.contains("dragon") || name.contains("zapalając") || name.contains("smocz")) return AmmoType.DRAGON_CARTRIDGE;
            if (name.contains("scatter") || name.contains("śrut")) return AmmoType.SCATTER_SHOT;
            if (name.contains("lead") || name.contains("ołowian")) return AmmoType.LEAD_BULLET;
        }
        return null;
    }
}

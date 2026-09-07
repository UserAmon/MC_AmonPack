package CustomContent.Guns;

import Plugin.AmonPackPlugin;
import RPG.Progression.ProgressionManager;
import RPG.Progression.model.ObjectiveType;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CrossbowMeta;
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
    private final Set<UUID> scopedPlayers = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Map<UUID, BukkitTask> activeReloads = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> activeReloadRealArrows = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastShotTime = new ConcurrentHashMap<>();
    private final Map<UUID, Location> lastPlayerLoc = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> stationaryTicks = new ConcurrentHashMap<>();
    private final Map<UUID, Long> exhaustedEnemies = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastStalkerInvisTime = new ConcurrentHashMap<>();
    private final Map<UUID, Long> shotgunFachBuff = new ConcurrentHashMap<>();
    private final Random random = new Random();

    public GunManager() {
        startStalkerTask();
    }

    private void startStalkerTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    ItemStack hand = player.getInventory().getItemInMainHand();
                    if (!GunData.isGun(hand)) {
                        stationaryTicks.remove(player.getUniqueId());
                        continue;
                    }

                    GunData data = GunData.fromItemStack(hand);
                    if (data == null || data.getUniqueMod() != GunUniqueMod.MUSKET_STALKER || data.getCurrentAmmo() <= 0) {
                        stationaryTicks.remove(player.getUniqueId());
                        continue;
                    }

                    UUID uuid = player.getUniqueId();
                    if (player.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
                        lastStalkerInvisTime.put(uuid, System.currentTimeMillis());
                    }

                    Location currentLoc = player.getLocation();
                    Location lastLoc = lastPlayerLoc.get(uuid);

                    if (lastLoc != null && currentLoc.getWorld() == lastLoc.getWorld() && currentLoc.distanceSquared(lastLoc) < 0.04) {
                        if (isNearFoliage(currentLoc, 2.5)) {
                            int ticks = stationaryTicks.getOrDefault(uuid, 0) + 5;
                            stationaryTicks.put(uuid, ticks);

                            if (ticks >= 30) {
                                // Aktywacja kamuflażu Stalkera
                                player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 30, 0, false, false, false));
                                lastStalkerInvisTime.put(uuid, System.currentTimeMillis());
                                player.getWorld().spawnParticle(Particle.FALLING_SPORE_BLOSSOM, currentLoc.clone().add(0, 1.0, 0), 4, 0.4, 0.5, 0.4, 0.02);
                                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§2🌿 §l[STALKER] §aKamuflaż aktywny §7(+35% Crit DMG z ukrycia)"));
                            }
                        } else {
                            stationaryTicks.remove(uuid);
                        }
                    } else {
                        stationaryTicks.put(uuid, 0);
                        lastPlayerLoc.put(uuid, currentLoc.clone());
                    }
                }
            }
        }.runTaskTimer(AmonPackPlugin.plugin, 10L, 5L);
    }

    public static boolean isNearFoliage(Location loc, double radius) {
        World world = loc.getWorld();
        if (world == null) return false;
        int r = (int) Math.ceil(radius);
        int bx = loc.getBlockX();
        int by = loc.getBlockY();
        int bz = loc.getBlockZ();

        for (int x = -r; x <= r; x++) {
            for (int y = -1; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    Block b = world.getBlockAt(bx + x, by + y, bz + z);
                    String name = b.getType().name();
                    if (name.contains("LEAVES") || name.contains("GRASS") || name.contains("FERN") ||
                            name.contains("VINE") || name.contains("BUSH") || name.contains("AZALEA") ||
                            name.contains("GLOW_BERRIES") || name.contains("MOSS")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public void handleLeftClick(Player player, ItemStack gunItem) {
        if (!GunData.isGun(gunItem)) return;
        GunData data = GunData.fromItemStack(gunItem);
        if (data == null) return;

        if (data.getCurrentAmmo() <= 0) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§cKomora pusta! §7[Przytrzymaj PPM aby załadować]"));
        } else {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§a● Broń załadowana! §7(" + data.getCurrentAmmo() + "/" + data.getMaxAmmoCapacity() + ") [Kliknij PPM aby strzelić]"));
        }
    }

    public long getCooldownMs(GunType type) {
        switch (type) {
            case PEPPERBOX:
                return 250L;
            case FLINTLOCK_PISTOL:
                return 500L;
            case FLINTLOCK_MUSKET:
            case BLUNDERBUSS:
            default:
                return 600L;
        }
    }

    public long getLastShotTime(UUID uuid) {
        return lastShotTime.getOrDefault(uuid, 0L);
    }

    public void fireGun(Player player, ItemStack gunItem, GunData data) {
        long now = System.currentTimeMillis();
        long last = lastShotTime.getOrDefault(player.getUniqueId(), 0L);
        long cooldownMs = getCooldownMs(data.getGunType());
        if (now - last < cooldownMs) {
            return;
        }

        if (data.getCurrentDurability() <= 0) {
            player.playSound(player.getLocation(), Sound.ITEM_SHIELD_BREAK, 1.0f, 0.8f);
            player.sendMessage("§c❌ Ta broń jest zniszczona! Napraw ją w Warsztacie Rusznikarskim.");
            return;
        }

        if (data.getCurrentAmmo() <= 0) {
            player.playSound(player.getLocation(), Sound.BLOCK_DISPENSER_FAIL, 1.0f, 1.4f);
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§c⚠ Brak kul w komorze! Przytrzymaj [PPM] aby załadować."));
            return;
        }

        lastShotTime.put(player.getUniqueId(), now);

        Location eyeLoc = player.getEyeLocation();
        Vector dir = eyeLoc.getDirection().normalize();
        World world = player.getWorld();

        AmmoType ammo = data.getLoadedAmmoType();
        if (ammo == null) {
            ammo = data.getGunType().getRequiredAmmoType();
        }

        boolean isDragonCartridge = (ammo == AmmoType.DRAGON_CARTRIDGE);
        boolean isDragonScatter = (ammo == AmmoType.DRAGON_SCATTER_SHOT);
        boolean isSlug = (ammo == AmmoType.SLUG_CARTRIDGE);
        boolean isDragon = isDragonCartridge || isDragonScatter;

        // Obliczenie obrażeń pocisku
        final double singleBulletDamage;
        if (data.getGunType() == GunType.BLUNDERBUSS) {
            if (isSlug) {
                singleBulletDamage = 15.0 + (data.getLevel() - 1) * 1.5;
            } else if (isDragonScatter) {
                singleBulletDamage = 1.8 + (data.getLevel() - 1) * 0.2;
            } else {
                singleBulletDamage = data.getDamage();
            }
        } else {
            singleBulletDamage = data.getDamage();
        }

        // 1. Dźwięki i dym wystrzału z lufy
        Location muzzleLoc = eyeLoc.clone().add(dir.clone().multiply(0.8)).add(0, -0.15, 0);

        float soundPitch = data.getGunType() == GunType.BLUNDERBUSS ? (isSlug ? 0.7f : 0.9f) :
                data.getGunType() == GunType.FLINTLOCK_MUSKET ? 1.1f : 1.4f;

        world.playSound(eyeLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, soundPitch);
        world.playSound(eyeLoc, Sound.ITEM_FLINTANDSTEEL_USE, 1.0f, 0.6f);
        if (isSlug) {
            world.playSound(eyeLoc, Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 1.0f, 0.8f);
        }

        boolean pepperboxUnique = (data.getUniqueMod() == GunUniqueMod.PEPPERBOX_PERFECT_SOLDIER);
        int smokeCount = pepperboxUnique ? 8 : (isSlug ? 28 : 18);
        int flameCount = isDragon ? 25 : (pepperboxUnique ? 5 : 12);

        world.spawnParticle(Particle.FLAME, muzzleLoc, flameCount, 0.15, 0.15, 0.15, 0.05);
        world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, muzzleLoc, smokeCount, 0.25, 0.25, 0.25, 0.05);
        world.spawnParticle(Particle.SMOKE, muzzleLoc, pepperboxUnique ? 10 : 25, 0.3, 0.3, 0.3, 0.08);

        // 2. Liczba pocisków z konfiguracji (zakres min_pellets .. max_pellets)
        int minP = GunConfigManager.getInstance().getAmmoMinPellets(ammo, GunConfigManager.getInstance().getMinPellets(data.getGunType()));
        int maxP = GunConfigManager.getInstance().getAmmoMaxPellets(ammo, GunConfigManager.getInstance().getMaxPellets(data.getGunType()));

        if (data.getUniqueMod() == GunUniqueMod.SHOTGUN_DEMOLITION) {
            minP += GunConfigManager.getInstance().getUniqueInt("shotgun_demolition", "extra_min_pellets", 4);
            maxP += GunConfigManager.getInstance().getUniqueInt("shotgun_demolition", "extra_max_pellets", 6);
        }

        int bulletCount;
        if (data.getGunType() == GunType.BLUNDERBUSS || ammo == AmmoType.SCATTER_SHOT || ammo == AmmoType.DRAGON_SCATTER_SHOT) {
            if (ammo == AmmoType.SLUG_CARTRIDGE) {
                bulletCount = 1;
            } else {
                int low = Math.min(minP, maxP);
                int high = Math.max(minP, maxP);
                bulletCount = low + (high > low ? random.nextInt(high - low + 1) : 0);
            }
        } else {
            bulletCount = 1;
        }

        boolean isAiming = isAiming(player);
        double baseSpread = data.getSpread();

        if (isAiming) baseSpread *= 0.25;
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
            simulateBullet(player, eyeLoc, spreadDir, data, ammo, singleBulletDamage);
        }

        // Demolka: Jeśli wystrzelono Breneka, wystrzel dodatkowo 4 rozproszone śruciny po bokach!
        if (data.getUniqueMod() == GunUniqueMod.SHOTGUN_DEMOLITION && isSlug) {
            int extraSlugs = GunConfigManager.getInstance().getUniqueInt("shotgun_demolition", "slug_extra_pellets", 4);
            for (int i = 0; i < extraSlugs; i++) {
                Vector extraDir = dir.clone().add(new Vector(
                        (random.nextDouble() - 0.5) * 0.35,
                        (random.nextDouble() - 0.5) * 0.35,
                        (random.nextDouble() - 0.5) * 0.35
                )).normalize();
                simulateBullet(player, eyeLoc, extraDir, data, AmmoType.SCATTER_SHOT, 2.5);
            }
        }

        // 3. Odrzut gracza
        applyRecoil(player, data.getGunType(), isSlug, pepperboxUnique);

        // 4. Zużycie amunicji i trwałości (z uwzględnieniem efektu Marksman_Ammo_Save: +5% na element pancerza)
        int ammoSavePieces = 0;
        for (ItemStack armorItem : player.getInventory().getArmorContents()) {
            if (armorItem != null && armorItem.hasItemMeta() && RPG.Crafting.CraftingMenager.HaveEffect(armorItem, "Marksman_Ammo_Save")) {
                ammoSavePieces++;
            }
        }
        double ammoSaveChance = ammoSavePieces * 0.05;
        boolean ammoSaved = (ammoSaveChance > 0 && Math.random() < ammoSaveChance);

        if (!ammoSaved) {
            data.setCurrentAmmo(data.getCurrentAmmo() - 1);
        } else {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.7f, 2.0f);
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§a✦ [Strzelec] Zaoszczędzono amunicję!"));
        }
        if (player.getGameMode() != GameMode.CREATIVE) {
            data.setCurrentDurability(data.getCurrentDurability() - 1);
        }

        // 5. Aktualizacja przedmiotu i stanu kuszy
        data.applyToItemStack(gunItem);

        int cooldownTicks;
        switch (data.getGunType()) {
            case PEPPERBOX:
                cooldownTicks = 6;
                break;
            case FLINTLOCK_PISTOL:
                cooldownTicks = 10;
                break;
            case FLINTLOCK_MUSKET:
            case BLUNDERBUSS:
            default:
                cooldownTicks = 12;
                break;
        }
        player.setCooldown(gunItem.getType(), cooldownTicks);

        if (data.getCurrentAmmo() > 0) {
            if (gunItem.getItemMeta() instanceof CrossbowMeta cm) {
                cm.setChargedProjectiles(Collections.singletonList(new ItemStack(Material.ARROW, 1)));
                gunItem.setItemMeta(cm);
            }
            StringBuilder sb = new StringBuilder("§a");
            for (int k = 0; k < data.getCurrentAmmo(); k++) sb.append("● ");
            for (int k = data.getCurrentAmmo(); k < data.getMaxAmmoCapacity(); k++) sb.append("§8○ ");
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(sb.toString() + "§e(" + data.getCurrentAmmo() + "/" + data.getMaxAmmoCapacity() + ") Gotowa do strzału! §7[PPM kolejny strzał]"));
        } else {
            if (gunItem.getItemMeta() instanceof CrossbowMeta cm) {
                cm.setChargedProjectiles(Collections.emptyList());
                gunItem.setItemMeta(cm);
            }
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§cKomora pusta! §7[Przytrzymaj PPM aby załadować]"));
        }

        // Natychmiastowa i synchroniczna aktualizacja broni w ręce gracza
        player.getInventory().setItemInMainHand(gunItem);
        player.updateInventory();

        // 1-tickowe potwierdzenie stanu w ekwipunku gracza
        final int targetAmmo = data.getCurrentAmmo();
        Bukkit.getScheduler().runTask(AmonPackPlugin.plugin, () -> {
            ItemStack handItem = player.getInventory().getItemInMainHand();
            if (GunData.isGun(handItem)) {
                GunData curData = GunData.fromItemStack(handItem);
                if (curData != null) {
                    curData.setCurrentAmmo(targetAmmo);
                    curData.applyToItemStack(handItem);
                    if (handItem.getItemMeta() instanceof CrossbowMeta cm) {
                        if (targetAmmo > 0) {
                            cm.setChargedProjectiles(Collections.singletonList(new ItemStack(Material.ARROW, 1)));
                        } else {
                            cm.setChargedProjectiles(Collections.emptyList());
                        }
                        handItem.setItemMeta(cm);
                    }
                    player.getInventory().setItemInMainHand(handItem);
                    player.updateInventory();
                }
            }
        });

        // 6. Powiadomienie progresji
        if (ProgressionManager.getInstance() != null && ProgressionManager.getInstance().getProgressionService() != null) {
            ProgressionManager.getInstance().getProgressionService().handleObjective(player, ObjectiveType.USE_ITEM, data.getGunType().getId(), 1);
        }
    }

    private void simulateBullet(Player shooter, Location startLoc, Vector initialVelocity, GunData data, AmmoType ammo, double bulletDamage) {
        World world = startLoc.getWorld();
        double speed = (ammo == AmmoType.SLUG_CARTRIDGE) ? 3.5 : 3.0; // 60-70 m/s
        Vector velocity = initialVelocity.clone().multiply(speed);
        Location currentLoc = startLoc.clone();
        final double maxRange = data.getEffectiveRange();
        final boolean isDragon = (ammo == AmmoType.DRAGON_CARTRIDGE || ammo == AmmoType.DRAGON_SCATTER_SHOT);
        final boolean isSlug = (ammo == AmmoType.SLUG_CARTRIDGE);
        final boolean isPiercing = (data.getGunType() == GunType.BLUNDERBUSS || isSlug || isDragon);

        Set<UUID> hitVictims = new HashSet<>();

        new BukkitRunnable() {
            private double distanceTraveled = 0.0;

            @Override
            public void run() {
                for (int step = 0; step < 3; step++) {
                    Vector stepMovement = velocity.clone().multiply(0.333);
                    RayTraceResult hit = world.rayTrace(currentLoc, stepMovement.normalize(), stepMovement.length(),
                            FluidCollisionMode.NEVER, true, 0.35, entity -> entity != shooter && entity instanceof LivingEntity && !hitVictims.contains(entity.getUniqueId()));

                    if (hit != null) {
                        Location hitPos = hit.getHitPosition().toLocation(world);
                        if (hit.getHitEntity() instanceof LivingEntity victim) {
                            hitVictims.add(victim.getUniqueId());
                            handleHitEntity(shooter, victim, hitPos, data, ammo, bulletDamage);
                            if (!isPiercing) {
                                cancel();
                                return;
                            }
                        } else if (hit.getHitBlock() != null) {
                            handleHitBlock(hitPos, isDragon, data.getUniqueMod() == GunUniqueMod.SHOTGUN_DEMOLITION);
                            cancel();
                            return;
                        }
                    }

                    currentLoc.add(stepMovement);
                    distanceTraveled += stepMovement.length();

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

    private void handleHitEntity(Player shooter, LivingEntity victim, Location hitLoc, GunData data, AmmoType ammo, double damage) {
        World world = victim.getWorld();
        boolean isHeadshot = hitLoc.getY() >= (victim.getEyeLocation().getY() - 0.25);
        boolean isDragon = (ammo == AmmoType.DRAGON_CARTRIDGE || ammo == AmmoType.DRAGON_SCATTER_SHOT);
        boolean isSlug = (ammo == AmmoType.SLUG_CARTRIDGE);

        if (isDragon) {
            damage += 3.0;
        }

        // Wsparcie Emocjonalne: jeśli cel jest wyczerpany, otrzymuje +30% obrażeń od pistoletów!
        if (data.getGunType() == GunType.FLINTLOCK_PISTOL && isExhausted(victim)) {
            damage *= 1.30;
            shooter.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§d💔 §l[Wsparcie Emocjonalne] §e+30% DMG (Cel Wyczerpany!)"));
        }

        final double baseBodyDamage = damage;

        if (isHeadshot) {
            double headshotMult = data.getHeadshotMultiplier();
            if (data.getUniqueMod() == GunUniqueMod.MUSKET_STALKER) {
                boolean stalkerActive = shooter.hasPotionEffect(PotionEffectType.INVISIBILITY)
                        || (System.currentTimeMillis() - lastStalkerInvisTime.getOrDefault(shooter.getUniqueId(), 0L) <= 5000L);
                if (stalkerActive) {
                    headshotMult += GunConfigManager.getInstance().getUniqueDouble("musket_stalker", "crit_damage_bonus", 0.35);
                    shooter.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§2🌿 §l[STALKER] §a+35% Crit DMG (Zasadzka)!"));
                }
            }
            damage *= headshotMult;
            int marksmanCritPieces = 0;
            for (ItemStack armorItem : shooter.getInventory().getArmorContents()) {
                if (armorItem != null && armorItem.hasItemMeta() && RPG.Crafting.CraftingMenager.HaveEffect(armorItem, "Marksman_Crit_Damage")) {
                    marksmanCritPieces++;
                }
            }
            if (marksmanCritPieces > 0) {
                damage *= (1.0 + (marksmanCritPieces * 0.05));
            }
            world.playSound(hitLoc, Sound.ENTITY_ARROW_HIT_PLAYER, 1.2f, 1.6f);
            world.playSound(hitLoc, Sound.BLOCK_ANVIL_LAND, 0.6f, 1.8f);
            world.spawnParticle(Particle.CRIT, hitLoc, 15, 0.2, 0.2, 0.2, 0.2);
            shooter.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§c🎯 §lTRAFIENIE W GŁOWĘ! §e(" + String.format(Locale.ROOT, "%.1f", damage) + " DMG)"));

            // Muszkiet: Rykoszetujący Zamek (Slot 40) - 33% szansy na przeskok pocisku do kolejnego celu
            if (data.getGunType() == GunType.FLINTLOCK_MUSKET && data.hasBayonet()) {
                if (Math.random() < 0.33) {
                    chainRicochet(shooter, victim, baseBodyDamage);
                }
            }
        } else {
            world.playSound(hitLoc, Sound.ENTITY_ARROW_HIT, 1.0f, 1.2f);
            world.spawnParticle(Particle.DAMAGE_INDICATOR, hitLoc, 6, 0.2, 0.2, 0.2, 0.1);
        }

        boolean willDie = (victim.getHealth() - damage <= 0);

        // Zerowanie noDamageTicks (i-frames), aby każdy trafiający śrut ze strzelby zadawał obrażenia!
        victim.setNoDamageTicks(0);
        victim.damage(damage, shooter);
        victim.setNoDamageTicks(0);

        // Pistolet: Wsparcie Emocjonalne - nakłada Wyczerpanie
        if (data.getUniqueMod() == GunUniqueMod.PISTOL_EMOTIONAL_SUPPORT) {
            int durationSec = GunConfigManager.getInstance().getUniqueInt("pistol_emotional_support", "exhaustion_duration_seconds", 5);
            exhaustedEnemies.put(victim.getUniqueId(), System.currentTimeMillis() + (durationSec * 1000L));
            victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, durationSec * 20, 1));
            world.spawnParticle(Particle.DAMAGE_INDICATOR, victim.getEyeLocation(), 8, 0.3, 0.3, 0.3, 0.1);
            world.playSound(hitLoc, Sound.ENTITY_VEX_HURT, 0.8f, 1.2f);
        }

        // Pieprzniczka: Huragan - Zabójstwo krytyczne (Headshot Kill) od razu ładuje 1 nabój do komory!
        if (data.getUniqueMod() == GunUniqueMod.PEPPERBOX_HURRICANE && isHeadshot && willDie) {
            int maxCap = data.getMaxAmmoCapacity();
            if (data.getCurrentAmmo() < maxCap) {
                data.setCurrentAmmo(data.getCurrentAmmo() + 1);
                ItemStack hand = shooter.getInventory().getItemInMainHand();
                if (GunData.isGun(hand)) {
                    data.applyToItemStack(hand);
                }
                world.playSound(shooter.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.2f, 2.0f);
                world.playSound(shooter.getLocation(), Sound.ITEM_ARMOR_EQUIP_GOLD, 1.0f, 1.4f);
                shooter.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§d🌪 §l[HURAGAN] §eZabójstwo w głowę! +1 Nabój naładowany natychmiast! (" + data.getCurrentAmmo() + "/" + maxCap + ")"));
            }
        }

        // ZWIĘKSZONY ODRZUT DLA STRZELBY (Garłacz) Z LEKKIM PODBICIEM Y +0.12
        if (data.getGunType() == GunType.BLUNDERBUSS) {
            Vector kb = victim.getLocation().toVector().subtract(shooter.getLocation().toVector());
            kb.setY(0);
            double kbStrength = isSlug ? 2.6 : 1.8;
            if (data.hasBayonet()) {
                kbStrength *= 1.5; // Ergonomiczne Łoże (+50% siły odrzutu przeciwnika)
            }
            if (kb.lengthSquared() > 0.001) {
                kb.normalize().multiply(kbStrength);
            }
            victim.setVelocity(new Vector(kb.getX(), 0.12, kb.getZ()));
        }

        if (isDragon) {
            victim.setFireTicks(120);
            world.spawnParticle(Particle.LAVA, hitLoc, 10, 0.3, 0.3, 0.3, 0.1);
            world.spawnParticle(Particle.FLAME, hitLoc, 15, 0.4, 0.3, 0.4, 0.05);
        }

        if (isHeadshot && ProgressionManager.getInstance() != null && ProgressionManager.getInstance().getProgressionService() != null) {
            ProgressionManager.getInstance().getProgressionService().handleObjective(shooter, ObjectiveType.KILL_ENTITY, "HEADSHOT", 1);
        }
    }

    public void grantShotgunFach(Player player) {
        shotgunFachBuff.put(player.getUniqueId(), System.currentTimeMillis() + 5000L);
    }

    public boolean isShotgunFachActive(Player player) {
        Long until = shotgunFachBuff.get(player.getUniqueId());
        return until != null && System.currentTimeMillis() < until;
    }

    private void chainRicochet(Player shooter, LivingEntity primaryVictim, double bodyDamage) {
        World world = primaryVictim.getWorld();
        Location pLoc = primaryVictim.getLocation().clone().add(0, 1.0, 0);
        LivingEntity nearest = null;
        double nearestDistSq = 64.0; // promień 8m

        for (org.bukkit.entity.Entity e : world.getNearbyEntities(pLoc, 8.0, 8.0, 8.0)) {
            if (e instanceof LivingEntity le && !le.equals(primaryVictim) && !le.equals(shooter) && !le.isDead()) {
                if (le instanceof org.bukkit.entity.ArmorStand) continue;
                double distSq = le.getLocation().distanceSquared(pLoc);
                if (distSq < nearestDistSq) {
                    nearestDistSq = distSq;
                    nearest = le;
                }
            }
        }

        if (nearest != null) {
            Location tLoc = nearest.getLocation().clone().add(0, 1.0, 0);
            Vector line = tLoc.toVector().subtract(pLoc.toVector());
            double dist = line.length();
            if (dist > 0.01) {
                Vector step = line.clone().normalize().multiply(0.4);
                Location cur = pLoc.clone();
                for (double d = 0; d < dist; d += 0.4) {
                    cur.add(step);
                    world.spawnParticle(Particle.CRIT, cur, 1, 0, 0, 0, 0);
                }
            }

            world.playSound(pLoc, Sound.ENTITY_ARROW_HIT, 1.2f, 1.8f);
            world.playSound(tLoc, Sound.ITEM_SHIELD_BLOCK, 1.0f, 1.8f);

            nearest.setNoDamageTicks(0);
            nearest.damage(bodyDamage, shooter);
            nearest.setNoDamageTicks(0);

            shooter.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§c⚡ §l[Rykoszet] §ePocisk trafił kolejny cel! (" + String.format(Locale.ROOT, "%.1f", bodyDamage) + " DMG)"));
        }
    }

    public boolean isExhausted(LivingEntity entity) {
        Long until = exhaustedEnemies.get(entity.getUniqueId());
        return until != null && System.currentTimeMillis() < until;
    }

    private void handleHitBlock(Location hitLoc, boolean isDragon, boolean isDemolition) {
        World world = hitLoc.getWorld();
        world.playSound(hitLoc, Sound.BLOCK_STONE_HIT, 1.0f, 1.2f);
        world.spawnParticle(Particle.BLOCK, hitLoc, 15, 0.2, 0.2, 0.2, 0.1, Material.STONE.createBlockData());

        if (isDragon) {
            int radius = isDemolition ? 2 : 1;
            world.spawnParticle(Particle.FLAME, hitLoc, isDemolition ? 35 : 16, 0.8, 0.4, 0.8, 0.08);

            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    Location fireLoc = hitLoc.clone().add(dx, 0, dz);
                    if (fireLoc.getBlock().getType() == Material.AIR) {
                        fireLoc.getBlock().setType(Material.FIRE);
                    }
                }
            }
        }
    }

    private void applyRecoil(Player player, GunType type, boolean isSlug, boolean pepperboxReduced) {
        float pitchKick = (type == GunType.BLUNDERBUSS) ? (isSlug ? -6.5f : -5.5f) :
                type == GunType.FLINTLOCK_MUSKET ? -4.0f : (pepperboxReduced ? -1.2f : -2.5f);
        float yawKick = (random.nextFloat() - 0.5f) * (pepperboxReduced ? 0.8f : 1.6f);

        Location loc = player.getLocation();
        loc.setPitch(Math.max(-90.0f, loc.getPitch() + pitchKick));
        loc.setYaw(loc.getYaw() + yawKick);
        player.teleport(loc);

        double backForce = (type == GunType.BLUNDERBUSS) ? (isSlug ? -0.40 : -0.28) :
                type == GunType.FLINTLOCK_MUSKET ? -0.15 : (pepperboxReduced ? -0.04 : -0.10);

        Vector back = player.getLocation().getDirection().setY(0).normalize().multiply(backForce);
        player.setVelocity(player.getVelocity().add(back));
    }

    public void startReload(Player player, ItemStack gunItem) {
        if (!GunData.isGun(gunItem)) return;
        GunData data = GunData.fromItemStack(gunItem);
        if (data == null) return;

        int maxCap = data.getMaxAmmoCapacity();
        if (data.getCurrentAmmo() >= maxCap) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§aBroń jest już w pełni załadowana! [PPM Wystrzał]"));
            return;
        }

        if (isReloading(player)) {
            return;
        }

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
        int baseTicks = totalTicks;

        // Efekt zbroi strzelca: Marksman_Reload_Speed (-5% czasu przeładowania za każdy element pancerza)
        int reloadSpeedPieces = 0;
        for (ItemStack armorItem : player.getInventory().getArmorContents()) {
            if (armorItem != null && armorItem.hasItemMeta() && RPG.Crafting.CraftingMenager.HaveEffect(armorItem, "Marksman_Reload_Speed")) {
                reloadSpeedPieces++;
            }
        }
        if (reloadSpeedPieces > 0) {
            double reductionPercent = reloadSpeedPieces * 0.05;
            int reductionTicks = (int) Math.round(baseTicks * reductionPercent);
            totalTicks = Math.max(10, totalTicks - reductionTicks);
        }

        // Strzelba: Ergonomiczne Łoże - efekt Fachu skraca reload o 0.5s (-10 ticków)
        if (data.getGunType() == GunType.BLUNDERBUSS && isShotgunFachActive(player)) {
            totalTicks = Math.max(10, totalTicks - 10);
        }

        if (gunItem.containsEnchantment(Enchantment.QUICK_CHARGE)) {
            int qc = gunItem.getEnchantmentLevel(Enchantment.QUICK_CHARGE);
            totalTicks = Math.max(10, totalTicks - (qc * 12));
        }

        final int finalTicks = totalTicks;
        UUID uuid = player.getUniqueId();
        int initialArrows = GunListener.countRealArrows(player);
        activeReloadRealArrows.put(uuid, initialArrows);

        // Muszkiet: Piechur - daje lekki efekt Speed podczas ładowania
        if (data.getUniqueMod() == GunUniqueMod.MUSKET_INFANTRYMAN) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, finalTicks + 5, 0, false, false, false));
        }

        BukkitTask task = new BukkitRunnable() {
            int tick = 0;
            int notRaisedTicks = 0;

            @Override
            public void run() {
                ItemStack currentHand = player.getInventory().getItemInMainHand();
                if (!GunData.isGun(currentHand) || !player.isOnline()) {
                    cancelReload(player);
                    return;
                }

                // Weryfikacja czy gracz rzeczywiście trzyma PPM (naciąga kuszę)
                if (!player.isHandRaised()) {
                    notRaisedTicks++;
                    if (notRaisedTicks > 2) {
                        cancelReload(player);
                        return;
                    }
                } else {
                    notRaisedTicks = 0;
                }

                tick++;
                double progress = (double) tick / finalTicks;
                int barBlocks = (int) (progress * 10);
                StringBuilder bar = new StringBuilder("§e[");
                for (int b = 0; b < 10; b++) {
                    if (b < barBlocks) bar.append("§a■");
                    else bar.append("§7□");
                }
                int pct = (int) (progress * 100);
                bar.append("§e] §fŁadowanie: ").append(chosenAmmo.getDisplayName()).append(" §7(").append(pct).append("%)");
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(bar.toString()));

                if (tick == 1) {
                    player.playSound(player.getLocation(), Sound.BLOCK_SAND_PLACE, 1.0f, 1.1f);
                } else if (tick == finalTicks / 2) {
                    player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_IRON, 1.0f, 1.2f);
                } else if (tick == (int) (finalTicks * 0.85)) {
                    player.playSound(player.getLocation(), Sound.BLOCK_LEVER_CLICK, 1.0f, 1.4f);
                }

                if (tick >= finalTicks) {
                    finishReload(player, currentHand, data, chosenAmmo);
                    cancel();
                }
            }
        }.runTaskTimer(AmonPackPlugin.plugin, 1L, 1L);

        activeReloads.put(uuid, task);
    }

    private void finishReload(Player player, ItemStack gunItem, GunData data, AmmoType ammoToLoad) {
        activeReloads.remove(player.getUniqueId());
        restoreConsumedArrows(player);
        GunListener.cleanGhostArrows(player);

        int maxCap = data.getMaxAmmoCapacity();
        boolean freeAmmo = false;

        // Muszkiet: Piechur - szansa na nie-zużycie kuli przy ładowaniu
        if (data.getUniqueMod() == GunUniqueMod.MUSKET_INFANTRYMAN) {
            double freeChance = GunConfigManager.getInstance().getUniqueDouble("musket_infantryman", "free_ammo_chance", 0.25);
            if (random.nextDouble() < freeChance) {
                freeAmmo = true;
            }
        }

        if (player.getGameMode() != GameMode.CREATIVE && !freeAmmo) {
            int needed = maxCap - data.getCurrentAmmo();
            int consumed = consumeAmmoByPriority(player, ammoToLoad, needed);
            if (consumed <= 0) return;
            data.setCurrentAmmo(data.getCurrentAmmo() + consumed);
        } else {
            data.setCurrentAmmo(maxCap);
        }

        data.setLoadedAmmoType(ammoToLoad);
        data.applyToItemStack(gunItem);
        if (gunItem.getItemMeta() instanceof CrossbowMeta cm) {
            cm.setChargedProjectiles(Collections.singletonList(new ItemStack(Material.ARROW, 1)));
            gunItem.setItemMeta(cm);
        }
        player.getInventory().setItemInMainHand(gunItem);
        player.updateInventory();

        player.playSound(player.getLocation(), Sound.ITEM_FLINTANDSTEEL_USE, 1.0f, 1.4f);
        player.playSound(player.getLocation(), Sound.BLOCK_LEVER_CLICK, 1.0f, 1.8f);

        if (freeAmmo) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§e✦ §l[Piechur] §aZachowano amunicję! §fZaładowano " + ammoToLoad.getDisplayName() + " (" + data.getCurrentAmmo() + "/" + maxCap + ") §e[PPM Wystrzał]"));
        } else {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§a✔ Załadowano " + ammoToLoad.getDisplayName() + " §f(" + data.getCurrentAmmo() + "/" + maxCap + ") §e[PPM Wystrzał]"));
        }
    }

    public boolean isReloading(Player player) {
        return activeReloads.containsKey(player.getUniqueId());
    }

    public void cancelReload(Player player) {
        BukkitTask task = activeReloads.remove(player.getUniqueId());
        restoreConsumedArrows(player);
        GunListener.cleanGhostArrows(player);
        if (task != null) {
            task.cancel();
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§cPrzeładowanie przerwane!"));
        }
    }

    private void restoreConsumedArrows(Player player) {
        Integer initial = activeReloadRealArrows.remove(player.getUniqueId());
        if (initial != null && player.isOnline()) {
            int current = GunListener.countRealArrows(player);
            int diff = initial - current;
            if (diff > 0) {
                player.getInventory().addItem(new ItemStack(Material.ARROW, diff));
                player.updateInventory();
            }
        }
    }

    public void startAiming(Player player, ItemStack gunItem) {
        GunData data = GunData.fromItemStack(gunItem);
        boolean hasScope = (data != null && data.hasBrassScope());

        if (hasScope) {
            if (!scopedPlayers.contains(player.getUniqueId())) {
                player.playSound(player.getLocation(), Sound.ITEM_SPYGLASS_USE, 1.0f, 1.2f);
            }
            scopedPlayers.add(player.getUniqueId());
            aimingPlayers.add(player.getUniqueId());

            player.setWalkSpeed(0.025f);
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 14, 6, false, false, false));
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§8[ §f─────── §c⊕ §f─────── §8] §e10x LUNETA OPTYCZNA"));
        } else {
            aimingPlayers.add(player.getUniqueId());
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 14, 4, false, false, false));
        }
    }

    public void stopAiming(Player player) {
        aimingPlayers.remove(player.getUniqueId());
        if (scopedPlayers.remove(player.getUniqueId())) {
            player.setWalkSpeed(0.2f);
            player.playSound(player.getLocation(), Sound.ITEM_SPYGLASS_STOP_USING, 1.0f, 1.2f);
        }
        player.removePotionEffect(PotionEffectType.SLOWNESS);
    }

    public boolean isAiming(Player player) {
        return aimingPlayers.contains(player.getUniqueId());
    }

    public AmmoType findHighestPriorityAmmo(Player player, GunType gunType) {
        for (int slot = 0; slot <= 8; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            AmmoType at = getAmmoTypeFromStack(stack);
            if (at != null && gunType.isCompatibleAmmo(at)) {
                return at;
            }
        }
        for (int slot = 9; slot <= 35; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            AmmoType at = getAmmoTypeFromStack(stack);
            if (at != null && gunType.isCompatibleAmmo(at)) {
                return at;
            }
        }
        ItemStack offhand = player.getInventory().getItemInOffHand();
        AmmoType atOff = getAmmoTypeFromStack(offhand);
        if (atOff != null && gunType.isCompatibleAmmo(atOff)) {
            return atOff;
        }

        return null;
    }

    public int consumeAmmoByPriority(Player player, AmmoType type, int maxToConsume) {
        int remaining = maxToConsume;
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
            if (cmd == AmmoType.DRAGON_SCATTER_SHOT.getCustomModelData()) return AmmoType.DRAGON_SCATTER_SHOT;
        }
        if (meta.hasDisplayName()) {
            String name = meta.getDisplayName().toLowerCase(Locale.ROOT);
            if (name.contains("slug") || name.contains("brenek")) return AmmoType.SLUG_CARTRIDGE;
            if (name.contains("smoczy śrut") || name.contains("dragon_scatter")) return AmmoType.DRAGON_SCATTER_SHOT;
            if (name.contains("dragon") || name.contains("zapalając") || name.contains("smocz")) return AmmoType.DRAGON_CARTRIDGE;
            if (name.contains("scatter") || name.contains("śrut")) return AmmoType.SCATTER_SHOT;
            if (name.contains("lead") || name.contains("ołowian")) return AmmoType.LEAD_BULLET;
        }
        return null;
    }
}

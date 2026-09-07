package RPG.BattleRoyale.Zombies;

import CustomContent.Guns.AmmoType;
import CustomContent.Guns.GunType;
import CustomContent.Guns.GunUniqueMod;
import Plugin.AmonPackPlugin;
import RPG.BattleRoyale.Weapons.BattleRoyaleWeaponHelper;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Zarządza niestandardowymi typami zombie na arenie Battle Royale:
 * 1. Leaper (Skoczek) - zwinny zombie skaczący na ofiarę z odległości 4-12 bloków.
 * 2. Gunner (Strzelec) - zombie uzbrojony w pistolet skałkowy, strzelający seriami z dystansu.
 */
public class CustomZombieManager {

    public static final NamespacedKey KEY_ZOMBIE_TYPE = new NamespacedKey(AmonPackPlugin.plugin, "br_zombie_type");

    public enum ZombieType {
        LEAPER,
        GUNNER
    }

    private final Set<UUID> leapers = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<UUID> gunners = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private final Map<UUID, Long> leaperCooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, Long> gunnerCooldowns = new ConcurrentHashMap<>();

    private final Random random = new Random();

    /**
     * Spawnuje losowego specjalnego zombie (Leaper lub Gunner).
     */
    public Zombie spawnRandomSpecialZombie(Location loc, LivingEntity target) {
        if (random.nextBoolean()) {
            return spawnLeaper(loc, target);
        } else {
            return spawnGunner(loc, target);
        }
    }

    /**
     * Spawnuje Skoczka (Leaper Zombie).
     */
    public Zombie spawnLeaper(Location loc, LivingEntity target) {
        if (loc == null || loc.getWorld() == null) return null;

        Zombie zombie = loc.getWorld().spawn(loc, Zombie.class, z -> {
            z.setCustomName(ChatColor.DARK_RED + "" + ChatColor.BOLD + "Skoczek Zombie");
            z.setCustomNameVisible(true);
            z.setCanPickupItems(false);

            if (z.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED) != null) {
                z.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.34);
            }
            if (z.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null) {
                z.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(26.0);
                z.setHealth(26.0);
            }

            EntityEquipment eq = z.getEquipment();
            if (eq != null) {
                ItemStack helmet = new ItemStack(Material.LEATHER_HELMET);
                LeatherArmorMeta meta = (LeatherArmorMeta) helmet.getItemMeta();
                if (meta != null) {
                    meta.setColor(Color.fromRGB(150, 20, 20));
                    helmet.setItemMeta(meta);
                }
                eq.setHelmet(helmet);
                eq.setHelmetDropChance(0.0f);

                ItemStack boots = new ItemStack(Material.LEATHER_BOOTS);
                LeatherArmorMeta bMeta = (LeatherArmorMeta) boots.getItemMeta();
                if (bMeta != null) {
                    bMeta.setColor(Color.fromRGB(40, 40, 40));
                    boots.setItemMeta(bMeta);
                }
                eq.setBoots(boots);
                eq.setBootsDropChance(0.0f);
            }

            z.getPersistentDataContainer().set(KEY_ZOMBIE_TYPE, PersistentDataType.STRING, ZombieType.LEAPER.name());
        });

        if (target != null) {
            zombie.setTarget(target);
        }

        leapers.add(zombie.getUniqueId());
        return zombie;
    }

    /**
     * Spawnuje Strzelca (Gunner Zombie).
     */
    public Zombie spawnGunner(Location loc, LivingEntity target) {
        if (loc == null || loc.getWorld() == null) return null;

        Zombie zombie = loc.getWorld().spawn(loc, Zombie.class, z -> {
            z.setCustomName(ChatColor.GOLD + "" + ChatColor.BOLD + "Strzelec Zombie");
            z.setCustomNameVisible(true);
            z.setCanPickupItems(false);

            if (z.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null) {
                z.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(30.0);
                z.setHealth(30.0);
            }

            EntityEquipment eq = z.getEquipment();
            if (eq != null) {
                ItemStack gun = BattleRoyaleWeaponHelper.createGun(GunType.FLINTLOCK_PISTOL, 1, false, false, false, false, GunUniqueMod.NONE);
                eq.setItemInMainHand(gun);
                eq.setItemInMainHandDropChance(0.35f);

                ItemStack chest = new ItemStack(Material.LEATHER_CHESTPLATE);
                LeatherArmorMeta meta = (LeatherArmorMeta) chest.getItemMeta();
                if (meta != null) {
                    meta.setColor(Color.fromRGB(80, 50, 30));
                    chest.setItemMeta(meta);
                }
                eq.setChestplate(chest);
                eq.setChestplateDropChance(0.0f);

                eq.setHelmet(new ItemStack(Material.CHAINMAIL_HELMET));
                eq.setHelmetDropChance(0.1f);
            }

            z.getPersistentDataContainer().set(KEY_ZOMBIE_TYPE, PersistentDataType.STRING, ZombieType.GUNNER.name());
        });

        if (target != null) {
            zombie.setTarget(target);
        }

        gunners.add(zombie.getUniqueId());
        return zombie;
    }

    /**
     * Główna pętla AI specjalnych zombie - wykonywana co 10 ticków (0.5 sekundy).
     */
    public void tick(World world) {
        if (world == null) return;
        long now = System.currentTimeMillis();

        // 1. Obsługa Skoczków (Leaper)
        Iterator<UUID> itLeaper = leapers.iterator();
        while (itLeaper.hasNext()) {
            UUID id = itLeaper.next();
            Entity entity = Bukkit.getEntity(id);
            if (!(entity instanceof Zombie zombie) || !zombie.isValid() || zombie.isDead()) {
                itLeaper.remove();
                leaperCooldowns.remove(id);
                continue;
            }

            LivingEntity target = zombie.getTarget();
            if (target == null || !target.isValid() || target.isDead()) continue;

            double distSq = zombie.getLocation().distanceSquared(target.getLocation());
            // Skacze z odległości 4.0 do 12.0 bloków
            if (distSq >= 16.0 && distSq <= 144.0) {
                long lastLeap = leaperCooldowns.getOrDefault(id, 0L);
                if (now - lastLeap >= 4000) { // Co 4 sekundy
                    leaperCooldowns.put(id, now);
                    performLeap(zombie, target);
                }
            }
        }

        // 2. Obsługa Strzelców (Gunner)
        Iterator<UUID> itGunner = gunners.iterator();
        while (itGunner.hasNext()) {
            UUID id = itGunner.next();
            Entity entity = Bukkit.getEntity(id);
            if (!(entity instanceof Zombie zombie) || !zombie.isValid() || zombie.isDead()) {
                itGunner.remove();
                gunnerCooldowns.remove(id);
                continue;
            }

            LivingEntity target = zombie.getTarget();
            if (target == null || !target.isValid() || target.isDead()) continue;

            double distSq = zombie.getLocation().distanceSquared(target.getLocation());
            // Strzela z dystansu 5.0 do 22.0 bloków
            if (distSq >= 25.0 && distSq <= 484.0) {
                if (zombie.hasLineOfSight(target)) {
                    long lastShot = gunnerCooldowns.getOrDefault(id, 0L);
                    if (now - lastShot >= 4500) { // Co 4.5 sekundy
                        gunnerCooldowns.put(id, now);
                        performGunshot(zombie, target);
                    }
                }
            }
        }
    }

    private void performLeap(Zombie zombie, LivingEntity target) {
        Location zLoc = zombie.getLocation();
        Location tLoc = target.getLocation();

        Vector dir = tLoc.toVector().subtract(zLoc.toVector()).normalize();
        dir.multiply(1.35);
        dir.setY(0.48);

        zombie.setVelocity(dir);

        // Efekty dźwiękowe i cząsteczkowe
        zombie.getWorld().playSound(zLoc, Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 1.0f, 1.4f);
        zombie.getWorld().playSound(zLoc, Sound.ENTITY_RAVAGER_ROAR, 0.7f, 1.8f);
        zombie.getWorld().spawnParticle(Particle.EXPLOSION, zLoc.clone().add(0, 0.5, 0), 1);
        zombie.getWorld().spawnParticle(Particle.CRIT, zLoc.clone().add(0, 1.0, 0), 10, 0.3, 0.3, 0.3, 0.1);
    }

    private void performGunshot(Zombie zombie, LivingEntity target) {
        Location eyeLoc = zombie.getEyeLocation();
        Location targetCenter = target.getLocation().add(0, target.getHeight() * 0.5, 0);

        Vector direction = targetCenter.toVector().subtract(eyeLoc.toVector()).normalize();

        // Dźwięk strzału i dym z lufy
        zombie.getWorld().playSound(eyeLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 1.8f);
        zombie.getWorld().playSound(eyeLoc, Sound.ITEM_FIRECHARGE_USE, 1.0f, 0.8f);
        zombie.getWorld().spawnParticle(Particle.LARGE_SMOKE, eyeLoc.clone().add(direction.clone().multiply(0.8)), 4, 0.1, 0.1, 0.1, 0.05);
        zombie.getWorld().spawnParticle(Particle.FLAME, eyeLoc.clone().add(direction.clone().multiply(0.8)), 3, 0.05, 0.05, 0.05, 0.02);

        // Raycast pocisku
        RayTraceResult hit = zombie.getWorld().rayTrace(
                eyeLoc,
                direction,
                24.0,
                FluidCollisionMode.NEVER,
                true,
                0.4,
                e -> e != zombie && e instanceof LivingEntity
        );

        double maxDist = 24.0;
        if (hit != null && hit.getHitPosition() != null) {
            maxDist = eyeLoc.toVector().distance(hit.getHitPosition());
        }

        // Renderowanie trajektorii pocisku
        for (double d = 0.5; d < maxDist; d += 0.8) {
            Location p = eyeLoc.clone().add(direction.clone().multiply(d));
            zombie.getWorld().spawnParticle(Particle.CRIT, p, 1, 0, 0, 0, 0);
        }

        // Trafienie
        if (hit != null && hit.getHitEntity() instanceof LivingEntity victim) {
            victim.damage(6.5, zombie);
            victim.playSound(victim.getLocation(), Sound.ENTITY_PLAYER_HURT, 0.9f, 1.0f);
            victim.getWorld().spawnParticle(Particle.BLOCK, victim.getLocation().add(0, 1.0, 0), 10, 0.2, 0.3, 0.2,
                    Material.REDSTONE_BLOCK.createBlockData());

            if (victim instanceof Player pVictim) {
                pVictim.sendMessage(ChatColor.RED + "[BattleRoyale] Zostałeś postrzelony przez Strzelca Zombie! (-6.5 HP)");
            }
        }
    }

    public boolean isSpecialZombie(Entity entity) {
        if (entity == null) return false;
        return leapers.contains(entity.getUniqueId()) || gunners.contains(entity.getUniqueId())
                || entity.getPersistentDataContainer().has(KEY_ZOMBIE_TYPE, PersistentDataType.STRING);
    }

    public void cleanup() {
        leapers.clear();
        gunners.clear();
        leaperCooldowns.clear();
        gunnerCooldowns.clear();
    }
}

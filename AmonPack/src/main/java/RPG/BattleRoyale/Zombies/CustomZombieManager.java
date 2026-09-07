package RPG.BattleRoyale.Zombies;

import CustomContent.Guns.AmmoType;
import CustomContent.Guns.GunType;
import CustomContent.Guns.GunUniqueMod;
import Plugin.AmonPackPlugin;
import RPG.BattleRoyale.Backpacks.BackpackManager;
import RPG.BattleRoyale.Blood.BloodTrail;
import RPG.BattleRoyale.Blood.BloodTrailManager;
import RPG.BattleRoyale.Events.NoiseManager;
import RPG.BattleRoyale.Infection.InfectionManager;
import RPG.BattleRoyale.Loot.BattleRoyaleLootManager;
import RPG.BattleRoyale.Weapons.BattleRoyaleWeaponHelper;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rozszerzony menedżer unikalnych typów zombie i ich zaawansowanego AI:
 * 1. LEAPER - zwinny skoczek atakujący z powietrza (4-12m)
 * 2. GUNNER - strzelec uzbrojony w pistolet skałkowy (5-22m)
 * 3. STALKER - cichy łowca zachowujący dystans, wycofujący się przy kontakcie wzrokowym i flankujący od tyłu
 * 4. BLOATER - powolny gigant, po śmierci wybucha toksyczną chmurą kwasu i infekcji (bez niszczenia bloków)
 * 5. SURVIVOR - udaje leżącego trupa; przy podejściu na 5 bloków wykonuje głośny jump-scare i atakuje
 */
public class CustomZombieManager {

    public static final NamespacedKey KEY_ZOMBIE_TYPE = new NamespacedKey(AmonPackPlugin.plugin, "br_zombie_type");

    public enum ZombieType {
        LEAPER,
        GUNNER,
        STALKER,
        BLOATER,
        SURVIVOR
    }

    public enum StalkerState {
        OBSERVE,
        RETREAT,
        FLANK,
        ATTACK,
        ESCAPE
    }

    private final Set<UUID> leapers = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<UUID> gunners = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<UUID> stalkers = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<UUID> bloaters = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<UUID> dormantSurvivors = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private final Map<UUID, Long> leaperCooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, Long> gunnerCooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, StalkerState> stalkerStates = new ConcurrentHashMap<>();
    private final Map<UUID, Long> stalkerStateTimers = new ConcurrentHashMap<>();

    // Pamięć zdarzeń (hałas, ślady krwi, ostatnia znana pozycja) dla wszystkich potworów
    private final Map<UUID, ZombieMemory> monsterMemories = new ConcurrentHashMap<>();

    private final Random random = new Random();

    public ZombieMemory getOrCreateMemory(Entity entity) {
        if (entity == null) return null;
        return monsterMemories.computeIfAbsent(entity.getUniqueId(), k -> new ZombieMemory());
    }

    public void onMonsterHearNoise(Mob mob, Location noiseLoc, double intensity) {
        if (mob == null || noiseLoc == null) return;
        ZombieMemory memory = getOrCreateMemory(mob);
        if (memory != null) {
            memory.setNoiseLocation(noiseLoc, 12000L);
            steerMobTowards(mob, noiseLoc, 0.28);
        }
    }

    public Zombie spawnRandomSpecialZombie(Location loc, LivingEntity target) {
        int roll = random.nextInt(5);
        switch (roll) {
            case 0: return spawnLeaper(loc, target);
            case 1: return spawnGunner(loc, target);
            case 2: return spawnStalker(loc, target);
            case 3: return spawnBloater(loc, target);
            case 4: default: return spawnSurvivor(loc);
        }
    }

    // ==========================================
    // 1. LEAPER
    // ==========================================
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

        if (target != null) zombie.setTarget(target);
        leapers.add(zombie.getUniqueId());
        return zombie;
    }

    // ==========================================
    // 2. GUNNER
    // ==========================================
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

        if (target != null) zombie.setTarget(target);
        gunners.add(zombie.getUniqueId());
        return zombie;
    }

    // ==========================================
    // 3. STALKER
    // ==========================================
    public Zombie spawnStalker(Location loc, LivingEntity target) {
        if (loc == null || loc.getWorld() == null) return null;

        Zombie zombie = loc.getWorld().spawn(loc, Zombie.class, z -> {
            z.setCustomName(ChatColor.DARK_GRAY + "" + ChatColor.BOLD + "☠ Prześladowca (Stalker)");
            z.setCustomNameVisible(true);
            z.setCanPickupItems(false);

            if (z.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED) != null) {
                z.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.35);
            }
            if (z.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null) {
                z.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(32.0);
                z.setHealth(32.0);
            }

            EntityEquipment eq = z.getEquipment();
            if (eq != null) {
                ItemStack helmet = new ItemStack(Material.LEATHER_HELMET);
                LeatherArmorMeta meta = (LeatherArmorMeta) helmet.getItemMeta();
                if (meta != null) {
                    meta.setColor(Color.fromRGB(20, 20, 20));
                    helmet.setItemMeta(meta);
                }
                eq.setHelmet(helmet);
                eq.setHelmetDropChance(0.0f);

                ItemStack boots = new ItemStack(Material.LEATHER_BOOTS);
                LeatherArmorMeta bMeta = (LeatherArmorMeta) boots.getItemMeta();
                if (bMeta != null) {
                    bMeta.setColor(Color.fromRGB(15, 15, 15));
                    boots.setItemMeta(bMeta);
                }
                eq.setBoots(boots);
                eq.setBootsDropChance(0.0f);
            }

            z.getPersistentDataContainer().set(KEY_ZOMBIE_TYPE, PersistentDataType.STRING, ZombieType.STALKER.name());
        });

        if (target != null) zombie.setTarget(target);
        stalkers.add(zombie.getUniqueId());
        stalkerStates.put(zombie.getUniqueId(), StalkerState.OBSERVE);
        stalkerStateTimers.put(zombie.getUniqueId(), System.currentTimeMillis());
        return zombie;
    }

    // ==========================================
    // 4. BLOATER
    // ==========================================
    public Zombie spawnBloater(Location loc, LivingEntity target) {
        if (loc == null || loc.getWorld() == null) return null;

        Zombie zombie = loc.getWorld().spawn(loc, Zombie.class, z -> {
            z.setCustomName(ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "☣ Spuchlak (Bloater)");
            z.setCustomNameVisible(true);
            z.setCanPickupItems(false);

            if (z.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED) != null) {
                z.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.18); // wolny
            }
            if (z.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null) {
                z.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(55.0); // wielkie HP
                z.setHealth(55.0);
            }
            if (z.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE) != null) {
                z.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE).setBaseValue(0.7);
            }

            EntityEquipment eq = z.getEquipment();
            if (eq != null) {
                ItemStack chest = new ItemStack(Material.LEATHER_CHESTPLATE);
                LeatherArmorMeta meta = (LeatherArmorMeta) chest.getItemMeta();
                if (meta != null) {
                    meta.setColor(Color.fromRGB(40, 120, 30));
                    chest.setItemMeta(meta);
                }
                eq.setChestplate(chest);
                eq.setChestplateDropChance(0.0f);

                ItemStack helmet = new ItemStack(Material.LEATHER_HELMET);
                LeatherArmorMeta hMeta = (LeatherArmorMeta) helmet.getItemMeta();
                if (hMeta != null) {
                    hMeta.setColor(Color.fromRGB(60, 140, 40));
                    helmet.setItemMeta(hMeta);
                }
                eq.setHelmet(helmet);
                eq.setHelmetDropChance(0.0f);
            }

            z.getPersistentDataContainer().set(KEY_ZOMBIE_TYPE, PersistentDataType.STRING, ZombieType.BLOATER.name());
        });

        if (target != null) zombie.setTarget(target);
        bloaters.add(zombie.getUniqueId());
        return zombie;
    }

    // ==========================================
    // 5. SURVIVOR (DORMANT JUMP-SCARE)
    // ==========================================
    public Zombie spawnSurvivor(Location loc) {
        if (loc == null || loc.getWorld() == null) return null;

        Zombie zombie = loc.getWorld().spawn(loc, Zombie.class, z -> {
            z.setCustomName(ChatColor.GRAY + "[Ciało Ocalałego]");
            z.setCustomNameVisible(true);
            z.setCanPickupItems(false);
            z.setAI(false); // nie rusza się dopóki gracz nie podejdzie
            z.setSilent(true);

            EntityEquipment eq = z.getEquipment();
            if (eq != null) {
                ItemStack chest = new ItemStack(Material.LEATHER_CHESTPLATE);
                LeatherArmorMeta meta = (LeatherArmorMeta) chest.getItemMeta();
                if (meta != null) {
                    meta.setColor(Color.fromRGB(110, 80, 50));
                    chest.setItemMeta(meta);
                }
                eq.setChestplate(chest);
                eq.setChestplateDropChance(0.0f);

                eq.setItemInMainHand(new ItemStack(Material.IRON_SWORD));
                eq.setItemInMainHandDropChance(0.2f);
            }

            z.getPersistentDataContainer().set(KEY_ZOMBIE_TYPE, PersistentDataType.STRING, ZombieType.SURVIVOR.name());
        });

        dormantSurvivors.add(zombie.getUniqueId());
        return zombie;
    }

    /**
     * Główna pętla AI specjalnych zombie - wykonywana co 10 ticków (0.5 sekundy).
     */
    public void tick(World world, BloodTrailManager bloodManager, NoiseManager noiseManager, List<Player> players) {
        if (world == null) return;
        long now = System.currentTimeMillis();

        // 1. Obsługa Skoczków (Leaper)
        tickLeapers(now);

        // 2. Obsługa Strzelców (Gunner)
        tickGunners(now);

        // 3. Obsługa Prześladowców (Stalker)
        tickStalkers(now);

        // 4. Detekcja podejścia gracza do leżących Ocalałych (Survivor Jump-Scare)
        tickDormantSurvivors(players, noiseManager);

        // 5. Globalna pamięć zombie: kierowanie się do hałasu lub tropienie śladów krwi
        tickZombieMemories(world, bloodManager);
    }

    private void tickLeapers(long now) {
        Iterator<UUID> it = leapers.iterator();
        while (it.hasNext()) {
            UUID id = it.next();
            Entity entity = Bukkit.getEntity(id);
            if (!(entity instanceof Zombie zombie) || !zombie.isValid() || zombie.isDead()) {
                it.remove();
                leaperCooldowns.remove(id);
                continue;
            }

            LivingEntity target = zombie.getTarget();
            if (target == null || !target.isValid() || target.isDead()) continue;

            double distSq = zombie.getLocation().distanceSquared(target.getLocation());
            if (distSq >= 16.0 && distSq <= 144.0) {
                long lastLeap = leaperCooldowns.getOrDefault(id, 0L);
                if (now - lastLeap >= 4000) {
                    leaperCooldowns.put(id, now);
                    performLeap(zombie, target);
                }
            }
        }
    }

    private void tickGunners(long now) {
        Iterator<UUID> it = gunners.iterator();
        while (it.hasNext()) {
            UUID id = it.next();
            Entity entity = Bukkit.getEntity(id);
            if (!(entity instanceof Zombie zombie) || !zombie.isValid() || zombie.isDead()) {
                it.remove();
                gunnerCooldowns.remove(id);
                continue;
            }

            LivingEntity target = zombie.getTarget();
            if (target == null || !target.isValid() || target.isDead()) continue;

            double distSq = zombie.getLocation().distanceSquared(target.getLocation());
            if (distSq >= 25.0 && distSq <= 484.0) {
                if (zombie.hasLineOfSight(target)) {
                    long lastShot = gunnerCooldowns.getOrDefault(id, 0L);
                    if (now - lastShot >= 4500) {
                        gunnerCooldowns.put(id, now);
                        performGunshot(zombie, target);
                    }
                }
            }
        }
    }

    /**
     * AI Stalkera: Obserwacja, wycofywanie się gdy gracz patrzy, flankowanie i uderzenie od tyłu.
     */
    private void tickStalkers(long now) {
        Iterator<UUID> it = stalkers.iterator();
        while (it.hasNext()) {
            UUID id = it.next();
            Entity entity = Bukkit.getEntity(id);
            if (!(entity instanceof Zombie zombie) || !zombie.isValid() || zombie.isDead()) {
                it.remove();
                stalkerStates.remove(id);
                stalkerStateTimers.remove(id);
                continue;
            }

            LivingEntity target = zombie.getTarget();
            if (target == null || !target.isValid() || target.isDead()) continue;

            Location zLoc = zombie.getLocation();
            Location tLoc = target.getLocation();
            double dist = zLoc.distance(tLoc);

            StalkerState state = stalkerStates.getOrDefault(id, StalkerState.OBSERVE);
            long stateStart = stalkerStateTimers.getOrDefault(id, now);

            // Wektor spojrzenia gracza
            Vector targetLook = target.getLocation().getDirection().normalize();
            Vector toStalker = zLoc.toVector().subtract(tLoc.toVector()).normalize();
            double dot = targetLook.dot(toStalker); // > 0.5 = gracz patrzy na stalkera

            switch (state) {
                case OBSERVE:
                    if (dot > 0.5 && zombie.hasLineOfSight(target)) {
                        // Gracz zauważył stalkera - uciekamy!
                        stalkerStates.put(id, StalkerState.RETREAT);
                        stalkerStateTimers.put(id, now);
                    } else if (dist <= 14.0) {
                        stalkerStates.put(id, StalkerState.FLANK);
                        stalkerStateTimers.put(id, now);
                    } else {
                        steerMobTowards(zombie, tLoc, 0.26);
                    }
                    break;

                case RETREAT:
                    // Biegniemy w przeciwnym kierunku niż gracz
                    Vector retreatDir = zLoc.toVector().subtract(tLoc.toVector()).normalize().multiply(1.5);
                    steerMobTowards(zombie, zLoc.clone().add(retreatDir), 0.38);
                    if (now - stateStart >= 2500 || dist >= 16.0) {
                        stalkerStates.put(id, StalkerState.FLANK);
                        stalkerStateTimers.put(id, now);
                    }
                    break;

                case FLANK:
                    // Obchodzimy gracza łukiem (prostopadle do wektora gracza), by zajść go od tyłu
                    Vector side = new Vector(-targetLook.getZ(), 0, targetLook.getX()).normalize().multiply(8.0);
                    Location behind = tLoc.clone().subtract(targetLook.clone().multiply(7.0)).add(side);
                    steerMobTowards(zombie, behind, 0.35);

                    if (dot < -0.3 && dist <= 8.0) {
                        // Jesteśmy za plecami gracza - atak!
                        stalkerStates.put(id, StalkerState.ATTACK);
                        stalkerStateTimers.put(id, now);
                    } else if (now - stateStart >= 6000) {
                        stalkerStates.put(id, StalkerState.OBSERVE);
                        stalkerStateTimers.put(id, now);
                    }
                    break;

                case ATTACK:
                    steerMobTowards(zombie, tLoc, 0.42);
                    if (dist <= 3.0) {
                        // Uderzenie z zaskoczenia
                        target.damage(8.0, zombie);
                        target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 0));
                        target.getWorld().playSound(tLoc, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 0.8f);
                        stalkerStates.put(id, StalkerState.ESCAPE);
                        stalkerStateTimers.put(id, now);
                    } else if (now - stateStart >= 3500) {
                        stalkerStates.put(id, StalkerState.ESCAPE);
                        stalkerStateTimers.put(id, now);
                    }
                    break;

                case ESCAPE:
                    Vector escapeDir = zLoc.toVector().subtract(tLoc.toVector()).normalize().multiply(2.0);
                    steerMobTowards(zombie, zLoc.clone().add(escapeDir), 0.40);
                    if (now - stateStart >= 2000) {
                        stalkerStates.put(id, StalkerState.OBSERVE);
                        stalkerStateTimers.put(id, now);
                    }
                    break;
            }
        }
    }

    /**
     * Sprawdza, czy gracz podszedł wystarczająco blisko nieprzytomnego ocalałego, by wywołać Jump-Scare.
     */
    private void tickDormantSurvivors(List<Player> players, NoiseManager noiseManager) {
        if (players == null || dormantSurvivors.isEmpty()) return;

        Iterator<UUID> it = dormantSurvivors.iterator();
        while (it.hasNext()) {
            UUID id = it.next();
            Entity entity = Bukkit.getEntity(id);
            if (!(entity instanceof Zombie zombie) || !zombie.isValid() || zombie.isDead()) {
                it.remove();
                continue;
            }

            Location zLoc = zombie.getLocation();
            Player closest = null;
            double minDstSq = 25.0; // 5 bloków

            for (Player p : players) {
                if (p != null && p.isOnline() && !p.isDead()) {
                    if (p.getWorld().equals(zLoc.getWorld())) {
                        double dstSq = p.getLocation().distanceSquared(zLoc);
                        if (dstSq <= minDstSq) {
                            closest = p;
                            break;
                        }
                    }
                }
            }

            if (closest != null) {
                it.remove(); // Ocalały budzi się!
                triggerSurvivorJumpScare(zombie, closest, noiseManager);
            }
        }
    }

    private void triggerSurvivorJumpScare(Zombie zombie, Player target, NoiseManager noiseManager) {
        zombie.setAI(true);
        zombie.setSilent(false);
        zombie.setCustomName(ChatColor.DARK_RED + "" + ChatColor.BOLD + "☠ Ocalały Zombie");
        zombie.setTarget(target);

        Location zLoc = zombie.getLocation();
        World world = zLoc.getWorld();

        // Przeraźliwe dźwięki
        world.playSound(zLoc, Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 1.2f, 0.7f);
        world.playSound(zLoc, Sound.ENTITY_GHAST_SCREAM, 1.0f, 0.6f);
        world.spawnParticle(Particle.EXPLOSION, zLoc.clone().add(0, 0.8, 0), 2);
        world.spawnParticle(Particle.SCULK_SOUL, zLoc.clone().add(0, 1.0, 0), 15, 0.4, 0.4, 0.4, 0.05);

        // Skok w stronę gracza
        Vector dir = target.getLocation().toVector().subtract(zLoc.toVector()).normalize().multiply(0.85);
        dir.setY(0.35);
        zombie.setVelocity(dir);

        target.sendMessage(ChatColor.DARK_RED + "☠ UWAGA! Ocalały okazał się zarażonym mutantem!");
        target.playSound(target.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, 1.0f, 1.0f);

        // Generuje duży hałas alarmujący pobliskie zombie
        if (noiseManager != null) {
            noiseManager.recordNoise(zLoc, 70.0, 40.0, "SURVIVOR_ROAR", zombie, this);
        }
    }

    /**
     * Pamięć zombie: jeśli mob nie ma celu w walce wręcz, bada ostatnie źródło hałasu lub tropi plamy krwi.
     */
    private void tickZombieMemories(World world, BloodTrailManager bloodManager) {
        long now = System.currentTimeMillis();

        for (LivingEntity e : world.getLivingEntities()) {
            if (!(e instanceof Zombie zombie) || !zombie.isValid() || zombie.isDead() || !zombie.hasAI()) continue;

            // Jeśli zombie już atakuje gracza w zwarciu, nie przerywamy
            LivingEntity target = zombie.getTarget();
            if (target instanceof Player && zombie.getLocation().distanceSquared(target.getLocation()) <= 16.0) {
                continue;
            }

            ZombieMemory memory = monsterMemories.get(zombie.getUniqueId());
            if (memory == null) continue;

            // 1. Sprawdzenie pamięci hałasu
            Location noiseLoc = memory.getActiveNoiseLocation();
            if (noiseLoc != null) {
                double distSq = zombie.getLocation().distanceSquared(noiseLoc);
                if (distSq > 9.0) {
                    steerMobTowards(zombie, noiseLoc, 0.28);
                } else {
                    // Dojście na miejsce hałasu - przeszukanie terenu przez 5 sekund
                    if (!memory.isInvestigating()) {
                        memory.startInvestigating(5000L);
                    } else if (now % 1000 < 500) {
                        // Rozglądanie się w lewo i prawo
                        zombie.getLocation().setYaw(zombie.getLocation().getYaw() + 45f);
                    }
                }
                continue;
            }

            // 2. Jeśli brak hałasu, sprawdzamy tropienie krwi
            if (bloodManager != null) {
                BloodTrail trail = bloodManager.findBestBloodTrailNear(zombie.getLocation(), 22.0);
                if (trail != null) {
                    memory.setBloodLocation(trail.getLocation(), 6000L);
                    steerMobTowards(zombie, trail.getLocation(), 0.25);
                }
            }
        }
    }

    /**
     * Obsługa zgonu zombie: wybuch Bloatera, drop plecaków i rozbryzg krwi.
     */
    public void handleZombieDeath(Zombie zombie, BloodTrailManager bloodManager, BackpackManager backpackManager,
                                   BattleRoyaleLootManager lootManager, InfectionManager infectionManager) {
        if (zombie == null) return;
        Location loc = zombie.getLocation();
        World world = loc.getWorld();

        // 1. Rozbryzg krwi na ziemi
        if (bloodManager != null) {
            bloodManager.onZombieKilled(loc);
        }

        // 2. Wybuch Bloatera
        if (bloaters.contains(zombie.getUniqueId()) || isZombieType(zombie, ZombieType.BLOATER)) {
            triggerBloaterExplosion(loc, world, infectionManager);
        }

        // 3. Szansa na drop plecaka z łupem
        if (backpackManager != null) {
            double roll = random.nextDouble();
            if (bloaters.contains(zombie.getUniqueId()) && roll < 0.15) {
                ItemStack bp = backpackManager.generateZombieBackpack(2, lootManager);
                world.dropItemNaturally(loc, bp);
            } else if (stalkers.contains(zombie.getUniqueId()) && roll < 0.10) {
                ItemStack bp = backpackManager.generateZombieBackpack(1, lootManager);
                world.dropItemNaturally(loc, bp);
            } else if (isZombieType(zombie, ZombieType.SURVIVOR) && roll < 0.25) {
                ItemStack bp = backpackManager.generateZombieBackpack(2, lootManager);
                world.dropItemNaturally(loc, bp);
            }
        }

        // Czyszczenie rejestrów
        UUID id = zombie.getUniqueId();
        leapers.remove(id);
        gunners.remove(id);
        stalkers.remove(id);
        bloaters.remove(id);
        dormantSurvivors.remove(id);
        monsterMemories.remove(id);
    }

    private void triggerBloaterExplosion(Location loc, World world, InfectionManager infectionManager) {
        world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.7f);
        world.playSound(loc, Sound.ENTITY_SLIME_DEATH, 1.5f, 0.6f);

        // Chmura cząsteczek gazu
        world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, loc.clone().add(0, 0.5, 0), 40, 1.5, 0.8, 1.5, 0.03);
        world.spawnParticle(Particle.SNEEZE, loc.clone().add(0, 0.8, 0), 30, 1.2, 0.6, 1.2, 0.05);

        // Trująca chmura (AreaEffectCloud) bez niszczenia terenu
        AreaEffectCloud cloud = world.spawn(loc, AreaEffectCloud.class, c -> {
            c.setRadius(3.5f);
            c.setDuration(160); // 8 sekund
            c.setParticle(Particle.SNEEZE);
            c.setColor(Color.fromRGB(80, 180, 40));
            c.addCustomEffect(new PotionEffect(PotionEffectType.POISON, 120, 1), true);
            c.addCustomEffect(new PotionEffect(PotionEffectType.WEAKNESS, 120, 0), true);
            c.addCustomEffect(new PotionEffect(PotionEffectType.SLOWNESS, 120, 1), true);
        });

        // Gracze w pobliżu wybuchu otrzymują natychmiastowy przyrost infekcji
        for (Player p : world.getPlayers()) {
            if (p.getLocation().distanceSquared(loc) <= 16.0) {
                p.sendMessage(ChatColor.RED + "[BattleRoyale] ☣ Zostałeś oblany toksycznym kwasem Spuchlaka! (+15s infekcji)");
                if (infectionManager != null) {
                    if (!infectionManager.isInfected(p)) {
                        infectionManager.infect(p);
                    }
                    RPG.BattleRoyale.Infection.InfectionState inf = infectionManager.getInfection(p.getUniqueId());
                    if (inf != null) inf.reduceSeconds(15);
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

        zombie.getWorld().playSound(zLoc, Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 1.0f, 1.4f);
        zombie.getWorld().playSound(zLoc, Sound.ENTITY_RAVAGER_ROAR, 0.7f, 1.8f);
        zombie.getWorld().spawnParticle(Particle.EXPLOSION, zLoc.clone().add(0, 0.5, 0), 1);
        zombie.getWorld().spawnParticle(Particle.CRIT, zLoc.clone().add(0, 1.0, 0), 10, 0.3, 0.3, 0.3, 0.1);
    }

    private void performGunshot(Zombie zombie, LivingEntity target) {
        Location eyeLoc = zombie.getEyeLocation();
        Location targetCenter = target.getLocation().add(0, target.getHeight() * 0.5, 0);

        Vector direction = targetCenter.toVector().subtract(eyeLoc.toVector()).normalize();

        zombie.getWorld().playSound(eyeLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 1.8f);
        zombie.getWorld().playSound(eyeLoc, Sound.ITEM_FIRECHARGE_USE, 1.0f, 0.8f);
        zombie.getWorld().spawnParticle(Particle.LARGE_SMOKE, eyeLoc.clone().add(direction.clone().multiply(0.8)), 4, 0.1, 0.1, 0.1, 0.05);
        zombie.getWorld().spawnParticle(Particle.FLAME, eyeLoc.clone().add(direction.clone().multiply(0.8)), 3, 0.05, 0.05, 0.05, 0.02);

        RayTraceResult hit = zombie.getWorld().rayTrace(
                eyeLoc, direction, 24.0, FluidCollisionMode.NEVER, true, 0.4,
                e -> e != zombie && e instanceof LivingEntity
        );

        double maxDist = 24.0;
        if (hit != null && hit.getHitPosition() != null) {
            maxDist = eyeLoc.toVector().distance(hit.getHitPosition());
        }

        for (double d = 0.5; d < maxDist; d += 0.8) {
            Location p = eyeLoc.clone().add(direction.clone().multiply(d));
            zombie.getWorld().spawnParticle(Particle.CRIT, p, 1, 0, 0, 0, 0);
        }

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

    private void steerMobTowards(Mob mob, Location targetLoc, double speed) {
        if (mob == null || targetLoc == null || !mob.isValid()) return;
        Location mLoc = mob.getLocation();
        Vector dir = targetLoc.toVector().subtract(mLoc.toVector());
        dir.setY(0);
        if (dir.lengthSquared() < 0.3) return;
        dir.normalize();

        Location inFront = mLoc.clone().add(dir.clone().multiply(0.8));
        boolean hasObstacle = inFront.getBlock().getType().isSolid();

        dir.multiply(speed);
        if (hasObstacle && mob.isOnGround()) {
            dir.setY(0.38);
        } else {
            dir.setY(mob.getVelocity().getY());
        }
        mob.setVelocity(dir);
    }

    public boolean isSpecialZombie(Entity entity) {
        if (entity == null) return false;
        UUID id = entity.getUniqueId();
        return leapers.contains(id) || gunners.contains(id) || stalkers.contains(id) || bloaters.contains(id) || dormantSurvivors.contains(id)
                || entity.getPersistentDataContainer().has(KEY_ZOMBIE_TYPE, PersistentDataType.STRING);
    }

    public boolean isZombieType(Entity entity, ZombieType type) {
        if (entity == null || type == null) return false;
        String t = entity.getPersistentDataContainer().get(KEY_ZOMBIE_TYPE, PersistentDataType.STRING);
        return type.name().equalsIgnoreCase(t);
    }

    /**
     * Obsługa śmierci zombie: rozbryzg krwi, wybuch Spuchlaka (chmura toksyczna),
     * szansa na upuszczenie plecaka z łupem przetrwania oraz czyszczenie z pamięci AI.
     */
    public void handleZombieDeath(Zombie zombie, BloodTrailManager bloodManager, BackpackManager backpackManager, BattleRoyaleLootManager lootManager, InfectionManager infectionManager) {
        if (zombie == null) return;
        UUID id = zombie.getUniqueId();
        Location loc = zombie.getLocation();

        // 1. Krew na ziemi z zabitego zombie
        if (bloodManager != null) {
            bloodManager.onZombieKilled(loc);
        }

        // 2. Czy to Bloater? Wybuch + chmura toksyczna (bez niszczenia bloków!)
        if (bloaters.contains(id) || isZombieType(zombie, ZombieType.BLOATER)) {
            triggerBloaterExplosion(loc, infectionManager);
        }

        // 3. Drop plecaka z łupem dla specjalnych typów
        if (backpackManager != null && loc.getWorld() != null) {
            double roll = random.nextDouble();
            if (isZombieType(zombie, ZombieType.STALKER) && roll < 0.10) {
                loc.getWorld().dropItemNaturally(loc, backpackManager.generateZombieBackpack(1, lootManager));
            } else if (isZombieType(zombie, ZombieType.BLOATER) && roll < 0.15) {
                loc.getWorld().dropItemNaturally(loc, backpackManager.generateZombieBackpack(2, lootManager));
            } else if (isZombieType(zombie, ZombieType.SURVIVOR) && roll < 0.25) {
                loc.getWorld().dropItemNaturally(loc, backpackManager.generateZombieBackpack(random.nextDouble() < 0.35 ? 2 : 1, lootManager));
            }
        }

        // 4. Czyszczenie ze struktur śledzących
        leapers.remove(id);
        gunners.remove(id);
        stalkers.remove(id);
        bloaters.remove(id);
        dormantSurvivors.remove(id);
        leaperCooldowns.remove(id);
        gunnerCooldowns.remove(id);
        stalkerStates.remove(id);
        stalkerStateTimers.remove(id);
        monsterMemories.remove(id);
    }

    private void triggerBloaterExplosion(Location loc, InfectionManager infectionManager) {
        World world = loc.getWorld();
        if (world == null) return;

        // Dźwięk i efekty wybuchu (BEZ niszczenia bloków!)
        world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.3f, 0.7f);
        world.playSound(loc, Sound.ENTITY_SLIME_DEATH, 1.4f, 0.5f);
        world.spawnParticle(Particle.EXPLOSION, loc.clone().add(0, 1.0, 0), 2);
        world.spawnParticle(Particle.ITEM_SLIME, loc.clone().add(0, 1.0, 0), 40, 0.6, 0.6, 0.6, 0.1);

        // Chmura toksyczna (AreaEffectCloud)
        world.spawn(loc, AreaEffectCloud.class, cloud -> {
            cloud.setRadius(4.5f);
            cloud.setRadiusOnUse(-0.1f);
            cloud.setDuration(240); // 12 sekund trwania chmury
            cloud.setColor(Color.fromRGB(30, 140, 20));
            cloud.addCustomEffect(new PotionEffect(PotionEffectType.POISON, 120, 1), true);
            cloud.addCustomEffect(new PotionEffect(PotionEffectType.WEAKNESS, 180, 0), true);
            cloud.addCustomEffect(new PotionEffect(PotionEffectType.SLOWNESS, 180, 1), true);
        });

        // Gracze w promieniu 4.5m: odrzut, obrażenia i przyspieszenie infekcji o 15s
        for (Entity nearby : world.getNearbyEntities(loc, 4.5, 3.5, 4.5)) {
            if (nearby instanceof Player player) {
                player.damage(5.5);
                Vector knockback = player.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(0.8);
                knockback.setY(0.35);
                player.setVelocity(knockback);

                if (infectionManager != null && infectionManager.isInfected(player)) {
                    infectionManager.reduceInfectionTime(player, 15);
                    player.sendMessage(ChatColor.DARK_RED + "☣ [Spuchlak] Toksyczna eksplozja przyspieszyła Twój wirus o 15 sekund!");
                }
            }
        }
    }

    public void cleanup() {
        leapers.clear();
        gunners.clear();
        stalkers.clear();
        bloaters.clear();
        dormantSurvivors.clear();
        leaperCooldowns.clear();
        gunnerCooldowns.clear();
        stalkerStates.clear();
        stalkerStateTimers.clear();
        monsterMemories.clear();
    }
}

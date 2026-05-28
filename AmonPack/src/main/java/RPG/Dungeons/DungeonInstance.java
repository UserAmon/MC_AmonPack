package RPG.Dungeons;

import RPG.Levels.Objects.LevelSkill;
import RPG.Levels.BendingTree.PlayerBendingBranch;
import Plugin.AmonPackPlugin;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class DungeonInstance {
    private final Dungeon template;
    private final UUID instanceId;
    private final World world;
    private final List<UUID> players = new ArrayList<>();
    private final Map<UUID, DungeonPlayerStats> playerStatsMap = new HashMap<>();
    private final Map<UUID, Boolean> readyPlayers = new HashMap<>();
    private final Set<UUID> spectatorPlayers = new HashSet<>();
    private final Map<Location, Set<UUID>> claimedChests = new HashMap<>();
    private final Map<Location, Map<UUID, DungeonLootChest>> chestGuis = new HashMap<>();

    private String activeEncounterId;
    private final Map<String, Integer> killedMobsCounter = new HashMap<>();
    private final Map<Location, String> activeLootChests = new HashMap<>();

    private boolean randomPhaseActive = false;
    private int randomClearsCount = 0;
    private int reqClears = 0;
    private String encAfterClears = null;
    private final List<String> randomExcludes = new ArrayList<>();
    private final Set<String> completedEncounters = new HashSet<>();

    private boolean isFinished = false;

    private org.bukkit.boss.BossBar bossBar;
    private final Map<DungeonCondition, Integer> zoneCaptureCounters = new HashMap<>();
    private final Map<DungeonCondition, Integer> throwHitsCounter = new HashMap<>();
    private final Set<UUID> activeShieldedEnemyUuids = new HashSet<>();
    private final Set<UUID> spawnedMobUuids = new HashSet<>();
    private long encounterStartTime = 0;
    private final List<Integer> spawnUntilTaskIds = new ArrayList<>();
    private final Set<Integer> completedConditionsIndices = new HashSet<>();
    private final List<String> randomPool = new ArrayList<>();

    private boolean poolListPhaseActive = false;
    private List<String> currentPoolListSequence = new ArrayList<>();
    private int poolListIndex = 0;

    private final Map<DungeonCondition, Integer> periodicCheckTimers = new HashMap<>();
    private final Map<DungeonCondition, Boolean> lookingStateMap = new HashMap<>();
    private final Map<DungeonCondition, Integer> lookingTimerMap = new HashMap<>();
    private final Map<DungeonCondition, Boolean> aliveStateMap = new HashMap<>();
    private final Map<DungeonPlatform, Boolean> activePlatformsState = new HashMap<>();
    private int regenTickTimer = 0;
    private final Map<DungeonPlatform, Integer> platformCheckTimers = new HashMap<>();
    private final Map<DungeonPlatform, Integer> platformDelayTimers = new HashMap<>();
    private final Map<DungeonPlatform, Boolean> platformTargetStates = new HashMap<>();
    private final Map<UUID, Location> playerLastLocations = new HashMap<>();


    public DungeonInstance(Dungeon template, List<Player> party) {
        this.template = template;
        this.instanceId = UUID.randomUUID();
        this.world = DungeonWorldManager.createDungeonWorld(template.getId());

        for (Player p : party) {
            this.players.add(p.getUniqueId());
            this.playerStatsMap.put(p.getUniqueId(), new DungeonPlayerStats(p.getUniqueId(), p.getName()));
            this.readyPlayers.put(p.getUniqueId(), false);
        }

        this.activeEncounterId = template.getInitialEncounterId();
    }

    public void start() {
        if (world == null) {
            broadcast(ChatColor.RED + "[Dungeons] Blad krytyczny: Nie udalo sie wygenerowac swiata!");
            cleanup();
            return;
        }

        for (UUID uuid : players) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                DungeonInventoryBackup.backupAndClearInventory(player, AmonPackPlugin.plugin);
            }
        }

        Vector paste = template.getPasteLocation();
        boolean pasted = SchematicManager.pasteSchematic(world, template.getSchematicFile(), paste.getBlockX(),
                paste.getBlockY(), paste.getBlockZ(), AmonPackPlugin.plugin);

        if (!pasted) {
            broadcast(ChatColor.RED + "[Dungeons] Blad krytyczny: Nie udalo sie wkleic schematu terenu!");
            cleanup();
            return;
        }

        Vector spawn = template.getSpawnLocation();
        Location spawnLoc = new Location(world, spawn.getX(), spawn.getY(), spawn.getZ());

        for (UUID uuid : players) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                player.teleport(spawnLoc);
                player.setGameMode(GameMode.SURVIVAL);
                player.setHealth(20.0);
                player.setFoodLevel(20);

                DungeonPlayerStats stats = playerStatsMap.get(uuid);
                stats.applyStatsToPlayer(player);
            }
        }

        for (DungeonPlatform platform : template.getPlatforms()) {
            boolean met = checkPlatformRequirement(platform);
            boolean shouldExist = met ^ platform.isInverted();
            activePlatformsState.put(platform, shouldExist);
            updatePlatformBlocksInWorld(platform, shouldExist, false);
        }

        executeActiveEncounterEffects();
    }

    public void update() {
        if (isFinished)
            return;

        for (Player p : getOnlinePlayers()) {
            DungeonPlayerStats stats = getPlayerStats(p);
            if (stats != null) {
                int charge = stats.getAmonGloveCharge();
                if (charge > 0) {
                    long lastChange = stats.getAmonGloveLastChangeTime();
                    long now = System.currentTimeMillis();
                    if (now - lastChange >= 4000) {
                        int newCharge = charge - 1;
                        stats.setAmonGloveCharge(newCharge);
                        stats.setAmonGloveLastChangeTime(now);
                    }
                }
                org.bukkit.boss.BossBar bar = stats.getAmonGloveBar();
                if (bar != null) {
                    org.bukkit.inventory.ItemStack held = p.getInventory().getItemInMainHand();
                    boolean holdingGlove = false;
                    if (held != null && held.getType() == Material.STONE_BUTTON && held.hasItemMeta()) {
                        org.bukkit.persistence.PersistentDataContainer pdc = held.getItemMeta().getPersistentDataContainer();
                        org.bukkit.NamespacedKey nkey = new org.bukkit.NamespacedKey(AmonPackPlugin.plugin, "dungeon_weapon_type");
                        if (pdc.has(nkey, org.bukkit.persistence.PersistentDataType.STRING)) {
                            if ("AMON_GLOVE".equals(pdc.get(nkey, org.bukkit.persistence.PersistentDataType.STRING))) {
                                holdingGlove = true;
                            }
                        }
                    }
                    if (holdingGlove && stats.getAmonGloveCharge() > 0) {
                        updateGloveBossBar(p, stats);
                    } else {
                        bar.setVisible(false);
                    }
                }

                int maceLvl = stats.getBlessingLevel("EARTH_MACE");
                if (maceLvl >= 3 && stats.hasWeaponInInventory(p, "EARTH_MACE")) {
                    if (p.isOnGround()) {
                        double currentDur = stats.getWeaponDurability("EARTH_MACE");
                        stats.setWeaponDurability("EARTH_MACE", currentDur + 10.0);
                    }
                }

                int sickleLvl = stats.getBlessingLevel("WIND_SICKLE");
                if (sickleLvl > 0 && stats.hasWeaponInInventory(p, "WIND_SICKLE")) {
                    Location lastLoc = playerLastLocations.get(p.getUniqueId());
                    Location curLoc = p.getLocation();
                    boolean moved = false;
                    if (lastLoc != null && lastLoc.getWorld() == curLoc.getWorld()) {
                        if (lastLoc.distanceSquared(curLoc) > 0.02) {
                            moved = true;
                        }
                    }
                    playerLastLocations.put(p.getUniqueId(), curLoc.clone());

                    if (moved) {
                        double pct = 3.0;
                        if (sickleLvl >= 3) {
                            com.projectkorra.projectkorra.BendingPlayer bPlayer = com.projectkorra.projectkorra.BendingPlayer.getBendingPlayer(p);
                            if (bPlayer != null && bPlayer.hasElement(com.projectkorra.projectkorra.Element.getElement("Air"))) {
                                pct = 6.0;
                            } else {
                                pct = 4.5;
                            }
                        } else if (sickleLvl == 2) {
                            pct = 4.5;
                        }
                        double currentDur = stats.getWeaponDurability("WIND_SICKLE");
                        stats.setWeaponDurability("WIND_SICKLE", currentDur + pct);
                    }
                    stats.applyStatsToPlayer(p);
                }

                int staffLvl = stats.getBlessingLevel("WATER_STAFF");
                if (staffLvl > 0 && stats.hasWeaponInInventory(p, "WATER_STAFF")) {
                    ItemStack held = p.getInventory().getItemInMainHand();
                    boolean holdingStaff = false;
                    if (held != null && held.hasItemMeta()) {
                        org.bukkit.persistence.PersistentDataContainer pdc = held.getItemMeta().getPersistentDataContainer();
                        org.bukkit.NamespacedKey nkey = new org.bukkit.NamespacedKey(AmonPackPlugin.plugin, "dungeon_weapon_type");
                        if (pdc.has(nkey, org.bukkit.persistence.PersistentDataType.STRING) && "WATER_STAFF".equals(pdc.get(nkey, org.bukkit.persistence.PersistentDataType.STRING))) {
                            holdingStaff = true;
                        }
                    }

                    if (!holdingStaff) {
                        double chargeAmt = 2.0;
                        if (staffLvl >= 3) {
                            com.projectkorra.projectkorra.BendingPlayer bPlayer = com.projectkorra.projectkorra.BendingPlayer.getBendingPlayer(p);
                            if (bPlayer != null && bPlayer.hasElement(com.projectkorra.projectkorra.Element.getElement("Air"))) {
                                chargeAmt = 3.0;
                            }
                        }
                        double currentDur = stats.getWeaponDurability("WATER_STAFF");
                        stats.setWeaponDurability("WATER_STAFF", currentDur + chargeAmt);
                    } else if (p.isSneaking()) {
                        double currentDur = stats.getWeaponDurability("WATER_STAFF");
                        double cost = 24.0;
                        if (staffLvl >= 3) {
                            com.projectkorra.projectkorra.BendingPlayer bPlayer = com.projectkorra.projectkorra.BendingPlayer.getBendingPlayer(p);
                            if (bPlayer != null && bPlayer.hasElement(com.projectkorra.projectkorra.Element.getElement("Earth"))) {
                                cost = 33.0;
                            }
                        }

                        if (currentDur >= cost) {
                            stats.setWeaponDurability("WATER_STAFF", currentDur - cost);
                            triggerWaterStaffAura(p, staffLvl);
                        } else {
                            p.sendMessage(ChatColor.RED + "[Kostur Wody] Za malo energii do podtrzymania aury!");
                        }
                    }
                }

                for (String key : new String[]{"EARTH_MACE", "WIND_SICKLE", "WATER_STAFF"}) {
                    if (stats.getBlessingLevel(key) > 0) {
                        ItemStack item = getLegendaryWeaponItem(p, key);
                        if (item != null) {
                            updateVisualDurability(item, stats.getWeaponDurability(key));
                        }
                    }
                }

                ItemStack offhand = p.getInventory().getItemInOffHand();
                if (offhand != null && offhand.hasItemMeta()) {
                    org.bukkit.persistence.PersistentDataContainer pdc = offhand.getItemMeta().getPersistentDataContainer();
                    org.bukkit.NamespacedKey nkey = new org.bukkit.NamespacedKey(AmonPackPlugin.plugin, "dungeon_weapon_type");
                    if (pdc.has(nkey, org.bukkit.persistence.PersistentDataType.STRING) && "WIND_SICKLE_OFFHAND".equals(pdc.get(nkey, org.bukkit.persistence.PersistentDataType.STRING))) {
                        updateVisualDurability(offhand, stats.getWeaponDurability("WIND_SICKLE"));
                    }
                }
            }
        }

        regenTickTimer++;
        if (regenTickTimer >= 10) {
            regenTickTimer = 0;
            for (Player p : getOnlinePlayers()) {
                if (!isPlayerSpectator(p)) {
                    DungeonPlayerStats stats = getPlayerStats(p);
                    if (stats != null && stats.getRegenLevel() > 0) {
                        double maxHP = p.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
                        double curHP = p.getHealth();
                        double heal = maxHP * (0.10 * stats.getRegenLevel());
                        p.setHealth(Math.min(maxHP, curHP + heal));
                        p.getWorld().spawnParticle(Particle.HEART, p.getLocation().add(0, 1.5, 0), 2, 0.2, 0.2, 0.2, 0.0);
                    }
                }
            }
        }

        List<Player> inWorld = world.getPlayers();
        if (inWorld.isEmpty()) {
            System.out.println("[Dungeons] Brak graczy w swiecie dungeonu: " + world.getName() + ". Czyszczenie...");
            cleanup();
            return;
        }

        boolean allSpectators = true;
        for (Player p : inWorld) {
            if (!isPlayerSpectator(p)) {
                allSpectators = false;
                break;
            }
        }

        if (allSpectators) {
            broadcast(ChatColor.RED + "[Dungeons] Wszyscy gracze polegli! Dungeon zakancza sie porazka.");
            cleanup();
            return;
        }

        Encounter encounter = getActiveEncounter();
        if (encounter != null) {
            if (System.currentTimeMillis() - encounterStartTime >= 3000) {
                for (DungeonCondition condition : encounter.getConditions()) {
                    if (condition.getType() == DungeonCondition.ConditionType.THROW_AT_ENEMY) {
                        boolean bossStillAlive = false;
                        if (!activeShieldedEnemyUuids.isEmpty()) {
                            for (UUID uuid : activeShieldedEnemyUuids) {
                                org.bukkit.entity.Entity entity = Bukkit.getEntity(uuid);
                                if (entity instanceof org.bukkit.entity.LivingEntity && !entity.isDead() && entity.isValid()) {
                                    bossStillAlive = true;
                                    break;
                                }
                            }
                        } else {
                            for (org.bukkit.entity.LivingEntity le : world.getLivingEntities()) {
                                if (!(le instanceof Player)) {
                                    String cleanName = ChatColor.stripColor(le.getName());
                                    if (cleanName.equalsIgnoreCase(condition.getMobName()) || le.getType().name().equalsIgnoreCase(condition.getMobName())) {
                                        if (!le.isDead() && le.isValid()) {
                                            bossStillAlive = true;
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                        if (!bossStillAlive) {
                            int req = condition.getAmount();
                            if (getThrowHits(condition) < req) {
                                throwHitsCounter.put(condition, req);
                                activeShieldedEnemyUuids.clear();
                            }
                        }
                    }
                }
            }

            List<DungeonCondition> conds = encounter.getConditions();
            for (int i = 0; i < conds.size(); i++) {
                DungeonCondition condition = conds.get(i);
                if (condition.isMet(this)) {
                    if (!completedConditionsIndices.contains(i)) {
                        completedConditionsIndices.add(i);
                        for (DungeonEffect effect : condition.getOnCompleteEffects()) {
                            effect.execute(this);
                        }
                    }
                }
            }

            for (DungeonCondition condition : encounter.getConditions()) {
                if (condition.getType() == DungeonCondition.ConditionType.ZONE) {
                    double cx = condition.getX();
                    double cy = condition.getY();
                    double cz = condition.getZ();
                    double radius = condition.getRadius();
                    Location center = new Location(world, cx, cy, cz);
                    
                    for (double angle = 0; angle < 2 * Math.PI; angle += Math.PI / 16) {
                        double px = cx + radius * Math.cos(angle);
                        double pz = cz + radius * Math.sin(angle);
                        world.spawnParticle(Particle.GLOW, px, cy + 0.1, pz, 1, 0, 0, 0, 0);
                    }
                    
                    boolean playerInZone = false;
                    for (Player player : getOnlinePlayers()) {
                        if (!isPlayerSpectator(player) && player.getLocation().distanceSquared(center) <= radius * radius) {
                            playerInZone = true;
                            break;
                        }
                    }
                    
                    if (playerInZone) {
                        incrementZoneProgress(condition);
                    } else {
                        int current = getZoneProgress(condition);
                        if (current > 0) {
                            zoneCaptureCounters.put(condition, current - 1);
                        }
                    }
                }
            }

            if (!activeShieldedEnemyUuids.isEmpty()) {
                double time = System.currentTimeMillis() / 1000.0;
                double angle = (System.currentTimeMillis() / 200.0) % (2 * Math.PI);
                for (UUID uuid : activeShieldedEnemyUuids) {
                    org.bukkit.entity.Entity entity = Bukkit.getEntity(uuid);
                    if (entity instanceof org.bukkit.entity.LivingEntity && !entity.isDead()) {
                        Location center = entity.getLocation().add(0, 1, 0);
                        for (int i = 0; i < 8; i++) {
                            double finalAngle = angle + (i * Math.PI / 4);
                            double px = 1.2 * Math.cos(finalAngle);
                            double pz = 1.2 * Math.sin(finalAngle);
                            world.spawnParticle(Particle.SOUL_FIRE_FLAME, center.getX() + px, center.getY(), center.getZ() + pz, 1, 0, 0, 0, 0);
                        }
                        for (int i = 0; i < 8; i++) {
                            double finalAngle = -angle + (i * Math.PI / 4);
                            double px = 1.2 * Math.cos(finalAngle);
                            double py = 1.2 * Math.sin(finalAngle);
                            world.spawnParticle(Particle.ELECTRIC_SPARK, center.getX() + px, center.getY() + py, center.getZ(), 1, 0, 0, 0, 0);
                        }
                        double haloY = center.getY() + 1.2;
                        for (int i = 0; i < 6; i++) {
                            double hAngle = (time * 3.0) + (i * Math.PI / 3.0);
                            double hx = 0.4 * Math.cos(hAngle);
                            double hz = 0.4 * Math.sin(hAngle);
                            world.spawnParticle(Particle.GLOW, center.getX() + hx, haloY, center.getZ() + hz, 1, 0, 0, 0, 0);
                        }
                    }
                }
            }

            if (bossBar != null && bossBar.isVisible()) {
                String titleTemplate = encounter.getTitle();
                if (titleTemplate != null) {
                    int act = 0;
                    int req = 1;
                    for (DungeonCondition condition : encounter.getConditions()) {
                        if (condition.getType() == DungeonCondition.ConditionType.KILL_MOBS) {
                            act = getKilledMobsCount(condition.getMobName());
                            req = condition.getAmount();
                            break;
                        } else if (condition.getType() == DungeonCondition.ConditionType.ZONE) {
                            act = getZoneProgress(condition);
                            req = condition.getTimeRequired();
                            break;
                        } else if (condition.getType() == DungeonCondition.ConditionType.THROW_AT || condition.getType() == DungeonCondition.ConditionType.THROW_AT_ENEMY) {
                            act = getThrowHits(condition);
                            req = condition.getAmount();
                            break;
                        }
                    }
                    int remaining = 0;
                    for (DungeonCondition condition : encounter.getConditions()) {
                        if (condition.getType() == DungeonCondition.ConditionType.PERIODIC_CHECK) {
                            remaining = periodicCheckTimers.getOrDefault(condition, 0);
                            break;
                        }
                    }
                    String titleText = titleTemplate
                        .replace("$ActNumber$", String.valueOf(act))
                        .replace("$ReqNumber$", String.valueOf(req))
                        .replace("%time%", String.valueOf(remaining));
                    bossBar.setTitle(ChatColor.translateAlternateColorCodes('&', titleText));
                    double progress = (double) act / req;
                    bossBar.setProgress(Math.max(0.0, Math.min(1.0, progress)));
                }
            }

            for (DungeonCondition condition : encounter.getConditions()) {
                if (condition.getType() == DungeonCondition.ConditionType.PERIODIC_CHECK) {
                    Integer rem = periodicCheckTimers.get(condition);
                    if (rem != null) {
                        rem--;
                        if (rem <= 0) {
                            boolean met = checkPeriodicRequirement(condition.getRequirement());
                            if (met) {
                                for (DungeonEffect eff : condition.getSuccessEffects()) {
                                    eff.execute(this);
                                }
                                if (condition.isOnce()) {
                                    periodicCheckTimers.remove(condition);
                                    continue;
                                }
                            } else {
                                for (DungeonEffect eff : condition.getFailEffects()) {
                                    eff.execute(this);
                                }
                            }
                            rem = condition.getInterval();
                        }
                        periodicCheckTimers.put(condition, rem);
                    }
                } else if (condition.getType() == DungeonCondition.ConditionType.LOOKING_AT) {
                    boolean currentLook = false;
                    for (Player p : getOnlinePlayers()) {
                        if (!isPlayerSpectator(p)) {
                            double distSq = p.getLocation().distanceSquared(new Location(world, condition.getX(), condition.getY(), condition.getZ()));
                            double maxDist = condition.getRadius();
                            if (maxDist > 0.0 && distSq > maxDist * maxDist) {
                                continue;
                            }
                            if (isLookingAt(p, condition.getX(), condition.getY(), condition.getZ())) {
                                currentLook = true;
                                break;
                            }
                        }
                    }
                    Boolean prevLook = lookingStateMap.get(condition);
                    if (prevLook == null) prevLook = false;
                    if (currentLook != prevLook) {
                        lookingStateMap.put(condition, currentLook);
                        if (currentLook) {
                            for (DungeonEffect eff : condition.getSuccessEffects()) {
                                  eff.execute(this);
                            }
                        } else {
                            for (DungeonEffect eff : condition.getFailEffects()) {
                                  eff.execute(this);
                            }
                        }
                    }

                    if (!currentLook) {
                        Integer timer = lookingTimerMap.get(condition);
                        if (timer != null) {
                            timer--;
                            if (timer <= 0) {
                                for (DungeonEffect eff : condition.getFailEffects()) {
                                    eff.execute(this);
                                }
                                timer = condition.getInterval();
                            }
                            lookingTimerMap.put(condition, timer);
                        }
                    } else {
                        lookingTimerMap.put(condition, condition.getInterval());
                    }
                } else if (condition.getType() == DungeonCondition.ConditionType.ALIVE) {
                    boolean currentAlive = condition.isMet(this);
                    Boolean prevAlive = aliveStateMap.get(condition);
                    if (prevAlive == null) prevAlive = false;
                    if (currentAlive != prevAlive) {
                        aliveStateMap.put(condition, currentAlive);
                        if (currentAlive) {
                            for (DungeonEffect eff : condition.getSuccessEffects()) {
                                eff.execute(this);
                            }
                        } else {
                            for (DungeonEffect eff : condition.getFailEffects()) {
                                eff.execute(this);
                            }
                        }
                    }
                }
            }

            for (DungeonPlatform platform : template.getPlatforms()) {
                tickPlatform(platform);
            }

            for (DungeonPlatform platform : encounter.getPlatforms()) {
                tickPlatform(platform);
            }

            for (Player p : getOnlinePlayers()) {
                if (!isPlayerSpectator(p)) {
                    playerLastLocations.put(p.getUniqueId(), p.getLocation().clone());
                }
            }

            boolean allMet = true;
            for (DungeonCondition condition : encounter.getConditions()) {
                if (!condition.isMet(this)) {
                    allMet = false;
                    break;
                }
            }

            if (allMet && !encounter.getConditions().isEmpty()) {
                transitionToNext();
            }
        }
    }

    public void transitionToNext() {
        Encounter current = getActiveEncounter();
        if (current == null)
            return;

        if (!current.isLeaveMobs()) {
            for (UUID uuid : spawnedMobUuids) {
                org.bukkit.entity.Entity entity = Bukkit.getEntity(uuid);
                if (entity instanceof org.bukkit.entity.LivingEntity && !entity.isDead()) {
                    ((org.bukkit.entity.LivingEntity) entity).setHealth(0.0);
                }
            }
        }
        spawnedMobUuids.clear();

        System.out.println("[Dungeons] Zrealizowano etap: " + current.getId() + " (" + current.getDescription()
                + ") na " + world.getName());

        if (isFinished)
            return;

        completedEncounters.add(current.getId());
        clearSpawnUntilTasks();
        completedConditionsIndices.clear();

        String nextId = null;

        if (poolListPhaseActive) {
            poolListIndex++;
            if (poolListIndex >= currentPoolListSequence.size()) {
                poolListPhaseActive = false;
                nextId = encAfterClears;
                System.out.println("[Dungeons] Sekwencja pool_lists ukonczona! Nastepny: " + nextId);
            } else {
                nextId = currentPoolListSequence.get(poolListIndex);
                System.out.println("[Dungeons] Pool list krok " + (poolListIndex + 1) + "/" + currentPoolListSequence.size() + ": " + nextId);
            }
        } else if (randomPhaseActive) {
            randomClearsCount++;
            System.out.println("[Dungeons] Postep fazy losowej: " + randomClearsCount + "/" + reqClears);

            if (randomClearsCount >= reqClears) {
                randomPhaseActive = false;
                nextId = encAfterClears;
                System.out.println("[Dungeons] Faza losowa ukonczona! Nastepny etap: " + nextId);
            } else {
                List<String> eligible = new ArrayList<>();
                Collection<String> sourceCollection = randomPool.isEmpty() ? template.getEncounters().keySet() : randomPool;
                for (String id : sourceCollection) {
                    if (!completedEncounters.contains(id) && !randomExcludes.contains(id)) {
                        eligible.add(id);
                    }
                }

                if (eligible.isEmpty()) {
                    completedEncounters.clear();
                    completedEncounters.add(current.getId());
                    for (String id : sourceCollection) {
                        if (!completedEncounters.contains(id) && !randomExcludes.contains(id)) {
                            eligible.add(id);
                        }
                    }
                }

                if (eligible.isEmpty()) {
                    randomPhaseActive = false;
                    nextId = encAfterClears;
                } else {
                    nextId = eligible.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(eligible.size()));
                }
            }
        } else if (!current.getNextEncounters().isEmpty()
                && current.getNextEncounters().get(0).equalsIgnoreCase("random")) {

            List<List<String>> poolLists = current.getPoolLists();
            if (!poolLists.isEmpty()) {
                List<List<String>> eligible = new ArrayList<>();
                for (List<String> candidate : poolLists) {
                    boolean available = true;
                    for (String encId : candidate) {
                        if (completedEncounters.contains(encId)) {
                            available = false;
                            break;
                        }
                    }
                    if (available) {
                        eligible.add(candidate);
                    }
                }

                if (eligible.isEmpty()) {
                    nextId = current.getEncAfterClears();
                    System.out.println("[Dungeons] Brak dostepnych pool_lists! Nastepny: " + nextId);
                } else {
                    List<String> chosen = eligible.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(eligible.size()));
                    poolListPhaseActive = true;
                    currentPoolListSequence = new ArrayList<>(chosen);
                    poolListIndex = 0;
                    encAfterClears = current.getEncAfterClears();
                    nextId = currentPoolListSequence.get(0);
                    System.out.println("[Dungeons] Wybrana lista pool_lists " + chosen + ". Start: " + nextId);
                }
            } else {
                randomPhaseActive = true;
                randomClearsCount = 0;
                reqClears = current.getReqClears();
                encAfterClears = current.getEncAfterClears();
                randomExcludes.clear();
                randomExcludes.addAll(current.getExclude());
                randomPool.clear();
                randomPool.addAll(current.getPool());

                System.out.println("[Dungeons] Inicjalizacja fazy losowej! Wymagane ukonczenia: " + reqClears
                        + ", Nastepny cel po zakonczeniu: " + encAfterClears);

                List<String> eligiblePool = new ArrayList<>();
                Collection<String> sourceCollection = randomPool.isEmpty() ? template.getEncounters().keySet() : randomPool;
                for (String id : sourceCollection) {
                    if (!completedEncounters.contains(id) && !randomExcludes.contains(id)) {
                        eligiblePool.add(id);
                    }
                }

                if (eligiblePool.isEmpty()) {
                    randomPhaseActive = false;
                    nextId = encAfterClears;
                } else {
                    nextId = eligiblePool.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(eligiblePool.size()));
                }
            }
        } else {
            List<String> nextList = current.getNextEncounters();
            if (nextList == null || nextList.isEmpty()) {
                completeDungeon();
                return;
            }

            if (nextList.size() == 1) {
                nextId = nextList.get(0);
            } else {
                nextId = nextList.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(nextList.size()));
            }
        }

        if (nextId == null || nextId.isEmpty()) {
            completeDungeon();
            return;
        }

        killedMobsCounter.clear();

        for (UUID uuid : readyPlayers.keySet()) {
            readyPlayers.put(uuid, false);
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                p.getInventory().remove(Material.COMPASS);
            }
        }

        this.activeEncounterId = nextId;
        System.out.println("[Dungeons] Aktywowano nowy etap: " + nextId + " na " + world.getName());

        executeActiveEncounterEffects();
    }

    private void executeActiveEncounterEffects() {
        encounterStartTime = System.currentTimeMillis();
        Encounter encounter = getActiveEncounter();
        if (encounter != null) {
            periodicCheckTimers.clear();
            lookingStateMap.clear();
            lookingTimerMap.clear();
            aliveStateMap.clear();
            activePlatformsState.clear();

            for (DungeonCondition condition : encounter.getConditions()) {
                if (condition.getType() == DungeonCondition.ConditionType.PERIODIC_CHECK) {
                    periodicCheckTimers.put(condition, condition.getInterval());
                } else if (condition.getType() == DungeonCondition.ConditionType.LOOKING_AT) {
                    lookingStateMap.put(condition, false);
                    lookingTimerMap.put(condition, condition.getInterval());
                } else if (condition.getType() == DungeonCondition.ConditionType.ALIVE) {
                    aliveStateMap.put(condition, false);
                }
            }

            for (DungeonPlatform platform : template.getPlatforms()) {
                boolean met = checkPlatformRequirement(platform);
                boolean shouldExist = met ^ platform.isInverted();
                activePlatformsState.put(platform, shouldExist);
                updatePlatformBlocksInWorld(platform, shouldExist, false);
            }

            for (DungeonPlatform platform : encounter.getPlatforms()) {
                boolean met = checkPlatformRequirement(platform);
                boolean shouldExist = met ^ platform.isInverted();
                activePlatformsState.put(platform, shouldExist);
                updatePlatformBlocksInWorld(platform, shouldExist, false);
            }

            if (encounter.getTitle() != null && !encounter.getTitle().isEmpty()) {
                if (bossBar == null) {
                    bossBar = Bukkit.createBossBar(encounter.getTitle(), org.bukkit.boss.BarColor.RED, org.bukkit.boss.BarStyle.SOLID);
                } else {
                    bossBar.setTitle(encounter.getTitle());
                }
                bossBar.removeAll();
                for (Player p : getOnlinePlayers()) {
                    bossBar.addPlayer(p);
                }
                bossBar.setVisible(true);
            } else {
                if (bossBar != null) {
                    bossBar.removeAll();
                    bossBar.setVisible(false);
                }
            }

            for (DungeonCondition condition : encounter.getConditions()) {
                if (condition.getType() == DungeonCondition.ConditionType.THROW_AT_ENEMY) {
                    String command = "mm mobs spawn -s " + condition.getMobName() + ":1 1 " +
                                     world.getName() + "," + condition.getX() + "," + condition.getY() + "," + condition.getZ();
                    Bukkit.dispatchCommand(Bukkit.getServer().getConsoleSender(), command);
                    
                    new org.bukkit.scheduler.BukkitRunnable() {
                        @Override
                        public void run() {
                            Location loc = new Location(world, condition.getX(), condition.getY(), condition.getZ());
                            for (org.bukkit.entity.Entity entity : world.getNearbyEntities(loc, 3.0, 3.0, 3.0)) {
                                if (entity instanceof org.bukkit.entity.LivingEntity && !(entity instanceof Player)) {
                                    String name = entity.getName();
                                    String cleanName = ChatColor.stripColor(name);
                                    if (cleanName.equalsIgnoreCase(condition.getMobName()) || entity.getType().name().equalsIgnoreCase(condition.getMobName())) {
                                        activeShieldedEnemyUuids.add(entity.getUniqueId());
                                    }
                                }
                            }
                        }
                    }.runTaskLater(Plugin.AmonPackPlugin.plugin, 2L);
                }
            }

            for (DungeonEffect effect : encounter.getEffects()) {
                if (effect.getType() != DungeonEffect.EffectType.COMPLETE_DUNGEON) {
                    effect.execute(this);
                }
            }
        }
    }

    private boolean checkPeriodicRequirement(String requirement) {
        if (requirement == null || requirement.isEmpty()) return true;
        List<Player> active = new ArrayList<>();
        for (Player p : getOnlinePlayers()) {
            if (!isPlayerSpectator(p)) {
                active.add(p);
            }
        }
        if (active.isEmpty()) return true;

        String upper = requirement.toUpperCase();
        if (upper.startsWith("AWAY_FROM_EACH_OTHER")) {
            double minDist = 8.0;
            String[] parts = requirement.split(",");
            if (parts.length > 1) {
                try {
                    minDist = Double.parseDouble(parts[1].trim());
                } catch (NumberFormatException e) {}
            }
            double minDistSq = minDist * minDist;
            for (Player p1 : active) {
                for (Player p2 : active) {
                    if (p1 != p2 && p1.getLocation().distanceSquared(p2.getLocation()) < minDistSq) {
                        return false;
                    }
                }
            }
            return true;
        }

        if (upper.startsWith("HAVE_ITEMS:")) {
            String details = requirement.substring(11).trim();
            String[] itemEntries = details.split(";");
            for (Player p : active) {
                boolean playerHasAll = true;
                for (String entry : itemEntries) {
                    String[] parts = entry.split(",");
                    String itemId = parts[0].trim();
                    int reqAmount = 1;
                    if (parts.length > 1) {
                        try {
                            reqAmount = Integer.parseInt(parts[1].trim());
                        } catch (NumberFormatException e) {}
                    }
                    if (getItemCount(p, itemId) < reqAmount) {
                        playerHasAll = false;
                        break;
                    }
                }
                if (playerHasAll) {
                    return true;
                }
            }
            return false;
        }

        if (upper.startsWith("ALL_HAVE_ITEMS:")) {
            String details = requirement.substring(15).trim();
            String[] itemEntries = details.split(";");
            for (Player p : active) {
                for (String entry : itemEntries) {
                    String[] parts = entry.split(",");
                    String itemId = parts[0].trim();
                    int reqAmount = 1;
                    if (parts.length > 1) {
                        try {
                            reqAmount = Integer.parseInt(parts[1].trim());
                        } catch (NumberFormatException e) {}
                    }
                    if (getItemCount(p, itemId) < reqAmount) {
                        return false;
                    }
                }
            }
            return true;
        }

        for (Player p : active) {
            boolean ok = false;
            switch (upper) {
                case "SNEAKING":
                    ok = p.isSneaking();
                    break;
                case "IN_AIR":
                case "AIR":
                    ok = !p.isOnGround();
                    break;
                case "SPRINTING":
                    ok = p.isSprinting();
                    break;
                case "MOVING":
                    Location last = playerLastLocations.get(p.getUniqueId());
                    if (last != null) {
                        ok = p.getLocation().distanceSquared(last) > 0.005;
                    } else {
                        ok = false;
                    }
                    break;
                case "NEAR_EACH_OTHER":
                    ok = true;
                    for (Player other : active) {
                        if (p != other && p.getLocation().distanceSquared(other.getLocation()) > 36.0) {
                            ok = false;
                            break;
                        }
                    }
                    break;
                default:
                    ok = true;
                    break;
            }
            if (!ok) return false;
        }
        return true;
    }

    private int getItemCount(Player p, String itemId) {
        int total = 0;
        for (ItemStack stack : p.getInventory().getContents()) {
            if (stack != null && !stack.getType().isAir() && isMatchingItem(stack, itemId)) {
                total += stack.getAmount();
            }
        }
        return total;
    }

    private boolean checkPlatformRequirement(DungeonPlatform platform) {
        String req = platform.getRequirement();
        if (req == null || req.isEmpty()) return true;
        if (req.equalsIgnoreCase("SNEAKING")) {
            if (platform.getTestMode().equalsIgnoreCase("NEAR")) {
                for (Player p : getOnlinePlayers()) {
                    if (!isPlayerSpectator(p) && isNearPlatform(p.getLocation(), platform) && p.isSneaking()) {
                        return true;
                    }
                }
            } else {
                for (Player p : getOnlinePlayers()) {
                    if (!isPlayerSpectator(p) && p.isSneaking()) {
                        return true;
                    }
                }
            }
            return false;
        }
        if (req.startsWith("ITEM:")) {
            String itemId = req.substring(5).trim();
            if (platform.getTestMode().equalsIgnoreCase("NEAR")) {
                for (Player p : getOnlinePlayers()) {
                    if (!isPlayerSpectator(p) && isNearPlatform(p.getLocation(), platform) && hasItemInHandOrInv(p, itemId)) {
                        return true;
                    }
                }
            } else {
                for (Player p : getOnlinePlayers()) {
                    if (!isPlayerSpectator(p) && hasItemInHandOrInv(p, itemId)) {
                        return true;
                    }
                }
            }
            return false;
        }
        if (req.startsWith("MOB_NEAR:")) {
            String targetMobName = req.substring(9).trim();
            Location center = getPlatformCenter(platform);
            for (org.bukkit.entity.Entity entity : world.getNearbyEntities(center, 5.0, 5.0, 5.0)) {
                if (entity instanceof org.bukkit.entity.LivingEntity && !(entity instanceof Player) && !entity.isDead()) {
                    String cleanName = ChatColor.stripColor(entity.getName());
                    if (cleanName.equalsIgnoreCase(targetMobName) || entity.getType().name().equalsIgnoreCase(targetMobName)) {
                        return true;
                    }
                }
            }
            return false;
        }
        return true;
    }

    private void tickPlatform(DungeonPlatform platform) {
        Integer interval = platform.getCheckInterval();
        boolean shouldEvaluate = true;
        if (interval != null && interval > 0) {
            int ticks = platformCheckTimers.getOrDefault(platform, 0) + 1;
            if (ticks < interval) {
                platformCheckTimers.put(platform, ticks);
                shouldEvaluate = false;
            } else {
                platformCheckTimers.put(platform, 0);
            }
        }
        Boolean currentActualState = activePlatformsState.get(platform);
        if (currentActualState == null) {
            currentActualState = !platform.isInverted();
            activePlatformsState.put(platform, currentActualState);
        }
        Boolean lastTarget = platformTargetStates.get(platform);
        boolean targetShouldExist = lastTarget != null ? lastTarget : currentActualState;
        if (shouldEvaluate) {
            boolean met = checkPlatformRequirement(platform);
            targetShouldExist = met ^ platform.isInverted();
        }
        Integer delay = platform.getDelay();
        if (delay != null && delay > 0) {
            if (currentActualState == targetShouldExist) {
                platformDelayTimers.remove(platform);
                platformTargetStates.remove(platform);
            } else {
                Boolean pendingTarget = platformTargetStates.get(platform);
                if (pendingTarget == null || pendingTarget != targetShouldExist) {
                    platformDelayTimers.put(platform, delay);
                    platformTargetStates.put(platform, targetShouldExist);
                } else {
                    int secondsLeft = platformDelayTimers.get(platform) - 1;
                    if (secondsLeft <= 0) {
                        activePlatformsState.put(platform, targetShouldExist);
                        updatePlatformBlocksInWorld(platform, targetShouldExist, true);
                        platformDelayTimers.remove(platform);
                        platformTargetStates.remove(platform);
                    } else {
                        platformDelayTimers.put(platform, secondsLeft);
                    }
                }
            }
        } else {
            if (currentActualState != targetShouldExist) {
                activePlatformsState.put(platform, targetShouldExist);
                updatePlatformBlocksInWorld(platform, targetShouldExist, true);
            }
        }
    }

    private boolean isNearPlatform(Location loc, DungeonPlatform platform) {
        double minX = Math.min(platform.getX1(), platform.getX2()) - 1.5;
        double maxX = Math.max(platform.getX1(), platform.getX2()) + 1.5;
        double minY = Math.min(platform.getY1(), platform.getY2()) - 1.5;
        double maxY = Math.max(platform.getY1(), platform.getY2()) + 1.5;
        double minZ = Math.min(platform.getZ1(), platform.getZ2()) - 1.5;
        double maxZ = Math.max(platform.getZ1(), platform.getZ2()) + 1.5;
        return loc.getX() >= minX && loc.getX() <= maxX &&
               loc.getY() >= minY && loc.getY() <= maxY &&
               loc.getZ() >= minZ && loc.getZ() <= maxZ;
    }

    private boolean hasItemInHandOrInv(Player p, String itemId) {
        ItemStack mainHand = p.getInventory().getItemInMainHand();
        if (mainHand != null && !mainHand.getType().isAir() && isMatchingItem(mainHand, itemId)) return true;
        for (ItemStack stack : p.getInventory().getContents()) {
            if (stack != null && !stack.getType().isAir() && isMatchingItem(stack, itemId)) return true;
        }
        return false;
    }

    private boolean isMatchingItem(ItemStack stack, String itemId) {
        if (stack.hasItemMeta()) {
            org.bukkit.persistence.PersistentDataContainer pdc = stack.getItemMeta().getPersistentDataContainer();
            org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(Plugin.AmonPackPlugin.plugin, "dungeon_item_id");
            if (pdc.has(key, org.bukkit.persistence.PersistentDataType.STRING)) {
                String id = pdc.get(key, org.bukkit.persistence.PersistentDataType.STRING);
                if (itemId.equalsIgnoreCase(id)) return true;
            }
        }
        if (stack.getType().name().equalsIgnoreCase(itemId)) return true;
        if (stack.hasItemMeta() && stack.getItemMeta().getDisplayName() != null) {
            String cleanName = ChatColor.stripColor(stack.getItemMeta().getDisplayName());
            if (cleanName.equalsIgnoreCase(itemId)) return true;
        }
        return false;
    }

    private Location getPlatformCenter(DungeonPlatform platform) {
        double cx = (platform.getX1() + platform.getX2()) / 2.0;
        double cy = (platform.getY1() + platform.getY2()) / 2.0;
        double cz = (platform.getZ1() + platform.getZ2()) / 2.0;
        return new Location(world, cx, cy, cz);
    }

    private void updatePlatformBlocksInWorld(DungeonPlatform platform, boolean exist, boolean spawnParticles) {
        Material mat = exist ? platform.getMaterial() : Material.AIR;
        int minX = (int) Math.min(platform.getX1(), platform.getX2());
        int minY = (int) Math.min(platform.getY1(), platform.getY2());
        int minZ = (int) Math.min(platform.getZ1(), platform.getZ2());
        int maxX = (int) Math.max(platform.getX1(), platform.getX2());
        int maxY = (int) Math.max(platform.getY1(), platform.getY2());
        int maxZ = (int) Math.max(platform.getZ1(), platform.getZ2());
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    block.setType(mat);
                    if (spawnParticles) {
                        Location blockCenter = block.getLocation().add(0.5, 0.5, 0.5);
                        world.spawnParticle(org.bukkit.Particle.CLOUD, blockCenter, 5, 0.2, 0.2, 0.2, 0.0);
                        world.spawnParticle(org.bukkit.Particle.GLOW, blockCenter, 3, 0.2, 0.2, 0.2, 0.0);
                    }
                }
            }
        }
    }

    public boolean isLookingAt(Player player, double tx, double ty, double tz) {
        Location eye = player.getEyeLocation();
        Vector toTarget = new Vector(tx - eye.getX(), ty - eye.getY(), tz - eye.getZ());
        if (toTarget.lengthSquared() == 0.0) return true;
        toTarget.normalize();
        Vector lookDir = eye.getDirection();
        double dot = lookDir.dot(toTarget);
        return dot > 0.95;
    }

    public void completeDungeon() {
        if (isFinished)
            return;
        isFinished = true;

        if (bossBar != null) {
            bossBar.removeAll();
            bossBar.setVisible(false);
        }

        clearSpawnUntilTasks();
        completedConditionsIndices.clear();

        broadcast(ChatColor.GREEN + "[Dungeons] ========================================");
        broadcast(ChatColor.YELLOW + "      DUNGEON UKONCZONY POMYSLNIE!");
        broadcast(ChatColor.GREEN + "[Dungeons] ========================================");

        ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();

        for (UUID uuid : players) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {

                DungeonInventoryBackup.restoreInventory(player, AmonPackPlugin.plugin);

                DungeonPlayerStats stats = playerStatsMap.get(uuid);
                if (stats != null) {
                    stats.resetAttributes(player);
                }

                player.setGameMode(GameMode.SURVIVAL);

                clearPlayerTemporaryStatsAndAbilities(player);

                int xp = template.getRewards().getDungeonXp();
                if (xp > 0) {
                    AmonPackPlugin.getPlayerMenager().AddPoints(LevelSkill.SkillType.DUNGEON, player, xp);
                    player.sendMessage(
                            ChatColor.GOLD + "[Levels] " + ChatColor.YELLOW + "+" + xp + " EXP Eksploracji Dungeonow!");
                }

                double money = template.getRewards().getMoney();
                if (money > 0) {
                    player.sendMessage(
                            ChatColor.GOLD + "[Portfel] " + ChatColor.YELLOW + "+" + money + "$ za ukonczenie lochu!");
                }

                for (ItemStack item : template.getRewards().getItems()) {
                    HashMap<Integer, ItemStack> left = player.getInventory().addItem(item.clone());
                    if (!left.isEmpty()) {
                        Location dropLoc = getExitLocation();
                        for (ItemStack drop : left.values()) {
                            dropLoc.getWorld().dropItemNaturally(dropLoc, drop);
                        }
                        player.sendMessage(ChatColor.RED
                                + "Brak miejsca w ekwipunku! Nagrody zostaly upuszczone pod Twoimi nogami.");
                    }
                }

                for (String cmd : template.getRewards().getCommands()) {
                    String finalCmd = cmd.replace("%player%", player.getName());
                    Bukkit.dispatchCommand(console, finalCmd);
                }

                player.teleport(getExitLocation());
                player.sendMessage(
                        ChatColor.GREEN + "Zostales bezpiecznie przeteleportowany z powrotem na glowny swiat.");
            }
        }

        cleanupWorldAndBackups();
    }

    public void cleanup() {
        isFinished = true;

        if (bossBar != null) {
            bossBar.removeAll();
            bossBar.setVisible(false);
        }

        clearSpawnUntilTasks();
        completedConditionsIndices.clear();

        for (UUID uuid : players) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                DungeonInventoryBackup.restoreInventory(player, AmonPackPlugin.plugin);

                DungeonPlayerStats stats = playerStatsMap.get(uuid);
                if (stats != null) {
                    stats.resetAttributes(player);
                }

                player.setGameMode(GameMode.SURVIVAL);

                clearPlayerTemporaryStatsAndAbilities(player);
                player.teleport(getExitLocation());
                player.sendMessage(ChatColor.RED + "Dungeon zostal zamkniety. Przywrocono Twoj ekwipunek survivalowy.");
            }
        }

        cleanupWorldAndBackups();
    }

    private void cleanupWorldAndBackups() {
        DungeonWorldManager.deleteDungeonWorld(world);
    }

    public void ejectPlayer(Player player) {
        if (player == null)
            return;
        UUID uuid = player.getUniqueId();

        if (players.contains(uuid)) {
            players.remove(uuid);
            readyPlayers.remove(uuid);
            spectatorPlayers.remove(uuid);

            if (bossBar != null) {
                bossBar.removePlayer(player);
            }

            DungeonInventoryBackup.restoreInventory(player, AmonPackPlugin.plugin);
            DungeonPlayerStats stats = playerStatsMap.remove(uuid);
            if (stats != null) {
                stats.resetAttributes(player);
            }

            player.setGameMode(GameMode.SURVIVAL);

            clearPlayerTemporaryStatsAndAbilities(player);
            player.teleport(getExitLocation());
            player.sendMessage(ChatColor.YELLOW + "Opusciles dungeon. Twoje przedmioty zostaly przywrocone.");
            broadcast(ChatColor.RED + player.getName() + " opuscil druzyne dungeonu.");
        }
    }

    public void registerLootChest(Location loc, String type) {
        activeLootChests.put(loc, type);
    }

    public boolean isRegisteredLootChest(Location loc) {
        return activeLootChests.containsKey(loc);
    }

    public void removeLootChest(Location loc) {
        activeLootChests.remove(loc);
    }

    public void onMobKill(String mobName) {
        String cleanName = mobName.toLowerCase();
        int count = killedMobsCounter.getOrDefault(cleanName, 0) + 1;
        killedMobsCounter.put(cleanName, count);

        Encounter encounter = getActiveEncounter();
        if (encounter != null) {
            boolean allMet = true;
            for (DungeonCondition condition : encounter.getConditions()) {
                if (!condition.isMet(this)) {
                    allMet = false;
                    break;
                }
            }
            if (allMet && !encounter.getConditions().isEmpty()) {
                transitionToNext();
            }
        }
    }

    public int getKilledMobsCount(String mobName) {
        return killedMobsCounter.getOrDefault(mobName.toLowerCase(), 0);
    }

    public boolean areAllPlayersReady() {
        if (readyPlayers.isEmpty())
            return false;
        for (boolean ready : readyPlayers.values()) {
            if (!ready)
                return false;
        }
        return true;
    }

    public void setPlayerReady(Player player, boolean ready) {
        if (readyPlayers.containsKey(player.getUniqueId())) {
            readyPlayers.put(player.getUniqueId(), ready);

            if (ready) {
                broadcast(ChatColor.GREEN + player.getName() + " jest gotowy!");
                double maxHealth = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
                player.setHealth(maxHealth);
                player.setFoodLevel(20);
            } else {
                broadcast(ChatColor.RED + player.getName() + " nie jest gotowy.");
            }

            Encounter encounter = getActiveEncounter();
            if (encounter != null) {
                boolean allMet = true;
                for (DungeonCondition condition : encounter.getConditions()) {
                    if (!condition.isMet(this)) {
                        allMet = false;
                        break;
                    }
                }
                if (allMet && !encounter.getConditions().isEmpty()) {
                    transitionToNext();
                }
            }
        }
    }

    public void broadcast(String msg) {
        for (UUID uuid : players) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                p.sendMessage(msg);
            }
        }
    }

    public List<Player> getOnlinePlayers() {
        return players.stream()
                .map(Bukkit::getPlayer)
                .filter(p -> p != null && p.isOnline())
                .collect(Collectors.toList());
    }

    public Location getExitLocation() {
        World exitW = Bukkit.getWorld(template.getExitWorld());
        Vector exit = template.getExitLocation();
        return new Location(exitW == null ? Bukkit.getWorlds().get(0) : exitW, exit.getX(), exit.getY(), exit.getZ());
    }

    public Encounter getActiveEncounter() {
        return template.getEncounters().get(activeEncounterId);
    }

    public World getWorld() {
        return world;
    }

    public Dungeon getTemplate() {
        return template;
    }

    public DungeonPlayerStats getPlayerStats(Player p) {
        return playerStatsMap.get(p.getUniqueId());
    }

    public List<UUID> getPlayers() {
        return players;
    }

    public boolean isFinished() {
        return isFinished;
    }

    public void setPlayerSpectator(Player player, boolean spec) {
        UUID uuid = player.getUniqueId();
        if (spec) {
            spectatorPlayers.add(uuid);
            player.setGameMode(GameMode.SPECTATOR);
            player.sendMessage(ChatColor.RED
                    + "[Dungeons] Polegles! Zostales spektatorem. Nie mozesz sie ruszac, dopoki druzyna nie wygra lub nie przegra.");
        } else {
            spectatorPlayers.remove(uuid);
            player.setGameMode(GameMode.SURVIVAL);
        }
    }

    public boolean isPlayerSpectator(Player player) {
        return spectatorPlayers.contains(player.getUniqueId());
    }

    public Set<UUID> getSpectatorPlayers() {
        return spectatorPlayers;
    }

    public boolean isPlayerReady(Player player) {
        return readyPlayers.getOrDefault(player.getUniqueId(), false);
    }

    public void markChestClaimed(Player player, Location loc) {
        Set<UUID> claimants = claimedChests.computeIfAbsent(loc, k -> new HashSet<>());
        claimants.add(player.getUniqueId());

        List<UUID> activeLiving = new ArrayList<>();
        for (UUID uuid : players) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline() && !isPlayerSpectator(p)) {
                activeLiving.add(uuid);
            }
        }

        if (claimants.containsAll(activeLiving)) {
            org.bukkit.block.Block block = loc.getBlock();
            block.setType(Material.AIR);
            block.getWorld().spawnParticle(org.bukkit.Particle.BLOCK, block.getLocation().add(0.5, 0.5, 0.5), 20, 0.3,
                    0.3, 0.3, Material.CHEST.createBlockData());
            block.getWorld().playSound(block.getLocation(), org.bukkit.Sound.BLOCK_CHEST_OPEN, 1.0f, 1.2f);

            removeLootChest(loc);
            claimedChests.remove(loc);
            chestGuis.remove(loc);
        }
    }

    public boolean hasClaimedChest(Player player, Location loc) {
        Set<UUID> claimants = claimedChests.get(loc);
        return claimants != null && claimants.contains(player.getUniqueId());
    }

    public void preGenerateChestGuis(Location loc) {
        Map<UUID, DungeonLootChest> playerGuis = new HashMap<>();
        for (UUID uuid : players) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                DungeonPlayerStats stats = getPlayerStats(player);
                DungeonLootChest gui = new DungeonLootChest(player, stats, template, loc);
                playerGuis.put(uuid, gui);
            }
        }
        chestGuis.put(loc, playerGuis);
    }

    public DungeonLootChest getChestGuiForPlayer(Location loc, Player player) {
        Map<UUID, DungeonLootChest> playerGuis = chestGuis.get(loc);
        if (playerGuis == null) {
            playerGuis = new HashMap<>();
            chestGuis.put(loc, playerGuis);
        }

        DungeonLootChest gui = playerGuis.get(player.getUniqueId());
        if (gui == null) {
            DungeonPlayerStats stats = getPlayerStats(player);
            gui = new DungeonLootChest(player, stats, template, loc);
            playerGuis.put(player.getUniqueId(), gui);
        }

        return gui;
    }

    public static void clearPlayerTemporaryStatsAndAbilities(org.bukkit.OfflinePlayer player) {
        if (player == null) return;
        
        PlayerBendingBranch branch = AmonPackPlugin.levelsBending.GetBranchByPlayerName(player.getName());
        if (branch != null) {
            branch.getTemporaryAbilities().clear();
            
            com.projectkorra.projectkorra.BendingPlayer bPlayer = com.projectkorra.projectkorra.BendingPlayer.getBendingPlayer(player);
            if (bPlayer != null) {
                for (com.projectkorra.projectkorra.Element tempEl : branch.getTemporaryElements()) {
                    bPlayer.getElements().remove(tempEl);
                }
                bPlayer.removeUnusableAbilities();
            }
            branch.getTemporaryElements().clear();
        }
    }

    public void addSpawnUntilTaskId(int id) {
        spawnUntilTaskIds.add(id);
    }

    public void clearSpawnUntilTasks() {
        for (int id : spawnUntilTaskIds) {
            Bukkit.getScheduler().cancelTask(id);
        }
        spawnUntilTaskIds.clear();
    }

    public int getZoneProgress(DungeonCondition condition) {
        return zoneCaptureCounters.getOrDefault(condition, 0);
    }

    public void incrementZoneProgress(DungeonCondition condition) {
        int val = getZoneProgress(condition) + 1;
        zoneCaptureCounters.put(condition, val);
    }

    public int getThrowHits(DungeonCondition condition) {
        return throwHitsCounter.getOrDefault(condition, 0);
    }

    public void incrementThrowHits(DungeonCondition condition) {
        int val = getThrowHits(condition) + 1;
        throwHitsCounter.put(condition, val);
    }

    public void registerProjectileHitCoord(Location hitLoc) {
        Encounter encounter = getActiveEncounter();
        if (encounter == null) return;
        for (DungeonCondition condition : encounter.getConditions()) {
            if (condition.getType() == DungeonCondition.ConditionType.THROW_AT) {
                Location target = new Location(world, condition.getX(), condition.getY(), condition.getZ());
                if (hitLoc.distanceSquared(target) <= condition.getRadius() * condition.getRadius()) {
                    incrementThrowHits(condition);
                    world.playSound(hitLoc, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.5f);
                    world.spawnParticle(Particle.HAPPY_VILLAGER, hitLoc, 10, 0.2, 0.2, 0.2, 0.05);
                    break;
                }
            }
        }
    }

    public void registerSpawnedMob(UUID uuid) {
        spawnedMobUuids.add(uuid);
    }

    public boolean isShieldedEnemy(UUID uuid) {
        return activeShieldedEnemyUuids.contains(uuid);
    }

    public void registerShieldedEnemyHit(org.bukkit.entity.LivingEntity entity, DungeonCustomItem customItem) {
        Encounter encounter = getActiveEncounter();
        if (encounter == null) return;
        for (DungeonCondition condition : encounter.getConditions()) {
            if (condition.getType() == DungeonCondition.ConditionType.THROW_AT_ENEMY) {
                if (activeShieldedEnemyUuids.contains(entity.getUniqueId()) && condition.getCustomItemId().equalsIgnoreCase(customItem.getId())) {
                    incrementThrowHits(condition);
                    world.playSound(entity.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 0.8f);
                    world.spawnParticle(Particle.FLASH, entity.getLocation().add(0, 1, 0), 5, 0.1, 0.1, 0.1, 0.01);
                    
                    if (getThrowHits(condition) >= condition.getAmount()) {
                        activeShieldedEnemyUuids.remove(entity.getUniqueId());
                        entity.damage(100.0);
                        world.spawnParticle(Particle.EXPLOSION, entity.getLocation().add(0, 1, 0), 10, 0.2, 0.2, 0.2, 0.05);
                        world.playSound(entity.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.2f);
                    }
                    break;
                }
            }
        }
    }

    public void updateGloveBossBar(Player p, DungeonPlayerStats stats) {
        if (p == null || !p.isOnline()) return;
        int charge = stats.getAmonGloveCharge();
        int max = 5;
        int blessingLevel = stats.getBlessingLevel("AMON_GLOVE");
        if (blessingLevel == 2) max = 4;
        else if (blessingLevel >= 3) max = 3;

        if (charge <= 0) {
            stats.cleanupGloveBar(p);
            return;
        }

        org.bukkit.boss.BossBar bar = stats.getAmonGloveBar();
        if (bar == null) {
            bar = Bukkit.createBossBar("", org.bukkit.boss.BarColor.BLUE, org.bukkit.boss.BarStyle.SEGMENTED_6);
            stats.setAmonGloveBar(bar);
            bar.addPlayer(p);
        }

        if (charge >= max) {
            bar.setColor(org.bukkit.boss.BarColor.YELLOW);
            bar.setTitle(ChatColor.translateAlternateColorCodes('&', "&e&lREKAWICA AMONA: &b&l⚡ NAŁADOWANO! ⚡"));
        } else {
            bar.setColor(org.bukkit.boss.BarColor.BLUE);
            String title = "&eMoc Rękawicy Amona: " + "&b" + "⚡".repeat(charge) + "&8" + "⚡".repeat(max - charge);
            bar.setTitle(ChatColor.translateAlternateColorCodes('&', title));
        }
        bar.setProgress(Math.max(0.0, Math.min(1.0, (double) charge / max)));
        bar.setVisible(true);
    }

    public void updateVisualDurability(ItemStack item, double pct) {
        if (item == null || item.getType() == Material.AIR) return;
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        if (meta instanceof org.bukkit.inventory.meta.Damageable) {
            org.bukkit.inventory.meta.Damageable dmg = (org.bukkit.inventory.meta.Damageable) meta;
            int max = item.getType().getMaxDurability();
            if (max <= 0) {
                dmg.setMaxDamage(100);
                max = 100;
            }
            double value = (1.0 - (pct / 100.0)) * max;
            int damage = (int) Math.round(value);
            if (damage >= max) {
                damage = max - 1;
            }
            if (damage < 0) {
                damage = 0;
            }
            dmg.setDamage(damage);
            item.setItemMeta(dmg);
        }
    }

    public ItemStack getLegendaryWeaponItem(Player player, String key) {
        if (player == null) return null;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.hasItemMeta()) {
                org.bukkit.persistence.PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
                org.bukkit.NamespacedKey nkey = new org.bukkit.NamespacedKey(AmonPackPlugin.plugin, "dungeon_weapon_type");
                if (pdc.has(nkey, org.bukkit.persistence.PersistentDataType.STRING)) {
                    if (key.equals(pdc.get(nkey, org.bukkit.persistence.PersistentDataType.STRING))) {
                        return item;
                    }
                }
            }
        }
        return null;
    }

    public void triggerWaterStaffAura(Player p, int lvl) {
        p.getWorld().spawnParticle(Particle.SNOWFLAKE, p.getLocation(), 30, 6.0, 1.0, 6.0, 0.05);
        p.getWorld().spawnParticle(Particle.FALLING_WATER, p.getLocation(), 20, 6.0, 1.0, 6.0, 0.05);
        double maxHP = p.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
        double curHP = p.getHealth();
        p.setHealth(Math.min(maxHP, curHP + 1.0));
        p.getWorld().spawnParticle(Particle.HEART, p.getLocation().add(0, 1.5, 0), 1, 0.1, 0.1, 0.1, 0.0);
        Block blockUnder = p.getLocation().getBlock();
        if (blockUnder.getType() == Material.AIR && blockUnder.getRelative(org.bukkit.block.BlockFace.DOWN).getType().isSolid()) {
            blockUnder.setType(Material.SNOW);
        }

        for (org.bukkit.entity.Entity ent : p.getNearbyEntities(6.0, 6.0, 6.0)) {
            if (ent instanceof org.bukkit.entity.LivingEntity) {
                org.bukkit.entity.LivingEntity le = (org.bukkit.entity.LivingEntity) ent;
                if (le instanceof Player) {
                    Player ally = (Player) le;
                    if (!isPlayerSpectator(ally)) {
                        double aMax = ally.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
                        double aCur = ally.getHealth();
                        ally.setHealth(Math.min(aMax, aCur + 1.0));
                        ally.getWorld().spawnParticle(Particle.HEART, ally.getLocation().add(0, 1.5, 0), 1, 0.1, 0.1, 0.1, 0.0);
                        Block bUnder = ally.getLocation().getBlock();
                        if (bUnder.getType() == Material.AIR && bUnder.getRelative(org.bukkit.block.BlockFace.DOWN).getType().isSolid()) {
                            bUnder.setType(Material.SNOW);
                        }
                    }
                } else {
                    le.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS, 60, 1));
                    Block bUnder = le.getLocation().getBlock();
                    if (bUnder.getType() == Material.AIR && bUnder.getRelative(org.bukkit.block.BlockFace.DOWN).getType().isSolid()) {
                        bUnder.setType(Material.SNOW);
                    }
                }
            }
        }
    }
}

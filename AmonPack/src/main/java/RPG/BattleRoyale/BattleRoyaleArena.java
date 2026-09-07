package RPG.BattleRoyale;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class BattleRoyaleArena {

    private String name;
    private String worldName;
    private String schematic;
    private Location pasteLocation;
    private Location centerLocation;
    private final List<Location> spawnLocations = new ArrayList<>();

    // Ustawienia lobby i rozgrywki
    private int lobbyDurationSeconds = 60;
    private int minPlayers = 1; // Umożliwia start w 1 osobę
    private int maxPlayers = 20;
    private int freezeCountdownSeconds = 20;

    // Głód
    private boolean increasedHungerDrain = true;
    private int hungerDrainIntervalSeconds = 4;
    private float hungerExhaustionAddition = 0.75f;

    // Bloki i ochrona areny
    private boolean restrictBlockBreaking = true;
    private final Map<Material, BreakableBlockRule> breakableBlocks = new HashMap<>();
    private boolean restrictBlockPlacing = true;
    private final Set<Material> placeableBlocks = new HashSet<>();
    private int barricadeHitsToDestroy = 3;

    // Strefa śmierci
    private double initialRadius = 120.0;
    private double finalRadius = 14.0;
    private double damageOutsidePerSecond = 2.0;
    private final List<ZonePhase> zonePhases = new ArrayList<>();

    // Infekcja
    private int infectionTotalDurationSeconds = 300; // 5 minut
    private double meleeInfectChance = 0.35;
    private String cureMaterial = "POTION";
    private String cureDisplayName = "&a&l💉 Antidotum na Infekcję";
    private List<String> cureLore = new ArrayList<>();
    private int cureCustomModelData = 22001;

    // PVE & Moby
    private int pveSpawnIntervalSeconds = 25;
    private int maxAliveMobs = 30;
    private final List<Map<?, ?>> vanillaMobs = new ArrayList<>();
    private final List<Map<?, ?>> mythicMobs = new ArrayList<>();
    private String finalBossMythicMob = "ZombieTitanBoss";
    private int finalZombiesCount = 15;

    // Boty AI wypełniające brakujące sloty graczy
    private boolean fillWithBots = true;
    private int maxBotsToSpawn = 19;

    // Portal ewakuacyjny i nagrody
    private double portalRadius = 2.5;
    private List<String> rewardCommands = new ArrayList<>();
    private int rewardExp = 500;

    public static class BreakableBlockRule {
        private final Material material;
        private final ItemStack optionalLoot; // null jeśli nic nie dropi

        public BreakableBlockRule(Material material, ItemStack optionalLoot) {
            this.material = material;
            this.optionalLoot = optionalLoot;
        }

        public Material getMaterial() { return material; }
        public ItemStack getOptionalLoot() { return optionalLoot; }
        public boolean hasLoot() { return optionalLoot != null; }
    }

    public static class ZonePhase {
        private final int durationSeconds;
        private final int waitSeconds;
        private final double targetRadius;

        public ZonePhase(int durationSeconds, int waitSeconds, double targetRadius) {
            this.durationSeconds = durationSeconds;
            this.waitSeconds = waitSeconds;
            this.targetRadius = targetRadius;
        }

        public int getDurationSeconds() { return durationSeconds; }
        public int getWaitSeconds() { return waitSeconds; }
        public double getTargetRadius() { return targetRadius; }
    }

    public static BattleRoyaleArena fromConfig(FileConfiguration config) {
        BattleRoyaleArena arena = new BattleRoyaleArena();

        arena.name = config.getString("arena.name", "Battle Royale Zombie Apocalypse");
        arena.worldName = config.getString("arena.world-name", "hungergames_arena");
        arena.schematic = config.getString("arena.schematic", "arena_br.schem");

        // Ustawienia
        arena.lobbyDurationSeconds = config.getInt("settings.lobby-duration-seconds", 60);
        arena.minPlayers = config.getInt("settings.min-players", 1);
        arena.maxPlayers = config.getInt("settings.max-players", 20);
        arena.freezeCountdownSeconds = config.getInt("settings.freeze-countdown-seconds", 20);

        // Pozycje
        World defaultWorld = Bukkit.getWorld(arena.worldName);
        double px = config.getDouble("arena.paste-location.x", 0.0);
        double py = config.getDouble("arena.paste-location.y", 100.0);
        double pz = config.getDouble("arena.paste-location.z", 0.0);
        arena.pasteLocation = new Location(defaultWorld, px, py, pz);

        double cx = config.getDouble("arena.center-location.x", 0.0);
        double cy = config.getDouble("arena.center-location.y", 100.0);
        double cz = config.getDouble("arena.center-location.z", 0.0);
        arena.centerLocation = new Location(defaultWorld, cx, cy, cz);

        // Punkty spawnów
        arena.spawnLocations.clear();
        List<Map<?, ?>> spawnsList = config.getMapList("arena.spawns");
        if (spawnsList != null && !spawnsList.isEmpty()) {
            for (Map<?, ?> sp : spawnsList) {
                double sx = getDouble(sp.get("x"), 0.0);
                double sy = getDouble(sp.get("y"), 100.0);
                double sz = getDouble(sp.get("z"), 0.0);
                float yaw = (float) getDouble(sp.get("yaw"), 0.0);
                float pitch = (float) getDouble(sp.get("pitch"), 0.0);
                arena.spawnLocations.add(new Location(defaultWorld, sx, sy, sz, yaw, pitch));
            }
        }

        // Domyślne spawny jeśli brak w konfiguracji
        if (arena.spawnLocations.isEmpty()) {
            for (int i = 0; i < 20; i++) {
                double angle = (2 * Math.PI / 20) * i;
                double r = 40.0;
                double sx = cx + r * Math.cos(angle);
                double sz = cz + r * Math.sin(angle);
                float yaw = (float) Math.toDegrees(angle) + 90f;
                arena.spawnLocations.add(new Location(defaultWorld, sx, cy + 1, sz, yaw, 0f));
            }
        }

        // Strefa
        arena.initialRadius = config.getDouble("zone.initial-radius", 120.0);
        arena.finalRadius = config.getDouble("zone.final-radius", 14.0);
        arena.damageOutsidePerSecond = config.getDouble("zone.damage-outside-per-second", 2.0);

        arena.zonePhases.clear();
        List<Map<?, ?>> phasesList = config.getMapList("zone.phases");
        if (phasesList != null && !phasesList.isEmpty()) {
            for (Map<?, ?> p : phasesList) {
                int dur = getInt(p.get("duration-seconds"), 60);
                int wait = getInt(p.get("wait-seconds"), 20);
                double targetR = getDouble(p.get("target-radius"), 40.0);
                arena.zonePhases.add(new ZonePhase(dur, wait, targetR));
            }
        } else {
            arena.zonePhases.add(new ZonePhase(60, 30, 80.0));
            arena.zonePhases.add(new ZonePhase(60, 20, 40.0));
            arena.zonePhases.add(new ZonePhase(60, 10, 14.0));
        }

        // Infekcja
        arena.infectionTotalDurationSeconds = config.getInt("infection.total-duration-seconds", 300);
        arena.meleeInfectChance = config.getDouble("infection.melee-infect-chance", 0.35);
        arena.cureMaterial = config.getString("infection.cure-item.material", "POTION");
        arena.cureDisplayName = config.getString("infection.cure-item.display-name", "&a&l💉 Antidotum na Infekcję");
        arena.cureLore = config.getStringList("infection.cure-item.lore");
        arena.cureCustomModelData = config.getInt("infection.cure-item.custom-model-data", 22001);

        // PVE
        arena.pveSpawnIntervalSeconds = config.getInt("pve.spawn-interval-seconds", 25);
        arena.maxAliveMobs = config.getInt("pve.max-alive-mobs", 30);
        arena.vanillaMobs.clear();
        if (config.getMapList("pve.vanilla-mobs") != null) {
            arena.vanillaMobs.addAll(config.getMapList("pve.vanilla-mobs"));
        }
        arena.mythicMobs.clear();
        if (config.getMapList("pve.mythic-mobs") != null) {
            arena.mythicMobs.addAll(config.getMapList("pve.mythic-mobs"));
        }
        arena.finalBossMythicMob = config.getString("pve.final-wave.boss-mythic-mob", "ZombieTitanBoss");
        arena.finalZombiesCount = config.getInt("pve.final-wave.zombies-count", 15);

        // Boty
        arena.fillWithBots = config.getBoolean("settings.fill-empty-slots-with-bots", true);
        arena.maxBotsToSpawn = config.getInt("settings.max-bots", 19);

        // Głód
        arena.increasedHungerDrain = config.getBoolean("hunger.increased-drain", true);
        arena.hungerDrainIntervalSeconds = config.getInt("hunger.drain-interval-seconds", 4);
        arena.hungerExhaustionAddition = (float) config.getDouble("hunger.exhaustion-addition", 0.75);

        // Ochrona i reguły bloków
        arena.restrictBlockBreaking = config.getBoolean("blocks.restrict-breaking", true);
        arena.breakableBlocks.clear();
        List<Map<?, ?>> breakableList = config.getMapList("blocks.breakable");
        if (breakableList != null && !breakableList.isEmpty()) {
            for (Map<?, ?> entry : breakableList) {
                String matStr = (String) entry.get("material");
                if (matStr == null) continue;
                Material mat = Material.matchMaterial(matStr);
                if (mat == null) continue;

                ItemStack lootItem = null;
                Object lootObj = entry.get("loot");
                if (lootObj instanceof String lootMatStr && !lootMatStr.isEmpty()) {
                    Material lootMat = Material.matchMaterial(lootMatStr);
                    int amount = entry.get("amount") instanceof Number ? ((Number) entry.get("amount")).intValue() : 1;
                    if (lootMat != null) {
                        lootItem = new ItemStack(lootMat, Math.max(1, amount));
                    }
                }
                arena.breakableBlocks.put(mat, new BreakableBlockRule(mat, lootItem));
            }
        }

        arena.restrictBlockPlacing = config.getBoolean("blocks.restrict-placing", true);
        arena.placeableBlocks.clear();
        List<String> placeableList = config.getStringList("blocks.placeable");
        if (placeableList != null && !placeableList.isEmpty()) {
            for (String pMatStr : placeableList) {
                Material mat = Material.matchMaterial(pMatStr);
                if (mat != null) arena.placeableBlocks.add(mat);
            }
        } else {
            arena.placeableBlocks.add(Material.OAK_SLAB);
        }

        arena.barricadeHitsToDestroy = config.getInt("blocks.barricade-hits-to-destroy", 3);

        // Portal i nagrody
        arena.portalRadius = config.getDouble("extraction.portal-radius", 2.5);
        arena.rewardCommands = config.getStringList("extraction.rewards.commands");
        arena.rewardExp = config.getInt("extraction.rewards.exp", 500);

        return arena;
    }

    private static double getDouble(Object obj, double def) {
        if (obj instanceof Number) return ((Number) obj).doubleValue();
        if (obj != null) {
            try { return Double.parseDouble(obj.toString()); } catch (NumberFormatException ignored) {}
        }
        return def;
    }

    private static int getInt(Object obj, int def) {
        if (obj instanceof Number) return ((Number) obj).intValue();
        if (obj != null) {
            try { return Integer.parseInt(obj.toString()); } catch (NumberFormatException ignored) {}
        }
        return def;
    }

    // Gettery
    public String getName() { return name; }
    public String getWorldName() { return worldName; }
    public String getSchematic() { return schematic; }
    public Location getPasteLocation() { return pasteLocation; }
    public Location getCenterLocation() { return centerLocation; }
    public List<Location> getSpawnLocations() { return spawnLocations; }
    public int getLobbyDurationSeconds() { return lobbyDurationSeconds; }
    public int getMinPlayers() { return minPlayers; }
    public int getMaxPlayers() { return maxPlayers; }
    public int getFreezeCountdownSeconds() { return freezeCountdownSeconds; }
    public boolean isIncreasedHungerDrain() { return increasedHungerDrain; }
    public int getHungerDrainIntervalSeconds() { return hungerDrainIntervalSeconds; }
    public float getHungerExhaustionAddition() { return hungerExhaustionAddition; }
    public boolean isRestrictBlockBreaking() { return restrictBlockBreaking; }
    public Map<Material, BreakableBlockRule> getBreakableBlocks() { return breakableBlocks; }
    public boolean isRestrictBlockPlacing() { return restrictBlockPlacing; }
    public Set<Material> getPlaceableBlocks() { return placeableBlocks; }
    public int getBarricadeHitsToDestroy() { return barricadeHitsToDestroy; }
    public double getInitialRadius() { return initialRadius; }
    public double getFinalRadius() { return finalRadius; }
    public double getDamageOutsidePerSecond() { return damageOutsidePerSecond; }
    public List<ZonePhase> getZonePhases() { return zonePhases; }
    public int getInfectionTotalDurationSeconds() { return infectionTotalDurationSeconds; }
    public double getMeleeInfectChance() { return meleeInfectChance; }
    public String getCureMaterial() { return cureMaterial; }
    public String getCureDisplayName() { return cureDisplayName; }
    public List<String> getCureLore() { return cureLore; }
    public int getCureCustomModelData() { return cureCustomModelData; }
    public int getPveSpawnIntervalSeconds() { return pveSpawnIntervalSeconds; }
    public int getMaxAliveMobs() { return maxAliveMobs; }
    public List<Map<?, ?>> getVanillaMobs() { return vanillaMobs; }
    public List<Map<?, ?>> getMythicMobs() { return mythicMobs; }
    public String getFinalBossMythicMob() { return finalBossMythicMob; }
    public int getFinalZombiesCount() { return finalZombiesCount; }
    public boolean isFillWithBots() { return fillWithBots; }
    public int getMaxBotsToSpawn() { return maxBotsToSpawn; }
    public double getPortalRadius() { return portalRadius; }
    public List<String> getRewardCommands() { return rewardCommands; }
    public int getRewardExp() { return rewardExp; }
}

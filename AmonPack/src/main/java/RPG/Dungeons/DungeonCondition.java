package RPG.Dungeons;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import java.util.ArrayList;
import java.util.List;

public class DungeonCondition {

    public enum ConditionType {
        ALL_PLAYERS_READY,
        PLAYER_ENTER_AREA,
        KILL_MOBS,
        INTERACT_BLOCK_WITH_ITEM,
        ZONE,
        THROW_AT,
        SHIELDED,
        DROP_ON_DEATH,
        PERIODIC_CHECK,
        LOOKING_AT,
        ALIVE,
        COLLECT_POINTS,
        DYNAMIC_PATH
    }

    private final ConditionType type;
    
    private double x, y, z;
    private float yaw = 0.0f;
    private float pitch = 0.0f;
    private boolean hasCoords = false;
    private boolean freezePlayers = true;

    private double minX, minY, minZ;
    private double maxX, maxY, maxZ;
    private double startX, startY, startZ;
    private boolean hasStartLoc = false;
    private double endX, endY, endZ;
    private boolean hasEndLoc = false;
    private double revealX, revealY, revealZ;
    private double revealRadius;
    private int reshuffleIntervalSeconds = 0;
    private Material safeBlockMaterial = Material.STONE;
    private Material crumbleBlockMaterial = Material.CRACKED_STONE_BRICKS;
    private boolean interactedMet = false;

    private List<Double> xList = new ArrayList<>();
    private List<Double> yList = new ArrayList<>();
    private List<Double> zList = new ArrayList<>();
    private String shieldType = "throw";
    private double radius;

    private String mobName;
    private String mobDisplayName;
    private int amount;

    private Material blockMaterial;
    private Material itemMaterial;
    private String itemDisplayName;

    private String customItemId;
    private double chance;
    private int timeRequired;
    private List<DungeonEffect> onCompleteEffects = new ArrayList<>();
    private String requirement;
    private int interval;
    private List<DungeonEffect> failEffects = new ArrayList<>();
    private List<DungeonEffect> successEffects = new ArrayList<>();
    private boolean once = false;
    private boolean requiredAllPlayers = false;
    private boolean requiredItems = true;
    private List<Location> points = new ArrayList<>();

    public static Material parseMaterial(String input) {
        if (input == null || input.trim().isEmpty()) return null;
        String clean = input.trim().toUpperCase().replace(" ", "_");
        Material mat = Material.getMaterial(clean);
        if (mat != null) return mat;

        if (clean.equals("IRON_CHAIN") || clean.equals("CHAINS") || clean.equals("CHAIN")) {
            mat = Material.getMaterial("CHAIN");
            if (mat != null) return mat;
        }
        if (clean.equals("IRON_DOORS") || clean.equals("IRON_DOOR")) {
            mat = Material.getMaterial("IRON_DOOR");
            if (mat != null) return mat;
        }
        if (clean.equals("WOOD_DOOR") || clean.equals("WOODEN_DOOR") || clean.equals("OAK_DOOR")) {
            mat = Material.getMaterial("OAK_DOOR");
            if (mat != null) return mat;
        }
        return null;
    }

    public List<Location> getPoints() {
        return points;
    }

    public void setPoints(List<Location> points) {
        this.points = points;
    }

    public DungeonCondition(ConditionType type) {
        this.type = type;
    }

    public DungeonCondition(double x, double y, double z, double radius) {
        this.type = ConditionType.PLAYER_ENTER_AREA;
        this.x = x;
        this.y = y;
        this.z = z;
        this.radius = radius;
    }

    public DungeonCondition(String mobName, int amount) {
        this.type = ConditionType.KILL_MOBS;
        this.mobName = mobName;
        this.amount = amount;
    }

    public DungeonCondition(double x, double y, double z, Material blockMaterial, Material itemMaterial, String itemDisplayName) {
        this.type = ConditionType.INTERACT_BLOCK_WITH_ITEM;
        this.x = x;
        this.y = y;
        this.z = z;
        this.blockMaterial = blockMaterial;
        this.itemMaterial = itemMaterial;
        this.itemDisplayName = itemDisplayName;
    }

    public DungeonCondition(double x, double y, double z, double radius, int timeRequired) {
        this.type = ConditionType.ZONE;
        this.x = x;
        this.y = y;
        this.z = z;
        this.radius = radius;
        this.timeRequired = timeRequired;
    }

    public DungeonCondition(ConditionType type, double x, double y, double z, double radius, String customItemId, int amount) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.z = z;
        this.radius = radius;
        this.customItemId = customItemId;
        this.amount = amount;
    }

    public DungeonCondition(ConditionType type, String mobName, String customItemId, int amount, double x, double y, double z) {
        this.type = type;
        this.mobName = mobName;
        this.customItemId = customItemId;
        this.amount = amount;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public DungeonCondition(String mobName, String customItemId, double chance) {
        this.type = ConditionType.DROP_ON_DEATH;
        this.mobName = mobName;
        this.customItemId = customItemId;
        this.chance = chance;
    }

    public Location getResolvedStartLocation(DungeonInstance instance) {
        if (hasStartLoc) {
            return new Location(instance.getWorld(), startX, startY, startZ);
        }
        double sx = Math.min(minX, maxX);
        double sy = minY;
        double sz = Math.min(minZ, maxZ);
        return new Location(instance.getWorld(), sx, sy, sz);
    }

    public Location getResolvedEndLocation(DungeonInstance instance) {
        if (hasEndLoc) {
            return new Location(instance.getWorld(), endX, endY, endZ);
        }
        double ex = Math.max(minX, maxX);
        double ey = minY;
        double ez = Math.max(minZ, maxZ);
        return new Location(instance.getWorld(), ex, ey, ez);
    }

    public boolean isMet(DungeonInstance instance) {
        switch (type) {
            case ALL_PLAYERS_READY:
                return instance.areAllPlayersReady();

            case PLAYER_ENTER_AREA:
                Location center = getResolvedLocation(instance);
                double radiusSq = radius * radius;
                List<Player> activePlayers = new ArrayList<>();
                for (Player p : instance.getOnlinePlayers()) {
                    if (!instance.isPlayerSpectator(p)) {
                        activePlayers.add(p);
                    }
                }
                if (activePlayers.isEmpty()) return false;
                if (requiredAllPlayers) {
                    for (Player p : activePlayers) {
                        if (p.getLocation().distanceSquared(center) > radiusSq) {
                            return false;
                        }
                    }
                    return true;
                } else {
                    for (Player player : activePlayers) {
                        if (player.getLocation().distanceSquared(center) <= radiusSq) {
                            return true;
                        }
                    }
                    return false;
                }

            case KILL_MOBS: {
                String nameToUse = (mobDisplayName != null && !mobDisplayName.isEmpty()) ? mobDisplayName : mobName;
                String cleanTarget = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', nameToUse));
                int currentKills = instance.getKilledMobsCount(cleanTarget);
                return currentKills >= amount;
            }

            case ZONE:
                return instance.getZoneProgress(this) >= timeRequired;

            case THROW_AT:
                return instance.getThrowHits(this) >= amount;

            case SHIELDED:
                boolean broken = false;
                if ("throw".equalsIgnoreCase(shieldType)) {
                    broken = instance.getThrowHits(this) >= amount;
                } else if ("event_removable".equalsIgnoreCase(shieldType)) {
                    broken = !instance.isShieldedEnemy(instance.getBossUuidForCondition(this));
                }
                if (!broken) return false;
                return instance.isBossDeadForCondition(this);

            case DROP_ON_DEATH:
                return true;

            case DYNAMIC_PATH: {
                Location targetEnd = getResolvedEndLocation(instance);
                for (Player p : instance.getOnlinePlayers()) {
                    if (!instance.isPlayerSpectator(p)) {
                        if (p.getLocation().distanceSquared(targetEnd) <= 2.25) { // within 1.5 blocks of endLoc
                            return true;
                        }
                    }
                }
                return false;
            }

            case INTERACT_BLOCK_WITH_ITEM:
                return this.interactedMet;

            case PERIODIC_CHECK:
                return true;

            case LOOKING_AT:
                Location lookingLoc = getResolvedLocation(instance);
                for (Player player : instance.getOnlinePlayers()) {
                    if (!instance.isPlayerSpectator(player)) {
                        double distSq = player.getLocation().distanceSquared(lookingLoc);
                        if (radius > 0.0 && distSq > radius * radius) {
                            continue;
                        }
                        if (instance.isLookingAt(player, lookingLoc.getX(), lookingLoc.getY(), lookingLoc.getZ())) {
                            return true;
                        }
                    }
                }
                return false;

            case ALIVE: {
                int count = 0;
                Location centerLoc = getResolvedLocation(instance);
                double rSq = radius * radius;
                for (org.bukkit.entity.Entity entity : instance.getWorld().getNearbyEntities(centerLoc, radius, radius, radius)) {
                    if (entity instanceof org.bukkit.entity.LivingEntity && !(entity instanceof Player) && !entity.isDead()) {
                        String name = entity.getName();
                        String cleanName = ChatColor.stripColor(name);
                        String targetName = (mobDisplayName != null && !mobDisplayName.isEmpty()) ? mobDisplayName : mobName;
                        String cleanTarget = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', targetName));
                        if (cleanName.equalsIgnoreCase(cleanTarget) || entity.getType().name().equalsIgnoreCase(cleanTarget)) {
                            if (entity.getLocation().distanceSquared(centerLoc) <= rSq) {
                                count++;
                            }
                        }
                    }
                }
                return count >= amount;
            }

            case COLLECT_POINTS:
                return instance.isCollectPointsMet(this);

            default:
                return false;
        }
    }

    public boolean isMetInteract(Location blockLoc, Material clickedBlock, ItemStack heldItem, DungeonInstance instance) {
        if (type != ConditionType.INTERACT_BLOCK_WITH_ITEM) return false;

        Location targetLoc = getResolvedLocation(instance);
        double distSq = blockLoc.distanceSquared(targetLoc);
        double maxDistSq = (radius > 0.0) ? (radius * radius) : 1.5;

        org.bukkit.Bukkit.getLogger().info("[Dungeon Debug] INTERACT_BLOCK_WITH_ITEM check triggered:");
        org.bukkit.Bukkit.getLogger().info("  Target Loc: (" + targetLoc.getX() + ", " + targetLoc.getY() + ", " + targetLoc.getZ() + ") | Clicked Loc: (" + blockLoc.getX() + ", " + blockLoc.getY() + ", " + blockLoc.getZ() + ")");
        org.bukkit.Bukkit.getLogger().info("  Distance: " + String.format("%.2f", Math.sqrt(distSq)) + " blocks (Max Allowed Radius: " + (radius > 0.0 ? radius : 1.22) + ")");

        if (distSq > maxDistSq) {
            org.bukkit.Bukkit.getLogger().warning("[Dungeon Debug] INTERACT FAILED: Distance check failed! (" + String.format("%.2f", Math.sqrt(distSq)) + " > " + String.format("%.2f", Math.sqrt(maxDistSq)) + ")");
            return false;
        }

        if (blockMaterial != null && clickedBlock != blockMaterial) {
            org.bukkit.Bukkit.getLogger().warning("[Dungeon Debug] INTERACT FAILED: Block Material mismatch! Clicked '" + clickedBlock + "' vs Target '" + blockMaterial + "'");
            return false;
        }

        if (requiredItems) {
            boolean itemMatched = false;
            String heldItemName = (heldItem != null && heldItem.hasItemMeta() && heldItem.getItemMeta().getDisplayName() != null) 
                ? ChatColor.stripColor(heldItem.getItemMeta().getDisplayName()) : "";
            String heldItemTypeStr = (heldItem != null) ? heldItem.getType().name() : "AIR";

            if (itemMaterial != null) {
                if (heldItem != null && heldItem.getType() == itemMaterial) {
                    if (itemDisplayName == null) {
                        itemMatched = true;
                    } else {
                        String cleanTarget = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', itemDisplayName));
                        if (heldItemName.equalsIgnoreCase(cleanTarget)) {
                            itemMatched = true;
                        }
                    }
                }
            }
            
            if (!itemMatched && customItemId != null && !customItemId.isEmpty()) {
                if (heldItem != null && !heldItem.getType().isAir()) {
                    // Check PDC
                    if (heldItem.hasItemMeta()) {
                        org.bukkit.persistence.PersistentDataContainer pdc = heldItem.getItemMeta().getPersistentDataContainer();
                        org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(Plugin.AmonPackPlugin.plugin, "dungeon_item_id");
                        if (pdc.has(key, org.bukkit.persistence.PersistentDataType.STRING)) {
                            String itemId = pdc.get(key, org.bukkit.persistence.PersistentDataType.STRING);
                            if (customItemId.equalsIgnoreCase(itemId)) {
                                itemMatched = true;
                            }
                        }
                    }

                    // Check Display Name
                    if (!itemMatched && !heldItemName.isEmpty()) {
                        String cleanTarget = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', customItemId));
                        if (heldItemName.equalsIgnoreCase(cleanTarget)) {
                            itemMatched = true;
                        }
                    }

                    // Check Item Type Name
                    if (!itemMatched && heldItemTypeStr.equalsIgnoreCase(customItemId)) {
                        itemMatched = true;
                    }
                }
            }

            if (!itemMatched && itemMaterial == null && (customItemId == null || customItemId.isEmpty())) {
                if (heldItem == null || heldItem.getType().isAir()) {
                    itemMatched = true;
                }
            }

            if (!itemMatched) {
                org.bukkit.Bukkit.getLogger().warning("[Dungeon Debug] INTERACT FAILED: Item mismatch! Held: " + heldItemTypeStr + " ('" + heldItemName + "') vs Target ItemMaterial: " + itemMaterial + " / CustomId: '" + customItemId + "'");
                return false;
            }
        }

        org.bukkit.Bukkit.getLogger().info("[Dungeon Debug] INTERACT SUCCESS! All conditions matched for INTERACT_BLOCK_WITH_ITEM.");
        this.interactedMet = true;
        return true;
    }

    public ConditionType getType() {
        return type;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public double getRadius() {
        return radius;
    }

    public String getMobName() {
        return mobName;
    }

    public String getMobDisplayName() {
        return mobDisplayName;
    }

    public void setMobDisplayName(String mobDisplayName) {
        this.mobDisplayName = mobDisplayName;
    }

    public int getAmount() {
        return amount;
    }

    public Material getBlockMaterial() {
        return blockMaterial;
    }

    public Material getItemMaterial() {
        return itemMaterial;
    }

    public String getItemDisplayName() {
        return itemDisplayName;
    }

    public String getCustomItemId() {
        return customItemId;
    }

    public double getChance() {
        return chance;
    }

    public int getTimeRequired() {
        return timeRequired;
    }

    public void setTimeRequired(int timeRequired) {
        this.timeRequired = timeRequired;
    }

    public List<DungeonEffect> getOnCompleteEffects() {
        return onCompleteEffects;
    }

    public void setOnCompleteEffects(List<DungeonEffect> onCompleteEffects) {
        this.onCompleteEffects = onCompleteEffects;
    }

    public void setCustomItemId(String customItemId) {
        this.customItemId = customItemId;
    }

    public void setX(double x) {
        this.x = x;
        if (this.xList.isEmpty()) {
            this.xList.add(x);
        } else {
            this.xList.set(0, x);
        }
    }

    public void setY(double y) {
        this.y = y;
        if (this.yList.isEmpty()) {
            this.yList.add(y);
        } else {
            this.yList.set(0, y);
        }
    }

    public void setZ(double z) {
        this.z = z;
        if (this.zList.isEmpty()) {
            this.zList.add(z);
        } else {
            this.zList.set(0, z);
        }
    }

    public List<Double> getXList() {
        return xList;
    }

    public void setXList(List<Double> xList) {
        this.xList = xList;
        if (!xList.isEmpty()) {
            this.x = xList.get(0);
        }
    }

    public List<Double> getYList() {
        return yList;
    }

    public void setYList(List<Double> yList) {
        this.yList = yList;
        if (!yList.isEmpty()) {
            this.y = yList.get(0);
        }
    }

    public List<Double> getZList() {
        return zList;
    }

    public void setZList(List<Double> zList) {
        this.zList = zList;
        if (!zList.isEmpty()) {
            this.z = zList.get(0);
        }
    }

    public String getShieldType() {
        return shieldType;
    }

    public void setShieldType(String shieldType) {
        this.shieldType = shieldType;
    }

    public Location getResolvedLocation(DungeonInstance instance) {
        return instance.getResolvedLocation(this);
    }

    public void setRadius(double radius) {
        this.radius = radius;
    }

    public void setMobName(String mobName) {
        this.mobName = mobName;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public String getRequirement() {
        return requirement;
    }

    public void setRequirement(String requirement) {
        this.requirement = requirement;
    }

    public int getInterval() {
        return interval;
    }

    public void setInterval(int interval) {
        this.interval = interval;
    }

    public List<DungeonEffect> getFailEffects() {
        return failEffects;
    }

    public void setFailEffects(List<DungeonEffect> failEffects) {
        this.failEffects = failEffects;
    }

    public List<DungeonEffect> getSuccessEffects() {
        return successEffects;
    }

    public void setSuccessEffects(List<DungeonEffect> successEffects) {
        this.successEffects = successEffects;
    }

    public boolean isOnce() {
        return once;
    }

    public void setOnce(boolean once) {
        this.once = once;
    }

    public boolean isRequiredAllPlayers() {
        return requiredAllPlayers;
    }

    public float getYaw() { return yaw; }
    public void setYaw(float yaw) { this.yaw = yaw; }

    public float getPitch() { return pitch; }
    public void setPitch(float pitch) { this.pitch = pitch; }

    public boolean hasCoords() { return hasCoords; }
    public void setHasCoords(boolean hasCoords) { this.hasCoords = hasCoords; }

    public boolean isFreezePlayers() { return freezePlayers; }
    public void setFreezePlayers(boolean freezePlayers) { this.freezePlayers = freezePlayers; }

    public double getMinX() { return minX; }
    public void setMinX(double minX) { this.minX = minX; }

    public double getMinY() { return minY; }
    public void setMinY(double minY) { this.minY = minY; }

    public double getMinZ() { return minZ; }
    public void setMinZ(double minZ) { this.minZ = minZ; }

    public double getMaxX() { return maxX; }
    public void setMaxX(double maxX) { this.maxX = maxX; }

    public double getMaxY() { return maxY; }
    public void setMaxY(double maxY) { this.maxY = maxY; }

    public double getMaxZ() { return maxZ; }
    public void setMaxZ(double maxZ) { this.maxZ = maxZ; }

    public double getStartX() { return startX; }
    public void setStartX(double startX) { this.startX = startX; }

    public double getStartY() { return startY; }
    public void setStartY(double startY) { this.startY = startY; }

    public double getStartZ() { return startZ; }
    public void setStartZ(double startZ) { this.startZ = startZ; }

    public boolean hasStartLoc() { return hasStartLoc; }
    public void setHasStartLoc(boolean hasStartLoc) { this.hasStartLoc = hasStartLoc; }

    public double getEndX() { return endX; }
    public void setEndX(double endX) { this.endX = endX; }

    public double getEndY() { return endY; }
    public void setEndY(double endY) { this.endY = endY; }

    public double getEndZ() { return endZ; }
    public void setEndZ(double endZ) { this.endZ = endZ; }

    public boolean hasEndLoc() { return hasEndLoc; }
    public void setHasEndLoc(boolean hasEndLoc) { this.hasEndLoc = hasEndLoc; }

    public double getRevealX() { return revealX; }
    public void setRevealX(double revealX) { this.revealX = revealX; }

    public double getRevealY() { return revealY; }
    public void setRevealY(double revealY) { this.revealY = revealY; }

    public double getRevealZ() { return revealZ; }
    public void setRevealZ(double revealZ) { this.revealZ = revealZ; }

    public double getRevealRadius() { return revealRadius; }
    public void setRevealRadius(double revealRadius) { this.revealRadius = revealRadius; }

    public int getReshuffleIntervalSeconds() { return reshuffleIntervalSeconds; }
    public void setReshuffleIntervalSeconds(int reshuffleIntervalSeconds) { this.reshuffleIntervalSeconds = reshuffleIntervalSeconds; }

    public Material getSafeBlockMaterial() { return safeBlockMaterial; }
    public void setSafeBlockMaterial(Material safeBlockMaterial) { this.safeBlockMaterial = safeBlockMaterial; }

    public Material getCrumbleBlockMaterial() { return crumbleBlockMaterial; }
    public void setCrumbleBlockMaterial(Material crumbleBlockMaterial) { this.crumbleBlockMaterial = crumbleBlockMaterial; }

    public void setRequiredAllPlayers(boolean requiredAllPlayers) {
        this.requiredAllPlayers = requiredAllPlayers;
    }

    public boolean isRequiredItems() {
        return requiredItems;
    }

    public void setRequiredItems(boolean requiredItems) {
        this.requiredItems = requiredItems;
    }
}

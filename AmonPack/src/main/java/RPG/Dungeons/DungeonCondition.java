package RPG.Dungeons;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
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
        COLLECT_POINTS
    }

    private final ConditionType type;
    
    private double x, y, z;
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
        if (blockLoc.distanceSquared(targetLoc) > 1.5) return false;

        if (blockMaterial != null && clickedBlock != blockMaterial) return false;

        if (requiredItems) {
            if (itemMaterial != null) {
                if (heldItem == null || heldItem.getType() != itemMaterial) return false;
                
                if (itemDisplayName != null) {
                    if (!heldItem.hasItemMeta() || heldItem.getItemMeta().getDisplayName() == null) return false;
                    
                    String cleanMetaName = ChatColor.stripColor(heldItem.getItemMeta().getDisplayName());
                    String cleanTargetName = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', itemDisplayName));
                    
                    if (!cleanMetaName.equalsIgnoreCase(cleanTargetName)) return false;
                }
            } else if (customItemId != null) {
                if (heldItem == null) return false;
                org.bukkit.persistence.PersistentDataContainer pdc = heldItem.getItemMeta().getPersistentDataContainer();
                org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(Plugin.AmonPackPlugin.plugin, "dungeon_item_id");
                if (!pdc.has(key, org.bukkit.persistence.PersistentDataType.STRING)) return false;
                String itemId = pdc.get(key, org.bukkit.persistence.PersistentDataType.STRING);
                if (!customItemId.equalsIgnoreCase(itemId)) return false;
            } else {
                if (heldItem != null && !heldItem.getType().isAir()) return false;
            }
        }

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

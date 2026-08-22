package CustomContent.Bosses;

import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.EntityType;

import java.util.ArrayList;
import java.util.List;

public class CustomBoss {

    public static class BossSkill {
        public String trigger; // TIMER, HEALTH_BELOW, ON_HIT
        public int intervalSeconds;
        public double healthPercent;
        public String ability;
        public double range = 15.0;
        public String announcement;
        public boolean executed = false;
    }

    public static class BossDrop {
        public String customItemId;
        public double chance = 1.0;
        public int min = 1;
        public int max = 1;
    }

    private final String id;
    private String displayName;
    private EntityType baseEntity = EntityType.HUSK;

    // Model 3D
    private String modelPath;
    private int customModelData = 20001;
    private double scale = 2.0;

    // Stats
    private double maxHealth = 500.0;
    private double attackDamage = 14.0;
    private double movementSpeed = 0.28;
    private double knockbackResistance = 1.0;
    private double followRange = 35.0;

    // BossBar
    private boolean bossBarEnabled = true;
    private String bossBarTitle = "&c&lBoss &7- &e{health}&7/&e{max_health} HP";
    private BarColor bossBarColor = BarColor.RED;
    private BarStyle bossBarStyle = BarStyle.SEGMENTED_10;
    private double bossBarRange = 35.0;

    // Skills & Drops
    private List<BossSkill> skills = new ArrayList<>();
    private List<BossDrop> drops = new ArrayList<>();
    private int expDrop = 250;

    public CustomBoss(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public EntityType getBaseEntity() { return baseEntity; }
    public void setBaseEntity(EntityType baseEntity) { this.baseEntity = baseEntity; }

    public String getModelPath() { return modelPath; }
    public void setModelPath(String modelPath) { this.modelPath = modelPath; }
    public int getCustomModelData() { return customModelData; }
    public void setCustomModelData(int customModelData) { this.customModelData = customModelData; }
    public double getScale() { return scale; }
    public void setScale(double scale) { this.scale = scale; }

    public double getMaxHealth() { return maxHealth; }
    public void setMaxHealth(double maxHealth) { this.maxHealth = maxHealth; }
    public double getAttackDamage() { return attackDamage; }
    public void setAttackDamage(double attackDamage) { this.attackDamage = attackDamage; }
    public double getMovementSpeed() { return movementSpeed; }
    public void setMovementSpeed(double movementSpeed) { this.movementSpeed = movementSpeed; }
    public double getKnockbackResistance() { return knockbackResistance; }
    public void setKnockbackResistance(double knockbackResistance) { this.knockbackResistance = knockbackResistance; }
    public double getFollowRange() { return followRange; }
    public void setFollowRange(double followRange) { this.followRange = followRange; }

    public boolean isBossBarEnabled() { return bossBarEnabled; }
    public void setBossBarEnabled(boolean bossBarEnabled) { this.bossBarEnabled = bossBarEnabled; }
    public String getBossBarTitle() { return bossBarTitle; }
    public void setBossBarTitle(String bossBarTitle) { this.bossBarTitle = bossBarTitle; }
    public BarColor getBossBarColor() { return bossBarColor; }
    public void setBossBarColor(BarColor bossBarColor) { this.bossBarColor = bossBarColor; }
    public BarStyle getBossBarStyle() { return bossBarStyle; }
    public void setBossBarStyle(BarStyle bossBarStyle) { this.bossBarStyle = bossBarStyle; }
    public double getBossBarRange() { return bossBarRange; }
    public void setBossBarRange(double bossBarRange) { this.bossBarRange = bossBarRange; }

    public List<BossSkill> getSkills() { return skills; }
    public List<BossDrop> getDrops() { return drops; }
    public int getExpDrop() { return expDrop; }
    public void setExpDrop(int expDrop) { this.expDrop = expDrop; }
}

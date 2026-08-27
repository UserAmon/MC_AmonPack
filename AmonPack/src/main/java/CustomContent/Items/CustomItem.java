package CustomContent.Items;

import org.bukkit.Material;
import java.util.ArrayList;
import java.util.List;

public class CustomItem {

    private final String id;
    private String displayName;
    private Material baseMaterial;
    private int customModelData;
    private boolean unbreakable = true;
    private List<String> lore = new ArrayList<>();

    // Custom stats
    private double attackDamage = 0;
    private double attackSpeed = 0;
    private double movementSpeed = 0;
    private double armor = 0;
    private double armorToughness = 0;

    // On-hit effects
    private String onHitEffectType = null; // np. BLEED, LIFESTEAL, METEOR_SMASH
    private double effectChance = 0.0;
    private int effectDuration = 0;
    private double effectValue = 0.0;

    public CustomItem(String id, String displayName, Material baseMaterial, int customModelData) {
        this.id = id;
        this.displayName = displayName;
        this.baseMaterial = baseMaterial;
        this.customModelData = customModelData;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public Material getBaseMaterial() { return baseMaterial; }
    public void setBaseMaterial(Material baseMaterial) { this.baseMaterial = baseMaterial; }
    public int getCustomModelData() { return customModelData; }
    public void setCustomModelData(int customModelData) { this.customModelData = customModelData; }
    public boolean isUnbreakable() { return unbreakable; }
    public void setUnbreakable(boolean unbreakable) { this.unbreakable = unbreakable; }
    public List<String> getLore() { return lore; }
    public void setLore(List<String> lore) { this.lore = lore; }

    public double getAttackDamage() { return attackDamage; }
    public void setAttackDamage(double attackDamage) { this.attackDamage = attackDamage; }
    public double getAttackSpeed() { return attackSpeed; }
    public void setAttackSpeed(double attackSpeed) { this.attackSpeed = attackSpeed; }
    public double getMovementSpeed() { return movementSpeed; }
    public void setMovementSpeed(double movementSpeed) { this.movementSpeed = movementSpeed; }
    public double getArmor() { return armor; }
    public void setArmor(double armor) { this.armor = armor; }
    public double getArmorToughness() { return armorToughness; }
    public void setArmorToughness(double armorToughness) { this.armorToughness = armorToughness; }

    public String getOnHitEffectType() { return onHitEffectType; }
    public void setOnHitEffectType(String onHitEffectType) { this.onHitEffectType = onHitEffectType; }
    public double getEffectChance() { return effectChance; }
    public void setEffectChance(double effectChance) { this.effectChance = effectChance; }
    public int getEffectDuration() { return effectDuration; }
    public void setEffectDuration(int effectDuration) { this.effectDuration = effectDuration; }
    public double getEffectValue() { return effectValue; }
    public void setEffectValue(double effectValue) { this.effectValue = effectValue; }

    private String model;
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
}

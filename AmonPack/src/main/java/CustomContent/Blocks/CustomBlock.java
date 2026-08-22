package CustomContent.Blocks;

import org.bukkit.Material;

public class CustomBlock {

    private final String id;
    private String displayName;
    private Material baseMaterial;
    private int customModelData;
    private double hardness = 3.0; // sekundy/wytrzymałość
    private String requiredTool = "PICKAXE"; // PICKAXE, AXE, SHOVEL
    private int requiredTier = 2; // 0=drewno, 1=kamień, 2=żelazo, 3=diament, 4=netherite

    // Drops
    private String dropCustomItemId;
    private Material dropVanillaMaterial;
    private int dropMin = 1;
    private int dropMax = 2;
    private double exp = 10.0;

    // Generation
    private boolean generateInWorld = true;
    private int veinSize = 4;
    private int veinsPerChunk = 6;
    private int minY = -64;
    private int maxY = 32;

    public CustomBlock(String id, String displayName, Material baseMaterial, int customModelData) {
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
    public double getHardness() { return hardness; }
    public void setHardness(double hardness) { this.hardness = hardness; }
    public String getRequiredTool() { return requiredTool; }
    public void setRequiredTool(String requiredTool) { this.requiredTool = requiredTool; }
    public int getRequiredTier() { return requiredTier; }
    public void setRequiredTier(int requiredTier) { this.requiredTier = requiredTier; }

    public String getDropCustomItemId() { return dropCustomItemId; }
    public void setDropCustomItemId(String dropCustomItemId) { this.dropCustomItemId = dropCustomItemId; }
    public Material getDropVanillaMaterial() { return dropVanillaMaterial; }
    public void setDropVanillaMaterial(Material dropVanillaMaterial) { this.dropVanillaMaterial = dropVanillaMaterial; }
    public int getDropMin() { return dropMin; }
    public void setDropMin(int dropMin) { this.dropMin = dropMin; }
    public int getDropMax() { return dropMax; }
    public void setDropMax(int dropMax) { this.dropMax = dropMax; }
    public double getExp() { return exp; }
    public void setExp(double exp) { this.exp = exp; }

    public boolean isGenerateInWorld() { return generateInWorld; }
    public void setGenerateInWorld(boolean generateInWorld) { this.generateInWorld = generateInWorld; }
    public int getVeinSize() { return veinSize; }
    public void setVeinSize(int veinSize) { this.veinSize = veinSize; }
    public int getVeinsPerChunk() { return veinsPerChunk; }
    public void setVeinsPerChunk(int veinsPerChunk) { this.veinsPerChunk = veinsPerChunk; }
    public int getMinY() { return minY; }
    public void setMinY(int minY) { this.minY = minY; }
    public int getMaxY() { return maxY; }
    public void setMaxY(int maxY) { this.maxY = maxY; }
}

package RPG.Dungeons;

import org.bukkit.Material;

public class DungeonPlatform {
    private final double x1, y1, z1;
    private final double x2, y2, z2;
    private final Material material;
    private final boolean inverted;
    private final String testMode;
    private final String requirement;

    public DungeonPlatform(double x1, double y1, double z1, double x2, double y2, double z2, Material material, boolean inverted, String testMode, String requirement) {
        this.x1 = x1;
        this.y1 = y1;
        this.z1 = z1;
        this.x2 = x2;
        this.y2 = y2;
        this.z2 = z2;
        this.material = material;
        this.inverted = inverted;
        this.testMode = testMode != null ? testMode : "GLOBAL";
        this.requirement = requirement != null ? requirement : "";
    }

    public double getX1() {
        return x1;
    }

    public double getY1() {
        return y1;
    }

    public double getZ1() {
        return z1;
    }

    public double getX2() {
        return x2;
    }

    public double getY2() {
        return y2;
    }

    public double getZ2() {
        return z2;
    }

    public Material getMaterial() {
        return material;
    }

    public boolean isInverted() {
        return inverted;
    }

    public String getTestMode() {
        return testMode;
    }

    public String getRequirement() {
        return requirement;
    }
}

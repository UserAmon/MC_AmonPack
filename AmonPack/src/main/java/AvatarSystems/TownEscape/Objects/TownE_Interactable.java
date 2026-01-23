package AvatarSystems.TownEscape.Objects;

import org.bukkit.Location;
import org.bukkit.block.Block;

public class TownE_Interactable {
    private TE_InteractableTypes type;
    private Location location;
    private Location location2;
    private int InteracRadius;
    private int Cost;
    private org.bukkit.Material DoorMaterial;

    public TownE_Interactable(int interacRadius, Location location, TE_InteractableTypes type) {
        this(interacRadius, location, type, 0, null, null);
    }

    public TownE_Interactable(int interacRadius, Location location, TE_InteractableTypes type, int cost,
            Location location2, org.bukkit.Material doorMaterial) {
        InteracRadius = interacRadius;
        this.location = location;
        this.type = type;
        Cost = cost;
        this.location2 = location2;
        DoorMaterial = doorMaterial;
    }

    public boolean PlayerHasInteracted(Block block) {
        if (type == TE_InteractableTypes.DOORS && location2 != null) {
            return isInside(block.getLocation());
        }
        return block.getLocation().distance(location) <= InteracRadius;
    }

    public boolean isInside(Location loc) {
        if (location == null || location2 == null)
            return false;
        double x1 = Math.min(location.getX(), location2.getX());
        double y1 = Math.min(location.getY(), location2.getY());
        double z1 = Math.min(location.getZ(), location2.getZ());
        double x2 = Math.max(location.getX(), location2.getX());
        double y2 = Math.max(location.getY(), location2.getY());
        double z2 = Math.max(location.getZ(), location2.getZ());

        return loc.getX() >= x1 && loc.getX() <= x2 &&
                loc.getY() >= y1 && loc.getY() <= y2 &&
                loc.getZ() >= z1 && loc.getZ() <= z2;
    }

    public TE_InteractableTypes getType() {
        return type;
    }

    public Location getLocation() {
        return location;
    }

    public Location getLocation2() {
        return location2;
    }

    public int getCost() {
        return Cost;
    }

    public org.bukkit.Material getDoorMaterial() {
        return DoorMaterial;
    }

    public enum TE_InteractableTypes {
        DOORS,
        CHEST
    }

    public TownE_Interactable clone() {
        return new TownE_Interactable(InteracRadius, location.clone(), type, Cost,
                location2 != null ? location2.clone() : null, DoorMaterial);
    }
}

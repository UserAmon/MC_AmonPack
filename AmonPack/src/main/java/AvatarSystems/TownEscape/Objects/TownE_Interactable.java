package AvatarSystems.TownEscape.Objects;

import org.bukkit.Location;
import org.bukkit.block.Block;

public class TownE_Interactable {
    private TE_InteractableTypes type;
    private Location location;
    private int InteracRadius;

    public TownE_Interactable(int interacRadius, Location location, TE_InteractableTypes type) {
        InteracRadius = interacRadius;
        this.location = location;
        this.type = type;
    }

    public boolean PlayerHasInteracted(Block block){
        return block.getLocation().distance(location) <= InteracRadius;
    }
    public enum TE_InteractableTypes{
        DOORS,
        CHEST
    }
}

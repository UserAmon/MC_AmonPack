package AvatarSystems.TownEscape.Objects;

import org.bukkit.Location;
import org.bukkit.block.Block;

public class TownE_Interactable {
    private TE_InteractableTypes type;
    private Location location;
    private int InteracRadius;


    public boolean PlayerHasInteracted(Block block){
        return block.getLocation().distance(location) <= InteracRadius;
    }
    public enum TE_InteractableTypes{
        DOORS,
        CHEST
    }
}

package AvatarSystems.TownEscape.Objects;

import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;

public class TownE_Room {
    private String ArenaName;
    private List<Location> EnemySpawnAreas = new ArrayList<>();
    private List<TownE_Interactable> Interactable = new ArrayList<>();

    public TownE_Room(String arenaName, List<Location> enemySpawnAreas, List<TownE_Interactable> interactable) {
        ArenaName = arenaName;
        EnemySpawnAreas = enemySpawnAreas;
        Interactable = interactable;
    }

    public List<Location> getEnemySpawnAreas() {
        return EnemySpawnAreas;
    }

    public String getArenaName() {
        return ArenaName;
    }

    public List<TownE_Interactable> getInteractable() {
        return Interactable;
    }

    public TownE_Room clone() {
        List<Location> clonedSpawns = new ArrayList<>(EnemySpawnAreas); // Locations are mutable but usually treated as
                                                                        // values. Deep copy if needed.
        List<TownE_Interactable> clonedInteractables = new ArrayList<>();
        for (TownE_Interactable interactable : Interactable) {
            clonedInteractables.add(interactable.clone());
        }
        return new TownE_Room(ArenaName, clonedSpawns, clonedInteractables);
    }
}

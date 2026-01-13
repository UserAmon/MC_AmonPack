package AvatarSystems.TownEscape.Objects;

import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;

public class TownE_Room {
    private String ArenaName;
    private boolean IsActive;
    private List<Location> EnemySpawnAreas=new ArrayList<>();
    private List<TownE_Interactable> Interactable=new ArrayList<>();

    public List<Location> getEnemySpawnAreas() {
        return EnemySpawnAreas;
    }
    public String getArenaName() {
        return ArenaName;
    }
    public void SetActive(){
        IsActive=true;
    }
}

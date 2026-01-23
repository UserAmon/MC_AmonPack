package AvatarSystems.TownEscape.Objects;

import org.bukkit.Location;
import java.util.ArrayList;
import java.util.List;

public class TownE_GameMap {
    private String MapName;
    private Location PlayerSpawn;
    private List<TownE_Room> Rooms;
    private List<TownE_Round> Rounds;

    public TownE_GameMap(String mapName, Location playerSpawn, List<TownE_Room> rooms, List<TownE_Round> rounds) {
        MapName = mapName;
        PlayerSpawn = playerSpawn;
        Rooms = rooms;
        Rounds = rounds;
    }

    public String getMapName() {
        return MapName;
    }

    public Location getPlayerSpawn() {
        return PlayerSpawn.clone();
    }

    public List<TownE_Room> getRooms() {
        List<TownE_Room> copiedRooms = new ArrayList<>();
        for (TownE_Room room : Rooms) {
            copiedRooms.add(room.clone());
        }
        return copiedRooms;
    }

    public List<TownE_Round> getRounds() {
        // Rounds are likely immutable enough, but let's clone list at least
        return new ArrayList<>(Rounds);
    }
}

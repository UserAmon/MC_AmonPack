package AvatarSystems.TownEscape;

import AvatarSystems.TownEscape.Objects.TownE_Room;
import AvatarSystems.TownEscape.Objects.TownE_Session;
import org.bukkit.entity.Player;

import java.util.*;

public class TownEscapeMenager {
    private final List<TownE_Session> ActiveSesions = new ArrayList<>();
    private final Map<UUID, TownE_Session> playerSession = new HashMap<>();
    private List<TownE_Room> LoadedRooms;

    public TownEscapeMenager(){
        LoadConfig();
    }
    public void LoadConfig(){
        LoadedRooms=new ArrayList<>();
    }

    public TownE_Session getSessionByPlayer(Player p) {
        return playerSession.get(p.getUniqueId());
    }
    public void assignPlayer(Player p, TownE_Session session) {
        playerSession.put(p.getUniqueId(), session);
    }
}

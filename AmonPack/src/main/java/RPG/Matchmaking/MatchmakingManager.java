package RPG.Matchmaking;

import RPG.Party.Party;
import RPG.Party.PartyManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.*;

public class MatchmakingManager {
    private static MatchmakingManager instance;

    // Active lobbies: owner UUID -> Lobby
    private final Map<UUID, MatchmakingLobby> lobbies = new HashMap<>();

    private MatchmakingManager() {
        instance = this;
    }

    public static MatchmakingManager getInstance() {
        if (instance == null) {
            new MatchmakingManager();
        }
        return instance;
    }

    public Map<UUID, MatchmakingLobby> getLobbies() {
        return lobbies;
    }

    public MatchmakingLobby getLobbyByOwner(UUID ownerUUID) {
        return lobbies.get(ownerUUID);
    }

    public MatchmakingLobby getLobbyByMember(UUID playerUUID) {
        for (MatchmakingLobby lobby : lobbies.values()) {
            if (lobby.getTemporaryMembers().contains(playerUUID)) {
                return lobby;
            }
        }
        return null;
    }

    public List<MatchmakingLobby> getLobbiesForDungeon(String dungeonId) {
        List<MatchmakingLobby> result = new ArrayList<>();
        for (MatchmakingLobby lobby : lobbies.values()) {
            if (lobby.getDungeonId().equalsIgnoreCase(dungeonId)) {
                result.add(lobby);
            }
        }
        return result;
    }

    public MatchmakingLobby createLobby(Player owner, String dungeonId) {
        UUID ownerUUID = owner.getUniqueId();
        
        // Remove existing lobby if any
        removeLobby(ownerUUID);

        MatchmakingLobby lobby = new MatchmakingLobby(ownerUUID, owner.getName(), dungeonId);
        lobbies.put(ownerUUID, lobby);

        // Set up temporary party in PartyManager
        PartyManager pm = PartyManager.getInstance();
        Party party = pm.getParty(ownerUUID);
        if (party == null) {
            // Create in-memory temporary party
            party = new Party(UUID.randomUUID(), ownerUUID);
            // Save it only in memory maps of PartyManager
            pm.saveParty(party); 
        }

        owner.sendMessage(ChatColor.GREEN + "Stworzyłeś pokój matchmakingowy dla lochu " + dungeonId + "!");
        return lobby;
    }

    public void removeLobby(UUID ownerUUID) {
        MatchmakingLobby lobby = lobbies.remove(ownerUUID);
        if (lobby != null) {
            Player owner = Bukkit.getPlayer(ownerUUID);
            if (owner != null && owner.isOnline()) {
                owner.sendMessage(ChatColor.YELLOW + "Twój pokój matchmakingowy został zamknięty.");
            }
            
            // Clear temporary party from members
            PartyManager pm = PartyManager.getInstance();
            Party party = pm.getParty(ownerUUID);
            if (party != null) {
                // If it was temporary, we leave it
                for (UUID uuid : new ArrayList<>(party.getMembers())) {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null && p.isOnline() && !uuid.equals(ownerUUID)) {
                        pm.leaveParty(p);
                        p.sendMessage(ChatColor.RED + "Pokój matchmakingowy został rozwiązany przez założyciela.");
                    }
                }
            }
        }
    }

    public void requestJoin(Player player, UUID ownerUUID) {
        MatchmakingLobby lobby = lobbies.get(ownerUUID);
        if (lobby == null) {
            player.sendMessage(ChatColor.RED + "Ten pokój matchmakingowy już nie istnieje!");
            return;
        }

        if (lobby.getOwnerUUID().equals(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Jesteś już założycielem tego pokoju!");
            return;
        }

        if (lobby.getTemporaryMembers().size() >= 5) {
            player.sendMessage(ChatColor.RED + "Ten pokój matchmakingowy jest pełny!");
            return;
        }

        lobby.addRequest(player.getUniqueId());
        player.sendMessage(ChatColor.GREEN + "Wysłałeś prośbę o dołączenie do pokoju gracza " + lobby.getOwnerName() + ".");

        Player owner = Bukkit.getPlayer(ownerUUID);
        if (owner != null && owner.isOnline()) {
            owner.sendMessage(ChatColor.GOLD + "========================================");
            owner.sendMessage(ChatColor.YELLOW + "Gracz " + player.getName() + " chce dołączyć do Twojego Matchmakingu!");
            owner.sendMessage(ChatColor.YELLOW + "Otwórz /dungeons start, aby zarządzać prośbami.");
            owner.sendMessage(ChatColor.GOLD + "========================================");
        }
    }

    public void acceptRequest(UUID ownerUUID, UUID targetUUID) {
        MatchmakingLobby lobby = lobbies.get(ownerUUID);
        if (lobby == null) return;

        if (lobby.getTemporaryMembers().size() >= 5) {
            Player owner = Bukkit.getPlayer(ownerUUID);
            if (owner != null) owner.sendMessage(ChatColor.RED + "Pokój jest już pełny!");
            return;
        }

        lobby.addTemporaryMember(targetUUID);

        Player target = Bukkit.getPlayer(targetUUID);
        Player owner = Bukkit.getPlayer(ownerUUID);

        // Join them in PartyManager
        PartyManager pm = PartyManager.getInstance();
        Party party = pm.getParty(ownerUUID);
        if (party != null) {
            // Add member to temporary party
            if (target != null && target.isOnline()) {
                pm.leaveParty(target); // Leave old party first
                party.addMember(targetUUID);
                pm.saveParty(party); // update SQLite or memory
                
                target.sendMessage(ChatColor.GREEN + "Zostałeś zaakceptowany do pokoju matchmakingu gracza " + lobby.getOwnerName() + "!");
            }
        }

        if (owner != null) {
            String targetName = target != null ? target.getName() : targetUUID.toString();
            owner.sendMessage(ChatColor.GREEN + "Zaakceptowałeś gracza " + targetName + " do pokoju.");
        }
    }

    public void rejectRequest(UUID ownerUUID, UUID targetUUID) {
        MatchmakingLobby lobby = lobbies.get(ownerUUID);
        if (lobby == null) return;

        lobby.removeRequest(targetUUID);

        Player target = Bukkit.getPlayer(targetUUID);
        if (target != null && target.isOnline()) {
            target.sendMessage(ChatColor.RED + "Twoja prośba o dołączenie do pokoju gracza " + lobby.getOwnerName() + " została odrzucona.");
        }

        Player owner = Bukkit.getPlayer(ownerUUID);
        if (owner != null) {
            String targetName = target != null ? target.getName() : targetUUID.toString();
            owner.sendMessage(ChatColor.YELLOW + "Odrzuciłeś prośbę gracza " + targetName + ".");
        }
    }

    public void handlePlayerQuit(UUID playerUUID) {
        // If owner quits, close lobby
        if (lobbies.containsKey(playerUUID)) {
            removeLobby(playerUUID);
        } else {
            // Remove from any lobbies they requested or joined
            for (MatchmakingLobby lobby : lobbies.values()) {
                if (lobby.getTemporaryMembers().contains(playerUUID)) {
                    lobby.removeTemporaryMember(playerUUID);
                    // Also leave party
                    Player p = Bukkit.getPlayer(playerUUID);
                    if (p != null) {
                        PartyManager.getInstance().leaveParty(p);
                    }
                }
                lobby.removeRequest(playerUUID);
            }
        }
    }
}

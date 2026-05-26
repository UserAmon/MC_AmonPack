package RPG.Party;

import Plugin.AmonPackPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;

public class PartyManager {
    private static PartyManager instance;

    // In-memory registry
    private final Map<UUID, Party> activeParties = new HashMap<>();
    private final Map<UUID, UUID> playerPartyMap = new HashMap<>(); // player UUID -> party UUID

    // Pending invites: invited UUID -> inviter UUID
    private final Map<UUID, UUID> pendingInvites = new HashMap<>();
    private final Map<UUID, Long> inviteTimestamps = new HashMap<>();

    // Toggled chat: player UUID -> true (sends default chat messages to party chat)
    private final Set<UUID> partyChatToggled = new HashSet<>();

    private PartyManager() {
        instance = this;
    }

    public static PartyManager getInstance() {
        if (instance == null) {
            new PartyManager();
        }
        return instance;
    }

    /**
     * Gets a player's party, loading it from the database if not present in memory.
     */
    public Party getParty(UUID playerUUID) {
        if (playerPartyMap.containsKey(playerUUID)) {
            return activeParties.get(playerPartyMap.get(playerUUID));
        }
        return loadPlayerParty(playerUUID);
    }

    /**
     * Loads a party from the SQLite database.
     */
    public Party loadPlayerParty(UUID playerUUID) {
        try {
            Connection conn = AmonPackPlugin.mysqllite().getConnection();
            Statement stmt = conn.createStatement();

            // 1. Find if player has a party in PartyMembers
            ResultSet rs = stmt.executeQuery("SELECT party_id FROM PartyMembers WHERE player_uuid='" + playerUUID.toString() + "'");
            if (rs.next()) {
                String partyIdStr = rs.getString("party_id");
                rs.close();

                UUID partyId = UUID.fromString(partyIdStr);

                // If already loaded in memory by another member
                if (activeParties.containsKey(partyId)) {
                    Party party = activeParties.get(partyId);
                    playerPartyMap.put(playerUUID, partyId);
                    stmt.close();
                    return party;
                }

                // 2. Load party details from Parties
                ResultSet rsParty = stmt.executeQuery("SELECT leader_uuid, friendly_fire FROM Parties WHERE party_id='" + partyIdStr + "'");
                if (rsParty.next()) {
                    UUID leaderUUID = UUID.fromString(rsParty.getString("leader_uuid"));
                    boolean friendlyFire = rsParty.getInt("friendly_fire") == 1;
                    rsParty.close();

                    Party party = new Party(partyId, leaderUUID);
                    party.setFriendlyFire(friendlyFire);

                    // 3. Load all members
                    ResultSet rsMembers = stmt.executeQuery("SELECT player_uuid FROM PartyMembers WHERE party_id='" + partyIdStr + "'");
                    party.getMembers().clear(); // Clear default constructor add
                    while (rsMembers.next()) {
                        UUID memberUUID = UUID.fromString(rsMembers.getString("player_uuid"));
                        party.addMember(memberUUID);
                        playerPartyMap.put(memberUUID, partyId);
                    }
                    rsMembers.close();

                    activeParties.put(partyId, party);
                    stmt.close();
                    return party;
                }
                rsParty.close();
            }
            rs.close();
            stmt.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Saves or updates a party and its members in SQLite.
     */
    public void saveParty(Party party) {
        if (party == null) return;
        try {
            int ff = party.isFriendlyFireEnabled() ? 1 : 0;
            // 1. Insert or replace Parties
            AmonPackPlugin.ExecuteQuery("INSERT OR REPLACE INTO Parties (party_id, leader_uuid, friendly_fire) VALUES ('"
                    + party.getPartyId() + "', '" + party.getLeaderUUID() + "', " + ff + ")");

            // 2. Delete old members
            AmonPackPlugin.ExecuteQuery("DELETE FROM PartyMembers WHERE party_id = '" + party.getPartyId() + "'");

            // 3. Re-insert members
            for (UUID memberUUID : party.getMembers()) {
                AmonPackPlugin.ExecuteQuery("INSERT OR REPLACE INTO PartyMembers (player_uuid, party_id) VALUES ('"
                        + memberUUID.toString() + "', '" + party.getPartyId().toString() + "')");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Deletes a party and its members from the SQLite database.
     */
    public void deletePartyFromDB(Party party) {
        if (party == null) return;
        try {
            AmonPackPlugin.ExecuteQuery("DELETE FROM Parties WHERE party_id = '" + party.getPartyId() + "'");
            AmonPackPlugin.ExecuteQuery("DELETE FROM PartyMembers WHERE party_id = '" + party.getPartyId() + "'");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends an invite to another player.
     */
    public void invitePlayer(Player inviter, Player target) {
        if (inviter.getUniqueId().equals(target.getUniqueId())) {
            inviter.sendMessage(ChatColor.RED + "Nie możesz zaprosić samego siebie!");
            return;
        }

        Party party = getParty(inviter.getUniqueId());
        if (party != null && !party.getLeaderUUID().equals(inviter.getUniqueId())) {
            inviter.sendMessage(ChatColor.RED + "Tylko lider drużyny może zapraszać innych graczy!");
            return;
        }

        if (party != null && party.getMembers().size() >= 5) {
            inviter.sendMessage(ChatColor.RED + "Twoja drużyna jest już pełna (maksymalnie 5 osób)!");
            return;
        }

        // Register invite
        pendingInvites.put(target.getUniqueId(), inviter.getUniqueId());
        inviteTimestamps.put(target.getUniqueId(), System.currentTimeMillis());

        inviter.sendMessage(ChatColor.GREEN + "Wysłałeś zaproszenie do gracza " + target.getName() + ".");
        target.sendMessage(ChatColor.GOLD + "========================================");
        target.sendMessage(ChatColor.YELLOW + "Gracz " + inviter.getName() + " zaprasza Cię do drużyny!");
        target.sendMessage(ChatColor.YELLOW + "Wpisz " + ChatColor.GREEN + "/party accept" + ChatColor.YELLOW + ", aby dołączyć.");
        target.sendMessage(ChatColor.GRAY + "Zaproszenie wygaśnie za 60 sekund.");
        target.sendMessage(ChatColor.GOLD + "========================================");
    }

    /**
     * Accepts a pending invite.
     */
    public void acceptInvite(Player player) {
        UUID playerUUID = player.getUniqueId();
        if (!pendingInvites.containsKey(playerUUID)) {
            player.sendMessage(ChatColor.RED + "Nie masz żadnych oczekujących zaproszeń do drużyny!");
            return;
        }

        Long timestamp = inviteTimestamps.get(playerUUID);
        if (timestamp == null || System.currentTimeMillis() - timestamp > 60000) {
            pendingInvites.remove(playerUUID);
            inviteTimestamps.remove(playerUUID);
            player.sendMessage(ChatColor.RED + "Twoje zaproszenie wygasło!");
            return;
        }

        UUID inviterUUID = pendingInvites.remove(playerUUID);
        inviteTimestamps.remove(playerUUID);

        Player inviter = Bukkit.getPlayer(inviterUUID);
        if (inviter == null || !inviter.isOnline()) {
            player.sendMessage(ChatColor.RED + "Gracz zapraszający jest offline.");
            return;
        }

        Party party = getParty(inviterUUID);
        if (party == null) {
            // Create a brand new party
            UUID partyId = UUID.randomUUID();
            party = new Party(partyId, inviterUUID);
            activeParties.put(partyId, party);
            playerPartyMap.put(inviterUUID, partyId);
            saveParty(party);
            inviter.sendMessage(ChatColor.GREEN + "Utworzyłeś nową drużynę!");
        }

        if (party.getMembers().size() >= 5) {
            player.sendMessage(ChatColor.RED + "Ta drużyna jest już pełna!");
            return;
        }

        // Leave existing party if any
        leaveParty(player);

        // Add to new party
        party.addMember(playerUUID);
        playerPartyMap.put(playerUUID, party.getPartyId());
        saveParty(party);

        party.getMembers().forEach(uuid -> {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                p.sendMessage(ChatColor.GREEN + player.getName() + " dołączył do drużyny!");
            }
        });
    }

    /**
     * Removes a player from their current party.
     */
    public void leaveParty(Player player) {
        UUID playerUUID = player.getUniqueId();
        Party party = getParty(playerUUID);
        if (party == null) return;

        party.removeMember(playerUUID);
        playerPartyMap.remove(playerUUID);
        partyChatToggled.remove(playerUUID);

        player.sendMessage(ChatColor.YELLOW + "Opuściłeś drużynę.");

        // Broadcast to other members
        party.getMembers().forEach(uuid -> {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                p.sendMessage(ChatColor.RED + player.getName() + " opuścił drużynę.");
            }
        });

        // Handle leadership/disband
        if (party.getMembers().isEmpty()) {
            activeParties.remove(party.getPartyId());
            deletePartyFromDB(party);
        } else {
            if (party.getLeaderUUID().equals(playerUUID)) {
                // Elect new leader
                UUID newLeader = party.getMembers().get(0);
                party.setLeaderUUID(newLeader);
                saveParty(party);

                Player pLeader = Bukkit.getPlayer(newLeader);
                if (pLeader != null && pLeader.isOnline()) {
                    pLeader.sendMessage(ChatColor.GOLD + "Zostałeś nowym liderem drużyny!");
                }
            } else {
                saveParty(party);
            }
        }
    }

    /**
     * Kicks a player from the party (lider only).
     */
    public void kickPlayer(Player leader, Player target) {
        Party party = getParty(leader.getUniqueId());
        if (party == null) {
            leader.sendMessage(ChatColor.RED + "Nie jesteś w żadnej drużynie!");
            return;
        }

        if (!party.getLeaderUUID().equals(leader.getUniqueId())) {
            leader.sendMessage(ChatColor.RED + "Tylko lider drużyny może wyrzucać graczy!");
            return;
        }

        UUID targetUUID = target.getUniqueId();
        if (!party.getMembers().contains(targetUUID)) {
            leader.sendMessage(ChatColor.RED + "Ten gracz nie jest w Twojej drużynie!");
            return;
        }

        if (leader.getUniqueId().equals(targetUUID)) {
            leader.sendMessage(ChatColor.RED + "Nie możesz wyrzucić samego siebie! Użyj /party leave.");
            return;
        }

        party.removeMember(targetUUID);
        playerPartyMap.remove(targetUUID);
        partyChatToggled.remove(targetUUID);
        saveParty(party);

        target.sendMessage(ChatColor.RED + "Zostałeś wyrzucony z drużyny przez lidera.");
        party.getMembers().forEach(uuid -> {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                p.sendMessage(ChatColor.RED + target.getName() + " został wyrzucony z drużyny.");
            }
        });
    }

    /**
     * Toggles friendly fire status (lider only).
     */
    public void toggleFriendlyFire(Player leader) {
        Party party = getParty(leader.getUniqueId());
        if (party == null) {
            leader.sendMessage(ChatColor.RED + "Nie jesteś w żadnej drużynie!");
            return;
        }

        if (!party.getLeaderUUID().equals(leader.getUniqueId())) {
            leader.sendMessage(ChatColor.RED + "Tylko lider drużyny może zmieniać status PvP!");
            return;
        }

        boolean newVal = !party.isFriendlyFireEnabled();
        party.setFriendlyFire(newVal);
        saveParty(party);

        String msg = newVal ? ChatColor.RED + "Walka drużynowa (Friendly Fire) została WŁĄCZONA!" 
                            : ChatColor.GREEN + "Walka drużynowa (Friendly Fire) została WYŁĄCZONA!";

        party.getMembers().forEach(uuid -> {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                p.sendMessage(msg);
            }
        });
    }

    /**
     * Checks if damage between players should be cancelled.
     */
    public boolean shouldBlockDamage(Player damager, Player victim) {
        Party party = getParty(damager.getUniqueId());
        if (party == null) return false;

        // If they are in the same party and friendly fire is disabled
        if (party.getMembers().contains(victim.getUniqueId())) {
            return !party.isFriendlyFireEnabled();
        }
        return false;
    }

    /**
     * Sends a chat message to all members.
     */
    public void sendPartyChat(Player sender, String message) {
        Party party = getParty(sender.getUniqueId());
        if (party == null) {
            sender.sendMessage(ChatColor.RED + "Nie jesteś w żadnej drużynie!");
            return;
        }

        String formatted = ChatColor.DARK_AQUA + "[" + ChatColor.AQUA + "Party Chat" + ChatColor.DARK_AQUA + "] " 
                + ChatColor.GRAY + sender.getName() + ": " + ChatColor.WHITE + message;

        party.getMembers().forEach(uuid -> {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                p.sendMessage(formatted);
            }
        });
    }

    /**
     * Toggles default chat routing to party chat.
     */
    public void togglePartyChatRouting(Player player) {
        UUID uuid = player.getUniqueId();
        if (getParty(uuid) == null) {
            player.sendMessage(ChatColor.RED + "Musisz być w drużynie, aby przełączyć czat!");
            return;
        }

        if (partyChatToggled.contains(uuid)) {
            partyChatToggled.remove(uuid);
            player.sendMessage(ChatColor.YELLOW + "Pętla czatu przełączona na: GLOBALNY.");
        } else {
            partyChatToggled.add(uuid);
            player.sendMessage(ChatColor.GREEN + "Pętla czatu przełączona na: DRUŻYNOWY.");
        }
    }

    public boolean isPartyChatToggled(UUID uuid) {
        return partyChatToggled.contains(uuid);
    }

    public void removePlayerFromMemory(UUID uuid) {
        playerPartyMap.remove(uuid);
        partyChatToggled.remove(uuid);
        // Note: We keep the party alive in activeParties map in case other players are still inside
    }
}

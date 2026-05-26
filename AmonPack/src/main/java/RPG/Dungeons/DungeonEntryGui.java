package RPG.Dungeons;

import RPG.Matchmaking.MatchmakingLobby;
import RPG.Matchmaking.MatchmakingManager;
import RPG.Party.Party;
import RPG.Party.PartyManager;
import Plugin.AmonPackPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

public class DungeonEntryGui implements InventoryHolder {

    public enum GuiType { MAIN, SEARCH_LOBBIES, MANAGE_LOBBY }

    private final Inventory inventory;
    private final GuiType type;
    private final String dungeonId;
    private final Map<Integer, Object> slotDataMap = new HashMap<>(); // Maps slot number to target object

    // Phase track for Party Start (Phase 1 = scan/display, Phase 2 = enter)
    private boolean partyScanConfirmed = false;
    private final List<UUID> nearbyPartyMembers = new ArrayList<>();

    public DungeonEntryGui(Player player, String dungeonId, GuiType type) {
        this.dungeonId = dungeonId;
        this.type = type;

        String title = switch (type) {
            case MAIN -> ChatColor.DARK_PURPLE + "۞ " + ChatColor.LIGHT_PURPLE + "Dungeon: " + dungeonId;
            case SEARCH_LOBBIES -> ChatColor.YELLOW + "🧭 Lobbies: " + dungeonId;
            case MANAGE_LOBBY -> ChatColor.GOLD + "🔥 Zarządzaj Matchmakingiem";
        };

        this.inventory = Bukkit.createInventory(this, 27, title);
        fillBlankSpace();
        
        switch (type) {
            case MAIN -> setupMainGui(player);
            case SEARCH_LOBBIES -> setupSearchLobbiesGui();
            case MANAGE_LOBBY -> setupManageLobbyGui(player);
        }
    }

    public static void open(Player player, String dungeonId) {
        // If they already have an active matchmaking lobby, open manage view, otherwise main
        MatchmakingLobby lobby = MatchmakingManager.getInstance().getLobbyByOwner(player.getUniqueId());
        GuiType type = (lobby != null && lobby.getDungeonId().equalsIgnoreCase(dungeonId)) 
                ? GuiType.MANAGE_LOBBY 
                : GuiType.MAIN;

        DungeonEntryGui gui = new DungeonEntryGui(player, dungeonId, type);
        player.openInventory(gui.getInventory());
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    private void fillBlankSpace() {
        ItemStack pane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            pane.setItemMeta(meta);
        }
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, pane);
        }
    }

    private void setupMainGui(Player player) {
        // Slot 11: Solo Game
        ItemStack solo = new ItemStack(Material.RED_DYE);
        ItemMeta soloMeta = solo.getItemMeta();
        if (soloMeta != null) {
            soloMeta.setDisplayName(ChatColor.RED + "⚔ " + ChatColor.BOLD + "GRA SOLO");
            soloMeta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Ruszaj na przygodę samotnie.",
                    ChatColor.GRAY + "Tylko Ty, Twoje umiejętności i loch."
            ));
            solo.setItemMeta(soloMeta);
        }
        inventory.setItem(11, solo);

        // Slot 12: Party Game
        ItemStack partyItem = new ItemStack(Material.BLUE_DYE);
        ItemMeta partyMeta = partyItem.getItemMeta();
        if (partyMeta != null) {
            partyMeta.setDisplayName(ChatColor.BLUE + "۞ " + ChatColor.BOLD + "GRA Z PARTY");
            partyMeta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Skanuje członków drużyny w zasięgu 100 bloków.",
                    ChatColor.GRAY + "Kliknij, aby wyszukać towarzyszy."
            ));
            partyItem.setItemMeta(partyMeta);
        }
        inventory.setItem(12, partyItem);

        // Slot 13: Search Matchmaking
        ItemStack search = new ItemStack(Material.COMPASS);
        ItemMeta searchMeta = search.getItemMeta();
        if (searchMeta != null) {
            searchMeta.setDisplayName(ChatColor.YELLOW + "🧭 " + ChatColor.BOLD + "SZUKAJ MATCHMAKINGU");
            searchMeta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Przeglądaj aktywne pokoje matchmakingowe",
                    ChatColor.GRAY + "i dołącz do innych graczy szukających ekipy."
            ));
            search.setItemMeta(searchMeta);
        }
        inventory.setItem(13, search);

        // Slot 14: Create Matchmaking
        ItemStack create = new ItemStack(Material.GOLD_INGOT);
        ItemMeta createMeta = create.getItemMeta();
        if (createMeta != null) {
            createMeta.setDisplayName(ChatColor.GOLD + "🔥 " + ChatColor.BOLD + "STWÓRZ MATCHMAKING");
            createMeta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Stwórz własny pokój matchmakingowy",
                    ChatColor.GRAY + "i rekrutuj graczy do wspólnej wyprawy."
            ));
            create.setItemMeta(createMeta);
        }
        inventory.setItem(14, create);

        // Slot 15: Cancel
        ItemStack cancel = new ItemStack(Material.BARRIER);
        ItemMeta cancelMeta = cancel.getItemMeta();
        if (cancelMeta != null) {
            cancelMeta.setDisplayName(ChatColor.DARK_RED + "✖ " + ChatColor.BOLD + "ANULUJ");
            cancelMeta.setLore(Collections.singletonList(ChatColor.GRAY + "Zamknij to menu i anuluj start."));
            cancel.setItemMeta(cancelMeta);
        }
        inventory.setItem(15, cancel);
    }

    private void setupSearchLobbiesGui() {
        List<MatchmakingLobby> activeLobbies = MatchmakingManager.getInstance().getLobbiesForDungeon(dungeonId);
        int[] slots = {10, 11, 12, 13, 14, 15, 16}; // Available slots for lobbies
        
        int idx = 0;
        for (MatchmakingLobby lobby : activeLobbies) {
            if (idx >= slots.length) break;

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.GREEN + "Pokój gracza: " + lobby.getOwnerName());
                OfflinePlayer op = Bukkit.getOfflinePlayer(lobby.getOwnerUUID());
                meta.setOwningPlayer(op);

                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + "Dungeon: " + ChatColor.LIGHT_PURPLE + lobby.getDungeonId());
                lore.add(ChatColor.GRAY + "Członkowie: " + ChatColor.GREEN + lobby.getTemporaryMembers().size() + "/5");
                lore.add("");
                lore.add(ChatColor.YELLOW + "Kliknij LPM, aby wysłać prośbę.");
                meta.setLore(lore);
                head.setItemMeta(meta);
            }

            inventory.setItem(slots[idx], head);
            slotDataMap.put(slots[idx], lobby);
            idx++;
        }

        // Slot 22: Back button
        ItemStack back = new ItemStack(Material.FEATHER);
        ItemMeta backMeta = back.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName(ChatColor.RED + "Powrót");
            back.setItemMeta(backMeta);
        }
        inventory.setItem(22, back);
    }

    private void setupManageLobbyGui(Player player) {
        MatchmakingLobby lobby = MatchmakingManager.getInstance().getLobbyByOwner(player.getUniqueId());
        if (lobby == null) return;

        // Display Temporary Members in top row slots: 10, 11, 12, 13, 14
        int[] memberSlots = {10, 11, 12, 13, 14};
        int memberIdx = 0;
        for (UUID uuid : lobby.getTemporaryMembers()) {
            if (memberIdx >= memberSlots.length) break;

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            if (meta != null) {
                String name = Bukkit.getOfflinePlayer(uuid).getName();
                meta.setDisplayName(ChatColor.GREEN + (uuid.equals(lobby.getOwnerUUID()) ? "Lider: " : "Członek: ") + name);
                meta.setOwningPlayer(Bukkit.getOfflinePlayer(uuid));
                head.setItemMeta(meta);
            }
            inventory.setItem(memberSlots[memberIdx], head);
            memberIdx++;
        }

        // Display Requests in slots: 19, 20, 21, 22, 23 (with Accept/Reject)
        int[] reqSlots = {19, 20, 21, 22, 23};
        int reqIdx = 0;
        for (UUID uuid : lobby.getPendingRequests()) {
            if (reqIdx >= reqSlots.length) break;

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            if (meta != null) {
                String name = Bukkit.getOfflinePlayer(uuid).getName();
                meta.setDisplayName(ChatColor.GOLD + "Prośba o dołączenie: " + name);
                meta.setOwningPlayer(Bukkit.getOfflinePlayer(uuid));
                meta.setLore(Arrays.asList(
                        ChatColor.GREEN + "LPM: Zaakceptuj",
                        ChatColor.RED + "PPM: Odrzuć"
                ));
                head.setItemMeta(meta);
            }
            inventory.setItem(reqSlots[reqIdx], head);
            slotDataMap.put(reqSlots[reqIdx], uuid);
            reqIdx++;
        }

        // Slot 16: Enter Dungeon
        ItemStack start = new ItemStack(Material.LIME_DYE);
        ItemMeta startMeta = start.getItemMeta();
        if (startMeta != null) {
            startMeta.setDisplayName(ChatColor.GREEN + "✔ " + ChatColor.BOLD + "WEJDŹ Z MATCHMAKINGIEM");
            startMeta.setLore(Collections.singletonList(ChatColor.GRAY + "Zatwierdź grupę i rozpocznij dungeon!"));
            start.setItemMeta(startMeta);
        }
        inventory.setItem(16, start);

        // Slot 8: Disband Matchmaking
        ItemStack disband = new ItemStack(Material.BARRIER);
        ItemMeta disbandMeta = disband.getItemMeta();
        if (disbandMeta != null) {
            disbandMeta.setDisplayName(ChatColor.RED + "✖ " + ChatColor.BOLD + "ROZWIĄŻ MATCHMAKING");
            disbandMeta.setLore(Collections.singletonList(ChatColor.GRAY + "Rozwiąż tymczasową grupę i zamknij pokój."));
            disband.setItemMeta(disbandMeta);
        }
        inventory.setItem(8, disband);
    }

    public void handleInventoryClick(Player player, int slot) {
        if (type == GuiType.MAIN) {
            handleMainClick(player, slot);
        } else if (type == GuiType.SEARCH_LOBBIES) {
            handleSearchLobbiesClick(player, slot);
        } else if (type == GuiType.MANAGE_LOBBY) {
            handleManageLobbyClick(player, slot);
        }
    }

    private void handleMainClick(Player player, int slot) {
        if (slot == 11) { // Solo Game
            player.closeInventory();
            List<Player> partyList = Collections.singletonList(player);
            DungeonManager.getInstance().startDungeon(dungeonId, partyList);
        } else if (slot == 12) { // Party Game (2 Phases)
            Party party = PartyManager.getInstance().getParty(player.getUniqueId());
            if (party == null) {
                player.sendMessage(ChatColor.RED + "Nie jesteś w żadnej drużynie party! Stwórz ją za pomocą /party invite.");
                return;
            }

            if (!partyScanConfirmed) {
                // Phase 1: Scan members within 100 blocks
                nearbyPartyMembers.clear();
                nearbyPartyMembers.add(player.getUniqueId()); // Leader is always included

                for (UUID uuid : party.getMembers()) {
                    if (uuid.equals(player.getUniqueId())) continue;
                    Player member = Bukkit.getPlayer(uuid);
                    if (member != null && member.isOnline()) {
                        if (member.getWorld().equals(player.getWorld()) && member.getLocation().distance(player.getLocation()) <= 100) {
                            nearbyPartyMembers.add(uuid);
                        }
                    }
                }

                // Update Party Button item
                ItemStack partyItem = new ItemStack(Material.LIME_DYE);
                ItemMeta partyMeta = partyItem.getItemMeta();
                if (partyMeta != null) {
                    partyMeta.setDisplayName(ChatColor.GREEN + "✔ " + ChatColor.BOLD + "GRA Z PARTY (POTWIERDŹ)");
                    List<String> lore = new ArrayList<>();
                    lore.add(ChatColor.GRAY + "Znalezieni członkowie w pobliżu:");
                    for (UUID uuid : party.getMembers()) {
                        String name = Bukkit.getOfflinePlayer(uuid).getName();
                        if (nearbyPartyMembers.contains(uuid)) {
                            lore.add(ChatColor.GREEN + " - " + name + " (W pobliżu)");
                        } else {
                            lore.add(ChatColor.RED + " - " + name + " (Za daleko / Offline)");
                        }
                    }
                    lore.add("");
                    lore.add(ChatColor.YELLOW + "Kliknij ponownie, aby rozpocząć dungeon!");
                    partyMeta.setLore(lore);
                    partyItem.setItemMeta(partyMeta);
                }
                inventory.setItem(12, partyItem);
                partyScanConfirmed = true;
                player.updateInventory();
            } else {
                // Phase 2: Enter Dungeon
                player.closeInventory();
                List<Player> partyPlayers = new ArrayList<>();
                for (UUID uuid : nearbyPartyMembers) {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null && p.isOnline()) {
                        partyPlayers.add(p);
                    }
                }
                if (partyPlayers.isEmpty()) {
                    player.sendMessage(ChatColor.RED + "Wszyscy członkowie drużyny się oddalili.");
                    return;
                }
                DungeonManager.getInstance().startDungeon(dungeonId, partyPlayers);
            }
        } else if (slot == 13) { // Search Matchmaking
            DungeonEntryGui gui = new DungeonEntryGui(player, dungeonId, GuiType.SEARCH_LOBBIES);
            player.openInventory(gui.getInventory());
        } else if (slot == 14) { // Create Matchmaking
            MatchmakingManager.getInstance().createLobby(player, dungeonId);
            DungeonEntryGui gui = new DungeonEntryGui(player, dungeonId, GuiType.MANAGE_LOBBY);
            player.openInventory(gui.getInventory());
        } else if (slot == 15) { // Cancel
            player.closeInventory();
        }
    }

    private void handleSearchLobbiesClick(Player player, int slot) {
        if (slot == 22) { // Back to MAIN
            DungeonEntryGui gui = new DungeonEntryGui(player, dungeonId, GuiType.MAIN);
            player.openInventory(gui.getInventory());
            return;
        }

        Object data = slotDataMap.get(slot);
        if (data instanceof MatchmakingLobby lobby) {
            player.closeInventory();
            MatchmakingManager.getInstance().requestJoin(player, lobby.getOwnerUUID());
        }
    }

    private void handleManageLobbyClick(Player player, int slot) {
        if (slot == 8) { // Disband Matchmaking
            player.closeInventory();
            MatchmakingManager.getInstance().removeLobby(player.getUniqueId());
            return;
        }

        if (slot == 16) { // Enter Dungeon
            MatchmakingLobby lobby = MatchmakingManager.getInstance().getLobbyByOwner(player.getUniqueId());
            if (lobby == null) return;

            player.closeInventory();
            List<Player> party = new ArrayList<>();
            for (UUID uuid : lobby.getTemporaryMembers()) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null && p.isOnline()) {
                    party.add(p);
                }
            }

            if (party.isEmpty()) {
                player.sendMessage(ChatColor.RED + "Brak graczy w Twoim matchmakingu do startu!");
                return;
            }

            // Close lobby before starting
            MatchmakingManager.getInstance().getLobbies().remove(player.getUniqueId());
            
            // Start
            DungeonManager.getInstance().startDungeon(dungeonId, party);
            return;
        }

        // Check if leader clicked request head (Accept with left click, Reject with right click)
        Object data = slotDataMap.get(slot);
        if (data instanceof UUID targetUUID) {
            MatchmakingLobby lobby = MatchmakingManager.getInstance().getLobbyByOwner(player.getUniqueId());
            if (lobby == null) return;

            // Wait, we need to distinguish Left click and Right click!
            // But since this method gets rawSlot, we should pass the ClickType from inventory click event!
            // Let's modify handleInventoryClick to accept ClickType or boolean for left click!
            // For safety, we can accept a boolean 'isLeftClick'.
        }
    }

    // Overloaded to support Left/Right clicks for accept/reject requests
    public void handleInventoryClick(Player player, int slot, boolean isLeftClick) {
        if (type == GuiType.MANAGE_LOBBY) {
            if (slot == 8) {
                player.closeInventory();
                MatchmakingManager.getInstance().removeLobby(player.getUniqueId());
                return;
            }

            if (slot == 16) {
                MatchmakingLobby lobby = MatchmakingManager.getInstance().getLobbyByOwner(player.getUniqueId());
                if (lobby == null) return;

                player.closeInventory();
                List<Player> party = new ArrayList<>();
                for (UUID uuid : lobby.getTemporaryMembers()) {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null && p.isOnline()) {
                        party.add(p);
                    }
                }

                if (party.isEmpty()) {
                    player.sendMessage(ChatColor.RED + "Brak graczy w matchmakingu.");
                    return;
                }

                MatchmakingManager.getInstance().getLobbies().remove(player.getUniqueId());
                DungeonManager.getInstance().startDungeon(dungeonId, party);
                return;
            }

            Object data = slotDataMap.get(slot);
            if (data instanceof UUID targetUUID) {
                if (isLeftClick) {
                    MatchmakingManager.getInstance().acceptRequest(player.getUniqueId(), targetUUID);
                } else {
                    MatchmakingManager.getInstance().rejectRequest(player.getUniqueId(), targetUUID);
                }
                // Refresh inventory!
                DungeonEntryGui gui = new DungeonEntryGui(player, dungeonId, GuiType.MANAGE_LOBBY);
                player.openInventory(gui.getInventory());
            }
        } else {
            handleInventoryClick(player, slot);
        }
    }
}

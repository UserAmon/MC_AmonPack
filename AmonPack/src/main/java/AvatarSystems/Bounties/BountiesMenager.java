package AvatarSystems.Bounties;

import AvatarSystems.Bounties.Objects.Bounty;
import AvatarSystems.Bounties.Objects.PlayerBountyData;
import AvatarSystems.Util_Objects.InventoryXHolder;
import AvatarSystems.Util_Objects.LevelSkill;
import AvatarSystems.Levels.PlayerLevelMenager;
import methods_plugins.AmonPackPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.block.data.Ageable;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static methods_plugins.AmonPackPlugin.ExecuteQuery;
import static methods_plugins.AmonPackPlugin.FastEasyStackWithLoreModelData;

public class BountiesMenager implements Listener {

    public static List<Bounty> AllBounties = new ArrayList<>();
    public static List<PlayerBountyData> PlayersData = new ArrayList<>();
    public static InventoryXHolder BountiesGui;

    public BountiesMenager() {
        ReloadConfig();
        CreateInventories();
        try {
            LoadPlayersFromDatabase();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void ReloadConfig() {
        AllBounties.clear();
        FileConfiguration config = AmonPackPlugin.getConfigs_menager().getBounties_Config();
        if (config.getConfigurationSection("Bounties") == null)
            return;

        for (String key : config.getConfigurationSection("Bounties").getKeys(false)) {
            String path = "Bounties." + key + ".";
            String name = ChatColor.translateAlternateColorCodes('&', config.getString(path + "Name"));
            List<String> lore = new ArrayList<>();
            for (String l : config.getStringList(path + "Lore")) {
                lore.add(ChatColor.translateAlternateColorCodes('&', l));
            }
            Bounty.BountyType type = Bounty.BountyType.valueOf(config.getString(path + "Type"));
            String target = config.getString(path + "Target");
            int amount = config.getInt(path + "Amount");
            List<String> rewards = config.getStringList(path + "Rewards");

            AllBounties.add(new Bounty(key, name, lore, type, target, amount, rewards));
        }
    }

    public void CreateInventories() {
        BountiesGui = new InventoryXHolder(27, ChatColor.DARK_GREEN + "Dzienne Zlecenia");
    }

    public static void OpenBountiesGui(Player player) {
        Inventory inv = Bukkit.createInventory(BountiesGui, BountiesGui.getSize(), BountiesGui.getTitle());
        PlayerBountyData data = GetPlayerData(player.getName());

        CheckAndResetDaily(data);

        int[] slots = { 11, 13, 15 };
        int i = 0;
        for (Map.Entry<String, Integer> entry : data.getActiveBounties().entrySet()) {
            if (i >= slots.length)
                break;
            String bountyId = entry.getKey();
            int progress = entry.getValue();
            Bounty bounty = GetBountyById(bountyId);

            if (bounty != null) {
                Material mat = Material.PAPER;
                String status = "&eW trakcie";
                if (data.isCompleted(bountyId)) {
                    mat = Material.FILLED_MAP;
                    status = "&aUkończone";
                } else if (progress >= bounty.getAmount()) {
                    mat = Material.WRITTEN_BOOK;
                    status = "&aGotowe do odebrania! &e(Kliknij)";
                }

                List<String> lore = new ArrayList<>(bounty.getLore());
                lore.add("");
                lore.add(ChatColor.translateAlternateColorCodes('&',
                        "&7Postęp: &f" + progress + "/" + bounty.getAmount()));
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7Status: " + status));
                lore.add("");
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7Nagrody:"));
                for (String reward : bounty.getRewards()) {
                    if (reward.startsWith("xp:")) {
                        lore.add(ChatColor.translateAlternateColorCodes('&',
                                "&8- &b" + reward.replace("xp:", "") + " XP Zleceń"));
                    } else if (reward.startsWith("command:money")) {
                        lore.add(ChatColor.translateAlternateColorCodes('&',
                                "&8- &6" + reward.replaceAll(".*\\s", "") + " Monet"));
                    }
                }

                ItemStack item = FastEasyStackWithLoreModelData(mat, bounty.getDisplayName(), lore, 0);
                inv.setItem(slots[i], item);
            }
            i++;
        }

        player.openInventory(inv);
    }

    public static void ClaimBounty(Player player, String bountyId) {
        PlayerBountyData data = GetPlayerData(player.getName());
        Bounty bounty = GetBountyById(bountyId);
        if (data == null || bounty == null)
            return;

        if (data.isCompleted(bountyId)) {
            player.sendMessage(ChatColor.RED + "Już odebrałeś nagrodę za to zlecenie!");
            return;
        }

        if (data.getProgress(bountyId) >= bounty.getAmount()) {
            data.completeBounty(bountyId);
            for (String reward : bounty.getRewards()) {
                if (reward.startsWith("command:")) {
                    String cmd = reward.replace("command:", "").replace("%player%", player.getName());
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                } else if (reward.startsWith("xp:")) {
                    int xp = Integer.parseInt(reward.replace("xp:", ""));
                    try {
                        // Add to BOUNTY skill
                        AmonPackPlugin.getPlayerMenager().AddPoints(LevelSkill.SkillType.BOUNTY, player, xp);

                        // Add to specific skill based on bounty type
                        LevelSkill.SkillType type = null;
                        switch (bounty.getType()) {
                            case KILL_MOB:
                                type = LevelSkill.SkillType.COMBAT;
                                break;
                            case MINING:
                                type = LevelSkill.SkillType.MINING;
                                break;
                            case LUMBERING:
                                type = LevelSkill.SkillType.LUMBERING;
                                break;
                            case FARMING:
                                type = LevelSkill.SkillType.FARMING;
                                break;
                        }
                        if (type != null) {
                            AmonPackPlugin.getPlayerMenager().AddPoints(type, player, xp);
                        }
                    } catch (Exception e) {
                        player.sendMessage(ChatColor.RED + "Błąd przy dodawaniu XP: " + e.getMessage());
                    }
                }
            }
            player.sendMessage(ChatColor.GREEN + "Odebrano nagrodę za zlecenie: " + bounty.getDisplayName());
            OpenBountiesGui(player); // Refresh GUI
            SavePlayerToDatabase(data);
        } else {
            player.sendMessage(ChatColor.RED + "Nie ukończyłeś jeszcze tego zlecenia!");
        }
    }

    public static void CheckAndResetDaily(PlayerBountyData data) {
        LocalDate lastReset = LocalDate.ofEpochDay(data.getLastResetTime() / (24 * 60 * 60 * 1000));
        LocalDate today = LocalDate.now();

        if (data.getActiveBounties().isEmpty() || lastReset.isBefore(today)) {
            data.getActiveBounties().clear();
            data.getCompletedBounties().clear();

            List<Bounty> available = new ArrayList<>(AllBounties);
            Collections.shuffle(available);

            for (int i = 0; i < 3 && i < available.size(); i++) {
                data.getActiveBounties().put(available.get(i).getId(), 0);
            }

            data.setLastResetTime(System.currentTimeMillis());
            SavePlayerToDatabase(data);
        }
    }

    public static void ForceReset(Player player) {
        PlayerBountyData data = GetPlayerData(player.getName());
        data.getActiveBounties().clear();
        data.getCompletedBounties().clear();
        data.setLastResetTime(0); // Force reset time to 0
        CheckAndResetDaily(data); // This will trigger the reset logic
        player.sendMessage(ChatColor.GREEN + "Twoje zlecenia zostały zresetowane!");
        OpenBountiesGui(player);
    }

    public static PlayerBountyData GetPlayerData(String name) {
        return PlayersData.stream().filter(d -> d.getPlayerName().equalsIgnoreCase(name)).findFirst().orElseGet(() -> {
            PlayerBountyData newData = new PlayerBountyData(name);
            PlayersData.add(newData);
            CheckAndResetDaily(newData); // Assign initial bounties
            return newData;
        });
    }

    public static Bounty GetBountyById(String id) {
        return AllBounties.stream().filter(b -> b.getId().equals(id)).findFirst().orElse(null);
    }

    @EventHandler
    public void onMobKill(EntityDeathEvent event) {
        if (event.getEntity().getKiller() != null) {
            Player player = event.getEntity().getKiller();
            PlayerBountyData data = GetPlayerData(player.getName());

            boolean updated = false;
            for (Map.Entry<String, Integer> entry : data.getActiveBounties().entrySet()) {
                String bountyId = entry.getKey();
                if (data.isCompleted(bountyId))
                    continue;

                Bounty bounty = GetBountyById(bountyId);
                if (bounty != null && bounty.getType() == Bounty.BountyType.KILL_MOB
                        && bounty.getTarget().equalsIgnoreCase(event.getEntityType().toString())) {
                    if (entry.getValue() < bounty.getAmount()) {
                        data.addProgress(bountyId, 1);
                        updated = true;

                        int current = entry.getValue();
                        if (current >= bounty.getAmount()) {
                            sendCompletionNotification(player, bounty.getDisplayName());
                        } else {
                            sendProgressNotification(player, bounty.getDisplayName(), current, bounty.getAmount());
                        }
                    }
                }
            }
            if (updated) {
                SavePlayerToDatabase(data);
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        PlayerBountyData data = GetPlayerData(player.getName());

        boolean updated = false;
        for (Map.Entry<String, Integer> entry : data.getActiveBounties().entrySet()) {
            String bountyId = entry.getKey();
            if (data.isCompleted(bountyId))
                continue;

            Bounty bounty = GetBountyById(bountyId);
            if (bounty != null) {
                boolean match = false;
                if (bounty.getType() == Bounty.BountyType.MINING || bounty.getType() == Bounty.BountyType.LUMBERING) {
                    if (isMatchingBlock(event.getBlock().getType(), bounty.getTarget())) {
                        match = true;
                    }
                } else if (bounty.getType() == Bounty.BountyType.FARMING) {
                    if (bounty.getTarget().equalsIgnoreCase(event.getBlock().getType().toString())) {
                        if (event.getBlock().getBlockData() instanceof Ageable) {
                            Ageable ageable = (Ageable) event.getBlock().getBlockData();
                            if (ageable.getAge() == ageable.getMaximumAge()) {
                                match = true;
                            }
                        } else {
                            match = true; // Not ageable (e.g. Melon), just break
                        }
                    }
                }

                if (match) {
                    if (entry.getValue() < bounty.getAmount()) {
                        data.addProgress(bountyId, 1);
                        updated = true;

                        int current = entry.getValue() + 1;
                        if (current >= bounty.getAmount()) {
                            sendCompletionNotification(player, bounty.getDisplayName());
                        } else {
                            sendProgressNotification(player, bounty.getDisplayName(), current, bounty.getAmount());
                        }
                    }
                }
            }
        }
        if (updated) {
            SavePlayerToDatabase(data);
        }
    }

    private boolean isMatchingBlock(Material block, String target) {
        String blockName = block.toString();
        if (blockName.equalsIgnoreCase(target))
            return true;

        // Ore variants (e.g. IRON_ORE -> DEEPSLATE_IRON_ORE)
        if (target.endsWith("_ORE")) {
            if (blockName.equals("DEEPSLATE_" + target))
                return true;
        }

        // Log variants (e.g. OAK_LOG -> OAK_WOOD, STRIPPED_OAK_LOG, STRIPPED_OAK_WOOD)
        if (target.endsWith("_LOG")) {
            String base = target.replace("_LOG", "");
            if (blockName.equals(base + "_WOOD"))
                return true;
            if (blockName.equals("STRIPPED_" + target))
                return true;
            if (blockName.equals("STRIPPED_" + base + "_WOOD"))
                return true;
        }

        return false;
    }

    private void sendProgressNotification(Player player, String bountyName, int current, int max) {
        String message = ChatColor.translateAlternateColorCodes('&', "&a" + bountyName + ": &f" + current + "/" + max);
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
    }

    private void sendCompletionNotification(Player player, String bountyName) {
        player.sendTitle(ChatColor.GOLD + "Zlecenie Ukończone!", ChatColor.YELLOW + bountyName, 10, 70, 20);
        player.playSound(player.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
    }

    @EventHandler
    public void onInventoryClick(org.bukkit.event.inventory.InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof InventoryXHolder) {
            InventoryXHolder holder = (InventoryXHolder) event.getInventory().getHolder();
            if (holder.getTitle().equals(BountiesGui.getTitle())) {
                event.setCancelled(true);
                if (event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR) {
                    Player player = (Player) event.getWhoClicked();
                    PlayerBountyData data = GetPlayerData(player.getName());

                    int slot = event.getRawSlot();
                    int index = -1;
                    if (slot == 11)
                        index = 0;
                    else if (slot == 13)
                        index = 1;
                    else if (slot == 15)
                        index = 2;

                    if (index != -1) {
                        int i = 0;
                        for (Map.Entry<String, Integer> entry : data.getActiveBounties().entrySet()) {
                            if (i == index) {
                                String bountyId = entry.getKey();
                                if (data.isCompleted(bountyId)) {
                                    // Reroll logic
                                    if (player.getInventory().containsAtLeast(new ItemStack(Material.DIAMOND), 5)) {
                                        player.getInventory().removeItem(new ItemStack(Material.DIAMOND, 5));

                                        data.getActiveBounties().remove(bountyId);
                                        data.getCompletedBounties().remove(bountyId);

                                        List<Bounty> available = new ArrayList<>(AllBounties);
                                        available.removeIf(b -> data.getActiveBounties().containsKey(b.getId()));
                                        Collections.shuffle(available);

                                        if (!available.isEmpty()) {
                                            data.getActiveBounties().put(available.get(0).getId(), 0);
                                            player.sendMessage(ChatColor.GREEN + "Wylosowano nowe zlecenie!");
                                        } else {
                                            player.sendMessage(ChatColor.RED + "Brak dostępnych nowych zleceń!");
                                        }
                                        SavePlayerToDatabase(data);
                                        OpenBountiesGui(player);
                                    } else {
                                        player.sendMessage(ChatColor.RED
                                                + "Potrzebujesz 5 diamentów, aby wylosować nowe zlecenie!");
                                    }
                                } else {
                                    ClaimBounty(player, bountyId);
                                }
                                break;
                            }
                            i++;
                        }
                    }
                }
            }
        }
    }

    // Database Methods

    private void LoadPlayersFromDatabase() throws SQLException {
        Statement stmt = AmonPackPlugin.mysqllite().getConnection().createStatement();
        // Ensure table exists
        ExecuteQuery(
                "CREATE TABLE IF NOT EXISTS PlayerBounties (Player VARCHAR(50) PRIMARY KEY, ActiveBounties TEXT, CompletedBounties TEXT, LastReset LONG)");

        ResultSet rs = stmt.executeQuery("select * from PlayerBounties");
        while (rs.next()) {
            String name = rs.getString("Player");
            PlayerBountyData data = new PlayerBountyData(name);

            String activeStr = rs.getString("ActiveBounties");
            if (activeStr != null && !activeStr.isEmpty()) {
                for (String s : activeStr.split(",")) {
                    String[] parts = s.split(":");
                    if (parts.length == 2) {
                        data.getActiveBounties().put(parts[0], Integer.parseInt(parts[1]));
                    }
                }
            }

            String completedStr = rs.getString("CompletedBounties");
            if (completedStr != null && !completedStr.isEmpty()) {
                for (String s : completedStr.split(",")) {
                    data.getCompletedBounties().put(s, true);
                }
            }

            data.setLastResetTime(rs.getLong("LastReset"));
            PlayersData.add(data);
        }
        stmt.close();
    }

    public static void SavePlayerToDatabase(PlayerBountyData data) {
        try {
            Statement stmt = AmonPackPlugin.mysqllite().getConnection().createStatement();
            ResultSet rs = stmt
                    .executeQuery("select * from PlayerBounties where Player='" + data.getPlayerName() + "'");

            String activeStr = data.getActiveBounties().entrySet().stream()
                    .map(e -> e.getKey() + ":" + e.getValue())
                    .collect(Collectors.joining(","));

            String completedStr = String.join(",", data.getCompletedBounties().keySet());

            if (!rs.next()) {
                ExecuteQuery(
                        "INSERT INTO PlayerBounties (Player, ActiveBounties, CompletedBounties, LastReset) VALUES ('" +
                                data.getPlayerName() + "', '" + activeStr + "', '" + completedStr + "', "
                                + data.getLastResetTime() + ")");
            } else {
                ExecuteQuery("UPDATE PlayerBounties SET ActiveBounties = '" + activeStr +
                        "', CompletedBounties = '" + completedStr +
                        "', LastReset = " + data.getLastResetTime() +
                        " WHERE Player = '" + data.getPlayerName() + "'");
            }
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void SaveAll() {
        for (PlayerBountyData data : PlayersData) {
            SavePlayerToDatabase(data);
        }
    }
}

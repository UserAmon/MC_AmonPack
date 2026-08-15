package RPG.Levels;

import RPG.Crafting.Objects.MagicEffects;
import RPG.Crafting.Objects.MagicEffectsConditions;
import RPG.Util.InventoryXHolder;
import RPG.Levels.Objects.LevelSkill;
import RPG.Levels.Objects.PlayerLevel;
import com.projectkorra.projectkorra.Element;
import Plugin.Commands;
import Plugin.AmonPackPlugin;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import dev.lone.itemsadder.api.FontImages.FontImageWrapper;
import dev.lone.itemsadder.api.FontImages.TexturedInventoryWrapper;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.stream.Collectors;

import static Plugin.AmonPackPlugin.ExecuteQuery;

public class PlayerLevelMenager {
    public static List<PlayerLevel> AllPlayerLevels;
    public static InventoryXHolder Holder1;
    public static InventoryXHolder SkillDetails;
    public static InventoryXHolder BendingSkillMenu;
    public static InventoryXHolder BendingSkillTree;
    public static InventoryXHolder BindingAbilitiesMenu;
    public static InventoryXHolder SelectElementMenu;
    public static List<LevelSkill.SkillType> EnabledSkillTypes;

    public PlayerLevelMenager() {
        AllPlayerLevels = new ArrayList<>();
        EnabledSkillTypes = new ArrayList<>();
        CreateInventories();

        FileConfiguration config = AmonPackPlugin.getLevelConfig();
        if (config != null) {
            try {
                for (String key : config.getStringList("AmonPack.Levels.Enabled")) {
                    EnabledSkillTypes.add(LevelSkill.SkillType.valueOf(key));
                }
            } catch (Exception e) {
                System.out.println("Error loading enabled skill types: " + e.getMessage());
            }
        }
        try {
            LoadPlayersFromDatabase();
        } catch (Exception e) {
            System.out.println("[AmonPack] Ostrzeżenie przy wczytywaniu graczy z bazy danych: " + e.getMessage());
        }
    }

    public static void TryOpenPlayerLevel(Player player) {
        try {
            PlayerLevel Level;
            Optional<PlayerLevel> Exist = AllPlayerLevels.stream()
                    .filter(lvl -> lvl.getPlayerName().equalsIgnoreCase(player.getName()))
                    .findFirst();
            if (Exist.isPresent()) {
                Level = Exist.get();
                List<LevelSkill> currentSkills = Level.getPlayerSkills();
                for (LevelSkill.SkillType enabledType : EnabledSkillTypes) {
                    boolean hasSkill = currentSkills.stream()
                            .anyMatch(skill -> skill.getType() == enabledType);
                    if (!hasSkill) {
                        currentSkills.add(new LevelSkill(0, enabledType, new ArrayList<>(), 0));
                    }
                }
            } else {
                List<LevelSkill> Skills = new ArrayList<>();
                for (LevelSkill.SkillType skillType : EnabledSkillTypes) {
                    Skills.add(new LevelSkill(0, skillType, new ArrayList<>(), 0));
                }
                Level = new PlayerLevel(player.getName(), Skills);
                AllPlayerLevels.add(Level);
            }
            OpenPlayerLevelWindow(Level);
        } catch (Exception e) {
            System.out.println("Error In Player Level " + e.getMessage());
        }
    }

    public static void OpenSkillDetails(LevelSkill skill, Player p) {
        if (skill == null || skill.getType() == null || p == null) return;
        Inventory inv = Bukkit.createInventory(SkillDetails, SkillDetails.getSize(), SkillDetails.getTitle());
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, GuiBlank());
        }
        FileConfiguration config = AmonPackPlugin.getLevelConfig();
        if (config == null) return;
        String Path = "AmonPack.Levels." + skill.getType().toString();
        org.bukkit.configuration.ConfigurationSection sec = config.getConfigurationSection(Path);
        if (sec == null) {
            p.openInventory(inv);
            return;
        }
        int ActualLevel = 0;
        int totallvl = (int) skill.getExpPoints();
        for (String key : sec.getKeys(false)) {
            if (key.startsWith("Level")) {
                String newpath = Path + "." + key;
                ItemStack Item;
                ItemMeta LockedItemMeta;
                int MaxLvL = config.getInt(newpath + ".ReqExp");
                int lvl = 0;
                try {
                    lvl = Integer.parseInt(key.replace("Level_", ""));
                } catch (NumberFormatException ignored) {}
                if (MaxLvL > 0 && totallvl >= MaxLvL) {
                    ActualLevel = lvl;
                    totallvl = totallvl - MaxLvL;
                    String unLockedMatStr = config.getString(Path + ".Details.UnLockedItem", "DIRT");
                    Material unLockedMat = Material.getMaterial(unLockedMatStr);
                    if (unLockedMat == null) unLockedMat = Material.DIRT;
                    Item = new ItemStack(unLockedMat);
                    LockedItemMeta = Item.getItemMeta();
                    if (config.getInt(Path + ".Details.UnLockedItemModelData") != 0) {
                        LockedItemMeta.setCustomModelData(config.getInt(Path + ".Details.UnLockedItemModelData"));
                    }
                    LockedItemMeta.setDisplayName(ChatColor.GREEN + "Poziom " + lvl);
                    List<String> Lore = new ArrayList<>();
                    if (skill.getUsedRewards().contains(lvl)) {
                        Lore.add(ChatColor.RED + "Juz odebrano tę nagrodę");
                    } else {
                        Lore.add(ChatColor.GREEN + "Nagroda dostepna");
                    }
                    org.bukkit.configuration.ConfigurationSection rewSec = config.getConfigurationSection(newpath);
                    if (rewSec != null) {
                        for (String Rewards : rewSec.getKeys(false)) {
                            if (Rewards.startsWith("Reward")) {
                                String reward = config.getString(Path + "." + key + "." + Rewards, "");
                                if (Rewards.endsWith("Lore")) {
                                    for (String line : reward.split("%break%")) {
                                        Lore.add(line);
                                    }
                                } else {
                                    if (reward.startsWith("command:")) {
                                        if (reward.contains("money add")) {
                                            reward = reward.replace("command:money add %player%", "");
                                            Lore.add(ChatColor.GOLD + "+" + reward + "¥");
                                        }
                                    }
                                    if (reward.startsWith("skillupgrade:")) {
                                        reward = reward.replace("skillupgrade:", "");
                                        Lore.add(ChatColor.AQUA + "+" + reward + " do poziomu umiejętności dziedziny");
                                    }
                                    if (reward.startsWith("SkillPoints:")) {
                                        reward = reward.replace("SkillPoints:", "");
                                    }
                                }
                            }
                        }
                    }
                    LockedItemMeta.setLore(Lore);
                } else {
                    String lockedMatStr = config.getString(Path + ".Details.LockedItem", "STONE");
                    Material lockedMat = Material.getMaterial(lockedMatStr);
                    if (lockedMat == null) lockedMat = Material.STONE;
                    Item = new ItemStack(lockedMat);
                    LockedItemMeta = Item.getItemMeta();
                    if (config.getInt(Path + ".Details.LockedItemModelData") != 0) {
                        LockedItemMeta.setCustomModelData(config.getInt(Path + ".Details.LockedItemModelData"));
                    }
                    LockedItemMeta.setDisplayName(ChatColor.RED + "Poziom " + lvl);
                    List<String> Lore = new ArrayList<>();
                    if (lvl == ActualLevel + 1) {
                        Lore.add(ChatColor.LIGHT_PURPLE + "Doświadczenie: " + (totallvl + "/" + MaxLvL));
                    }
                    org.bukkit.configuration.ConfigurationSection rewSec = config.getConfigurationSection(newpath);
                    if (rewSec != null) {
                        for (String Rewards : rewSec.getKeys(false)) {
                            if (Rewards.startsWith("Reward")) {
                                String reward = config.getString(Path + "." + key + "." + Rewards, "");
                                if (Rewards.endsWith("Lore")) {
                                    for (String line : reward.split("%break%")) {
                                        Lore.add(line);
                                    }
                                } else {
                                    if (reward.startsWith("command:")) {
                                        if (reward.contains("economy give")) {
                                            reward = reward.replace("command:economy give %player%", "");
                                            Lore.add(ChatColor.GOLD + "+" + reward + "¥");
                                        }
                                    }
                                }
                            }
                        }
                    }
                    LockedItemMeta.setLore(Lore);
                }
                Item.setItemMeta(LockedItemMeta);
                inv.setItem(8 + lvl, Item);
            }
        }
        String npath = Path + ".Gui";
        ItemStack pl = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta plmeta = pl.getItemMeta();
        String skillDisplay = config.getString(npath + ".SkillDisplay", "Poziom");
        plmeta.setDisplayName(skillDisplay + ": " + ActualLevel);
        List<String> Lore = new ArrayList<>();
        Lore.add(ChatColor.LIGHT_PURPLE + "Obecne doświadczenie: " + totallvl);
        String BuffSkillLore = config.getString(npath + ".BuffSkilllore");
        if (BuffSkillLore != null) {
            BuffSkillLore = BuffSkillLore.replace("%chance%", skill.getUpgradePercent() + "%");
            for (String line : BuffSkillLore.split("%break%")) {
                Lore.add(ChatColor.GRAY + line);
            }
        }
        plmeta.setLore(Lore);
        pl.setItemMeta(plmeta);
        inv.setItem(0, pl);
        inv.setItem(35, ReturnItem());
        p.openInventory(inv);
    }

    private static void OpenPlayerLevelWindow(PlayerLevel level) {
        if (level == null) return;
        TexturedInventoryWrapper inventory = new TexturedInventoryWrapper(Holder1,
                Holder1.getSize(), Holder1.getTitle(), new FontImageWrapper("amonpack:first_gui"));
        Inventory inv = inventory.getInternal();

        FileConfiguration config = AmonPackPlugin.getLevelConfig();
        if (config == null || config.getConfigurationSection("AmonPack.Levels") == null) {
            System.out.println("[AmonPack] Blad: Konfiguracja Levels.yml nie zostala zaladowana lub brakuje sekcji AmonPack.Levels!");
            return;
        }
        try {
            org.bukkit.configuration.ConfigurationSection levelsSec = config.getConfigurationSection("AmonPack.Levels");
            if (levelsSec != null) {
                for (String key : levelsSec.getKeys(false)) {
                    if (!key.startsWith("Enabled")) {
                        if (!key.startsWith("Mastery")) {
                            LevelSkill skill = level.getPlayerSkills().stream()
                                    .filter(sk -> sk.getType().toString().equalsIgnoreCase(key)).findFirst().orElse(null);
                            if (skill == null) {
                                try {
                                    LevelSkill.SkillType st = LevelSkill.SkillType.valueOf(key);
                                    skill = new LevelSkill(0, st, new ArrayList<>(), 0);
                                    level.getPlayerSkills().add(skill);
                                } catch (IllegalArgumentException ignored) {
                                    continue;
                                }
                            }
                            String Path = "AmonPack.Levels." + skill.getType().toString();
                            int place = config.getInt(Path + ".Gui.Place");
                            String title = config.getString(Path + ".Gui.Title", key);
                            String itemMatStr = config.getString(Path + ".Gui.Item", "BOOK");
                            Material mat = Material.getMaterial(itemMatStr);
                            if (mat == null) mat = Material.BOOK;
                            ItemStack Item1 = new ItemStack(mat);
                            ItemMeta Item1Meta = Item1.getItemMeta();
                            Item1Meta.setDisplayName(title);
                            Item1.setItemMeta(Item1Meta);
                            inv.setItem(place, Item1);
                        } else {
                            String Path = "AmonPack.Levels." + key;
                            int place = config.getInt(Path + ".Gui.Place");
                            int ModelData = config.getInt(Path + ".Gui.ModelData");
                            String title = config.getString(Path + ".Gui.Title", key);
                            ItemStack Item1 = new ItemStack(Material.PAPER);
                            ItemMeta Item1Meta = Item1.getItemMeta();
                            Item1Meta.setCustomModelData(ModelData);
                            Item1Meta.setDisplayName(title);
                            Item1.setItemMeta(Item1Meta);
                            inv.setItem(place, Item1);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("error open1  " + e.getMessage());
            System.out.println("error open2  " + e.getLocalizedMessage());
            System.out.println("error open3  " + e);
        }
        Player p = Bukkit.getPlayer(level.getPlayerName());
        if (p != null) {
            inventory.showInventory(p);
        }
    }

    public static void ClaimReward(LevelSkill.SkillType Type, Player player, String title) {
        try {
            if (player == null || Type == null) return;
            PlayerLevel Level = AllPlayerLevels.stream()
                    .filter(lvl -> lvl.getPlayerName().equalsIgnoreCase(player.getName())).findFirst().orElse(null);
            if (Level == null) return;
            LevelSkill skill = Level.getPlayerSkills().stream().filter(sk -> sk.getType().equals(Type)).findFirst()
                    .orElse(null);
            if (skill == null) return;
            FileConfiguration config = AmonPackPlugin.getLevelConfig();
            if (config == null) return;
            String Path = "AmonPack.Levels." + skill.getType().toString();
            org.bukkit.configuration.ConfigurationSection sec = config.getConfigurationSection(Path);
            if (sec == null) return;
            for (String key : sec.getKeys(false)) {
                String lvl = key.replace("Level_", "");
                title = title.replaceAll("\\D+", "");
                if (lvl.equalsIgnoreCase(title)) {
                    int lvlNum = 0;
                    try {
                        lvlNum = Integer.parseInt(lvl);
                    } catch (NumberFormatException ignored) {}
                    if (lvlNum <= ReturnUnlocked(skill)) {
                        if (!skill.getUsedRewards().contains(lvlNum)) {
                            org.bukkit.configuration.ConfigurationSection rewSec = config.getConfigurationSection(Path + "." + key);
                            if (rewSec != null) {
                                for (String Rewards : rewSec.getKeys(false)) {
                                    if (Rewards.startsWith("Reward")) {
                                        String reward = config.getString(Path + "." + key + "." + Rewards);
                                        if (reward != null && reward.startsWith("command:")) {
                                            reward = reward.replace("command:", "");
                                            reward = reward.replace("%player%", player.getName());
                                            Commands.ExecuteCommandExample example = new Commands.ExecuteCommandExample();
                                            example.executeCommand(reward);
                                        }
                                        if (reward != null && reward.startsWith("skillupgrade:")) {
                                            reward = reward.replace("skillupgrade:", "");
                                            try {
                                                skill.setUpgradePercent(skill.getUpgradePercent() + Double.parseDouble(reward));
                                            } catch (NumberFormatException ignored) {}
                                        }
                                    }
                                }
                            }
                            List<Integer> usedreward = skill.getUsedRewards();
                            usedreward.add(Integer.valueOf(lvlNum));
                            skill.setUsedRewards(usedreward);
                            OpenSkillDetails(skill, player);
                        }
                    }
                    break;
                }
            }
        } catch (Exception e) {
            System.out.println("Error In Player Adding Level Points " + e.getMessage());
        }
    }

    public static int GetSkillByPlayer(LevelSkill.SkillType type, Player player) {
        if (player == null || type == null) return 0;
        PlayerLevel Level = AllPlayerLevels.stream()
                .filter(lvl -> lvl.getPlayerName().equalsIgnoreCase(player.getName())).findFirst().orElse(null);
        if (Level == null)
            return 0;
        LevelSkill skill = Level.getPlayerSkills().stream().filter(sk -> sk.getType().equals(type)).findFirst()
                .orElse(null);
        if (skill == null)
            return 0;
        return ReturnUnlocked(skill);
    }

    public static int ReturnUnlocked(LevelSkill skill) {
        if (skill == null || skill.getType() == null) return 0;
        FileConfiguration config = AmonPackPlugin.getLevelConfig();
        if (config == null) return 0;
        String Path = "AmonPack.Levels." + skill.getType().toString();
        org.bukkit.configuration.ConfigurationSection sec = config.getConfigurationSection(Path);
        if (sec == null) return 0;
        int totallvl = (int) skill.getExpPoints();
        int lvl = 0;
        for (String key : sec.getKeys(false)) {
            if (key.startsWith("Level")) {
                String newpath = Path + "." + key;
                int MaxLvL = config.getInt(newpath + ".ReqExp");
                if (MaxLvL > 0 && totallvl >= MaxLvL) {
                    totallvl = totallvl - MaxLvL;
                    try {
                        lvl = Integer.parseInt(key.replace("Level_", ""));
                    } catch (NumberFormatException ignored) {}
                } else {
                    break;
                }
            }
        }
        return lvl;
    }

    public void AddPoints(LevelSkill.SkillType Type, Player player, int points) {
        try {
            if (player == null || Type == null) return;
            PlayerLevel Level = AllPlayerLevels.stream()
                    .filter(lvl -> lvl.getPlayerName().equalsIgnoreCase(player.getName()))
                    .findFirst().orElse(null);
            if (Level == null) {
                List<LevelSkill> Skills = new ArrayList<>();
                for (LevelSkill.SkillType skillType : EnabledSkillTypes) {
                    Skills.add(new LevelSkill(0, skillType, new ArrayList<>(), 0));
                }
                Level = new PlayerLevel(player.getName(), Skills);
                AllPlayerLevels.add(Level);
            }
            LevelSkill skill = Level.getPlayerSkills().stream()
                    .filter(sk -> sk.getType().equals(Type))
                    .findFirst().orElse(null);
            if (skill == null) {
                skill = new LevelSkill(0, Type, new ArrayList<>(), 0);
                Level.getPlayerSkills().add(skill);
            }
            FileConfiguration config = AmonPackPlugin.getLevelConfig();
            if (config == null) {
                skill.setExpPoints(skill.getExpPoints() + points);
                return;
            }
            String path = "AmonPack.Levels." + skill.getType().toString();
            org.bukkit.configuration.ConfigurationSection sec = config.getConfigurationSection(path);
            if (sec == null) {
                skill.setExpPoints(skill.getExpPoints() + points);
                return;
            }
            int actualLevel = 0;
            int neededExp = 0;
            int totalExpBefore = (int) skill.getExpPoints();
            int expPool = totalExpBefore;
            for (String key : sec.getKeys(false)) {
                if (!key.startsWith("Level"))
                    continue;
                String levelPath = path + "." + key;
                int reqExp = config.getInt(levelPath + ".ReqExp");
                int level = 0;
                try {
                    level = Integer.parseInt(key.replace("Level_", ""));
                } catch (NumberFormatException ignored) {}
                if (reqExp > 0 && expPool >= reqExp) {
                    actualLevel = level;
                    expPool -= reqExp;
                } else {
                    if (reqExp > 0 && expPool + points >= reqExp) {
                        player.sendTitle(
                                ChatColor.GREEN + "Osiągnąłeś poziom " + (actualLevel + 1) + " " + skill.getType()
                                        + "!",
                                ChatColor.YELLOW + "Odbierz swoje nagrody!", 20, 80, 20);
                    } else {
                        neededExp = reqExp;
                        break;
                    }
                }
            }
            skill.setExpPoints(skill.getExpPoints() + points);
            
            try {
                if (AmonPackPlugin.ENABLE_DATABASE && AmonPackPlugin.mysqllite() != null) {
                    Connection conn = AmonPackPlugin.mysqllite().getConnection();
                    if (conn != null) {
                        try (Statement stmt = conn.createStatement();
                             ResultSet rs = stmt.executeQuery("select * from Level" + skill.getType().toString() + " where Player='" + player.getName() + "'")) {
                            String usedRewardsStr = skill.getUsedRewards().stream()
                                    .map(String::valueOf)
                                    .collect(Collectors.joining(","));
                            if (!rs.next()) {
                                ExecuteQuery("INSERT INTO Level" + skill.getType().toString()
                                        + " (Player,GeneralLevel,UsedRewards,UpgradePercent)" +
                                        " VALUES ('" + player.getName() + "'," + skill.getExpPoints() + ",'" + usedRewardsStr + "'"
                                        + "," + skill.getUpgradePercent() + ")");
                            } else {
                                ExecuteQuery("UPDATE Level" + skill.getType().toString() + " SET GeneralLevel = '"
                                        + skill.getExpPoints() + "' WHERE Player = '" + player.getName() + "'");
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("Error In Database Player Saving Level Points: " + e.getMessage());
            }

            if (neededExp > 0) {
                int expGained = (int) skill.getExpPoints() - totalExpBefore;
                String msg = ChatColor.GOLD + "+" + points + " EXP " +
                        ChatColor.YELLOW + "(" + skill.getType() + ") " +
                        ChatColor.AQUA + "Poziom: " + actualLevel + " " +
                        ChatColor.GRAY + "[" +
                        ChatColor.GREEN + expPool +
                        ChatColor.GRAY + "/" +
                        ChatColor.GREEN + neededExp +
                        ChatColor.GRAY + "]";

                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(msg));
            }
        } catch (Exception e) {
            System.out.println("Error In Player Adding Level Points: " + e.getMessage());
        }
    }

    public static LevelSkill.SkillType GetSkillTypeByMaterial(Material mat) {
        if (mat == null) return null;
        FileConfiguration config = AmonPackPlugin.getLevelConfig();
        if (config == null) return null;
        org.bukkit.configuration.ConfigurationSection sec = config.getConfigurationSection("AmonPack.Levels");
        if (sec == null) return null;
        for (String key : sec.getKeys(false)) {
            if (!key.startsWith("Mastery") && !key.startsWith("Enabled")) {
                String Path = "AmonPack.Levels." + key;
                String item1 = config.getString(Path + ".Gui.Item");
                String item2 = config.getString(Path + ".Details.LockedItem");
                String item3 = config.getString(Path + ".Details.UnLockedItem");
                Material foundmat = item1 != null ? Material.getMaterial(item1) : null;
                Material foundmat2 = item2 != null ? Material.getMaterial(item2) : null;
                Material foundmat3 = item3 != null ? Material.getMaterial(item3) : null;
                if (foundmat == mat || foundmat2 == mat || foundmat3 == mat) {
                    try {
                        return LevelSkill.SkillType.valueOf(key);
                    } catch (IllegalArgumentException ignored) {}
                }
            }
        }
        return null;
    }

    public static Element GetElementByPlace(int place) {
        FileConfiguration config = AmonPackPlugin.getLevelConfig();
        if (config == null) return null;
        org.bukkit.configuration.ConfigurationSection sec = config.getConfigurationSection("AmonPack.Levels");
        if (sec == null) return null;
        for (String key : sec.getKeys(false)) {
            if (key.startsWith("Mastery")) {
                String Path = "AmonPack.Levels." + key;
                int placeinconfig = config.getInt(Path + ".Gui.Place");
                if (place == placeinconfig) {
                    return Element.getElement(key.replace("Mastery", ""));
                }
            }
        }
        return null;
    }

    public static PlayerLevel GetPlayerLevelFromList(String name) {
        Optional<PlayerLevel> Exist = AllPlayerLevels.stream().filter(lvl -> lvl.getPlayerName().equalsIgnoreCase(name))
                .findFirst();
        if (Exist.isPresent()) {
            return Exist.get();
        }
        return null;
    }

    public void AddPointsToSkill(LevelSkill.SkillType Type, Player player, double points, boolean set) {
        try {
            PlayerLevel Level = AllPlayerLevels.stream()
                    .filter(lvl -> lvl.getPlayerName().equalsIgnoreCase(player.getName())).findFirst().get();
            LevelSkill skill = Level.getPlayerSkills().stream().filter(sk -> sk.getType().equals(Type)).findFirst()
                    .get();
            if (set) {
                skill.setExpPoints(points);
            } else {
                skill.setExpPoints(skill.getExpPoints() + points);
            }
        } catch (Exception e) {
            System.out.println("Error In Player Adding Level Points " + e.getMessage());
        }
    }

    public void CreateInventories() {
        Holder1 = new InventoryXHolder(54, "");
        SkillDetails = new InventoryXHolder(36, "");
        BendingSkillMenu = new InventoryXHolder(54, "");
        BendingSkillTree = new InventoryXHolder(54, "");
        BindingAbilitiesMenu = new InventoryXHolder(18, "");
        SelectElementMenu = new InventoryXHolder(27, "§0§lWybierz Żywioł");
    }

    public static void OpenSelectElementMenu(Player player) {
        Inventory inv = Bukkit.createInventory(SelectElementMenu, SelectElementMenu.getSize(), SelectElementMenu.getTitle());

        ItemStack blank = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta blankMeta = blank.getItemMeta();
        blankMeta.setDisplayName(" ");
        blank.setItemMeta(blankMeta);

        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, blank);
        }

        // Reset button (Slot 10)
        ItemStack reset = new ItemStack(Material.BARRIER);
        ItemMeta resetMeta = reset.getItemMeta();
        resetMeta.setDisplayName("§c§lRESET ELEMENTU I SKILLI");
        resetMeta.setLore(Arrays.asList("§7Czyści wybrane żywioły i", "§7resetuje drzewko skilli."));
        reset.setItemMeta(resetMeta);
        inv.setItem(10, reset);

        // Fire button (Slot 12)
        ItemStack fire = new ItemStack(Material.BLAZE_POWDER);
        ItemMeta fireMeta = fire.getItemMeta();
        fireMeta.setDisplayName("§c§lOGIEŃ (FIRE)");
        fireMeta.setLore(Arrays.asList("§7Daje żywioł Ognia oraz", "§e+5000 punktów ognia."));
        fire.setItemMeta(fireMeta);
        inv.setItem(12, fire);

        // Earth button (Slot 13)
        ItemStack earth = new ItemStack(Material.GRASS_BLOCK);
        ItemMeta earthMeta = earth.getItemMeta();
        earthMeta.setDisplayName("§a§lZIEMIA (EARTH)");
        earthMeta.setLore(Arrays.asList("§7Daje żywioł Ziemi oraz", "§e+5000 punktów ziemi."));
        earth.setItemMeta(earthMeta);
        inv.setItem(13, earth);

        // Water button (Slot 14)
        ItemStack water = new ItemStack(Material.WATER_BUCKET);
        ItemMeta waterMeta = water.getItemMeta();
        waterMeta.setDisplayName("§b§lWODA (WATER)");
        waterMeta.setLore(Arrays.asList("§7Daje żywioł Wody oraz", "§e+5000 punktów wody."));
        water.setItemMeta(waterMeta);
        inv.setItem(14, water);

        // Air button (Slot 16)
        ItemStack air = new ItemStack(Material.FEATHER);
        ItemMeta airMeta = air.getItemMeta();
        airMeta.setDisplayName("§f§lPOWIETRZE (AIR)");
        airMeta.setLore(Arrays.asList("§7Daje żywioł Powietrza oraz", "§e+5000 punktów powietrza."));
        air.setItemMeta(airMeta);
        inv.setItem(16, air);

        player.openInventory(inv);
    }

    private void LoadPlayersFromDatabase() throws SQLException {
        Connection conn = AmonPackPlugin.mysqllite().getConnection();
        if (conn == null) {
            return;
        }
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("select * from LevelGENERAL")) {
            while (rs.next()) {
                List<LevelSkill> Skills = new ArrayList<>();
                String PlayerName = rs.getString(1);
                for (LevelSkill.SkillType Skillt : EnabledSkillTypes) {
                    try (Statement stmtSub = conn.createStatement();
                         ResultSet Result = stmtSub.executeQuery("select * from Level" + Skillt.toString() + " where Player='" + PlayerName + "'")) {
                        while (Result.next()) {
                            String[] parts = Result.getString(3).split(",");
                            List<Integer> intList = new ArrayList<>();
                            for (String part : parts) {
                                try {
                                    if (!Objects.equals(part, "")) {
                                        intList.add(Integer.parseInt(part));
                                    }
                                } catch (Exception e) {
                                    System.out.println("Blad przy wgrywaniu poziomow z bazy danych");
                                }
                            }
                            Skills.add(new LevelSkill(Result.getDouble(2), Skillt, intList, Result.getDouble(4)));
                        }
                    }
                }
                AllPlayerLevels.add(new PlayerLevel(PlayerName, Skills));
            }
        }
    }

    public void LoadIntoDatabase() throws SQLException {
        Connection conn = AmonPackPlugin.mysqllite().getConnection();
        if (conn == null) {
            return;
        }
        for (PlayerLevel PlayerL : AllPlayerLevels) {
            for (LevelSkill Skill : PlayerL.getPlayerSkills()) {
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("select * from Level" + Skill.getType().toString() + " where Player='"
                             + PlayerL.getPlayerName() + "'")) {
                    String result = Skill.getUsedRewards().stream()
                            .map(String::valueOf)
                            .collect(Collectors.joining(","));
                    if (!rs.next()) {
                        ExecuteQuery("INSERT INTO Level" + Skill.getType().toString()
                                + " (Player,GeneralLevel,UsedRewards,UpgradePercent)" +
                                " VALUES ('" + PlayerL.getPlayerName() + "'," + Skill.getExpPoints() + ",'" + result + "'"
                                + "," + Skill.getUpgradePercent() + ")");
                    } else {
                        ExecuteQuery("UPDATE Level" + Skill.getType().toString() + " SET GeneralLevel = '"
                                + Skill.getExpPoints() + "' WHERE Player = '" + PlayerL.getPlayerName() + "'");
                        ExecuteQuery("UPDATE Level" + Skill.getType().toString() + " SET UsedRewards = '" + result
                                + "' WHERE Player = '" + PlayerL.getPlayerName() + "'");
                        ExecuteQuery("UPDATE Level" + Skill.getType().toString() + " SET UpgradePercent = '"
                                + Skill.getUpgradePercent() + "' WHERE Player = '" + PlayerL.getPlayerName() + "'");
                    }
                }
            }
        }
    }

    private static ItemStack ReturnItem() {
        ItemStack Item1 = new ItemStack(Material.BARRIER);
        ItemMeta Item1Meta = Item1.getItemMeta();
        Item1Meta.setDisplayName(ChatColor.RED + "Powrót");
        Item1.setItemMeta(Item1Meta);
        return Item1;
    }

    private static ItemStack GuiBlank() {
        ItemStack Item1 = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta Item1Meta = Item1.getItemMeta();
        Item1Meta.setDisplayName("");
        Item1.setItemMeta(Item1Meta);
        return Item1;
    }

    private String getSkillIcon(LevelSkill.SkillType type) {
        return switch (type) {
            case MINING -> "⛏"; // Kopalnie
            case LUMBERING -> "🌲"; // Drzewa
            case FARMING -> "🌾"; // Uprawy
            case MAGIC -> "✨"; // Magia
            case COMBAT -> "⚔"; // Walka
            case GENERAL -> "❖"; // Postęp ogólny
            case SMITHING -> "🔥"; // Przepalanie
            case BUILDING -> "🏗"; // Budowanie
            case CRAFTING -> "🛠"; // Crafting
            case BOUNTY -> "📜"; // Zlecenia
            default -> "✦"; // Domyślna ikona
        };
    }

    public static boolean CheckPlayerMagicEffectsCondition(MagicEffects effect, Player player) {
        boolean Check = true;
        for (MagicEffectsConditions conditions : effect.getConditions()) {
            if (conditions.isSkillRequired()) {
                if (conditions.getRequiredSkillLevel() > GetSkillByPlayer(conditions.getType(), player)) {
                    Check = false;
                }
            }
        }
        return Check;
    }

}

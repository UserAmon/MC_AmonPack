package AvatarSystems.Levels;

import AvatarSystems.Crafting.Objects.MagicEffects;
import AvatarSystems.Crafting.Objects.MagicEffectsConditions;
import AvatarSystems.Util_Objects.InventoryXHolder;
import AvatarSystems.Util_Objects.LevelSkill;
import AvatarSystems.Util_Objects.PlayerLevel;
import com.projectkorra.projectkorra.Element;
import commands.Commands;
import methods_plugins.AmonPackPlugin;
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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.stream.Collectors;

import static Mechanics.PVP.PvPMethods.sendTitleMessage;
import static methods_plugins.AmonPackPlugin.ExecuteQuery;

public class PlayerLevelMenager {
    public static List<PlayerLevel> AllPlayerLevels;
    public static InventoryXHolder Holder1;
    public static InventoryXHolder SkillDetails;
    public static InventoryXHolder BendingSkillMenu;
    public static InventoryXHolder BendingSkillTree;
    public static InventoryXHolder BindingAbilitiesMenu;
    public static List<LevelSkill.SkillType> EnabledSkillTypes;

    public PlayerLevelMenager() throws SQLException {
        AllPlayerLevels = new ArrayList<>();
        EnabledSkillTypes = new ArrayList<>();
        FileConfiguration config = AmonPackPlugin.getLevelConfig();
        try {
            for (String key : config.getStringList("AmonPack.Levels.Enabled")) {
                EnabledSkillTypes.add(LevelSkill.SkillType.valueOf(key));
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
        LoadPlayersFromDatabase();
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
        Inventory inv = Bukkit.createInventory(SkillDetails, SkillDetails.getSize(), SkillDetails.getTitle());
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, GuiBlank());
        }
        FileConfiguration config = AmonPackPlugin.getLevelConfig();
        String Path = "AmonPack.Levels." + skill.getType().toString();
        int ActualLevel = 0;
        int totallvl = (int) skill.getExpPoints();
        for (String key : config.getConfigurationSection(Path).getKeys(false)) {
            if (key.startsWith("Level")) {
                String newpath = Path + "." + key;
                ItemStack Item;
                ItemMeta LockedItemMeta;
                int MaxLvL = config.getInt(newpath + ".ReqExp");
                int lvl = Integer.parseInt(key.replace("Level_", ""));
                if (totallvl >= MaxLvL) {
                    ActualLevel = lvl;
                    totallvl = totallvl - MaxLvL;
                    Item = new ItemStack(Material.valueOf(config.getString(Path + ".Details.UnLockedItem")));
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
                    for (String Rewards : config.getConfigurationSection(newpath).getKeys(false)) {
                        if (Rewards.startsWith("Reward")) {
                            String reward = config.getString(Path + "." + key + "." + Rewards);
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
                                    Lore.add(ChatColor.LIGHT_PURPLE + "+" + reward + " Punktów Drzewka Magii");
                                }
                            }
                        }
                    }
                    LockedItemMeta.setLore(Lore);
                } else {
                    Item = new ItemStack(Material.valueOf(config.getString(Path + ".Details.LockedItem")));
                    LockedItemMeta = Item.getItemMeta();
                    if (config.getInt(Path + ".Details.LockedItemModelData") != 0) {
                        LockedItemMeta.setCustomModelData(config.getInt(Path + ".Details.LockedItemModelData"));
                    }
                    LockedItemMeta.setDisplayName(ChatColor.RED + "Poziom " + lvl);
                    List<String> Lore = new ArrayList<>();
                    if (lvl == ActualLevel + 1) {
                        Lore.add(ChatColor.LIGHT_PURPLE + "Doświadczenie: " + (totallvl + "/" + MaxLvL));
                    }
                    for (String Rewards : config.getConfigurationSection(newpath).getKeys(false)) {
                        if (Rewards.startsWith("Reward")) {
                            String reward = config.getString(Path + "." + key + "." + Rewards);
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
                                if (reward.startsWith("skillupgrade:")) {
                                    reward = reward.replace("skillupgrade:", "");
                                    Lore.add(ChatColor.AQUA + "Zwiększenie poziomu umiejętności dziedziny");
                                }
                                if (reward.startsWith("SkillPoints:")) {
                                    reward = reward.replace("SkillPoints:", "");
                                    Lore.add(ChatColor.LIGHT_PURPLE + "+" + reward + " Punktów Drzewka Magii");
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
        plmeta.setDisplayName(config.getString(npath + ".SkillDisplay") + ": " + ActualLevel);
        List<String> Lore = new ArrayList<>();
        Lore.add(ChatColor.LIGHT_PURPLE + "Obecne doświadczenie: " + totallvl);
        String BuffSkillLore = config.getString(npath + ".BuffSkilllore");
        BuffSkillLore = BuffSkillLore.replace("%chance%", skill.getUpgradePercent() + "%");
        for (String line : BuffSkillLore.split("%break%")) {
            Lore.add(ChatColor.GRAY + line);
        }
        plmeta.setLore(Lore);
        pl.setItemMeta(plmeta);
        inv.setItem(0, pl);
        inv.setItem(35, ReturnItem());
        p.openInventory(inv);
    }

    private static void OpenPlayerLevelWindow(PlayerLevel level) {
        TexturedInventoryWrapper inventory = new TexturedInventoryWrapper(Holder1,
                Holder1.getSize(), Holder1.getTitle(), new FontImageWrapper("amonpack:first_gui"));
        Inventory inv = inventory.getInternal();
        /*
         * for (int i = 0; i < inv.getSize(); i++) {
         * inv.setItem(i,GuiBlank());
         * }
         */
        FileConfiguration config = AmonPackPlugin.getLevelConfig();
        try {
            for (String key : config.getConfigurationSection("AmonPack.Levels").getKeys(false)) {
                if (!key.startsWith("Enabled")) {
                    if (!key.startsWith("Mastery")) {
                        LevelSkill skill = level.getPlayerSkills().stream()
                                .filter(sk -> sk.getType().toString().equalsIgnoreCase(key)).findFirst().get();
                        String Path = "AmonPack.Levels." + skill.getType().toString();
                        int place = config.getInt(Path + ".Gui.Place");
                        String title = config.getString(Path + ".Gui.Title");
                        ItemStack Item1 = new ItemStack(Material.valueOf(config.getString(Path + ".Gui.Item")));
                        ItemMeta Item1Meta = Item1.getItemMeta();
                        Item1Meta.setDisplayName(title);
                        Item1.setItemMeta(Item1Meta);
                        inv.setItem(place, Item1);
                    } else {
                        String Path = "AmonPack.Levels." + key;
                        int place = config.getInt(Path + ".Gui.Place");
                        int ModelData = config.getInt(Path + ".Gui.ModelData");
                        String title = config.getString(Path + ".Gui.Title");
                        ItemStack Item1 = new ItemStack(Material.PAPER);
                        ItemMeta Item1Meta = Item1.getItemMeta();
                        Item1Meta.setCustomModelData(ModelData);
                        Item1Meta.setDisplayName(title);
                        Item1.setItemMeta(Item1Meta);
                        inv.setItem(place, Item1);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("error open1  " + e.getMessage());
            System.out.println("error open2  " + e.getLocalizedMessage());
            System.out.println("error open3  " + e);
        }
        inventory.showInventory(Bukkit.getPlayer(level.getPlayerName()));
    }

    public static void ClaimReward(LevelSkill.SkillType Type, Player player, String title) {
        try {
            PlayerLevel Level = AllPlayerLevels.stream()
                    .filter(lvl -> lvl.getPlayerName().equalsIgnoreCase(player.getName())).findFirst().get();
            LevelSkill skill = Level.getPlayerSkills().stream().filter(sk -> sk.getType().equals(Type)).findFirst()
                    .get();
            FileConfiguration config = AmonPackPlugin.getLevelConfig();
            String Path = "AmonPack.Levels." + skill.getType().toString();
            for (String key : config.getConfigurationSection(Path).getKeys(false)) {
                String lvl = key.replace("Level_", "");// 1
                title = title.replaceAll("\\D+", "");
                if (lvl.equalsIgnoreCase(title)) {
                    if (Integer.parseInt(lvl) <= ReturnUnlocked(skill)) {
                        if (!skill.getUsedRewards().contains(Integer.parseInt(lvl))) {
                            for (String Rewards : config.getConfigurationSection(Path + "." + key).getKeys(false)) {
                                if (Rewards.startsWith("Reward")) {
                                    String reward = config.getString(Path + "." + key + "." + Rewards);
                                    if (reward.startsWith("command:")) {
                                        reward = reward.replace("command:", "");
                                        reward = reward.replace("%player%", player.getName());
                                        Commands.ExecuteCommandExample example = new Commands.ExecuteCommandExample();
                                        example.executeCommand(reward);
                                    }
                                    if (reward.startsWith("skillupgrade:")) {
                                        reward = reward.replace("skillupgrade:", "");
                                        skill.setUpgradePercent(skill.getUpgradePercent() + Double.parseDouble(reward));
                                    }
                                    if (reward.startsWith("SkillPoints:")) {
                                        reward = reward.replace("SkillPoints:", "");
                                        // BendingGuiMenu.getPlayerSkillTreeByName(player).AddSkillPoints(Integer.parseInt(reward));
                                    }
                                }
                            }
                            List<Integer> usedreward = skill.getUsedRewards();
                            usedreward.add(Integer.valueOf(lvl));
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
        PlayerLevel Level = AllPlayerLevels.stream()
                .filter(lvl -> lvl.getPlayerName().equalsIgnoreCase(player.getName())).findFirst().get();
        LevelSkill skill = Level.getPlayerSkills().stream().filter(sk -> sk.getType().equals(type)).findFirst().get();
        return ReturnUnlocked(skill);
    }

    public static int ReturnUnlocked(LevelSkill skill) {
        FileConfiguration config = AmonPackPlugin.getLevelConfig();
        String Path = "AmonPack.Levels." + skill.getType().toString();
        int totallvl = (int) skill.getExpPoints();
        int lvl = 0;
        for (String key : config.getConfigurationSection(Path).getKeys(false)) {
            if (key.startsWith("Level")) {
                String newpath = Path + "." + key;
                int MaxLvL = config.getInt(newpath + ".ReqExp");
                if (totallvl >= MaxLvL) {
                    totallvl = totallvl - MaxLvL;
                    lvl = Integer.parseInt(key.replace("Level_", ""));
                } else {
                    break;
                }
            }
        }
        return lvl;
    }

    public void AddPoints(LevelSkill.SkillType Type, Player player, int points) {
        try {
            PlayerLevel Level = AllPlayerLevels.stream()
                    .filter(lvl -> lvl.getPlayerName().equalsIgnoreCase(player.getName()))
                    .findFirst().orElse(null);
            if (Level == null)
                return;
            LevelSkill skill = Level.getPlayerSkills().stream()
                    .filter(sk -> sk.getType().equals(Type))
                    .findFirst().orElse(null);
            if (skill == null)
                return;
            FileConfiguration config = AmonPackPlugin.getLevelConfig();
            String path = "AmonPack.Levels." + skill.getType().toString();
            int actualLevel = 0;
            int neededExp = 0;
            int totalExpBefore = (int) skill.getExpPoints();
            int expPool = totalExpBefore;
            for (String key : config.getConfigurationSection(path).getKeys(false)) {
                if (!key.startsWith("Level"))
                    continue;
                String levelPath = path + "." + key;
                int reqExp = config.getInt(levelPath + ".ReqExp");
                int level = Integer.parseInt(key.replace("Level_", ""));
                if (expPool >= reqExp) {
                    actualLevel = level;
                    expPool -= reqExp;
                } else {
                    if (expPool + points >= reqExp) {
                        sendTitleMessage(player,
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
            int newTotalExp = totalExpBefore + points;
            if (points > 1 || newTotalExp % 5 == 0) {
                double progress = (double) (expPool + points) / neededExp;
                ChatColor color;
                if (progress < 0.33) {
                    color = ChatColor.RED;
                } else if (progress < 0.66) {
                    color = ChatColor.GOLD;
                } else {
                    color = ChatColor.GREEN;
                }
                String icon = getSkillIcon(Type);
                String skillName = Type.toString().substring(0, 1) + Type.toString().substring(1).toLowerCase(); // Capitalize
                String msg = ChatColor.DARK_GRAY + "[" +
                        ChatColor.AQUA + icon + " " + ChatColor.BOLD + skillName +
                        ChatColor.DARK_GRAY + "] " +
                        color + (expPool + points) +
                        ChatColor.GRAY + "/" +
                        ChatColor.GREEN + neededExp;

                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(msg));
            }
        } catch (Exception e) {
            System.out.println("Error In Player Adding Level Points: " + e.getMessage());
        }
    }

    public static LevelSkill.SkillType GetSkillTypeByMaterial(Material mat) {
        for (String key : AmonPackPlugin.getLevelConfig().getConfigurationSection("AmonPack.Levels").getKeys(false)) {
            if (!key.startsWith("Mastery") && !key.startsWith("Enabled")) {
                String Path = "AmonPack.Levels." + key;
                Material foundmat = Material.valueOf(AmonPackPlugin.getLevelConfig().getString(Path + ".Gui.Item"));
                Material foundmat2 = Material
                        .valueOf(AmonPackPlugin.getLevelConfig().getString(Path + ".Details.LockedItem"));
                Material foundmat3 = Material
                        .valueOf(AmonPackPlugin.getLevelConfig().getString(Path + ".Details.UnLockedItem"));
                if (foundmat == mat) {
                    return LevelSkill.SkillType.valueOf(key);
                }
                if (foundmat2 == mat) {
                    return LevelSkill.SkillType.valueOf(key);
                }
                if (foundmat3 == mat) {
                    return LevelSkill.SkillType.valueOf(key);
                }
            }
        }
        return null;
    }

    public static Element GetElementByPlace(int place) {
        for (String key : AmonPackPlugin.getLevelConfig().getConfigurationSection("AmonPack.Levels").getKeys(false)) {
            if (key.startsWith("Mastery")) {
                String Path = "AmonPack.Levels." + key;
                int placeinconfig = AmonPackPlugin.getLevelConfig().getInt(Path + ".Gui.Place");
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
    }

    private void LoadPlayersFromDatabase() throws SQLException {
        Statement stmt = AmonPackPlugin.mysqllite().getConnection().createStatement();
        ResultSet rs = stmt.executeQuery("select * from LevelGENERAL");
        while (rs.next()) {
            List<LevelSkill> Skills = new ArrayList<>();
            String PlayerName = rs.getString(1);
            for (LevelSkill.SkillType Skillt : EnabledSkillTypes) {
                ResultSet Result = stmt
                        .executeQuery("select * from Level" + Skillt.toString() + " where Player='" + PlayerName + "'");
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
            AllPlayerLevels.add(new PlayerLevel(PlayerName, Skills));
        }
        stmt.close();
    }

    public void LoadIntoDatabase() throws SQLException {
        Statement stmt = AmonPackPlugin.mysqllite().getConnection().createStatement();
        for (PlayerLevel PlayerL : AllPlayerLevels) {
            for (LevelSkill Skill : PlayerL.getPlayerSkills()) {
                ResultSet rs = stmt.executeQuery("select * from Level" + Skill.getType().toString() + " where Player='"
                        + PlayerL.getPlayerName() + "'");
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
        stmt.close();
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

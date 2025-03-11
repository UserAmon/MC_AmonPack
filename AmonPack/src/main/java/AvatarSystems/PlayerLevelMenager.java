package AvatarSystems;

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

    public PlayerLevelMenager() throws SQLException {
        AllPlayerLevels=new ArrayList<>();
        LoadPlayersFromDatabase();
    }

    public static void TryOpenPlayerLevel(Player player){
        try {
            PlayerLevel Level;
            Optional<PlayerLevel> Exist = AllPlayerLevels.stream().filter(lvl->lvl.getPlayerName().equalsIgnoreCase(player.getName())).findFirst();
            if(Exist.isPresent() && Exist.get().getPlayerSkills().size()>=3){
                Level = Exist.get();
            }else{
                List<LevelSkill>Skills=new ArrayList<>();
                Skills.add(new LevelSkill(0, LevelSkill.SkillType.MINING,new ArrayList<>(),0));
                Skills.add(new LevelSkill(0, LevelSkill.SkillType.COMBAT,new ArrayList<>(),0));
                Skills.add(new LevelSkill(0, LevelSkill.SkillType.GENERAL,new ArrayList<>(),0));
                Level=new PlayerLevel(player.getName(),Skills);
                AllPlayerLevels.add(Level);
            }
            OpenPlayerLevelWindow(Level);
        }catch (Exception e){
            System.out.println("Error In Player Level "+e.getMessage());
        }}
    public static void OpenSkillDetails(LevelSkill skill,Player p){
        Inventory inv = Bukkit.createInventory(SkillDetails, SkillDetails.getSize(), SkillDetails.getTitle());
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i,GuiBlank());
        }
        FileConfiguration config= AmonPackPlugin.getLevelConfig();
        String Path = "AmonPack.Levels." + skill.getType().toString();
        int ActualLevel=0;
        int totallvl= (int) skill.getExpPoints();
        for(String key : config.getConfigurationSection(Path).getKeys(false)) {
            if(key.startsWith("Level")){
                String newpath=Path + "."+key;
                ItemStack Item;
                ItemMeta LockedItemMeta;
                int MaxLvL = config.getInt(newpath+".ReqExp");
                int lvl = Integer.parseInt(key.replace("Level_",""));
                if(totallvl>=MaxLvL){
                    ActualLevel=lvl;
                    totallvl=totallvl-MaxLvL;
                    Item = new ItemStack(Material.valueOf(config.getString(Path + ".Details.UnLockedItem")));
                    LockedItemMeta = Item.getItemMeta();
                    LockedItemMeta.setDisplayName(ChatColor.GREEN+"Poziom "+lvl);
                    List<String>Lore=new ArrayList<>();
                    if(skill.getUsedRewards().contains(lvl)){
                        Lore.add(ChatColor.RED+ "Juz odebrano tę nagrodę");
                    }else{
                        Lore.add(ChatColor.GREEN+ "Nagroda dostepna");
                    }
                    LockedItemMeta.setLore(Lore);
                }else{
                    Item = new ItemStack(Material.valueOf(config.getString(Path + ".Details.LockedItem")));
                    LockedItemMeta = Item.getItemMeta();
                    LockedItemMeta.setDisplayName(ChatColor.RED+"Poziom "+lvl);
                    if(lvl==ActualLevel+1){
                        List<String>Lore=new ArrayList<>();
                        Lore.add(totallvl+"/"+MaxLvL);
                        LockedItemMeta.setLore(Lore);
                    }}
                Item.setItemMeta(LockedItemMeta);
                inv.setItem(8 + lvl, Item);
            }
        }
        ItemStack pl = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta plmeta = pl.getItemMeta();
        plmeta.setDisplayName(ChatColor.GOLD+"Twój poziom: "+ActualLevel);
        List<String>Lore=new ArrayList<>();
        Lore.add(ChatColor.LIGHT_PURPLE+ "Twój Exp: "+totallvl);
        Lore.add(ChatColor.AQUA+ "Szansa na podwójny drop: "+skill.getUpgradePercent()+"%");
        plmeta.setLore(Lore);
        pl.setItemMeta(plmeta);
        inv.setItem(0,pl);
        inv.setItem(26,ReturnItem());
        p.openInventory(inv);
    }
    private static void OpenPlayerLevelWindow(PlayerLevel level){
        Inventory inv = Bukkit.createInventory(Holder1, Holder1.getSize(), Holder1.getTitle());
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i,GuiBlank());
        }
        FileConfiguration config= AmonPackPlugin.getLevelConfig();
        try {
            for(String key : config.getConfigurationSection("AmonPack.Levels").getKeys(false)) {
                if(!key.startsWith("Mastery")) {
                    LevelSkill skill = level.getPlayerSkills().stream().filter(sk -> sk.getType().toString().equalsIgnoreCase(key)).findFirst().get();
                    String Path = "AmonPack.Levels." + skill.getType().toString();
                    int place = config.getInt(Path + ".Gui.Place");
                    String title = config.getString(Path + ".Gui.Title");
                    ItemStack Item1 = new ItemStack(Material.valueOf(config.getString(Path + ".Gui.Item")));
                    ItemMeta Item1Meta = Item1.getItemMeta();
                    Item1Meta.setDisplayName(title);
                    Item1.setItemMeta(Item1Meta);
                    inv.setItem(place, Item1);
                }else{
                    String Path = "AmonPack.Levels." + key;
                    int place = config.getInt(Path + ".Gui.Place");
                    int ModelData = config.getInt(Path + ".Gui.ModelData");
                    String title = config.getString(Path + ".Gui.Title");
                    ItemStack Item1=new ItemStack(Material.PAPER);
                    ItemMeta Item1Meta = Item1.getItemMeta();
                    Item1Meta.setCustomModelData(ModelData);
                    Item1Meta.setDisplayName(title);
                    Item1.setItemMeta(Item1Meta);
                    inv.setItem(place,Item1);
                }
            }
        } catch (Exception e) {
            System.out.println("error "+e.getMessage());
        }
        Bukkit.getPlayer(level.getPlayerName()).openInventory(inv);
    }
    public static void ClaimReward(LevelSkill.SkillType Type, Player player, String title){
        try {
            PlayerLevel Level = AllPlayerLevels.stream().filter(lvl->lvl.getPlayerName().equalsIgnoreCase(player.getName())).findFirst().get();
            LevelSkill skill = Level.getPlayerSkills().stream().filter(sk->sk.getType().equals(Type)).findFirst().get();
            FileConfiguration config= AmonPackPlugin.getLevelConfig();
            String Path = "AmonPack.Levels." + skill.getType().toString();
            for(String key : config.getConfigurationSection(Path).getKeys(false)) {
                String lvl = key.replace("Level_", "");//1
                title = title.replaceAll("\\D+", "");
                if(lvl.equalsIgnoreCase(title)){
                    if(Integer.parseInt(lvl)<=ReturnUnlocked(skill)){
                        if(skill.getUsedRewards().contains(Integer.parseInt(lvl))){
                            player.sendMessage(ChatColor.RED+ "Juz odebrano te nagrode");
                        }else{
                            for(String Rewards : config.getConfigurationSection(Path + "." + key).getKeys(false)) {
                                if(Rewards.startsWith("Reward")){
                                    String reward = config.getString(Path + "." + key + "."+Rewards);
                                    if(reward.startsWith("command:")){
                                        reward=reward.replace("command:","");
                                        reward=reward.replace("%player%",player.getName());
                                        Commands.ExecuteCommandExample example = new Commands.ExecuteCommandExample();
                                        example.executeCommand(reward);
                                    }
                                    if(reward.startsWith("skillupgrade:")){
                                        reward=reward.replace("skillupgrade:","");
                                        skill.setUpgradePercent(skill.getUpgradePercent()+Double.parseDouble(reward));
                                    }
                                }
                            }
                            List<Integer> usedreward=skill.getUsedRewards();
                            usedreward.add(Integer.valueOf(lvl));
                            skill.setUsedRewards(usedreward);
                            OpenSkillDetails(skill,player);
                        }}
                    break;
                }}
        }catch (Exception e){
            System.out.println("Error In Player Adding Level Points "+e.getMessage());
        }
    }
    public static int GetSkillByPlayer(LevelSkill.SkillType type, Player player){
        PlayerLevel Level = AllPlayerLevels.stream().filter(lvl->lvl.getPlayerName().equalsIgnoreCase(player.getName())).findFirst().get();
        LevelSkill skill = Level.getPlayerSkills().stream().filter(sk->sk.getType().equals(type)).findFirst().get();
        return ReturnUnlocked(skill);
    }
    public static int ReturnUnlocked(LevelSkill skill){
        FileConfiguration config= AmonPackPlugin.getLevelConfig();
        String Path = "AmonPack.Levels." + skill.getType().toString();
        int totallvl= (int) skill.getExpPoints();
        int lvl = 0;
        for(String key : config.getConfigurationSection(Path).getKeys(false)) {
            if (key.startsWith("Level")) {
                String newpath = Path + "." + key;
                int MaxLvL = config.getInt(newpath + ".ReqExp");
                if (totallvl >= MaxLvL) {
                    totallvl = totallvl - MaxLvL;
                    lvl = Integer.parseInt(key.replace("Level_", ""));
                }else{
                    break;
                }
            }}
        return lvl;
    }
    public void AddPoints(LevelSkill.SkillType Type, Player player, double points,String title){
        try {
            PlayerLevel Level = AllPlayerLevels.stream().filter(lvl->lvl.getPlayerName().equalsIgnoreCase(player.getName())).findFirst().get();
            LevelSkill skill = Level.getPlayerSkills().stream().filter(sk->sk.getType().equals(Type)).findFirst().get();
            FileConfiguration config= AmonPackPlugin.getLevelConfig();
            String Path = "AmonPack.Levels." + skill.getType().toString();
            int ActualLevel=0;
            int NeededLvL=0;
            int totallvl= (int) skill.getExpPoints();
            for(String key : config.getConfigurationSection(Path).getKeys(false)) {
                if (key.startsWith("Level")) {
                    String newpath = Path + "." + key;
                    int MaxLvL = config.getInt(newpath + ".ReqExp");
                    int lvl = Integer.parseInt(key.replace("Level_", ""));
                    if (totallvl >= MaxLvL) {
                        ActualLevel = lvl;
                        totallvl = totallvl - MaxLvL;
                    } else {
                        if (totallvl + points >= MaxLvL) {
                            sendTitleMessage(player, ChatColor.GREEN + "Osiągnieto " + (ActualLevel+1) + " Poziom " + skill.getType() + "!", ChatColor.YELLOW + "Udało Ci się osiągnąć nowy poziom, odbierz nagrody", 20, 80, 20);
                        }else{
                            NeededLvL=MaxLvL;
                            break;
                        }
                    }}}
            skill.setExpPoints(skill.getExpPoints()+points);
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(title+" "+(totallvl + points)+"/"+NeededLvL));
        }catch (Exception e){
            System.out.println("Error In Player Adding Level Points "+e.getMessage());
        }
    }
    public static LevelSkill.SkillType GetSkillTypeByMaterial(Material mat){
        for(String key : AmonPackPlugin.getLevelConfig().getConfigurationSection("AmonPack.Levels").getKeys(false)) {
            if(!key.startsWith("Mastery")) {
                String Path = "AmonPack.Levels." + key;
                Material foundmat = Material.valueOf(AmonPackPlugin.getLevelConfig().getString(Path + ".Gui.Item"));
                Material foundmat2 = Material.valueOf(AmonPackPlugin.getLevelConfig().getString(Path + ".Details.LockedItem"));
                Material foundmat3 = Material.valueOf(AmonPackPlugin.getLevelConfig().getString(Path + ".Details.UnLockedItem"));
                if(foundmat==mat){
                    return LevelSkill.SkillType.valueOf(key);
                }
                if(foundmat2==mat){
                    return LevelSkill.SkillType.valueOf(key);
                }
                if(foundmat3==mat){
                    return LevelSkill.SkillType.valueOf(key);
                }
            }
        }
        return null;
    }
    public static Element GetElementByPlace(int place){
        for(String key : AmonPackPlugin.getLevelConfig().getConfigurationSection("AmonPack.Levels").getKeys(false)) {
            if(key.startsWith("Mastery")) {
                String Path = "AmonPack.Levels." + key;
                int placeinconfig =AmonPackPlugin.getLevelConfig().getInt(Path + ".Gui.Place");
                if(place==placeinconfig){
                    return Element.getElement(key.replace("Mastery",""));
                }
            }
        }
        return null;
    }
    public static PlayerLevel GetPlayerLevelFromList(String name){
        Optional<PlayerLevel> Exist = AllPlayerLevels.stream().filter(lvl->lvl.getPlayerName().equalsIgnoreCase(name)).findFirst();
        if(Exist.isPresent()){
            return Exist.get();
        }
        return null;
    }
    public void AddPointsToSkill(LevelSkill.SkillType Type, Player player, double points,boolean set){
        try {
            PlayerLevel Level = AllPlayerLevels.stream().filter(lvl->lvl.getPlayerName().equalsIgnoreCase(player.getName())).findFirst().get();
            LevelSkill skill = Level.getPlayerSkills().stream().filter(sk->sk.getType().equals(Type)).findFirst().get();
            if(set){
                skill.setExpPoints(points);
            }else{
                skill.setExpPoints(skill.getExpPoints()+points);
            }
        }catch (Exception e){
            System.out.println("Error In Player Adding Level Points "+e.getMessage());
        }
    }
    public void CreateInventories(){
        Holder1=new InventoryXHolder(36,"");
        SkillDetails=new InventoryXHolder(27,"");
    }
    private void LoadPlayersFromDatabase()throws SQLException {
        Statement stmt = AmonPackPlugin.mysqllite().getConnection().createStatement();
        ResultSet rs = stmt.executeQuery("select * from LevelGENERAL");//PlayerMiningLevel,PlayerCombatLevel
        while (rs.next()) {
            List<LevelSkill>Skills=new ArrayList<>();
            String PlayerName = rs.getString(1);
            Double PlayerPoints = rs.getDouble(2);
            ResultSet Combat = stmt.executeQuery("select * from LevelCOMBAT where Player='"+PlayerName+"'");
            while (Combat.next()) {
                String[] parts = Combat.getString(3).split(",");
                List<Integer> intList = new ArrayList<>();
                for (String part : parts) {
                    try {
                        if(!Objects.equals(part, "")){
                            intList.add(Integer.parseInt(part));
                        }
                    }catch (Exception e){
                        System.out.println("Blad przy wgrywaniu poziomow z bazy danych");
                    }
                }
                Skills.add(new LevelSkill(Combat.getDouble(2), LevelSkill.SkillType.COMBAT,intList,Combat.getDouble(4)));
            }
            ResultSet Mining = stmt.executeQuery("select * from LevelMINING where Player='"+PlayerName+"'");
            while (Mining.next()) {
                String[] parts = Mining.getString(3).split(",");
                List<Integer> intList = new ArrayList<>();
                for (String part : parts) {
                    try {
                        if(!Objects.equals(part, "")){
                            intList.add(Integer.parseInt(part));
                        }
                    }catch (Exception e){
                        System.out.println("Blad przy wgrywaniu poziomow z bazy danych "+e.getMessage());
                    }
                }
                Skills.add(new LevelSkill(Mining.getDouble(2), LevelSkill.SkillType.MINING,intList,Mining.getDouble(4)));
            }
            List<Integer> intList = new ArrayList<>();
            try {
                String[] parts = rs.getString(3).split(",");
                for (String part : parts) {
                    intList.add(Integer.parseInt(part));
                }
            } catch (Exception e) {
                System.out.println("Blad przy wgrywaniu poziomow z bazy danych " + e.getMessage());
            }
            Skills.add(new LevelSkill(PlayerPoints, LevelSkill.SkillType.GENERAL,intList,rs.getDouble(4)));
            AllPlayerLevels.add(new PlayerLevel(PlayerName,Skills));
        }
        stmt.close();
    }
    public void LoadIntoDatabase() throws SQLException {
        Statement stmt = AmonPackPlugin.mysqllite().getConnection().createStatement();
        for (PlayerLevel PlayerL : AllPlayerLevels){
            for (LevelSkill Skill : PlayerL.getPlayerSkills()){
                ResultSet rs = stmt.executeQuery("select * from Level"+Skill.getType().toString()+" where Player='" +PlayerL.getPlayerName()+"'");
                String result = Skill.getUsedRewards().stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(","));
                if (!rs.next()) {
                    ExecuteQuery("INSERT INTO Level"+Skill.getType().toString()+" (Player,GeneralLevel,UsedRewards,UpgradePercent)" +
                            " VALUES ('" + PlayerL.getPlayerName() +"',"+ Skill.getExpPoints()+",'"+result+"'"+","+Skill.getUpgradePercent()+")");
                } else {
                    ExecuteQuery("UPDATE Level"+Skill.getType().toString()+" SET GeneralLevel = '" + Skill.getExpPoints() + "' WHERE Player = '"+ PlayerL.getPlayerName()+"'");
                    ExecuteQuery("UPDATE Level"+Skill.getType().toString()+" SET UsedRewards = '" + result + "' WHERE Player = '"+ PlayerL.getPlayerName()+"'");
                    ExecuteQuery("UPDATE Level"+Skill.getType().toString()+" SET UpgradePercent = '" + Skill.getUpgradePercent() + "' WHERE Player = '"+ PlayerL.getPlayerName()+"'");
                }}}
        stmt.close();
    }
    private static ItemStack ReturnItem(){
        ItemStack Item1=new ItemStack(Material.BARRIER);
        ItemMeta Item1Meta = Item1.getItemMeta();
        Item1Meta.setDisplayName(ChatColor.RED+"Powrót");
        Item1.setItemMeta(Item1Meta);
        return Item1;
    }
    private static ItemStack GuiBlank(){
        ItemStack Item1=new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta Item1Meta = Item1.getItemMeta();
        Item1Meta.setDisplayName("");
        Item1.setItemMeta(Item1Meta);
        return Item1;
    }
}

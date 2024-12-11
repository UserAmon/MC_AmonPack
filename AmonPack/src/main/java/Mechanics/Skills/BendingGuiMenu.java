package Mechanics.Skills;

import Mechanics.PVE.Menagerie.Menagerie;
import UtilObjects.Skills.PlayerSkillTree;
import UtilObjects.Skills.SkillTree_Ability;
import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.ability.Ability;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.board.BendingBoardManager;
import methods_plugins.AmonPackPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

import static Mechanics.PVE.Menagerie.MenagerieMenager.ListOfAllMenageries;
import static UtilObjects.Skills.PlayerSkillTree.SetPathAndPoints;

public class BendingGuiMenu {

    public static List<SkillTree_Ability> ListOfAllAvailableAbilities = new ArrayList<>();
    public static List<String> DefAbilities = new ArrayList<>();
    public static List<PlayerSkillTree> AllPlayersSkillTrees = new ArrayList<>();
    private static final HashMap<Element,List<Integer>> PathDecoration = new HashMap<>();
    public static final HashMap<String,Integer> MaxRowsForElementTree = new HashMap<>();


    public BendingGuiMenu() throws SQLException {
        AddPlayerFromDBToListOnEnable();
        DefineAvailableAbilities();
    }



    public static void OpenGeneralBendingMenu(Player player) {
        PlayerSkillTree PSkillTree = getPlayerSkillTreeByName(player);
        if(PSkillTree==null ||PSkillTree.getElementsInPossesionAsString().contains("brak")){
            player.sendMessage(ChatColor.RED+ "Nie mozesz uzyc tej komendy - nie wybrales jeszcze zywiolu");
            return;
        }
            Inventory menu = Bukkit.createInventory(null, 36, "GeneralBending");
            ItemStack Background = FastEasyStack(Material.BLACK_STAINED_GLASS_PANE,ChatColor.BLACK + "");
            for (int i = 0; i < menu.getSize(); i++) {
                menu.setItem(i, Background);
            }
           // if (!newPvP.Multibend){
                Element ele = Element.getElement(PSkillTree.getCurrentElement());
                menu.setItem(4, FastEasyStack(MaterialByElement(ele),ele.getColor()+ele.getName()));
            //}else{
            //    menu.setItem(4, FastEasyStack(Material.AMETHYST_SHARD,ChatColor.LIGHT_PURPLE+"Multibend Aktywny"));
            //}
            menu.setItem(20, FastEasyStack(MaterialByElement(Element.WATER),Element.WATER.getColor()+Element.WATER.getName()));
            menu.setItem(21, FastEasyStack(MaterialByElement(Element.EARTH),Element.EARTH.getColor()+Element.EARTH.getName()));
            menu.setItem(23, FastEasyStack(MaterialByElement(Element.FIRE),Element.FIRE.getColor()+Element.FIRE.getName()));
            menu.setItem(24, FastEasyStack(MaterialByElement(Element.AIR),Element.AIR.getColor()+Element.AIR.getName()));
            menu.setItem(35, FastEasyStack(Material.BARRIER,ChatColor.RED + "Zamknij"));
            player.openInventory(menu);
        }


    public static void OpenAbilitiesByElement(PlayerSkillTree PSkillTree ,Element ele, Player player){
        Inventory menu = Bukkit.createInventory(null, 54, "Menu: "+ele.getName());
        ItemStack Background = FastEasyStack(Material.BLACK_STAINED_GLASS_PANE,ChatColor.BLACK + "");
        for (int i = 0; i < menu.getSize(); i++) {
            menu.setItem(i, Background);
        }
        int i =0;
            for (SkillTree_Ability STA:ListOfAllAvailableAbilities) {
                Ability abi = CoreAbility.getAbility(STA.getName());
                if (abi != null && (PSkillTree.getSelectedPath().contains(STA.getName()) || STA.isdef()) && abi.getElement() != null) {
                    if (SubElementByElement(ele,abi) ||abi.getElement().equals(ele) || abi.getElement().equals(ElementBasedOnSubElement(ele)) || SubElementByElement(ElementBasedOnSubElement(ele),abi)) {
                        menu.setItem(i, FastEasyStack(Material.GREEN_TERRACOTTA,STA.getName()));
                        i++;
                    }}}
        for (Menagerie mena:ListOfAllMenageries) {
            if (mena.IsInMenagerie(player.getLocation())){
                List<String> upgrade = AmonPackPlugin.getPlayerUpgrades(player);
                for (Mechanics.Skills.Upgrades UPV: UpgradesMenager.MenagerieUpgradesList) {
                    if (upgrade.contains(UPV.getName())){
                        if (UPV.getType() == Upgrades.MenagerieUpgradeType.ABILITY){
                            Ability abi = CoreAbility.getAbility(UPV.getAbilityName());
                            if (abi != null && (!PSkillTree.getSelectedPath().contains(abi.getName())) && abi.getElement() != null) {
                                if (SubElementByElement(ele,abi) ||abi.getElement().equals(ele) || abi.getElement().equals(ElementBasedOnSubElement(ele)) || SubElementByElement(ElementBasedOnSubElement(ele),abi)) {
                                    menu.setItem(i, FastEasyStack(Material.GREEN_TERRACOTTA,UPV.getAbilityName()));
                                    i++;
                                    break;
                                }}}}}
                break;
            }}
        menu.setItem(45, FastEasyStack(Material.CHEST,ChatColor.LIGHT_PURPLE + "Twoje Punkty: " + PSkillTree.getActSkillPoints()));
        menu.setItem(53, FastEasyStack(Material.BARRIER,ChatColor.RED + "Zamknij"));
        player.openInventory(menu);
    }

    public static void OpenSkillTreeMenuByElement(Player p, Element ele, int page,PlayerSkillTree PSkillTree){
        PSkillTree.setCurrentPage(page);
                Inventory menu = Bukkit.createInventory(null, 54, "Skills: "+ele.getName());
                for (int i = 0; i < menu.getSize()-1; i++) {
                    menu.setItem(i, FastEasyStack(Material.BLACK_STAINED_GLASS_PANE,ChatColor.BLACK + ""));
                }
                for (SkillTree_Ability STA : ListOfAllAvailableAbilities){
                    int tempplace = STA.getPlace()-(54*page);
                    if (tempplace>=0 && tempplace<53){
                        Ability tempabi = CoreAbility.getAbility(STA.getName());
                        if (tempabi != null && STA.getElement() != null) {
                            if (SubElementByElement(ele,tempabi) ||STA.getElement().equals(ele) ||
                                    SubElementByElement(ElementBasedOnSubElement(ele),tempabi) ||
                                    STA.getElement().equals(ElementBasedOnSubElement(ele))) {
                                if (PSkillTree.getSelectedPath().contains(STA.getName()) || STA.isdef()){
                                    menu.setItem(tempplace, FastEasyStack(Material.GREEN_TERRACOTTA,STA.getName()));
                                }else{
                                    if (PSkillTree.getActSkillPoints() >= STA.getCost() && (PSkillTree.getSelectedPath().containsAll(STA.getListOfPreAbility()) || STA.getListOfPreAbility().size()==0)) {
                                        List<String> modifiedList = new ArrayList<>(Collections.singleton("Koszt: " + STA.getCost()));
                                        for (String st:STA.getListOfPreAbility()) {
                                            modifiedList.add("Wymagane: "+st);
                                        }
                                        ItemStack TempItem = FastEasyStackWithLore(Material.ORANGE_TERRACOTTA,STA.getName(),modifiedList);
                                        menu.setItem(tempplace, TempItem);
                                    }else{
                                        List<String> modifiedList = new ArrayList<>(Collections.singleton("Koszt: " + STA.getCost()));
                                        for (String st:STA.getListOfPreAbility()) {
                                            modifiedList.add("Wymagane: "+st);
                                        }
                                        ItemStack TempItem = FastEasyStackWithLore(Material.RED_TERRACOTTA,STA.getName(),modifiedList);
                                        menu.setItem(tempplace, TempItem);
                                    }}}}}
                    for (Integer i:PathDecoration.get(ele)) {
                        int tempPatDec = i-(54*page);
                        if (tempPatDec>=0 && tempPatDec<53){
                            menu.setItem(tempPatDec, FastEasyStack(Material.GLASS_PANE,ChatColor.BLACK + ""));
                        }}}
                menu.setItem(44, FastEasyStack(Material.CHEST,ChatColor.RED + "Twoje Punkty: " + PSkillTree.getActSkillPoints()));
                menu.setItem(53, FastEasyStack(Material.BARRIER,ChatColor.RED + "Powrot"));
                menu.setItem(26, FastEasyStack(Material.OAK_SIGN,ChatColor.RED + "/\\"));
                menu.setItem(35, FastEasyStack(Material.DARK_OAK_SIGN,ChatColor.RED + "\\/"));
                p.openInventory(menu);
    }

    public static void OpenBindingGui(Player p, String st){
        Inventory menu = Bukkit.createInventory(null, 18, "Bind");
        menu.setItem(4, FastEasyStack(Material.GREEN_STAINED_GLASS_PANE,st));
        for (int i = 9; i < 18; i++) {
            menu.setItem(i, FastEasyStack(Material.RED_STAINED_GLASS_PANE,""+(i-8)));
        }
        menu.setItem(8, FastEasyStack(Material.BARRIER,ChatColor.RED + "Powrot"));
        p.openInventory(menu);
    }
    public static void OpenElementChangeMenu(Player p,PlayerSkillTree PSkillTree){
        Inventory menu = Bukkit.createInventory(null, 18, "SelectElement");
        ItemStack Background = FastEasyStack(Material.BLACK_STAINED_GLASS_PANE,ChatColor.BLACK + "");
        for (int i = 0; i < menu.getSize(); i++) {
            menu.setItem(i, Background);
        }
        if (PSkillTree.getElementsInPossesion().contains("Water")){
            menu.setItem(2, FastEasyStack(MaterialByElement(Element.WATER),Element.WATER.getColor()+"Woda"));
        }
        if (PSkillTree.getElementsInPossesion().contains("Fire")){
            menu.setItem(3, FastEasyStack(MaterialByElement(Element.FIRE),Element.FIRE.getColor()+"Ogień"));
        }
        if (PSkillTree.getElementsInPossesion().contains("Air")){
            menu.setItem(5, FastEasyStack(MaterialByElement(Element.AIR),Element.AIR.getColor()+"Powietrze"));
        }
        if (PSkillTree.getElementsInPossesion().contains("Earth")){
            menu.setItem(6, FastEasyStack(MaterialByElement(Element.EARTH),Element.EARTH.getColor()+"Ziemia"));
        }
        switch (PSkillTree.getCurrentElement()){
            case "Water":
                menu.setItem(11, FastEasyStack(Material.GREEN_STAINED_GLASS_PANE,ChatColor.BLACK + ""));
                break;
            case "Fire":
                menu.setItem(12, FastEasyStack(Material.GREEN_STAINED_GLASS_PANE,ChatColor.BLACK + ""));
                break;
            case "Air":
                menu.setItem(14, FastEasyStack(Material.GREEN_STAINED_GLASS_PANE,ChatColor.BLACK + ""));
                break;
            case "Earth":
                menu.setItem(15, FastEasyStack(Material.GREEN_STAINED_GLASS_PANE,ChatColor.BLACK + ""));
                break;
        }
        menu.setItem(8, FastEasyStack(Material.BARRIER,ChatColor.RED + "Powrot"));
        p.openInventory(menu);
    }

    public static void ChangeElement(Player p, BendingPlayer bPlayer, String st) throws SQLException {
        PlayerSkillTree PSkillTree = BendingGuiMenu.getPlayerSkillTreeByName(p);
        if (PSkillTree == null){
            SetPathAndPoints(p.getName(),0,"",st);
        }else{
        /*if (!Multibend){
            for (int i = 0; i <= 9; i++) {
                BendingBoardManager.getBoard(p).get().clearSlot(i);
                bPlayer.getAbilities().remove(i);
            }}*/
        SetPathAndPoints(p.getName(),PSkillTree.getActSkillPoints(),PSkillTree.getSelectedPathAsString(),st);
    }}
    public static Material MaterialByElement(Element ele){
        if (Element.AIR.equals(ele)) {
            return Material.COBWEB;
        } else if (Element.EARTH.equals(ele)) {
            return Material.DIRT;
        } else if (Element.WATER.equals(ele)) {
            return Material.WATER_BUCKET;
        } else if (Element.FIRE.equals(ele)) {
            return Material.FIRE_CHARGE;
        }else return null;
    }
    public static Element ElementByMaterial(Material mat){
        if (Material.COBWEB.equals(mat)) {
            return Element.AIR;
        } else if (Material.DIRT.equals(mat)) {
            return Element.EARTH;
        } else if (Material.WATER_BUCKET.equals(mat)) {
            return Element.WATER;
        } else if (Material.FIRE_CHARGE.equals(mat)) {
            return Element.FIRE;
        }
        return null;
    }
    public static ItemStack FastEasyStack(Material mat, String name){
        ItemStack TempItem = new ItemStack(mat);
        ItemMeta IMeta = TempItem.getItemMeta();
        IMeta.setDisplayName(name);
        TempItem.setItemMeta(IMeta);
        return TempItem;
    }
    public static ItemStack FastEasyStackWithLore(Material mat, String name,List<String> lore){
        ItemStack TempItem = new ItemStack(mat);
        ItemMeta IMeta = TempItem.getItemMeta();
        IMeta.setDisplayName(name);
        IMeta.setLore(lore);
        TempItem.setItemMeta(IMeta);
        return TempItem;
    }
    private void DefineAvailableAbilities(){
        for(String AbiElement : Objects.requireNonNull(AmonPackPlugin.getSkillTreeConfig().getConfigurationSection("AmonPack.SpellTree.Abilities")).getKeys(false)) {
            MaxRowsForElementTree.put(AbiElement,0);
            PathDecoration.put(Element.getElement(AbiElement),AmonPackPlugin.getSkillTreeConfig().getIntegerList("AmonPack.SpellTree.Abilities."+AbiElement+".PathDecoration"));
            for(String Ability : Objects.requireNonNull(AmonPackPlugin.getSkillTreeConfig().getConfigurationSection("AmonPack.SpellTree.Abilities."+AbiElement)).getKeys(false)) {
                int Cost = AmonPackPlugin.getSkillTreeConfig().getInt("AmonPack.SpellTree.Abilities."+AbiElement+"."+Ability+".Cost");
                int Place = AmonPackPlugin.getSkillTreeConfig().getInt("AmonPack.SpellTree.Abilities."+AbiElement+"."+Ability+".Place");
                List<String> ReqAbi = AmonPackPlugin.getSkillTreeConfig().getStringList("AmonPack.SpellTree.Abilities."+AbiElement+"."+Ability+".ReqAbilities");
                SkillTree_Ability AbilityObject = new SkillTree_Ability(Element.getElement(AbiElement),Ability,Cost,ReqAbi,Place,Cost == 0);
                if (Cost==0){
                    DefAbilities.add(Ability);
                }
                ListOfAllAvailableAbilities.add(AbilityObject);
                if (MaxRowsForElementTree.get(AbiElement) < Place){
                    MaxRowsForElementTree.replace(AbiElement,Place);
                }}}
    }
    private void AddPlayerFromDBToListOnEnable() throws SQLException {
        Statement stmt = AmonPackPlugin.mysqllite().getConnection().createStatement();
        ResultSet rs = stmt.executeQuery("select * from SpellTree");
        if (rs.next()) {
            AllPlayersSkillTrees.add(new PlayerSkillTree(
                    rs.getString(1),
                    rs.getInt(2),
                    rs.getString(3),
                    rs.getString(5),
                    rs.getString(4)
            ));
        }
        stmt.close();
    }
    public static PlayerSkillTree getPlayerSkillTreeByName(Player p) {
        for (PlayerSkillTree skillTree : AllPlayersSkillTrees) {
            if (skillTree.getPlayer().equals(p.getName())) {
                return skillTree;
            }}
        return null;
    }
    public static Element ElementBasedOnSubElement(Element SubElement){
        for (Element element:Element.getAllElements()) {
            if (element.equals(SubElement)){
                return element;
            }
            for (Element subele:Element.getSubElements(element)) {
                if (subele.equals(SubElement)){
                    return element;
                }}}
        return null;
    }
    public static boolean SubElementByElement(Element MElement, Ability abi){
        if (abi.getElement().equals(MElement)) {
            return true;
        }
        for (Element element:Element.getSubElements(MElement)) {
            if (abi.getElement().equals(element)) {
                return true;
            }}
        return false;
    }
}

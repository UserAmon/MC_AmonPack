package OLD;
/*
import OLD.Assault.AssaultDef;
import OLD.Assault.AssaultMenager;
import OLD.Assault.AssaultMethods;
import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.ability.Ability;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.board.BendingBoardManager;
import General.AmonPackPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import static Mechanics.PVP.PvP.Multibend;
import static OLD.SkillTreeObj.*;
import static General.AmonPackPlugin.ExecuteQuery;

public class PGrowth implements Listener {
    public static List<SkillTreeObj> SkillPoints = new ArrayList<>();
    private static HashMap<Element,List<Integer>> PathDecoration = new HashMap<>();
    public static List<STAbility> STAList = new ArrayList<>();
    public HashMap<String,Integer> MaxForElement = new HashMap<>();
    public PGrowth() throws SQLException {
        for(String AbiElement : Objects.requireNonNull(AmonPackPlugin.getSkillTreeConfig().getConfigurationSection("AmonPack.SpellTree.Abilities")).getKeys(false)) {
            MaxForElement.put(AbiElement,0);
            PathDecoration.put(Element.getElement(AbiElement),AmonPackPlugin.getSkillTreeConfig().getIntegerList("AmonPack.SpellTree.Abilities."+AbiElement+".PathDecoration"));
            for(String Ability : Objects.requireNonNull(AmonPackPlugin.getSkillTreeConfig().getConfigurationSection("AmonPack.SpellTree.Abilities."+AbiElement)).getKeys(false)) {
                int Cost = AmonPackPlugin.getSkillTreeConfig().getInt("AmonPack.SpellTree.Abilities."+AbiElement+"."+Ability+".Cost");
                int Place = AmonPackPlugin.getSkillTreeConfig().getInt("AmonPack.SpellTree.Abilities."+AbiElement+"."+Ability+".Place");
                List<String> ReqAbi = AmonPackPlugin.getSkillTreeConfig().getStringList("AmonPack.SpellTree.Abilities."+AbiElement+"."+Ability+".ReqAbilities");
                STAbility AbilityObject = new STAbility(Element.getElement(AbiElement),Ability,Cost,ReqAbi,Place,Cost == 0);
                STAList.add(AbilityObject);
                if (MaxForElement.get(AbiElement) < Place){
                    MaxForElement.replace(AbiElement,Place);
                }}}
        Statement stmt = AmonPackPlugin.mysqllite().getConnection().createStatement();
        ResultSet rs = stmt.executeQuery("select * from SpellTree");
        while (rs.next()){
            SkillPoints.add(new SkillTreeObj(rs.getString(1),rs.getInt(2),rs.getString(3),rs.getString(5),rs.getString(4)));
        }
        stmt.close();
        for (SkillTreeObj p:SkillPoints) {
            System.out.println("Loaded From DB   "+p.getPlayer()+"  "+p.getActSkillPoints() +"  "+p.getSelectedPath() + "    "+ p.getCurrentElement() + "   " + p.getElementsInPossesion());
        }
    }
    public void OpenBindingGui(Player p,String st){
        Inventory menu = Bukkit.createInventory(null, 18, "BindingGui");
        for (int i = 0; i < 9; i++) {
            ItemStack TempItem = new ItemStack(Material.RED_STAINED_GLASS_PANE);
            ItemMeta SKM_item_meta = TempItem.getItemMeta();
            SKM_item_meta.setDisplayName(""+(i+1));
            TempItem.setItemMeta(SKM_item_meta);
            menu.setItem(i, TempItem);
        }
        ItemStack TempItem = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
        ItemMeta SKM_item_meta = TempItem.getItemMeta();
        SKM_item_meta.setDisplayName(st);
        TempItem.setItemMeta(SKM_item_meta);
        menu.setItem(13, TempItem);
        p.openInventory(menu);
    }
    public static List<String>GetDefAbiName(){
        List<String> listdef = new ArrayList<>();
        for (STAbility abi:STAList) {
            if (abi.isdef()){
                listdef.add(abi.getName());
            }
        }
        return listdef;
    }

    public static void OpenElementMenu(Player p){
        Inventory menu = Bukkit.createInventory(null, 18, "ElementMenu");
        ItemStack TempItem = new ItemStack(Material.DIRT);
        ItemMeta SKM_item_meta = TempItem.getItemMeta();
        ItemStack Background = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta Backgroundmeta = Background.getItemMeta();
        Backgroundmeta.setDisplayName(ChatColor.BLACK + "");
        Background.setItemMeta(Backgroundmeta);
        for (int i = 0; i < menu.getSize(); i++) {
            menu.setItem(i, Background);
        }
        for (SkillTreeObj sto : SkillPoints){
            if (sto.getPlayer().equalsIgnoreCase(p.getName())){
                if (sto.getElementsInPossesion().contains("WATER")){
                    TempItem = new ItemStack(Material.WATER_BUCKET);
                    SKM_item_meta.setDisplayName(ChatColor.BLUE+"Woda");
                    TempItem.setItemMeta(SKM_item_meta);
                    menu.setItem(3, TempItem);
                }
                if (sto.getElementsInPossesion().contains("FIRE")){
                    TempItem = new ItemStack(Material.FIRE_CHARGE);
                    SKM_item_meta.setDisplayName(ChatColor.RED+"Ogień");
                    TempItem.setItemMeta(SKM_item_meta);
                    menu.setItem(2, TempItem);
                }
                if (sto.getElementsInPossesion().contains("AIR")){
                    TempItem = new ItemStack(Material.COBWEB);
                    SKM_item_meta.setDisplayName(ChatColor.GRAY+"Powietrze");
                    TempItem.setItemMeta(SKM_item_meta);
                    menu.setItem(5, TempItem);
                }
                if (sto.getElementsInPossesion().contains("EARTH")){
                    TempItem = new ItemStack(Material.DIRT);
                    SKM_item_meta.setDisplayName(ChatColor.GREEN+"Ziemia");
                    TempItem.setItemMeta(SKM_item_meta);
                    menu.setItem(6, TempItem);
                }
            }}
        TempItem = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
        SKM_item_meta.setDisplayName(ChatColor.BLACK+"");
        TempItem.setItemMeta(SKM_item_meta);
        for (SkillTreeObj sto:SkillPoints) {
            if (p.getName().equalsIgnoreCase(sto.getPlayer())){
                switch (sto.getCurrentElement()){
                    case "WATER":
                        menu.setItem(12, TempItem);
                        break;
                    case "AIR":
                        menu.setItem(14, TempItem);
                        break;
                    case "FIRE":
                        menu.setItem(11, TempItem);
                        break;
                    case "EARTH":
                        menu.setItem(15, TempItem);
                        break;
                }}}
        p.openInventory(menu);
    }

    public static void OpenBendingGui(Player p, Element ele){
        BendingPlayer bplayer = BendingPlayer.getBendingPlayer(p);
        if (!bplayer.hasElement(ele)){
            OpenElementMenu(p);
        }else{
        for (SkillTreeObj STO:SkillPoints) {
            if (STO.getPlayer().equalsIgnoreCase(p.getName())){
                Inventory menu = Bukkit.createInventory(null, 54, "BendingGui");
                int i =0;
                for (STAbility STA:STAList) {
                    Ability abi = CoreAbility.getAbility(STA.getName());
                    if (abi != null && (STO.getSelectedPath().contains(STA.getName()) || STA.isdef()) && abi.getElement() != null) {
                    if (SubElementByElement(ele,abi) ||abi.getElement().equals(ele) || abi.getElement().equals(ElementBasedOnSubElement(ele)) || SubElementByElement(ElementBasedOnSubElement(ele),abi)) {
                        ItemStack TempItem = new ItemStack(Material.GREEN_TERRACOTTA);
                        ItemMeta SKM_item_meta = TempItem.getItemMeta();
                        SKM_item_meta.setDisplayName(STA.getName());
                        TempItem.setItemMeta(SKM_item_meta);
                        menu.setItem(i, TempItem);
                        i++;
                    }}}
                for (AssaultDef A: AssaultMenager.listOfAssaultDef) {
                    if (AssaultMethods.InArenaRange(p.getLocation(),A.getArenaLocation(),A.getRange(),A.getRange())){
                        if (A.BonusAbilities.get(p) != null){
                            for (String st:A.BonusAbilities.get(p)) {
                                Ability abi = CoreAbility.getAbility(st);
                                    if (SubElementByElement(ele,abi) ||abi.getElement().equals(ele) || abi.getElement().equals(ElementBasedOnSubElement(ele)) || SubElementByElement(ElementBasedOnSubElement(ele),abi)) {
                                        ItemStack TempItem = new ItemStack(Material.GREEN_TERRACOTTA);
                                        ItemMeta SKM_item_meta = TempItem.getItemMeta();
                                        SKM_item_meta.setDisplayName(st);
                                        TempItem.setItemMeta(SKM_item_meta);
                                        menu.setItem(i, TempItem);
                                        i++;
                                    }}}}}

                ItemStack TempItem = new ItemStack(Material.BARRIER);
                ItemMeta EXIT_item_meta = TempItem.getItemMeta();
                EXIT_item_meta.setDisplayName(ChatColor.RED + "Zamknij");
                TempItem.setItemMeta(EXIT_item_meta);
                menu.setItem(53, TempItem);
                ItemStack SkTree= new ItemStack(Material.CHEST);
                if (ele.equals(Element.AIR) || ElementBasedOnSubElement(ele).equals(Element.AIR)){
                    SkTree = new ItemStack(Material.COBWEB);
                }
                if (ele.equals(Element.EARTH) || ElementBasedOnSubElement(ele).equals(Element.EARTH)){
                    SkTree = new ItemStack(Material.DIRT);
                }
                if (ele.equals(Element.FIRE) || ElementBasedOnSubElement(ele).equals(Element.FIRE)){
                    SkTree = new ItemStack(Material.FIRE_CHARGE);
                }
                if (ele.equals(Element.WATER) || ElementBasedOnSubElement(ele).equals(Element.WATER)){
                    SkTree = new ItemStack(Material.WATER_BUCKET);
                }
                ItemMeta SkTreemeta = SkTree.getItemMeta();
                SkTreemeta.setDisplayName(ChatColor.RED + "Twoje Punkty: " + STO.getActSkillPoints());
                SkTree.setItemMeta(SkTreemeta);
                menu.setItem(52, SkTree);
                p.openInventory(menu);
                break;
            }}}
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

    public static void OpenSkillTreeMenuByElement(Player p, Element ele, int page){
        for (SkillTreeObj STO:SkillPoints) {
            if (STO.getPlayer().equalsIgnoreCase(p.getName())){
                STO.setCurrentPage(page);
                Inventory menu = Bukkit.createInventory(null, 54, "SkillTreeMenu");
                ItemStack Background = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
                ItemMeta Backgroundmeta = Background.getItemMeta();
                Backgroundmeta.setDisplayName(ChatColor.BLACK + "");
                Background.setItemMeta(Backgroundmeta);
                for (int i = 0; i < menu.getSize()-1; i++) {
                    menu.setItem(i, Background);
                }
                for (STAbility STA : STAList){
                    int tempplace = STA.getPlace()-(54*page);
                    if (tempplace>=0 && tempplace<53){
                    Ability tempabi = CoreAbility.getAbility(STA.getName());
                if (tempabi != null && STA.getElement() != null) {
                    if (SubElementByElement(ele,tempabi) ||STA.getElement().equals(ele) ||
                            SubElementByElement(ElementBasedOnSubElement(ele),tempabi) ||
                            STA.getElement().equals(ElementBasedOnSubElement(ele))) {
                            if (STO.getSelectedPath().contains(STA.getName()) || STA.isdef()){
                                ItemStack TempItem = new ItemStack(Material.GREEN_TERRACOTTA);
                                ItemMeta SKM_item_meta = TempItem.getItemMeta();
                                SKM_item_meta.setDisplayName(STA.getName());
                                TempItem.setItemMeta(SKM_item_meta);
                                menu.setItem(tempplace, TempItem);
                            }else{
                                if (STO.getActSkillPoints() >= STA.getCost() && (STO.getSelectedPath().containsAll(STA.getListOfPreAbility()) || STA.getListOfPreAbility().size()==0)) {
                                    ItemStack TempItem = new ItemStack(Material.ORANGE_TERRACOTTA);
                                    ItemMeta SKM_item_meta = TempItem.getItemMeta();
                                    SKM_item_meta.setDisplayName(STA.getName());
                                    List<String> modifiedList = new ArrayList<>();
                                    modifiedList.add("Koszt: "+STA.getCost());
                                    for (String st:STA.getListOfPreAbility()) {
                                        modifiedList.add("Wymagane: "+st);
                                    }
                                    SKM_item_meta.setLore(modifiedList);
                                    TempItem.setItemMeta(SKM_item_meta);
                                    menu.setItem(tempplace, TempItem);
                                }else{
                                ItemStack TempItem = new ItemStack(Material.RED_TERRACOTTA);
                                ItemMeta SKM_item_meta = TempItem.getItemMeta();
                                SKM_item_meta.setDisplayName(STA.getName());
                                    List<String> modifiedList = new ArrayList<>();
                                    modifiedList.add("Koszt: "+STA.getCost());
                                    for (String st:STA.getListOfPreAbility()) {
                                        modifiedList.add("Wymagane: "+st);
                                    }
                                    SKM_item_meta.setLore(modifiedList);
                                    TempItem.setItemMeta(SKM_item_meta);
                                menu.setItem(tempplace, TempItem);
                            }}}}
                }
                for (Integer i:PathDecoration.get(ele)) {
                    int tempPatDec = i-(54*page);
                    if (tempPatDec>=0 && tempPatDec<53){
                    ItemStack TempItem = new ItemStack(Material.GLASS_PANE);
                    ItemMeta SkTreemeta = TempItem.getItemMeta();
                    SkTreemeta.setDisplayName(ChatColor.BLACK + "");
                    TempItem.setItemMeta(SkTreemeta);
                    menu.setItem(tempPatDec, TempItem);
                }}}
                ItemStack EXIT = new ItemStack(Material.BARRIER);
                ItemMeta EXIT_item_meta = EXIT.getItemMeta();
                EXIT_item_meta.setDisplayName(ChatColor.RED + "Twoje Punkty: " + STO.getActSkillPoints());
                EXIT.setItemMeta(EXIT_item_meta);
                menu.setItem(53, EXIT);
                ItemStack SkTree= new ItemStack(Material.CHEST);
                ItemMeta SkTreemeta = SkTree.getItemMeta();
                SkTreemeta.setDisplayName(ChatColor.RED + "Powrot");
                SkTree.setItemMeta(SkTreemeta);
                menu.setItem(44, SkTree);

                ItemStack GoDown= new ItemStack(Material.OAK_SIGN);
                ItemMeta GoDownMeta = GoDown.getItemMeta();
                GoDownMeta.setDisplayName(ChatColor.RED + "/\\" +STO.getCurrentPage());
                GoDown.setItemMeta(GoDownMeta);
                menu.setItem(26, GoDown);

                ItemStack GoUp= new ItemStack(Material.DARK_OAK_SIGN);
                ItemMeta GoUpMeta = GoUp.getItemMeta();
                GoUpMeta.setDisplayName(ChatColor.RED + "\\/ "+STO.getCurrentPage());
                GoUp.setItemMeta(GoUpMeta);
                menu.setItem(35, GoUp);

                ItemStack Ele= new ItemStack(Material.COMPASS);
                ItemMeta EleMeta = Ele.getItemMeta();
                EleMeta.setDisplayName(ele.getName());
                Ele.setItemMeta(EleMeta);
                menu.setItem(17, Ele);
                p.openInventory(menu);
                break;
            }}
    }



    public static void OpenSkillTreeMenu(Player p){
        for (SkillTreeObj STO:SkillPoints) {
            if (STO.getPlayer().equalsIgnoreCase(p.getName())){
                Inventory menu = Bukkit.createInventory(null, 54, "SkillTreeMenu");
                int place = 0;
                for (int i = 0; i < menu.getSize()-1; i++) {
                    if (i<AbilitiesList.size()){
                        if (STO.getSelectedPath().contains(AbilitiesList.get(i))){
                            if (CoreAbility.getAbility(AbilitiesList.get(i)) != null) {
                                ItemStack SKM = new ItemStack(Material.GREEN_STAINED_GLASS);
                                ItemMeta SKM_item_meta = SKM.getItemMeta();
                                SKM_item_meta.setDisplayName(CoreAbility.getAbility(AbilitiesList.get(i)).getElement().getColor() + AbilitiesList.get(i));
                                SKM.setItemMeta(SKM_item_meta);
                                menu.setItem(place, SKM);
                                place++;
                            }}else{
                        ItemStack SKM = new ItemStack(Material.BLACK_STAINED_GLASS);
                        ItemMeta SKM_item_meta = SKM.getItemMeta();
                        SKM_item_meta.setDisplayName(AbilitiesList.get(i));
                        SKM.setItemMeta(SKM_item_meta);
                        menu.setItem(place, SKM);
                        place++;
                    }}}
                ItemStack EXIT = new ItemStack(Material.BARRIER);
                ItemMeta EXIT_item_meta = EXIT.getItemMeta();
                EXIT_item_meta.setDisplayName(ChatColor.RED + "Twoje Punkty: " + STO.getActSkillPoints());
                EXIT.setItemMeta(EXIT_item_meta);
                menu.setItem(53, EXIT);
                p.openInventory(menu);
                break;
            }}
    }

    void changeele(Player p, BendingPlayer bPlayer, String st) throws SQLException {
        if (!Multibend){
            for (int i = 0; i <= 9; i++) {
                BendingBoardManager.getBoard(p).get().clearSlot(i);
                bPlayer.getAbilities().remove(i);
            }
        }
        String PName = null;
        for (SkillTreeObj sto : SkillPoints){
            if (sto.getPlayer().equalsIgnoreCase(p.getName())){
                //int Points = sto.getActSkillPoints() + sto.CountCost(STAList);
                SetPathAndPoints(p.getName(),sto.getActSkillPoints(),sto.getSelectedPathAsString(),st);
                PName = sto.getPlayer();
            }break;
        }
        if (PName == null){
            SetPathAndPoints(p.getName(),0,"",st);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) throws SQLException {
        if (event.getInventory().getHolder() == null && event.getCurrentItem() != null) {
        if (event.getView().getTitle().equals("ElementMenu")) {
            ItemStack clickedItem = event.getCurrentItem();
            Player p = (Player) event.getWhoClicked();
            BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(p);
            event.setCancelled(true);
            switch (Objects.requireNonNull(clickedItem).getType()) {
                case FIRE_CHARGE:
                    if (Multibend){
                        bPlayer.addElement(Element.FIRE);
                    }else{
                        bPlayer.setElement(Element.FIRE);
                    }
                    bPlayer.saveElements();
                    bPlayer.saveSubElements();
                    changeele(p,bPlayer,"FIRE");
                    OpenBendingGui(p,Element.FIRE);
                    p.sendMessage(ChatColor.RED+"Wybrałeś Żywioł: Ogień!");
                    break;
                case WATER_BUCKET:
                    if (Multibend){
                        bPlayer.addElement(Element.WATER);
                    }else{
                        bPlayer.setElement(Element.WATER);
                    }
                    bPlayer.saveElements();
                    bPlayer.saveSubElements();
                    changeele(p,bPlayer,"WATER");
                    OpenBendingGui(p,Element.WATER);
                    p.sendMessage(ChatColor.BLUE+"Wybrałeś Żywioł: Woda!");
                    break;
                case COBWEB:
                    if (Multibend){
                        bPlayer.addElement(Element.AIR);
                    }else{
                        bPlayer.setElement(Element.AIR);
                    }
                    bPlayer.saveElements();
                    bPlayer.saveSubElements();
                    changeele(p,bPlayer,"AIR");
                    OpenBendingGui(p,Element.AIR);
                    p.sendMessage(ChatColor.GRAY+"Wybrałeś Żywioł: Powietrze!");
                    break;
                case DIRT:
                    if (Multibend){
                        bPlayer.addElement(Element.EARTH);
                    }else{
                        bPlayer.setElement(Element.EARTH);
                    }
                    bPlayer.saveElements();
                    bPlayer.saveSubElements();
                    changeele(p,bPlayer,"EARTH");
                    OpenBendingGui(p,Element.EARTH);
                    p.sendMessage(ChatColor.GREEN+"Wybrałeś Żywioł: Ziemia!");
                    break;
        }}
        if (event.getView().getTitle().equals("SkillTreeMenu")) {
            ItemStack clickedItem = event.getCurrentItem();
            Player p = (Player) event.getWhoClicked();
            event.setCancelled(true);
            if (clickedItem != null){
            if (clickedItem.getType().equals(Material.BARRIER)) {
                BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(p);
                bPlayer.removeUnusableAbilities();
                for (int i = 0; i <= 9; i++) {
                    BendingBoardManager.getBoard(p).get().clearSlot(i);
                    bPlayer.getAbilities().remove(i);
                }
                for (SkillTreeObj sto : SkillPoints){
                    if (sto.getPlayer().equalsIgnoreCase(p.getName())){
                        Element ele = Element.getElement(event.getClickedInventory().getItem(17).getItemMeta().getDisplayName());
                        SetPathAndPoints(p.getName(),sto.getActSkillPoints()+sto.CountCostByElement(STAList,ele),sto.PathRemoveElement(ele),sto.getCurrentElement());
                    }}
                OpenSkillTreeMenuByElement(p,Element.getElement(event.getClickedInventory().getItem(17).getItemMeta().getDisplayName()),0);
            }
            if (clickedItem.getType().equals(Material.CHEST)) {
                OpenBendingGui(p,Element.getElement(event.getClickedInventory().getItem(17).getItemMeta().getDisplayName()));
            }
            if (clickedItem.getType().equals(Material.OAK_SIGN)) {
                for (SkillTreeObj sto : SkillPoints){
                    if (sto.getPlayer().equalsIgnoreCase(p.getName())){
                        String e = event.getClickedInventory().getItem(17).getItemMeta().getDisplayName();
                        if (sto.getCurrentPage()>0){
                            OpenSkillTreeMenuByElement(p,Element.getElement(e),sto.getCurrentPage()-1);
                        }}}
            }
            if (clickedItem.getType().equals(Material.DARK_OAK_SIGN)) {
                for (SkillTreeObj sto : SkillPoints){
                    if (sto.getPlayer().equalsIgnoreCase(p.getName())){
                        String e = event.getClickedInventory().getItem(17).getItemMeta().getDisplayName();
                        if (sto.getCurrentPage()<(MaxForElement.get(e))/54){
                        OpenSkillTreeMenuByElement(p,Element.getElement(event.getClickedInventory().getItem(17).getItemMeta().getDisplayName()),sto.getCurrentPage()+1);
                    }}break;
                }}
            for (STAbility STA:STAList) {
                if (STA.getName().equalsIgnoreCase(clickedItem.getItemMeta().getDisplayName())){
                    if (clickedItem.getType().equals(Material.ORANGE_TERRACOTTA)) {
                        for (SkillTreeObj sto : SkillPoints){
                            if (sto.getPlayer().equalsIgnoreCase(p.getName())){
                                if (sto.getActSkillPoints() >= STA.getCost()) {
                                    if (sto.getSelectedPath().containsAll(STA.getListOfPreAbility()) || STA.getListOfPreAbility().size()==0) {
                                        SetPathAndPoints(p.getName(), sto.getActSkillPoints() - STA.getCost(), sto.getSelectedPathAsString() + "," + STA.getName() + ",",sto.getCurrentElement());
                                        OpenSkillTreeMenuByElement(p,STA.getElement(),sto.getCurrentPage());
                                    }}break;
                            }}}}}}
        }
        if (event.getView().getTitle().equals("BendingGui")) {
            ItemStack clickedItem = event.getCurrentItem();
            Player p = (Player) event.getWhoClicked();
            event.setCancelled(true);
            if (clickedItem != null && clickedItem.getType().equals(Material.BARRIER)) {
                p.closeInventory();
            }
            if (clickedItem != null && clickedItem.getType().equals(Material.FIRE_CHARGE)) {
                OpenSkillTreeMenuByElement(p,Element.FIRE,0);
            }
            if (clickedItem != null && clickedItem.getType().equals(Material.WATER_BUCKET)) {
                OpenSkillTreeMenuByElement(p,Element.WATER,0);
            }
            if (clickedItem != null && clickedItem.getType().equals(Material.COBWEB)) {
                OpenSkillTreeMenuByElement(p,Element.AIR,0);
            }
            if (clickedItem != null && clickedItem.getType().equals(Material.DIRT)) {
                OpenSkillTreeMenuByElement(p,Element.EARTH,0);
            }
            if (clickedItem != null && clickedItem.getType().equals(Material.GREEN_TERRACOTTA)) {
                OpenBindingGui(p,clickedItem.getItemMeta().getDisplayName());
            }}
        if (event.getView().getTitle().equals("BindingGui")) {
            ItemStack clickedItem = event.getCurrentItem();
            Player p = (Player) event.getWhoClicked();
            event.setCancelled(true);
            if (clickedItem != null && clickedItem.getType().equals(Material.RED_STAINED_GLASS_PANE)) {
                BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(p);
                bPlayer.bindAbility(Objects.requireNonNull(event.getClickedInventory().getItem(13).getItemMeta()).getDisplayName(), Integer.parseInt(clickedItem.getItemMeta().getDisplayName()));
                bPlayer.saveAbility(Objects.requireNonNull(event.getClickedInventory().getItem(13)).getItemMeta().getDisplayName(), Integer.parseInt(clickedItem.getItemMeta().getDisplayName()));
                OpenBendingGui(p,CoreAbility.getAbility(event.getClickedInventory().getItem(13).getItemMeta().getDisplayName()).getElement());
            }}}}



    public static int ActPoints(Player player) throws SQLException {
        int actualSkillPoints = 0;
        Statement stmt = AmonPackPlugin.mysqllite().getConnection().createStatement();
        ResultSet rs = stmt.executeQuery("select SkillPoint from SpellTree where Player='" + player.getName()+"'");
        actualSkillPoints = rs.getInt(1);
        return actualSkillPoints;
    }
    public static void AddPoints(String player, String i) throws SQLException {
        Player p = Bukkit.getPlayer(player);
        Statement stmt = AmonPackPlugin.mysqllite().getConnection().createStatement();
        ResultSet rs = stmt.executeQuery("select * from SpellTree where Player='" + p.getName()+"'");
        if (!rs.next()) {
            ExecuteQuery("INSERT INTO SpellTree (Player,SkillPoint) VALUES ('" + p.getName() +"',"+ i +");");
        } else {
            ExecuteQuery("UPDATE SpellTree SET SkillPoint = '" + (rs.getInt(2)+Integer.parseInt(i)) + "' WHERE Player = '"+ p.getName()+"';");
        }
        stmt.close();
    }
}
*/
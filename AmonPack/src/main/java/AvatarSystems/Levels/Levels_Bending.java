package AvatarSystems.Levels;

import UtilObjects.Skills.SkillTree_Ability;
import com.projectkorra.projectkorra.Element;
import dev.lone.itemsadder.api.FontImages.FontImageWrapper;
import dev.lone.itemsadder.api.FontImages.TexturedInventoryWrapper;
import methods_plugins.AmonPackPlugin;
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

import static AvatarSystems.Levels.PlayerLevelMenager.*;
import static methods_plugins.AmonPackPlugin.FastEasyStack;

public class Levels_Bending {
    private FileConfiguration LevelConfig;
    private FileConfiguration SkillTreeConfig;
    private List<ElementTree> ListOfElements = new ArrayList<>();
    private List<PlayerBendingBranch> PlayersBending = new ArrayList<>();

    public Levels_Bending() {
        LoadData();
    }

    public void AddNewBranch(PlayerBendingBranch branch){
        PlayersBending.add(branch);
        try {
            branch.SaveInDatabaes();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    public void OpenBindingMenu(String name, String AbilityName){
        PlayerBendingBranch branch= AmonPackPlugin.levelsBending.GetBranchByPlayerName(name);
        if(branch==null)return;
        Element element=branch.getCurrentElement();
        TexturedInventoryWrapper inventory = new TexturedInventoryWrapper(BindingAbilitiesMenu,
                BindingAbilitiesMenu.getSize(), BindingAbilitiesMenu.getTitle(), new FontImageWrapper("amon:bending_skills_binding")
        );
        Inventory inv = inventory.getInternal();
        int modelid=SkillTreeConfig.getInt("AmonPack.Menu." + element.getName().toString().toLowerCase() + ".Green");
        inv.setItem(4, FastEasyStack(Material.PAPER,AbilityName,modelid));
        int baseint = 10062;
        for (int i = 9; i < 18; i++) {
            inv.setItem(i, FastEasyStack(Material.PAPER,""+(i-8),baseint));
            baseint++;
        }
        inv.setItem(8, FastEasyStack(Material.PAPER, ChatColor.RED+"Zamknij",10036));
        inventory.showInventory(Bukkit.getPlayer(name));
    }
    public void OpenSkillTreeMenuByElement(Player p, int page){

        PlayerBendingBranch playersBranch = GetBranchByPlayerName(p.getName());
        if(playersBranch==null)return;

        Element element = playersBranch.getCurrentElement();
        ElementTree SelectedElement = GetElement(element);

        String ElementName = element.getName().toLowerCase();
        //                Holder1.getSize(), Holder1.getTitle(), new FontImageWrapper("amon:first_gui")
        TexturedInventoryWrapper inventory = new TexturedInventoryWrapper(BendingSkillTree,
                BendingSkillTree.getSize(), BendingSkillTree.getTitle(), new FontImageWrapper("amon:bending_skills_tree_"+ElementName)
        );

        Inventory inv = inventory.getInternal();
        playersBranch.setCurrentPage(page);
        for (SkillTree_Ability STA:SelectedElement.getAbilities()) {
            int tempplace = STA.getPlace()-(54*page);
            if (tempplace>=0 && tempplace<53){

                Material material = Material.getMaterial(Objects.requireNonNull(SkillTreeConfig.getString("AmonPack.Menu." + SelectedElement.element.getName().toString().toLowerCase() + ".Material")));
                ItemStack item = FastEasyStack(material,STA.getName());
                ItemMeta meta = item.getItemMeta();
                int modelid;
                if ((playersBranch.getUnlockedAbilities().contains(STA.getName())|| playersBranch.getTemporaryAbilities().contains(STA.getName()) || STA.isdef())){
                    modelid = SkillTreeConfig.getInt("AmonPack.Menu." + SelectedElement.element.getName().toString().toLowerCase() + ".Green");


                } else if (playersBranch.GetPoints(element) >= STA.getCost() && (new HashSet<>(playersBranch.getUnlockedAbilities()).containsAll(STA.getListOfPreAbility()) || STA.getListOfPreAbility().isEmpty())) {
                    modelid = SkillTreeConfig.getInt("AmonPack.Menu." + SelectedElement.element.getName().toString().toLowerCase() + ".Orange");

                    List<String> modifiedList = new ArrayList<>(Collections.singleton("Koszt: " + STA.getCost()));
                    for (String st:STA.getListOfPreAbility()) {
                        modifiedList.add("Wymagane: "+st);
                    }
                    meta.setLore(modifiedList);

                }else{
                    modelid = SkillTreeConfig.getInt("AmonPack.Menu." + SelectedElement.element.getName().toString().toLowerCase() + ".Red");

                    List<String> modifiedList = new ArrayList<>(Collections.singleton("Koszt: " + STA.getCost()));
                    for (String st:STA.getListOfPreAbility()) {
                        modifiedList.add("Wymagane: "+st);
                    }
                    meta.setLore(modifiedList);

                }
                meta.setCustomModelData(modelid);
                item.setItemMeta(meta);
                inv.setItem(tempplace, item);
            }}

        Material PathMaterial = Material.getMaterial(Objects.requireNonNull(SkillTreeConfig.getString("AmonPack.Menu." + SelectedElement.element.getName().toString().toLowerCase() + ".Material")));
        int ModelId = SkillTreeConfig.getInt("AmonPack.Menu." + SelectedElement.element.getName().toString().toLowerCase() + ".Path");

            for (Integer i: SelectedElement.getPathDecoration()) {
                int tempPatDec = i-(54*page);
                if (tempPatDec>=0 && tempPatDec<53){
                    inv.setItem(tempPatDec, FastEasyStack(PathMaterial,ChatColor.BLACK + "",ModelId));
                }}

        inv.setItem(44, FastEasyStack(Material.CHEST,ChatColor.RED + "Twoje Punkty: " + playersBranch.GetPoints(element)));
        inv.setItem(53, FastEasyStack(Material.PAPER,ChatColor.RED + "Powrot",10036));
        inv.setItem(26, FastEasyStack(Material.PAPER,ChatColor.RED + "/\\",10071));
        inv.setItem(35, FastEasyStack(Material.PAPER,ChatColor.RED + "\\/",10072));
        inventory.showInventory(Bukkit.getPlayer(p.getName()));
    }
    public void OpenBendingSkillMenu(String name){
        TexturedInventoryWrapper inventory = new TexturedInventoryWrapper(BendingSkillMenu,
                BendingSkillMenu.getSize(), BendingSkillMenu.getTitle(), new FontImageWrapper("amon:bending_abilities_list")
        );
        Inventory inv = inventory.getInternal();

        PlayerBendingBranch playersBranch = GetBranchByPlayerName(name);
        if(playersBranch==null)return;
        Element element = playersBranch.getCurrentElement();
        ElementTree SelectedElement = GetElement(element);
        int i =0;
        List<SkillTree_Ability> UsabelAbilities = SelectedElement.getAbilities().stream()
                .filter(sta -> sta.isdef()||playersBranch.getUnlockedAbilities().contains(sta.getName())|| playersBranch.getTemporaryAbilities().contains(sta.getName()))
                .collect(Collectors.toList());
        for (SkillTree_Ability STA:UsabelAbilities) {
            if(!STA.isUpgrade()){
                String elementname = element.getName().toLowerCase();
                Material material = Material.getMaterial(SkillTreeConfig.getString("AmonPack.Menu." + elementname + ".Material"));
                int modelid = SkillTreeConfig.getInt("AmonPack.Menu." + elementname + ".Green");
                ItemStack item = FastEasyStack(material,STA.getName());
                if(modelid>0){
                    ItemMeta meta = item.getItemMeta();
                    meta.setCustomModelData(modelid);
                    item.setItemMeta(meta);
                }
                inv.setItem(i, item);
                i++;
            }}
        ItemStack CloseButton = FastEasyStack(Material.PAPER, ChatColor.RED+"Zamknij");
        ItemMeta CloseMeta = CloseButton.getItemMeta();
        CloseMeta.setCustomModelData(10036);
        CloseButton.setItemMeta(CloseMeta);
        inv.setItem(53, CloseButton);
        inv.setItem(45, FastEasyStack(Material.CHEST, ChatColor.DARK_PURPLE+"Drzewko Magii"));
        inventory.showInventory(Bukkit.getPlayer(name));
    }
    public void LoadData(){
        LevelConfig= AmonPackPlugin.getLevelConfig();
        SkillTreeConfig= AmonPackPlugin.getSkillTreeConfig();
        DefineAvailableAbilities();
        try {
            AddPlayerFromDBToListOnEnable();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }//Reload Danych
    private void AddPlayerFromDBToListOnEnable() throws SQLException {
        PlayersBending=new ArrayList<>();
        Statement stmt = AmonPackPlugin.mysqllite().getConnection().createStatement();
        ResultSet rs = stmt.executeQuery("select * from BendingTree");
        while (rs.next()) {
            String name = rs.getString(1);
            int AirP = rs.getInt(2);
            int FireP = rs.getInt(3);
            int WaterP = rs.getInt(4);
            int EarthP = rs.getInt(5);
            String UnlockedAbilities = rs.getString(6);
            String CurrentElement = rs.getString(7);
            String AllElements = rs.getString(8);
            List<String> unlockedAbilities = UnlockedAbilities == null || UnlockedAbilities.isEmpty() ? new ArrayList<>() : Arrays.asList(UnlockedAbilities.split(","));
            Element currentElement = CurrentElement == null ? null : Element.getElement(CurrentElement);
            List<Element> allElements = AllElements == null || AllElements.isEmpty() ? new ArrayList<>() : Arrays.stream(AllElements.split(",")).map(Element::getElement).filter(Objects::nonNull).collect(Collectors.toList());

            PlayersBending.add(new PlayerBendingBranch(AirP,currentElement,EarthP,allElements,FireP,name,unlockedAbilities,WaterP));
        }
        stmt.close();
    }//Przy reloadzie pobierz wszystkich graczy z DB
    private void DefineAvailableAbilities(){

        for(String Element : Objects.requireNonNull(SkillTreeConfig.getConfigurationSection("AmonPack.Tree")).getKeys(false)) {

            com.projectkorra.projectkorra.Element pk_element= com.projectkorra.projectkorra.Element.getElement(Element);
            int MaxPlace = 0;
            List<SkillTree_Ability> ElementAbilities = new ArrayList<>();
            List<Integer> PathDecoration = new ArrayList<>(SkillTreeConfig.getIntegerList("AmonPack.Tree."+Element+".PathDecoration"));

            for(String Ability : Objects.requireNonNull(SkillTreeConfig.getConfigurationSection("AmonPack.Tree."+Element)).getKeys(false)) {
                if(!Ability.equalsIgnoreCase("PathDecoration")){
                int Cost = SkillTreeConfig.getInt("AmonPack.Tree."+Element+"."+Ability+".Cost");
                int Place = SkillTreeConfig.getInt("AmonPack.Tree."+Element+"."+Ability+".Place");
                List<String> ReqAbi = SkillTreeConfig.getStringList("AmonPack.Tree."+Element+"."+Ability+".ReqAbilities");
                SkillTree_Ability AbilityObject = new SkillTree_Ability(pk_element,Ability,Cost,ReqAbi,Place,Cost == 0);
                if(SkillTreeConfig.getBoolean("AmonPack.Tree."+Element+"."+Ability+".IsAbilityUpgrade")){
                    AbilityObject.setUpgrade(true);
                }
                ElementAbilities.add(AbilityObject);
                if (MaxPlace < Place){
                    MaxPlace=Place;
                }}}
            ListOfElements.add(new ElementTree(ElementAbilities,pk_element,PathDecoration,MaxPlace));
        }
    }//przy reload ogarnij wszystkie dostepne skille w drzewku
    public PlayerBendingBranch GetBranchByPlayerName(String name){
        PlayerBendingBranch playerbranch = PlayersBending.stream().filter(branch -> branch.getName().equals(name)).findFirst().orElse(null);
        if(playerbranch==null){
        }
        return playerbranch;
    }
    public ElementTree GetElement(Element element){
        return ListOfElements.stream().filter(ele->ele.element.equals(element)).findFirst().orElse(null);
    }
}

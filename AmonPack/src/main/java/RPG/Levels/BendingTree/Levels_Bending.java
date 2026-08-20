package RPG.Levels.BendingTree;

import RPG.Levels.BendingTree.SkillTree_Ability;
import com.projectkorra.projectkorra.Element;
import dev.lone.itemsadder.api.FontImages.FontImageWrapper;
import dev.lone.itemsadder.api.FontImages.TexturedInventoryWrapper;
import Plugin.AmonPackPlugin;
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

import static RPG.Levels.PlayerLevelMenager.*;
import static Plugin.AmonPackPlugin.FastEasyStack;

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
                BindingAbilitiesMenu.getSize(), BindingAbilitiesMenu.getTitle(), new FontImageWrapper("amonpack:bending_skills_binding")
        );
        Inventory inv = inventory.getInternal();
        int modelid=SkillTreeConfig.getInt("AmonPack.Menu." + element.getName().toString().toLowerCase() + ".Green");
        inv.setItem(4, FastEasyStack(Material.PAPER,AbilityName,modelid));

        boolean isSpecial = SkillTreeConfig.getBoolean("AmonPack.Tree." + element.getName() + "." + AbilityName + ".IsSpecialBindAbility", false);
        if (isSpecial) {
            String curSwap = branch.getSwapAbility();
            String swapLore = curSwap.equalsIgnoreCase(AbilityName) ? ChatColor.GREEN + "[PRZYPISANO DO SWAP (F)]" : (curSwap.isEmpty() ? ChatColor.GRAY + "Aktualnie: Brak" : ChatColor.GRAY + "Aktualnie: " + curSwap);
            inv.setItem(0, AmonPackPlugin.FastEasyStackWithLoreModelData(Material.PAPER, ChatColor.GOLD + "Slot SWAP (F)", Arrays.asList(ChatColor.YELLOW + "Kliknij, aby przypisać ten skill do F", swapLore), 10071));

            String curAdv = branch.getDropAbility();
            String advLore = curAdv.equalsIgnoreCase(AbilityName) ? ChatColor.GREEN + "[PRZYPISANO DO OSIĄGNIĘĆ (L)]" : (curAdv.isEmpty() ? ChatColor.GRAY + "Aktualnie: Brak" : ChatColor.GRAY + "Aktualnie: " + curAdv);
            inv.setItem(1, AmonPackPlugin.FastEasyStackWithLoreModelData(Material.PAPER, ChatColor.AQUA + "Slot OSIĄGNIĘĆ (L)", Arrays.asList(ChatColor.YELLOW + "Kliknij, aby przypisać ten skill do L", advLore), 10072));
        } else {
            inv.setItem(0, null);
            inv.setItem(1, null);
            int baseint = 10062;
            for (int i = 9; i < 18; i++) {
                inv.setItem(i, FastEasyStack(Material.PAPER,""+(i-8),baseint));
                baseint++;
            }
        }
        inv.setItem(8, FastEasyStack(Material.PAPER, ChatColor.RED+"Zamknij",10013));
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
                BendingSkillTree.getSize(), BendingSkillTree.getTitle(), new FontImageWrapper("amonpack:bending_skills_tree_"+ElementName)
        );

        Inventory inv = inventory.getInternal();
        playersBranch.setCurrentPage(page);
        for (SkillTree_Ability STA:SelectedElement.getAbilities()) {
            if (STA.isSkillUpgrade()) {
                continue; // Sub-menu skill upgrades are not placed on the main tree grid
            }
            int tempplace = STA.getPlace()-(54*page);
            if (tempplace>=0 && tempplace<53){

                Material material = Material.getMaterial(Objects.requireNonNull(SkillTreeConfig.getString("AmonPack.Menu." + SelectedElement.element.getName().toString().toLowerCase() + ".Material")));
                ItemStack item = FastEasyStack(material,STA.getName());
                ItemMeta meta = item.getItemMeta();
                int modelid;
                List<String> locks = STA.getLockAbilities();
                if ((playersBranch.getUnlockedAbilities().contains(STA.getName())|| playersBranch.getTemporaryAbilities().contains(STA.getName()) || STA.isdef())){
                    modelid = SkillTreeConfig.getInt("AmonPack.Menu." + SelectedElement.element.getName().toString().toLowerCase() + ".Green");

                    List<String> modifiedList = new ArrayList<>();
                    modifiedList.add(ChatColor.GREEN + "ODBLOKOWANO");
                    if (locks != null && !locks.isEmpty()) {
                        modifiedList.add(ChatColor.DARK_RED + "✖ Zablokowano: " + String.join(", ", locks));
                    }
                    modifiedList.addAll(getFormattedDescription(SelectedElement.element.getName(), STA.getName()));
                    if (SelectedElement.hasSkillUpgrades(STA.getName())) {
                        modifiedList.add(" ");
                        modifiedList.add(ChatColor.GOLD + "▶ Kliknij LPM, aby otworzyć ulepszenia tej umiejętności!");
                    }
                    meta.setLore(modifiedList);
                } else if (playersBranch.GetPoints(element) >= STA.getCost() && (new HashSet<>(playersBranch.getUnlockedAbilities()).containsAll(STA.getListOfPreAbility()) || STA.getListOfPreAbility().isEmpty())) {
                    boolean blockedByOther = false;
                    for (SkillTree_Ability other : SelectedElement.getAbilities()) {
                        if (playersBranch.getUnlockedAbilities().contains(other.getName()) && other.getLockAbilities() != null && other.getLockAbilities().contains(STA.getName())) {
                            blockedByOther = true;
                            break;
                        }
                    }
                    if (blockedByOther) {
                        modelid = SkillTreeConfig.getInt("AmonPack.Menu." + SelectedElement.element.getName().toString().toLowerCase() + ".Red");
                        List<String> modifiedList = new ArrayList<>();
                        modifiedList.add(ChatColor.RED + "✖ ZABLOKOWANE PRZEZ INNY SKILL");
                        modifiedList.addAll(getFormattedDescription(SelectedElement.element.getName(), STA.getName()));
                        meta.setLore(modifiedList);
                    } else {
                        modelid = SkillTreeConfig.getInt("AmonPack.Menu." + SelectedElement.element.getName().toString().toLowerCase() + ".Orange");

                        List<String> modifiedList = new ArrayList<>(Collections.singleton("Koszt: " + STA.getCost()));
                        for (String st:STA.getListOfPreAbility()) {
                            modifiedList.add("Wymagane: "+st);
                        }
                        if (locks != null && !locks.isEmpty()) {
                            modifiedList.add(ChatColor.RED + "⚠ Zablokuje: " + String.join(", ", locks));
                        }
                        modifiedList.addAll(getFormattedDescription(SelectedElement.element.getName(), STA.getName()));
                        meta.setLore(modifiedList);
                    }
                }else{
                    modelid = SkillTreeConfig.getInt("AmonPack.Menu." + SelectedElement.element.getName().toString().toLowerCase() + ".Red");

                    List<String> modifiedList = new ArrayList<>(Collections.singleton("Koszt: " + STA.getCost()));
                    for (String st:STA.getListOfPreAbility()) {
                        modifiedList.add("Wymagane: "+st);
                    }
                    if (locks != null && !locks.isEmpty()) {
                        modifiedList.add(ChatColor.RED + "⚠ Zablokuje: " + String.join(", ", locks));
                    }
                    modifiedList.addAll(getFormattedDescription(SelectedElement.element.getName(), STA.getName()));
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
        inv.setItem(53, FastEasyStack(Material.PAPER,ChatColor.RED + "Powrot",10013));
        inv.setItem(26, FastEasyStack(Material.PAPER,ChatColor.RED + "/\\",10011));
        inv.setItem(35, FastEasyStack(Material.PAPER,ChatColor.RED + "\\/",10012));
        inventory.showInventory(Bukkit.getPlayer(p.getName()));
    }
    public void OpenBendingSkillMenu(String name){
        TexturedInventoryWrapper inventory = new TexturedInventoryWrapper(BendingSkillMenu,
                BendingSkillMenu.getSize(), BendingSkillMenu.getTitle(), new FontImageWrapper("amonpack:bending_abilities_list")
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

    public void OpenSkillUpgradeMenu(Player p, String skillName) {
        PlayerBendingBranch playersBranch = GetBranchByPlayerName(p.getName());
        if (playersBranch == null) return;
        Element element = playersBranch.getCurrentElement();
        ElementTree SelectedElement = GetElement(element);
        if (SelectedElement == null) return;

        SkillUpgradeMenuHolder holder = new SkillUpgradeMenuHolder(54, ChatColor.DARK_PURPLE + "Ulepszenia: " + skillName, skillName);
        Inventory inv = holder.getInventory();

        String elName = element.getName().toLowerCase();
        Material blankMat = Material.GRAY_STAINED_GLASS_PANE;
        ItemStack blank = FastEasyStack(blankMat, " ");
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, blank);
        }

        // Header in slot 4
        Material skillMat = Material.getMaterial(SkillTreeConfig.getString("AmonPack.Menu." + elName + ".Material", "PAPER"));
        int baseGreenModel = SkillTreeConfig.getInt("AmonPack.Menu." + elName + ".Green", 10013);
        ItemStack headerItem = FastEasyStack(skillMat != null ? skillMat : Material.PAPER, ChatColor.GOLD + "★ " + skillName + " ★", baseGreenModel);
        ItemMeta hMeta = headerItem.getItemMeta();
        if (hMeta != null) {
            List<String> hLore = new ArrayList<>();
            hLore.add(ChatColor.GREEN + "Główna umiejętność");
            hLore.addAll(getFormattedDescription(element.getName(), skillName));
            hMeta.setLore(hLore);
            headerItem.setItemMeta(hMeta);
        }
        inv.setItem(4, headerItem);

        List<SkillTree_Ability> upgrades = SelectedElement.getSkillUpgradesFor(skillName);
        int[] availableSlots = new int[]{19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};
        int uIdx = 0;

        for (SkillTree_Ability upg : upgrades) {
            int slot = (upg.getPlace() >= 0 && upg.getPlace() < 54 && upg.getPlace() != 4 && upg.getPlace() != 49 && upg.getPlace() != 45)
                    ? upg.getPlace() : (uIdx < availableSlots.length ? availableSlots[uIdx] : 18 + uIdx);
            uIdx++;

            boolean unlocked = playersBranch.getUnlockedAbilities().contains(upg.getName()) || upg.isdef();
            boolean canAfford = playersBranch.GetPoints(element) >= upg.getCost();
            boolean reqMet = upg.getListOfPreAbility().isEmpty() || new HashSet<>(playersBranch.getUnlockedAbilities()).containsAll(upg.getListOfPreAbility());

            int modelId;
            ItemStack item = FastEasyStack(skillMat != null ? skillMat : Material.PAPER, upg.getName());
            ItemMeta meta = item.getItemMeta();
            List<String> lore = new ArrayList<>();

            if (unlocked) {
                modelId = SkillTreeConfig.getInt("AmonPack.Menu." + elName + ".Green", 10013);
                lore.add(ChatColor.GREEN + "✔ ODBLOKOWANO");
            } else if (canAfford && reqMet) {
                modelId = SkillTreeConfig.getInt("AmonPack.Menu." + elName + ".Orange", 10012);
                lore.add(ChatColor.GOLD + "▶ KOSZT: " + ChatColor.YELLOW + upg.getCost() + " pkt " + element.getName());
                if (!upg.getListOfPreAbility().isEmpty()) {
                    lore.add(ChatColor.GRAY + "Wymagane: " + String.join(", ", upg.getListOfPreAbility()));
                }
                lore.add(" ");
                lore.add(ChatColor.GREEN + "▶ Kliknij LPM, aby odblokować!");
            } else {
                modelId = SkillTreeConfig.getInt("AmonPack.Menu." + elName + ".Red", 10011);
                lore.add(ChatColor.RED + "✖ ZABLOKOWANE");
                lore.add(ChatColor.RED + "Koszt: " + upg.getCost() + " pkt (Posiadasz: " + playersBranch.GetPoints(element) + ")");
                if (!upg.getListOfPreAbility().isEmpty()) {
                    lore.add(ChatColor.GRAY + "Wymagane: " + String.join(", ", upg.getListOfPreAbility()));
                }
            }

            List<String> formattedDesc = getFormattedDescription(element.getName(), upg.getName());
            if (!formattedDesc.isEmpty()) {
                lore.add(" ");
                lore.addAll(formattedDesc);
            }

            if (meta != null) {
                meta.setCustomModelData(modelId);
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            inv.setItem(slot, item);
        }

        inv.setItem(49, FastEasyStack(Material.PAPER, ChatColor.RED + "Powrot", 10013));

        p.openInventory(inv);
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
            String name = rs.getString("Player");
            int AirP = rs.getInt("AirPoints");
            int FireP = rs.getInt("FirePoints");
            int WaterP = rs.getInt("WaterPoints");
            int EarthP = rs.getInt("EarthPoints");
            int ChiP = 0;
            try {
                ChiP = rs.getInt("ChiPoints");
            } catch (Exception ignored) {}
            String UnlockedAbilities = rs.getString("UnlockedAbilities");
            String CurrentElement = rs.getString("CurrentElement");
            String AllElements = rs.getString("AllElements");
            List<String> unlockedAbilities = UnlockedAbilities == null || UnlockedAbilities.isEmpty() ? new ArrayList<>() : Arrays.asList(UnlockedAbilities.split(","));
            Element currentElement = CurrentElement == null ? null : Element.getElement(CurrentElement);
            List<Element> allElements = AllElements == null || AllElements.isEmpty() ? new ArrayList<>() : Arrays.stream(AllElements.split(",")).map(Element::getElement).filter(Objects::nonNull).collect(Collectors.toList());

            PlayersBending.add(new PlayerBendingBranch(AirP, currentElement, EarthP, allElements, FireP, name, unlockedAbilities, WaterP, ChiP));
        }
        stmt.close();
    }//Przy reloadzie pobierz wszystkich graczy z DB
    private void DefineAvailableAbilities(){
        ListOfElements.clear();
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
                boolean isPass = SkillTreeConfig.getBoolean("AmonPack.Tree."+Element+"."+Ability+".IsPassiveUpgrade", false)
                        || SkillTreeConfig.getBoolean("AmonPack.Tree."+Element+"."+Ability+".IsAbilityUpgrade", false)
                        || SkillTreeConfig.getBoolean("AmonPack.Tree."+Element+"."+Ability+".IsUpgrade", false);
                AbilityObject.setPassiveUpgrade(isPass);

                boolean isSkillUp = SkillTreeConfig.getBoolean("AmonPack.Tree."+Element+"."+Ability+".IsSkillUpgrade", false);
                AbilityObject.setSkillUpgrade(isSkillUp);

                String targetSkill = SkillTreeConfig.getString("AmonPack.Tree."+Element+"."+Ability+".SkillName", "");
                AbilityObject.setSkillName(targetSkill);

                if(SkillTreeConfig.getBoolean("AmonPack.Tree."+Element+"."+Ability+".IsSpecialBindAbility")){
                    AbilityObject.setSpecialBindAbility(true);
                }
                List<String> lockList = SkillTreeConfig.getStringList("AmonPack.Tree."+Element+"."+Ability+".LockAbilities");
                if (lockList != null && !lockList.isEmpty()) {
                    AbilityObject.setLockAbilities(lockList);
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

    public boolean isPlayerInDungeon(Player player) {
        return player.getWorld().getName().startsWith("dungeon_");
    }

    public void OpenDungeonSkillMenu(String name) {
        PlayerBendingBranch playersBranch = GetBranchByPlayerName(name);
        if (playersBranch == null) return;

        DungeonSkillMenuHolder holder = new DungeonSkillMenuHolder(54, ChatColor.DARK_PURPLE + "Dungeonowe Umiejętności");
        dev.lone.itemsadder.api.FontImages.TexturedInventoryWrapper inventory = new dev.lone.itemsadder.api.FontImages.TexturedInventoryWrapper(
                holder, 54, ChatColor.DARK_PURPLE + "Dungeonowe Umiejętności", new dev.lone.itemsadder.api.FontImages.FontImageWrapper("amonpack:bending_abilities_list")
        );
        Inventory inv = inventory.getInternal();

        List<SkillTree_Ability> unlockedAbilities = new ArrayList<>();
        org.bukkit.configuration.file.FileConfiguration skillTreeConfig = AmonPackPlugin.getSkillTreeConfig();
        if (skillTreeConfig != null && skillTreeConfig.getConfigurationSection("AmonPack.Tree") != null) {
            for (String elName : skillTreeConfig.getConfigurationSection("AmonPack.Tree").getKeys(false)) {
                com.projectkorra.projectkorra.Element pkEl = com.projectkorra.projectkorra.Element.getElement(elName);
                if (pkEl != null) {
                    ElementTree tree = AmonPackPlugin.levelsBending.GetElement(pkEl);
                    if (tree != null) {
                        for (SkillTree_Ability ability : tree.getAbilities()) {
                            if (!ability.isUpgrade()) {
                                if (ability.isdef() && playersBranch.getElementsInPossesion().contains(pkEl)) {
                                    unlockedAbilities.add(ability);
                                } else if (playersBranch.getUnlockedAbilities().contains(ability.getName()) || playersBranch.getTemporaryAbilities().contains(ability.getName())) {
                                    unlockedAbilities.add(ability);
                                }
                            }
                        }
                    }
                }
            }
        }

        int slot = 0;
        for (SkillTree_Ability STA : unlockedAbilities) {
            if (slot >= 45) break;
            String elementname = STA.getElement().getName().toLowerCase();
            org.bukkit.Material material = org.bukkit.Material.getMaterial(SkillTreeConfig.getString("AmonPack.Menu." + elementname + ".Material", "PAPER"));
            int modelid = SkillTreeConfig.getInt("AmonPack.Menu." + elementname + ".Green", 0);
            org.bukkit.inventory.ItemStack item = AmonPackPlugin.FastEasyStack(material, STA.getName());
            if (modelid > 0) {
                org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setCustomModelData(modelid);
                    item.setItemMeta(meta);
                }
            }
            inv.setItem(slot, item);
            slot++;
        }

        org.bukkit.inventory.ItemStack CloseButton = AmonPackPlugin.FastEasyStack(org.bukkit.Material.PAPER, ChatColor.RED + "Zamknij");
        org.bukkit.inventory.meta.ItemMeta CloseMeta = CloseButton.getItemMeta();
        if (CloseMeta != null) {
            CloseMeta.setCustomModelData(10036);
            CloseButton.setItemMeta(CloseMeta);
        }
        inv.setItem(53, CloseButton);

        inventory.showInventory(Bukkit.getPlayer(name));
    }

    public static class SkillUpgradeMenuHolder implements org.bukkit.inventory.InventoryHolder {
        private final Inventory inventory;
        private final int size;
        private final String title;
        private final String skillName;

        public SkillUpgradeMenuHolder(int size, String title, String skillName) {
            this.size = size;
            this.title = title;
            this.skillName = skillName;
            this.inventory = Bukkit.createInventory(this, size, title);
        }

        public String getSkillName() {
            return skillName;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }

        public int getSize() {
            return size;
        }

        public String getTitle() {
            return title;
        }
    }

    public static class DungeonSkillMenuHolder implements org.bukkit.inventory.InventoryHolder {
        private final Inventory inventory;
        private final int size;
        private final String title;
        public DungeonSkillMenuHolder(int size, String title) {
            this.inventory = Bukkit.createInventory(this, size, title);
            this.size = size;
            this.title = title;
        }
        @Override
        public Inventory getInventory() {
            return inventory;
        }
        public int getSize() {
            return size;
        }
        public String getTitle() {
            return title;
        }
    }

    public static List<String> getFormattedDescription(String elementName, String nodeName) {
        org.bukkit.configuration.file.FileConfiguration treeCfg = AmonPackPlugin.getSkillTreeConfig();
        if (treeCfg == null) return new ArrayList<>();
        List<String> desc = treeCfg.getStringList("AmonPack.Tree." + elementName + "." + nodeName + ".Description");
        if (desc == null || desc.isEmpty()) return new ArrayList<>();

        List<String> formatted = new ArrayList<>();
        org.bukkit.configuration.file.FileConfiguration abiCfg = AmonPackPlugin.getAbilitiesConfig();

        for (String line : desc) {
            String processed = line;
            if (abiCfg != null) {
                // 1. %config_sec:PATH% -> converts ms or ticks to seconds (e.g. 2500 -> 2.5, 40 ticks -> 2)
                java.util.regex.Matcher mSec = java.util.regex.Pattern.compile("%config_sec:([^%]+)%").matcher(processed);
                StringBuffer sbSec = new StringBuffer();
                while (mSec.find()) {
                    String path = mSec.group(1);
                    Object val = abiCfg.get(path);
                    String valStr = (val != null) ? formatSecondsVal(path, val) : mSec.group(0);
                    mSec.appendReplacement(sbSec, java.util.regex.Matcher.quoteReplacement(valStr));
                }
                mSec.appendTail(sbSec);
                processed = sbSec.toString();

                // 2. %config_percent:PATH% -> converts decimals or numbers to % (e.g. 0.15 -> 15, 1.25 -> 25, 30 -> 30)
                java.util.regex.Matcher mPct = java.util.regex.Pattern.compile("%config_percent:([^%]+)%").matcher(processed);
                StringBuffer sbPct = new StringBuffer();
                while (mPct.find()) {
                    String path = mPct.group(1);
                    Object val = abiCfg.get(path);
                    String valStr = (val != null) ? formatPercentVal(val) : mPct.group(0);
                    mPct.appendReplacement(sbPct, java.util.regex.Matcher.quoteReplacement(valStr));
                }
                mPct.appendTail(sbPct);
                processed = sbPct.toString();

                // 3. %config_hearts:PATH% -> converts raw damage (half-hearts) to full hearts (e.g. 2.0 -> 1.0, 8.0 -> 4.0)
                java.util.regex.Matcher mHrt = java.util.regex.Pattern.compile("%config_hearts:([^%]+)%").matcher(processed);
                StringBuffer sbHrt = new StringBuffer();
                while (mHrt.find()) {
                    String path = mHrt.group(1);
                    Object val = abiCfg.get(path);
                    String valStr = (val != null) ? formatHeartsVal(val) : mHrt.group(0);
                    mHrt.appendReplacement(sbHrt, java.util.regex.Matcher.quoteReplacement(valStr));
                }
                mHrt.appendTail(sbHrt);
                processed = sbHrt.toString();

                // 4. Standard %config:PATH%
                java.util.regex.Matcher mCfg = java.util.regex.Pattern.compile("%config:([^%]+)%").matcher(processed);
                StringBuffer sbCfg = new StringBuffer();
                while (mCfg.find()) {
                    String path = mCfg.group(1);
                    Object val = abiCfg.get(path);
                    String valStr = (val != null) ? formatConfigVal(val) : mCfg.group(0);
                    mCfg.appendReplacement(sbCfg, java.util.regex.Matcher.quoteReplacement(valStr));
                }
                mCfg.appendTail(sbCfg);
                processed = sbCfg.toString();
            }
            formatted.add(ChatColor.GRAY + ChatColor.translateAlternateColorCodes('&', processed));
        }
        return formatted;
    }

    private static String formatSecondsVal(String path, Object val) {
        double num = 0;
        if (val instanceof Number n) num = n.doubleValue();
        if (path.toLowerCase().endsWith("ticks")) {
            double sec = num / 20.0;
            return (sec == Math.floor(sec)) ? String.valueOf((int) sec) : String.valueOf(sec);
        } else if (path.toLowerCase().endsWith("ms") || path.toLowerCase().endsWith("time") || path.toLowerCase().endsWith("duration")) {
            double sec = num / 1000.0;
            return (sec == Math.floor(sec)) ? String.valueOf((int) sec) : String.valueOf(sec);
        }
        return formatConfigVal(val);
    }

    private static String formatPercentVal(Object val) {
        if (val instanceof Number n) {
            double d = n.doubleValue();
            if (d > 1.0 && d < 2.0) {
                d = (d - 1.0) * 100.0;
            } else if (d > 0.0 && d < 1.0) {
                d = d * 100.0;
            }
            return (d == Math.floor(d)) ? String.valueOf((int) d) : String.valueOf(d);
        }
        return String.valueOf(val);
    }

    private static String formatHeartsVal(Object val) {
        if (val instanceof Number n) {
            double hearts = n.doubleValue() / 2.0;
            return (hearts == Math.floor(hearts)) ? String.valueOf((int) hearts) : String.valueOf(hearts);
        }
        return String.valueOf(val);
    }

    private static String formatConfigVal(Object val) {
        if (val instanceof Double d) {
            return (d == Math.floor(d)) ? String.valueOf(d.intValue()) : String.valueOf(d);
        }
        if (val instanceof Float f) {
            return (f == Math.floor(f)) ? String.valueOf(f.intValue()) : String.valueOf(f);
        }
        return String.valueOf(val);
    }
}

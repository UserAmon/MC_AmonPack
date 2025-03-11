package Mechanics.PVE.Menagerie;

import Mechanics.Skills.Upgrades;
import Mechanics.PVE.Menagerie.Objectives.ObjectiveConditions;
import Mechanics.PVE.Menagerie.Objectives.Objectives;
import Mechanics.Skills.BendingGuiMenu;
import Mechanics.Skills.UpgradesMenager;
import UtilObjects.Skills.PlayerSkillTree;
import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.board.BendingBoardManager;
import methods_plugins.AmonPackPlugin;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import java.util.*;
import java.util.stream.Collectors;

import static Mechanics.Skills.BendingGuiMenu.FastEasyStack;
import static Mechanics.Skills.BendingGuiMenu.MaterialByElement;

public class Menagerie {
    private final String MenagerieName;
    private final Location CenterLocation;
    private Location ReturnLocation;
    private final Location EndLocation;
    private final String LastEncounter;
    private final boolean RequireReady;
    private final int XRange;
    private final int ZRange;
    private BukkitTask TaskEverySecond;
    private final HashMap<Player,Boolean> ReadyPlayers = new HashMap<>();
    private final CustomInventoryOwner MenagerieMenuGeneral = new CustomInventoryOwner();
    private final CustomInventoryOwner MenagerieMenuUpgrades = new CustomInventoryOwner();
    private final CustomInventoryOwner MenagerieMenuListUpgrades = new CustomInventoryOwner();
    private final CustomInventoryOwner MenagerieUpgradesShop = new CustomInventoryOwner();
    private final Map<Player, Double> PlayerPoints = new HashMap<>();
    private final List<Player> PlayersInDungAndBossBars = new ArrayList<>();
    private List<String> TitleList=new ArrayList<>();
    private final List<Encounter> ListOfEncounters;
    private Encounter ActiveEncounter;
    private BoardManager sbManager;

    public Menagerie(String menagerieName, Location centerLocation, Location endLocation, String lastEncounter, boolean requireReady, int XRange, int ZRange, List<Encounter> listOfEncounters) {
        MenagerieName = menagerieName;
        CenterLocation = centerLocation;
        EndLocation = endLocation;
        LastEncounter = lastEncounter;
        RequireReady = requireReady;
        this.XRange = XRange;
        this.ZRange = ZRange;
        ListOfEncounters = listOfEncounters;
    }

    public void StartMenagerie(List<Player> p){
        ActiveEncounter=ListOfEncounters.get(0);
        for(Player player :p){
            Reset(player);
        }
        ClearMenagerie();
        PlayersInDungAndBossBars.addAll(p);
        NextEncouter();
        sbManager=new BoardManager();
        //ItemStack Gifts = FastEasyStack(Material.ENDER_CHEST, ChatColor.RED+"Dar");
        //Gifts.setAmount(2);
        //p.getInventory().addItem(Gifts);
        if(TaskEverySecond!=null){
            TaskEverySecond.cancel();
        }
            TaskEverySecond = Bukkit.getScheduler().runTaskTimer(AmonPackPlugin.plugin, this::MenagerieUpdater, 0L, 20L);

    }
    private void MenagerieUpdater(){
        if(!PlayersInDungAndBossBars.isEmpty()){
            List<Player>Players = new ArrayList<>(PlayersInDungAndBossBars);
            for (Player p :Players) {
                if (!IsInMenagerie(p.getLocation())){
                    Reset(p);
                }}
            for (Player p:PlayersInMenagerie()) {
                if (!IsInMenagerie(p.getLocation())){
                    Reset(p);
                }else{
                    //String[] newTexts = {ChatColor.DARK_PURPLE+ "   ", ChatColor.LIGHT_PURPLE+CurrentObjectiveSubTitle, ChatColor.LIGHT_PURPLE+CurrentObjectiveTitle,ChatColor.DARK_PURPLE+""};
                    if(TitleList!=null && !TitleList.isEmpty()) {
                        String[] objectiveArray = TitleList.toArray(new String[0]);

                        String[] newTexts = new String[objectiveArray.length];
                        for (int i = 0; i < objectiveArray.length; i++) {
                            newTexts[i] = ChatColor.DARK_PURPLE + objectiveArray[i];
                        }
                        sbManager.addRows(p, newTexts);
                    }
                }}
            UpdateObjectives(null,null);
            if(RequireReady){
            if (ActiveEncounter.getReadyWaitingDoors().IsClosed && ReadyPlayers.values().stream().allMatch(ready -> ready)) {
                ActiveEncounter.getReadyWaitingDoors().ChangeFirst(true);
                for (Player p :Players) {
                    removeAllItemX(p,FastEasyStack(Material.COMPASS, ChatColor.RED+"Gotowy?"));
                }}}
        }else{
            ClearMenagerie();
            TaskEverySecond.cancel();
            TaskEverySecond = null;
        }
    }

    private void UpdateObjectives(Entity ent,PlayerInteractEvent e){
        List<String>TestObList = new ArrayList<>();
        if(ActiveEncounter!=null || ent!=null){
        if (ActiveEncounter.getActiveObjectivesList()!=null&&!ActiveEncounter.getActiveObjectivesList().isEmpty()){
            for (Objectives OBJ:ActiveEncounter.getActiveObjectivesList()) {
                boolean Skip=false;
                if(OBJ.getReqObjectivesComplete()!=null &&!OBJ.getReqObjectivesComplete().isEmpty()){
                    int isObjectiveChecked = 0;
                    for (String reqObjective : OBJ.getReqObjectivesComplete()) {
                        for (Objectives objective : ActiveEncounter.getAllObjectivesList()) {
                            if (objective.getObjectiveName().equals(reqObjective) && objective.isUsed()) {
                                isObjectiveChecked++;
                            }}}
                    if(isObjectiveChecked<OBJ.getReqObjectivesComplete().size()){
                        Skip=true;
                    }}
                if (!OBJ.isUsed()&&!Skip){
                    int ConditionsCounter = 0;
                    boolean foundNonKillCondition = false;
                    for (ObjectiveConditions Condition : OBJ.getConditions()) {
                        if (!Condition.getCType().equals(ObjectiveConditions.ConditionType.KILL) && !Condition.getCType().equals(ObjectiveConditions.ConditionType.MULTIKILL)) {
                            foundNonKillCondition = true;
                        }}
                    for (ObjectiveConditions Condition:OBJ.getConditions()) {
                        if(!Condition.isChecked()){
                            if(ent!=null){
                                Condition.CheckConditions(ent,this);
                                if(Condition.getCType().equals(ObjectiveConditions.ConditionType.KILL) && !foundNonKillCondition){
                                    setCurrentObjectiveTitle("Pokonano: "+Condition.getEnemyKilledCounter()+"/"+Condition.getEnemies().getAmount());
                                }
                                if(Condition.getCType().equals(ObjectiveConditions.ConditionType.MULTIKILL) && !foundNonKillCondition){
                                    setCurrentObjectiveTitle("Pokonano: "+Condition.getEnemyKilledCounter()+"/"+Condition.getTotalSpawnedEnemies());
                                }
                            }else if(e!=null){
                                Condition.CheckConditions(e,this);
                            }else{
                                Condition.CheckConditions(this);
                            }}
                        if(Condition.isChecked()){
                            ConditionsCounter++;
                        }}
                    if(ConditionsCounter>=OBJ.getConditions().size()){
                        TestObList.addAll(OBJ.Start(PlayersInDungAndBossBars,this));
                        TitleList = OBJ.getDisplay();
                        OBJ.setUsed(true);
                    }
                }
            }
            if (!TestObList.isEmpty()){
                ActiveEncounter.getActiveObjectivesList().addAll(ActiveEncounter.getAllObjectivesList().stream()
                        .filter(objective -> TestObList.contains(objective.getObjectiveName()))
                        .collect(Collectors.toList()));
                for (Objectives obj : ActiveEncounter.getActiveObjectivesList()){
                    if(obj.getConditions()!=null && TestObList.contains(obj.getObjectiveName())){
                        for (ObjectiveConditions con:obj.getConditions()) {
                            con.setChecked(false);
                        }}}
            }}}
    }

    public void ActivateByKill(Entity entity){
        UpdateObjectives(entity,null);
    }
    public void ActivateByClick(PlayerInteractEvent event){
        Player player = event.getPlayer();
        if(event.getItem() != null && event.getItem().isSimilar(FastEasyStack(Material.COMPASS, ChatColor.RED+"Gotowy?"))){
            event.setCancelled(true);
            OpenMenagerieMenu(player);
        }else if(event.getItem() != null && event.getItem().isSimilar(FastEasyStack(Material.ENDER_CHEST, ChatColor.RED+"Dar"))){
            event.setCancelled(true);
            OpenUpgradesMenagerie(player);
        }else{
            UpdateObjectives(null,event);
        }
    }

    private void OpenYourUpgradesMenagerie(Player p){
        Inventory menu = Bukkit.createInventory(MenagerieMenuListUpgrades, 18, "Menagerie");
        for (int i = 0; i < menu.getSize(); i++) {
            menu.setItem(i, FastEasyStack(Material.BLACK_STAINED_GLASS_PANE,ChatColor.BLACK + ""));
        }
        List<String> UpgradesOfPlayer = AmonPackPlugin.getPlayerUpgrades(p);
        List<Upgrades> eligibleUpgrades = new ArrayList<>();
        for (Upgrades up : UpgradesMenager.MenagerieUpgradesList) {
            if (UpgradesOfPlayer.contains(up.getName())) {
                eligibleUpgrades.add(up);
            }}
        List<Upgrades> RandomUpgrades = new ArrayList<>(eligibleUpgrades);
        int i =0;
        for (Upgrades up:RandomUpgrades) {
            menu.setItem(i, up.getItem());
            i++;
        }
        menu.setItem(17,BendingGuiMenu.FastEasyStack(Material.BARRIER,ChatColor.RED+"Wyjscie"));
        p.openInventory(menu);
    }
    private void OpenShop(Player p){
        Inventory menu = Bukkit.createInventory(MenagerieUpgradesShop, 54, "Menagerie");
        for (int i = 0; i < menu.getSize(); i++) {
            menu.setItem(i, FastEasyStack(Material.BLACK_STAINED_GLASS_PANE,ChatColor.BLACK + ""));
        }
        int i =0;
        for (Upgrades up : UpgradesMenager.MenagerieUpgradesList) {
            menu.setItem(i,up.getItem());
            i++;
        }
        menu.setItem(53,BendingGuiMenu.FastEasyStack(Material.BARRIER,ChatColor.RED+"Wyjscie"));
        menu.setItem(52,BendingGuiMenu.FastEasyStack(Material.GOLD_BLOCK,ChatColor.RED+"Wyjscie"));
        p.openInventory(menu);
    }
    public void OpenUpgradesMenagerie(Player p){
        Inventory menu = Bukkit.createInventory(MenagerieMenuUpgrades, 18, "Menagerie");
        for (int i = 0; i < menu.getSize(); i++) {
            menu.setItem(i, FastEasyStack(Material.BLACK_STAINED_GLASS_PANE,ChatColor.BLACK + ""));
        }
        List<String> UpgradesOfPlayer = AmonPackPlugin.getPlayerUpgrades(p);
        List<Upgrades> eligibleUpgrades = new ArrayList<>();
        List<String> SelectedPath = new ArrayList<>(Objects.requireNonNull(BendingGuiMenu.getPlayerSkillTreeByName(p)).getSelectedPath());
        for (Mechanics.Skills.Upgrades up : UpgradesMenager.MenagerieUpgradesList) {
            if (!SelectedPath.contains(up.getName())&&
                    !UpgradesOfPlayer.contains(up.getName()) &&
                    (up.getReqUpdates() == null || UpgradesOfPlayer.stream().anyMatch(up.getReqUpdates()::contains))) {
                eligibleUpgrades.add(up);
            }}
        Collections.shuffle(eligibleUpgrades);
        List<Upgrades> RandomUpgrades = new ArrayList<>(eligibleUpgrades.subList(0, Math.min(5, eligibleUpgrades.size())));
        int i =2;
        for (Upgrades up:RandomUpgrades) {
            menu.setItem(i, up.getItem());
            i++;
        }
        menu.setItem(17,BendingGuiMenu.FastEasyStack(Material.BARRIER,ChatColor.RED+"Wyjscie"));
        p.openInventory(menu);
    }
    public void OpenMenagerieMenu(Player p){
        if (ReadyPlayers !=null){
        Inventory menu = Bukkit.createInventory(MenagerieMenuGeneral, 18, "Menagerie");
        for (int i = 0; i < menu.getSize(); i++) {
            menu.setItem(i, FastEasyStack(Material.BLACK_STAINED_GLASS_PANE,ChatColor.BLACK + ""));
        }
        if (ReadyPlayers.get(p)){
            menu.setItem(4, FastEasyStack(Material.GREEN_STAINED_GLASS_PANE,ChatColor.GREEN+"Gotowy"));
        }else{
            menu.setItem(4, FastEasyStack(Material.RED_STAINED_GLASS_PANE,ChatColor.RED+"Gotowy?"));
        }
        menu.setItem(8, FastEasyStack(Material.CHEST,ChatColor.DARK_PURPLE + "Dary"));
        menu.setItem(7, FastEasyStack(Material.ENDER_CHEST,ChatColor.DARK_PURPLE + "Sklep"));
        PlayerSkillTree Skills=BendingGuiMenu.getPlayerSkillTreeByName(p);
        Element ele = Element.getElement(Skills.getCurrentElement());
        menu.setItem(17, FastEasyStack(MaterialByElement(ele),ele.getColor()+"Wybierz Ruchy"));
        p.openInventory(menu);
    }}
    public void OnInventoryClickMenagerie(InventoryClickEvent e){
        Player p = (Player) e.getWhoClicked();
        e.setCancelled(true);
        if (e.getInventory().getHolder() == MenagerieMenuGeneral) {
            if (e.getCurrentItem().getType().equals(Material.CHEST)) {
                OpenYourUpgradesMenagerie(p);
            }
            if (e.getCurrentItem().getType().equals(Material.ENDER_CHEST)) {
                OpenShop(p);
            }
            if (e.getCurrentItem().getType().equals(Material.RED_STAINED_GLASS_PANE)){
                setPlayerReady(p);
                p.closeInventory();
            }
            PlayerSkillTree Skills=BendingGuiMenu.getPlayerSkillTreeByName(p);
            if (e.getCurrentItem().getType().equals(MaterialByElement(Element.getElement(Skills.getCurrentElement())))) {
                BendingGuiMenu.OpenGeneralBendingMenu(p);
            }
        }
        if (e.getInventory().getHolder() == MenagerieMenuListUpgrades) {
            if (e.getCurrentItem().getType().equals(Material.BARRIER)) {
                OpenMenagerieMenu(p);
            }
        }
        if (e.getInventory().getHolder() == MenagerieUpgradesShop) {
            if (e.getCurrentItem().getType().equals(Material.BARRIER)) {
                OpenMenagerieMenu(p);
            }
        }
        if (e.getInventory().getHolder() == MenagerieMenuUpgrades) {
            if (e.getCurrentItem().getType().equals(Material.BARRIER)) {
                OpenMenagerieMenu(p);
            }
            for (Mechanics.Skills.Upgrades upgrade:UpgradesMenager.MenagerieUpgradesList) {
                if (e.getCurrentItem().equals(upgrade.getItem())){
                    //if (PlayerPoints.get(p)>=upgrade.getPrice()){
                        List<String> TestList = new ArrayList<>(AmonPackPlugin.getPlayerUpgrades(p));
                        if (!AmonPackPlugin.getPlayerUpgrades(p).contains(upgrade.getName())){
                            TestList.add(upgrade.getName());
                        }
                        AmonPackPlugin.setPlayerUpgrade(p,TestList);
                        //PlayerPoints.put(p,PlayerPoints.get(p)-upgrade.getPrice());
                        //p.sendMessage(ChatColor.RED+"Wydałeś: "+upgrade.getPrice()+"$ Zostało ci: "+PlayerPoints.get(p)+"$");
                        p.closeInventory();
                        ItemStack Gifts = FastEasyStack(Material.ENDER_CHEST, ChatColor.RED + "Dar");
                        Inventory inventory = p.getInventory();
                        ItemStack[] items = inventory.getContents();
                        for (int i = 0; i < items.length; i++) {
                            if (items[i] != null && items[i].isSimilar(Gifts)) {
                                int newAmount = items[i].getAmount() - 1;
                                if (newAmount > 0) {
                                    items[i].setAmount(newAmount);
                                    inventory.setItem(i, items[i]);
                                } else {
                                    inventory.setItem(i, null);
                                }
                                break;
                            }}
                        break;
                    /*}else{
                        p.sendMessage(ChatColor.RED+"Nie stać Cię na to! Brakuje ci: "+(upgrade.getPrice()-PlayerPoints.get(p)));
                    }*/
                }}

        }
    }
    public void TitleChange(Player player, String title, BarColor color, BarStyle style, double progress, int durationInSeconds) {
        BossBar bossBar = Bukkit.createBossBar(title, color, style);
        bossBar.setProgress(progress);
        bossBar.addPlayer(player);
        new BukkitRunnable() {
            @Override
            public void run() {
                bossBar.removeAll();
            }
        }.runTaskLater(AmonPackPlugin.plugin, durationInSeconds * 20L);
    }
    public void NextRandomEncouter(){
        if (ActiveEncounter != null) {
            ActiveEncounter.setDone(true);
        if(ActiveEncounter.isLast()){
            for (Player p : PlayersInMenagerie()){
                if(ReturnLocation!=null){
                    ReturnLocation.setWorld(Bukkit.getWorld(AmonPackPlugin.getNewConfigz().getString("AmonPack.Spawn.World")));
                    p.teleport(ReturnLocation);
                }else{
                EndLocation.setWorld(Bukkit.getWorld(AmonPackPlugin.getNewConfigz().getString("AmonPack.Spawn.World")));
                p.teleport(EndLocation);
                }
            }
        }else{
        List<Encounter> unfinishedEncounters = ListOfEncounters.stream()
                .filter(encounter -> !encounter.isDone())
                .collect(Collectors.toList());
        if (!unfinishedEncounters.isEmpty()) {
            int randomIndex = new Random().nextInt(unfinishedEncounters.size());
            ActiveEncounter=unfinishedEncounters.get(randomIndex);
        }
        NextEncouter();
        UpdateObjectives(null, null);
        }
    }}
    private void NextEncouter(){
        MenagerieRest(PlayersInDungAndBossBars);
        TitleList.addAll(ActiveEncounter.getFirstObjectiveTitle());
    }
    private void CombatRest(Player p, int i, double money){
        ReadyPlayers.put(p,false);
        if(!PlayerPoints.isEmpty()&&PlayerPoints.containsKey(p)) {
            PlayerPoints.put(p, PlayerPoints.get(p) + money);
        }else{
            PlayerPoints.put(p, money);
        }
        ItemStack menu=FastEasyStack(Material.COMPASS, ChatColor.RED+"Gotowy?");
        //ItemStack Gifts = FastEasyStack(Material.ENDER_CHEST, ChatColor.RED+"Dar");
        removeAllItemX(p,menu);
        p.getInventory().addItem(FastEasyStack(Material.COMPASS, ChatColor.RED+"Gotowy?"));
        //Gifts.setAmount(i);
        //p.getInventory().addItem(Gifts);
    }
    public void setCurrentObjectiveTitle(String currentObjectiveTitle) {
        List<String>list=new ArrayList<>();
        list.add(currentObjectiveTitle);
        TitleList = list;
    }
    public void MenagerieRest(List<Player>players){
        for (Player p:players) {
            p.teleport(ActiveEncounter.getStartTeleportLocation());
            if(RequireReady){
                CombatRest(p,1,50.0);
            }}
        if(RequireReady){
            ActiveEncounter.getReadyWaitingDoors().ChangeFirst(false);
        }
    }
    public String getMenagerieName() {
        return MenagerieName;
    }
    public static void addTemporaryHealth(Player player, double healthAmount, long seconds) {
        player.setAbsorptionAmount(player.getAbsorptionAmount()+healthAmount);
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.getAbsorptionAmount()-healthAmount>=0){
                    player.setAbsorptionAmount(player.getAbsorptionAmount()-healthAmount);
                }else{
                    player.setAbsorptionAmount(0);
                }
            }
        }.runTaskLater(AmonPackPlugin.plugin, 20*seconds);
    }
    public void Reset(Player p){
        removeAllItemX(p,FastEasyStack(Material.COMPASS, ChatColor.RED+"Gotowy?"));
        removeAllItemX(p,FastEasyStack(Material.ENDER_CHEST, ChatColor.RED+"Dar"));
        ResetAdditionalAbilities(p);
        ResetUpgrades(p);
        if(sbManager!=null){
            sbManager.removeTopRows(p);
        }
        if (!PlayersInDungAndBossBars.isEmpty()){
            PlayersInDungAndBossBars.remove(p);
        }
    }
    public void ResetAdditionalAbilities(Player p){
        BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(p);
        PlayerSkillTree skills = BendingGuiMenu.getPlayerSkillTreeByName(p);
        List<String> defabilities = BendingGuiMenu.DefAbilities;
        for (int i = 0; i <= 9; i++) {
            String ability = bPlayer.getAbilities().get(i);
            if (CoreAbility.getAbility(ability) != null &&
                    !skills.getSelectedPath().contains(ability) &&
                    !defabilities.contains(ability)) {
                BendingBoardManager.getBoard(p).get().clearSlot(i);
                bPlayer.getAbilities().remove(i);
            }
        }
    }
    public static void removeAllItemX(Player player, ItemStack targetItem) {
        Inventory inventory = player.getInventory();
        ItemStack[] items = inventory.getContents();

        for (int i = 0; i < items.length; i++) {
            if (items[i] != null && items[i].isSimilar(targetItem)) {
                inventory.setItem(i, null);
            }
        }
    }
    public void ResetUpgrades(Player p){
        List<String> UpgradesOfPlayer = new ArrayList<>(AmonPackPlugin.getPlayerUpgrades(p));
        for (Upgrades UPV : UpgradesMenager.MenagerieUpgradesList) {
            if (!UpgradesOfPlayer.isEmpty() && UpgradesOfPlayer.contains(UPV.getName())) {
                UpgradesOfPlayer.removeIf(upgrade -> upgrade.equals(UPV.getName()));
            }
        }
        AmonPackPlugin.setPlayerUpgrade(p, UpgradesOfPlayer);
    }
    public void setPlayerReady(Player p){
        ReadyPlayers.remove(p);
        ReadyPlayers.put(p,true);
    }
    public static class Doors {
        private final Location l1;
        private final Location l2;
        private final Material m;
        public boolean IsClosed;
        public Doors(Location l1, Location l2, Material mat) {
            this.l1=l1;
            this.l2=l2;
            this.m=mat;
            this.IsClosed=true;
        }
        public void ChangeFirst(boolean SetAir) {
            World world = l1.getWorld();
            int minX = Math.min(l1.getBlockX(), l2.getBlockX());
            int minY = Math.min(l1.getBlockY(), l2.getBlockY());
            int minZ = Math.min(l1.getBlockZ(), l2.getBlockZ());
            int maxX = Math.max(l1.getBlockX(), l2.getBlockX());
            int maxY = Math.max(l1.getBlockY(), l2.getBlockY());
            int maxZ = Math.max(l1.getBlockZ(), l2.getBlockZ());
            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        Block block = world.getBlockAt(x, y, z);
                        if (SetAir){
                            if (block.getType().equals(m)){
                                block.setType(Material.AIR);
                                this.IsClosed = false;
                            }}else{
                            if (block.getType().equals(Material.AIR)) {
                            block.setType(m);
                                this.IsClosed = true;
                        }}}}}}

        public void setClosed(boolean closed) {
            IsClosed = closed;
        }
    }
    public List<Player> getPlayersInRadius(Location center, double radius) {
        List<Player> playersInRange = new ArrayList<>();
        double radiusSquared = radius * radius;
        for (Player player :PlayersInDungAndBossBars) {
            Location playerLocation = player.getLocation();
            if (center.getWorld() == playerLocation.getWorld() && center.distanceSquared(playerLocation) <= radiusSquared) {
                playersInRange.add(player);
            }}
        return playersInRange;
    }
    public boolean IsInMenagerie(Location l){
        if (l.getWorld().equals(CenterLocation.getWorld())){
            return (l.getX() < CenterLocation.getX() + XRange && l.getX() > CenterLocation.getX() - XRange) && (l.getZ() < CenterLocation.getZ() + ZRange && l.getZ() > CenterLocation.getZ() - ZRange);
        }
        return false;
    }
    public List<Player> PlayersInMenagerie(){
        List<Player> PInDung = new ArrayList<>();
        for (Player p:Bukkit.getOnlinePlayers()) {
            if (p.getWorld().equals(CenterLocation.getWorld())){
                Location l = p.getLocation();
                if ((l.getX() < CenterLocation.getX()+XRange && l.getX() > CenterLocation.getX()-XRange)&&(l.getZ() < CenterLocation.getZ()+ZRange && l.getZ() > CenterLocation.getZ()-ZRange)) {
                    PInDung.add(p);
                }}}
        return PInDung;
    }
    private void ClearMenagerie(){
        PlayersInDungAndBossBars.clear();
        PlayerPoints.clear();
        ReadyPlayers.clear();
        for (Entity entity : Objects.requireNonNull(CenterLocation.getWorld()).getEntities()) {
            if (entity.getWorld().equals(CenterLocation.getWorld()) && !entity.getType().equals(EntityType.PLAYER)){
                Location l = entity.getLocation();
                if ((l.getX() < CenterLocation.getX()+XRange && l.getX() > CenterLocation.getX()-XRange)&&(l.getZ() < CenterLocation.getZ()+ZRange && l.getZ() > CenterLocation.getZ()-ZRange)) {
                    entity.remove();
                }}}
        for (Encounter enc:ListOfEncounters) {
            enc.ResetObjectives();
        }
        }
    public List<Entity> GetEnemiesInDung(){
        List<Entity>List=new ArrayList<>();
        for (Entity entity : Objects.requireNonNull(CenterLocation.getWorld()).getEntities()) {
            if (entity.getWorld().equals(CenterLocation.getWorld()) && !entity.getType().equals(EntityType.PLAYER) && entity instanceof LivingEntity){
                Location l = entity.getLocation();
                if ((l.getX() < CenterLocation.getX()+XRange && l.getX() > CenterLocation.getX()-XRange)&&(l.getZ() < CenterLocation.getZ()+ZRange && l.getZ() > CenterLocation.getZ()-ZRange)) {
                    List.add(entity);
                }}}
        return List;
        }
    public void Create1SecondZone(Location loc, int DamageRadius,Material mat) {
        BukkitRunnable runnable = new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                spawnParticleCircle(loc,DamageRadius,DamageRadius*10,mat);
                ticks++;
                if (ticks >= 4) {
                    cancel();
                }}};
        runnable.runTaskTimer(AmonPackPlugin.plugin, 0L, 5L);
    }
    public void spawnParticleCircle(Location center, double radius, int numParticles, Material b) {
        World world = center.getWorld();
        double increment = 360.0 / numParticles;
        for (int i = 0; i < numParticles; i++) {
            double angle = Math.toRadians(i * increment);
            double xOffset = radius * Math.cos(angle);
            double zOffset = radius * Math.sin(angle);
            Location particleLocation = center.clone().add(xOffset, 1.5, zOffset);
            world.spawnParticle(Particle.BLOCK_CRACK, particleLocation, 1, 0, 0, 0, 0, b.createBlockData());
            world.spawnParticle(Particle.BLOCK_DUST, particleLocation, 1, 0, 0, 0, 0, b.createBlockData());
        }
    }
    public HashMap<Player, Boolean> getReadyPlayers() {
        return ReadyPlayers;
    }
    public Encounter getActiveEncounter() {
        return ActiveEncounter;
    }

    public void setReturnLocation(Location returnLocation) {
        ReturnLocation = returnLocation;
    }

    public Location getEndLocation() {
        return EndLocation;
    }

    public Location getReturnLocation() {
        return ReturnLocation;
    }

    public Location getCenterLocation() {
        return CenterLocation;
    }
    public List<Player> GetPlayersList(){
        return PlayersInDungAndBossBars;
    }
}



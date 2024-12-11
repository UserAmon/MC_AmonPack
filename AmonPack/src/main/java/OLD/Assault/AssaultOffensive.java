package OLD.Assault;
/*
import commands.Commands;
import methods_plugins.AmonPackPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

import static OLD.Assault.AssaultMethods.*;


public class AssaultOffensive {
    private String name;
    private Location ArenaLocation;
    private Location Shop_Location;
    private int LocRange;
    private Map<Player, Float> PlayerScore = new HashMap<>();
    private List<Player>PlayersInDungeon;
    private ArmorStand Shop;
    private List<Upgrades> GeneralUpgradesList;
    public Map<Player, List<Upgrades>> IndividualUpgrades = new HashMap<>();
    private String Command;
    private final Map<Player, BossBar> playerBossBars = new HashMap<>();
    private BukkitTask TaskEverySecond;
    private BukkitTask TaskAfterRest;
    private Map<Player,Boolean> UpgradesInThisRound = new HashMap<>();
    private String CurrentTitle;
    public Map<Player, List<String>> BonusAbilities = new HashMap<>();
    private Map<Player, List<Upgrades>> RandomPUpgrades = new HashMap<>();
    private Inventory GeneralShop;
    private Inventory IndividualShop;
    public int secondsElapsed;
    private int RestTimer;
    private int RestTask;



    void AssaultTimer() {
        if (!PInArena(ArenaLocation,LocRange,LocRange).isEmpty()){
            if (secondsElapsed>=RestTimer){
                try {
                    if (!GeneralShop.getViewers().isEmpty()){
                        for (HumanEntity p : GeneralShop.getViewers()) {
                            p.closeInventory();
                        }}
                    if (!IndividualShop.getViewers().isEmpty()){
                        for (HumanEntity p : IndividualShop.getViewers()) {
                            p.closeInventory();
                        }}
                }catch (Exception ignored){}
            }
            if (!playerBossBars.isEmpty()) {
                for (Player p : playerBossBars.keySet()) {
                    if (!InArenaRange(p.getLocation(), ArenaLocation, LocRange, LocRange)) {
                        removePrivateBossBar(p,playerBossBars);
                    } else {
                        if (!playerBossBars.get(p).getTitle().equalsIgnoreCase(CurrentTitle) && !playerBossBars.get(p).getTitle().startsWith("Przygotuj")) {
                            CurrentObjectiveUpdater(p,playerBossBars,CurrentTitle);
                        }}}}
            /*if (CurrentObjective !=  null){
                Collection<Entity> NearbyEntity = Objects.requireNonNull(ArenaLocation.getWorld()).getNearbyEntities(ArenaLocation, LocRange, 150, LocRange);
                if (NearbyEntity.size() < CurrentObjective.getMinMobs()){
                    CurrentObjective.SpawnMob();
                }}
        }else{
            if (TaskEverySecond != null){
                TaskEverySecond.cancel();
            }}}

    public void Start(Player player) {
        player.teleport(ArenaLocation);
        PlayersInDungeon = PInArena(ArenaLocation,LocRange,LocRange);
        for (Player p:PlayersInDungeon) {
            PlayerScore.put(p,150f);
            AssaultMethods.ResetAddonAbis(p);
            CurrentObjectiveUpdater(p,playerBossBars,CurrentTitle);
            p.removePotionEffect(PotionEffectType.NIGHT_VISION);
            p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, true, false));
        }
        for (Upgrades up:GeneralUpgradesList) {
            up.setUnlocked(false);
        }
        if (TaskAfterRest != null){
            TaskAfterRest.cancel();
        }
        if (TaskEverySecond != null){
            TaskEverySecond.cancel();
        }
        NextObjective();
        TaskEverySecond = Bukkit.getScheduler().runTaskTimer(AmonPackPlugin.plugin, this::AssaultTimer, 0L, 20L);
        Commands.ExecuteCommandExample example = new Commands.ExecuteCommandExample();
        example.executeCommand(Command);
        PlayerScore.clear();
        IndividualUpgrades.clear();
        ClearArena();
        Shop = (ArmorStand) Objects.requireNonNull(Shop_Location.getWorld()).spawnEntity(Shop_Location, EntityType.ARMOR_STAND);
        Shop.setCustomNameVisible(true);
        Shop.setGravity(false);
        Shop.setVisible(false);
        Shop.setCustomName(ChatColor.DARK_PURPLE+"Ulepszenia");
        UpgradesInThisRound.clear();
    }


    void NextObjective(){
        for (Player p:PlayersInDungeon) {
            PlayerScore.put(p,PlayerScore.get(p)+150f);
        }
        Shop.setCustomName(ChatColor.DARK_PURPLE+"Ulepszenia");
        /*System.out.println(Objectives.size()-1);
        CurrentObjective = Objectives.get(new Random().nextInt(Objectives.size()-1));
        TaskAfterRest = Bukkit.getScheduler().runTaskLater(AmonPackPlugin.plugin, () -> {
            CurrentObjective.SpawnMob();
                Shop.setCustomName("");
                for (Player p:PlayersInDungeon) {
                    p.setAbsorptionAmount(0);
                    PvP.sendTitleMessage(p, CurrentObjective.getName(), CurrentObjective.getMessage(), 20,60,20);
                    p.setHealth(p.getMaxHealth());
                    List<Upgrades> PlayerUpgrades = new ArrayList<>();
                    if (IndividualUpgrades.get(p)!=null){
                        PlayerUpgrades = IndividualUpgrades.get(p);
                    }
                    updatePrivateBossBar(p,CurrentObjective.getDepositedKeys(), CurrentObjective.getKeysToDeposit(),"Złożone klucze: "+CurrentObjective.getDepositedKeys()+"/"+CurrentObjective.getKeysToDeposit(),playerBossBars);
                    if (GetUpgradeByNameFromPlayer("SpiritOrbsWave", PlayerUpgrades)!=null){
                        if (GetUpgradeByNameFromPlayer("SpiritOrbsWave",PlayerUpgrades).isUnlocked()){
                            ItemStack buddingAmethyst = new ItemStack(Material.BUDDING_AMETHYST);
                            ItemMeta meta = buddingAmethyst.getItemMeta();
                            meta.setDisplayName(ChatColor.LIGHT_PURPLE+"Duchowa Kula");
                            buddingAmethyst.setItemMeta(meta);
                            for (int i = 0; i < 5; i++) {
                                p.getInventory().addItem(buddingAmethyst);
                            }}}
                    if (GetUpgradeByNameFromPlayer("EarthArmorWave", PlayerUpgrades)!=null){
                        if (GetUpgradeByNameFromPlayer("EarthArmorWave",PlayerUpgrades).isUnlocked()){
                            AssaultMethods.addTemporaryHealth(p,6,30);
                        }}
            }}, 20L *RestTimer);
        Bukkit.getScheduler().cancelTask(RestTask);
        RestTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(AmonPackPlugin.plugin, () -> {
            if (secondsElapsed < RestTimer) {
                secondsElapsed++;
                for (Player p:PlayersInDungeon) {
                    updatePrivateBossBar(p,(RestTimer-secondsElapsed), RestTimer,"Przygotuj się! Kolejny Atak za: "+(RestTimer-secondsElapsed),playerBossBars);
                }} else {
                Bukkit.getScheduler().cancelTask(RestTask);
            }
        }, 0, 20L);
    }





    public void ActivateByKill(EntityType KilledEntity){
    }


    void ClearArena(){
        for (Entity entity : Objects.requireNonNull(ArenaLocation.getWorld()).getEntities()) {
            if (entity.getWorld().equals(ArenaLocation.getWorld()) && !entity.getType().equals(EntityType.PLAYER)){
                Location l = entity.getLocation();
                if ((l.getX() < ArenaLocation.getX()+LocRange && l.getX() > ArenaLocation.getX()-LocRange)&&(l.getZ() < ArenaLocation.getZ()+LocRange && l.getZ() > ArenaLocation.getZ()-LocRange)) {
                    entity.remove();
                }}}
    }





















/*
    public void AssaultMenuClick(Player player, ItemStack item) {
        Upgrades upgrade = GetUpgradeByMaterialAndName(item.getType(),item.getItemMeta().getDisplayName(),GeneralUpgradesList);
        if (upgrade.getPrice()>0 || upgrade.isBlessing()){
            for (Player p:PlayerScore.keySet()) {
                if (player.equals(p)){
                    if (upgrade.isUnlocked() && !upgrade.isMultiBuy()){
                        p.sendMessage(ChatColor.RED+"Już odblokowano!");
                        break;
                    }else{
                        if (upgrade.isBlessing()){
                            if (upgrade.isAbility()) {
                                List<String> templist = new ArrayList<>();
                                if (BonusAbilities.get(p) != null) {
                                    templist = BonusAbilities.get(p);
                                }
                                templist.add(upgrade.getName());
                                BonusAbilities.put(p, templist);
                                p.sendMessage(ChatColor.GREEN + "Odblokowano ruch:   : " + upgrade.getName());
                            }
                            upgrade.setUnlocked(true);
                            UpgradesInThisRound.put(player,true);
                            List<Upgrades> CurrentUpgrades = new ArrayList<>();
                            if (IndividualUpgrades.get(p)!=null){
                                CurrentUpgrades = IndividualUpgrades.get(p);
                            }
                            CurrentUpgrades.add(upgrade);
                            IndividualUpgrades.put(p,CurrentUpgrades);
                            break;
                        }else{
                            if (PlayerScore.get(p) >=upgrade.getPrice()) {
                                if (upgrade.getName().equalsIgnoreCase("Reroll")){
                                    RandomPUpgrades.put(p,null);
                                    UpgradesInThisRound.put(p,null);
                                    PlayerScore.put(p, (float) (PlayerScore.get(p) - upgrade.getPrice()));
                                    break;
                                }else {
                                    PlayerScore.put(p, (float) (PlayerScore.get(p) - upgrade.getPrice()));
                                    p.sendMessage(ChatColor.GREEN + "Kupiono!!! Twoje $$$: " + PlayerScore.get(p));
                                    upgrade.setUnlocked(true);
                                    p.sendMessage(ChatColor.GREEN + "UPGRADE" );
                                    break;
                                }}else{
                                p.sendMessage(ChatColor.RED+"Za mało $$$  Cena:" +upgrade.getPrice());
                                break;
                            }}}}}}
        OpenAssaultMenu(player);
    }

    public void OpenAssaultMenu(Player player) {
        if (secondsElapsed < RestTimer) {
            if (RandomPUpgrades.get(player)==null || UpgradesInThisRound.get(player)==null){
                IndividualShop.clear();
                if (RandomPUpgrades.get(player)==null){
                    List<Upgrades> UpgradesOfPlayer = IndividualUpgrades.getOrDefault(player, new ArrayList<>());
                    List<Upgrades> RandomUpgrades = new ArrayList<>();
                    List<Upgrades> eligibleUpgrades = new ArrayList<>();
                    List<String> SelectedPath = new ArrayList<>();
                    for (SkillTreeObj STO: PGrowth.SkillPoints) {
                        if (STO.getPlayer().equalsIgnoreCase(player.getName())){
                            SelectedPath.addAll(STO.getSelectedPath());
                            break;
                        }}
                    for (Upgrades up : GeneralUpgradesList) {
                        if (!SelectedPath.contains(up.getName())&&up.isBlessing() && !UpgradesOfPlayer.contains(up) &&
                                (up.getReqUpgrades() == null || UpgradesOfPlayer.stream().anyMatch(up.getReqUpgrades()::contains))) {
                            eligibleUpgrades.add(up);
                        }}
                    Collections.shuffle(eligibleUpgrades);
                    for (Upgrades up : eligibleUpgrades.subList(0, Math.min(5, eligibleUpgrades.size()))) {
                        RandomUpgrades.add(up);
                    }
                    RandomPUpgrades.put(player,RandomUpgrades);
                }
                int i =2;
                for (Upgrades up:RandomPUpgrades.get(player)) {
                    IndividualShop.setItem(i, up.getItemStackReturn());
                    i++;
                }
                player.openInventory(IndividualShop);

            }else{
                for (Upgrades up:GeneralUpgradesList) {
                    if (up.getInmenu() == 1) GeneralShop.setItem(up.getInmenu(),up.SetName(ChatColor.RED+"Ulepszenie Wybuchu"));
                    if (up.getInmenu() == 7) GeneralShop.setItem(up.getInmenu(),up.SetName(ChatColor.GOLD+"Wybierz kolejne Wzmocnienie"));
                    if (up.getInmenu() == 8) GeneralShop.setItem(up.getInmenu(),up.SetName(ChatColor.GOLD+"Punkty: "+PlayerScore.get(player)));
                }
                player.openInventory(GeneralShop);
            }
        }else {
            player.sendMessage(ChatColor.RED + "Ulepszenia można nabyć tylko między Falami!!!");
        }
    }

    public String getName() {
        return name;
    }
    public Location getArenaLocation() {
        return ArenaLocation;
    }
}
*/
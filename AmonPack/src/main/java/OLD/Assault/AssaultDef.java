package OLD.Assault;
/*
import Mechanics.PVP.PvP;
import UtilObjects.PVE.Wave;
import commands.Commands;
import methods_plugins.AmonPackPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;

import static OLD.Assault.AssaultMethods.*;
import static OLD.Assault.AssaultMethods.CurrentObjectiveUpdater;


public class AssaultDef {

    private final int maxrounds;
    private int currentround2;
    private int currentHp;
    private final int MaxHp;
    private int currentkillcounter;
    private final int DamageRadius;
    private final List<EntityType>AllowedEntity = new ArrayList<>();
    private int TotalWaveKills;
    private int attackDuration;
    private boolean attackInProgress;
    private List<Wave> waveList;
    private final String name;
    private final Location ArenaLocation;
    private final Location Shop_Location;
    private final int range;
    private final String Command;
    private final int RestTimer;
    private final Map<Player, BossBar> playerBossBars = new HashMap<>();
    private String CurrentTitle;
    private BukkitTask TaskEverySecond;
    private BukkitTask TaskAfterRest;
    private int RestTask;
    private ArmorStand Shop;
    private final Inventory GeneralShop;
    private final Inventory IndividualShop;
    public int secondsElapsed;
    private final Map<Player, Float> PlayerScore = new HashMap<>();
    private List<Upgrades> GeneralUpgradesList;
    public final Map<Player, List<String>> BonusAbilities = new HashMap<>();
    public final Map<Player, List<Upgrades>> IndividualUpgrades = new HashMap<>();
    private final Map<Player, List<Upgrades>> RandomPUpgrades = new HashMap<>();
    private final Map<Player,Boolean> UpgradesInThisRound = new HashMap<>();

    public AssaultDef(String name, Location startloc, Location shop_Location, int range, int maxw, List<Wave> waves, int maxHp, String command, int restTimer, int damageRadius) {
        GeneralUpgradesList = AssaultMethods.Upgrades;
        MaxHp = maxHp;
        Command = command;
        RestTimer = restTimer;
        DamageRadius = damageRadius;
        this.name = name;
        this.range = range;
        ArenaLocation = startloc;
        Shop_Location = shop_Location;
        maxrounds = maxw;
        waveList = waves;
        //maxrounds = maxw;
        //maxrounds = uniqueWaveNumbers.size();
        GeneralShop = Bukkit.createInventory(null, 18, "AssaultGui");
        IndividualShop = Bukkit.createInventory(null, 18, "AssaultGui");
        TaskEverySecond = Bukkit.getScheduler().runTaskTimer(AmonPackPlugin.plugin, this::AssaultTimer, 0L, 20L);
    }
    public void AssaultTimer() {
        if (!PInArena(ArenaLocation,range,range).isEmpty()){
                attackInProgress = false;
                for (Entity entity : Objects.requireNonNull(ArenaLocation.getWorld()).getNearbyEntities(ArenaLocation, DamageRadius, 100, DamageRadius)) {
                    if (entity instanceof Player || entity instanceof ArmorStand || AllowedEntity.contains(entity.getType())) {
                        continue;
                    }
                    if (entity instanceof LivingEntity) {
                        attackInProgress = true;
                        break;
                    }}
                if (attackInProgress) {
                    attackDuration++;
                }else{
                    attackDuration=0;
                }
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
                    if (attackInProgress){
                        Create1SecondZone(ArenaLocation.clone().subtract(0,1,0),DamageRadius,Material.REDSTONE_BLOCK);
                    }else{
                        Create1SecondZone(ArenaLocation.clone().subtract(0,1,0),DamageRadius,Material.EMERALD_BLOCK);
                    }
                }
                if (attackInProgress && attackDuration >= 5) {
                    currentHp = currentHp - 1;
                    attackDuration=0;
                    Shop.setCustomName(ChatColor.DARK_PURPLE+"Twoje życie:" + currentHp);
                    for (Player p : PInArena(ArenaLocation, range, range)) {
                        PvP.sendTitleMessage(p, ChatColor.RED + "Atakują!", ChatColor.YELLOW + "Zdrowie: " + currentHp+"/"+MaxHp, 20,20,10);
                    }
                    if (GetUpgradeByMaterialAndName(Material.FIRE_CHARGE,"ZoneExplosion",GeneralUpgradesList).isUnlocked()){
                        ArenaLocation.getWorld().createExplosion(ArenaLocation, 8f, false, false);
                        for (Entity nearbyEntity : ArenaLocation.getWorld().getNearbyEntities(ArenaLocation, DamageRadius, 100, DamageRadius)) {
                            if (nearbyEntity instanceof LivingEntity) {
                                if (!(nearbyEntity instanceof Player)){
                                    Vector direction = nearbyEntity.getLocation().toVector().subtract(ArenaLocation.clone().subtract(0,4,0).toVector()).normalize();
                                    nearbyEntity.setVelocity(direction.multiply(1.2));
                                    ((LivingEntity) nearbyEntity).damage(2);
                                }}}
                    }

                }
            }else{
            if (TaskEverySecond != null){
                TaskEverySecond.cancel();
            }
        }
        if (!playerBossBars.isEmpty()) {
            for (Player p : playerBossBars.keySet()) {
                    if(attackInProgress){
                        BossBar bossBar = playerBossBars.get(p);
                        bossBar.setColor(BarColor.RED);
                    }else{
                        BossBar bossBar = playerBossBars.get(p);
                        bossBar.setColor(BarColor.GREEN);
                    }
                if (!InArenaRange(p.getLocation(), ArenaLocation, range, range)) {
                    removePrivateBossBar(p,playerBossBars);
                } else {
                    if (!playerBossBars.get(p).getTitle().equalsIgnoreCase(CurrentTitle) && !playerBossBars.get(p).getTitle().startsWith("Przygotuj")) {
                        CurrentObjectiveUpdater(p,playerBossBars,CurrentTitle);
                    }}}}
    }
    public void Start(Player p) {
        AssaultMethods.ResetAddonAbis(p);
        BonusAbilities.clear();
        IndividualUpgrades.clear();
        RandomPUpgrades.clear();
        for (Upgrades up:GeneralUpgradesList) {
            up.setUnlocked(false);
            }
        Bukkit.getScheduler().cancelTask(RestTask);
        if (TaskAfterRest != null){
            TaskAfterRest.cancel();
        }
        if (TaskEverySecond != null){
            TaskEverySecond.cancel();
        }
        TaskEverySecond = Bukkit.getScheduler().runTaskTimer(AmonPackPlugin.plugin, this::AssaultTimer, 0L, 20L);
        Commands.ExecuteCommandExample example = new Commands.ExecuteCommandExample();
        example.executeCommand(Command);
        ClearArena();
        Shop = (ArmorStand) Objects.requireNonNull(Shop_Location.getWorld()).spawnEntity(Shop_Location, EntityType.ARMOR_STAND);
        Shop.setCustomNameVisible(true);
        Shop.setGravity(false);
        Shop.setVisible(false);
        Shop.setCustomName(ChatColor.DARK_PURPLE+"Ulepszenia");
            currentHp=MaxHp;
            currentround2=0;
            currentkillcounter=0;
            NextWave();
        CurrentObjectiveUpdater(p,playerBossBars,CurrentTitle);
        p.removePotionEffect(PotionEffectType.NIGHT_VISION);
        p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, true, false));
        p.teleport(ArenaLocation);
        PlayerScore.clear();
        UpgradesInThisRound.clear();
        PlayerScore.put(p,200f);
    }


    private void NextWave(){
        RandomPUpgrades.clear();
        for (Player p:PlayerScore.keySet()) {
            PlayerScore.put(p,PlayerScore.get(p)+300f);
        }
        List<Player>PlayersInDungeon = PInArena(ArenaLocation,range,range);
        Shop.setCustomName(ChatColor.DARK_PURPLE+"Ulepszenia");
        currentround2++;
        currentkillcounter=0;
        secondsElapsed=0;
        TotalWaveKills=0;
            TaskAfterRest = Bukkit.getScheduler().runTaskLater(AmonPackPlugin.plugin, () -> {
                for (Wave W: waveList) {
                    if (currentround2<=W.getWaveDifficultyLvL()){
                        TotalWaveKills = W.SpawnMobAndTP(currentround2);
                        break;
                    }}
                CurrentTitle = "Runda: "+currentround2+" Zabiles: " +currentkillcounter+"/"+TotalWaveKills;
                if (secondsElapsed >= RestTimer) {
                    Shop.setCustomName(ChatColor.DARK_PURPLE+"Twoje życie:" + currentHp);
                    for (Player p:PlayersInDungeon) {
                        p.setAbsorptionAmount(0);
                        CurrentObjectiveUpdater(p,playerBossBars,CurrentTitle);
                        PvP.sendTitleMessage(p, ChatColor.RED + "Nowa Fala Nachodzi", ChatColor.YELLOW + "Obecna Runda: " + currentround2+"/"+maxrounds, 20,60,20);
                        p.setHealth(p.getMaxHealth());
                        List<Upgrades> PlayerUpgrades = new ArrayList<>();
                        if (IndividualUpgrades.get(p)!=null){
                            PlayerUpgrades = IndividualUpgrades.get(p);
                        }
                        updatePrivateBossBar(p,(RestTimer-secondsElapsed), RestTimer,"Przygotuj się! Fala "+currentround2+" za: "+(RestTimer-secondsElapsed),playerBossBars);
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
                    }
                }}, 20L *RestTimer);
        Bukkit.getScheduler().cancelTask(RestTask);
        RestTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(AmonPackPlugin.plugin, () -> {
            if (secondsElapsed < RestTimer) {
                secondsElapsed++;
                for (Player p:PlayersInDungeon) {
                    updatePrivateBossBar(p,(RestTimer-secondsElapsed), RestTimer,"Przygotuj się! Fala "+currentround2+" za: "+(RestTimer-secondsElapsed),playerBossBars);
                }} else {
                Bukkit.getScheduler().cancelTask(RestTask);
            }
        }, 0, 20L);
        CurrentTitle = "Runda: "+currentround2+" Zabiles: " +currentkillcounter+"/"+TotalWaveKills;
    }


    public void ActivateByKill(EntityType KilledEntity){
            if (!AllowedEntity.contains(KilledEntity)){
                currentkillcounter++;
                if (currentkillcounter>=TotalWaveKills) {
                    for (Player p : PInArena(ArenaLocation, range, range)) {
                        if (currentround2 >= maxrounds) {
                            p.sendMessage("UKONCZYLES DEFENDERA WOW !!!!");
                            ClearArena();
                            removePrivateBossBar(p,playerBossBars);
                            End();
                        }else{
                            p.resetTitle();
                            PvP.sendTitleMessage(p, ChatColor.GREEN + "Fala Ukończona!", ChatColor.YELLOW + "Obecna Runda: " + currentround2+"/"+maxrounds, 20,60,20);
                            UpgradesInThisRound.clear();
                            NextWave();
                        }}
                } else {
                            CurrentTitle = "Runda: " + currentround2 + " Zabiles: " + currentkillcounter + "/" + TotalWaveKills;
                        }}
    }


    public void End(){
        BonusAbilities.clear();
        IndividualUpgrades.clear();
        RandomPUpgrades.clear();
        PlayerScore.clear();
        currentround2=0;
        currentkillcounter=0;
        currentHp=0;
        Shop.remove();
        for (Upgrades up:GeneralUpgradesList) {
            up.setUnlocked(false);
        }
        Bukkit.getScheduler().cancelTask(RestTask);
        if (TaskAfterRest != null){
            TaskAfterRest.cancel();
        }
        if (TaskEverySecond != null){
            TaskEverySecond.cancel();
        }
        ClearArena();
    }
    public void ClearArena(){
        for (Entity entity : Objects.requireNonNull(ArenaLocation.getWorld()).getEntities()) {
            if (entity.getWorld().equals(ArenaLocation.getWorld()) && !entity.getType().equals(EntityType.PLAYER)){
                Location l = entity.getLocation();
                if ((l.getX() < ArenaLocation.getX()+range && l.getX() > ArenaLocation.getX()-range)&&(l.getZ() < ArenaLocation.getZ()+range && l.getZ() > ArenaLocation.getZ()-range)) {
                    entity.remove();
                }}}
    }

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
                                }else if (upgrade.getName().equalsIgnoreCase("Heal")){
                                    if (currentHp<MaxHp){
                                        currentHp++;
                                        PlayerScore.put(p, (float) (PlayerScore.get(p) - upgrade.getPrice()));
                                        p.sendMessage(ChatColor.GREEN + "Kupiono!!! Twoje $$$: " + PlayerScore.get(p));
                                    }else{
                                        p.sendMessage(ChatColor.RED+"Maksymalna ilość życia");
                                    }
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
                    if (up.getInmenu() == 0 && !up.isBlessing()) GeneralShop.setItem(up.getInmenu(), up.SetName("Zycie: " + currentHp + "/" + MaxHp));
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
    public Location getShop_Location() {
        return Shop_Location;
    }
    public String getCommand() {
        return Command;
    }
    public int getRange() {
        return range;
    }
}
*/
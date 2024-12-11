package Mechanics.PVE.Menagerie.Objectives;

import Mechanics.PVE.Menagerie.Menagerie;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static Mechanics.Skills.BendingGuiMenu.FastEasyStack;


public class ObjectiveConditions {
    Location ActivationLoc;
    double ActivationRange;
    Enemy Enemies;
    List<Enemy> EnemiesMultiKill;
    int EnemyKilledCounter;
    int TotalSpawnedEnemies;
    ConditionType CType;
    ItemStack CollectItemInHand;
    Material CollectClickedMat;

    int ClickCounter;
    int ReqClicks;

    boolean IsChecked;
    int CurrentCounter;
    int ZoneChargeTime;
    ItemStack CollectItemRecived;
    Material ZoneMaterial;


    public ObjectiveConditions(Location activationLoc, double activationRange) {
        ActivationLoc = activationLoc;
        ActivationRange = activationRange;
        CType = ConditionType.LOCATION;
        IsChecked=false;
    }
    public ObjectiveConditions(ConditionType type) {
        CType = type;
        IsChecked=false;
    }

    public ObjectiveConditions(Location activationLoc, double activationRange, int zoneChargeTime, Material zoneMaterial) {
        ActivationLoc = activationLoc;
        ActivationRange = activationRange;
        ZoneChargeTime = zoneChargeTime;
        ZoneMaterial = zoneMaterial;
        CType = ConditionType.ZONE;
        IsChecked=false;
        CurrentCounter=0;
    }

    public ObjectiveConditions(Enemy enemies) {
        Enemies = enemies;
        CType = ConditionType.KILL;
        EnemyKilledCounter=0;
        IsChecked=false;
    }
    public ObjectiveConditions(List<Enemy> enemies) {
        EnemiesMultiKill = enemies;
        CType = ConditionType.MULTIKILL;
        EnemyKilledCounter=0;
        IsChecked=false;

    }

    public ObjectiveConditions(Location activationLoc, double activationRange, ItemStack collectItemInHand, Material collectClickedMat) {
        ActivationLoc = activationLoc;
        ActivationRange = activationRange;
        CollectItemInHand = collectItemInHand;
        CollectClickedMat = collectClickedMat;
        CType = ConditionType.INTERACT;
        ClickCounter=0;
        IsChecked=false;

    }
    public ObjectiveConditions(){
        CType = ConditionType.READY;
    }

    public ObjectiveConditions(Location activationLoc, double activationRange, Material collectClickedMat, ItemStack collectItemRecived) {
        ActivationLoc = activationLoc;
        ActivationRange = activationRange;
        CollectClickedMat = collectClickedMat;
        CollectItemRecived = collectItemRecived;
        CType = ConditionType.COLLECT;
        IsChecked=false;

    }

    public void CheckConditions(Menagerie menagerie){
        switch (CType){
            case LOCATION:
                if (!menagerie.getPlayersInRadius(ActivationLoc,ActivationRange).isEmpty()){
                    IsChecked= true;
                }
                break;
            case ALLPLAYERSLOC:
                if (menagerie.getPlayersInRadius(ActivationLoc,ActivationRange).size()==menagerie.PlayersInMenagerie().size()){
                    IsChecked= true;
                }
                break;
            case ZONE:
                if(!isChecked()){
                    menagerie.Create1SecondZone(ActivationLoc, (int) ActivationRange,ZoneMaterial);
                }
                if(CurrentCounter>=ZoneChargeTime){
                    IsChecked= true;
                    CurrentCounter=0;
                }
                if (!menagerie.getPlayersInRadius(ActivationLoc,ActivationRange).isEmpty()){
                    CurrentCounter++;
                    for (Player p:menagerie.getPlayersInRadius(ActivationLoc,ActivationRange)) {
                        menagerie.TitleChange(p,"Przejęto: "+CurrentCounter+"/"+ZoneChargeTime, BarColor.RED, BarStyle.SOLID,(1.0f/ZoneChargeTime)*CurrentCounter,1);
                    }
                }else{
                    CurrentCounter=0;
                }
                break;
            case READY:
                if (menagerie.getActiveEncounter().getReadyWaitingDoors().IsClosed && menagerie.getReadyPlayers().values().stream().allMatch(ready -> ready)) {
                    IsChecked= true;
                }
                break;
            case NOMOBS:
                if (menagerie.GetEnemiesInDung().isEmpty()) {
                    IsChecked= true;
                }
                break;
        }
    }
    public void CheckConditions(Entity entity,Menagerie menagerie){
        switch (CType){
            case KILL:
                if (Enemies.getEnemyDisplayName().contains(entity.getName().substring(4)) && Enemies.getEnemyType().equalsIgnoreCase(entity.getType().name())){
                    EnemyKilledCounter++;
                    if (Enemies.getAmount()<=EnemyKilledCounter){
                        EnemyKilledCounter=0;
                        IsChecked= true;
                    }}
                break;
            case MULTIKILL:
                List<String>NamesOfEnemies=new ArrayList<>();
                List<String>TypesOfEnemies=new ArrayList<>();
                for (Enemy E:EnemiesMultiKill) {
                    NamesOfEnemies.add(E.getEnemyDisplayName().toLowerCase());
                    TypesOfEnemies.add(E.getEnemyType().toLowerCase());
                }
                TotalSpawnedEnemies=0;
                for (Enemy E:EnemiesMultiKill) {
                    TotalSpawnedEnemies=TotalSpawnedEnemies+E.getAmount();
                }
                if (NamesOfEnemies.contains(entity.getName().substring(4).toLowerCase()) && TypesOfEnemies.contains(entity.getType().name().toLowerCase())){
                    EnemyKilledCounter++;
                    if (TotalSpawnedEnemies<=EnemyKilledCounter){
                        EnemyKilledCounter=0;
                        IsChecked= true;
                    }}
                break;
        }
    }
    public void CheckConditions(PlayerInteractEvent e,Menagerie menagerie){
        switch (CType){
            case INTERACT:
                if(e.getClickedBlock()!=null){
                    if (e.getClickedBlock().getType().equals(CollectClickedMat)){
                        if(CollectItemInHand==null){
                            fkinnullerror(e);
                        }else if(e.getItem()!=null &&e.getItem().isSimilar(CollectItemInHand)){
                            fkinnullerror(e);
                        }
                        }}
                break;
            case COLLECT:
                if(e.getClickedBlock()!=null){
                    if (e.getClickedBlock().getType().equals(CollectClickedMat)){
                        if(e.getPlayer().getLocation().distance(ActivationLoc)<=ActivationRange){
                            if(CollectItemRecived!=null){
                                e.getPlayer().getInventory().addItem(CollectItemRecived);
                            }
                            IsChecked= true;
                        }}}
                break;
        }
    }

    private void fkinnullerror(PlayerInteractEvent e){
        if(e.getPlayer().getLocation().distance(ActivationLoc)<=ActivationRange){
            Inventory inventory = e.getPlayer().getInventory();
            ItemStack[] items = inventory.getContents();
            for (int i = 0; i < items.length; i++) {
                if (items[i] != null && items[i].isSimilar(CollectItemInHand)) {
                    int newAmount = items[i].getAmount() - 1;
                    if (newAmount > 0) {
                        items[i].setAmount(newAmount);
                        inventory.setItem(i, items[i]);
                    } else {
                        inventory.setItem(i, null);
                    }
                    break;
                }}

            if(CollectItemInHand!=null){
                ClickCounter++;
            if(ClickCounter>=CollectItemInHand.getAmount()){
                IsChecked= true;
                ClickCounter=0;
            }}else{
                IsChecked= true;
            }
        }
    }
    public enum ConditionType {
        LOCATION,
        ALLPLAYERSLOC,
        KILL,
        INTERACT,
        COLLECT,
        ZONE,
        MULTIKILL,
        READY,
        NOMOBS
    }

    public int getTotalSpawnedEnemies() {
        return TotalSpawnedEnemies;
    }
    public int getEnemyKilledCounter() {
        return EnemyKilledCounter;
    }
    public void setChecked(boolean checked) {
        IsChecked = checked;
    }
    public boolean isChecked() {
        return IsChecked;
    }
    public Location getActivationLoc() {
        return ActivationLoc;
    }
    public double getActivationRange() {
        return ActivationRange;
    }
    public Enemy getEnemies() {
        return Enemies;
    }
    public ConditionType getCType() {
        return CType;
    }

    public int getClickCounter() {
        return ClickCounter;
    }

    public void setClickCounter(int clickCounter) {
        ClickCounter = clickCounter;
    }

    public int getReqClicks() {
        return ReqClicks;
    }

    public void setReqClicks(int reqClicks) {
        ReqClicks = reqClicks;
    }
}

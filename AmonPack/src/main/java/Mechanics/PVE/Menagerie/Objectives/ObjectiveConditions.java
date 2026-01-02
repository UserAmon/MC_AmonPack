package Mechanics.PVE.Menagerie.Objectives;

import Mechanics.PVE.Menagerie.Menagerie;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

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
        IsChecked = false;
    }

    public ObjectiveConditions(ConditionType type) {
        CType = type;
        IsChecked = false;
    }

    public ObjectiveConditions(Location activationLoc, double activationRange, int zoneChargeTime,
            Material zoneMaterial) {
        ActivationLoc = activationLoc;
        ActivationRange = activationRange;
        ZoneChargeTime = zoneChargeTime;
        ZoneMaterial = zoneMaterial;
        CType = ConditionType.ZONE;
        IsChecked = false;
        CurrentCounter = 0;
    }

    public ObjectiveConditions(Enemy enemies) {
        Enemies = enemies;
        CType = ConditionType.KILL;
        EnemyKilledCounter = 0;
        IsChecked = false;
    }

    public ObjectiveConditions(List<Enemy> enemies) {
        EnemiesMultiKill = enemies;
        CType = ConditionType.MULTIKILL;
        EnemyKilledCounter = 0;
        IsChecked = false;
    }

    public ObjectiveConditions(Location activationLoc, double activationRange, ItemStack collectItemInHand,
            Material collectClickedMat) {
        ActivationLoc = activationLoc;
        ActivationRange = activationRange;
        CollectItemInHand = collectItemInHand;
        CollectClickedMat = collectClickedMat;
        CType = ConditionType.INTERACT;
        ClickCounter = 0;
        IsChecked = false;
    }

    public ObjectiveConditions() {
        CType = ConditionType.READY;
    }

    public ObjectiveConditions(Location activationLoc, double activationRange, Material collectClickedMat,
            ItemStack collectItemRecived) {
        ActivationLoc = activationLoc;
        ActivationRange = activationRange;
        CollectClickedMat = collectClickedMat;
        CollectItemRecived = collectItemRecived;
        CType = ConditionType.COLLECT;
        IsChecked = false;
    }

    public void CheckConditions(Menagerie menagerie) {
        switch (CType) {
            case LOCATION:
                if (!menagerie.getPlayersInRadius(ActivationLoc, ActivationRange).isEmpty()) {
                    CurrentCounter++;
                    for (Player p : menagerie.getPlayersInRadius(ActivationLoc, ActivationRange)) {
                        menagerie.TitleChange(p, "Przejęto: " + CurrentCounter + "/" + ZoneChargeTime, BarColor.RED,
                                BarStyle.SOLID, (1.0f / ZoneChargeTime) * CurrentCounter, 1);
                    }
                    if (CurrentCounter >= ZoneChargeTime) {
                        IsChecked = true;
                    }
                } else {
                    CurrentCounter = 0;
                }
                break;
            case READY:
                if (menagerie.getActiveEncounter() != null
                        && menagerie.getActiveEncounter().getReadyWaitingDoors() != null
                        && menagerie.getActiveEncounter().getReadyWaitingDoors().IsClosed) {
                    IsChecked = true;
                }
                break;
            default:
                break;
        }
    }

    public void CheckConditions(Entity entity, Menagerie menagerie) {
        switch (CType) {
            case KILL:
                if (Enemies.getEnemyDisplayName().contains(entity.getName().substring(4))
                        && Enemies.getEnemyType().equalsIgnoreCase(entity.getType().name())) {
                    EnemyKilledCounter++;
                    if (Enemies.getAmount() <= EnemyKilledCounter) {
                        EnemyKilledCounter = 0;
                        IsChecked = true;
                    }
                }
                break;
            case MULTIKILL:
                List<String> NamesOfEnemies = new ArrayList<>();
                List<String> TypesOfEnemies = new ArrayList<>();
                for (Enemy E : EnemiesMultiKill) {
                    NamesOfEnemies.add(E.getEnemyDisplayName().toLowerCase());
                    TypesOfEnemies.add(E.getEnemyType().toLowerCase());
                }
                TotalSpawnedEnemies = 0;
                for (Enemy E : EnemiesMultiKill) {
                    TotalSpawnedEnemies = TotalSpawnedEnemies + E.getAmount();
                }
                if (NamesOfEnemies.contains(entity.getName().substring(4).toLowerCase())
                        && TypesOfEnemies.contains(entity.getType().name().toLowerCase())) {
                    EnemyKilledCounter++;
                    if (TotalSpawnedEnemies <= EnemyKilledCounter) {
                        EnemyKilledCounter = 0;
                        IsChecked = true;
                    }
                }
                break;
            default:
                break;
        }
    }

    public void CheckConditions(PlayerInteractEvent e, Menagerie menagerie) {
        switch (CType) {
            case INTERACT:
                if (e.getClickedBlock() != null && e.getClickedBlock().getType() == CollectClickedMat
                        && e.getClickedBlock().getLocation().distance(ActivationLoc) <= ActivationRange) {
                    if (CollectItemInHand != null) {
                        if (e.getItem() != null && e.getItem().isSimilar(CollectItemInHand)) {
                            ClickCounter++;
                            if (ClickCounter >= CollectItemInHand.getAmount()) {
                                IsChecked = true;
                                ClickCounter = 0;
                            }
                        }
                    } else {
                        IsChecked = true;
                    }
                }
                break;
            case COLLECT:
                // Logic for collect
                break;
            default:
                break;
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

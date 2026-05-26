package Abilities.Util_Objects;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.util.ParticleEffect;

import Abilities.Bending.*;
import Plugin.AmonPackPlugin;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class SmokeSource {
    private Location location;
    private int duration;
    private int maxduration;
    private double range;
    private double Yrange;
    private boolean isSelected;
    private Player player;
    private SmokeSource ThisSource = this;
    private boolean IsUsable;
    private boolean IsPulled;
    private long SpeedP = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Fire.SmokeSource.SpeedPower");
    private long SlowP = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Fire.SmokeSource.SlowPower");

    public SmokeSource(Location location, int maxduration, double range, double yrange, Player p) {
        this.location = location;
        this.maxduration = maxduration;
        this.range = range;
        Yrange = yrange;
        duration = 0;
        isSelected = false;
        CreateSmoke();
        player = p;
        IsUsable = false;
        IsPulled = false;
        SmokeAbility.AddSmokeSource(this);
    }

    public SmokeSource(Location location, int maxduration, double yrange, double range, Player player, boolean IsSafe) {
        this.location = location;
        this.maxduration = maxduration;
        Yrange = yrange;
        this.range = range;
        this.player = player;
        duration = 0;
        CreateSmoke();
        IsUsable = IsSafe;
    }

    private void CreateSmoke() {
        new BukkitRunnable() {
            @Override
            public void run() {
                duration++;
                if (IsPulled) {
                    if (duration >= 3) {
                        SmokeAbility.DeleteSource(ThisSource);
                        this.cancel();
                        return;
                    }
                }
                if (duration >= maxduration) {
                    SmokeAbility.DeleteSource(ThisSource);
                    this.cancel();
                    return;
                }
                
                double scaledRange = range * (1 - ((double) duration / maxduration));
                for (Entity entity : GeneralMethods.getEntitiesAroundPoint(location, range * scaledRange)) {
                    // BOOrst upgrade check
                    if (entity instanceof Player) {
                        Player p = (Player) entity;
                        com.projectkorra.projectkorra.BendingPlayer bp = com.projectkorra.projectkorra.BendingPlayer.getBendingPlayer(p);
                        if (bp != null && bp.getAbilities().containsValue("SmokeBurst")) {
                            RPG.Levels.BendingTree.PlayerBendingBranch branch = AmonPackPlugin.levelsBending.GetBranchByPlayerName(p.getName());
                            if (branch != null && branch.hasUpgrade("BOOrst")) {
                                Abilities.PK_Abilities.Fire.SmokeBurst.explodeSmokeSource(p, location);
                                SmokeAbility.DeleteSource(ThisSource);
                                this.cancel();
                                return;
                            }
                        }
                    }
                    
                    if (IsUsable) {
                        if ((entity instanceof LivingEntity) && entity.getUniqueId() == player.getUniqueId()) {
                            if (entity.getLocation().getY() <= location.getY() + Yrange + 1) {
                                ((LivingEntity) entity).addPotionEffect(
                                        new PotionEffect(PotionEffectType.SPEED, 5, 4, false, false, false));
                            }
                        }
                    } else {
                        if ((entity instanceof LivingEntity) && entity.getUniqueId() != player.getUniqueId()) {
                            if (entity.getLocation().getY() <= location.getY() + Yrange + 1) {
                                ((LivingEntity) entity).addPotionEffect(
                                        new PotionEffect(PotionEffectType.SLOWNESS, 30, 2, false, false, false));
                            }
                        }
                    }
                }
                if (isSelected) {
                    ParticleEffect.CAMPFIRE_COSY_SMOKE.display(location, (int) (scaledRange * 2), (scaledRange * 0.1),
                            Yrange, (scaledRange * 0.1), 0);
                    ParticleEffect.VILLAGER_ANGRY.display(location, (int) (scaledRange * 3.5), (scaledRange * 0.4),
                            Yrange, (scaledRange * 0.4), 0);
                    ParticleEffect.SMOKE_NORMAL.display(location, (int) (scaledRange * 12), (scaledRange * 0.5), Yrange,
                            (scaledRange * 0.5), 0.05);
                } else {
                    if (!IsUsable) {
                        ParticleEffect.CAMPFIRE_COSY_SMOKE.display(location, (int) (scaledRange * 2.5),
                                (scaledRange * 0.3), Yrange, (scaledRange * 0.3), 0);
                        ParticleEffect.SMOKE_NORMAL.display(location, (int) (scaledRange * 15), (scaledRange * 0.5),
                                Yrange, (scaledRange * 0.5), 0.05);
                    } else {
                        ParticleEffect.SMOKE_NORMAL.display(location, (int) (scaledRange * 5), (scaledRange * 0.5),
                                Yrange, (scaledRange * 0.5), 0.05);

                    }
                }
            }
        }.runTaskTimer(AmonPackPlugin.plugin, 0, 4);
    }

    public Location getLocation() {
        return location;
    }

    public void Select() {
        isSelected = true;
    }

    public void AdvanceLocation(Location loc) {
        isSelected = false;
        duration = 0;
        this.location = loc.clone().add(0, 0.5, 0);
    }

    public boolean IsNearPlayer(Location playerloc, double speed, Player player) {
        IsPulled = true;
        double dmg = 1.0;
        if (player != null) {
            RPG.Levels.BendingTree.PlayerBendingBranch branch = AmonPackPlugin.levelsBending.GetBranchByPlayerName(player.getName());
            if (branch != null && branch.hasUpgrade("Strategist")) {
                dmg += 3.0;
            }
        }
        for (Entity entity : GeneralMethods.getEntitiesAroundPoint(location, range)) {
            if ((entity instanceof LivingEntity)) {
                if (entity.getUniqueId() != player.getUniqueId()) {
                    if (entity.getLocation().getY() <= location.getY() + Yrange) {
                        ((LivingEntity) entity).damage(dmg, player);
                    }
                }
            }
        }
        if (location.distance(playerloc) > 1) {
            location.add(GeneralMethods.getDirection(location, playerloc).normalize().multiply(speed));
            duration = 0;
            return false;
        }
        location = null;
        duration = maxduration;
        return true;
    }
}

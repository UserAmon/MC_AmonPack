package abilities.Util_Objects;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.Ability;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.ParticleEffect;
import com.projectkorra.projectkorra.util.TempBlock;
import methods_plugins.Abilities.SmokeAbility;
import methods_plugins.AmonPackPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
        duration=0;
        isSelected=false;
        CreateSmoke();
        player=p;
        IsUsable=false;
        IsPulled=false;
        SmokeAbility.AddSmokeSource(this);
    }

    public SmokeSource(Location location, int maxduration, double yrange, double range, Player player,boolean IsSafe) {
        this.location = location;
        this.maxduration = maxduration;
        Yrange = yrange;
        this.range = range;
        this.player = player;
        duration=0;
        CreateSmoke();
        IsUsable=IsSafe;
    }

    private void CreateSmoke() {
        new BukkitRunnable() {
            @Override
            public void run() {
                duration++;
                if(IsPulled){
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
                for (Entity entity : GeneralMethods.getEntitiesAroundPoint(location, range*scaledRange)) {
                    if(IsUsable){
                        if ((entity instanceof LivingEntity)&&entity.getUniqueId()==player.getUniqueId()) {
                            if (entity.getLocation().getY() <= location.getY()+Yrange+1) {
                                ((LivingEntity) entity).addPotionEffect(new PotionEffect(PotionEffectType.SPEED,5,4,false,false,false));
                            }}
                    }else{
                        if ((entity instanceof LivingEntity)&&entity.getUniqueId()!=player.getUniqueId()) {
                            if (entity.getLocation().getY() <= location.getY()+Yrange+1) {
                                ((LivingEntity) entity).addPotionEffect(new PotionEffect(PotionEffectType.SLOW,30,2,false,false,false));
                            }}
                    }
                    }
                if(isSelected){
                    ParticleEffect.CAMPFIRE_COSY_SMOKE.display(location,(int) (scaledRange * 2), (scaledRange*0.1), Yrange, (scaledRange*0.1), 0);
                    ParticleEffect.VILLAGER_ANGRY.display(location,(int) (scaledRange * 3.5), (scaledRange*0.4), Yrange, (scaledRange*0.4), 0);
                    ParticleEffect.SMOKE_NORMAL.display(location,(int) (scaledRange * 12), (scaledRange*0.5), Yrange, (scaledRange*0.5), 0.05);
                }else{
                if(!IsUsable){
                    ParticleEffect.CAMPFIRE_COSY_SMOKE.display(location,(int) (scaledRange * 2.5), (scaledRange*0.3), Yrange, (scaledRange*0.3), 0);
                    ParticleEffect.SMOKE_NORMAL.display(location,(int) (scaledRange * 15), (scaledRange*0.5), Yrange, (scaledRange*0.5), 0.05);
                }else{
                    ParticleEffect.SMOKE_NORMAL.display(location,(int) (scaledRange * 5), (scaledRange*0.5), Yrange, (scaledRange*0.5), 0.05);

                }
            }}
        }.runTaskTimer(AmonPackPlugin.plugin, 0, 4);
    }

    public Location getLocation() {
        return location;
    }
    public void Select(){
        isSelected=true;
    }
    public void AdvanceLocation(Location loc){
        isSelected=false;
        duration=0;
        this.location=loc.clone().add(0,0.5,0);
    }
    public boolean IsNearPlayer(Location playerloc, double speed, Player player) {
        IsPulled=true;
        for (Entity entity : GeneralMethods.getEntitiesAroundPoint(location, range)) {
            if ((entity instanceof LivingEntity)) {
                if (entity.getUniqueId() != player.getUniqueId()) {
                    if (entity.getLocation().getY() <= location.getY()+Yrange) {
                        ((LivingEntity) entity).damage(1);
                    }}}}
        if(location.distance(playerloc)>1){
            location.add(GeneralMethods.getDirection(location, playerloc).normalize().multiply(speed));
            duration=0;
            return false;
        }
        location=null;
        duration=maxduration;
        return true;
    }
}

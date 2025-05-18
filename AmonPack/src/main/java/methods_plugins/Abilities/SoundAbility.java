package methods_plugins.Abilities;

import abilities.Util_Objects.BetterParticles;
import abilities.Util_Objects.SmokeSource;
import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.Ability;
import com.projectkorra.projectkorra.ability.AirAbility;
import com.projectkorra.projectkorra.ability.FireAbility;
import com.projectkorra.projectkorra.ability.SubAbility;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.ParticleEffect;
import methods_plugins.AmonPackPlugin;
import org.bukkit.Color;
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
import java.util.Objects;
import java.util.stream.Collectors;

public abstract class SoundAbility extends AirAbility implements SubAbility {
    public static HashMap<Entity,Integer> AfffectedEntities = new HashMap<>();
    public SoundAbility(Player player) {
        super(player);
    }
    public Class<? extends Ability> getParentAbility() {
        return AirAbility.class;
    }
    public Element getElement() {
        return Element.SubElement.getElement("Sound");
    }
    public static void HandleDamage(Entity entity, int i){
        ParticleEffect.SPELL.display(entity.getLocation(),(10),1.4,1.7,1.4, Color.fromRGB(192, 192, 192));
        ParticleEffect.NOTE.display(entity.getLocation(),(10),1.4,1.7,1.4, Color.fromRGB(192, 192, 192));
        ParticleEffect.SPELL_MOB_AMBIENT.display(entity.getLocation(),(10),1.4,1.7,1.4, Color.fromRGB(192, 192, 192));
        if(!AfffectedEntities.isEmpty()&&AfffectedEntities.get(entity)!=null){
            int actual = AfffectedEntities.get(entity);
            AfffectedEntities.put(entity,actual+i);
        }else{
            AfffectedEntities.put(entity,i);
        }
    }
    public static void StartDeafnessTimer(){
        new BukkitRunnable() {
            @Override
            public void run() {
                List<Entity>ToRemove=new ArrayList<>();
                if(!AfffectedEntities.keySet().isEmpty()){
                    for (Entity entity:AfffectedEntities.keySet()){
                        if(entity!=null){
                        if(entity.isDead()){
                            ToRemove.add(entity);
                        }else{
                        int time = AfffectedEntities.get(entity);
                        if(time>0){
                            if(time>15){
                                ((LivingEntity) entity).damage(4);
                                ((LivingEntity)entity).addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS,40,3,false,false,false));
                                ((LivingEntity)entity).addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA,40,10,false,false,false));
                                ((LivingEntity)entity).addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,40,3,false,false,false));
                                ParticleEffect.SPELL.display(entity.getLocation(),(10),1.4,1.7,1.4, Color.fromRGB(192, 192, 192));
                                ParticleEffect.NOTE.display(entity.getLocation(),(10),1.4,1.7,1.4, Color.fromRGB(192, 192, 192));
                                ParticleEffect.SPELL_MOB_AMBIENT.display(entity.getLocation(),(10),1.4,1.7,1.4,Color.fromRGB(192, 192, 192));
                                ToRemove.add(entity);
                            }else{
                            ParticleEffect.SPELL.display(entity.getLocation(),(4*time),0.4,1.7,0.4, Color.fromRGB(192, 192, 192));
                            ParticleEffect.NOTE.display(entity.getLocation(),(2*time),0.4,1.7,0.4, Color.fromRGB(192, 192, 192));
                            ParticleEffect.SPELL_MOB_AMBIENT.display(entity.getLocation(),(4*time),0.4,1.7,0.4,Color.fromRGB(192, 192, 192));
                            AfffectedEntities.put(entity,time-1);
                        }}}}
                    }
                    for (Entity ent : ToRemove){
                        AfffectedEntities.remove(ent);
                    }
                }
            }
        }.runTaskTimer(AmonPackPlugin.plugin, 20, 20);
    }
}

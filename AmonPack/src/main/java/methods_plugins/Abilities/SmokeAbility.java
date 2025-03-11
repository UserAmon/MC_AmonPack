package methods_plugins.Abilities;
import abilities.Util_Objects.SmokeSource;
import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.ability.Ability;
import com.projectkorra.projectkorra.ability.FireAbility;
import com.projectkorra.projectkorra.ability.SubAbility;

import methods_plugins.AmonPackPlugin;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public abstract class SmokeAbility extends FireAbility implements SubAbility {
    static List<SmokeSource> ListOfSources = new ArrayList<>();
    public static HashMap<Player,SmokeSource> ListOfReadySources = new HashMap<>();
    public SmokeAbility(Player player) {
        super(player);
    }
    public Class<? extends Ability> getParentAbility() {
        return FireAbility.class;
    }
    public Element getElement() {
        return AmonPackPlugin.getSmokeElement();
    }
    public static void AddSmokeSource(SmokeSource source){
        ListOfSources.add(source);
    }
    public static void MakeSourceReady(Player player, SmokeSource source){
        source.Select();
        ListOfReadySources.put(player,source);
    }
    public static SmokeSource UseSmokeSource(Player player, double range) {
        Location location = player.getLocation();
        List<SmokeSource> sources = ListOfSources.stream().filter(source->source!=null &&source.getLocation().getWorld()!=null &&Objects.equals(source.getLocation().getWorld(), location.getWorld())&&source.getLocation().distance(location)<range+5).collect(Collectors.toList());
        Vector direction = player.getLocation().getDirection().clone().multiply(0.1D);
        Location loc = player.getEyeLocation().clone();
        Location startLoc = loc.clone();
        do {
            loc.add(direction);
            for (SmokeSource source : sources){
                    if(source.getLocation().distance(loc)<5){
                        source.Select();
                        ListOfSources.remove(source);
                        return source;
                }
            }
        } while(startLoc.distance(loc) < range && loc.getBlock().getBlockData().getMaterial() == Material.AIR);
        return null;
    }
    public static void DeleteSource(SmokeSource source){
        ListOfSources.remove(source);
    }
}

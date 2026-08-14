package Abilities.PK_Abilities.Earth;

import com.projectkorra.projectkorra.ability.AddonAbility;
import org.bukkit.entity.Player;

/**
 * @deprecated Reworked into {@link EarthBarricade}. Kept for backward compatibility.
 */
@Deprecated
public class Baricade extends EarthBarricade implements AddonAbility {

    public Baricade(Player player) {
        super(player);
    }

    @Override
    public String getName() {
        return "EarthBarricade";
    }
}

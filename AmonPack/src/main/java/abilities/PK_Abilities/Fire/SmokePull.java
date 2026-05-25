package Abilities.PK_Abilities.Fire;

import Abilities.Util_Objects.AbilityProjectile;
import Abilities.Util_Objects.SmokeSource;
import com.projectkorra.projectkorra.ability.AddonAbility;
import Abilities.Bending.SmokeAbility;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SmokePull extends SmokeAbility implements AddonAbility {

	private static Map<Player, Integer> absorbedMap = new HashMap<>();
	private List<AbilityProjectile> Projectiles;
	private Location origin;
	private long interval;
	int Absorbed;
	private State AbilityState;

	private enum State {
		BENDABLE,
		CHARGING,
		READY,
		USED
	}

	private SmokeSource Source;

	public SmokePull(Player player) {
		super(player);
		if (!this.bPlayer.isOnCooldown(getName()) && this.bPlayer.canBend(this)) {
			SmokeSource source = SmokeAbility.UseSmokeSource(player, 20);
			if (source != null) {
				Source = source;
				bPlayer.addCooldown(this);
				start();
			}
		}
	}

	@Override
	public void progress() {

		if (player.isDead() || !player.isOnline()) {
			remove();
			return;
		}
		if (System.currentTimeMillis() > getStartTime() + getCooldown() - 100) {
			remove();
			return;
		}
		interval++;
		if (interval >= 1) {
			interval = 0;
			if (Source.IsNearPlayer(player.getLocation(), 0.75, player)) {
				player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 3, false, false, false));
				remove();
			}
		}

	}

	@Override
	public long getCooldown() {
		return 5000;
	}

	@Override
	public Location getLocation() {
		return null;
	}

	@Override
	public String getName() {
		return "SmokePull";
	}

	@Override
	public String getAuthor() {
		return "AmonPack";
	}

	@Override
	public String getVersion() {
		return "1.0";
	}

	@Override
	public boolean isHarmlessAbility() {
		return false;
	}

	@Override
	public boolean isSneakAbility() {
		return true;
	}

	@Override
	public void load() {
	}

	@Override
	public void stop() {
		super.remove();
	}

	@Override
	public String getDescription() {
		return "This ability uses smoke source! Pull nearby smoke clouds toward you, damaging oponents that smoke passes through. Upon collecting smoke - gain speed boost!";
	}

	@Override
	public String getInstructions() {
		return "Left-click to pull targeted smoke cloud.";
	}

}
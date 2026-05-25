package Abilities.PK_Abilities.Air;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import Abilities.Bending.SoundAbility;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class Echo extends SoundAbility implements AddonAbility {
	private List<Entity> hited = new ArrayList<>();

	public Echo(Player player) {
		super(player);
		if (bPlayer.isOnCooldown(this)) {
			return;
		}
		if (!bPlayer.canBend(this)) {
			return;
		}
		Location origin = GeneralMethods.getMainHandLocation(player).clone();
		origin.setPitch(0);

		// Mniej pociskow, lekko sprezone (mniejszy kat) + lekki losowy pitch
		int offset = 0;
		for (int i = 1; i <= 4; i++) {
			offset += 6;
			Location multiL = origin.clone();
			Location multiR = origin.clone();
			multiL.setYaw(multiL.getYaw() + offset);
			multiR.setYaw(multiR.getYaw() - offset);
			
			// Dodaj losowy pitch (noise)
			multiL.setPitch(multiL.getPitch() + (float)(Math.random() * 20 - 10));
			multiR.setPitch(multiR.getPitch() + (float)(Math.random() * 20 - 10));
			
			new EchoProjectile(player, multiL, multiL.getDirection(), 8.0, 0, hited);
			new EchoProjectile(player, multiR, multiR.getDirection(), 8.0, 0, hited);
		}
		// Dodatkowy srodkowy
		new EchoProjectile(player, origin, origin.getDirection(), 8.0, 0, hited);

		bPlayer.addCooldown(this);
	}

	@Override
	public void progress() {
		remove(); // EchoProjectile zarządza się samodzielnie, Echo można od razu usunąć
	}

	@Override
	public long getCooldown() {
		return 3000;
	}

	@Override
	public Location getLocation() {
		return null;
	}

	@Override
	public String getName() {
		return "Echo";
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
		return false;
	}

	@Override
	public void load() {}

	@Override
	public void stop() {
		super.remove();
	}

	@Override
	public String getDescription() {
		return "Fires echoing sound waves that ricochet off walls. First hit builds up substantial acoustic pressure, while subsequent ricochets maintain and slightly increase it.";
	}

	@Override
	public String getInstructions() {
		return "Left-click to launch echoing sound waves.";
	}
}

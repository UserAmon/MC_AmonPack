package Abilities.PK_Abilities.Air;

import Plugin.AmonPackPlugin;
import Plugin.Methods;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.AirAbility;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.ParticleEffect;

public class AirPressure extends AirAbility implements AddonAbility {

	private long Cooldown = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Air.AirPressure.Cooldown", 5000L);
	private double dmg = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Air.AirPressure.Dmg", 3.0);
	private double sphererange = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Air.AirPressure.Range-Sphere", 15.0);
	private double pullrange = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Air.AirPressure.Range-Pull", 5.0);
	private double pushpower = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Air.AirPressure.PushPower", 2.0);
	private long mintime = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Air.AirPressure.MinHoldTime", 1000L);
	private boolean cancontrol = AmonPackPlugin.getAbilitiesConfig()
			.getBoolean("AmonPack.Air.AirPressure.CanControlSphere", true);
	private long maxtime = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Air.AirPressure.MaxHoldTime", 4000L);

	public Location preloc;
	public int abilityState;
	private int initialSlot;

	public AirPressure(Player player) {
		super(player);
		if (bPlayer.isOnCooldown(this)) {
			return;
		}
		if (!bPlayer.canBend(this)) {
			return;
		}
		this.initialSlot = player.getInventory().getHeldItemSlot();
		if (!cancontrol) {
			preloc = Methods.getTargetLocation(player, (int) sphererange).clone();
		}
		abilityState = 0;
		start();
	}

	@Override
	public void progress() {
		if (player.isDead() || !player.isOnline()) {
			remove();
			return;
		}

		if (player.getInventory().getHeldItemSlot() != initialSlot) {
			bPlayer.addCooldown(this);
			remove();
			return;
		}

		if (bPlayer.getBoundAbilityName() == null || !bPlayer.getBoundAbilityName().equalsIgnoreCase("AirPressure")) {
			bPlayer.addCooldown(this);
			remove();
			return;
		}

		if (cancontrol) {
			preloc = Methods.getTargetLocation(player, (int) sphererange).clone();
		}

		if (abilityState == 0) {
			if (player.isSneaking()) {
				if (System.currentTimeMillis() > getStartTime() + 300) {
					abilityState = 1;
				} else if (System.currentTimeMillis() < getStartTime() + mintime) {
					ParticleEffect.CLOUD.display(preloc, (int) (pullrange * 2), (float) (pullrange / 2), (float) (pullrange / 2),
							(float) pullrange, 0);
					for (Entity entity : GeneralMethods.getEntitiesAroundPoint(preloc, pullrange)) {
						Vector forceDir = GeneralMethods.getDirection(entity.getLocation(), preloc);
						entity.setVelocity(forceDir.clone().normalize().multiply(0.5));
					}
				}
			} else {
				bPlayer.addCooldown(this);
				abilityState = 0;
				remove();
			}
		} else if (abilityState == 1) {
			ParticleEffect.CLOUD.display(preloc, (int) (pullrange * 2), (float) (pullrange / 2), (float) (pullrange / 2), (float) (pullrange / 2),
					0);
			ParticleEffect.SMOKE_NORMAL.display(
					player.getEyeLocation().add(player.getLocation().getDirection().clone().multiply(1)), 1, 0.3f,
					0.3f, 0.3f, 0);
			for (Entity entity : GeneralMethods.getEntitiesAroundPoint(preloc, pullrange)) {
				Vector forceDir = GeneralMethods.getDirection(entity.getLocation(), preloc.clone().add(0, 1, 0));
				entity.setVelocity(forceDir.clone().normalize().multiply(0.5));
			}
			if (!player.isSneaking() || System.currentTimeMillis() > getStartTime() + maxtime) {
				abilityState = 0;
				for (Entity entity : GeneralMethods.getEntitiesAroundPoint(preloc, pullrange)) {
					Vector forceDir = GeneralMethods.getDirection(entity.getLocation(),
							preloc.clone().subtract(0, 2, 0));
					entity.setVelocity(forceDir.clone().normalize().multiply(-pushpower));
					if (entity.getUniqueId() != player.getUniqueId() && entity instanceof LivingEntity) {
						DamageHandler.damageEntity(entity, dmg, this);
					}
				}
				bPlayer.addCooldown(this);
				remove();
			}
		}
	}

	@Override
	public long getCooldown() {
		return Cooldown;
	}

	@Override
	public Location getLocation() {
		return null;
	}

	@Override
	public String getName() {
		return "AirPressure";
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

	public AirPressure(Player player, Entity victim, int use) {
		super(player);
		switch (use) {
			case 0:
				if (victim.isOnGround() && !bPlayer.isOnCooldown("UpThrust")) {
					Location loc = player.getLocation().clone();
					loc.setPitch(0);
					Vector dir = loc.getDirection().clone();
					double forward = 1;
					double upward = 0.85;
					player.setVelocity(new Vector(
							dir.getX() * (forward / 2),
							upward,
							dir.getZ() * (forward / 2)));
					ParticleEffect.CLOUD.display(player.getLocation(), 5, 0.5, 0.5, 0.5, 0.05);
					bPlayer.addCooldown("UpThrust", 5000);
				}
				break;
			case 1:
				if (!victim.isOnGround() && !bPlayer.isOnCooldown("DownThrust")) {
					victim.setVelocity(new Vector(0, -1, 0));
					ParticleEffect.CLOUD.display(victim.getLocation(), 5, 0.5, 0.5, 0.5, 0.05);
					bPlayer.addCooldown("DownThrust", 5000);
				}
				break;

		}
	}

	@Override
	public String getDescription() {
		return "Creates high air pressure around a target location, pulling nearby entities toward the center. Deal damage, slow and push them away upon relasing pressure.";
	}

	@Override
	public String getInstructions() {
		return "Left-click and hold to create air pressure. Release to push.";
	}

}
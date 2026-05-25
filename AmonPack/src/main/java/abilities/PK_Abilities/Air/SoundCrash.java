package Abilities.PK_Abilities.Air;

import Abilities.Util_Objects.AbilityProjectile;
import Abilities.Util_Objects.BetterParticles;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.AirAbility;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.ParticleEffect;
import Abilities.Bending.SoundAbility;
import Plugin.AmonPackPlugin;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class SoundCrash extends SoundAbility implements AddonAbility {
	private Location origin;
	private Location currentLoc;
	private Vector direction;
	private double speed = 1.0;
	private double range = 20.0;
	private double distanceTraveled = 0;
	private List<Entity> hited = new ArrayList<>();

	public SoundCrash(Player player) {
		super(player);
		if (bPlayer.isOnCooldown(this)) {
			return;
		}
		if (!bPlayer.canBend(this)) {
			return;
		}
		origin = player.getEyeLocation().clone();
		currentLoc = origin.clone();
		direction = player.getLocation().getDirection().normalize();

		bPlayer.addCooldown(this);
		start();
	}

	@Override
	public void progress() {
		if (player.isDead() || !player.isOnline()) {
			remove();
			return;
		}

		if (distanceTraveled > range) {
			remove();
			return;
		}

		currentLoc.add(direction.clone().multiply(speed));
		distanceTraveled += speed;

		if (currentLoc.getBlock().getType().isSolid()) {
			remove();
			return;
		}

		Vector right = direction.clone().crossProduct(new Vector(0, 1, 0)).normalize();
		for (double i = -1.5; i <= 1.5; i += 0.1) {
			Vector offset = right.clone().multiply(i);
			Vector curve = direction.clone().multiply(-Math.abs(i) * 0.2); // mniejsze wygięcie kosy
			Location pLoc = currentLoc.clone().add(offset).add(curve);

			pLoc.getWorld().spawnParticle(Particle.SCULK_CHARGE_POP, pLoc, 1, 0.05, 0.05, 0.05, 0);
		}

		for (Entity entity : GeneralMethods.getEntitiesAroundPoint(currentLoc, 1.8)) {
			if ((entity instanceof LivingEntity) && entity.getUniqueId() != player.getUniqueId()
					&& !hited.contains(entity)) {
				applySoundCrashEffect((LivingEntity) entity);
				hited.add(entity);
				remove();
				return;
			}
		}
	}

	private void applySoundCrashEffect(LivingEntity target) {
		target.getWorld().spawnParticle(Particle.SONIC_BOOM, target.getLocation(), 1, 0, 0, 0, 0);

		double S = 0.0;
		if (AfffectedEntities.containsKey(target)) {
			S = AfffectedEntities.get(target);
		}

		if (S <= 0.0) {
			HandleDamage(target, 10.0);
			Vector push = target.getLocation().toVector().subtract(player.getLocation().toVector());
			if (push.lengthSquared() > 0) {
				push.normalize().multiply(0.8).setY(0.2);
				target.setVelocity(push);
			}
		} else {
			double dmg = 1.0 + (S * 0.3);
			DamageHandler.damageEntity(target, dmg, this);

			target.addPotionEffect(
					new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.BLINDNESS, 60, 2));
			target.addPotionEffect(
					new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.NAUSEA, 100, 2));
			target.addPotionEffect(
					new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS, 60, 2));

			Vector push = target.getLocation().toVector().subtract(player.getLocation().toVector());
			if (push.lengthSquared() > 0) {
				push.normalize().multiply(1.2).setY(0.35);
				target.setVelocity(push);
			}
		}
	}

	@Override
	public long getCooldown() {
		return 3000;
	}

	@Override
	public Location getLocation() {
		return currentLoc;
	}

	@Override
	public String getName() {
		return "SoundCrash";
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
	public void load() {
	}

	@Override
	public void stop() {
		super.remove();
	}

	public SoundCrash(Player player, Entity victim, int use) {
		super(player);
		switch (use) {
			case 0:
				if (!bPlayer.isOnCooldown("Major_Sound_OnHit")) {
					if (victim instanceof LivingEntity) {
						applySoundCrashEffect((LivingEntity) victim);
					} else {
						HandleDamage(victim, 10);
					}
					bPlayer.addCooldown("Major_Sound_OnHit", 5000);
					break;
				}
		}
	}

	@Override
	public String getDescription() {
		return "Unleashes a powerful sonic boom. Targets below 10 sound stacks receive +5 stacks and are pushed away. Targets with 10 or more stacks instantly detonate, releasing a powerful shockwave.";
	}

	@Override
	public String getInstructions() {
		return "Left-click to cause a sound crash.";
	}
}
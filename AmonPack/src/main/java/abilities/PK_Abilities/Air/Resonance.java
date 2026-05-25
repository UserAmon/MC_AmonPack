package Abilities.PK_Abilities.Air;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.AirAbility;
import com.projectkorra.projectkorra.util.ParticleEffect;
import Abilities.Bending.SoundAbility;
import Plugin.AmonPackPlugin;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;

public class Resonance extends SoundAbility implements AddonAbility {
	private Location origin;
	private Location currentLoc;
	private Vector direction;
	private double range = 15.0;
	private double speed = 2.0;
	private Set<Entity> hitEntities = new HashSet<>();

	public Resonance(Player player) {
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

		currentLoc.add(direction.clone().multiply(speed));

		// wiązka nie używa notes, tylko lekki sculk
		currentLoc.getWorld().spawnParticle(Particle.SCULK_CHARGE_POP, currentLoc, 3, 0.1, 0.1, 0.1, 0);

		if (currentLoc.distance(origin) > range || currentLoc.getBlock().getType().isSolid()) {
			triggerExplosion(currentLoc, false);
			remove();
			return;
		}

		for (Entity entity : GeneralMethods.getEntitiesAroundPoint(currentLoc, 1.5)) {
			if (entity instanceof LivingEntity && entity.getUniqueId() != player.getUniqueId()) {
				LivingEntity target = (LivingEntity) entity;
				double S = 0.0;
				if (AfffectedEntities.containsKey(target)) {
					S = AfffectedEntities.get(target);
				}

				if (S <= 0.0) {
					triggerExplosion(target.getLocation(), false);
				} else {
					triggerExplosion(target.getLocation(), true);
				}
				remove();
				return;
			}
		}
	}

	private void triggerExplosion(Location loc, boolean empowered) {
		double radius = empowered ? 10.0 : 5.0;
		double stacksToApply = empowered ? 8.0 : 5.0;

		if (empowered) {
			loc.getWorld().spawnParticle(Particle.SONIC_BOOM, loc, 1, 0, 0, 0, 0);
			ParticleEffect.NOTE.display(loc, 25, radius / 2, 1.0, radius / 2, 0);
			loc.getWorld().spawnParticle(Particle.SCULK_CHARGE_POP, loc, 60, radius / 2, 1.0, radius / 2, 0.02);
			
			for (int i = 0; i < 3; i++) {
				Vector randomDir = new Vector(Math.random() - 0.5, 0, Math.random() - 0.5).normalize();
				new EchoProjectile(player, loc.clone().add(0, 0.5, 0), randomDir, 2.0, 0.0, new java.util.ArrayList<>());
			}
		} else {
			loc.getWorld().spawnParticle(Particle.SONIC_BOOM, loc, 1, 0, 0, 0, 0);
			loc.getWorld().spawnParticle(Particle.SCULK_CHARGE_POP, loc, 30, radius / 2, 1.0, radius / 2, 0.02);
		}

		// Zbieramy cele w zasięgu, by wyzwolić dodatkowe wybuchy bez problemu ConcurrentModification
		Set<LivingEntity> toChain = new HashSet<>();

		for (Entity entity : GeneralMethods.getEntitiesAroundPoint(loc, radius)) {
			if (entity instanceof LivingEntity && entity.getUniqueId() != player.getUniqueId() && !hitEntities.contains(entity)) {
				LivingEntity target = (LivingEntity) entity;
				hitEntities.add(target);

				double S = 0.0;
				if (AfffectedEntities.containsKey(target)) {
					S = AfffectedEntities.get(target);
				}

				HandleDamage(target, stacksToApply);

				if (S > 0 && !empowered) {
					// jeśli cel miał stacki a to był mały wybuch, wywołuje duży wybuch z tego miejsca
					toChain.add(target);
				}

				Vector pushDir = target.getLocation().toVector().subtract(loc.toVector());
				if (pushDir.lengthSquared() > 0) {
					pushDir.normalize().multiply(empowered ? 0.8 : 0.6).setY(0.2);
					target.setVelocity(pushDir);
				}
			}
		}

		for (LivingEntity chainTarget : toChain) {
			triggerExplosion(chainTarget.getLocation(), true);
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
		return "Resonance";
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
		return "Fires a resonant wave. Detonates on impact. Hits on stacked targets cause a massively amplified chained explosion.";
	}

	@Override
	public String getInstructions() {
		return "Left-click to fire.";
	}
}

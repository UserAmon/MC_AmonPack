package Abilities.PK_Abilities.Air;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.util.ParticleEffect;
import Abilities.Bending.SoundAbility;
import Plugin.AmonPackPlugin;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class Resonance extends SoundAbility implements AddonAbility {
	private Location origin;
	private Location currentLoc;
	private Vector direction;
	private double range = 15.0;
	private double speed = 1.0;

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
		
		ParticleEffect.SPELL.display(currentLoc, 3, 0.1, 0.1, 0.1, 0.01);
		ParticleEffect.NOTE.display(currentLoc, 1, 0.05, 0.05, 0.05, 0);

		if (currentLoc.distance(origin) > range) {
			explodeAtBlock(currentLoc);
			remove();
			return;
		}

		if (currentLoc.getBlock().getType().isSolid()) {
			explodeAtBlock(currentLoc);
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
					explodeAtBlock(target.getLocation());
				} else {
					explodeAtStackedTarget(target, S);
				}
				remove();
				return;
			}
		}
	}

	private void explodeAtBlock(Location loc) {
		ParticleEffect.CLOUD.display(loc, 15, 1.5, 0.2, 1.5, 0.05);
		ParticleEffect.SPELL.display(loc, 10, 1.5, 0.2, 1.5, Color.fromRGB(192, 192, 192));
		ParticleEffect.NOTE.display(loc, 10, 1.5, 0.2, 1.5, Color.fromRGB(192, 192, 192));
		
		double radius = 3.5;
		for (Entity entity : GeneralMethods.getEntitiesAroundPoint(loc, radius + 2.0)) {
			if (entity instanceof LivingEntity && entity.getUniqueId() != player.getUniqueId()) {
				Location targetLoc = entity.getLocation();
				double distance = loc.distance(targetLoc);
				double verticalDist = Math.abs(loc.getY() - targetLoc.getY());
				
				if (distance <= radius && verticalDist <= 2.0) {
					LivingEntity le = (LivingEntity) entity;
					HandleDamage(le, 10.0);
					le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1));
					
					Vector pushDir = targetLoc.toVector().subtract(loc.toVector());
					if (pushDir.lengthSquared() > 0) {
						pushDir.normalize().multiply(0.6).setY(0.2);
						le.setVelocity(pushDir);
					}
				}
			}
		}
	}

	private void explodeAtStackedTarget(LivingEntity target, double S) {
		Location loc = target.getLocation();
		loc.getWorld().spawnParticle(org.bukkit.Particle.EXPLOSION, loc, 3, 0.5, 0.5, 0.5, 0.1);
		ParticleEffect.SPELL.display(loc, 25, 2.5, 1.5, 2.5, Color.fromRGB(192, 192, 192));
		ParticleEffect.NOTE.display(loc, 25, 2.5, 1.5, 2.5, Color.fromRGB(192, 192, 192));
		
		HandleDamage(target, 20.0);

		double radius = 5.5;
		for (Entity entity : GeneralMethods.getEntitiesAroundPoint(loc, radius)) {
			if (entity instanceof LivingEntity && entity.getUniqueId() != player.getUniqueId() && entity.getUniqueId() != target.getUniqueId()) {
				LivingEntity le = (LivingEntity) entity;
				HandleDamage(le, S);
				
				Vector pullDir = loc.toVector().subtract(le.getLocation().toVector());
				if (pullDir.lengthSquared() > 0) {
					pullDir.normalize().multiply(0.8).setY(0.35);
					le.setVelocity(pullDir);
				}
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
	public void load() {
	}

	@Override
	public void stop() {
		super.remove();
	}

	@Override
	public String getDescription() {
		return "Launches a narrow resonance beam. On solid blocks or stack-less targets, it creates a horizontal shockwave. On stacked targets, it detonates them and pulls surrounding enemies in.";
	}

	@Override
	public String getInstructions() {
		return "Left-click to launch the resonance beam.";
	}
}

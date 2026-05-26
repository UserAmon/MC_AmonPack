package Abilities.PK_Abilities.Air;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.AirAbility;
import com.projectkorra.projectkorra.util.DamageHandler;
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Resonance extends SoundAbility implements AddonAbility {
	private Location targetBlockLoc;
	private int initialSlot;
	private int chargeTicks = 0;
	private int detonateTicks = 0;
	private int damage = 1;
	private boolean detonating = false;
	private Set<Entity> hitEntities = new HashSet<>();
	private List<SoundRing> activeRings = new ArrayList<>();
	private boolean hasDysonance = false;

	private static java.util.HashMap<java.util.UUID, Integer> resonanceCharges = new java.util.HashMap<>();
	private static java.util.HashMap<java.util.UUID, Long> lastResonanceTime = new java.util.HashMap<>();

	private static class SoundRing {
		Location center;
		double currentRadius;
		double maxRadius;
		double speed;
		int delayTicks;
		boolean empowered;
		boolean isFirstRing;
		Set<Entity> hitInThisRing = new HashSet<>();

		SoundRing(Location center, double maxRadius, double speed, int delayTicks, boolean empowered,
				boolean isFirstRing) {
			this.center = center.clone();
			this.currentRadius = 0.0;
			this.maxRadius = maxRadius;
			this.speed = speed;
			this.delayTicks = delayTicks;
			this.empowered = empowered;
			this.isFirstRing = isFirstRing;
		}
	}

	public Resonance(Player player) {
		super(player);

		RPG.Levels.BendingTree.PlayerBendingBranch branch = AmonPackPlugin.levelsBending
				.GetBranchByPlayerName(player.getName());
		this.hasDysonance = (branch != null && branch.hasUpgrade("Dysonance"));

		if (bPlayer.isOnCooldown(this)) {
			return;
		}

		if (!bPlayer.canBend(this)) {
			return;
		}

		org.bukkit.block.Block targetBlock = player.getTargetBlockExact(20);
		if (targetBlock == null || targetBlock.getType().isAir()) {
			return;
		}

		targetBlockLoc = targetBlock.getLocation().clone().add(0.5, 1, 0.5);
		initialSlot = player.getInventory().getHeldItemSlot();
		bPlayer.addCooldown(this);
		start();
	}

	@Override
	public void progress() {
		if (player.isDead() || !player.isOnline()) {
			remove();
			return;
		}

		if (!detonating) {
			if (player.getInventory().getHeldItemSlot() != initialSlot) {
				remove();
				return;
			}
			if (player.getLocation().distance(targetBlockLoc) > 25.0) {
				remove();
				return;
			}
			Location start = player.getLocation().clone().add(
					(Math.random() - 0.5),
					0.5 + (Math.random() - 0.5),
					(Math.random() - 0.5));
			drawTether(start, targetBlockLoc);

			chargeTicks++;
			int requiredTicks = this.hasDysonance ? 20 : 40;
			if (chargeTicks >= requiredTicks) {
				detonating = true;
				activeRings.add(new SoundRing(targetBlockLoc.clone().add(0, 0.25, 0), 7.0, 0.21, 0, false, true));
				targetBlockLoc.getWorld().spawnParticle(Particle.SONIC_BOOM, targetBlockLoc.clone().add(0, 0.25, 0), 1,
						0, 0, 0, 0);
			}
		} else {
			detonateTicks++;
			if (detonateTicks == 15) {
				activeRings.add(new SoundRing(targetBlockLoc.clone().add(0, 0.25, 0), 7.0, 0.21, 0, false, false));
			} else if (detonateTicks == 20) {
				activeRings.add(new SoundRing(targetBlockLoc.clone().add(0, 0.25, 0), 7.0, 0.21, 0, false, false));
			}

			List<SoundRing> toRemove = new ArrayList<>();
			List<SoundRing> toAdd = new ArrayList<>();

			for (SoundRing ring : activeRings) {
				if (ring.delayTicks > 0) {
					ring.delayTicks--;
					continue;
				}
				ring.currentRadius += ring.speed;

				if (ring.currentRadius >= ring.maxRadius) {
					toRemove.add(ring);
					continue;
				}

				drawRingParticles(ring);

				double innerRad = ring.currentRadius - 0.75;
				double outerRad = ring.currentRadius + 0.75;

				for (Entity entity : GeneralMethods.getEntitiesAroundPoint(ring.center, outerRad)) {
					if (entity instanceof LivingEntity && entity.getUniqueId() != player.getUniqueId()) {
						LivingEntity target = (LivingEntity) entity;
						double dist = target.getLocation().distance(ring.center);
						if (dist >= innerRad && dist <= outerRad) {
							if (!ring.hitInThisRing.contains(target)) {
								ring.hitInThisRing.add(target);

								if (!hitEntities.contains(target)) {
									hitEntities.add(target);

									double S = 0.0;
									if (AfffectedEntities.containsKey(target)) {
										S = AfffectedEntities.get(target);
									}

									if (S <= 0.0) {
										HandleDamage(player, target, 5.0);
										target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 30, 2));
										target.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 30, 2));
									} else {
										DamageHandler.damageEntity(entity, damage, Resonance.this);
										HandleDamage(player, target, 8.0);
										target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 30, 2));
										target.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 30, 2));
										toAdd.add(new SoundRing(target.getLocation(), 6.0, 0.21, 10, true, false));
										toAdd.add(new SoundRing(target.getLocation(), 6.0, 0.21, 20, true, false));

										target.getWorld().spawnParticle(Particle.SONIC_BOOM,
												target.getLocation().clone().add(0, 0.5, 0), 1, 0, 0, 0, 0);

										Vector pushDir = target.getLocation().toVector()
												.subtract(ring.center.toVector());
										if (pushDir.lengthSquared() > 0) {
											pushDir.normalize().multiply(0.8).setY(0.2);
											target.setVelocity(pushDir);
										}
									}
								}
							}
						}
					}
				}
			}

			activeRings.removeAll(toRemove);
			activeRings.addAll(toAdd);
			if (activeRings.isEmpty() && detonateTicks >= 30) {
				remove();
			}
		}
	}

	private void drawTether(Location start, Location end) {
		if (!start.getWorld().equals(end.getWorld()))
			return;
		double dist = start.distance(end);
		Vector dir = end.toVector().subtract(start.toVector()).normalize();
		for (double d = 0; d < dist; d += 0.4) {
			Location point = start.clone().add(dir.clone().multiply(d));
			double wave = Math.sin(d * 1.5 + (System.currentTimeMillis() / 80.0)) * 0.15;
			point.add(0, wave, 0);
			point.getWorld().spawnParticle(org.bukkit.Particle.SCULK_CHARGE_POP, point, 1, 0, 0, 0, 0);
			if (Math.random() < 0.02) {
				ParticleEffect.NOTE.display(point, 1, 0.0, 0.0, 0.0, Color.fromRGB(192, 192, 192));
			}
		}
	}

	private void drawRingParticles(SoundRing ring) {
		double radius = ring.currentRadius;
		if (radius <= 0.1)
			return;
		int points = (int) (Math.PI * radius * 2);
		points = Math.max(4, points);

		for (int i = 0; i < points; i++) {
			double angle = 2 * Math.PI * i / points;
			double x = radius * Math.cos(angle);
			double z = radius * Math.sin(angle);

			double yOffset = (Math.random() - 0.5) * 0.5;
			Location particleLoc = ring.center.clone().add(x, yOffset, z);

			if (ring.isFirstRing && Math.random() < 0.2) {
				playAirbendingParticles(ring.center.clone().add(x, 0.1, z), 1);
			} else {
				particleLoc.getWorld().spawnParticle(Particle.SCULK_CHARGE_POP, particleLoc, 1, 0, 0, 0, 0);
			}

			if (Math.random() < 0.01) {
				ParticleEffect.NOTE.display(particleLoc, 1, 0.0, 0.0, 0.0, Color.fromRGB(192, 192, 192));
			}
			if (Math.random() < 0.01) {
				particleLoc.getWorld().spawnParticle(org.bukkit.Particle.SONIC_BOOM, particleLoc, 1, 0, 0, 0, 0);
			}
		}
	}

	@Override
	public long getCooldown() {
		return 3000;
	}

	@Override
	public Location getLocation() {
		return targetBlockLoc;
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
		return "1.2";
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
		return "Establishes a tether to a clicked block. After charging, triggers sound waves. Stacked targets chain secondary waves.";
	}

	@Override
	public String getInstructions() {
		return "Left-click on a block to start charging.";
	}
}

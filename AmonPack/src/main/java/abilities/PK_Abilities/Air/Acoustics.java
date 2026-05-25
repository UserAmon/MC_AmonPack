package Abilities.PK_Abilities.Air;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.AirAbility;
import com.projectkorra.projectkorra.util.ParticleEffect;
import Abilities.Bending.SoundAbility;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class Acoustics extends SoundAbility implements AddonAbility {
	private List<LivingEntity> chain = new ArrayList<>();
	private List<LivingEntity> allTargets = new ArrayList<>();
	private int ticksElapsed = 0;
	private int spreadCooldown = 0;

	public Acoustics(Player player) {
		super(player);
		if (bPlayer.isOnCooldown(this)) {
			return;
		}
		if (!bPlayer.canBend(this)) {
			return;
		}

		LivingEntity target = findInitialTarget();
		if (target == null) {
			return;
		}

		chain.add(target);
		allTargets.add(target);
		bPlayer.addCooldown(this);
		start();
	}

	private LivingEntity findInitialTarget() {
		LivingEntity bestTarget = null;
		double bestDot = -1.0;
		Vector direction = player.getEyeLocation().getDirection().normalize();

		for (Entity entity : GeneralMethods.getEntitiesAroundPoint(player.getLocation(), 12.0)) {
			if (entity instanceof LivingEntity && entity.getUniqueId() != player.getUniqueId()) {
				LivingEntity le = (LivingEntity) entity;
				if (AfffectedEntities.containsKey(le) && AfffectedEntities.get(le) > 0) {
					Vector toEntity = le.getEyeLocation().toVector().subtract(player.getEyeLocation().toVector());
					double dist = toEntity.length();
					if (dist > 0) {
						toEntity.normalize();
						double dot = direction.dot(toEntity);
						if (dot > 0.8 && dot > bestDot) {
							bestDot = dot;
							bestTarget = le;
						}
					}
				}
			}
		}
		return bestTarget;
	}

	@Override
	public void progress() {
		if (player.isDead() || !player.isOnline() || !player.isSneaking()) {
			remove();
			return;
		}

		if (chain.isEmpty()) {
			remove();
			return;
		}

		LivingEntity mainTarget = chain.get(0);
		if (mainTarget.isDead() || player.getLocation().distance(mainTarget.getLocation()) > 15.0) {
			remove();
			return;
		}

		ticksElapsed++;
		if (ticksElapsed >= 100) {
			for (LivingEntity target : chain) {
				com.projectkorra.projectkorra.util.DamageHandler.damageEntity(target, 2.0, this);
				target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20, 1));
				target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20, 1));
				
				for (int i = 0; i < 2; i++) {
					Vector randomDir = new Vector(Math.random() - 0.5, 0, Math.random() - 0.5).normalize();
					new EchoProjectile(player, target.getLocation().clone().add(0, 1.0, 0), randomDir, 2.0, 0.0, new java.util.ArrayList<>());
				}
			}
			remove();
			return;
		}

		Location pStart = player.getLocation().clone().add(
				(Math.random() - 0.5), 
				0.5 + (Math.random() - 0.5), 
				(Math.random() - 0.5)
		);
		drawTether(pStart, chain.get(0).getEyeLocation());

		for (int i = 0; i < chain.size() - 1; i++) {
			drawTether(chain.get(i).getEyeLocation(), chain.get(i + 1).getEyeLocation());
		}

		spreadCooldown++;
		if (spreadCooldown >= 20) {
			spreadCooldown = 0;
			List<LivingEntity> deadOrShocked = new ArrayList<>();

			for (LivingEntity target : chain) {
				if (target.isDead() || !AfffectedEntities.containsKey(target)) {
					deadOrShocked.add(target);
					continue;
				}

				double currentStack = AfffectedEntities.get(target);
				if (currentStack >= 20.0) {
					HandleDamage(target, 1.0);
					deadOrShocked.add(target);
					continue;
				}

				HandleDamage(target, 3.0);
				target.getWorld().spawnParticle(org.bukkit.Particle.SONIC_BOOM, target.getLocation(), 1, 0, 0, 0, 0);

				double newStack = currentStack + 3.0;
				int amp = (int) (newStack / 5.0);
				if (amp > 4)
					amp = 4;

				target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, amp));
				target.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 60, 2));
			}

			chain.removeAll(deadOrShocked);

			if (!chain.isEmpty()) {
				LivingEntity lastLink = chain.get(chain.size() - 1);
				LivingEntity nextTarget = findNextTarget(lastLink);
				if (nextTarget != null) {
					chain.add(nextTarget);
					allTargets.add(nextTarget);
					HandleDamage(nextTarget, 3.0);
					nextTarget.getWorld().spawnParticle(org.bukkit.Particle.SONIC_BOOM, nextTarget.getLocation(), 1, 0, 0, 0, 0);
				}
			}
		}
	}

	private LivingEntity findNextTarget(LivingEntity source) {
		for (Entity entity : GeneralMethods.getEntitiesAroundPoint(source.getLocation(), 6.0)) {
			if (entity instanceof LivingEntity && entity.getUniqueId() != player.getUniqueId()) {
				LivingEntity le = (LivingEntity) entity;
				if (!allTargets.contains(le) && !le.isDead()) {
					return le;
				}
			}
		}
		return null;
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
			if (Math.random() < 0.04) {
				ParticleEffect.NOTE.display(point, 1, 0.0, 0.0, 0.0, Color.fromRGB(192, 192, 192));
			}
		}
	}

	@Override
	public long getCooldown() {
		return 6000;
	}

	@Override
	public Location getLocation() {
		return chain.isEmpty() ? null : chain.get(0).getLocation();
	}

	@Override
	public String getName() {
		return "Acoustics";
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
		return "Establishes a tether to an already stacked enemy by holding sneak. co-seconds the tether adds +3 sound stacks, slows them, and inflicts nausea. The tether can chain to nearby enemies every second.";
	}

	@Override
	public String getInstructions() {
		return "Hold sneak on an enemy with sound stacks to tether them.";
	}
}

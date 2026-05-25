package Abilities.PK_Abilities.Air;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.util.ParticleEffect;
import Abilities.Bending.SoundAbility;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.List;

public class EchoProjectile extends SoundAbility {
	private Location origin;
	private Location location;
	private Vector direction;
	private double speed = 1.0;
	private boolean bounced = false;
	private double stacksToApply;
	private double dmg;
	private List<Entity> sharedHitList;

	public EchoProjectile(Player player, Location origin, Vector direction, double stacksToApply, double dmg, List<Entity> sharedHitList) {
		super(player);
		this.origin = origin.clone();
		this.location = origin.clone();
		this.direction = direction.normalize();
		this.stacksToApply = stacksToApply;
		this.dmg = dmg;
		this.sharedHitList = sharedHitList;
		start();
	}

	@Override
	public void progress() {
		if (!player.isOnline() || player.isDead()) {
			remove();
			return;
		}

		Vector velocity = direction.clone().multiply(speed);
		org.bukkit.util.RayTraceResult result = location.getWorld().rayTraceBlocks(location, velocity, speed, org.bukkit.FluidCollisionMode.NEVER, true);

		if (result != null && result.getHitBlock() != null) {
			Block hitBlock = result.getHitBlock();
			if (hitBlock.getType().isSolid()) {
				BlockFace face = result.getHitBlockFace();
				if (face != null && !bounced) {
					Vector normal = new Vector(face.getModX(), face.getModY(), face.getModZ());
					double dot = direction.dot(normal);
					direction.subtract(normal.multiply(2 * dot));

					location.getWorld().spawnParticle(org.bukkit.Particle.SONIC_BOOM, location, 1, 0, 0, 0, 0);
					ParticleEffect.NOTE.display(location, 3, 0.2, 0.2, 0.2, 0);
					location.getWorld().playSound(location, Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 0.5f, 1.2f);
					location.add(result.getHitPosition().subtract(location.toVector()).multiply(0.9));
					bounced = true;
				} else {
					remove();
					return;
				}
			}
		} else {
			location.add(velocity);
		}

		location.getWorld().spawnParticle(org.bukkit.Particle.SCULK_CHARGE_POP, location, 1, 0.1, 0, 0.1, 0);

		for (Entity entity : GeneralMethods.getEntitiesAroundPoint(location, 1.2)) {
			if ((entity instanceof LivingEntity) && entity.getUniqueId() != player.getUniqueId()) {
				if (sharedHitList != null) {
					if (!sharedHitList.contains(entity)) {
						HandleDamage(entity, stacksToApply);
						sharedHitList.add(entity);
					} else {
						HandleDamage(entity, 2.0);
					}
				} else {
					HandleDamage(entity, stacksToApply);
				}
				
				if (dmg > 0) {
					com.projectkorra.projectkorra.util.DamageHandler.damageEntity(entity, dmg, this);
				}
				location.getWorld().spawnParticle(org.bukkit.Particle.SONIC_BOOM, location, 1, 0, 0, 0, 0);
				ParticleEffect.NOTE.display(location, 3, 0.2, 0.2, 0.2, 0);
				remove();
				return;
			}
		}

		if (location.distance(origin) > 15) {
			remove();
		}
	}

	@Override
	public boolean isHiddenAbility() {
		return true;
	}

	@Override
	public long getCooldown() { return 0; }
	@Override
	public Location getLocation() { return location; }
	@Override
	public String getName() { return "EchoProjectile"; }
	@Override
	public boolean isHarmlessAbility() { return false; }
	@Override
	public boolean isSneakAbility() { return false; }
}

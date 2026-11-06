package Plugin;

import java.util.*;

import Abilities.Util_Objects.AbilityProjectile;
import Abilities.Util_Objects.BetterParticles;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class Methods {

	public Methods() {
		super();
	}

	public static Vector getDirection(Location from, Location to) {
		Vector dir = to.toVector().subtract(from.toVector());
		if (dir.lengthSquared() > 0.0001) {
			dir.normalize();
		}
		return dir;
	}

	public static List<Block> getBlocksAroundPoint(Location location, double radius) {
		List<Block> blocks = new ArrayList<>();
		if (location == null || location.getWorld() == null) return blocks;
		World world = location.getWorld();
		int r = (int) Math.ceil(radius);
		int cX = location.getBlockX();
		int cY = location.getBlockY();
		int cZ = location.getBlockZ();
		double rSq = radius * radius;
		for (int x = cX - r; x <= cX + r; x++) {
			for (int y = cY - r; y <= cY + r; y++) {
				for (int z = cZ - r; z <= cZ + r; z++) {
					double distSq = (x - cX) * (x - cX) + (y - cY) * (y - cY) + (z - cZ) * (z - cZ);
					if (distSq <= rSq) {
						blocks.add(world.getBlockAt(x, y, z));
					}
				}
			}
		}
		return blocks;
	}

	public static List<Entity> getEntitiesAroundPoint(Location location, double radius) {
		if (location == null || location.getWorld() == null) return Collections.emptyList();
		return new ArrayList<>(location.getWorld().getNearbyEntities(location, radius, radius, radius));
	}

	public static void LightningProjectile(Location location, Player player) {
		List<AbilityProjectile> projectiles = new ArrayList<>();
		List<BetterParticles> particles = new ArrayList<>();
		Location origin = location.clone();

		Location offset1 = player.getLocation().clone();
		Location offset2 = player.getLocation().clone();
		offset1.setYaw(location.getYaw() + 30);
		offset2.setYaw(location.getYaw() - 30);

		particles.add(new BetterParticles(3, Particle.DUST, 0.3, 0.01, 0.15, Color.fromRGB(135, 206, 250)));
		particles.add(new BetterParticles(3, Particle.DUST, 0.3, 0.01, 0.15, Color.fromRGB(135, 206, 250)));
		particles.add(new BetterParticles(1, Particle.CRIT, 0.2, 0.01, 0.15));
		particles.add(new BetterParticles(1, Particle.FIREWORK, 0.2, 0.01, 0.15));

		projectiles.add(new AbilityProjectile(player.getLocation().getDirection(), location, location, particles, 1));
		projectiles.add(new AbilityProjectile(offset1.getDirection(), location, location, particles, 1));
		projectiles.add(new AbilityProjectile(offset2.getDirection(), location, location, particles, 1));

		new BukkitRunnable() {
			int ticksElapsed = 0;

			@Override
			public void run() {
				ticksElapsed++;
				if (ticksElapsed > 80 || projectiles.isEmpty()) {
					this.cancel();
					return;
				}
				for (Iterator<AbilityProjectile> it = projectiles.iterator(); it.hasNext();) {
					AbilityProjectile projectile = it.next();
					Location loc = projectile.LightningAdvance().clone();
					for (Entity entity : getEntitiesAroundPoint(loc, 1)) {
						if (entity instanceof LivingEntity && !entity.getUniqueId().equals(player.getUniqueId())) {
							((LivingEntity) entity).damage(1, player);
						}
					}

					if (loc.distance(origin) > 10 || loc.getBlock().getType().isSolid()) {
						it.remove();
					}
				}
			}
		}.runTaskTimer(AmonPackPlugin.plugin, 0, 4);
	}

	public static int getRandom(int lower, int upper) {
		Random random = new Random();
		return random.nextInt((upper - lower) + 1) + lower;
	}

	public static List<UUID> SpawnedByMe = new ArrayList<>();

	public static void spawnFallingBlocks(Location location, Material mat, int amount, double factor, Player player) {
		if (location == null || location.getWorld() == null)
			return;
		World world = location.getWorld();
		Random random = new Random();
		for (int i = 0; i < amount; i++) {
			FallingBlock fallingBlock = world.spawnFallingBlock(location, mat.createBlockData());
			fallingBlock.setDropItem(false);
			fallingBlock.setCancelDrop(true);
			fallingBlock.setHurtEntities(false);
			fallingBlock.setMetadata("AmonPack_NoPlace", new org.bukkit.metadata.FixedMetadataValue(AmonPackPlugin.plugin, true));
			double x = (random.nextDouble() - 0.5) * (0.5 * factor);
			double z = (random.nextDouble() - 0.5) * (0.5 * factor);
			double y = 0.3 + random.nextDouble() * (0.1 * factor);
			fallingBlock.setVelocity(new Vector(x, y, z));
			SpawnedByMe.add(fallingBlock.getUniqueId());

			new BukkitRunnable() {
				int ticks = 0;
				@Override
				public void run() {
					ticks++;
					if (fallingBlock.isDead() || !fallingBlock.isValid() || fallingBlock.isOnGround() || ticks > 25) {
						if (fallingBlock.isValid()) {
							fallingBlock.getWorld().spawnParticle(Particle.BLOCK, fallingBlock.getLocation(), 4, 0.2, 0.2, 0.2, 0.05, mat.createBlockData());
							fallingBlock.remove();
						}
						SpawnedByMe.remove(fallingBlock.getUniqueId());
						this.cancel();
					}
				}
			}.runTaskTimer(AmonPackPlugin.plugin, 1L, 1L);

			if (player != null) {
				startDamageTask(fallingBlock, player);
			}
		}
	}

	private static void startDamageTask(FallingBlock fallingBlock, Player player) {
		new BukkitRunnable() {
			@Override
			public void run() {
				if (fallingBlock.isDead() || !fallingBlock.isValid()) {
					this.cancel();
					SpawnedByMe.remove(fallingBlock.getUniqueId());
					return;
				}
				for (Entity entity : fallingBlock.getNearbyEntities(1.5, 1.5, 1.5)) {
					if (entity instanceof LivingEntity && entity != player) {
						((LivingEntity) entity).damage(1.0);
					}
				}
			}
		}.runTaskTimer(AmonPackPlugin.plugin, 0, 5);
	}

	public static Location getTargetLocation(Player player, double range) {
		Vector direction = player.getLocation().getDirection().clone().multiply(0.1D);
		Location loc = player.getEyeLocation().clone();
		Location startLoc = loc.clone();
		do {
			loc.add(direction);
		} while (startLoc.distance(loc) < range && loc.getBlock().getBlockData().getMaterial() == Material.AIR);
		return loc;
	}

	public static List<Block> getBlocksInRadius(Location center, int radius) {
		List<Block> blocks = new ArrayList<>();
		World world = center.getWorld();
		if (world == null) {
			return blocks;
		}
		int minX = center.getBlockX() - radius;
		int minY = center.getBlockY() - radius;
		int minZ = center.getBlockZ() - radius;
		int maxX = center.getBlockX() + radius;
		int maxY = center.getBlockY() + radius;
		int maxZ = center.getBlockZ() + radius;
		for (int x = minX; x <= maxX; x++) {
			for (int y = minY; y <= maxY; y++) {
				for (int z = minZ; z <= maxZ; z++) {
					Block block = world.getBlockAt(x, y, z);
					blocks.add(block);
				}
			}
		}
		return blocks;
	}
}

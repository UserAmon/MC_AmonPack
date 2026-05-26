package Abilities.PK_Abilities.Earth;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.EarthAbility;
import com.projectkorra.projectkorra.ability.SandAbility;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.TempBlock;
import Abilities.Util_Objects.EarthDisc;
import Plugin.AmonPackPlugin;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SandDisc extends SandAbility implements AddonAbility {
	private int state = 0;
	private Location sourceLoc;
	private Location currentSandLoc;
	private int slot;
	private long lastDrawTime = 0;
	private double radius = 0.5;

	public SandDisc(Player player) {
		super(player);
		if (bPlayer.isOnCooldown(this)) {
			return;
		}
		if (!bPlayer.canBend(this)) {
			return;
		}

		long now = System.currentTimeMillis();
		if (now - lastDrawTime < 1000) {
			return;
		}

		Block sourceBlock = player.getTargetBlockExact(20);
		if (sourceBlock == null || (sourceBlock.getType() != Material.SAND && sourceBlock.getType() != Material.RED_SAND)) {
			return;
		}

		this.slot = player.getInventory().getHeldItemSlot();
		this.sourceLoc = sourceBlock.getLocation().add(0.5, 0.5, 0.5);
		this.currentSandLoc = sourceLoc.clone();
		this.lastDrawTime = now;
		
		state = 1;
		start();
	}

	@Override
	public void progress() {
		if (player.isDead() || !player.isOnline()) {
			remove();
			return;
		}

		if (player.getInventory().getHeldItemSlot() != slot) {
			remove();
			return;
		}

		if (state == 1) {
			Location handLoc = getHandLocation();
			Vector dir = handLoc.toVector().subtract(currentSandLoc.toVector());
			double dist = dir.length();
			
			if (dist < 1.5) {
				state = 2;
				player.getWorld().playSound(player.getLocation(), Sound.BLOCK_SAND_PLACE, 1f, 1.2f);
			} else {
				dir.normalize().multiply(0.8);
				currentSandLoc.add(dir);
				player.getWorld().spawnParticle(org.bukkit.Particle.BLOCK, currentSandLoc, 4, 0.1, 0.1, 0.1, 0.05, Material.SAND.createBlockData());
			}
		} else if (state == 2) {
			renderSandHand();
		}
	}

	private Location getHandLocation() {
		Location hand = player.getLocation().clone().add(0, 1.1, 0);
		Vector right = player.getLocation().getDirection().clone().crossProduct(new Vector(0, 1, 0)).normalize().multiply(0.35);
		return hand.add(right);
	}

	private void renderSandHand() {
		Location handLoc = getHandLocation();
		double time = System.currentTimeMillis() / 150.0;
		for (int i = 0; i < 3; i++) {
			double angle = time + (i * (Math.PI * 2 / 3));
			double x = 0.35 * Math.cos(angle);
			double z = 0.35 * Math.sin(angle);
			Location p = handLoc.clone().add(x, 0, z);
			player.getWorld().spawnParticle(org.bukkit.Particle.BLOCK, p, 1, 0, 0, 0, 0, Material.SAND.createBlockData());
		}
	}

	public void onLeftClick() {
		if (state == 2) {
			Location spawn = getHandLocation();
			Vector dir = player.getLocation().getDirection().normalize();
			new SandEarthDisc(player, spawn, dir, 4.0, 0.85, true, this);
			player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EGG_THROW, 1f, 0.8f);
			bPlayer.addCooldown(this);
			remove();
		}
	}

	@Override
	public long getCooldown() {
		return 5000;
	}

	@Override
	public Location getLocation() {
		return player.getLocation();
	}

	@Override
	public String getName() {
		return "SandDisc";
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
		return "Extracts and hurls a sharp disc of sand. On wall ricochets, it splinters into random blinding sand shrapnels that convert standard earth blocks to sand. Upon impact or max range, it explodes into a forward cone of shrapnel.";
	}

	@Override
	public String getInstructions() {
		return "Left-click a sand block to draw water-like sand to your hand. Once loaded, left-click again to hurl the sand disc.";
	}
}

class SandEarthDisc extends EarthDisc {
	private int bounces = 0;
	private SandDisc sourceAbility;

	public SandEarthDisc(Player player, Location location, Vector direction, double damage, double speed, boolean destroyOnEntityHit, SandDisc sourceAbility) {
		super(player, location, direction, damage, speed, destroyOnEntityHit);
		this.sourceAbility = sourceAbility;
	}

	@Override
	protected void progress() {
		Vector velocity = direction.clone().multiply(speed);

		org.bukkit.util.RayTraceResult result = location.getWorld().rayTraceBlocks(location, velocity, speed,
				org.bukkit.FluidCollisionMode.NEVER, true);

		if (result != null && result.getHitBlock() != null) {
			Block hitBlock = result.getHitBlock();
			if (hitBlock.getType().isSolid()) {
				BlockFace face = result.getHitBlockFace();
				if (face != null) {
					Vector normal = new Vector(face.getModX(), face.getModY(), face.getModZ());
					double dot = direction.dot(normal);
					direction.subtract(normal.multiply(2 * dot));

					location.getWorld().playSound(location, Sound.BLOCK_SAND_HIT, 1f, 1.5f);
					location.add(result.getHitPosition().subtract(location.toVector()).multiply(0.9));

					bounces++;
					spawnRicochetShrapnel();

					if (bounces >= 4) {
						explode();
						remove();
						return;
					}
				} else {
					explode();
					remove();
					return;
				}
			}
		} else {
			location.add(velocity);
		}

		List<org.bukkit.entity.Entity> hitEntities = new ArrayList<>();
		for (Entity entity : GeneralMethods.getEntitiesAroundPoint(location, 1.5)) {
			if (entity instanceof LivingEntity && !entity.getUniqueId().equals(player.getUniqueId())) {
				if (!hitEntities.contains(entity)) {
					hitEntities.add(entity);
					LivingEntity target = (LivingEntity) entity;
					DamageHandler.damageEntity(target, 4.0, sourceAbility);
					Vector knock = target.getLocation().toVector().subtract(player.getLocation().toVector());
					if (knock.lengthSquared() > 0) {
						knock.normalize().multiply(0.85).setY(0.25);
						target.setVelocity(knock);
					}
				}
			}
		}

		display();
	}

	@Override
	protected void display() {
		World world = location.getWorld();
		Vector baseVector = new Vector(0, 0.5, 0);
		// Sand colours: bright sand / dark sand / orange grain
		Particle.DustOptions dustSand   = new Particle.DustOptions(Color.fromRGB(237, 201, 122), 0.55f);
		Particle.DustOptions dustDark   = new Particle.DustOptions(Color.fromRGB(160, 120,  50), 0.60f);
		Particle.DustOptions dustOrange = new Particle.DustOptions(Color.fromRGB(210, 160,  70), 0.45f);
		for (double angle = 0; angle < 360; angle += 30) {
			if (new Random().nextDouble() > 0.25) {
				Vector blockOffset = GeneralMethods.getOrthogonalVector(baseVector, angle, radius);
				world.spawnParticle(Particle.BLOCK, location.clone().add(blockOffset), 1, 0, 0, 0, 0,
						Material.SAND.createBlockData());
			}
			Vector dustOffset = GeneralMethods.getOrthogonalVector(baseVector, angle, radius / 2);
			world.spawnParticle(Particle.DUST, location.clone().add(dustOffset), 1, 0, 0, 0, 0, dustSand);
			Vector darkOffset = GeneralMethods.getOrthogonalVector(baseVector, angle, radius + 0.25);
			world.spawnParticle(Particle.DUST, location.clone().add(darkOffset), 1, 0, 0, 0, 0, dustDark);
			if (angle % 60 == 0) {
				Vector orangeOff = GeneralMethods.getOrthogonalVector(baseVector, angle + 15, radius * 0.75);
				world.spawnParticle(Particle.DUST, location.clone().add(orangeOff), 1, 0, 0, 0, 0, dustOrange);
			}
		}
	}

	@Override
	public void explode() {
		location.getWorld().spawnParticle(org.bukkit.Particle.BLOCK, location, 30, 0.5, 0.5, 0.5, 0.1, Material.SAND.createBlockData());
		location.getWorld().playSound(location, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1.4f);

		for (int i = 0; i < 5; i++) {
			Location shLoc = location.clone();
			Vector shDir = direction.clone().add(new Vector((Math.random() - 0.5) * 0.3, (Math.random() - 0.5) * 0.15, (Math.random() - 0.5) * 0.3)).normalize().multiply(0.6);
			new BukkitRunnable() {
				int t = 0;

				@Override
				public void run() {
					t++;
					if (t > 22 || shLoc.getBlock().getType().isSolid()) {
						cancel();
						return;
					}
					shLoc.add(shDir);
					shLoc.getWorld().spawnParticle(org.bukkit.Particle.BLOCK, shLoc, 4, 0.1, 0.1, 0.1, 0, Material.SAND.createBlockData());

					for (Entity e : GeneralMethods.getEntitiesAroundPoint(shLoc, 1.2)) {
						if (e instanceof LivingEntity && e.getUniqueId() != player.getUniqueId()) {
							LivingEntity target = (LivingEntity) e;
							DamageHandler.damageEntity(target, 2.0, sourceAbility);
							target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 30, 1));
							cancel();
							return;
						}
					}

					Block b = shLoc.getBlock();
					if (com.projectkorra.projectkorra.ability.EarthAbility.isEarthbendable(player, b)) {
						new TempBlock(b, Material.SAND).setRevertTime(5000);
					}
				}
			}.runTaskTimer(AmonPackPlugin.plugin, 0, 1);
		}
	}

	private void spawnRicochetShrapnel() {
		for (int i = 0; i < 6; i++) {
			Location shLoc = location.clone();
			Vector shDir = new Vector(Math.random() - 0.5, Math.random() - 0.5, Math.random() - 0.5).normalize().multiply(0.6);
			new BukkitRunnable() {
				int t = 0;

				@Override
				public void run() {
					t++;
					if (t > 14 || shLoc.getBlock().getType().isSolid()) {
						cancel();
						return;
					}
					shLoc.add(shDir);
					shLoc.getWorld().spawnParticle(org.bukkit.Particle.BLOCK, shLoc, 4, 0.1, 0.1, 0.1, 0, Material.SAND.createBlockData());

					for (Entity e : GeneralMethods.getEntitiesAroundPoint(shLoc, 1.2)) {
						if (e instanceof LivingEntity && e.getUniqueId() != player.getUniqueId()) {
							LivingEntity target = (LivingEntity) e;
							DamageHandler.damageEntity(target, 2.0, sourceAbility);
							target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 30, 1));
							cancel();
							return;
						}
					}

					Block b = shLoc.getBlock();
					if (com.projectkorra.projectkorra.ability.EarthAbility.isEarthbendable(player, b)) {
						new TempBlock(b, Material.SAND).setRevertTime(5000);
					}
				}
			}.runTaskTimer(AmonPackPlugin.plugin, 0, 1);
		}
	}
}

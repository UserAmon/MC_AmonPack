package Abilities.PK_Abilities.Water;

import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.WaterAbility;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.TempBlock;
import Plugin.AmonPackPlugin;
import Plugin.Methods;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class SacrificialHeal extends WaterAbility implements AddonAbility {
	private int state = 0;
	private int slot;
	private Location sourceLoc;
	private Location currentWaterLoc;
	private double targetAngle = 0.0;
	private int healingTicks = 0;
	private double totalHealed = 0.0;
	private int sourcingTicks = 0;

	public SacrificialHeal(Player player) {
		super(player);
		if (bPlayer.isOnCooldown(this)) {
			return;
		}
		if (!bPlayer.canBend(this)) {
			return;
		}

		Location source = Methods.findWaterSource(player, 15);
		if (source == null) {
			return;
		}

		this.slot = player.getInventory().getHeldItemSlot();
		this.sourceLoc = source;
		this.currentWaterLoc = sourceLoc.clone();
		this.state = 1;
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
			if (!player.isSneaking()) {
				remove();
				return;
			}

			sourcingTicks++;
			Location handLoc = getHandLocation();
			Vector dir = handLoc.toVector().subtract(currentWaterLoc.toVector());
			double dist = dir.length();

			if (dist < 1.5 || sourcingTicks > 40) {
				state = 2;
				targetAngle = 0.0;
				player.getWorld().playSound(player.getLocation(), Sound.BLOCK_WATER_AMBIENT, 1f, 1.2f);
			} else {
				dir.normalize().multiply(0.6);
				currentWaterLoc.add(dir);
				new TempBlock(currentWaterLoc.getBlock(), Material.WATER).setRevertTime(100);
				player.getWorld().spawnParticle(org.bukkit.Particle.DUST, currentWaterLoc, 4, 0.1, 0.1, 0.1, 0, new org.bukkit.Particle.DustOptions(org.bukkit.Color.fromRGB(0, 191, 255), 1.0f));
			}
		} else if (state == 2) {
			if (!player.isSneaking()) {
				remove();
				return;
			}

			renderWeavingWaterSphere();

			if (targetAngle < 360.0) {
				for (double a = 0; a < 360; a += 15) {
					double r = Math.toRadians(a);
					Vector off = new Vector(Math.cos(r) * 2.0, 0.0, Math.sin(r) * 2.0);
					Location pLoc = player.getLocation().clone().add(0, 1.0, 0).add(off);
					if (a < targetAngle) {
						player.spawnParticle(org.bukkit.Particle.DUST, pLoc, 1, 0, 0, 0, 0, new org.bukkit.Particle.DustOptions(org.bukkit.Color.fromRGB(50, 205, 50), 0.8f));
					} else {
						player.spawnParticle(org.bukkit.Particle.DUST, pLoc, 1, 0, 0, 0, 0, new org.bukkit.Particle.DustOptions(org.bukkit.Color.fromRGB(192, 192, 192), 0.5f));
					}
				}

				double rad = Math.toRadians(targetAngle);
				Vector offset = new Vector(Math.cos(rad) * 2.0, 0.0, Math.sin(rad) * 2.0);
				Location particleLoc = player.getLocation().clone().add(0, 1.0, 0).add(offset);
				player.spawnParticle(org.bukkit.Particle.DUST, particleLoc, 5, 0.05, 0.05, 0.05, 0, new org.bukkit.Particle.DustOptions(org.bukkit.Color.fromRGB(0, 191, 255), 2.0f));

				Vector toParticle = particleLoc.toVector().subtract(player.getEyeLocation().toVector()).normalize();
				double dot = player.getEyeLocation().getDirection().normalize().dot(toParticle);
				if (dot > 0.94) {
					targetAngle += 6.0;
					if (((int) targetAngle) % 30 == 0) {
						player.getWorld().playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.3f, 1.4f);
					}
				}
			} else {
				player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, new net.md_5.bungee.api.chat.TextComponent(ChatColor.AQUA + "Look down to begin healing / Spójrz w dół"));
				Location feetLoc = player.getLocation().clone().subtract(0, 0.2, 0);
				player.spawnParticle(org.bukkit.Particle.DUST, feetLoc, 6, 0.3, 0.0, 0.3, 0, new org.bukkit.Particle.DustOptions(org.bukkit.Color.fromRGB(0, 191, 255), 1.5f));

				if (player.getLocation().getPitch() > 70.0) {
					state = 3;
					healingTicks = 0;
					totalHealed = 0.0;
					player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_SPLASH, 1f, 1f);
				}
			}
		} else if (state == 3) {
			if (!player.isSneaking()) {
				bPlayer.addCooldown(this);
				remove();
				return;
			}

			healingTicks++;
			if (healingTicks % 10 == 0) {
				double maxH = player.getMaxHealth();
				if (player.getHealth() < maxH) {
					double newH = Math.min(maxH, player.getHealth() + 1.0);
					totalHealed += (newH - player.getHealth());
					player.setHealth(newH);
					player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.4f, 1.3f);
				}
			}

			double progressRatio = (double) healingTicks / 120.0;
			double orbitSpeed = 6.0;
			double currentOrbitAngle = healingTicks * orbitSpeed;

			for (int j = 0; j < 2; j++) {
				double angleRad = Math.toRadians(currentOrbitAngle + (j * 180.0));
				double yOff = 0.3 + Math.sin(Math.toRadians(healingTicks * 4.0)) * 0.7;
				Vector offset = new Vector(Math.cos(angleRad) * 1.2, yOff, Math.sin(angleRad) * 1.2);
				Location orbLoc = player.getLocation().clone().add(offset);

				if (Math.random() > progressRatio) {
					player.getWorld().spawnParticle(org.bukkit.Particle.DUST, orbLoc, 2, 0.05, 0.05, 0.05, 0, new org.bukkit.Particle.DustOptions(org.bukkit.Color.fromRGB(0, 191, 255), 1.0f));
					player.getWorld().spawnParticle(org.bukkit.Particle.HAPPY_VILLAGER, orbLoc, 1, 0, 0, 0, 0);
					Block b = orbLoc.getBlock();
					if (isAir(b.getType()) || isWater(b)) {
						new TempBlock(b, Material.WATER).setRevertTime(150);
					}
				} else {
					player.getWorld().spawnParticle(org.bukkit.Particle.DUST, orbLoc, 2, 0.05, 0.05, 0.05, 0, new org.bukkit.Particle.DustOptions(org.bukkit.Color.fromRGB(139, 0, 0), 1.2f));
				}
			}

			if (healingTicks >= 120) {
				state = 4;
				double sacrificeDmg = totalHealed / 2.0;
				if (sacrificeDmg > 0) {
					double newHealth = player.getHealth() - sacrificeDmg;
					player.setHealth(Math.max(1.0, newHealth));
					player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT, 1f, 0.7f);
					player.getWorld().spawnParticle(org.bukkit.Particle.DUST, player.getEyeLocation(), 20, 0.4, 0.4, 0.4, 0, new org.bukkit.Particle.DustOptions(org.bukkit.Color.fromRGB(139, 0, 0), 1.8f));
				}
				player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 0.5f);
			}
		} else if (state == 4) {
			if (!player.isSneaking()) {
				launchBloodDiscs();
				bPlayer.addCooldown(this);
				remove();
				return;
			}

			Location discCenter = getHandLocation().add(player.getLocation().getDirection().multiply(1.5));
			Vector base = new Vector(0, 0.5, 0);
			for (double angle = 0; angle < 360; angle += 45) {
				Vector offset = GeneralMethods.getOrthogonalVector(base, angle, 0.5);
				player.getWorld().spawnParticle(org.bukkit.Particle.DUST, discCenter.clone().add(offset), 1, 0, 0, 0, 0, new org.bukkit.Particle.DustOptions(org.bukkit.Color.fromRGB(139, 0, 0), 1.2f));
			}
		}
	}

	private Location getHandLocation() {
		Location hand = player.getLocation().clone().add(0, 1.1, 0);
		Vector right = player.getLocation().getDirection().clone().crossProduct(new Vector(0, 1, 0)).normalize().multiply(0.35);
		return hand.add(right);
	}

	private void launchBloodDiscs() {
		Location spawn = getHandLocation().add(player.getLocation().getDirection().multiply(1.5));
		Vector dir = player.getLocation().getDirection().normalize();
		player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GHAST_SHOOT, 0.8f, 0.7f);

		new BukkitRunnable() {
			private Location loc = spawn.clone();
			private int t = 0;

			@Override
			public void run() {
				t++;
				if (t > 25 || loc.getBlock().getType().isSolid()) {
					loc.getWorld().spawnParticle(org.bukkit.Particle.DUST, loc, 15, 0.2, 0.2, 0.2, 0, new org.bukkit.Particle.DustOptions(org.bukkit.Color.fromRGB(139, 0, 0), 1.4f));
					loc.getWorld().playSound(loc, Sound.ENTITY_SLIME_SQUISH, 1f, 0.8f);
					cancel();
					return;
				}

				loc.add(dir.clone().multiply(1.0));
				
				Vector base = new Vector(0, 0.5, 0);
				for (double angle = 0; angle < 360; angle += 45) {
					Vector offset = GeneralMethods.getOrthogonalVector(base, angle, 0.4);
					loc.getWorld().spawnParticle(org.bukkit.Particle.DUST, loc.clone().add(offset), 1, 0, 0, 0, 0, new org.bukkit.Particle.DustOptions(org.bukkit.Color.fromRGB(139, 0, 0), 1.2f));
				}

				for (Entity e : GeneralMethods.getEntitiesAroundPoint(loc, 1.5)) {
					if (e instanceof LivingEntity && e.getUniqueId() != player.getUniqueId()) {
						LivingEntity target = (LivingEntity) e;
						DamageHandler.damageEntity(target, 4.0, SacrificialHeal.this);
						target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 2));
						target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100, 1));
						
						if (target instanceof Player) {
							BendingPlayer targetBP = BendingPlayer.getBendingPlayer((Player) target);
							if (targetBP != null) {
								targetBP.blockChi();
							}
						}

						loc.getWorld().spawnParticle(org.bukkit.Particle.DUST, loc, 25, 0.3, 0.3, 0.3, 0, new org.bukkit.Particle.DustOptions(org.bukkit.Color.fromRGB(139, 0, 0), 1.6f));
						loc.getWorld().playSound(loc, Sound.ENTITY_PLAYER_SPLASH, 1f, 0.7f);
						cancel();
						return;
					}
				}
			}
		}.runTaskTimer(AmonPackPlugin.plugin, 0, 1);
	}

	@Override
	public long getCooldown() {
		return 8000;
	}

	@Override
	public Location getLocation() {
		return player.getLocation();
	}

	@Override
	public String getName() {
		return "SacrificialHeal";
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
		return "A multi-phase dark waterbending art. Draw water to your front, weave a circle and look down to begin self-healing. Holding the healing to its end sacrifices half the healed health to weave blood discs, which can be fired by releasing Shift to damage and chi-block your target.";
	}

	@Override
	public String getInstructions() {
		return "Hold shift at water source. Follow prompt particles in a circle with your crosshair, then look down to heal. Release shift to cancel or fire the blood disc once the sacrifice phase is reached.";
	}

	private void renderWeavingWaterSphere() {
		Location center = player.getEyeLocation().add(player.getLocation().getDirection().multiply(2.0));
		Block centerB = center.getBlock();
		if (isAir(centerB.getType()) || isWater(centerB)) {
			new TempBlock(centerB, Material.WATER).setRevertTime(100);
		}
		org.bukkit.block.BlockFace[] faces = { org.bukkit.block.BlockFace.UP, org.bukkit.block.BlockFace.DOWN, org.bukkit.block.BlockFace.EAST, org.bukkit.block.BlockFace.WEST, org.bukkit.block.BlockFace.NORTH, org.bukkit.block.BlockFace.SOUTH };
		for (org.bukkit.block.BlockFace face : faces) {
			Block adj = centerB.getRelative(face);
			if (isAir(adj.getType()) && Math.random() < 0.4) {
				new TempBlock(adj, Material.WATER).setRevertTime(100);
			}
		}
	}
}

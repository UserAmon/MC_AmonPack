package Abilities.PK_Abilities.Water;

import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.HealingAbility;
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

public class CalmTide extends HealingAbility implements AddonAbility {
	private int state = 0;
	private int slot;
	private Location sourceLoc;
	private Location currentWaterLoc;
	private double targetAngle = 0.0;
	private int healingTicks = 0;
	private double totalHealed = 0.0;
	private int sourcingTicks = 0;
	private Vector weaveStartDir;

	public CalmTide(Player player) {
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
				weaveStartDir = player.getLocation().getDirection().setY(0).normalize().multiply(2.0);
				if (weaveStartDir.lengthSquared() == 0) weaveStartDir = new Vector(2.0, 0, 0);
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
					double cos = Math.cos(r);
					double sin = Math.sin(r);
					Vector off = new Vector(weaveStartDir.getX() * cos - weaveStartDir.getZ() * sin, 0, weaveStartDir.getX() * sin + weaveStartDir.getZ() * cos);
					Location pLoc = player.getLocation().clone().add(0, 1.0, 0).add(off);
					if (a < targetAngle) {
						player.spawnParticle(org.bukkit.Particle.DUST, pLoc, 1, 0, 0, 0, 0, new org.bukkit.Particle.DustOptions(org.bukkit.Color.fromRGB(50, 205, 50), 0.8f));
					} else {
						player.spawnParticle(org.bukkit.Particle.DUST, pLoc, 1, 0, 0, 0, 0, new org.bukkit.Particle.DustOptions(org.bukkit.Color.fromRGB(192, 192, 192), 0.5f));
					}
				}

				double rad = Math.toRadians(targetAngle);
				double cos = Math.cos(rad);
				double sin = Math.sin(rad);
				Vector offset = new Vector(weaveStartDir.getX() * cos - weaveStartDir.getZ() * sin, 0, weaveStartDir.getX() * sin + weaveStartDir.getZ() * cos);
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

				player.getWorld().spawnParticle(org.bukkit.Particle.DUST, orbLoc, 2, 0.05, 0.05, 0.05, 0, new org.bukkit.Particle.DustOptions(org.bukkit.Color.fromRGB(0, 191, 255), 1.0f));
				player.getWorld().spawnParticle(org.bukkit.Particle.HAPPY_VILLAGER, orbLoc, 1, 0, 0, 0, 0);
				Block b = orbLoc.getBlock();
				if (isAir(b.getType()) || isWater(b)) {
					new TempBlock(b, Material.WATER).setRevertTime(150);
				}
			}

			if (healingTicks >= 120) {
				player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100, 1));
				player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 1.5f);
				bPlayer.addCooldown(this);
				remove();
				return;
			}
		}
	}

	private Location getHandLocation() {
		Location hand = player.getLocation().clone().add(0, 1.1, 0);
		Vector right = player.getLocation().getDirection().clone().crossProduct(new Vector(0, 1, 0)).normalize().multiply(0.35);
		return hand.add(right);
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
		return "CalmTide";
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
		return "A calming waterbending art. Draw water to your front, weave a circle, and look down to begin self-healing. Restores your life force and provides a burst of speed upon completion.";
	}

	@Override
	public String getInstructions() {
		return "Hold Shift at a water source. Follow prompt particles in a circle with your crosshair, then look down to heal. Release Shift to cancel.";
	}

	private void renderWeavingWaterSphere() {
		Location center = player.getEyeLocation().add(player.getLocation().getDirection().multiply(3.0));
		Block centerB = center.getBlock();
		if (isAir(centerB.getType()) || isWater(centerB)) {
			new TempBlock(centerB, Material.WATER).setRevertTime(100);
		}
		org.bukkit.block.BlockFace[] faces = { org.bukkit.block.BlockFace.UP, org.bukkit.block.BlockFace.DOWN, org.bukkit.block.BlockFace.EAST, org.bukkit.block.BlockFace.WEST, org.bukkit.block.BlockFace.NORTH, org.bukkit.block.BlockFace.SOUTH };
		for (org.bukkit.block.BlockFace face : faces) {
			Block adj = centerB.getRelative(face);
			if (isAir(adj.getType()) && Math.random() < 0.15) {
				new TempBlock(adj, Material.WATER).setRevertTime(100);
			}
		}
	}
}

package Abilities.PK_Abilities.Earth;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.EarthAbility;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.ParticleEffect;
import com.projectkorra.projectkorra.util.TempBlock;
import Plugin.AmonPackPlugin;
import Plugin.Methods;
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

public class SandRupture extends EarthAbility implements AddonAbility {
	private int state = 0;
	private int slot;
	private long startTime;
	private boolean charged = false;
	private int chargeProgress = 0;

	private Location waveLoc;
	private Vector waveDir;
	private double waveDistanceTraveled = 0.0;
	private int tickCount = 0;

	public SandRupture(Player player) {
		super(player);
		if (bPlayer.isOnCooldown(this)) {
			return;
		}
		if (!bPlayer.canBend(this)) {
			return;
		}

		this.slot = player.getInventory().getHeldItemSlot();
		this.startTime = System.currentTimeMillis();
		this.state = 0;
		start();
	}

	private boolean isStandingOnSand(Player p) {
		Block below = p.getLocation().clone().subtract(0, 0.5, 0).getBlock();
		return below.getType() == Material.SAND || below.getType() == Material.RED_SAND;
	}

	@Override
	public void progress() {
		if (player.isDead() || !player.isOnline()) {
			remove();
			return;
		}

		if (player.getInventory().getHeldItemSlot() != slot) {
			bPlayer.addCooldown(this);
			remove();
			return;
		}

		if (state == 0) {
			if (player.isSneaking()) {
				if (isStandingOnSand(player)) {
					charged = true;
					state = 2;
					player.getWorld().playSound(player.getLocation(), Sound.BLOCK_SAND_PLACE, 1f, 1.5f);
				} else {
					state = 1;
				}
			} else {
				remove();
				return;
			}
		} else if (state == 1) {
			if (!player.isSneaking()) {
				remove();
				return;
			}

			chargeProgress++;
			ParticleEffect.BLOCK_CRACK.display(player.getLocation().clone().add(0, 0.8, 0), 3, 0.4, 0.4, 0.4, 0.05, Material.SAND.createBlockData());
			
			if (chargeProgress >= 40) {
				charged = true;
				state = 2;
				player.getWorld().playSound(player.getLocation(), Sound.BLOCK_SAND_PLACE, 1f, 1.5f);
			}
		} else if (state == 2) {
			if (!player.isSneaking()) {
				state = 3;
				waveLoc = player.getLocation().clone();
				waveDir = player.getLocation().getDirection().clone().setY(0).normalize();
				waveDistanceTraveled = 0.0;
				player.getWorld().playSound(player.getLocation(), Sound.ENTITY_HORSE_GALLOP, 1f, 0.8f);
			} else {
				ParticleEffect.BLOCK_CRACK.display(player.getLocation().clone().add(0, 0.8, 0), 4, 0.5, 0.5, 0.5, 0.05, Material.SAND.createBlockData());
			}
		} else if (state == 3) {
			tickCount++;
			waveLoc.add(waveDir.clone().multiply(0.7));
			waveDistanceTraveled += 0.7;

			adjustWaveY();

			Block b = waveLoc.getBlock();
			if (EarthAbility.isEarthbendable(player, b)) {
				new TempBlock(b, Material.SAND).setRevertTime(5000);
			}

			waveLoc.getWorld().spawnParticle(org.bukkit.Particle.BLOCK, waveLoc, 10, 0.5, 0.2, 0.5, 0.05, Material.SAND.createBlockData());

			if (tickCount % 2 == 0) {
				spawnWaveFallingBlock(waveLoc.clone().add(0, 0.5, 0));
			}

			for (Entity entity : GeneralMethods.getEntitiesAroundPoint(waveLoc, 1.6)) {
				if (entity instanceof LivingEntity && entity.getUniqueId() != player.getUniqueId()) {
					LivingEntity target = (LivingEntity) entity;
					DamageHandler.damageEntity(target, 4.0, this);
					target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 2));

					Vector knock = waveDir.clone().multiply(0.6).setY(0.35);
					target.setVelocity(knock);
				}
			}

			if (waveDistanceTraveled >= 12.0 || waveLoc.getBlock().getType().isSolid()) {
				explodeSand(waveLoc);
				bPlayer.addCooldown(this);
				remove();
			}
		}
	}

	private void adjustWaveY() {
		Location check = waveLoc.clone();
		for (int yOffset = 3; yOffset >= -3; yOffset--) {
			Location test = check.clone().add(0, yOffset, 0);
			if (test.getBlock().getType().isSolid()) {
				waveLoc.setY(test.getY() + 1.0);
				return;
			}
		}
	}

	private void spawnWaveFallingBlock(Location loc) {
		Methods.spawnFallingBlocks(loc, Material.SAND, 1, 2.0, player);
	}

	public void onLeftClick() {
		if (state != 0) {
			return;
		}

		Block targetBlock = player.getTargetBlockExact(15);
		if (targetBlock == null || (targetBlock.getType() != Material.SAND && targetBlock.getType() != Material.RED_SAND)) {
			return;
		}

		explodeSand(targetBlock.getLocation().add(0.5, 0.5, 0.5));
		bPlayer.addCooldown(this);
		remove();
	}

	private void explodeSand(Location loc) {
		loc.getWorld().spawnParticle(org.bukkit.Particle.BLOCK, loc, 40, 0.6, 0.6, 0.6, 0.1, Material.SAND.createBlockData());
		loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1.2f);

		Methods.spawnFallingBlocks(loc, Material.SAND, 8, 4.0, player);

		double radius = 3.0;
		for (Entity entity : GeneralMethods.getEntitiesAroundPoint(loc, radius)) {
			if (entity instanceof LivingEntity && entity.getUniqueId() != player.getUniqueId()) {
				LivingEntity target = (LivingEntity) entity;
				DamageHandler.damageEntity(target, 3.0, this);
				target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 30, 1));
			}
		}

		for (int x = -3; x <= 3; x++) {
			for (int y = -3; y <= 3; y++) {
				for (int z = -3; z <= 3; z++) {
					Block b = loc.clone().add(x, y, z).getBlock();
					if (loc.distance(b.getLocation()) <= radius && EarthAbility.isEarthbendable(player, b)) {
						new TempBlock(b, Material.SAND).setRevertTime(5000);
					}
				}
			}
		}
	}

	@Override
	public long getCooldown() {
		return 5000;
	}

	@Override
	public Location getLocation() {
		return waveLoc == null ? player.getLocation() : waveLoc;
	}

	@Override
	public String getName() {
		return "SandRupture";
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
		return "Launches sand explosions or massive ground sand waves. LPM detonates an existing sand block, producing a 3m sand field and blindness. Holding Shift charges a ground wave that travels 12 blocks and detonates at its end (charge time is skipped if standing on sand).";
	}

	@Override
	public String getInstructions() {
		return "Left-click on a sand block to detonate it, OR hold Shift (Sneak) to charge the ground wave and release Shift to launch it.";
	}
}

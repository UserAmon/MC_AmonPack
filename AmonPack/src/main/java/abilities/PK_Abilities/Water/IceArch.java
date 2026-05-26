package Abilities.PK_Abilities.Water;

import com.projectkorra.projectkorra.attribute.Attribute;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.EarthAbility;
import com.projectkorra.projectkorra.ability.IceAbility;
import com.projectkorra.projectkorra.ability.WaterAbility;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.ParticleEffect;
import com.projectkorra.projectkorra.util.TempBlock;

import Plugin.AmonPackPlugin;
import Plugin.Methods;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class IceArch extends IceAbility implements AddonAbility {

	private int Cooldown;
	private int Range;
	private int Dmg;
	private int ArchWidth;
	private int ArchDuration;
	private int Thick;
	private int chargetime;
	private boolean freeze;
	private int freezeDuration;

	int act;
	Location origin;
	Location projectile;
	Vector dir;
	int i;
	public int abilityState;

	// Rising animation state
	private int risingTick = 0;
	private static final int RISING_TICKS = 25;   // ~1.25 seconds
	private static final int CONVERSION_RADIUS = 5;
	private Location riseOrigin;
	// track which enemies already got spiked
	private final List<Entity> spikedEnemies = new ArrayList<>();

	public IceArch(Player player) {
		super(player);
		if (bPlayer.isOnCooldown(this)) {
			return;
		}
		if (!bPlayer.canBend(this)) {
			return;
		}
		SetConf();
		act = 0;
		abilityState = 0;
		i = 0;
		Location loc = Methods.getTargetLocation(player, 20);
		if (!player.isSneaking()) {
			if (WaterAbility.isWaterbendable(loc.getBlock().getBlockData().getMaterial())
					|| WaterAbility.isPlantbendable(player, loc.getBlock().getBlockData().getMaterial(), false)
					|| WaterAbility.isIcebendable(player, loc.getBlock().getBlockData().getMaterial(), false)
					|| WaterAbility.isSnow(loc.getBlock().getBlockData().getMaterial())
					|| WaterAbility.isWater(loc.getBlock().getBlockData().getMaterial())
					|| loc.getBlock().getBlockData().getMaterial() == Material.GRASS_BLOCK) {
				Methods.SmoothBlock(player, loc, Material.WATER, true);
				start();
				bPlayer.addCooldown(this);
			}
		}

	}

	@Override
	public void progress() {
		if (bPlayer.getBoundAbility() == null || bPlayer.getBoundAbilityName() == null) {
			if (player.isSneaking() && i == 0) {
				bPlayer.addCooldown(this);
				remove();
			}
		}
		if (bPlayer.getBoundAbility() != null || bPlayer.getBoundAbilityName() != null) {
			if (!bPlayer.getBoundAbilityName().equalsIgnoreCase("IceArch")) {
				if (player.isSneaking() && i == 0) {
					bPlayer.addCooldown(this);
					remove();
				}
			}
			if (player.isDead() || !player.isOnline()) {
				remove();
				return;
			}
			if (abilityState == 0) {
				if (player.isSneaking()) {
					bPlayer.addCooldown(this);
					if (System.currentTimeMillis() > getStartTime() + chargetime) {
						abilityState = 1;
					}
				} else if (!player.isSneaking()) {
					remove();
				}
			} else if (abilityState == 1) {
				if (player.isSneaking() && i == 0) {
					bPlayer.addCooldown(this);
					ParticleEffect.BLOCK_CRACK.display(
							player.getLocation().clone().add(player.getLocation().getDirection()).multiply(1), 4, 0.3,
							0.3, 0.3, 0, Material.ICE.createBlockData());
				} else if (!player.isSneaking() || i == 1) {
					// Transition to RISING animation state on first release
					if (i == 0) {
						i = 1;
						risingTick = 0;
						riseOrigin = player.getLocation().clone();
						abilityState = 2;
						// Slight upward boost to the player
						player.setVelocity(player.getVelocity().add(new Vector(0, 0.45, 0)));
						bPlayer.addCooldown(this);
						return;
					}
					// State 1 firing (i==1 means rising is done, now fire)
					runFiringPhase();
				}
			} else if (abilityState == 2) {
				// RISING animation
				progressRising();
			}
		} else {
			bPlayer.addCooldown(this);
			remove();
		}

	}

	/**
	 * Smooth rising animation: converts nearby bendable blocks to snow/ice,
	 * sends ice particles, and creates ice spikes under nearby enemies.
	 */
	private void progressRising() {
		risingTick++;

		// Particle burst around player
		Location pLoc = player.getLocation().clone().add(0, 0.5, 0);
		ParticleEffect.BLOCK_CRACK.display(pLoc, 8, 1.2, 0.4, 1.2, 0.05, Material.ICE.createBlockData());
		ParticleEffect.BLOCK_CRACK.display(pLoc, 5, 0.8, 0.2, 0.8, 0.03, Material.SNOW_BLOCK.createBlockData());
		pLoc.getWorld().spawnParticle(Particle.SNOWFLAKE, pLoc, 12, 1.5, 0.5, 1.5, 0.08);

		// Gradually expand the conversion radius
		int currentRadius = 1 + (risingTick * CONVERSION_RADIUS / RISING_TICKS);
		convertBlocksAround(riseOrigin, currentRadius);

		// Ice spikes under nearby enemies (only once per enemy)
		for (Entity entity : GeneralMethods.getEntitiesAroundPoint(riseOrigin, 7)) {
			if (!(entity instanceof LivingEntity)) continue;
			if (entity.getUniqueId().equals(player.getUniqueId())) continue;
			if (spikedEnemies.contains(entity)) continue;

			spikedEnemies.add(entity);
			Location enemyLoc = entity.getLocation().clone();
			// Animate a growing spike: 3 blocks tall from ground
			spawnIceSpikeAnimation(enemyLoc);
			DamageHandler.damageEntity(entity, Dmg * 0.5, this);
		}

		// After RISING_TICKS ticks, switch to firing phase
		if (risingTick >= RISING_TICKS) {
			abilityState = 1; // back to firing, i is already 1
			dir = player.getLocation().getDirection().clone();
			origin = player.getLocation().add(0, 1, 0).clone();
			projectile = origin.clone();
		}
	}

	/**
	 * Converts earthbendable, plantbendable and waterbendable blocks
	 * in a sphere of given radius around 'center' to snow/ice.
	 */
	private void convertBlocksAround(Location center, int radius) {
		for (Block b : GeneralMethods.getBlocksAroundPoint(center, radius)) {
			// Only affect ground-level and below-eye-level blocks
			if (b.getY() > center.getY() + 1) continue;
			if (b.getY() < center.getY() - 2) continue;
			Material mat = b.getType();
			boolean isConvertible = WaterAbility.isWaterbendable(mat)
					|| WaterAbility.isPlantbendable(player, mat, false)
					|| EarthAbility.isEarthbendable(player, b);
			if (!isConvertible) continue;
			// Randomly assign snow or ice, favouring snow
			Material target = (Math.random() < 0.65) ? Material.SNOW_BLOCK : Material.PACKED_ICE;
			TempBlock tb = new TempBlock(b, target);
			tb.setRevertTime(ArchDuration + 5000L);
		}
	}

	/**
	 * Plays a growing ice column animation starting from 1 block below the
	 * target entity's feet, growing 3 blocks upward.
	 */
	private void spawnIceSpikeAnimation(Location enemyLoc) {
		Location base = enemyLoc.clone().subtract(0, 1, 0);
		for (int h = 0; h < 3; h++) {
			Location spikeLoc = base.clone().add(0, h, 0);
			Block block = spikeLoc.getBlock();
			if (block.getType().isAir() || block.getType() == Material.WATER) {
				TempBlock tb = new TempBlock(block, Material.ICE);
				tb.setRevertTime(ArchDuration + 4000L);
			}
			// Crack and snowflake particles for visual flair
			ParticleEffect.BLOCK_CRACK.display(spikeLoc.clone().add(0.5, 0.5, 0.5), 10, 0.3, 0.3, 0.3, 0.1,
					Material.ICE.createBlockData());
		}
		// Falling ice shards from the tip
		Methods.spawnFallingBlocks(base.clone().add(0, 3, 0), Material.ICE, 4, 0.9, player);
	}

	/**
	 * The original arch-projectile firing phase (runs after rising).
	 */
	private void runFiringPhase() {
		if (dir == null) {
			dir = player.getLocation().getDirection().clone();
			origin = player.getLocation().add(0, 1, 0).clone();
			projectile = origin.clone();
		}
		projectile.add(dir).multiply(1);
		ParticleEffect.BLOCK_CRACK.display(projectile, 15, 2, 2, 2, 0, Material.ICE.createBlockData());
		if (projectile.distance(origin) > Range || (projectile.getBlock().getType().isSolid()
				&& projectile.getBlock().getType() != Material.ICE)) {
			remove();
		}
		for (Entity entity : GeneralMethods.getEntitiesAroundPoint(projectile, ArchWidth)) {
			if ((entity instanceof LivingEntity) && entity.getUniqueId() != player.getUniqueId()
					&& entity.getLocation().distance(player.getLocation()) > entity.getLocation()
							.distance(projectile)) {
				DamageHandler.damageEntity(entity, Dmg, this);
			}
		}
		for (Block b : GeneralMethods.getBlocksAroundPoint(projectile, ArchWidth)) {
			if (b.getType() == Material.WATER) {
				TempBlock tb1 = new TempBlock(b, Material.ICE);
				tb1.setRevertTime(5000);
			} else if (b.getLocation().distance(projectile) > Thick && b.getType() == Material.AIR
					&& b.getLocation().distance(origin) > (Thick + 1)
					&& b.getLocation().distance(origin) > projectile.distance(origin)) {
				TempBlock tb1 = new TempBlock(b, Material.ICE);
				tb1.setRevertTime(ArchDuration);
			} else if (b.getLocation().distance(projectile) < Thick && b.getType() == Material.ICE
					&& b.getLocation().getY() >= (projectile.getY() - 1)) {
				TempBlock tb1 = new TempBlock(b, Material.AIR);
				tb1.setRevertTime(5000);
			}
		}
	}

	public void SetConf() {

		if (isNight(player.getWorld())) {
			if (isFullMoon(player.getWorld())) {
				Cooldown = AmonPackPlugin.plugin.getConfig()
						.getInt("AmonPack.Water.Ice.IceArch.FullMoonAugment.Cooldown");
				chargetime = AmonPackPlugin.plugin.getConfig()
						.getInt("AmonPack.Water.Ice.IceArch.FullMoonAugment.ChargeTime");
				Range = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Water.Ice.IceArch.FullMoonAugment.Range");
				Dmg = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Water.Ice.IceArch.FullMoonAugment.Damage");
				ArchWidth = AmonPackPlugin.plugin.getConfig()
						.getInt("AmonPack.Water.Ice.IceArch.FullMoonAugment.Arch-Width");
				ArchDuration = AmonPackPlugin.plugin.getConfig()
						.getInt("AmonPack.Water.Ice.IceArch.FullMoonAugment.Arch-Duration");
				Thick = AmonPackPlugin.plugin.getConfig()
						.getInt("AmonPack.Water.Ice.IceArch.FullMoonAugment.Arch-Thickness");
				freeze = AmonPackPlugin.plugin.getConfig()
						.getBoolean("AmonPack.Water.Ice.IceArch.FullMoonAugment.CanFreeze");
				freezeDuration = AmonPackPlugin.plugin.getConfig()
						.getInt("AmonPack.Water.Ice.IceArch.FullMoonAugment.FreezeDuration");
			} else {
				Cooldown = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Water.Ice.IceArch.NightAugment.Cooldown");
				chargetime = AmonPackPlugin.plugin.getConfig()
						.getInt("AmonPack.Water.Ice.IceArch.NightAugment.ChargeTime");
				Range = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Water.Ice.IceArch.NightAugment.Range");
				Dmg = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Water.Ice.IceArch.NightAugment.Damage");
				ArchWidth = AmonPackPlugin.plugin.getConfig()
						.getInt("AmonPack.Water.Ice.IceArch.NightAugment.Arch-Width");
				ArchDuration = AmonPackPlugin.plugin.getConfig()
						.getInt("AmonPack.Water.Ice.IceArch.NightAugment.Arch-Duration");
				Thick = AmonPackPlugin.plugin.getConfig()
						.getInt("AmonPack.Water.Ice.IceArch.NightAugment.Arch-Thickness");
				freeze = AmonPackPlugin.plugin.getConfig()
						.getBoolean("AmonPack.Water.Ice.IceArch.NightAugment.CanFreeze");
				freezeDuration = AmonPackPlugin.plugin.getConfig()
						.getInt("AmonPack.Water.Ice.IceArch.NightAugment.FreezeDuration");
			}
		} else if (isDay(player.getWorld())) {
			Cooldown = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Water.Ice.IceArch.Cooldown");
			Range = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Water.Ice.IceArch.Range");
			Dmg = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Water.Ice.IceArch.Damage");
			chargetime = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Water.Ice.IceArch.ChargeTime");
			ArchWidth = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Water.Ice.IceArch.Arch-Width");
			ArchDuration = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Water.Ice.IceArch.Arch-Duration");
			Thick = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Water.Ice.IceArch.Arch-Thickness");
			freeze = AmonPackPlugin.plugin.getConfig().getBoolean("AmonPack.Water.Ice.IceArch.CanFreeze");
			freezeDuration = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Water.Ice.IceArch.FreezeDuration");
		}
	}

	@Override
	public long getCooldown() {
		return Cooldown;
	}

	@Override
	public Location getLocation() {
		return null;
	}

	@Override
	public String getName() {
		return "IceArch";
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
		return "Builds a magnificent, protective arch of solid ice to block incoming projectiles and attacks.";
	}

	@Override
	public String getInstructions() {
		return "Charge ability by sneaking while looking at water-bendable sources, release shift to summon the ice arch.";
	}

}
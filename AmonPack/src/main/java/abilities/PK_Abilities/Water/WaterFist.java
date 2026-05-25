package Abilities.PK_Abilities.Water;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.WaterAbility;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.ParticleEffect;
import com.projectkorra.projectkorra.util.TempBlock;
import Abilities.Util_Objects.BetterParticles;
import Plugin.AmonPackPlugin;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class WaterFist extends WaterAbility implements AddonAbility {
	private int state = 0;
	private Location sourceLoc;
	private Location currentWaterLoc;
	private int slot;
	private long chargeStartTime;
	private long lastPunchTime = 0;
	private int clicksUsed = 0;
	private double speed = 0.8;

	public WaterFist(Player player) {
		super(player);
		if (bPlayer.isOnCooldown(this)) {
			return;
		}
		if (!bPlayer.canBend(this)) {
			return;
		}

		this.slot = player.getInventory().getHeldItemSlot();
		sourceLoc = findWaterSource(player.getLocation(), 20);
		if (sourceLoc == null) {
			return;
		}

		currentWaterLoc = sourceLoc.clone();
		state = 1;
		start();
	}

	private Location findWaterSource(Location center, int radius) {
		for (int x = -radius; x <= radius; x++) {
			for (int y = -radius; y <= radius; y++) {
				for (int z = -radius; z <= radius; z++) {
					Block b = center.clone().add(x, y, z).getBlock();
					if (WaterAbility.isWaterbendable(b.getType()) || WaterAbility.isWater(b.getType())) {
						return b.getLocation().add(0.5, 0.5, 0.5);
					}
				}
			}
		}
		return null;
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

			Location handLoc = getHandLocation();
			Vector dir = handLoc.toVector().subtract(currentWaterLoc.toVector());
			double dist = dir.length();
			
			if (dist < 1.5) {
				state = 2;
				chargeStartTime = System.currentTimeMillis();
				player.getWorld().playSound(player.getLocation(), Sound.ITEM_BOTTLE_FILL, 1f, 1.2f);
			} else {
				dir.normalize().multiply(speed);
				currentWaterLoc.add(dir);
				ParticleEffect.WATER_WAKE.display(currentWaterLoc, 4, 0.1, 0.1, 0.1, 0.05);
				if (Math.random() < 0.25) {
					player.getWorld().playSound(currentWaterLoc, Sound.BLOCK_WATER_AMBIENT, 0.5f, 1f);
				}
			}
		} else if (state == 2) {
			if (System.currentTimeMillis() - chargeStartTime > 15000) {
				bPlayer.addCooldown(this);
				remove();
				return;
			}

			renderWaterHand();
		}
	}

	private Location getHandLocation() {
		Location hand = player.getLocation().clone().add(0, 1.1, 0);
		Vector right = player.getLocation().getDirection().clone().crossProduct(new Vector(0, 1, 0)).normalize().multiply(0.35);
		return hand.add(right);
	}

	private void renderWaterHand() {
		Location handLoc = getHandLocation();
		Vector viewDir = player.getLocation().getDirection().normalize();
		
		Location p1 = handLoc.clone().add(viewDir.clone().multiply(0.4));
		Location p2 = handLoc.clone().add(viewDir.clone().multiply(0.8));
		Location p3 = handLoc.clone().add(viewDir.clone().multiply(1.2));

		spawnWaterBlock(p1.getBlock());
		spawnWaterBlock(p2.getBlock());
		spawnWaterBlock(p3.getBlock());

		ParticleEffect.WATER_WAKE.display(p1, 2, 0.05, 0.05, 0.05, 0.01);
		ParticleEffect.WATER_SPLASH.display(p1, 2, 0.05, 0.05, 0.05, 0.01);

		ParticleEffect.WATER_WAKE.display(p2, 3, 0.05, 0.05, 0.05, 0.01);
		ParticleEffect.WATER_SPLASH.display(p2, 3, 0.05, 0.05, 0.05, 0.01);

		ParticleEffect.WATER_WAKE.display(p3, 4, 0.08, 0.08, 0.08, 0.01);
		ParticleEffect.WATER_SPLASH.display(p3, 4, 0.08, 0.08, 0.08, 0.01);
	}

	private void spawnWaterBlock(Block b) {
		if (b.getType() == Material.AIR) {
			new TempBlock(b, Material.WATER).setRevertTime(100);
		}
	}

	public void onLeftClick() {
		if (state != 2) {
			return;
		}

		long now = System.currentTimeMillis();
		if (now - lastPunchTime < 1000) {
			return;
		}

		clicksUsed++;
		lastPunchTime = now;

		player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_1, 1f, 1.2f);

		Location start = player.getEyeLocation().clone().add(player.getLocation().getDirection().multiply(0.5));
		Vector dir = player.getLocation().getDirection().normalize();
		boolean hitTarget = false;

		for (double d = 0.5; d <= 7.0; d += 0.5) {
			Location point = start.clone().add(dir.clone().multiply(d));
			ParticleEffect.WATER_SPLASH.display(point, 6, 0.15, 0.15, 0.15, 0.05);
			ParticleEffect.WATER_WAKE.display(point, 3, 0.1, 0.1, 0.1, 0.05);

			for (Entity entity : GeneralMethods.getEntitiesAroundPoint(point, 1.4)) {
				if (entity instanceof LivingEntity && entity.getUniqueId() != player.getUniqueId()) {
					LivingEntity target = (LivingEntity) entity;
					
					DamageHandler.damageEntity(target, 4.0, this);
					
					Vector knock = target.getLocation().toVector().subtract(player.getLocation().toVector());
					if (knock.lengthSquared() > 0) {
						knock.normalize().multiply(0.85).setY(0.25);
						target.setVelocity(knock);
					}

					if (clicksUsed == 3) {
						target.getWorld().spawnParticle(org.bukkit.Particle.SNOWFLAKE, target.getLocation(), 40, 0.5, 0.5, 0.5, 0.1);
						target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 2));

						Block feet = target.getLocation().getBlock();
						if (feet.getType().isAir()) {
							new TempBlock(feet, Material.ICE).setRevertTime(2000);
						}
					}

					hitTarget = true;
					break;
				}
			}
			if (hitTarget) {
				break;
			}
		}

		if (clicksUsed >= 3) {
			bPlayer.addCooldown(this);
			remove();
		}
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
		return "WaterFist";
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
		return "Summons a floating liquid fist on your hand from a nearby water source. Once loaded, you can release sneak and punch up to 3 times. The final successful strike freezes the opponent's feet in solid ice.";
	}

	@Override
	public String getInstructions() {
		return "Hold sneak to draw water from a source within 20 blocks. Once fully loaded, left-click to throw punches (max 3 charges, 1s internal cooldown).";
	}
}

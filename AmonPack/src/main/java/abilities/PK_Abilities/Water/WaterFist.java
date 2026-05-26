package Abilities.PK_Abilities.Water;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.WaterAbility;
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

public class WaterFist extends WaterAbility implements AddonAbility {
	private int state = 0;
	private Location sourceLoc;
	private Location currentWaterLoc;
	private int slot;
	private long chargeStartTime;
	private long lastPunchTime = 0;
	private int clicksUsed = 0;
	private double speed = 0.8;

	// Reka: startuje 1 blok z boku (right), 1 blok do przodu, na wys. ramienia
	// Segmenty: od 1.0 do 4.0 (4 bloki dlugosci)
	private static final double HAND_FORWARD_OFFSET = 0.0; // przesuniecie poczatku reki od gracza
	private static final double HAND_RIGHT_OFFSET    = 1.5; // przesuniecie boczne
	private static final double HAND_HEIGHT          = 1.05; // wysokosc reki
	private static final double[] HAND_SEGMENTS      = {1.0, 1.6, 2.2, 2.8}; // 4 segmenty

	public WaterFist(Player player) {
		super(player);
		if (bPlayer.isOnCooldown(this)) {
			return;
		}
		if (!bPlayer.canBend(this)) {
			return;
		}

		this.slot = player.getInventory().getHeldItemSlot();
		sourceLoc = Methods.findWaterSource(player, 20);
		if (sourceLoc == null) {
			return;
		}

		currentWaterLoc = sourceLoc.clone();
		state = 0;
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

		if (state == 0) {
			if (player.isSneaking()) {
				state = 1;
			} else {
				remove();
				return;
			}
		} else if (state == 1) {
			if (!player.isSneaking()) {
				remove();
				return;
			}

			Location handLoc = getHandAnchor();
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
			renderWaterHand(HAND_SEGMENTS);
		}
	}

	/** Punkt kotwicy reki - 1 blok z boku i 1 blok do przodu od gracza */
	private Location getHandAnchor() {
		Location base = player.getLocation().clone().add(0, HAND_HEIGHT, 0);
		Vector forward = player.getLocation().getDirection().clone().setY(0).normalize();
		Vector right   = forward.clone().crossProduct(new Vector(0, 1, 0)).normalize();
		return base.add(forward.multiply(HAND_FORWARD_OFFSET)).add(right.multiply(HAND_RIGHT_OFFSET));
	}

	/** Renderuje segmenty reki wzdluz kierunku patrzenia gracza */
	private void renderWaterHand(double[] segments) {
		Location anchor = getHandAnchor();
		Vector viewDir  = player.getLocation().getDirection().clone().setY(0).normalize();
		Vector rightVec = viewDir.clone().crossProduct(new Vector(0, 1, 0)).normalize();
		viewDir.add(rightVec.multiply(0.12)).normalize(); // Lekkie oddalenie od celownika (outward)

		for (double seg : segments) {
			Location p = anchor.clone().add(viewDir.clone().multiply(seg));
			spawnWaterBlock(p.getBlock());
			int amt = (seg < 2.0) ? 2 : (seg < 3.0) ? 3 : 4;
			double spread = (seg < 2.0) ? 0.05 : 0.08;
			ParticleEffect.WATER_WAKE.display(p, amt, spread, spread, spread, 0.01);
			ParticleEffect.WATER_SPLASH.display(p, amt, spread, spread, spread, 0.01);
		}
	}

	private void spawnWaterBlock(Block b) {
		if (b.getType() == Material.AIR) {
			new TempBlock(b, Material.WATER).setRevertTime(170);
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

		RPG.Levels.BendingTree.PlayerBendingBranch branch = AmonPackPlugin.levelsBending.GetBranchByPlayerName(player.getName());
		boolean hasUppercut = (branch != null && branch.hasUpgrade("Uppercut"));
		int maxClicks = hasUppercut ? 4 : 3;

		clicksUsed++;
		lastPunchTime = now;
		final int thisClick = clicksUsed;

		player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_1, 1f, 1.2f);

		// ---- Animacja: extend 3 -> 7/8 blokow, potem retract ----
		Location anchor   = getHandAnchor();
		double maxReach = hasUppercut ? 8.0 : 7.0;
		// Celownik na max reach do przodu od głowy gracza
		Location targetHit = player.getEyeLocation().clone().add(player.getLocation().getDirection().normalize().multiply(maxReach));
		Vector viewDir = targetHit.toVector().subtract(anchor.toVector()).normalize();
		final boolean[] hitDone = {false};

		new BukkitRunnable() {
			int tick = 0;
			// Fazy: 0-5 extend (0.5 bloku/tick), 6-10 retract
			final int EXTEND_TICKS  = 6;
			final int RETRACT_TICKS = 5;
			final double EXTEND_SPEED = 0.5;  // bloków/tick
			final double MAX_REACH    = maxReach;

			@Override
			public void run() {
				tick++;
				int totalTicks = EXTEND_TICKS + RETRACT_TICKS;
				if (tick > totalTicks || hitDone[0]) {
					cancel();
					return;
				}

				// Oblicz ile segmentow widac w tym ticku
				double reachNow;
				if (tick <= EXTEND_TICKS) {
					reachNow = Math.min(tick * EXTEND_SPEED + 3.0, MAX_REACH); // start od 3 (normalny zasiag)
				} else {
					double retractProgress = (tick - EXTEND_TICKS) / (double) RETRACT_TICKS;
					reachNow = MAX_REACH * (1.0 - retractProgress) + 1.0;
				}

				// Rysuj segmenty tymczasowe
				double step = 0.6;
				for (double d = 1.0; d <= reachNow; d += step) {
					Location p = anchor.clone().add(viewDir.clone().multiply(d));
					spawnWaterBlock(p.getBlock());
					ParticleEffect.WATER_WAKE.display(p, 3, 0.08, 0.08, 0.08, 0.02);
					ParticleEffect.WATER_SPLASH.display(p, 2, 0.08, 0.08, 0.08, 0.02);

					// Hit detection na koncu wyciagnietego punchowania
					if (tick <= EXTEND_TICKS && d >= reachNow - step) {
						for (Entity entity : GeneralMethods.getEntitiesAroundPoint(p, 1.4)) {
							if (entity instanceof LivingEntity && !entity.getUniqueId().equals(player.getUniqueId())) {
								LivingEntity target = (LivingEntity) entity;
								DamageHandler.damageEntity(target, 4.0, WaterFist.this);

								Vector knock = target.getLocation().toVector().subtract(player.getLocation().toVector());
								if (knock.lengthSquared() > 0) {
									knock.normalize().multiply(0.85).setY(0.25);
									target.setVelocity(knock);
								}

								if (hasUppercut || thisClick == 3) {
									target.getWorld().spawnParticle(org.bukkit.Particle.SNOWFLAKE, target.getLocation(), 40, 0.5, 0.5, 0.5, 0.1);
									target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 2));
									Block feet = target.getLocation().getBlock();
									if (feet.getType().isAir()) {
										new TempBlock(feet, Material.ICE).setRevertTime(2000);
									}
								}
								hitDone[0] = true;
								cancel();
								return;
							}
						}
					}
				}
			}
		}.runTaskTimer(AmonPackPlugin.plugin, 0, 1);

		if (clicksUsed >= maxClicks) {
			bPlayer.addCooldown(this);
			remove();
		}
	}

	@Override
	public long getCooldown() {
		RPG.Levels.BendingTree.PlayerBendingBranch branch = AmonPackPlugin.levelsBending.GetBranchByPlayerName(player.getName());
		boolean hasUppercut = (branch != null && branch.hasUpgrade("Uppercut"));
		return hasUppercut ? 4000 : 8000;
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
		return "Summons a floating liquid fist on your main hand from a nearby water source. Once loaded, left-click to throw punches (3 uses). The third strike freezes the target in ice.";
	}

	@Override
	public String getInstructions() {
		return "Hold sneak to draw water from within 20 blocks. Release sneak to keep the fist, left-click to punch (max 3 times, 1s cooldown between punches).";
	}
}

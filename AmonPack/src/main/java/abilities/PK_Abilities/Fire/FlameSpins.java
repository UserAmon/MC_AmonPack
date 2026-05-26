package Abilities.PK_Abilities.Fire;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.FireAbility;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.ParticleEffect;
import Plugin.AmonPackPlugin;
import Plugin.Methods;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class FlameSpins extends FireAbility implements AddonAbility {
	private int state = 0;
	private int slot;
	private long startTime;
	private long castTime;
	private long lastPunchTime = 0;
	private int clicksUsed = 0;

	public FlameSpins(Player player) {
		super(player);
		if (bPlayer.isOnCooldown(this)) {
			return;
		}
		if (!bPlayer.canBend(this)) {
			return;
		}

		this.slot = player.getInventory().getHeldItemSlot();
		this.startTime = System.currentTimeMillis();
		this.castTime = System.currentTimeMillis();


		// Flame particles burst at feet
		ParticleEffect.FLAME.display(player.getLocation().clone().add(0, 0.15, 0), 25, 0.4, 0.1, 0.4, 0.08);
		player.spawnParticle(Particle.LAVA, player.getLocation().clone().add(0, 0.2, 0), 6, 0.3, 0.1, 0.3, 0);
		player.getWorld().spawnParticle(org.bukkit.Particle.EXPLOSION, player.getLocation(), 1);
		player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.2f);
		player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1f, 0.8f);

		// Slow falling - 2.5 sekundy opadania (3.5 dla Firefly)
		RPG.Levels.BendingTree.PlayerBendingBranch branch = AmonPackPlugin.levelsBending.GetBranchByPlayerName(player.getName());
		boolean hasFirefly = (branch != null && branch.hasUpgrade("Firefly"));
		int hoverTicks = hasFirefly ? 70 : 50;
		player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, hoverTicks, 0, false, false));

		// Dash - poziome w 100% z ruchu gracza
		Vector motion  = player.getVelocity().clone().setY(0).multiply(1.35);
		Vector dash    = motion.clone();
		dash.setY(0.85); // mocno w gore
		player.setVelocity(dash);

		for (Entity entity : GeneralMethods.getEntitiesAroundPoint(player.getLocation(), 3.5)) {
			if (entity instanceof LivingEntity && entity.getUniqueId() != player.getUniqueId()) {
				DamageHandler.damageEntity(entity, 3.0, this);
			}
		}

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
			bPlayer.addCooldown(this);
			remove();
			return;
		}

		if (System.currentTimeMillis() - startTime > 10000) {
			bPlayer.addCooldown(this);
			remove();
			return;
		}

		if (state == 1) {
			if (player.isOnGround() && System.currentTimeMillis() - castTime > 500) {
				if (player.hasPotionEffect(PotionEffectType.SLOW_FALLING)) {
					player.removePotionEffect(PotionEffectType.SLOW_FALLING);
				}
				bPlayer.addCooldown(this);
				remove();
				return;
			}

			Location feet = player.getLocation().clone().add(0, -0.4, 0);
			Vector rightVec = player.getLocation().getDirection().crossProduct(new Vector(0, 1, 0)).normalize().multiply(0.25);
			
			Location rightFoot = feet.clone().add(rightVec);
			Location leftFoot = feet.clone().subtract(rightVec);
			
			ParticleEffect.FLAME.display(rightFoot, 1, 0, 0, 0, 0);
			ParticleEffect.FLAME.display(leftFoot, 1, 0, 0, 0, 0);
		}
	}

	public void onLeftClick() {
		if (state != 1) {
			return;
		}

		long now = System.currentTimeMillis();
		if (now - lastPunchTime < 500) {
			return;
		}

		clicksUsed++;
		lastPunchTime = now;

		player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1f, 1.1f);

		Location projLoc = player.getEyeLocation().clone();
		Vector projDir = player.getLocation().getDirection().normalize().multiply(0.8);

		new BukkitRunnable() {
			int ticks = 0;
			double angle = 0;

			@Override
			public void run() {
				ticks++;
				if (ticks > 40 || projLoc.getBlock().getType().isSolid()) {
					cancel();
					return;
				}

				projDir.setY(projDir.getY() - 0.03);
				projLoc.add(projDir);

				angle += 0.5;
				double r = 0.6;
				double x1 = r * Math.cos(angle);
				double z1 = r * Math.sin(angle);
				Location p1 = projLoc.clone().add(x1, 0, z1);
				Location p2 = projLoc.clone().subtract(x1, 0, z1);

				ParticleEffect.FLAME.display(p1, 1, 0, 0, 0, 0);
				ParticleEffect.FLAME.display(p2, 1, 0, 0, 0, 0);
				ParticleEffect.SMOKE_NORMAL.display(projLoc, 1, 0.1, 0.1, 0.1, 0.01);

				for (Entity entity : GeneralMethods.getEntitiesAroundPoint(projLoc, 1.2)) {
					if (entity instanceof LivingEntity && entity.getUniqueId() != player.getUniqueId()) {
						DamageHandler.damageEntity(entity, 4.0, FlameSpins.this);
						entity.setFireTicks(50);
						cancel();
						return;
					}
				}
			}
		}.runTaskTimer(AmonPackPlugin.plugin, 0, 1);

		RPG.Levels.BendingTree.PlayerBendingBranch branch = AmonPackPlugin.levelsBending.GetBranchByPlayerName(player.getName());
		boolean hasFirefly = (branch != null && branch.hasUpgrade("Firefly"));
		int maxClicks = hasFirefly ? 3 : 2;

		if (clicksUsed >= maxClicks) {
			bPlayer.addCooldown(this);
			remove();
		}
	}

	@Override
	public long getCooldown() {
		RPG.Levels.BendingTree.PlayerBendingBranch branch = AmonPackPlugin.levelsBending.GetBranchByPlayerName(player.getName());
		boolean hasFirefly = (branch != null && branch.hasUpgrade("Firefly"));
		return hasFirefly ? 3000 : 6000;
	}

	@Override
	public Location getLocation() {
		return player.getLocation();
	}

	@Override
	public String getName() {
		return "FlameSpins";
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
		return "Initiates a flaming spin on shift, exploding at your feet and dashing you in the direction you look. While airborne, you can click LPM up to 2 times to throw spinning fire discs with gravity.";
	}

	@Override
	public String getInstructions() {
		return "Sneak (Shift) to trigger the flaming spin dash, then left-click (LPM) while airborne to throw fire discs (max 2 charges, 1s internal cooldown).";
	}
}

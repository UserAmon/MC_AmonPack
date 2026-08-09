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

	private long cooldown;
	private long cooldownFirefly;
	private int maxClicks;
	private int maxClicksFirefly;
	private int hoverTicks;
	private int hoverTicksFirefly;
	private double dashMultiplier;
	private double dashYForce;
	private double dashRange;
	private double dashDamage;
	private double projectileSpeed;
	private int projectileRange;
	private double projectileDamage;
	private int projectileFireTicks;

	public FlameSpins(Player player) {
		super(player);
		
		this.cooldown = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Fire.FlameSpins.Cooldown", 6000L);
		this.cooldownFirefly = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Fire.FlameSpins.CooldownFirefly", 3000L);
		this.maxClicks = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Fire.FlameSpins.MaxClicks", 2);
		this.maxClicksFirefly = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Fire.FlameSpins.MaxClicksFirefly", 3);
		this.hoverTicks = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Fire.FlameSpins.HoverTicks", 50);
		this.hoverTicksFirefly = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Fire.FlameSpins.HoverTicksFirefly", 70);
		this.dashMultiplier = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Fire.FlameSpins.DashMultiplier", 1.35);
		this.dashYForce = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Fire.FlameSpins.DashYForce", 0.85);
		this.dashRange = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Fire.FlameSpins.DashRange", 3.5);
		this.dashDamage = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Fire.FlameSpins.DashDamage", 3.0);
		this.projectileSpeed = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Fire.FlameSpins.ProjectileSpeed", 0.8);
		this.projectileRange = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Fire.FlameSpins.ProjectileRange", 40);
		this.projectileDamage = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Fire.FlameSpins.ProjectileDamage", 4.0);
		this.projectileFireTicks = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Fire.FlameSpins.ProjectileFireTicks", 50);

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
		int ht = hasFirefly ? this.hoverTicksFirefly : this.hoverTicks;
		player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, ht, 0, false, false));

		// Dash - poziome w 100% z ruchu gracza
		Vector motion  = player.getVelocity().clone().setY(0).multiply(this.dashMultiplier);
		Vector dash    = motion.clone();
		dash.setY(this.dashYForce); // mocno w gore
		player.setVelocity(dash);

		for (Entity entity : GeneralMethods.getEntitiesAroundPoint(player.getLocation(), this.dashRange)) {
			if (entity instanceof LivingEntity && entity.getUniqueId() != player.getUniqueId()) {
				DamageHandler.damageEntity(entity, this.dashDamage, this);
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
		Vector projDir = player.getLocation().getDirection().normalize().multiply(this.projectileSpeed);

		new BukkitRunnable() {
			int ticks = 0;
			double angle = 0;

			@Override
			public void run() {
				ticks++;
				if (ticks > FlameSpins.this.projectileRange || projLoc.getBlock().getType().isSolid()) {
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

				boolean isBlue = bPlayer.hasElement(com.projectkorra.projectkorra.Element.BLUE_FIRE) || bPlayer.canUseSubElement(com.projectkorra.projectkorra.Element.BLUE_FIRE);
				if (isBlue) {
					p1.getWorld().spawnParticle(org.bukkit.Particle.SOUL_FIRE_FLAME, p1, 1, 0, 0, 0, 0);
					p2.getWorld().spawnParticle(org.bukkit.Particle.SOUL_FIRE_FLAME, p2, 1, 0, 0, 0, 0);
				} else {
					ParticleEffect.FLAME.display(p1, 1, 0, 0, 0, 0);
					ParticleEffect.FLAME.display(p2, 1, 0, 0, 0, 0);
				}
				ParticleEffect.SMOKE_NORMAL.display(projLoc, 1, 0.1, 0.1, 0.1, 0.01);

				for (Entity entity : GeneralMethods.getEntitiesAroundPoint(projLoc, 1.2)) {
					if (entity instanceof LivingEntity && entity.getUniqueId() != player.getUniqueId()) {
						DamageHandler.damageEntity(entity, FlameSpins.this.projectileDamage, FlameSpins.this);
						entity.setFireTicks(FlameSpins.this.projectileFireTicks);
						cancel();
						return;
					}
				}
			}
		}.runTaskTimer(AmonPackPlugin.plugin, 0, 1);

		RPG.Levels.BendingTree.PlayerBendingBranch branch = AmonPackPlugin.levelsBending.GetBranchByPlayerName(player.getName());
		boolean hasFirefly = (branch != null && branch.hasUpgrade("Firefly"));
		int maxClicks = hasFirefly ? this.maxClicksFirefly : this.maxClicks;

		if (clicksUsed >= maxClicks) {
			bPlayer.addCooldown(this);
			remove();
		}
	}

	@Override
	public long getCooldown() {
		RPG.Levels.BendingTree.PlayerBendingBranch branch = AmonPackPlugin.levelsBending.GetBranchByPlayerName(player.getName());
		boolean hasFirefly = (branch != null && branch.hasUpgrade("Firefly"));
		if (hasFirefly) {
			return AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Fire.FlameSpins.CooldownFirefly", 3000L);
		} else {
			return AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Fire.FlameSpins.Cooldown", 6000L);
		}
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

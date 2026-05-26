package Abilities.PK_Abilities.Fire;

import Abilities.Util_Objects.AbilityProjectile;
import Abilities.Util_Objects.BetterParticles;
import Abilities.Util_Objects.SmokeSource;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.ParticleEffect;

import Plugin.AmonPackPlugin;
import Plugin.Methods;
import Abilities.Bending.SmokeAbility;

import java.util.ArrayList;
import java.util.List;

public class SmokeDaggers extends SmokeAbility implements AddonAbility {
	private static final java.util.HashMap<java.util.UUID, Integer> steadyHandClicks = new java.util.HashMap<>();
	private static final java.util.HashMap<java.util.UUID, Long> steadyHandLastTime = new java.util.HashMap<>();
	private boolean leaveSmoke = true;

	private int Cooldown = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Fire.Smoke.SmokeDaggers.Cooldown");
	private int dmg = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Fire.Smoke.SmokeDaggers.Dmg");
	private int range = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Fire.Smoke.SmokeDaggers.Range");
	private int slowpower = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Fire.Smoke.SmokeDaggers.SlowPower");
	private int slowdur = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Fire.Smoke.SmokeDaggers.SlowDuration");
	private int poisonpower = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Fire.Smoke.SmokeDaggers.PoisonPower");
	private int poisondur = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Fire.Smoke.SmokeDaggers.PoisonDuration");
	private int blinddur = AmonPackPlugin.plugin.getConfig()
			.getInt("AmonPack.Fire.Smoke.SmokeDaggers.BlindnessDuration");
	private int zonerange = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Fire.Smoke.SmokeDaggers.SmokeZoneRange");
	private int zonedur = AmonPackPlugin.plugin.getConfig()
			.getInt("AmonPack.Fire.Smoke.SmokeDaggers.SmokeZoneDuration");
	Location origin;
	Location location;
	Location location2;
	Location location3;
	Vector direction;
	Location loc1;
	private List<AbilityProjectile> Projectiles;
	private int interval;
	Location loc2;

	public SmokeDaggers(Player player) {
		super(player);
		
		RPG.Levels.BendingTree.PlayerBendingBranch branch = AmonPackPlugin.levelsBending.GetBranchByPlayerName(player.getName());
		boolean hasSteadyHand = (branch != null && branch.hasUpgrade("SteadyHand"));

		if (hasSteadyHand) {
			int clicks = steadyHandClicks.getOrDefault(player.getUniqueId(), 0);
			long lastClick = steadyHandLastTime.getOrDefault(player.getUniqueId(), 0L);
			
			// Reset clicks if they waited too long (more than 6 seconds) to prevent entering cooldown on next fresh sequence
			if (clicks > 0 && System.currentTimeMillis() - lastClick > 6000) {
				clicks = 0;
				steadyHandClicks.put(player.getUniqueId(), 0);
			}

			if (clicks == 0 && bPlayer.isOnCooldown(this)) {
				return;
			}
			
			if (clicks > 0 && System.currentTimeMillis() - lastClick < 1000) {
				return;
			}
			
			clicks++;
			steadyHandClicks.put(player.getUniqueId(), clicks);
			steadyHandLastTime.put(player.getUniqueId(), System.currentTimeMillis());
			
			if (clicks < 3) {
				this.leaveSmoke = false;
				bPlayer.removeCooldown(this);
			} else {
				this.leaveSmoke = true;
				bPlayer.addCooldown(this);
				steadyHandClicks.put(player.getUniqueId(), 0);
			}
		} else {
			if (bPlayer.isOnCooldown(this)) {
				return;
			}
			bPlayer.addCooldown(this);
		}

		if (!bPlayer.canBend(this)) {
			return;
		}
		origin = player.getLocation().clone().add(0, 1.3, 0);
		Projectiles = new ArrayList<>();
		interval = 0;
		List<BetterParticles> Particles = new ArrayList<>();
		Particles.add(new BetterParticles(8, ParticleEffect.SMOKE_NORMAL, 0.3, 0.01, 0.15));
		Location tloc1 = new Location(player.getWorld(), player.getLocation().getX(), player.getLocation().getY() + 1,
				player.getLocation().getZ(), (player.getLocation().getYaw() - 15), player.getLocation().getPitch());
		Vector Loc1Dir = tloc1.clone().getDirection();
		Location tloc2 = new Location(player.getWorld(), player.getLocation().getX(), player.getLocation().getY() + 1,
				player.getLocation().getZ(), (player.getLocation().getYaw() + 15), player.getLocation().getPitch());
		Vector Loc2Dir = tloc2.clone().getDirection();
		Projectiles.add(new AbilityProjectile(Loc1Dir, tloc1, origin, Particles, 1));
		Projectiles.add(new AbilityProjectile(Loc2Dir, tloc2, origin, Particles, 1));
		start();
	}

	@Override
	public void progress() {
		if (player.isDead() || !player.isOnline()) {
			remove();
			return;
		}
		for (AbilityProjectile Projectile : Projectiles) {
			Location location = Projectile.Advance().clone();
			for (Entity entity : GeneralMethods.getEntitiesAroundPoint(location, 1)) {
				if ((entity instanceof LivingEntity) && entity.getUniqueId() != player.getUniqueId()) {
					DamageHandler.damageEntity(entity, 1, this);
					if (leaveSmoke) {
						SmokeSource SourceEnd = new SmokeSource(location.clone().add(0, 1, 0), 120, 3, 1, player);
					}
					Projectiles.remove(Projectile);
					return;
				}
			}
			if (location.distance(origin) > 20 || !location.clone().add(0, 0.8, 0).getBlock().getType().isAir()
					|| location.clone().add(0, 0.8, 0).getBlock().getType().isSolid()) {
				if (leaveSmoke) {
					SmokeSource SourceEnd = new SmokeSource(location.clone().add(0, 1, 0), 120, 3, 1, player);
				}
				Projectiles.remove(Projectile);
				return;
			}
		}
		if (Projectiles.isEmpty()) {
			remove();
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
		return "SmokeDaggers";
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
		return "This ability creates smoke clouds! Forms and throws deadly daggers made of solid smoke that poison and blind your targets.";
	}

	@Override
	public String getInstructions() {
		return "Left-click to throw smoke daggers.";
	}

}
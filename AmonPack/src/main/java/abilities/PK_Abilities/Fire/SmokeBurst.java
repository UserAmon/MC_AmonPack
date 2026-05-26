package Abilities.PK_Abilities.Fire;

import Abilities.Util_Objects.BetterParticles;
import Abilities.Util_Objects.SmokeSource;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.util.ParticleEffect;
import Abilities.Bending.SmokeAbility;
import Plugin.AmonPackPlugin;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class SmokeBurst extends SmokeAbility implements AddonAbility {

	private long Cooldown = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Fire.Smoke.SmokeBurst.Cooldown", 3000);
	private SmokeSource Source;

	public SmokeBurst(Player player, boolean IsShift) {
		super(player);
		if (!this.bPlayer.isOnCooldown(getName()) && this.bPlayer.canBend(this)) {
			if (IsShift) {
				SmokeSource source = SmokeAbility.UseSmokeSource(player, 20);
				if (source != null) {
					this.Source = source;
					bPlayer.addCooldown(this);
					start();
				}
			}
		}
	}

	@Override
	public void progress() {
		if (player.isDead() || !player.isOnline()) {
			remove();
			return;
		}
		if (!player.isSneaking()) {
			if (Source != null) {
				explodeSmokeSource(player, Source.getLocation());
			}
			remove();
			return;
		}
		if (Source != null) {
			if (Source.IsNearPlayer(player.getLocation(), 1.0, player)) {
				explodeSmokeSource(player, Source.getLocation());
				remove();
			}
		} else {
			remove();
		}
	}

	public void onLeftClick() {
		if (Source != null) {
			explodeSmokeSource(player, Source.getLocation());
			remove();
		}
	}

	public static void explodeSmokeSource(Player player, Location loc) {
		if (loc == null || loc.getWorld() == null) return;
		loc.getWorld().playSound(loc, org.bukkit.Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.8f);
		loc.getWorld().playSound(loc, org.bukkit.Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 1.0f, 1.5f);
		
		new org.bukkit.scheduler.BukkitRunnable() {
			double currentRadius = 0.5;
			final double maxRadius = 6.0;
			final double speed = 0.5; // expansion per tick
			final java.util.Set<java.util.UUID> hitEntities = new java.util.HashSet<>();
			
			@Override
			public void run() {
				if (currentRadius >= maxRadius) {
					this.cancel();
					return;
				}
				
				int points = (int) (Math.PI * currentRadius * 1.5);
				points = Math.max(8, points);
				
				for (int i = 0; i < points; i++) {
					double angle = 2 * Math.PI * i / points;
					double x = currentRadius * Math.cos(angle);
					double z = currentRadius * Math.sin(angle);
					Location particleLoc = loc.clone().add(x, (Math.random() - 0.5) * 0.4, z);
					
					ParticleEffect.SMOKE_NORMAL.display(particleLoc, 1, 0.1, 0.1, 0.1, 0.02);
					if (Math.random() < 0.3) {
						ParticleEffect.CAMPFIRE_COSY_SMOKE.display(particleLoc, 1, 0.05, 0.05, 0.05, 0.01);
					}
					if (Math.random() < 0.1) {
						ParticleEffect.SMOKE_LARGE.display(particleLoc, 1, 0.05, 0.05, 0.05, 0.01);
					}
				}
				
				for (Entity entity : GeneralMethods.getEntitiesAroundPoint(loc, currentRadius)) {
					if (entity instanceof LivingEntity && entity.getUniqueId() != player.getUniqueId()) {
						if (!hitEntities.contains(entity.getUniqueId())) {
							hitEntities.add(entity.getUniqueId());
							LivingEntity le = (LivingEntity) entity;
							le.damage(4.0, player);
							le.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.BLINDNESS, 60, 0));
							le.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.POISON, 60, 0));
							
							Vector pushDir = le.getLocation().toVector().subtract(loc.toVector());
							if (pushDir.lengthSquared() > 0) {
								pushDir.normalize().multiply(0.8).setY(0.3);
								le.setVelocity(pushDir);
							}
						}
					}
				}
				
				currentRadius += speed;
			}
		}.runTaskTimer(AmonPackPlugin.plugin, 0, 1);
	}

	@Override
	public long getCooldown() {
		return Cooldown;
	}

	@Override
	public Location getLocation() {
		return Source != null ? Source.getLocation() : null;
	}

	@Override
	public String getName() {
		return "SmokeBurst";
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
		return "This ability pulls a smoke source towards you when shifting. If you left-click during flight or when it returns, it explodes in expanding smoke rings, damaging and blinding nearby enemies.";
	}

	@Override
	public String getInstructions() {
		return "Shift while looking at a smoke source to pull it. Left-click to detonate.";
	}
}

package Abilities.PK_Abilities.Fire;

import Abilities.Util_Objects.BetterParticles;
import Abilities.Util_Objects.SmokeSource;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.ParticleEffect;
import Abilities.Bending.SmokeAbility;
import Plugin.AmonPackPlugin;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.Particle;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class SmokeBurst extends SmokeAbility implements AddonAbility {

	private enum State {
		PULLING, CHARGING, FIRING
	}

	private State state;
	private long startTime;
	private long chargeTimePerLevel = 1000;
	private int maxChargeLevel = 3;
	private int lastReportedLevel = -1;
	private double baseRadius;
	private double radiusPerLevel;
	private double baseDamage;
	private double damagePerLevel;

	private long Cooldown = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Fire.Smoke.SmokeBurst.Cooldown", 3000);
	private SmokeSource Source;

	public SmokeBurst(Player player, boolean IsShift) {
		super(player);
		this.chargeTimePerLevel = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Fire.Smoke.SmokeBurst.ChargeTimePerLevel", 1000);
		this.maxChargeLevel = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Fire.Smoke.SmokeBurst.MaxChargeLevel", 3);
		this.baseRadius = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Fire.Smoke.SmokeBurst.BaseRadius", 4.0);
		this.radiusPerLevel = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Fire.Smoke.SmokeBurst.RadiusPerLevel", 2.5);
		this.baseDamage = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Fire.Smoke.SmokeBurst.BaseDamage", 1.0);
		this.damagePerLevel = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Fire.Smoke.SmokeBurst.DamagePerLevel", 1.0);

		if (!this.bPlayer.isOnCooldown(getName()) && this.bPlayer.canBend(this)) {
			if (IsShift) {
				SmokeSource source = SmokeAbility.UseSmokeSource(player, 20);
				if (source != null) {
					this.Source = source;
					this.state = State.PULLING;
					start();
				} else {
					remove();
				}
			} else {
				remove();
			}
		} else {
			remove();
		}
	}

	@Override
	public void progress() {
		if (player.isDead() || !player.isOnline()) {
			remove();
			return;
		}

		switch (state) {
			case PULLING:
				if (!player.isSneaking()) {
					remove();
					return;
				}
				if (Source == null) {
					remove();
					return;
				}
				if (Source.IsNearPlayer(player.getLocation(), 1.2, player)) {
					player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_BREATH, 1.0f, 0.8f);
					player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 0.5f, 0.8f);
					player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation().add(0, 1, 0), 15, 0.3, 0.3,
							0.3, 0.05);

					this.state = State.CHARGING;
					this.startTime = System.currentTimeMillis();
					this.Source = null;
				}
				break;

			case CHARGING:
				if (!player.isSneaking()) {
					fire();
					return;
				}

				int level = getChargeLevel();
				if (level != lastReportedLevel) {
					lastReportedLevel = level;
					if (level > 0) {
						float pitch = 0.7f + (level * 0.3f);
						player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, pitch);
						player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.4f, pitch);
					}
				}
				String bar;
				if (level == 0) {
					bar = "§7[ §f░░░ §7] §7§lABSORBING SMOKE...";
				} else if (level == 1) {
					bar = "§7[ §8█§7░░ §7] §8§lCHARGE LEVEL 1";
				} else if (level == 2) {
					bar = "§8[ §7██§8░ §8] §7§lCHARGE LEVEL 2";
				} else {
					bar = "§c§l[ §8███ §c§l] §e§lSMOKE POWER FULLY CHARGED!";
				}
				player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
						net.md_5.bungee.api.chat.TextComponent.fromLegacyText(bar));

				double radius = 0.8 + (level * 0.2);
				double angle = (System.currentTimeMillis() / 150.0) * (level + 1);
				double x = radius * Math.cos(angle);
				double z = radius * Math.sin(angle);
				Location pLoc = player.getLocation().clone().add(x, 0.2 + (level * 0.4), z);
				player.getWorld().spawnParticle(Particle.SMOKE, pLoc, 1, 0, 0, 0, 0);
				player.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, pLoc, 1, 0, 0, 0, 0);
				break;

			case FIRING:
				break;
		}
	}

	private int getChargeLevel() {
		long duration = System.currentTimeMillis() - startTime;
		int level = (int) (duration / chargeTimePerLevel);
		return Math.min(level, maxChargeLevel);
	}

	private void fire() {
		int level = getChargeLevel();
		if (level == 0) {
			remove();
			return;
		}
		bPlayer.addCooldown(this);
		this.state = State.FIRING;

		Location baseLoc = player.getLocation().clone();

		spawnSmokeBladeCircle(player, this, baseLoc.clone().add(0, 1.0, 0), level, 1);

		if (level >= 2) {
			new BukkitRunnable() {
				@Override
				public void run() {
					if (player.isOnline()) {
						spawnSmokeBladeCircle(player, SmokeBurst.this, baseLoc.clone().add(0, 0.5, 0), level, 2);
					}
				}
			}.runTaskLater(AmonPackPlugin.plugin, 10);
		}

		if (level >= 3) {
			new BukkitRunnable() {
				@Override
				public void run() {
					if (player.isOnline()) {
						spawnSmokeBladeCircle(player, SmokeBurst.this, baseLoc.clone().add(0, 1.5, 0), level, 3);
					}
				}
			}.runTaskLater(AmonPackPlugin.plugin, 15);
		}

		new BukkitRunnable() {
			@Override
			public void run() {
				remove();
			}
		}.runTaskLater(AmonPackPlugin.plugin, 25);
	}

	private static void spawnSmokeBladeCircle(Player player, SmokeBurst ability, Location loc, int level,
			int circleIndex) {
		double maxRadius = ability.baseRadius + level * ability.radiusPerLevel;
		double damage = ability.baseDamage + level * ability.damagePerLevel;

		loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.2f - (circleIndex * 0.15f));
		loc.getWorld().playSound(loc, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 0.6f, 1.6f - (circleIndex * 0.15f));

		new BukkitRunnable() {
			double currentRadius = 0.5;
			final double speed = 0.6; // expansion per tick
			final Set<UUID> hitEntities = new HashSet<>();

			@Override
			public void run() {
				if (currentRadius >= maxRadius) {
					this.cancel();
					return;
				}

				int points = (int) (Math.PI * currentRadius * 2.5);
				points = Math.max(8, points);

				for (int i = 0; i < points; i++) {
					double angle = 2 * Math.PI * i / points;
					double x = currentRadius * Math.cos(angle);
					double z = currentRadius * Math.sin(angle);
					Location particleLoc = loc.clone().add(x, (Math.random() - 0.5) * 0.1, z);

					ParticleEffect.SMOKE_NORMAL.display(particleLoc, 1, 0.05, 0.05, 0.05, 0.01);
					if (Math.random() < 0.08) {
						ParticleEffect.CAMPFIRE_COSY_SMOKE.display(particleLoc, 1, 0.05, 0.05, 0.05, 0.01);
					}
					if (level >= 3 && Math.random() < 0.08) {
						ParticleEffect.SWEEP_ATTACK.display(particleLoc, 1, 0, 0, 0, 0);
					}
				}

				for (Entity entity : GeneralMethods.getEntitiesAroundPoint(loc, currentRadius)) {
					if (entity instanceof LivingEntity && entity.getUniqueId() != player.getUniqueId()) {
						if (!hitEntities.contains(entity.getUniqueId())) {
							hitEntities.add(entity.getUniqueId());
							LivingEntity le = (LivingEntity) entity;
							DamageHandler.damageEntity(le, damage, ability);

							if (level >= 1) {
								le.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40 + level * 20, 0));
							}
							if (level >= 2) {
								le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, level - 1));
							}
							if (level >= 3) {
								le.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 60, 0));
							}

							Vector pushDir = le.getLocation().toVector().subtract(loc.toVector());
							if (pushDir.lengthSquared() > 0) {
								pushDir.normalize().multiply(0.4 + level * 0.25).setY(0.25);
								le.setVelocity(pushDir);
							}
						}
					}
				}

				currentRadius += speed;
			}
		}.runTaskTimer(AmonPackPlugin.plugin, 0, 1);
	}

	public void onLeftClick() {
	}

	@Override
	public long getCooldown() {
		return Cooldown;
	}

	@Override
	public Location getLocation() {
		return Source != null ? Source.getLocation() : player.getLocation();
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
		return "Hold shift while looking at a smoke source to draw and absorb it. Continue holding shift to charge up to 3 levels. Release shift to fire expanding rings of smoke blades at offset heights, slicing and debuffing enemies.";
	}

	@Override
	public String getInstructions() {
		return "Shift while looking at a smoke source to absorb. Hold shift to charge, release to burst.";
	}

	public static void explodeSmokeSource(Player player, Location loc) {
		if (loc == null || loc.getWorld() == null)
			return;
		loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.8f);
		loc.getWorld().playSound(loc, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 1.0f, 1.5f);

		new BukkitRunnable() {
			double currentRadius = 0.5;
			final double maxRadius = 6.0;
			final double speed = 0.5;

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
				}

				for (Entity entity : GeneralMethods.getEntitiesAroundPoint(loc, currentRadius)) {
					if (entity instanceof LivingEntity && entity.getUniqueId() != player.getUniqueId()) {
						LivingEntity le = (LivingEntity) entity;
						le.damage(4.0, player);
						le.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0));
						le.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 60, 0));

						Vector pushDir = le.getLocation().toVector().subtract(loc.toVector());
						if (pushDir.lengthSquared() > 0) {
							pushDir.normalize().multiply(0.8).setY(0.3);
							le.setVelocity(pushDir);
						}
					}
				}

				currentRadius += speed;
			}
		}.runTaskTimer(AmonPackPlugin.plugin, 0, 1);
	}
}

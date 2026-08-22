package Abilities.PK_Abilities.Fire;

import Abilities.Bending.SmokeAbility;
import Abilities.Util_Objects.SmokeSource;
import Plugin.AmonPackPlugin;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.util.ParticleEffect;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class SmokeCamouflage extends SmokeAbility implements AddonAbility {

	private long cooldown;
	private double force;
	private double forceY;
	private long durationTicks;
	private int effectsPower;
	private long minChargeTimeMs = 300L;

	private enum State {
		CHARGING,
		ACTIVE
	}

	private State state;
	private long chargeStartTime;
	private int activeTicks;
	private SmokeSource source;

	public SmokeCamouflage(Player player) {
		super(player);
		if (hasAbility(player, SmokeCamouflage.class)) {
			return;
		}
		if (bPlayer.isOnCooldown(getName()) || !bPlayer.canBend(this)) {
			return;
		}

		loadConfig();

		this.source = SmokeAbility.UseSmokeSource(player, 20);
		this.state = State.CHARGING;
		this.chargeStartTime = System.currentTimeMillis();
		this.activeTicks = 0;

		player.getWorld().playSound(player.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 0.6f, 1.4f);
		start();
	}

	private void loadConfig() {
		this.cooldown = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Fire.Smoke.SmokeCamouflage.Cooldown", 6000L);
		this.durationTicks = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Fire.Smoke.SmokeCamouflage.Duration", 60L);
		this.force = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Fire.Smoke.SmokeCamouflage.DashForce", 1.8);
		this.forceY = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Fire.Smoke.SmokeCamouflage.DashForce-Y", 0.35);
		this.effectsPower = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Fire.Smoke.SmokeCamouflage.Effects-Power", 1);
	}

	@Override
	public void progress() {
		if (player == null || !player.isOnline() || player.isDead()) {
			remove();
			return;
		}

		if (state == State.CHARGING) {
			if (bPlayer.getBoundAbilityName() == null
					|| !bPlayer.getBoundAbilityName().equalsIgnoreCase(getName())) {
				remove();
				return;
			}

			if (player.isSneaking()) {
				// Visual smoke charging around player
				ParticleEffect.SMOKE_NORMAL.display(player.getLocation().add(0, 0.5, 0), 4, 0.25, 0.25, 0.25, 0.02);
				if (source != null) {
					source.IsNearPlayer(player.getLocation(), 1.5, player);
				}
			} else {
				long chargedMs = System.currentTimeMillis() - chargeStartTime;
				if (chargedMs >= minChargeTimeMs) {
					launchCamouflage();
				} else {
					remove();
				}
			}
		} else if (state == State.ACTIVE) {
			activeTicks++;
			if (activeTicks >= durationTicks) {
				remove();
				return;
			}

			player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 10, effectsPower, false, false, false));
			player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 10, effectsPower, false, false, false));

			if (activeTicks % 2 == 0) {
				player.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, player.getLocation().add(0, 0.3, 0), 2, 0.2, 0.1, 0.2, 0.01);
				player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation().add(0, 0.3, 0), 2, 0.2, 0.1, 0.2, 0.02);
			}
		}
	}

	private void launchCamouflage() {
		this.state = State.ACTIVE;
		this.activeTicks = 0;

		// Launch dash velocity
		Vector dir = player.getLocation().getDirection().clone();
		dir.setY(Math.max(0.2, dir.getY() + forceY));
		player.setVelocity(dir.multiply(force));

		// Immediate potion effects for full duration
		player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, (int) durationTicks, effectsPower, false, false, false));
		player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, (int) durationTicks, effectsPower, false, false, false));

		// Launch burst visuals & sounds
		Location loc = player.getLocation().add(0, 0.5, 0);
		loc.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, loc, 20, 0.5, 0.5, 0.5, 0.05);
		loc.getWorld().spawnParticle(Particle.SMOKE, loc, 30, 0.5, 0.5, 0.5, 0.08);
		loc.getWorld().playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0f, 0.8f);
		loc.getWorld().playSound(loc, Sound.BLOCK_FIRE_EXTINGUISH, 1.0f, 0.7f);

		bPlayer.addCooldown(this, cooldown);
	}

	@Override
	public long getCooldown() {
		return cooldown;
	}

	@Override
	public Location getLocation() {
		return player != null ? player.getLocation() : null;
	}

	@Override
	public String getName() {
		return "SmokeCamouflage";
	}

	@Override
	public String getAuthor() {
		return "AmonPack";
	}

	@Override
	public String getVersion() {
		return "1.1";
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
		return "Ładuj kucaniem (SHIFT), a po puszczeniu wystrzel się do przodu w kłębie dymu, zyskując niewidzialność i prędkość.";
	}

	@Override
	public String getInstructions() {
		return "Przytrzymaj SHIFT aby naładować dym, a następnie puść SHIFT aby wystrzelić.";
	}
}
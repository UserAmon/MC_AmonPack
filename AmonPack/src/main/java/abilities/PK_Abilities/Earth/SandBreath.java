package Abilities.PK_Abilities.Earth;

import java.util.HashMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ProjectKorra;
import com.projectkorra.projectkorra.ability.Ability;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.EarthAbility;
import com.projectkorra.projectkorra.ability.SandAbility;
import com.projectkorra.projectkorra.util.ParticleEffect;

import Plugin.AmonPackPlugin;
import Plugin.Methods;

public class SandBreath extends SandAbility implements AddonAbility {

	private int cooldown;
	private int Range;
	private int time;
	private int ChargeTime;
	private int speedsand;
	private int speedearth;
	private int Dmg;
	private int durationtuse;

	public static Boolean buffs;
	public static int DeBuffsPower;
	public static int DeBuffsDuration;
	public static Boolean push;

	public static void loadConfig() {
		buffs = AmonPackPlugin.getAbilitiesConfig().getBoolean("AmonPack.Earth.Sand.SandBreath.ChargedBreathBuff", true);
		DeBuffsPower = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Earth.Sand.SandBreath.DeBuffPower", 2);
		DeBuffsDuration = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Earth.Sand.SandBreath.DebuffDuration", 60);
		push = AmonPackPlugin.getAbilitiesConfig().getBoolean("AmonPack.Earth.Sand.SandBreath.CanDebuffEnemy", true);
	}

	private int abilityState;
	private Ability abi = this;
	private int usage;
	private int usagev2;
	private HashMap<String, Integer> taskID = new HashMap<String, Integer>();
	private HashMap<String, BukkitTask> deltask = new HashMap<String, BukkitTask>();

	public SandBreath(Player player) {
		super(player);
		this.cooldown = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Earth.Sand.SandBreath.Cooldown", 6000);
		this.Range = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Earth.Sand.SandBreath.Range", 12);
		this.time = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Earth.Sand.SandBreath.Duration", 4000);
		this.ChargeTime = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Earth.Sand.SandBreath.ChargeTime", 2000);
		this.speedsand = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Earth.Sand.SandBreath.SpeedOnSand", 6);
		this.speedearth = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Earth.Sand.SandBreath.SpeedOnEarth", 2);
		this.Dmg = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Earth.Sand.SandBreath.Damage", 1);
		this.durationtuse = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Earth.Sand.SandBreath.DurationToUseBreath", 120);

		if (bPlayer.isOnCooldown(this)) {
			return;
		}
		if (!bPlayer.canBend(this)) {
			return;
		}
		if (EarthAbility.isSandbendable(player,
				Methods.getTargetLocation(player, 15).getBlock().getBlockData().getMaterial())) {
			usage = 0;
			usagev2 = 0;
			if (!deltask.isEmpty()) {
				deltask.clear();
			}
			abilityState = 0;
			this.time = 0;
			start();
		}
	}

	@Override
	public void progress() {
		if (!bPlayer.isOnCooldown(this)) {
			if (player.isDead() || !player.isOnline()) {
				remove();
				return;
			}

			if (abilityState == 0) {
				if (player.isSneaking()) {
					if (System.currentTimeMillis() > getStartTime() + ChargeTime) {
						time = 0;
						abilityState = 1;
						if (deltask.isEmpty()) {

							deltask.put("delayedtask", new BukkitRunnable() {
								@Override
								public void run() {
									if (abilityState == 1) {
										abilityState = 0;
										if (!deltask.isEmpty()) {
											deltask.clear();
										}
										bPlayer.addCooldown(abi);
										remove();
										return;
									}
								}
							}.runTaskLater(ProjectKorra.plugin, durationtuse * 20));

						} else if (!deltask.isEmpty()) {
							deltask.clear();
							deltask.put("delayedtask", new BukkitRunnable() {
								@Override
								public void run() {
									if (abilityState == 1) {
										abilityState = 0;
										if (!deltask.isEmpty()) {
											deltask.clear();
										}
										bPlayer.addCooldown(abi);
										remove();
										return;
									}
								}
							}.runTaskLater(ProjectKorra.plugin, durationtuse * 20));

						}
					} else if (System.currentTimeMillis() < getStartTime() + 3000) {
						if (!EarthAbility.isSandbendable(player,
								Methods.getTargetLocation(player, 15).getBlock().getBlockData().getMaterial())) {
							remove();
							return;
						} else {
							Location pStart = player.getLocation().clone().add(
									(Math.random() - 0.5),
									0.35 + (Math.random() - 0.5),
									(Math.random() - 0.5));
							Location targetLoc = Methods.getTargetLocation(player, 10).getBlock().getLocation().clone().add(
									(Math.random() - 0.5),
									(Math.random() - 0.5),
									(Math.random() - 0.5));
							Methods.displayLineBetweenPoints(pStart, targetLoc, 10, Material.SAND, 1);

						}
					} else if (!player.isSneaking()) {
						remove();
						return;

					}
				} else if (!player.isSneaking()) {
					remove();
					abilityState = 0;
					return;
				}
			}

			if (abilityState == 1) {
				ParticleEffect.BLOCK_CRACK.display(player.getLocation(), 1, 1, 1, 1, 0.1,
						Material.SAND.createBlockData());
				if (buffs == true) {

					for (Block blocks : GeneralMethods.getBlocksAroundPoint(player.getLocation(), 2)) {
						if (EarthAbility.isSandbendable(player, blocks.getType())) {
							player.addPotionEffect(
									new PotionEffect(PotionEffectType.SPEED, 10, speedsand, false, false));
						} else if (EarthAbility.isEarthbendable(player, blocks)) {
							player.addPotionEffect(
									new PotionEffect(PotionEffectType.SPEED, 10, speedearth, false, false));
						}
					}

				}
				if (usage == 0 && !player.isSneaking()) {
					usage = 1;
				}
			}
			if (abilityState == 1 && usage == 1 && player.isSneaking()
					&& bPlayer.getBoundAbilityName().equalsIgnoreCase("SandBreath")) {
				usagev2 = 1;
				if (!bPlayer.isOnCooldown(this)) {
					if (time >= 60) {
						bPlayer.addCooldown(this);
						abilityState = 0;
						time = 0;
						remove();
						return;
					}
					if (bPlayer.getBoundAbilityName().equalsIgnoreCase("SandBreath")) {
						Location location = player.getLocation().clone().add(0, 1, 0);
						Vector dir = player.getLocation().getDirection();
						Methods.stream(location, dir, player, abi, Material.SAND, Range, Dmg);
						time = time + 1;
					}
				}

			}
			if (usagev2 == 1 && !player.isSneaking()) {
				if (!deltask.isEmpty()) {
					deltask.clear();
				}
				usagev2 = 0;
				bPlayer.addCooldown(this);
				abilityState = 0;
				remove();
				return;

			}

		} else {
			remove();
			abilityState = 0;
			return;
		}

	}

	public static void usageforcertainp() {
	}

	@Override
	public long getCooldown() {
		return cooldown;
	}

	@Override
	public Location getLocation() {
		return null;
	}

	@Override
	public String getName() {
		return "SandBreath";
	}

	@Override
	public String getAuthor() {
		return "AmonPack";
	}

	@Override
	public String getVersion() {
		return "AmonPack";
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
		return "Exhales a continuous stream of blinding, choking sand that damages and slows targets. While charged with sand, gain movementspeed on sand and extra on earth.";
	}

	@Override
	public String getInstructions() {
		return "Charge sand toward yourself by shifting while looking at it. Hold sneak to exhale a sand breath.";
	}

}
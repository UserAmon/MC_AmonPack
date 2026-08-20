package Abilities.Bending;

import Abilities.PK_Abilities.Air.*;
import Abilities.PK_Abilities.Chi.*;
import Abilities.PK_Abilities.Earth.*;
import Abilities.PK_Abilities.Fire.*;
import Abilities.PK_Abilities.Water.*;
import Abilities.Util_Objects.EarthDisc;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;

import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.Element;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import RPG.Levels.BendingTree.PlayerBendingBranch;
import Plugin.AmonPackPlugin;
import RPG.Crafting.CraftingMenager;
import org.bukkit.entity.Arrow;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.Sound;
import org.bukkit.Particle;
import org.bukkit.util.Vector;
import com.projectkorra.projectkorra.ability.ChiAbility;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.event.AbilityStartEvent;
import com.projectkorra.projectkorra.util.DamageHandler;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class AbilitiesListener implements Listener {

	private static final java.util.Set<java.util.UUID> activeAttackProcessors = java.util.concurrent.ConcurrentHashMap.newKeySet();

	private void CheckEarthHealthBoost(Player player, CoreAbility ability) {
		if (ability.getElement() == Element.EARTH) {
			boolean hasEffect = false;
			for (ItemStack item : player.getInventory().getArmorContents()) {
				if (item != null && CraftingMenager.HaveEffect(item, "Earth_Health_Boost_On_Abilities")) {
					hasEffect = true;
					break;
				}
			}
			if (hasEffect) {
				BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);
				if (!bPlayer.isOnCooldown("Earth_Health_Boost_On_Abilities")) {
					player.addPotionEffect(
							new org.bukkit.potion.PotionEffect(PotionEffectType.HEALTH_BOOST, 120, 1, false, false));
					bPlayer.addCooldown("Earth_Health_Boost_On_Abilities", 10000);
				}
			}
		}
	}

	@EventHandler
	public void onShift(PlayerToggleSneakEvent event) {
		Player player = event.getPlayer();
		if (!event.isSneaking()) {
			if (CoreAbility.hasAbility(player, EarthStrike.class)) {
				CoreAbility.getAbility(player, EarthStrike.class).onShiftRelease();
			}
			if (CoreAbility.hasAbility(player, WaterStrike.class)) {
				CoreAbility.getAbility(player, WaterStrike.class).onShiftRelease();
			}
			if (CoreAbility.hasAbility(player, EarthBarricade.class)) {
				CoreAbility.getAbility(player, EarthBarricade.class).onShiftRelease();
			}
			if (CoreAbility.hasAbility(player, IceBarricade.class)) {
				CoreAbility.getAbility(player, IceBarricade.class).onShiftRelease();
			}
			return;
		}
		if (SpecialTriggerManager.isSpecialActive(player)) {
			return;
		}
		BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);
		if (bPlayer != null && bPlayer.getBoundAbility() != null) {
			if (!bPlayer.isOnCooldown(bPlayer.getBoundAbility())) {
				if (!event.isCancelled()) {
					CheckEarthHealthBoost(player, bPlayer.getBoundAbility());
					String boundAbility = bPlayer.getBoundAbilityName();
					if (boundAbility.equalsIgnoreCase("EarthStrike")) {
						if (!CoreAbility.hasAbility(player, EarthStrike.class)) {
							new EarthStrike(player);
						}
					} else if (boundAbility.equalsIgnoreCase("WaterStrike")) {
						if (!CoreAbility.hasAbility(player, WaterStrike.class)) {
							new WaterStrike(player);
						}
					} else if (boundAbility.equalsIgnoreCase("EarthBarricade") || boundAbility.equalsIgnoreCase("Baricade")) {
						if (!CoreAbility.hasAbility(player, EarthBarricade.class)) {
							new EarthBarricade(player);
						}
					} else if (boundAbility.equalsIgnoreCase("IceBarricade")) {
						if (!CoreAbility.hasAbility(player, IceBarricade.class)) {
							new IceBarricade(player);
						}
					} else if (boundAbility.equalsIgnoreCase("AirVolley")) {
						if (!CoreAbility.hasAbility(player, AirVolley.class)) {
							new AirVolley(player);
						}
					} else if (boundAbility.equalsIgnoreCase("SandBreath")) {
						new SandBreath(player);
					} else if (boundAbility.equalsIgnoreCase("Acoustics")) {
						new Acoustics(player);
					} else if (boundAbility.equalsIgnoreCase("AirScythe")) {
						if (com.projectkorra.projectkorra.ability.CoreAbility.hasAbility(player,
								Abilities.PK_Abilities.Air.AirScythe.class)) {
							Abilities.PK_Abilities.Air.AirScythe scythe = com.projectkorra.projectkorra.ability.CoreAbility
									.getAbility(player, Abilities.PK_Abilities.Air.AirScythe.class);
							scythe.onShift();
						}
					} else if (boundAbility.equalsIgnoreCase("TideLock")) {
						new TideLock(player);
					} else if (boundAbility.equalsIgnoreCase("FlameSpins")) {
						new FlameSpins(player);
					} else if (boundAbility.equalsIgnoreCase("FlameWeave")) {
						new FlameWeave(player);
					} else if (boundAbility.equalsIgnoreCase("EarthShift")) {
						new EarthShift(player);
					} else if (boundAbility.equalsIgnoreCase("IceThorn")) {
						new IceThorn(player);
					} else if (boundAbility.equalsIgnoreCase("CalmTide")) {
						new CalmTide(player);
					} else if (boundAbility.equalsIgnoreCase("EarthDiscs")) {
						new EarthDiscs(player);
					} else if (boundAbility.equalsIgnoreCase("DiscHurl")) {
						new DiscHurl(player);
					} else if (boundAbility.equalsIgnoreCase("WaterFist")) {
						if (!CoreAbility.hasAbility(player, WaterFist.class)) {
							new WaterFist(player);
						}
					} else if (boundAbility.equalsIgnoreCase("SandRupture")) {
						new SandRupture(player);
					} else if (boundAbility.equalsIgnoreCase("AirPressure")) {
						new AirPressure(player);
					} else if (boundAbility.equalsIgnoreCase("EarthHammer")) {
						new EarthHammer(player);
					} else if (boundAbility.equalsIgnoreCase("FlameSplit")) {
						if (!com.projectkorra.projectkorra.ability.CoreAbility.hasAbility(player, FlameSplit.class)) {
							new FlameSplit(player);
						}
					} else if (boundAbility.equalsIgnoreCase("BloodArrow")) {
						new BloodArrow(player);
					} else if (boundAbility.equalsIgnoreCase("BloodCall")) {
						new BloodCall(player);
					} else if (boundAbility.equalsIgnoreCase("SmokeBurst")) {
						new SmokeBurst(player, true);
					} else if (boundAbility.equalsIgnoreCase("SmokeBarrage")) {
						new SmokeBarrage(player);
					} else if (boundAbility.equalsIgnoreCase("SmokeCamouflage")) {
						new SmokeCamouflage(player);
					} else if (boundAbility.equalsIgnoreCase("AirSteps")) {
						new AirSteps(player);
					} else if (boundAbility.equalsIgnoreCase("MoltenBlast")) {
						if (!CoreAbility.hasAbility(player, Abilities.PK_Abilities.Earth.MoltenBlast.class)) {
							new Abilities.PK_Abilities.Earth.MoltenBlast(player);
						}
					} else if (boundAbility.equalsIgnoreCase("LavaTangles")) {
						if (!CoreAbility.hasAbility(player, Abilities.PK_Abilities.Earth.LavaTangles.class)) {
							new Abilities.PK_Abilities.Earth.LavaTangles(player);
						}
					} else if (boundAbility.equalsIgnoreCase("WaterTentacle")) {
						if (!CoreAbility.hasAbility(player, Abilities.PK_Abilities.Water.WaterTentacle.class)) {
							new Abilities.PK_Abilities.Water.WaterTentacle(player);
						}
					} else if (boundAbility.equalsIgnoreCase("EarthSpear")) {
						if (!CoreAbility.hasAbility(player, Abilities.PK_Abilities.Earth.EarthSpear.class)) {
							new Abilities.PK_Abilities.Earth.EarthSpear(player);
						}
					} else if (boundAbility.equalsIgnoreCase("Coil")) {
						if (!CoreAbility.hasAbility(player, Abilities.PK_Abilities.Fire.Coil.class)) {
							new Abilities.PK_Abilities.Fire.Coil(player);
						}
					} else if (boundAbility.equalsIgnoreCase("FirelordStance")) {
						new FirelordStance(player);
					} else if (boundAbility.equalsIgnoreCase("DaggerTrick")) {
						if (!CoreAbility.hasAbility(player, DaggerTrick.class)) {
							new DaggerTrick(player);
						}
					} else if (boundAbility.equalsIgnoreCase("Feintstep")) {
						new Feintstep(player);
					} else if (boundAbility.equalsIgnoreCase("ArcBlast")) {
						new ArcBlast(player);
					} else if (boundAbility.equalsIgnoreCase("FrostGrip")) {
						new FrostGrip(player);
					} else if (boundAbility.equalsIgnoreCase("VeinFlow")) {
						new VeinFlow(player);
					} else if (boundAbility.equalsIgnoreCase("QuickPalms")) {
						new QuickPalms(player, true);
					} else if (boundAbility.equalsIgnoreCase("AirBlast")) {
						CustomAirBlast.setOrigin(player);
					} else if (boundAbility.equalsIgnoreCase("Circulation")) {
						new Circulation(player);
					} else if (boundAbility.equalsIgnoreCase("HeartReading")) {
						if (CoreAbility.hasAbility(player, HeartReading.class)) {
							HeartReading hr = CoreAbility.getAbility(player, HeartReading.class);
							hr.onUseSlot();
						}
					} else if (boundAbility.equalsIgnoreCase("Burrow")) {
						if (!CoreAbility.hasAbility(player, Abilities.PK_Abilities.Earth.Burrow.class)) {
							new Abilities.PK_Abilities.Earth.Burrow(player);
						}
					} else if (boundAbility.equalsIgnoreCase("FlameWhip")) {
						if (!CoreAbility.hasAbility(player, FlameWhip.class)) {
							new FlameWhip(player);
						}
					} else if (!AmonPackPlugin.ENABLE_SKILL_TREE && boundAbility.equalsIgnoreCase("BoulderRoll")) {
						if (!CoreAbility.hasAbility(player, BoulderRoll.class)) {
							new BoulderRoll(player);
						}
					} else if (boundAbility.equalsIgnoreCase("Lasso")) {
						new Lasso(player);
					}
				}
			} else
				return;
		} else {
			if (CoreAbility.hasAbility(player, Lasso.class)) {
				Lasso lasso = CoreAbility.getAbility(player, Lasso.class);
				if (lasso != null) {
					lasso.onSneakRelease();
				}
			}
		}
	}

	@EventHandler
	public void OnSwing(PlayerAnimationEvent event) {
		Player player = event.getPlayer();
		if (SpecialTriggerManager.isSpecialActive(player)) {
			return;
		}
		BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);
		if (bPlayer.getBoundAbility() != null) {
			if (bPlayer.getBoundAbilityName().equalsIgnoreCase("Cyclone")) {
				new Cyclone(player);
			} else if (bPlayer.getBoundAbilityName().equalsIgnoreCase("Blossom")) {
				new Blossom(player);
			} else if (bPlayer.getBoundAbilityName().equalsIgnoreCase("DiscHurl")
					|| bPlayer.getBoundAbilityName().equalsIgnoreCase("EarthDiscs")
					|| bPlayer.getBoundAbilityName().equalsIgnoreCase("SandDisc")) {
				EarthDisc.redirectNearby(player, 4.0);
			} else if (bPlayer.getBoundAbilityName().equalsIgnoreCase("ArcBlast")) {
				if (CoreAbility.hasAbility(player, ArcBlast.class)) {
					ArcBlast ab = CoreAbility.getAbility(player, ArcBlast.class);
					if (ab.isFullyCharged()) {
						ab.onClick();
					}
				}
			}
			if (!bPlayer.isOnCooldown(bPlayer.getBoundAbility())) {
				CheckEarthHealthBoost(player, bPlayer.getBoundAbility());

				// if (bPlayer.getBoundAbilityName().equalsIgnoreCase("MetalFlex")) {
				// if (player.getInventory().getChestplate().getType() !=
				// Material.IRON_CHESTPLATE) {
				// return;}
				// else if (player.getInventory().getChestplate().getType() == null) {
				// return;}
				// new MetalFlex(player);
				// } else if (bPlayer.getBoundAbilityName().equalsIgnoreCase("SteelShackles")) {
				// if (player.getInventory().getChestplate().getType() !=
				// Material.IRON_CHESTPLATE) {
				// return;}
				// else if (player.getInventory().getChestplate().getType() == null) {
				// return;}
				// new SteelShackles(player);
				// }else if (bPlayer.getBoundAbilityName().equalsIgnoreCase("Slash")) {
				// if (player.getInventory().getItemInHand().getType() == Material.AIR) {
				// new Slash(player);
				// }}else if (bPlayer.getBoundAbilityName().equalsIgnoreCase("Pierce")) {
				// if (player.getInventory().getItemInHand().getType() == Material.AIR) {
				// new Pierce(player);
				// }}else if (bPlayer.getBoundAbilityName().equalsIgnoreCase("Stab")) {
				// if (player.getInventory().getItemInHand().getType() == Material.AIR) {
				// new Stab(player);
				// }else
				// if
				// (player.getInventory().getItemInMainHand().isSimilar(BladesAbility.Sword1)) {
				// new Stab(player);
				// Stab.LpmSkill(player);
				// player.getInventory().remove(BladesAbility.Sword1);
				// player.getInventory().setItemInHand(BladesAbility.Sword1);
				// }
				// }else
				if (bPlayer.getBoundAbilityName().equalsIgnoreCase("SmokeSurge")) {
					new SmokeSurge(player);
				} else if (bPlayer.getBoundAbilityName().equalsIgnoreCase("SmokeDaggers")) {
					new SmokeDaggers(player);
				} else if (bPlayer.getBoundAbilityName().equalsIgnoreCase("SmokePath")) {
					new SmokePath(player);
				} else if (bPlayer.getBoundAbilityName().equalsIgnoreCase("SoundCrash")) {
					new SoundCrash(player);
				} else if (bPlayer.getBoundAbilityName().equalsIgnoreCase("SmokeSlash")) {
					new SmokeSlash(player);
				} else if (bPlayer.getBoundAbilityName().equalsIgnoreCase("SmokeBurst")) {
					if (com.projectkorra.projectkorra.ability.CoreAbility.hasAbility(player, SmokeBurst.class)) {
						SmokeBurst sb = com.projectkorra.projectkorra.ability.CoreAbility.getAbility(player,
								SmokeBurst.class);
						sb.onLeftClick();
					} else {
						new SmokeBurst(player, false);
					}
				} else if (bPlayer.getBoundAbilityName().equalsIgnoreCase("AirScythe")) {
					new AirScythe(player);
				} else if (bPlayer.getBoundAbilityName().equalsIgnoreCase("Resonance")) {
					new Resonance(player);
				} else if (bPlayer.getBoundAbilityName().equalsIgnoreCase("Echo")) {
					new Echo(player);
				} else if (bPlayer.getBoundAbilityName().equalsIgnoreCase("FlameSpins")) {
					if (com.projectkorra.projectkorra.ability.CoreAbility.hasAbility(player, FlameSpins.class)) {
						FlameSpins fs = com.projectkorra.projectkorra.ability.CoreAbility.getAbility(player,
								FlameSpins.class);
						fs.onLeftClick();
					}
				} else if (bPlayer.getBoundAbilityName().equalsIgnoreCase("DaggerTrick")) {
					if (com.projectkorra.projectkorra.ability.CoreAbility.hasAbility(player, DaggerTrick.class)) {
						DaggerTrick dt = com.projectkorra.projectkorra.ability.CoreAbility.getAbility(player,
								DaggerTrick.class);
						dt.onLeftClick();
					}
				} else if (bPlayer.getBoundAbilityName().equalsIgnoreCase("FireSwirl")) {
					if (CoreAbility.hasAbility(player, FireSwirl.class)) {
						CoreAbility.getAbility(player, FireSwirl.class).onClick();
					} else {
						new FireSwirl(player);
					}
				} else if (bPlayer.getBoundAbilityName().equalsIgnoreCase("AirSwirl")) {
					if (CoreAbility.hasAbility(player, AirSwirl.class)) {
						CoreAbility.getAbility(player, AirSwirl.class).onClick();
					} else {
						new AirSwirl(player);
					}
				} else if (bPlayer.getBoundAbilityName().equalsIgnoreCase("WaterWhip")) {
					if (CoreAbility.hasAbility(player, WaterWhip.class)) {
						CoreAbility.getAbility(player, WaterWhip.class).onClick();
					} else {
						new WaterWhip(player);
					}
				} else if (bPlayer.getBoundAbilityName().equalsIgnoreCase("FireRain")) {
					if (CoreAbility.hasAbility(player, FireRain.class)) {
						CoreAbility.getAbility(player, FireRain.class).onClick();
					} else {
						new FireRain(player);
					}
				} else if (bPlayer.getBoundAbilityName().equalsIgnoreCase("FirelordStance")) {
					new FirelordStance(player);
				} else if (bPlayer.getBoundAbilityName().equalsIgnoreCase("PoisonDagger")
						|| bPlayer.getBoundAbilityName().equalsIgnoreCase("PoisonKnife")) {
					new PoisonDagger(player);
				} else if (bPlayer.getBoundAbilityName().equalsIgnoreCase("CrudeBomb")) {
					new CrudeBomb(player);
				} else if (bPlayer.getBoundAbilityName().equalsIgnoreCase("PointBlank")) {
					if (!CoreAbility.hasAbility(player, PointBlank.class)) {
						new PointBlank(player);
					}
				} else if (bPlayer.getBoundAbilityName().equalsIgnoreCase("VeinFlow")) {
					new VeinFlow(player);
				} else if (bPlayer.getBoundAbilityName().equalsIgnoreCase("HeartReading")) {
					if (CoreAbility.hasAbility(player, HeartReading.class)) {
						HeartReading hr = CoreAbility.getAbility(player, HeartReading.class);
						hr.onUseSlot();
					}
				} else if (bPlayer.getBoundAbilityName().equalsIgnoreCase("AerialPush")) {
					new AerialPush(player);
				} else if (bPlayer.getBoundAbilityName().equalsIgnoreCase("AirBlast")) {
					new CustomAirBlast(player);
				} else if (bPlayer.getBoundAbilityName().equalsIgnoreCase("GustShield")) {
					new GustShield(player);
				} else if (bPlayer.getBoundAbilityName().equalsIgnoreCase("Ionization")) {
					if (com.projectkorra.projectkorra.ability.CoreAbility.hasAbility(player, Ionization.class)) {
						Ionization ionization = com.projectkorra.projectkorra.ability.CoreAbility.getAbility(player,
								Ionization.class);
						ionization.onClick();
					} else {
						new Ionization(player);
					}
				} else if (bPlayer.getBoundAbilityName().equalsIgnoreCase("EarthDiscs")) {
					if (com.projectkorra.projectkorra.ability.CoreAbility.hasAbility(player,
							Abilities.PK_Abilities.Earth.EarthDiscs.class)) {
						Abilities.PK_Abilities.Earth.EarthDiscs discs = com.projectkorra.projectkorra.ability.CoreAbility
								.getAbility(player, Abilities.PK_Abilities.Earth.EarthDiscs.class);
						discs.onClick();
					}
				} else if (bPlayer.getBoundAbilityName().equalsIgnoreCase("SandDisc")) {
					if (com.projectkorra.projectkorra.ability.CoreAbility.hasAbility(player, SandDisc.class)) {
						SandDisc sd = com.projectkorra.projectkorra.ability.CoreAbility.getAbility(player,
								SandDisc.class);
						sd.onLeftClick();
					} else {
						new SandDisc(player);
					}
				} else if (bPlayer.getBoundAbilityName().equalsIgnoreCase("SandRupture")) {
					if (com.projectkorra.projectkorra.ability.CoreAbility.hasAbility(player, SandRupture.class)) {
						SandRupture sb = com.projectkorra.projectkorra.ability.CoreAbility.getAbility(player,
								SandRupture.class);
						sb.onLeftClick();
					} else {
						new SandRupture(player).onLeftClick();
					}
				} else if (bPlayer.getBoundAbilityName().equalsIgnoreCase("WaterFist")) {
					if (com.projectkorra.projectkorra.ability.CoreAbility.hasAbility(player, WaterFist.class)) {
						WaterFist wf = com.projectkorra.projectkorra.ability.CoreAbility.getAbility(player,
								WaterFist.class);
						wf.onLeftClick();
					}
				} else if (bPlayer.getBoundAbilityName().equalsIgnoreCase("SteelSwing")) {
					if (com.projectkorra.projectkorra.ability.CoreAbility.hasAbility(player, SteelSwing.class)) {
						com.projectkorra.projectkorra.ability.CoreAbility.getAbility(player, SteelSwing.class).onClick();
					} else {
						new SteelSwing(player);
					}
				} else if (bPlayer.getBoundAbilityName().equalsIgnoreCase("LavaTangles")) {
					if (com.projectkorra.projectkorra.ability.CoreAbility.hasAbility(player, Abilities.PK_Abilities.Earth.LavaTangles.class)) {
						com.projectkorra.projectkorra.ability.CoreAbility.getAbility(player, Abilities.PK_Abilities.Earth.LavaTangles.class).onClick();
					}
				} else if (bPlayer.getBoundAbilityName().equalsIgnoreCase("WaterTentacle")) {
					if (com.projectkorra.projectkorra.ability.CoreAbility.hasAbility(player, Abilities.PK_Abilities.Water.WaterTentacle.class)) {
						com.projectkorra.projectkorra.ability.CoreAbility.getAbility(player, Abilities.PK_Abilities.Water.WaterTentacle.class).onClick();
					}
				} else if (bPlayer.getBoundAbilityName().equalsIgnoreCase("Harmony")) {
					if (com.projectkorra.projectkorra.ability.CoreAbility.hasAbility(player, Abilities.PK_Abilities.Air.Harmony.class)) {
						com.projectkorra.projectkorra.ability.CoreAbility.getAbility(player, Abilities.PK_Abilities.Air.Harmony.class).onLeftClick();
					} else {
						new Abilities.PK_Abilities.Air.Harmony(player);
					}
				} else if (bPlayer.getBoundAbilityName().equalsIgnoreCase("Resonance")) {
					new Abilities.PK_Abilities.Air.Resonance(player);
				} else if (bPlayer.getBoundAbilityName().equalsIgnoreCase("Burrow")) {
					if (com.projectkorra.projectkorra.ability.CoreAbility.hasAbility(player, Abilities.PK_Abilities.Earth.Burrow.class)) {
						com.projectkorra.projectkorra.ability.CoreAbility.getAbility(player, Abilities.PK_Abilities.Earth.Burrow.class).onClick();
					}
				} else if (bPlayer.getBoundAbilityName().equalsIgnoreCase("FlameWhip")) {
					if (!CoreAbility.hasAbility(player, FlameWhip.class)) {
						new FlameWhip(player);
					}
				} else if (!AmonPackPlugin.ENABLE_SKILL_TREE && bPlayer.getBoundAbilityName().equalsIgnoreCase("BoulderRoll")) {
					if (com.projectkorra.projectkorra.ability.CoreAbility.hasAbility(player, BoulderRoll.class)) {
						BoulderRoll br = com.projectkorra.projectkorra.ability.CoreAbility.getAbility(player, BoulderRoll.class);
						br.onLeftClick();
					}
				}
			}

		}

	}

	// @SuppressWarnings("deprecation")
	// @EventHandler
	// public void OnHit(EntityDamageByEntityEvent event) {
	// Entity attacker = event.getDamager();
	// Entity victim = event.getEntity();
	// if (victim instanceof Player) {
	// Player player = (Player) victim;
	// BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);
	// if (bPlayer != null && bPlayer.getBoundAbilityName() != null &&
	// bPlayer.getBoundAbilityName().equalsIgnoreCase("Counter")) {
	// if (!bPlayer.isOnCooldown(bPlayer.getBoundAbility())) {
	// if (player.isSneaking()) {
	// if
	// (player.getInventory().getItemInMainHand().isSimilar(BladesAbility.Sword1)) {
	// event.setCancelled(true);
	// }else return;
	// }else return;
	// }else return;
	// }else return;}
	// if (attacker instanceof Player){
	// Player player = (Player) attacker;
	// BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);
	// if (player.getItemInHand().isSimilar(BladesAbility.Sword1)) {
	// if
	// (player.getInventory().getItemInMainHand().isSimilar(BladesAbility.Sword1)) {
	// player.getInventory().remove(BladesAbility.Sword1);
	// player.getInventory().setItemInHand(BladesAbility.Sword1);
	// if (bPlayer.getBoundAbilityName() == null) {
	// player.getInventory().remove(BladesAbility.Sword1);
	// event.setCancelled(true);}else
	// if (bPlayer.getBoundAbility() == null) {
	// player.getInventory().remove(BladesAbility.Sword1);
	// event.setCancelled(true);}else
	// if (bPlayer.getBoundAbility().getElement() !=
	// AmonPackPlugin.getBladesElement()) {
	// player.getInventory().remove(BladesAbility.Sword1);
	// event.setCancelled(true);}else
	// if (bPlayer.getBoundAbility().getElement() ==
	// AmonPackPlugin.getBladesElement()) {
	// if (!bPlayer.isOnCooldown(bPlayer.getBoundAbilityName())) {
	// if (bPlayer.getBoundAbilityName().equalsIgnoreCase("Counter")) {
	// event.setCancelled(true);
	// }}
	// event.setDamage(0);
	// if (!bPlayer.isOnCooldown(bPlayer.getBoundAbilityName())) {
	// if (bPlayer.getBoundAbilityName().equalsIgnoreCase("Slash")) {
	// new Slash(player);
	// Slash.skill(player, (LivingEntity) event.getEntity());
	// player.getInventory().remove(BladesAbility.Sword1);
	// player.getInventory().setItemInHand(BladesAbility.Sword1);
	// }else
	// if (bPlayer.getBoundAbilityName().equalsIgnoreCase("Pierce")) {
	// new Pierce(player);
	// Pierce.skill(player, (LivingEntity) event.getEntity());
	// player.getInventory().remove(BladesAbility.Sword1);
	// player.getInventory().setItemInHand(BladesAbility.Sword1);
	// }
	// }
	// if (bPlayer.isOnCooldown(bPlayer.getBoundAbilityName())) {
	// player.getInventory().remove(BladesAbility.Sword1);
	// }}}
	// }else
	// if (!player.getItemInHand().isSimilar(BladesAbility.Sword1)) {
	// if (bPlayer.getBoundAbilityName() != null && bPlayer.getBoundAbility() !=
	// null) {
	// if (bPlayer.getBoundAbility().getElement() ==
	// AmonPackPlugin.getBladesElement()) {
	// event.setCancelled(true);
	// }} }}else return;
	// }
	//
	// @EventHandler
	// public void OnDrop(org.bukkit.event.player.PlayerDropItemEvent event) {
	// Player player = event.getPlayer();
	// if (event.getItemDrop().getItemStack().isSimilar(BladesAbility.Sword1)) {
	// player.getInventory().remove(BladesAbility.Sword1);
	// event.setCancelled(true);
	// }}
	// @EventHandler
	// public void OnInv(InventoryClickEvent event) {
	// HumanEntity player = event.getWhoClicked();
	// if (event.getCursor().isSimilar(BladesAbility.Sword1)) {
	// player.getInventory().remove(BladesAbility.Sword1);
	// event.setCancelled(true);
	// event.setResult(Result.DENY);
	// } else {
	// return;
	// }
	// if (event.getCurrentItem().isSimilar(BladesAbility.Sword1)) {
	// player.getInventory().remove(BladesAbility.Sword1);
	// event.setCancelled(true);
	// event.setResult(Result.DENY);
	// } else {
	// return;
	// }
	// }
	//
	// @EventHandler
	// public void OnWorldChange(PlayerChangedWorldEvent event) {
	// Player player = event.getPlayer();
	// if (player.getInventory().contains(BladesAbility.Sword1)) {
	// player.getInventory().remove(BladesAbility.Sword1);
	// }}
	// @EventHandler
	// public void OnLogin(PlayerLoginEvent event) {
	// Player player = event.getPlayer();
	// if (player.getInventory().contains(BladesAbility.Sword1)) {
	// player.getInventory().remove(BladesAbility.Sword1);
	// }}
	// @EventHandler
	// public void OnLogin(PlayerDeathEvent event) {
	// if (event.getDrops().contains(BladesAbility.Sword1)) {
	// event.getDrops().remove(BladesAbility.Sword1);
	// }}
	// @EventHandler
	// public void OnInteract(PlayerInteractEvent event) {
	// Player player = event.getPlayer();
	// BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);
	// if (bPlayer.getBoundAbilityName() == null) {
	// return;
	// }else
	// if (bPlayer.getBoundAbility() == null) {
	// return;
	// }else
	// if (bPlayer.getBoundAbility().getElement() !=
	// AmonPackPlugin.getBladesElement()) {
	// return;
	// }else
	// if (bPlayer.getBoundAbility().getElement() ==
	// AmonPackPlugin.getBladesElement()) {
	// if (!bPlayer.isOnCooldown(bPlayer.getBoundAbility())) {
	// if
	// (player.getInventory().getItemInMainHand().isSimilar(BladesAbility.Sword1)) {
	// if (event.getAction() == Action.RIGHT_CLICK_AIR) {
	// if (bPlayer.getBoundAbilityName().equalsIgnoreCase("Stab")) {
	// new Stab(player);
	// Stab.PpmSkill(player);
	// }}}}}else return;
	// }
	//

	@EventHandler
	public void onEntityDamage(org.bukkit.event.entity.EntityDamageEvent event) {
		if (event.getEntity() instanceof Player) {
			Player player = (Player) event.getEntity();
			if (CoreAbility.hasAbility(player, Feintstep.class)) {
				Feintstep fs = CoreAbility.getAbility(player, Feintstep.class);
				if (fs != null && fs.isStanceActive()) {
					event.setCancelled(true);
					org.bukkit.entity.Entity damager = (event instanceof EntityDamageByEntityEvent edbe) ? edbe.getDamager() : null;
					fs.onDodge(damager);
					return;
				}
			}
			if (CoreAbility.hasAbility(player, QuickPalms.class)) {
				QuickPalms qp = CoreAbility.getAbility(player, QuickPalms.class);
				if (qp != null && qp.isCounterStanceActive()) {
					org.bukkit.entity.Entity damager = (event instanceof EntityDamageByEntityEvent edbe) ? edbe.getDamager() : null;
					boolean blocked = qp.handleCounterDamage(damager);
					if (blocked) {
						event.setCancelled(true);
						return;
					}
				}
			}
			if (event.getCause() == DamageCause.FALL && FirelordStanceManager.isActive(player)) {
				event.setCancelled(true);
				FirelordStance stance = FirelordStanceManager.getStance(player);
				if (stance != null) {
					stance.triggerBoltBurst();
				}
			}
			if (com.projectkorra.projectkorra.ability.CoreAbility.hasAbility(player, FlameSplit.class)) {
				FlameSplit fs = com.projectkorra.projectkorra.ability.CoreAbility.getAbility(player, FlameSplit.class);
				if (fs != null && fs.isParrying()) {
					event.setCancelled(true);
					fs.onParryDamage();
				}
			}
			if (com.projectkorra.projectkorra.ability.CoreAbility.hasAbility(player, BoulderRoll.class)) {
				BoulderRoll br = com.projectkorra.projectkorra.ability.CoreAbility.getAbility(player, BoulderRoll.class);
				if (br != null && br.isRolling()) {
					event.setCancelled(true);
				}
			}
			if (com.projectkorra.projectkorra.ability.CoreAbility.hasAbility(player, GustShield.class)) {
				GustShield gs = com.projectkorra.projectkorra.ability.CoreAbility.getAbility(player, GustShield.class);
				if (gs != null && gs.isShielding()) {
					event.setCancelled(true);
					gs.onHit();
				}
			}
		}
	}

	@EventHandler
	public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
		if (event.getCause() == DamageCause.CUSTOM) {
			return;
		}

		if (event.getDamager() instanceof Arrow arrow) {
			if (arrow.hasMetadata("DaggerTrickArrow") || arrow.hasMetadata("PoisonDaggerArrow")) {
				event.setDamage(0.0);
				return;
			}
		}

		if (event.getDamager() instanceof Player attacker) {
			if (!activeAttackProcessors.add(attacker.getUniqueId())) {
				return;
			}
			try {
				BendingPlayer bAttacker = BendingPlayer.getBendingPlayer(attacker);

				// PulseBreak melee attack handler
				if (bAttacker != null && "PulseBreak".equalsIgnoreCase(bAttacker.getBoundAbilityName())) {
					if (!bAttacker.isOnCooldown("PulseBreak")) {
						if (event.getEntity() instanceof LivingEntity victim) {
							PulseBreak pb = new PulseBreak(attacker);
							pb.onHitEntity(victim);
						}
					}
				}

				// QuickPalms melee attack handler
				if (bAttacker != null && "QuickPalms".equalsIgnoreCase(bAttacker.getBoundAbilityName())) {
					if (!bAttacker.isOnCooldown("QuickPalms")) {
						if (event.getEntity() instanceof LivingEntity victim) {
							QuickPalms qp = CoreAbility.getAbility(attacker, QuickPalms.class);
							if (qp == null) {
								qp = new QuickPalms(attacker);
							}
							qp.onHitEntity(victim);
						}
					}
				}

				// HeartReading melee attack handler
				if (bAttacker != null && "HeartReading".equalsIgnoreCase(bAttacker.getBoundAbilityName())) {
					if (!bAttacker.isOnCooldown("HeartReading")) {
						if (event.getEntity() instanceof LivingEntity victim) {
							HeartReading hr = CoreAbility.getAbility(attacker, HeartReading.class);
							if (hr == null) {
								hr = new HeartReading(attacker);
							}
							hr.onHitEntity(victim);
						}
					}
				}

				// PointBlank precision attack handler
				if (CoreAbility.hasAbility(attacker, PointBlank.class)) {
					PointBlank pb = CoreAbility.getAbility(attacker, PointBlank.class);
					if (pb != null && event.getEntity() instanceof LivingEntity victim) {
						pb.onHitTarget(victim);
					}
				}

				// VeinFlow stance attack handler
				if (VeinFlowManager.isActive(attacker)) {
					VeinFlow stance = VeinFlowManager.getStance(attacker);
					if (stance != null && event.getEntity() instanceof LivingEntity victim) {
						stance.tryApplyAttackBuffs(victim);
					}
				}

				// FeintstepFlow bonus melee damage
				if (CoreAbility.hasAbility(attacker, Feintstep.class)) {
					Feintstep fs = CoreAbility.getAbility(attacker, Feintstep.class);
					if (fs != null && fs.isStanceActive() && fs.hasFlow()) {
						event.setDamage(event.getDamage() + 2.0);
					}
				}

				// Chi Tree Passives & Upgrades on Hit
				if (AmonPackPlugin.levelsBending != null) {
					PlayerBendingBranch branch = AmonPackPlugin.levelsBending.GetBranchByPlayerName(attacker.getName());
					if (branch != null) {
						if (branch.hasUpgrade("PrecisionStrikes") && Math.random() < 0.15) {
							if (event.getEntity() instanceof Player targetPlayer) {
								BendingPlayer bTarget = BendingPlayer.getBendingPlayer(targetPlayer);
								if (bTarget != null) {
									bTarget.blockChi();
									targetPlayer.getWorld().playSound(targetPlayer.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 1.8f);
									targetPlayer.getWorld().spawnParticle(Particle.CRIT, targetPlayer.getLocation().add(0, 1.0, 0), 10, 0.2, 0.2, 0.2, 0.1);
								}
							}
						}
						if (branch.hasUpgrade("RapidPunchDrain")) {
							if (bAttacker != null && "RapidPunch".equalsIgnoreCase(bAttacker.getBoundAbilityName())) {
								ChiManager.addChi(attacker, 5.0);
							}
						}
						if (branch.hasUpgrade("SwiftKickVault")) {
							if (bAttacker != null && "SwiftKick".equalsIgnoreCase(bAttacker.getBoundAbilityName())) {
								attacker.setVelocity(new Vector(0, 1.1, 0));
								attacker.getWorld().playSound(attacker.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 1.0f, 1.5f);
							}
						}
						if (branch.hasUpgrade("SwiftKickDisarm")) {
							if (bAttacker != null && "SwiftKick".equalsIgnoreCase(bAttacker.getBoundAbilityName())) {
								if (event.getEntity() instanceof LivingEntity victim) {
									victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 2, false, true));
									victim.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 40, 2, false, true));
								}
							}
						}
					}
				}
			} finally {
				activeAttackProcessors.remove(attacker.getUniqueId());
			}
		}
	}

	@EventHandler
	public void onProjectileHit(ProjectileHitEvent event) {
		if (event.getEntity() instanceof Arrow arrow) {
			if (arrow.hasMetadata("DaggerTrickArrow")) {
				if (event.getHitEntity() != null) {
					Player caster = null;
					if (arrow.getShooter() instanceof Player p) {
						caster = p;
					}
					if (caster != null && CoreAbility.hasAbility(caster, DaggerTrick.class)) {
						DaggerTrick dt = CoreAbility.getAbility(caster, DaggerTrick.class);
						dt.handleArrowHit(arrow, event.getHitEntity());
					} else if (event.getHitEntity() instanceof LivingEntity victim) {
						double dmg = arrow.getMetadata("DaggerTrickArrow").get(0).asDouble();
						DamageHandler.damageEntity(victim, dmg, null);
						arrow.remove();
					}
				} else {
					arrow.remove();
				}
			} else if (arrow.hasMetadata("PoisonDaggerArrow")) {
				if (event.getHitEntity() != null) {
					Player caster = null;
					if (arrow.getShooter() instanceof Player p) {
						caster = p;
					}
					if (caster != null && CoreAbility.hasAbility(caster, PoisonDagger.class)) {
						PoisonDagger pd = CoreAbility.getAbility(caster, PoisonDagger.class);
						pd.handleArrowHit(arrow, event.getHitEntity());
					} else if (event.getHitEntity() instanceof LivingEntity victim) {
						double dmg = arrow.getMetadata("PoisonDaggerArrow").get(0).asDouble();
						DamageHandler.damageEntity(victim, dmg, null);
						victim.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 100, 1, false, true));
						victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 1, false, true));
						arrow.remove();
					}
				} else {
					arrow.remove();
				}
			}
		}
	}

	@EventHandler
	public void onAbilityStart(AbilityStartEvent event) {
		if (event.getAbility() == null) return;
		String abiName = event.getAbility().getName();

		if (event.getAbility() instanceof ChiAbility && !(event.getAbility() instanceof AddonAbility)) {
			Player player = event.getAbility().getPlayer();
			if (player != null && !event.getAbility().isHarmlessAbility()) {
				PlayerBendingBranch branch = (AmonPackPlugin.levelsBending != null) ? AmonPackPlugin.levelsBending.GetBranchByPlayerName(player.getName()) : null;
				double defaultCost = 25.0;
				if ("Paralyze".equalsIgnoreCase(abiName)) {
					defaultCost = (branch != null && branch.hasUpgrade("ParalyzeExtended")) ? 25.0 : 40.0;
				} else if ("WarriorStance".equalsIgnoreCase(abiName)) {
					defaultCost = 40.0;
					if (branch != null && branch.hasUpgrade("WarriorStanceFortitude")) {
						player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 200, 1, false, false));
					}
				} else if ("AcrobatStance".equalsIgnoreCase(abiName)) {
					defaultCost = 40.0;
				} else if ("Smokescreen".equalsIgnoreCase(abiName)) defaultCost = 35.0;
				else if ("RapidPunch".equalsIgnoreCase(abiName)) defaultCost = 30.0;
				else if ("SwiftKick".equalsIgnoreCase(abiName)) defaultCost = 25.0;
				else if ("QuickStrike".equalsIgnoreCase(abiName) || "HighJump".equalsIgnoreCase(abiName)) defaultCost = 20.0;

				double chiCost = ChiManager.getAbilityChiCost(abiName, defaultCost);
				if (chiCost > 0) {
					if (!ChiManager.consumeChi(player, chiCost)) {
						event.setCancelled(true);
						event.getAbility().remove();
						return;
					}
				}
			}
		}

		if ("AirBlast".equalsIgnoreCase(abiName)) {
			if (event.getAbility() instanceof com.projectkorra.projectkorra.airbending.AirBlast pkBlast) {
				if (pkBlast.getSource() == null) {
					event.setCancelled(true);
					pkBlast.remove();
				}
			}
		}
	}

	@EventHandler
	public void onEntityPotionEffect(org.bukkit.event.entity.EntityPotionEffectEvent event) {
		if (event.getEntity() instanceof Player player) {
			if (event.getAction() == org.bukkit.event.entity.EntityPotionEffectEvent.Action.ADDED
					|| event.getAction() == org.bukkit.event.entity.EntityPotionEffectEvent.Action.CHANGED) {
				PotionEffect newEffect = event.getNewEffect();
				if (newEffect != null) {
					PotionEffectType type = newEffect.getType();
					if (type == PotionEffectType.SLOWNESS || type == PotionEffectType.BLINDNESS
							|| type == PotionEffectType.NAUSEA || type == PotionEffectType.WEAKNESS) {
						PlayerBendingBranch branch = (AmonPackPlugin.levelsBending != null) ? AmonPackPlugin.levelsBending.GetBranchByPlayerName(player.getName()) : null;
						if (branch != null && branch.hasUpgrade("IronBody")) {
							int reduced = (int) (newEffect.getDuration() * 0.65);
							if (reduced > 0 && reduced < newEffect.getDuration()) {
								event.setCancelled(true);
								player.addPotionEffect(new PotionEffect(type, reduced, newEffect.getAmplifier(), newEffect.isAmbient(), newEffect.hasParticles(), newEffect.hasIcon()));
							}
						}
					}
				}
			}
		}
	}

	@EventHandler
	public void onSwapHandItems(PlayerSwapHandItemsEvent event) {
		Player player = event.getPlayer();
		if (AmonPackPlugin.levelsBending == null) return;
		PlayerBendingBranch branch = AmonPackPlugin.levelsBending.GetBranchByPlayerName(player.getName());
		if (branch != null) {
			String swapAbi = branch.getSwapAbility();
			if (swapAbi != null && !swapAbi.isEmpty()) {
				boolean executed = SpecialTriggerManager.executeSpecialAbility(player, swapAbi, SpecialTriggerable.TriggerType.SWAP);
				if (executed) {
					event.setCancelled(true);
				}
			}
		}
	}

	@EventHandler
	public void onBassDropBlockClick(org.bukkit.event.player.PlayerInteractEvent event) {
		if (event.isCancelled() || event.getAction() != org.bukkit.event.block.Action.LEFT_CLICK_BLOCK
				|| event.getClickedBlock() == null) {
			return;
		}

		Player player = event.getPlayer();
		BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);
		if (bPlayer == null || bPlayer.getBoundAbilityName() == null
				|| !bPlayer.getBoundAbilityName().equalsIgnoreCase("BassDrop")) {
			return;
		}
		if (bPlayer.isOnCooldown("BassDrop") || bPlayer.getBoundAbility() == null
				|| !bPlayer.canBend(bPlayer.getBoundAbility())) {
			return;
		}

		new BassDrop(player, event.getClickedBlock());
		event.setCancelled(true);
	}
}

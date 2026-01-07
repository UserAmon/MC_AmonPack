package methods_plugins.Abilities;

import abilities.*;
import abilities.Util_Objects.EarthDisc;
import methods_plugins.AmonPackPlugin;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;

import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.Element;
import org.bukkit.inventory.ItemStack;
import AvatarSystems.Crafting.CraftingMenager;
import org.bukkit.potion.PotionEffectType;

public class AbilitiesListener implements Listener {

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
		BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);
		if (bPlayer.getBoundAbility() != null) {
			if (!bPlayer.isOnCooldown(bPlayer.getBoundAbility())) {
				if (!event.isCancelled() || bPlayer != null) {
					CheckEarthHealthBoost(player, bPlayer.getBoundAbility());
					if (bPlayer.getBoundAbilityName().equalsIgnoreCase("SandBreath")) {
						new SandBreath(player);
					} else if (bPlayer.getBoundAbilityName().equalsIgnoreCase("AirScythe")) {
						if (com.projectkorra.projectkorra.ability.CoreAbility.hasAbility(player,
								abilities.AirScythe.class)) {
							abilities.AirScythe scythe = com.projectkorra.projectkorra.ability.CoreAbility
									.getAbility(player, abilities.AirScythe.class);
							scythe.onShift();
						}
					} else if (bPlayer.getBoundAbilityName().equalsIgnoreCase("TideLock")) {
						new TideLock(player);
					} else if (bPlayer.getBoundAbilityName().equalsIgnoreCase("LightningWeave")) {
						new LightningWeave(player);
					}  else if (bPlayer.getBoundAbilityName().equalsIgnoreCase("EarthShift")) {
						new EarthShift(player);
					}  else if (bPlayer.getBoundAbilityName().equalsIgnoreCase("Whirlpool")) {
						new Whirlpool(player);
					} else if (bPlayer.getBoundAbilityName().equalsIgnoreCase("EarthDiscs")) {
						new EarthDiscs(player);
					} else if (bPlayer.getBoundAbilityName().equalsIgnoreCase("DiscHurl")) {
						if (!player.isSneaking()) {
							new DiscHurl(player);
						}
					} // else if (bPlayer.getBoundAbilityName().equalsIgnoreCase("MetalCompress")) {
					// if (player.getInventory().getChestplate().getType() !=
					// Material.IRON_CHESTPLATE) {
					// return;}
					// else if (player.getInventory().getChestplate().getType() == null) {
					// return;}
					// new MetalCompress(player);
					// }
				}
			} else
				return;
		} else
			return;
	}

	@SuppressWarnings("deprecation")
	@EventHandler
	public void OnSwing(PlayerAnimationEvent event) {
		Player player = event.getPlayer();
		BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);
		if (bPlayer.getBoundAbility() != null) {
			if (bPlayer.getBoundAbilityName().equalsIgnoreCase("DiscHurl")
					|| bPlayer.getBoundAbilityName().equalsIgnoreCase("EarthDiscs")) {
				EarthDisc.redirectNearby(player, 4.0);
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
				} else if (bPlayer.getBoundAbilityName().equalsIgnoreCase("SmokePull")) {
					new SmokePull(player);
				} else if (bPlayer.getBoundAbilityName().equalsIgnoreCase("SmokePath")) {
					new SmokePath(player);
				} else if (bPlayer.getBoundAbilityName().equalsIgnoreCase("SoundCrash")) {
					new SoundCrash(player);
				} else if (bPlayer.getBoundAbilityName().equalsIgnoreCase("SmokeSlash")) {
					new SmokeSlash(player);
				} else if (bPlayer.getBoundAbilityName().equalsIgnoreCase("SmokeShot")) {
					new SmokeShot(player, false);
				} else if (bPlayer.getBoundAbilityName().equalsIgnoreCase("AirScythe")) {
					new AirScythe(player);
				} else if (bPlayer.getBoundAbilityName().equalsIgnoreCase("NoisySlash")) {
					new NoisySlash(player);
				} else if (bPlayer.getBoundAbilityName().equalsIgnoreCase("EchoJab")) {
					new EchoJab(player);
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
							abilities.EarthDiscs.class)) {
						abilities.EarthDiscs discs = com.projectkorra.projectkorra.ability.CoreAbility
								.getAbility(player, abilities.EarthDiscs.class);
						discs.onClick();
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

	@SuppressWarnings("deprecation")
	@EventHandler
	public void Shift(PlayerToggleSneakEvent event) {
		Player player = event.getPlayer();
		BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);
		if (bPlayer.getBoundAbilityName() != null && bPlayer.getBoundAbility() != null) {
			if (!bPlayer.isOnCooldown(bPlayer.getBoundAbility())) {
				if (!event.isCancelled() && bPlayer != null) {
					if (!bPlayer.getBoundAbilityName().equalsIgnoreCase((String) null)) {
						// if (bPlayer.getBoundAbilityName().equalsIgnoreCase("Counter")) {
						// if (player.getInventory().getItemInHand().getType() == Material.AIR) {
						// new Counter(player);
						// }}
						if (bPlayer.getBoundAbilityName().equalsIgnoreCase("AirPressure")) {
							new AirPressure(player);
						}
						if (bPlayer.getBoundAbilityName().equalsIgnoreCase("EarthHammer")) {
							if (bPlayer.getBoundAbilityName().equalsIgnoreCase("SmokeShot")) {
								new SmokeShot(player, true);
							}
							if (bPlayer.getBoundAbilityName().equalsIgnoreCase("IceArch")) {
								new IceArch(player);
							}
						}
					}
				}
			}
		}

	}
}
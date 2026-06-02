package Abilities.Bending;

import Abilities.PK_Abilities.Air.*;
import Abilities.PK_Abilities.Earth.*;
import Abilities.PK_Abilities.Fire.*;
import Abilities.PK_Abilities.Water.*;
import Abilities.Util_Objects.EarthDisc;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;

import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.Element;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

public class AbilitiesListener implements Listener {

	@EventHandler
	public void onShift(PlayerToggleSneakEvent event) {
		Player player = event.getPlayer();
		BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);
		if (bPlayer.getBoundAbility() != null) {
			if (!bPlayer.isOnCooldown(bPlayer.getBoundAbility())) {
				if (!event.isCancelled() || bPlayer != null) {
					String boundAbility = bPlayer.getBoundAbilityName();
					if (boundAbility.equalsIgnoreCase("SandBreath")) {
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
					} else if (boundAbility.equalsIgnoreCase("Whirlpool")) {
						new Whirlpool(player);
					} else if (boundAbility.equalsIgnoreCase("CalmTide")) {
						new CalmTide(player);
					} else if (boundAbility.equalsIgnoreCase("FerroAbsorb")) {
						if (com.projectkorra.projectkorra.ability.CoreAbility.hasAbility(player, FerroAbsorb.class)) {
							if (event.isSneaking()) {
								com.projectkorra.projectkorra.ability.CoreAbility.getAbility(player, FerroAbsorb.class).onShift();
							}
						} else {
							if (event.isSneaking()) {
								new FerroAbsorb(player);
							}
						}
					} else if (boundAbility.equalsIgnoreCase("EarthDiscs")) {
						new EarthDiscs(player);
					} else if (boundAbility.equalsIgnoreCase("DiscHurl")) {
						if (!player.isSneaking()) {
							new DiscHurl(player);
						}
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
					}
					 else if (boundAbility.equalsIgnoreCase("BloodArrow")) {
						new BloodArrow(player);
					}
					 else if (boundAbility.equalsIgnoreCase("BloodCall")) {
						new BloodCall(player);
					} else if (boundAbility.equalsIgnoreCase("IceArch")) {
						new IceArch(player);
					} else if (boundAbility.equalsIgnoreCase("SmokeBurst")) {
						new SmokeBurst(player, true);
					} else if (boundAbility.equalsIgnoreCase("SmokeBarrage")) {
						new SmokeBarrage(player);
					} else if (boundAbility.equalsIgnoreCase("SmokeCamouflage")) {
						new SmokeCamouflage(player);
					}
				}
			} else
				return;
		} else
			return;
	}

	@EventHandler
	public void OnSwing(PlayerAnimationEvent event) {
		Player player = event.getPlayer();
		BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);
		if (bPlayer.getBoundAbility() != null) {
			if (bPlayer.getBoundAbilityName().equalsIgnoreCase("DiscHurl")
					|| bPlayer.getBoundAbilityName().equalsIgnoreCase("EarthDiscs")
					|| bPlayer.getBoundAbilityName().equalsIgnoreCase("SandDisc")) {
				EarthDisc.redirectNearby(player, 4.0);
			}
			if (!bPlayer.isOnCooldown(bPlayer.getBoundAbility())) {

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
				} else if (bPlayer.getBoundAbilityName().equalsIgnoreCase("AerialPush")) {
					new AerialPush(player);
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
				} else if (bPlayer.getBoundAbilityName().equalsIgnoreCase("SteelGrab")) {
					if (!com.projectkorra.projectkorra.ability.CoreAbility.hasAbility(player, SteelGrab.class)) {
						new SteelGrab(player);
					}
				} else if (bPlayer.getBoundAbilityName().equalsIgnoreCase("FerroAbsorb")) {
					if (com.projectkorra.projectkorra.ability.CoreAbility.hasAbility(player, FerroAbsorb.class)) {
						com.projectkorra.projectkorra.ability.CoreAbility.getAbility(player, FerroAbsorb.class).onLeftClick();
					} else {
						new FerroAbsorb(player);
					}
				}
			}

		}

	}

	private boolean isFerroItem(ItemStack item) {
		if (item == null || item.getItemMeta() == null) {
			return false;
		}
		String name = item.getItemMeta().getDisplayName();
		if (name == null) {
			return false;
		}
		return name.equals(org.bukkit.ChatColor.GOLD + "Ferro-Absorb Plate") || name.equals(org.bukkit.ChatColor.GOLD + "Ferro-Clip Plate");
	}

	@EventHandler
	public void onInventoryClick(org.bukkit.event.inventory.InventoryClickEvent event) {
		ItemStack item = event.getCurrentItem();
		if (isFerroItem(item)) {
			event.setCancelled(true);
			return;
		}
		ItemStack cursor = event.getCursor();
		if (isFerroItem(cursor)) {
			event.setCancelled(true);
			return;
		}
		if (event.getClick() == org.bukkit.event.inventory.ClickType.NUMBER_KEY) {
			ItemStack hotbarItem = event.getWhoClicked().getInventory().getItem(event.getHotbarButton());
			if (isFerroItem(hotbarItem)) {
				event.setCancelled(true);
				return;
			}
		}
	}

	@EventHandler
	public void onPlayerDropItem(org.bukkit.event.player.PlayerDropItemEvent event) {
		ItemStack item = event.getItemDrop().getItemStack();
		if (isFerroItem(item)) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onPlayerDeath(org.bukkit.event.entity.PlayerDeathEvent event) {
		Player player = event.getEntity();
		if (com.projectkorra.projectkorra.ability.CoreAbility.hasAbility(player, FerroAbsorb.class)) {
			FerroAbsorb fa = com.projectkorra.projectkorra.ability.CoreAbility.getAbility(player, FerroAbsorb.class);
			if (fa != null) {
				java.util.Iterator<ItemStack> iterator = event.getDrops().iterator();
				while (iterator.hasNext()) {
					ItemStack drop = iterator.next();
					if (drop != null && drop.getType() == org.bukkit.Material.IRON_CHESTPLATE && drop.getItemMeta() != null && drop.getItemMeta().getDisplayName().equals(org.bukkit.ChatColor.GOLD + "Ferro-Absorb Plate")) {
						iterator.remove();
					}
				}
				ItemStack orig = fa.getOriginalChestplate();
				if (orig != null && orig.getType() != org.bukkit.Material.AIR) {
					event.getDrops().add(orig);
				}
				fa.setPlateActive(false);
				fa.remove();
			}
		}
	}

	@EventHandler
	public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
		Player player = event.getPlayer();
		if (com.projectkorra.projectkorra.ability.CoreAbility.hasAbility(player, FerroAbsorb.class)) {
			FerroAbsorb fa = com.projectkorra.projectkorra.ability.CoreAbility.getAbility(player, FerroAbsorb.class);
			if (fa != null) {
				fa.cleanup();
				fa.remove();
			}
		}
	}

	@EventHandler
	public void onDamage(org.bukkit.event.entity.EntityDamageByEntityEvent event) {
		if (event.getEntity() instanceof Player victim) {
			if (com.projectkorra.projectkorra.ability.CoreAbility.hasAbility(victim,
					Abilities.PK_Abilities.Air.GustShield.class)) {
				Abilities.PK_Abilities.Air.GustShield shield = com.projectkorra.projectkorra.ability.CoreAbility.getAbility(
						victim,
						Abilities.PK_Abilities.Air.GustShield.class);
				event.setCancelled(true);
				shield.onHit();
			}
		}
	}
}
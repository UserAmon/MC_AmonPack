package abilities;

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

import methods_plugins.AmonPackPlugin;
import methods_plugins.Methods;
public class SandBreath extends SandAbility implements AddonAbility {
	
    private int cooldown = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Earth.Sand.SandBreath.Cooldown");
    private int Range = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Earth.Sand.SandBreath.Range");
    private int time = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Earth.Sand.SandBreath.Duration");
    private int ChargeTime = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Earth.Sand.SandBreath.ChargeTime");
    static Boolean buffs = AmonPackPlugin.plugin.getConfig().getBoolean("AmonPack.Earth.Sand.SandBreath.ChargedBreathBuff");
	private int speedsand = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Earth.Sand.SandBreath.SpeedOnSand");
	private int speedearth = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Earth.Sand.SandBreath.SpeedOnEarth");
	public static int DeBuffsPower = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Earth.Sand.SandBreath.DeBuffPower");
	public static int DeBuffsDuration = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Earth.Sand.SandBreath.DebuffDuration");
	private int Dmg = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Earth.Sand.SandBreath.Damage");
	private int durationtuse = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Earth.Sand.SandBreath.DurationToUseBreath");
	public static Boolean push = AmonPackPlugin.plugin.getConfig().getBoolean("AmonPack.Earth.Sand.SandBreath.CanDebuffEnemy");
	private int abilityState;
	private Ability abi = this;
	private int usage;
	private int usagev2;
	private HashMap<String, Integer> taskID = new HashMap<String, Integer>();
	private HashMap<String, BukkitTask> deltask = new HashMap<String, BukkitTask>();
	public SandBreath(Player player) {
		super(player);
		if (bPlayer.isOnCooldown(this)) {
			return;
		}
		if (!bPlayer.canBend(this)) {
			return;
		}
		if (EarthAbility.isSandbendable(player, Methods.getTargetLocation(player, 15).getBlock().getBlockData().getMaterial())) {
		usage = 0;
		usagev2 = 0;
		if (!deltask.isEmpty()) {
			deltask.clear();
		}
		abilityState = 0;
		time = 0;
		start();
	}}
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
		 				if (abilityState ==1) {
		 					abilityState = 0;
		 					if (!deltask.isEmpty()) {
		 						deltask.clear();
		 					}
		 					bPlayer.addCooldown(abi);
		 					remove();
		 					return;
		 				}
		 			}	
         		}.runTaskLater(ProjectKorra.plugin,durationtuse*20));
				
			} else if (!deltask.isEmpty()) {
				deltask.clear();
				deltask.put("delayedtask", new BukkitRunnable() {
         			@Override
         			public void run() {
		 				if (abilityState ==1) {
		 					abilityState =0;
		 					if (!deltask.isEmpty()) {
		 						deltask.clear();
		 					}
		 					bPlayer.addCooldown(abi);
		 					remove();
		 					return;
		 				}
		 			}	
         		}.runTaskLater(ProjectKorra.plugin,durationtuse*20));
				
			
			}
		}else if (System.currentTimeMillis() < getStartTime() + 3000) {
			if (!EarthAbility.isSandbendable(player, Methods.getTargetLocation(player, 15).getBlock().getBlockData().getMaterial())) {
				remove();
				return; 
			} else {
				Methods.displayLineBetweenPoints(player.getLocation().subtract(0,0.3,0), Methods.getTargetLocation(player, 10).getBlock().getLocation(), 10, Material.SAND, 1);

			}} else if (!player.isSneaking()) {
				remove();
				return;
				
				}
			}else if (!player.isSneaking()) {
				remove();
				abilityState = 0;
				return;
			}
				}
		
		if (abilityState == 1) {
			ParticleEffect.BLOCK_CRACK.display(player.getLocation(), 1, 1, 1, 1, 0.1, Material.SAND.createBlockData());
			if (buffs == true) {
				
				for (Block blocks : GeneralMethods.getBlocksAroundPoint(player.getLocation(), 2)) {
					if (EarthAbility.isSandbendable(player, blocks.getType())) {
						player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 10 , speedsand , false , false));
					}else
					if (EarthAbility.isEarthbendable(player, blocks)) {
						player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 10 , speedearth , false , false));
					}
				}
				
	        	
			}
			if (usage == 0 && !player.isSneaking()) {
				usage = 1;
			}
		}
		if (abilityState == 1 && usage == 1 && player.isSneaking() && bPlayer.getBoundAbilityName().equalsIgnoreCase("SandBreath")) {
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
			Location location = player.getLocation().clone().add(0,1,0);
			Vector dir = player.getLocation().getDirection();
			Methods.stream(location, dir, player, abi, Material.SAND, Range, Dmg);
			time = time+1;
				}}
			
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
	public String getDescription() {
		return "...";
	}
	@Override
	public String getInstructions() {
		return "...";
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
}
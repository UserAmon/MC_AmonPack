package abilities;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.IceAbility;
import com.projectkorra.projectkorra.ability.WaterAbility;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.ParticleEffect;
import com.projectkorra.projectkorra.util.TempBlock;

import methods_plugins.AmonPackPlugin;
import methods_plugins.Methods;


public class IceArch extends IceAbility implements AddonAbility {
	
    private int Cooldown;
    private int Range;
    private int Dmg;
    private int ArchWidth;
    private int ArchDuration;
    private int Thick;
    private int chargetime;
	private boolean freeze;
	private int freezeDuration;
	
	int act;
	Location origin;
	Location projectile;
	Vector dir;
	int i;
	public int abilityState;
	public IceArch(Player player) {
		super(player);
		if (bPlayer.isOnCooldown(this)) {
			return;
		}
		if (!bPlayer.canBend(this)) {
			return;
		}
		SetConf();
		act = 0;
		abilityState = 0;
		i=0;
		Location loc = Methods.getTargetLocation(player, 20);
		if (!player.isSneaking()) {
		if (WaterAbility.isWaterbendable(loc.getBlock().getBlockData().getMaterial()) || WaterAbility.isPlantbendable(player, loc.getBlock().getBlockData().getMaterial(), false) || WaterAbility.isIcebendable(player, loc.getBlock().getBlockData().getMaterial(), false) || WaterAbility.isSnow(loc.getBlock().getBlockData().getMaterial()) || WaterAbility.isWater(loc.getBlock().getBlockData().getMaterial()) || loc.getBlock().getBlockData().getMaterial() == Material.GRASS_BLOCK) {
		Methods.SmoothBlock(player, loc, Material.WATER, true);
		start();
		bPlayer.addCooldown(this);
		}}
	        
	}
	@Override
	public void progress() {
		if (bPlayer.getBoundAbility() == null || bPlayer.getBoundAbilityName() == null) {
			if (player.isSneaking() && i==0) {
				bPlayer.addCooldown(this);
				remove();
			}
		}
		if (bPlayer.getBoundAbility() != null || bPlayer.getBoundAbilityName() != null) {
		if (!bPlayer.getBoundAbilityName().equalsIgnoreCase("IceArch")) {
			if (player.isSneaking()  && i==0) {
				bPlayer.addCooldown(this);
				remove();
			}
		}
		if (player.isDead() || !player.isOnline()) {
		remove();
		return;
		}
		if (abilityState == 0) {
		if (player.isSneaking()) {
			bPlayer.addCooldown(this);
		if (System.currentTimeMillis() > getStartTime() + chargetime) {
			abilityState = 1;
		}
		}else if (!player.isSneaking()) {
		remove();
		}}else if (abilityState == 1){
		if (player.isSneaking() && i ==0) {
			bPlayer.addCooldown(this);
			ParticleEffect.BLOCK_CRACK.display(player.getLocation().clone().add(player.getLocation().getDirection()).multiply(1), 4, 0.3, 0.3, 0.3, 0, Material.ICE.createBlockData());
		}else if (!player.isSneaking() || i == 1) {
			if (i == 0) {
			i = 1;
			dir = player.getLocation().getDirection().clone();
			origin = player.getLocation().add(0,1,0).clone();
			projectile = origin.clone();
			bPlayer.addCooldown(this);
			}
			projectile.add(dir).multiply(1);
			ParticleEffect.BLOCK_CRACK.display(projectile, 15, 2, 2, 2, 0, Material.ICE.createBlockData());
			if (projectile.distance(origin) > Range || (projectile.getBlock().getType().isSolid() && projectile.getBlock().getType() != Material.ICE)) {
				remove();
			}
	   		for (Entity entity : GeneralMethods.getEntitiesAroundPoint(projectile, ArchWidth)) {
	   		if ((entity instanceof LivingEntity) && entity.getUniqueId() != player.getUniqueId() && entity.getLocation().distance(player.getLocation()) > entity.getLocation().distance(projectile)) {
	   		DamageHandler.damageEntity(entity, Dmg, this);
	   	    }}
				for (Block b : GeneralMethods.getBlocksAroundPoint(projectile, ArchWidth)) {
		   			if (b.getType() == Material.WATER) {
		   				TempBlock tb1 = new TempBlock(b, Material.ICE);
		   	    		tb1.setRevertTime(5000);
		   			}else if (b.getLocation().distance(projectile) > Thick && b.getType() == Material.AIR && b.getLocation().distance(origin) > (Thick+1) && b.getLocation().distance(origin) > projectile.distance(origin)) {
		   				TempBlock tb1 = new TempBlock(b, Material.ICE);
		   	    		tb1.setRevertTime(ArchDuration);
		   			}else if (b.getLocation().distance(projectile) < Thick && b.getType() == Material.ICE && b.getLocation().getY() >= (projectile.getY()-1)) {
		   				TempBlock tb1 = new TempBlock(b, Material.AIR);
		   	    		tb1.setRevertTime(5000);
		   			}
				  
    			  }
	   		
	   		
	   		
		}
		}}else {
			bPlayer.addCooldown(this);
			remove();}
		
		}
	public void SetConf() {
		
		
		if (isNight(player.getWorld())) {
    	if (isFullMoon(player.getWorld())) {
    	    Cooldown = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Water.Ice.IceArch.FullMoonAugment.Cooldown");
    	    chargetime = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Water.Ice.IceArch.FullMoonAugment.ChargeTime");
    	    Range = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Water.Ice.IceArch.FullMoonAugment.Range");
    	    Dmg = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Water.Ice.IceArch.FullMoonAugment.Damage");
    	    ArchWidth = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Water.Ice.IceArch.FullMoonAugment.Arch-Width");
    	    ArchDuration = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Water.Ice.IceArch.FullMoonAugment.Arch-Duration");
    	    Thick = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Water.Ice.IceArch.FullMoonAugment.Arch-Thickness");
    	    freeze = AmonPackPlugin.plugin.getConfig().getBoolean("AmonPack.Water.Ice.IceArch.FullMoonAugment.CanFreeze");
    	    freezeDuration = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Water.Ice.IceArch.FullMoonAugment.FreezeDuration");
    	}else{
    	    Cooldown = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Water.Ice.IceArch.NightAugment.Cooldown");
    	    chargetime = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Water.Ice.IceArch.NightAugment.ChargeTime");
    	    Range = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Water.Ice.IceArch.NightAugment.Range");
    	    Dmg = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Water.Ice.IceArch.NightAugment.Damage");
    	    ArchWidth = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Water.Ice.IceArch.NightAugment.Arch-Width");
    	    ArchDuration = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Water.Ice.IceArch.NightAugment.Arch-Duration");
    	    Thick = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Water.Ice.IceArch.NightAugment.Arch-Thickness");
    	    freeze = AmonPackPlugin.plugin.getConfig().getBoolean("AmonPack.Water.Ice.IceArch.NightAugment.CanFreeze");
    	    freezeDuration = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Water.Ice.IceArch.NightAugment.FreezeDuration");
    	}	
		}else if (isDay(player.getWorld())) {
		    Cooldown = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Water.Ice.IceArch.Cooldown");
		    Range = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Water.Ice.IceArch.Range");
		    Dmg = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Water.Ice.IceArch.Damage");
		    chargetime = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Water.Ice.IceArch.ChargeTime");
		    ArchWidth = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Water.Ice.IceArch.Arch-Width");
		    ArchDuration = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Water.Ice.IceArch.Arch-Duration");
		    Thick = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Water.Ice.IceArch.Arch-Thickness");
		    freeze = AmonPackPlugin.plugin.getConfig().getBoolean("AmonPack.Water.Ice.IceArch.CanFreeze");
		    freezeDuration = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Water.Ice.IceArch.FreezeDuration");
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
		return "IceArch";
	}
	@Override
	public String getDescription() {
		return "";
	}
	@Override
	public String getInstructions() {
		return "";
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
	public void load() {}
	@Override
	public void stop() {
		super.remove();
	}
}
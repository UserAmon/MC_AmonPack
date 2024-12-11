package abilities;

import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.ComboAbility;
import com.projectkorra.projectkorra.ability.IceAbility;
import com.projectkorra.projectkorra.ability.WaterAbility;
import com.projectkorra.projectkorra.ability.util.ComboManager.AbilityInformation;
import com.projectkorra.projectkorra.util.ClickType;

import methods_plugins.AmonPackPlugin;
import methods_plugins.Methods;

import java.util.ArrayList;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class IcySpace extends IceAbility implements AddonAbility, ComboAbility {
	private Location origin;
    private int Cooldown = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Water.Ice.IcySpace.Cooldown");
    private int Range = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Water.Ice.IcySpace.Range");
    private int Duration = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Water.Ice.IcySpace.Duration");
    private int FulCooldown = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Water.Ice.IcySpace.FullMoonAugment.Cooldown");
    private int FulRange = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Water.Ice.IcySpace.FullMoonAugment.Range");
    private int FulDuration = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Water.Ice.IcySpace.FullMoonAugment.Duration");
    private int NightCooldown = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Water.Ice.IcySpace.NightAugment.Cooldown");
    private int NightRange = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Water.Ice.IcySpace.NightAugment.Range");
    private int NightDuration = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Water.Ice.IcySpace.NightAugment.Duration");
    private int Delay1 = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Water.Ice.IcySpace.1stPhaseDelay");
    private int Delay2 = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Water.Ice.IcySpace.2ndPhaseDelay");
    	@SuppressWarnings("deprecation")
		public IcySpace(Player player) {
    		super(player);
    		if (bPlayer.isOnCooldown(this)) {
    			return;
    		}
    		if (player.isOnGround()) {
    			origin = player.getLocation().clone();
        		start();
    	}else return;
    		}

		@Override
    	public void progress() {
			Location loc = Methods.getTargetLocation(player, 20);
			if (WaterAbility.isWaterbendable(loc.getBlock().getBlockData().getMaterial()) || WaterAbility.isPlantbendable(player, loc.getBlock().getBlockData().getMaterial(), false) || WaterAbility.isIcebendable(player, loc.getBlock().getBlockData().getMaterial(), false) || WaterAbility.isSnow(loc.getBlock().getBlockData().getMaterial()) || WaterAbility.isWater(loc.getBlock().getBlockData().getMaterial()) || loc.getBlock().getBlockData().getMaterial() == Material.GRASS_BLOCK) {
				Methods.SmoothBlock(player, loc, Material.WATER, true);
				if (isNight(player.getWorld())) {
		    		if (isFullMoon(player.getWorld())) {
				        Methods.FreezeField(origin, FulRange, FulDuration, Delay1, Delay2);
				   	    bPlayer.addCooldown(this, FulCooldown);
				   	    remove();
				   	    return;
		    		} else
			        Methods.FreezeField(origin, NightRange, NightDuration, Delay1, Delay2);
			   	    bPlayer.addCooldown(this, NightCooldown);
			   	    remove();
			   	    return;
	    		}else if (isDay(player.getWorld())) {
			        Methods.FreezeField(origin, Range, Duration, Delay1, Delay2);
			   	    bPlayer.addCooldown(this);
			   	    remove();
			   	    return;
	    		}
		        
			} else 
				remove();
			return;
    			
    			}

    public long getCooldown() {
        return Cooldown; 
    }
    public String getName() {
        return "IcySpace";
    }

public String getDescription() {
    return "";
}

public String getInstructions() {
    return "Torrent (Left Click)  -> PhaseChange (Shift)";
}

public String getAuthor() {
    return "AmonPack";
}

public String getVersion() {
    return "AmonPack";
}

public boolean isHarmlessAbility() {
    return false;
}

public boolean isSneakAbility() {
    return false;
}


public Object createNewComboInstance(Player player) {
    return new IcySpace(player);
}

public ArrayList<AbilityInformation> getCombination() {
    ArrayList<AbilityInformation> IcySpace = new ArrayList<AbilityInformation>();
    IcySpace.add(new AbilityInformation("Torrent", ClickType.LEFT_CLICK));
    IcySpace.add(new AbilityInformation("PhaseChange", ClickType.SHIFT_DOWN));
    return IcySpace;
    }

    public void load() {

    }
    


	@Override
	public void stop() {
	    super.remove();
		
	}

	@Override
	public Location getLocation() {
		return null;
	}


	}
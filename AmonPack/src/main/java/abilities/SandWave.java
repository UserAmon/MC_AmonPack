package abilities;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.ComboAbility;
import com.projectkorra.projectkorra.ability.EarthAbility;
import com.projectkorra.projectkorra.ability.util.ComboManager.AbilityInformation;
import com.projectkorra.projectkorra.util.ClickType;
import com.projectkorra.projectkorra.util.TempBlock;

import methods_plugins.AmonPackPlugin;

import java.util.ArrayList;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
@SuppressWarnings("deprecation")

public class SandWave extends EarthAbility implements AddonAbility, ComboAbility {
	
    private int cooldown = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Earth.Sand.SandWave.Cooldown");
	private int range = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Earth.Sand.SandWave.Range");
	private int time = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Earth.Sand.SandWave.Duration");
    private int size = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Earth.Sand.SandWave.Size");
    public static int DeBuffsPower = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Earth.Sand.SandWave.DeBuffPower");
    public static int DeBuffsDuration = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Earth.Sand.SandWave.DebuffDuration");
    public static int burrow = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Earth.Sand.SandWave.BurrowPower");
	private Location proj;
	private Location oriploc;
	private Vector direction;
	int safeint;
	
		public SandWave(Player player) {
    		super(player);
    		if (bPlayer.isOnCooldown(this)) {
    			return;
    		}
    		if (!player.isOnGround()) {
    			return;
    		}
    		oriploc = player.getLocation().clone();
    		proj = oriploc.clone();	
    		direction = player.getLocation().getDirection();
    		safeint = 0;
        	start();
        	bPlayer.addCooldown(this);
    		}
		
		@Override
    	public void progress() {
			
    		if (player.isDead() || !player.isOnline()) {
    			remove();
    			return;
    		}
    		
    		/*if (GeneralMethods.isSolid(proj.getBlock())) {
    			remove();
    			return;
    		}*/

    		if (oriploc.distance(proj) > (range)) {
    			remove();
    			return;
    		}
    		safeint = safeint+1;
    		if (safeint > 20) {
    			return;
    		}
    		if (proj.getY() != oriploc.getY()) {
    			proj.setY(oriploc.getY());;
    		}
    		proj.add(direction.multiply(1));
        	for (Block blocks : GeneralMethods.getBlocksAroundPoint(proj.clone().subtract(0,1,0), size)) {
        		if (blocks.getLocation().getY() == oriploc.clone().subtract(0,1,0).getY()) {
        			if (EarthAbility.isEarthbendable(player,blocks)) {
        				if (proj.distance(blocks.getLocation()) < size) {
		        		TempBlock tb1 = new TempBlock(blocks, Material.SANDSTONE);
		   	    		tb1.setRevertTime(time);
        			}}
        		}
        		if (blocks.getLocation().distance(oriploc) > 3) {
        			if (blocks.getLocation().getY() == oriploc.clone().getY()) {
	        				if (proj.distance(blocks.getLocation()) < size) {
	           		for (Entity entity : GeneralMethods.getEntitiesAroundPoint(blocks.getLocation(), 2)) {
	        			if ((entity instanceof LivingEntity)) {
		        			if (entity.getUniqueId() != player.getUniqueId()) {
		        				if (entity.getLocation().getY() >= oriploc.getY()) {
									entity.teleport(entity.getLocation().clone().subtract(0,SandWave.burrow,0));
									((LivingEntity) entity).addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, SandWave.DeBuffsDuration , SandWave.DeBuffsPower , false , false));
									((LivingEntity) entity).addPotionEffect(new PotionEffect(PotionEffectType.SLOW, SandWave.DeBuffsDuration, SandWave.DeBuffsPower , false , false));
		        				}
		        			}
	        			}}
	           		for (Entity entity : GeneralMethods.getEntitiesAroundPoint(blocks.getLocation(), 4)) {
	        			if ((entity instanceof LivingEntity)) {
	    		        			if (entity.getUniqueId() == player.getUniqueId()) {
				    					if (player.isSneaking()) {
	    								Vector forceDir = GeneralMethods.getDirection(player.getLocation(), proj.clone().add(0,2,0));
	    								player.setVelocity(forceDir.clone().normalize().multiply(0.8));
	    		        			}}}}}}
 
        		if (blocks.getLocation().getY() == oriploc.clone().add(0,1,0).getY()) {
        			if (blocks.getType().isAir()) {
        				if (EarthAbility.isEarthbendable(player,blocks.getLocation().clone().subtract(0,1,0).getBlock())) {
	        				if (!blocks.getLocation().clone().subtract(0,1,0).getBlock().isLiquid()) {
		        				if (proj.distance(blocks.getLocation()) < size) {
			        			if (oriploc.distance(blocks.getLocation()) > (proj.distance(oriploc)-1)) {
						        TempBlock tb1 = new TempBlock(blocks, Material.SANDSTONE);
						   	    tb1.setRevertTime(100);
			        				}}}}}}
        		if (blocks.getLocation().getY() == oriploc.clone().getY()) {
        			if (blocks.getType().isAir()) {
        				if (EarthAbility.isEarthbendable(player,blocks.getLocation().clone().subtract(0,1,0).getBlock())) {
	        				if (!blocks.getLocation().clone().subtract(0,1,0).getBlock().isLiquid()) {
		        				if (proj.distance(blocks.getLocation()) < size) {
			        			if (oriploc.distance(blocks.getLocation()) > (proj.distance(oriploc)-2)) {
						        TempBlock tb1 = new TempBlock(blocks, Material.SANDSTONE);
						   	    tb1.setRevertTime(100);
			        				}}}}}}
        		}}}	
		
public long getCooldown() {
        return cooldown; 
}
public String getName() {
        return "SandWave";
}

public String getDescription() {
    return "";
}

public String getInstructions() {
    return "SandBreath (Shift Down)  -> Shockwave (Shift Up)";
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
    return new SandWave(player);
}

public ArrayList<AbilityInformation> getCombination() {
    ArrayList<AbilityInformation> SandWave = new ArrayList<AbilityInformation>();
    SandWave.add(new AbilityInformation("SandBreath", ClickType.SHIFT_DOWN));
    SandWave.add(new AbilityInformation("Shockwave", ClickType.SHIFT_UP));
    return SandWave;
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
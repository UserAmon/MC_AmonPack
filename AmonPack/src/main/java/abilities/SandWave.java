package abilities;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.*;
import com.projectkorra.projectkorra.ability.util.ComboManager.AbilityInformation;
import com.projectkorra.projectkorra.util.ClickType;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.ParticleEffect;
import com.projectkorra.projectkorra.util.TempBlock;

import methods_plugins.AmonPackPlugin;

import java.util.ArrayList;
import java.util.List;

import methods_plugins.Methods;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import static methods_plugins.Methods.getRandom;

@SuppressWarnings("deprecation")

public class SandWave extends SandAbility implements AddonAbility, ComboAbility {
	
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
	List<Entity>Burrowed;
	private int interval;
	private double growth;

		public SandWave(Player player) {
    		super(player);
    		if (bPlayer.isOnCooldown(this)) {
    			return;
    		}
    		if (!player.isOnGround()) {
    			return;
    		}
			interval=0;
			Burrowed=new ArrayList<>();
    		oriploc = player.getLocation().clone();
			oriploc.setPitch(0);
			growth=1;
    		proj = oriploc.clone();	
    		direction = oriploc.getDirection();
			proj.add(direction).multiply(1);
			start();
        	bPlayer.addCooldown(this);
    		}
		
		@Override
    	public void progress() {
			if (player.isDead() || !player.isOnline()) {
				remove();
				return;
			}
			interval++;
				if(interval>1){
					interval=0;
					if(growth<size){
						growth=growth+0.5;
					}
					proj.add(direction).multiply(1);
					if(proj.getBlock().getType()!=Material.AIR && !PlantAbility.isPlant(proj.getBlock())){
						proj.setY(proj.getY()+1);
					}
					if(proj.clone().subtract(0,1,0).getBlock().getType()==Material.AIR){
						proj.setY(proj.getY()-1);
					}
					List<Block> BendableBlocks = new ArrayList<>();
					for (Block b : GeneralMethods.getBlocksAroundPoint(proj, growth)) {
						if (b.getY() < proj.getY() && (EarthAbility.isEarthbendable(player, b))) {
							BendableBlocks.add(b);
							if(oriploc.distance(b.getLocation())>oriploc.distance(proj)+0.5){
							int chance = getRandom(0, 20);
							if (chance <=  8) {
								TempBlock tb2 = new TempBlock(b, Material.SAND);
								tb2.setRevertTime(time);
							} else if (chance <=  16) {
								TempBlock tb2 = new TempBlock(b, Material.SANDSTONE);
								tb2.setRevertTime(time);
							}else if (chance <=  18) {
								Methods.spawnFallingBlocks(b.getLocation(),Material.SANDSTONE,1,0.7,player);
							}else {
								Methods.spawnFallingBlocks(b.getLocation(),Material.SAND,1,0.7,player);
							}
						}}}

					for (Entity entity : GeneralMethods.getEntitiesAroundPoint(proj, growth+2)) {
						if(entity.getLocation().distance(proj)<=growth){
							if ((entity instanceof LivingEntity)) {
								if (entity.getUniqueId() != player.getUniqueId() && !Burrowed.contains(entity)) {
									if (entity.getLocation().getY() < proj.getY()+1) {
										entity.teleport(entity.getLocation().clone().subtract(0,burrow,0));
										((LivingEntity) entity).addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, SandWave.DeBuffsDuration , SandWave.DeBuffsPower , false , false));
										((LivingEntity) entity).addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, SandWave.DeBuffsDuration, SandWave.DeBuffsPower , false , false));
										Burrowed.add(entity);
									}}}
						}else{
							if (entity.getUniqueId() == player.getUniqueId()) {
								if (player.isSneaking()) {
									Vector forceDir = GeneralMethods.getDirection(player.getLocation(), proj.clone().add(0,1,0));
									player.setVelocity(forceDir.clone().normalize().multiply(0.8));
								}}}}
					if (BendableBlocks.size()<2){
						remove();
					}}
				if (proj.distance(oriploc) > range+3){
					remove();
				}
		}
		
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
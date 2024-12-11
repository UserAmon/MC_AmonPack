package abilities;
import java.util.HashMap;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ProjectKorra;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.ability.MetalAbility;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.MovementHandler;
import com.projectkorra.projectkorra.util.ParticleEffect;

import methods_plugins.AmonPackPlugin;
@SuppressWarnings("deprecation")
public class SteelShackles extends MetalAbility implements AddonAbility {
	private Location origin = player.getLocation().clone().add(0, 1, 0);
	private Location location = origin.clone();
	private Vector dir = player.getLocation().getDirection();
	private CoreAbility SteelShackles = this;
	private int range = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Earth.Metal.SteelShackles.Range");
	private int hitbox = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Earth.Metal.SteelShackles.Hitbox");
	private int dmg1 = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Earth.Metal.SteelShackles.DamageFirst");
	private int dmg2 = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Earth.Metal.SteelShackles.DamageSecond");
	private int stunrange = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Earth.Metal.SteelShackles.StunRange");
	private int stunduration = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Earth.Metal.SteelShackles.StunDuration");
	private int cooldown = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Earth.Metal.SteelShackles.Cooldown");
	private int escapetime = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Earth.Metal.SteelShackles.TimeToEscape");
	private short durabilitynow = player.getInventory().getChestplate().getDurability();
	private int durabilitycost = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Earth.Metal.SteelShackles.DurabilityCost");
	public HashMap<String, Integer> taskID = new HashMap<String, Integer>();
	public SteelShackles(Player player) {
		super(player);
		if (bPlayer.isOnCooldown(this)) {
			return;
		}
		if (player.getInventory().getChestplate() == null)  {
            this.bPlayer.addCooldown(this);
			remove();
			return;
		}else 
			if (!(player.getInventory().getChestplate().getType() == Material.IRON_CHESTPLATE))  {
            this.bPlayer.addCooldown(this);
			remove();
			return;
		}else if (player.getInventory().getChestplate().getDurability() > 240)  {			
	            this.bPlayer.addCooldown(this);
				remove();
				return;
	    	} else
			start();
	}
	@Override
	public void progress() {
		bPlayer.addCooldown(this);
		player.getInventory().getChestplate().setDurability((short) ((short) durabilitynow + durabilitycost)); 
		if (player.isDead() || !player.isOnline()) {
			remove();
			return;
		}
		if (origin.distance(location) > range) {
					remove();
					return;
			}
		location.add(dir.multiply(1));
		location.add(dir.multiply(1));
		location.add(dir.multiply(1));
		displayLineBetweenEntities(location, player, range);
           		for (Entity entity : GeneralMethods.getEntitiesAroundPoint(location, hitbox)) {
        			if ((entity instanceof LivingEntity) && entity.getUniqueId() != player.getUniqueId()) {
        				DamageHandler.damageEntity(entity, dmg1, SteelShackles);
               			taskID.put("Tasknr1",Bukkit.getScheduler().scheduleSyncRepeatingTask(ProjectKorra.plugin, new Runnable() {
          	    		   public void run() {
          	    			   if (entity.isDead()) {
                					remove();
                 					return;
          	    			   } else
          	    				bPlayer.addCooldown(SteelShackles, cooldown);
         	    				if (player.getLocation().distance(entity.getLocation()) > 3) {
 	    						displayLineBetweenEntities(entity.getLocation(), player, range);
                       				Vector forceDir = GeneralMethods.getDirection(player.getLocation(), entity.getLocation().clone().add(0, 1, 0));
                           			player.setVelocity(forceDir.clone().normalize().multiply(1));
                           			entity.setVelocity(forceDir.clone().normalize().multiply(-1));
          	  			} else {
                   			player.setVelocity(new Vector(0,0,0));
                   			entity.setVelocity(new Vector(0,0,0));
                            taskID.put("Tasknr2",Bukkit.getScheduler().scheduleSyncRepeatingTask(ProjectKorra.plugin, new Runnable() {
               	    		   public void run() {
              	    			   if (entity.isDead()) {
                   					remove();
                    					return;
             	    			   } else
             	    				bPlayer.addCooldown(SteelShackles, cooldown);
              	    				if (player.getLocation().distance(entity.getLocation()) < stunrange) {
             	    					if (!entity.isDead()) {
             	    						displayLineBetweenEntities(entity.getLocation(), player, stunrange);
              	    					}	} else if (player.getLocation().distance(entity.getLocation()) > stunrange) {
                                Bukkit.getScheduler().cancelTask(taskID.get("Tasknr1"));
                                Bukkit.getScheduler().cancelTask(taskID.get("Tasknr1"));
              					remove();
              					return;
              	    					}
               	    		   }
                         		}, 5, 0));
                            Bukkit.getScheduler().cancelTask(taskID.get("Tasknr1"));
         					remove();
         					return;
          	  			}
          	    		   }
                    		}, 0, 0));
                  		new BukkitRunnable() {
                  			@Override
                  			public void run() {
           	    				if (!taskID.isEmpty()) {
                                    Bukkit.getScheduler().cancelTask(taskID.get("Tasknr2"));
           	    				}
          	    				if (player.getLocation().distance(entity.getLocation()) < stunrange) {
          	    					if (!entity.isDead()) {
          	    						ParticleEffect.CRIT.display(entity.getLocation(), 5, 2, 2, 2, 0);
                          	        	DamageHandler.damageEntity(entity, dmg2, SteelShackles);
                                    	MovementHandler mh = new MovementHandler((LivingEntity)entity, SteelShackles);
                                    	mh.stopWithDuration(stunduration,ChatColor.GREEN + "* GET MORGANED ES *");            	                                   			
                                    	}}}}.runTaskLater(ProjectKorra.plugin,escapetime);
                 		new BukkitRunnable() {
                 			@Override
                 			public void run() {
           	    				if (!taskID.isEmpty()) {
                                    Bukkit.getScheduler().cancelTask(taskID.get("Tasknr1"));
           	    				}
                 			}	
                 		}.runTaskLater(ProjectKorra.plugin,100);
  	    				bPlayer.addCooldown(SteelShackles, cooldown);
     					remove();
     					return;   				
                    } }
	        }	 
	public void displayLineBetweenEntities(Location source, Entity destination, int particleAmount) {
        if (!source.getWorld().equals(destination.getWorld()))
            return;
        double distance = source.clone().add(0,1,0).distance(destination.getLocation());
        Vector direction = destination.getLocation().clone().add(0,1,0).toVector().subtract(source.clone().add(0,1,0).toVector()).normalize().multiply(distance / particleAmount);
        Location particleLocation = source.clone().add(0,1,0);
        for (int i = 0; i < particleAmount; i++) {
        	
        	player.getWorld().spawnParticle(Particle.REDSTONE, particleLocation, 1, new Particle.DustOptions(Color.fromRGB(50, 50, 50), 1));
        	
				//ParticleEffect.CRIT.display(particleLocation, 3, 0.1, 0.1, 0.1, 0);
            particleLocation.add(direction);
        }
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
		return "SteelShackles";
	}
	@Override
	public String getDescription() {
		return "Pull your opponent towards you. After the collision, you will put shackles on him, which if the enemy does not move away from you within a few seconds, will injured him again and immobilized.";
	}
	@Override
	public String getInstructions() {
		return "Left Click to stretch your cables";
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
		return false;
	}
	@Override
	public void load() {}
	@Override
	public void stop() {
		super.remove();
	}
}
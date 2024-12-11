package abilities;

import java.util.HashMap;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ProjectKorra;
import com.projectkorra.projectkorra.ability.Ability;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.MetalAbility;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.ParticleEffect;
import com.projectkorra.projectkorra.util.TempBlock;

import methods_plugins.AmonPackPlugin;
@SuppressWarnings("deprecation")
public class MetalCompress extends MetalAbility implements AddonAbility {

	private long cooldown1 = AmonPackPlugin.plugin.getConfig().getLong("AmonPack.Earth.Metal.MetalCompress.CooldownMin");
	private long cooldown2 = AmonPackPlugin.plugin.getConfig().getLong("AmonPack.Earth.Metal.MetalCompress.CooldownMax");
    private double damage = AmonPackPlugin.plugin.getConfig().getDouble("AmonPack.Earth.Metal.MetalCompress.Damage");
    private long MaxChargeTime = AmonPackPlugin.plugin.getConfig().getLong("AmonPack.Earth.Metal.MetalCompress.MaxChargeTime");
    private long duration = AmonPackPlugin.plugin.getConfig().getLong("AmonPack.Earth.Metal.MetalCompress.Duration");
    private long durability1 = AmonPackPlugin.plugin.getConfig().getLong("AmonPack.Earth.Metal.MetalCompress.DurabilityCostMin");
    private long durability2 = AmonPackPlugin.plugin.getConfig().getLong("AmonPack.Earth.Metal.MetalCompress.DurabilityCostMax");
	private int abilityState;
	public HashMap<String, Integer> taskID = new HashMap<String, Integer>();
	private Ability MetalCompress = this;
    private FallingBlock fallingblock;
	private short durabilitynow = player.getInventory().getChestplate().getDurability();
	//private List<TempFallingBlock> temps = new ArrayList<TempFallingBlock>();
	private Location magnet;
	
	private ItemStack iron = new ItemStack(Material.IRON_INGOT, 1);
	public MetalCompress(Player player) {
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
	    	setField();
			start();
	}
	public void setField() {
		abilityState = 0;
	}
	@Override
	public void progress() {
		if (player.isDead() || !player.isOnline()) {
			bPlayer.addCooldown(this);
			remove();
			return;
		}
		if (abilityState == 0) {
			if (!player.isSneaking()) {
				remove();
				return; 
			} else if (System.currentTimeMillis() > getStartTime() + 100) {
				abilityState++;
			} else {
			}
		} else if (abilityState == 1) {
				if (System.currentTimeMillis() < getStartTime() + MaxChargeTime)  {
					if (!player.isSneaking()) {
						player.getInventory().getChestplate().setDurability((short) ((short) durabilitynow + durability1)); 
						shoot();
				 		} else {
						ParticleEffect.SMOKE_NORMAL.display(player.getLocation(), 5, 1, 1, 1, 0);
					} }
				else if (System.currentTimeMillis() > getStartTime() + (MaxChargeTime + 10))  {
					
				if (!player.isSneaking()) {
					player.getInventory().getChestplate().setDurability((short) (durabilitynow + durability2)); 
					maxcharge();
					} else {
  						ParticleEffect.BLOCK_CRACK.display(player.getLocation(), 5, 1, 1, 1, 0, Material.IRON_BLOCK.createBlockData());
				} }	
			} 
}
	
	public void shoot() { 
		mincharge();
		new BukkitRunnable() {
 			@Override
 			public void run() {
 				mincharge();	
 			}
 		}.runTaskLater(ProjectKorra.plugin,2);
		new BukkitRunnable() {
 			@Override
 			public void run() {
				mincharge();
 			}
 		}.runTaskLater(ProjectKorra.plugin,4);
		new BukkitRunnable() {
 			@Override
 			public void run() {
 				mincharge();	
 			}
 		}.runTaskLater(ProjectKorra.plugin,6);
	}
	
	public void mincharge() { 
		Vector direction = player.getLocation().clone().add(0,1,0).getDirection();
		Vector dir = direction.add(new Vector(Math.random() * 0.25D - 0.1D,0.10,Math.random()* 0.25D - 0.1D)).multiply(2);
		Item ii1 = player.getWorld().dropItemNaturally(player.getLocation().clone().add(0,1,0).getBlock().getRelative(GeneralMethods.getCardinalDirection(direction)).getLocation(), new ItemStack(iron));
		ii1.setPickupDelay(2147483647);
		ii1.setVelocity(dir);
		taskID.put("Rules",Bukkit.getScheduler().scheduleSyncRepeatingTask(ProjectKorra.plugin, new Runnable() {
  		   public void run() {
  			   if (ii1.isOnGround()) {
             			ii1.teleport(player.getLocation().clone().add(0,1000,0));
             			ii1.remove();
						remove();
						return;
             		}
               		for (Entity entity : GeneralMethods.getEntitiesAroundPoint(ii1.getLocation(), 2)) {
            			if ((entity instanceof LivingEntity) && entity.getUniqueId() != player.getUniqueId()) {
            				DamageHandler.damageEntity(entity, damage, MetalCompress);
  	               			ii1.teleport(player.getLocation().clone().add(0,1000,0));
  	               			ii1.remove();
                        } }
       		 } 
  		}, 1, 0));
		bPlayer.addCooldown(this);
			new BukkitRunnable() {
 			@Override
 			public void run() {
          		if (!ii1.isDead()) {
                    ii1.remove();
            		iron = null;
            		remove();
            		return;	
         		}
        		remove();
                 Bukkit.getScheduler().cancelTask(taskID.get("Rules"));
 			}
 		}.runTaskLater(ProjectKorra.plugin,80);
		remove();
		return;	
	}			

	public void maxcharge() { 
		Vector direction = player.getLocation().clone().add(0,1,0).getDirection();
		 Vector dir = direction.add(new Vector(0,0.5,0)).multiply(1);
		 
		 
	        this.fallingblock = player.getLocation().getWorld().spawnFallingBlock(player.getLocation().clone().add(0,1,0), Material.IRON_BLOCK.createBlockData());
	        this.fallingblock.setVelocity(dir);
	        this.fallingblock.setDropItem(false);
		 
		 
		 
		 
			taskID.put("Task1",Bukkit.getScheduler().scheduleSyncRepeatingTask(ProjectKorra.plugin, new Runnable() {
 		   public void run() {
 	            bPlayer.addCooldown(MetalCompress, cooldown2);
               if (fallingblock != null && !fallingblock.isDead()) {
        			magnet =fallingblock.getLocation().clone().add(0,-1,0);
           		if (magnet.getBlock().getType().isSolid()) {
           			fallingblock.remove();
                 			TempBlock tb1 = new TempBlock((magnet).getBlock(), Material.IRON_BLOCK);
                    		tb1.setRevertTime(duration*1000);	 
                      		for (Entity entity : GeneralMethods.getEntitiesAroundPoint(magnet, 10)) {
                       			if ((entity instanceof LivingEntity)) {
                           			Vector forceDir = GeneralMethods.getDirection(entity.getLocation(), magnet.clone().add(0, 1, 0));
                           			entity.setVelocity(forceDir.clone().normalize().multiply(2));	
                                   } }
                	    	taskID.put("Task2",Bukkit.getScheduler().scheduleSyncRepeatingTask(ProjectKorra.plugin, new Runnable() {
              	    		   public void run() {
              	  	            bPlayer.addCooldown(MetalCompress, cooldown2);
              	    			   if (!magnet.getBlock().getType().equals(Material.IRON_BLOCK)) {
              	    		        bPlayer.addCooldown(MetalCompress, cooldown2);
              	    				remove();
              	    				return;
              	    			   }
              						ParticleEffect.BLOCK_CRACK.display(magnet, 10, 6, 6, 6, 0, Material.IRON_BLOCK.createBlockData());
                            		for (Entity entity : GeneralMethods.getEntitiesAroundPoint(magnet, 12)) {
                            			if ((entity instanceof LivingEntity)) {
                            				if (!entity.isOnGround()) {
                            					entity.setVelocity(new Vector (0,-10,0));	
                            				}} 
                            		}}}, 0, 0));
                			new BukkitRunnable() {
                				@Override
                				public void run() {
                	                Bukkit.getScheduler().cancelTask(taskID.get("Task2"));
                				}
                			}.runTaskLater(ProjectKorra.plugin,duration*20);
                	}
               }
      		 } 
 		}, 0, 1));
        	bPlayer.addCooldown(this, cooldown2);
			new BukkitRunnable() {
			@Override
			public void run() {
                Bukkit.getScheduler().cancelTask(taskID.get("Task1"));
			}
		}.runTaskLater(ProjectKorra.plugin,duration*20);
		remove();
		return;
	}
	
	@Override
	public long getCooldown() {
		return cooldown1;
	}

	@Override
	public Location getLocation() {
		return null;
	}

	@Override
	public String getName() {
		return "MetalCompress";
	}
	
	@Override
	public String getDescription() {
		return "Tap shift to blast iron projectiles into your enemy, hold shift for longer to launch huge magnet, which will pull over and Ground them";
	}
	
	@Override
	public String getInstructions() {
		return "Hold shift to charge";
	}

	@Override
	public String getAuthor() {
		return "AmonPack";
	}

	@Override
	public String getVersion() {
		return "3.0";
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
		iron = null;
		super.remove();
	}

}
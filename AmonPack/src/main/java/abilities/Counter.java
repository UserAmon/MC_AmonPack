package abilities;
import methods_plugins.AmonPackPlugin;
import methods_plugins.Abilities.BladesAbility;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.util.ParticleEffect;


public class Counter extends BladesAbility implements AddonAbility {
	private int maxhold = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Chi.Blades.Counter.MaxHoldTime");
	private int dashpower = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Chi.Blades.Counter.EvadePower");
	private int Cooldown = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Chi.Blades.Counter.Cooldown");
	private boolean candashair = AmonPackPlugin.plugin.getConfig().getBoolean("AmonPack.Chi.Blades.Counter.DashInAir");
	@SuppressWarnings("deprecation")
	public Counter(Player player) {
		super(player);
		if (bPlayer.isOnCooldown(this)) {
			return;
		}
		start();
        if (player.getInventory().getItemInMainHand().isSimilar(BladesAbility.Sword1)) {
			return;
		} else
		player.getInventory().setItemInHand(Sword1);
	        
	}
	@SuppressWarnings("deprecation")
	@Override
	public void progress() {
		BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);
		if (bPlayer.isOnCooldown(this)) {
	        remove();
			return;
		}
		if (System.currentTimeMillis() < getStartTime() + maxhold) {
			if (player.isSneaking()) {
			ParticleEffect.CRIT_MAGIC.display(player.getLocation(), 10, 2, 2, 2, 0);
			ParticleEffect.CRIT.display(player.getLocation(), 10, 2, 2, 2, 0);
			}else if (!player.isSneaking()) {
			if (candashair == true) {
			Vector dir = player.getLocation().getDirection();
			player.setVelocity(dir.add(new Vector (0,0.5,0)).multiply(dashpower));
			}else if (candashair == false) {
			if (player.isOnGround()) {
			Vector dir = player.getLocation().getDirection();
			player.setVelocity(dir.add(new Vector (0,0.5,0)).multiply(dashpower));
			}}
			bPlayer.addCooldown(this);
	        player.getInventory().remove(BladesAbility.Sword1);
	        remove();
	    	return;
			}
		}else if (System.currentTimeMillis() > getStartTime() + maxhold) {
			bPlayer.addCooldown(this);
	        player.getInventory().remove(BladesAbility.Sword1);
	        remove();
	    	return;
		}
		if (!bPlayer.getBoundAbilityName().equalsIgnoreCase("Counter")) {
			bPlayer.addCooldown(this);
	        player.getInventory().remove(BladesAbility.Sword1);
	        remove();
	    	return;
		} else return;
		
		
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
		return "Counter";
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
		return false;
	}
	@Override
	public void load() {}
	@Override
	public void stop() {
		super.remove();
	}
}
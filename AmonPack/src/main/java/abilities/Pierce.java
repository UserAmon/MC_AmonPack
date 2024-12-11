package abilities;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.util.ParticleEffect;

import methods_plugins.AmonPackPlugin;
import methods_plugins.Abilities.BladesAbility;

public class Pierce extends BladesAbility implements AddonAbility {
	
	private static int spower = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Chi.Blades.Pierce.SpeedPower");
	private static long Cooldown = AmonPackPlugin.plugin.getConfig().getLong("AmonPack.Chi.Blades.Pierce.Cooldown");
	private static int sduration = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Chi.Blades.Pierce.SpeedDuration");
	private static int dmg1 = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Chi.Blades.Pierce.Dmg-1");
	private static int dmg2 = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Chi.Blades.Pierce.Dmg-2");
	private static long dashpower = AmonPackPlugin.plugin.getConfig().getLong("AmonPack.Chi.Blades.Pierce.DashPower");
	static int i;
	@SuppressWarnings("deprecation")
	public Pierce(Player player) {
		super(player);
		if (bPlayer.isOnCooldown(this)) {
			return;
		}
		start();
        if (player.getInventory().getItemInMainHand().isSimilar(BladesAbility.Sword1)) {
			return;
		} else
		player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, sduration, (spower-1)));
		i = 0;
		player.getInventory().setItemInHand(Sword1);
	        
	}
	@Override
	public void progress() {
		BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);
		if (bPlayer.isOnCooldown(this)) {
	        remove();
			return;
		}
		if (System.currentTimeMillis() > getStartTime() + 4000) {
			bPlayer.addCooldown(this);
	        player.getInventory().remove(BladesAbility.Sword1);
	        remove();
	    	return;
		}
		if (!bPlayer.getBoundAbilityName().equalsIgnoreCase("Pierce")) {
			bPlayer.addCooldown(this);
	        player.getInventory().remove(BladesAbility.Sword1);
	        remove();
	    	return;
		} else return;
	}	
	
	public static void skill(Player player, LivingEntity victim) {
		BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);
		
		if (i == 0) {
		victim.damage(dmg1);
		EntityDamageEvent event = new EntityDamageEvent(player, EntityDamageEvent.DamageCause.ENTITY_ATTACK, victim.getHealth());
		victim.setLastDamageCause(event);
 	    Vector dir = player.getLocation().getDirection();
    	player.setVelocity(dir.add(new Vector (0,0.5,0)).multiply(dashpower));
 	    ParticleEffect.CRIT_MAGIC.display(player.getLocation(), 15, 2, 2, 2, 0);
 	    ParticleEffect.CRIT.display(player.getLocation(), 15, 2, 2, 2, 0);
		i = 1;
		} else if (i == 1) {
		victim.damage(dmg2);
		EntityDamageEvent event = new EntityDamageEvent(player, EntityDamageEvent.DamageCause.ENTITY_ATTACK, victim.getHealth());
		victim.setLastDamageCause(event);
 	    Vector dir = player.getLocation().getDirection();
    	player.setVelocity(dir.add(new Vector (0,0.5,0)).multiply(dashpower));
 	    ParticleEffect.CRIT_MAGIC.display(player.getLocation(), 15, 2, 2, 2, 0);
 	    ParticleEffect.CRIT.display(player.getLocation(), 15, 2, 2, 2, 0);
		bPlayer.addCooldown("Pierce", Cooldown);
		i = 0;
		}  
		return;
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
		return "Pierce";
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
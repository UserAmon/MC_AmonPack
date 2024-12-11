package abilities;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.Vector;

import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.util.ParticleEffect;

import methods_plugins.AmonPackPlugin;
import methods_plugins.Abilities.BladesAbility;
import methods_plugins.Methods;

public class Stab extends BladesAbility implements AddonAbility {
	private static int uses = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Chi.Blades.Stab.Uses");
	private static int dmgl = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Chi.Blades.Stab.Dmg-Left");
	private static int dmgr = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Chi.Blades.Stab.Dmg-Right");
	private static long Cooldown = AmonPackPlugin.plugin.getConfig().getLong("AmonPack.Chi.Blades.Stab.Cooldown");
	private static int i;
	
	@SuppressWarnings("deprecation")
	public Stab(Player player) {
		super(player);
		if (bPlayer.isOnCooldown(this)) {
			return;
		}
		start();
        if (player.getInventory().getItemInMainHand().isSimilar(BladesAbility.Sword1)) {
			return;
		} else
		i = 0;
		player.getInventory().setItemInHand(Sword1);
	        
	}
	@Override
	public void progress() {
		BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);
		if (bPlayer.isOnCooldown(this)) {
			player.getInventory().remove(BladesAbility.Sword1);
	   		i = 0;
	        remove();
			return;
		}
		if (i > uses) {
			player.getInventory().remove(BladesAbility.Sword1);
	   		i = 0;
	   		bPlayer.addCooldown(this);
	        remove();
			return;
		}
		if (System.currentTimeMillis() > getStartTime() + 8000) {
			bPlayer.addCooldown(this);
	        player.getInventory().remove(BladesAbility.Sword1);
	   		i = 0;
	        remove();
	    	return;
		}
		if (!bPlayer.getBoundAbilityName().equalsIgnoreCase("Stab")) {
			bPlayer.addCooldown(this);
	        player.getInventory().remove(BladesAbility.Sword1);
	   		i = 0;
	        remove();
	    	return;
		} else return;
	}	
	
	public static void LpmSkill(Player player) {
		i++;
		Methods.Spin(player);
 	    ParticleEffect.CRIT_MAGIC.display(player.getLocation(), 15, 2, 2, 2, 0);
 	    ParticleEffect.CRIT.display(player.getLocation(), 15, 2, 2, 2, 0);
   		for (Entity entity : GeneralMethods.getEntitiesAroundPoint(player.getLocation(), 4)) {
			if (entity instanceof LivingEntity) {
			if (entity.getUniqueId() != null && !entity.isDead() && player.getUniqueId() != null) {
			if (entity.getUniqueId() != player.getUniqueId()) {
				LivingEntity victim = (LivingEntity) entity;
				victim.damage(dmgl);
				EntityDamageEvent event = new EntityDamageEvent(player, EntityDamageEvent.DamageCause.ENTITY_ATTACK, victim.getHealth());
				victim.setLastDamageCause(event);
			}}
			}else return;
			}
		return;
	    }
	@SuppressWarnings("deprecation")
	public static void PpmSkill(Player player) {
		if (player.isOnGround()) {
			i++;
		player.setVelocity(new Vector(0,1,0));
 	    ParticleEffect.CRIT_MAGIC.display(player.getLocation(), 15, 2, 2, 2, 0);
 	    ParticleEffect.CRIT.display(player.getLocation(), 15, 2, 2, 2, 0);
   		for (Entity entity : GeneralMethods.getEntitiesAroundPoint(player.getLocation(), 4)) {
			if (entity instanceof LivingEntity) {
			if (entity.getUniqueId() != null && !entity.isDead() && player.getUniqueId() != null) {
			if (entity.getUniqueId() != player.getUniqueId()) {
				LivingEntity victim = (LivingEntity) entity;
				victim.damage(2);
				entity.setVelocity(new Vector(0,1,0));
				EntityDamageEvent event = new EntityDamageEvent(player, EntityDamageEvent.DamageCause.ENTITY_ATTACK, victim.getHealth());
				victim.setLastDamageCause(event);
			}}
			}else return;
			}}else if (!player.isOnGround()) {
			for (Block b : GeneralMethods.getBlocksAroundPoint(player.getLocation(),5)) {
				if (!b.getType().isAir()){
					Vector dir = player.getLocation().getDirection();
					player.setVelocity(dir.multiply(1));
					ParticleEffect.CRIT_MAGIC.display(player.getLocation(), 15, 2, 2, 2, 0);
					ParticleEffect.CRIT.display(player.getLocation(), 15, 2, 2, 2, 0);
					for (Entity entity : GeneralMethods.getEntitiesAroundPoint(player.getLocation(), 4)) {
						if (entity instanceof LivingEntity) {
							if (entity.getUniqueId() != null && !entity.isDead() && player.getUniqueId() != null) {
								if (entity.getUniqueId() != player.getUniqueId()) {
									LivingEntity victim = (LivingEntity) entity;
									victim.damage(dmgr);
									EntityDamageEvent event = new EntityDamageEvent(player, EntityDamageEvent.DamageCause.ENTITY_ATTACK, victim.getHealth());
									victim.setLastDamageCause(event);
								}}
						}else return;
					}
					i++;
					break;
				}}}
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
		return "Stab";
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
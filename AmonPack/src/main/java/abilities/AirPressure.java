package abilities;
import methods_plugins.AmonPackPlugin;
import methods_plugins.Methods;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.AirAbility;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.ParticleEffect;


public class AirPressure extends AirAbility implements AddonAbility {
	
	private int Cooldown = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Air.AirPressure.Cooldown");
	private int dmg = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Air.AirPressure.Dmg");
	private int sphererange = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Air.AirPressure.Range-Sphere");
	private int pullrange = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Air.AirPressure.Range-Pull");
	private int pushpower = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Air.AirPressure.PushPower");
	private int mintime = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Air.AirPressure.MinHoldTime");
	private boolean cancontrol = AmonPackPlugin.plugin.getConfig().getBoolean("AmonPack.Air.AirPressure.CanControlSphere");
	private int maxtime = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Air.AirPressure.MaxHoldTime");
	
	public Location preloc;
	public int abilityState;
	public AirPressure(Player player) {
		super(player);
		if (bPlayer.isOnCooldown(this)) {
			return;
		}
		if (!bPlayer.canBend(this)) {
			return;
		}
		if (cancontrol == false) {
			preloc = Methods.getTargetLocation(player, sphererange).clone();
		}
		abilityState = 0;
		if (!player.isSneaking()) {
		start();
		}  
	}
	@Override
	public void progress() {
		if (cancontrol == true) {
			preloc = Methods.getTargetLocation(player, sphererange).clone();
		}
		if (bPlayer.isOnCooldown(this)) {
			remove();
		}
		if (bPlayer.getBoundAbility() == null || bPlayer.getBoundAbilityName() == null) {
			bPlayer.addCooldown(this);
			remove();
		}
		if (bPlayer.getBoundAbility() != null || bPlayer.getBoundAbilityName() != null) {
		if (!bPlayer.getBoundAbilityName().equalsIgnoreCase("AirPressure")) {
			bPlayer.addCooldown(this);
			remove();
		}
		if (player.isDead() || !player.isOnline()) {
		remove();
		return;
		}
		if (abilityState == 0) {
		if (player.isSneaking()) {
		if (System.currentTimeMillis() > getStartTime() + 300) {
			abilityState = 1;
		}else if (System.currentTimeMillis() < getStartTime() + mintime) {
		ParticleEffect.CLOUD.display(preloc, (pullrange*2), (pullrange/2), (pullrange/2), (pullrange), 0);
       	for (Entity entity : GeneralMethods.getEntitiesAroundPoint(preloc, pullrange)) {
		Vector forceDir = GeneralMethods.getDirection(entity.getLocation(), preloc);
		entity.setVelocity(forceDir.clone().normalize().multiply(0.5));
		}}}else if (!player.isSneaking()) {
		bPlayer.addCooldown(this);
		abilityState = 0;
		remove();
		}}else if (abilityState == 1){
			ParticleEffect.CLOUD.display(preloc, (pullrange*2), (pullrange/2), (pullrange/2), (pullrange/2), 0);
			ParticleEffect.SMOKE_NORMAL.display(player.getEyeLocation().add(player.getLocation().getDirection().clone().multiply(1)), 1, 0.3, 0.3, 0.3, 0);
       		for (Entity entity : GeneralMethods.getEntitiesAroundPoint(preloc, pullrange)) {
					Vector forceDir = GeneralMethods.getDirection(entity.getLocation(), preloc.clone().add(0,1,0));
					entity.setVelocity(forceDir.clone().normalize().multiply(0.5));
		}
		if (!player.isSneaking() || System.currentTimeMillis() > getStartTime() + maxtime) {
		abilityState = 0;
   		for (Entity entity : GeneralMethods.getEntitiesAroundPoint(preloc, pullrange)) {
				Vector forceDir = GeneralMethods.getDirection(entity.getLocation(), preloc.clone().subtract(0,2,0));
				entity.setVelocity(forceDir.clone().normalize().multiply(-pushpower));
    			if (entity.getUniqueId() != player.getUniqueId() && entity instanceof LivingEntity) {
				DamageHandler.damageEntity(entity, dmg, this);
    			}}
		bPlayer.addCooldown(this);
		remove();
		}}}else {
			bPlayer.addCooldown(this);
			remove();}
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
		return "AirPressure";
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
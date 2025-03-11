package abilities;

import abilities.Util_Objects.AbilityProjectile;
import abilities.Util_Objects.BetterParticles;
import abilities.Util_Objects.SmokeSource;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.ParticleEffect;
import methods_plugins.Abilities.SmokeAbility;
import methods_plugins.AmonPackPlugin;
import methods_plugins.Methods;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;


public class SmokeShot extends SmokeAbility implements AddonAbility {
	@Attribute("Cooldown")
	private long Cooldown = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Fire.Smoke.SmokeShot.Cooldown");
	@Attribute("Range")
	private long Range = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Fire.Smoke.SmokeShot.Range");
	private AbilityProjectile Projectiles;
	private Location origin;
    private long interval;
	private SmokeSource Source;
	public SmokeShot(Player player, boolean IsShift) {
		super(player);
				if (!this.bPlayer.isOnCooldown(getName()) && this.bPlayer.canBend(this)) {
					if(IsShift){
						SmokeSource source = SmokeAbility.UseSmokeSource(player,20);
						if(source!=null){
							SmokeAbility.MakeSourceReady(player,source);
							remove();
						}
					}else{
						if(SmokeAbility.ListOfReadySources.containsKey(player)){
							Source=SmokeAbility.ListOfReadySources.get(player);
							interval=0;
							origin = Source.getLocation().clone();
							List<BetterParticles> Particles = new ArrayList<>();
							Particles.add(new BetterParticles(8,ParticleEffect.SMOKE_NORMAL,0.1,0.05,0.1));
							Particles.add(new BetterParticles(8,ParticleEffect.CRIT,0.1,0.05,0.1));
							Particles.add(new BetterParticles(2,ParticleEffect.CAMPFIRE_COSY_SMOKE,0.1,0,0.1));
							Location to = Methods.getTargetLocation(player,25);
							Vector direction = to.toVector().subtract(Source.getLocation().toVector()).normalize();
							Location location = origin.clone().add(0,1,0);
							Projectiles =new AbilityProjectile(direction,location,origin,Particles,1);
							SmokeAbility.ListOfReadySources.remove(player);
							bPlayer.addCooldown(this);
							start();
						}
					}
		}}


	@Override
	public void progress() {
		if (player.isDead() || !player.isOnline()) {
			remove();
			return;
		}
		interval++;
		if(interval>1){
			interval=0;
				Location location = Projectiles.Advance().clone();
				Source.AdvanceLocation(location);
				if (location.distance(origin) > Range || !location.clone().add(0,0.8,0).getBlock().getType().isAir() || location.clone().add(0,0.8,0).getBlock().getType().isSolid()) {
					remove();
					return;
				}else{
					for (Entity entity : GeneralMethods.getEntitiesAroundPoint(location, 1)) {
						if ((entity instanceof LivingEntity) && entity.getUniqueId() != player.getUniqueId()) {
							DamageHandler.damageEntity(entity, 1, this);
						}

				}
			}
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
		return "SmokeShot";
	}
	@Override
	public String getDescription() {
		return "";
	}
	@Override
	public String getInstructions() {
		return "Hold Shift near EarthBendable blocks to charge, then relase it";
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
	public void load() {
	}
	@Override
	public void stop() {
		super.remove();
	}
}
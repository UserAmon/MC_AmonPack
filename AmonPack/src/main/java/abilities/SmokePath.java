package abilities;

import abilities.Util_Objects.AbilityProjectile;
import abilities.Util_Objects.BetterParticles;
import abilities.Util_Objects.SmokeSource;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.ParticleEffect;
import methods_plugins.Abilities.SmokeAbility;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class SmokePath extends SmokeAbility implements AddonAbility {
	/*@Attribute("Cooldown")
	private long Cooldown = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Earth.EarthHammer.Cooldown");
	@Attribute("Damage")
	private int dmg = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Earth.EarthHammer.Damage");
	@Attribute("ChargeTime")
	private long TimeToCharge = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Earth.EarthHammer.ChargeTime");
	private final long RevertTime = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Earth.EarthHammer.RevertTime");
	@Attribute("Range")
	private int range = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Earth.EarthHammer.Range");
	@Attribute("Radius")
	private int radius = AmonPackPlugin.plugin.getConfig().getInt("AmonPack.Earth.EarthHammer.Radius");*/
	private List<AbilityProjectile> Projectiles;
	private Location origin;
	private SmokeSource Source;
    private long interval;
	public SmokePath(Player player) {
		super(player);
				if (!this.bPlayer.isOnCooldown(getName()) && this.bPlayer.canBend(this)) {
					Projectiles=new ArrayList<>();
					interval=0;
					origin = player.getLocation().clone();
					List<BetterParticles> Particles = new ArrayList<>();
					Particles.add(new BetterParticles(3,ParticleEffect.SMOKE_NORMAL,1.5,0.03,0.1));
					Particles.add(new BetterParticles(1,ParticleEffect.CAMPFIRE_COSY_SMOKE,0.5,0.03,0.1));
					Location Projectile = player.getLocation().clone().add(0,0.5,0);
					Projectile.setPitch(0);
					Vector Dir = Projectile.clone().getDirection();
					Projectiles.add(new AbilityProjectile(Dir,Projectile,origin,Particles,1));
					bPlayer.addCooldown(this);
					Source = new SmokeSource(origin.clone().add(0,1.5,0), 120, 3, 1,player);

					start();
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
			for (AbilityProjectile Projectile : Projectiles) {
				Location location = Projectile.Advance().clone();
				Source.AdvanceLocation(location.clone().add(0,1,0));
				if (location.distance(origin) > 15 || !location.clone().add(0,0.8,0).getBlock().getType().isAir() || location.clone().add(0,0.8,0).getBlock().getType().isSolid()) {
					remove();
					return;
				}else{
					for (Entity entity : GeneralMethods.getEntitiesAroundPoint(location, 1)) {
						if ((entity instanceof LivingEntity) && entity.getUniqueId() != player.getUniqueId()) {
							DamageHandler.damageEntity(entity, 1, this);
						}
					}
					SmokeSource Source = new SmokeSource(location.clone().subtract(0,0.2,0), 60, 0.25, 2,player,true);
				}
			}
		}
	}

	@Override
	public long getCooldown() {
		return 5000;
	}
	@Override
	public Location getLocation() {
		return null;
	}
	@Override
	public String getName() {
		return "SmokePath";
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
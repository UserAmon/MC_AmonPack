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
import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class SmokeBarrage extends SmokeAbility implements AddonAbility {
	//@Attribute("Cooldown")
	private long Cooldown = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Fire.Smoke.SmokeBarrage.Cooldown");
	private double Gravity = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Fire.Smoke.SmokeBarrage.Gravity-Factor");
	private double progress = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Fire.Smoke.SmokeBarrage.Progress-Factor");
	private long range = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Fire.Smoke.SmokeBarrage.Range");
	private long damage = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Fire.Smoke.SmokeBarrage.Damage");
	private long projectiles = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Fire.Smoke.SmokeBarrage.Projectiles");

	private State AbilityState;
	private enum State {
		BENDABLE,
		CHARGING,
		READY,
		USED
	}
	private List<AbilityProjectile> Projectiles;
	private Location origin;
    private long interval;
	private boolean UseSmokeSource;
	private SmokeSource Source;
	private double Progress;
	public SmokeBarrage(Player player) {
		super(player);
			if (!player.isSneaking()) {
				if (!this.bPlayer.isOnCooldown(getName()) && this.bPlayer.canBend(this)) {
					SmokeSource source = SmokeAbility.UseSmokeSource(player,20);
					if(source!=null){
						UseSmokeSource=true;
						AbilityState = State.BENDABLE;
						Source=source;
						interval=0;
						Progress=0.75;
						Projectiles=new ArrayList<>();
						start();
					}
			}
		}
	}

	@Override
	public void progress() {
		if (player.isDead() || !player.isOnline()) {
			remove();
			return;
		}
		if (AbilityState == State.BENDABLE) {
			if (player.isSneaking()) {
				interval++;
				if(interval>=3) {
					interval = 0;
					if(Source.IsNearPlayer(player.getLocation(),1,player)){
						AbilityState = State.READY;
					}}
			}else{
				bPlayer.addCooldown(this);
				remove();
				return;
			}}
		if (AbilityState == State.READY) {
			if (player.isSneaking()) {
				ParticleEffect.SMOKE_NORMAL.display(player.getLocation(), 5, 0.3, 0.3, 0.3, 0.1);
			} else {
				AbilityState = State.USED;
				origin = player.getLocation().clone();
				List<BetterParticles> Particles = new ArrayList<>();
				Particles.add(new BetterParticles(8,ParticleEffect.SMOKE_NORMAL,0.2,0.05,0.1));
				Particles.add(new BetterParticles(8,ParticleEffect.CRIT,0.2,0.05,0.1));
				Particles.add(new BetterParticles(2,ParticleEffect.CAMPFIRE_COSY_SMOKE,0.1,0,0.1));
				if(UseSmokeSource){
					int offset = 0;
					for (int i = 0; i <= projectiles; i++) {
						offset+=5;
						Location MultiProjectile1 = player.getLocation().clone().add(0,0.6,0);
						Location MultiProjectile2 = player.getLocation().clone().add(0,0.6,0);
						Random r = new Random();
						MultiProjectile1.setYaw(MultiProjectile1.getYaw()-offset);
						MultiProjectile2.setYaw(MultiProjectile2.getYaw()+offset);
						Vector Dir1 = MultiProjectile1.clone().getDirection();
						Vector Dir2 = MultiProjectile2.clone().getDirection();
						Dir1.setY(Dir1.getY()+0.2+r.nextDouble());
						Dir2.setY(Dir2.getY()+0.2+r.nextDouble());
						Projectiles.add(new AbilityProjectile(Dir1,MultiProjectile1,origin,Particles,1));
						Projectiles.add(new AbilityProjectile(Dir2,MultiProjectile2,origin,Particles,1));
					}
					Vector Direction = origin.getDirection();
					Direction.setY(Direction.getY() + 0.5);
					Location location = origin.clone().add(0,1,0);
					Projectiles.add(new AbilityProjectile(Direction,location,origin,Particles,1));
				}else{
					Vector Direction = origin.getDirection();
					Direction.setY(Direction.getY() + 0.5);
					Location location = origin.clone().add(0,1,0);
					Projectiles.add(new AbilityProjectile(Direction,location,origin,Particles,1));
				}
				bPlayer.addCooldown(this);
			}}
		if (AbilityState == State.USED) {
			interval++;
			if(interval >= 2) {
				interval = 0;
				List<AbilityProjectile> ToDel = new ArrayList<>();
				for (AbilityProjectile Projectile : Projectiles) {
					double x = Projectile.getDirection().getX() * Progress;
					double y = Projectile.getDirection().getY() * Progress + 0.6 * Gravity * Progress * Progress;
					double z = Projectile.getDirection().getZ() * Progress;
					Location location = Projectile.Advance(x, y, z).clone();
					for (Entity entity : GeneralMethods.getEntitiesAroundPoint(location, 1)) {
						if ((entity instanceof LivingEntity) && entity.getUniqueId() != player.getUniqueId()) {
							DamageHandler.damageEntity(entity, damage, this);
						}}
					if (location.distance(origin) > range || !location.getBlock().getType().isAir() || location.getBlock().getType().isSolid()) {
						ToDel.add(Projectile);
					}}
				Projectiles.removeAll(ToDel);
				Progress+=progress;
			}
			if(Projectiles.isEmpty()){
				remove();
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
		return "SmokeBarrage";
	}
	@Override
	public String getDescription() {
		return "";
	}
	@Override
	public String getInstructions() {
		return "Hold Shift near SmokeSource to charge, then relase barrage of projetiles";
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
package Abilities.PK_Abilities.Earth;

import Plugin.AmonPackPlugin;
import Plugin.Methods;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.EarthAbility;
import com.projectkorra.projectkorra.ability.PlantAbility;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.TempBlock;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class EarthHammer extends EarthAbility implements AddonAbility {

	public static final java.util.HashMap<UUID, Long> chunkyHaste = new java.util.HashMap<>();

	private long cooldown;
	private double damage;
	private long chargeTime;
	private long revertTime;
	private double range;
	private double radius;
	private double speed = 1.0;

	private enum State {
		CHARGING,
		READY,
		SLAMMING
	}

	private State state;
	private long chargeStartTime;
	private Location origin;
	private Location projectile;
	private Vector direction;
	private final Set<UUID> hitEntities = new HashSet<>();
	private final List<TempBlock> tempBlocks = new ArrayList<>();
	private int travelledDistance = 0;

	public EarthHammer(Player player) {
		super(player);
		if (hasAbility(player, EarthHammer.class)) {
			return;
		}
		if (bPlayer.isOnCooldown(this) || !bPlayer.canBend(this)) {
			return;
		}

		loadConfig();

		RPG.Levels.BendingTree.PlayerBendingBranch branch = (AmonPackPlugin.levelsBending != null)
				? AmonPackPlugin.levelsBending.GetBranchByPlayerName(player.getName()) : null;
		boolean hasChunky = (branch != null && branch.hasUpgrade("Chunky"));
		if (hasChunky) {
			this.range += 1;
			this.radius += 1;
		}

		long now = System.currentTimeMillis();
		if (chunkyHaste.containsKey(player.getUniqueId()) && now - chunkyHaste.getOrDefault(player.getUniqueId(), 0L) < 10000) {
			chunkyHaste.remove(player.getUniqueId());
			this.speed = 1.6;
			this.state = State.READY;
			player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_THUNDER, 1.2f, 1.8f);
			start();
			return;
		}

		this.state = State.CHARGING;
		this.chargeStartTime = System.currentTimeMillis();

		player.getWorld().playSound(player.getLocation(), Sound.BLOCK_ROOTED_DIRT_BREAK, 0.8f, 0.7f);
		start();
	}

	public EarthHammer(Player player, int mode) {
		super(player);
		loadConfig();

		if (mode == 0) {
			if (!this.bPlayer.isOnCooldown("EarthHammerItem_Smash")) {
				Methods.spawnFallingBlocks(player.getLocation(), Material.DIRT, 6, 1.5, player);
				bPlayer.addCooldown("EarthHammerItem_Smash", 5000);
			}
			return;
		}
		if (mode == 1) {
			if (!this.bPlayer.isOnCooldown("EarthHammerItem")) {
				this.origin = player.getLocation().clone();
				this.origin.setPitch(0);
				this.direction = origin.getDirection().clone().setY(0).normalize();
				this.projectile = origin.clone();
				this.state = State.SLAMMING;
				bPlayer.addCooldown("EarthHammerItem", 5000);
				start();
			}
		}
	}

	private void loadConfig() {
		this.cooldown = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Earth.EarthHammer.Cooldown", 7000L);
		this.damage = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Earth.EarthHammer.Damage", 4.0);
		this.chargeTime = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Earth.EarthHammer.ChargeTime", 1000L);
		this.revertTime = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Earth.EarthHammer.RevertTime", 8000L);
		this.range = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Earth.EarthHammer.Range", 20.0);
		this.radius = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Earth.EarthHammer.Radius", 2.0);
	}

	@Override
	public void progress() {
		if (player == null || !player.isOnline() || player.isDead()) {
			remove();
			return;
		}

		if (state == State.CHARGING) {
			if (bPlayer.getBoundAbilityName() == null
					|| !bPlayer.getBoundAbilityName().equalsIgnoreCase(getName())) {
				remove();
				return;
			}

			if (player.isSneaking()) {
				long elapsed = System.currentTimeMillis() - chargeStartTime;
				Location pLoc = player.getLocation();
				pLoc.getWorld().spawnParticle(Particle.BLOCK, pLoc.clone().add(0, 0.2, 0), 4, 0.4, 0.1, 0.4, 0.05,
						Material.DIRT.createBlockData());

				if (elapsed >= chargeTime) {
					state = State.READY;
					player.getWorld().playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.6f, 1.4f);
				}
			} else {
				long elapsed = System.currentTimeMillis() - chargeStartTime;
				if (elapsed >= chargeTime) {
					launchHammerSlam();
				} else {
					remove();
				}
			}
		} else if (state == State.READY) {
			if (bPlayer.getBoundAbilityName() == null
					|| !bPlayer.getBoundAbilityName().equalsIgnoreCase(getName())) {
				remove();
				return;
			}

			if (player.isSneaking()) {
				Location pLoc = player.getLocation();
				pLoc.getWorld().spawnParticle(Particle.BLOCK, pLoc.clone().add(0, 0.3, 0), 6, 0.5, 0.2, 0.5, 0.1,
						Material.STONE.createBlockData());
				pLoc.getWorld().spawnParticle(Particle.CRIT, pLoc.clone().add(0, 0.8, 0), 2, 0.3, 0.3, 0.3, 0.05);
			} else {
				launchHammerSlam();
			}
		} else if (state == State.SLAMMING) {
			progressSlam();
		}
	}

	private void launchHammerSlam() {
		this.state = State.SLAMMING;
		this.origin = player.getLocation().clone();
		this.origin.setPitch(0);
		this.direction = origin.getDirection().clone().setY(0).normalize();
		this.projectile = origin.clone();
		this.travelledDistance = 0;

		bPlayer.addCooldown(this, cooldown);

		player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 1.0f, 0.7f);
		player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.3f);
	}

	private void progressSlam() {
		projectile.add(direction.clone().multiply(speed));
		travelledDistance += (int) Math.ceil(speed);

		// Ground hugging
		Block topBlock = GeneralMethods.getTopBlock(projectile, 4, 4);
		if (topBlock != null && topBlock.getY() > 0) {
			projectile.setY(topBlock.getY() + 1.0);
		}

		// Collision check with solid non-bendable walls
		Block front = projectile.getBlock();
		if (front.getType().isSolid() && !EarthAbility.isEarthbendable(player, front) && !PlantAbility.isPlant(front)) {
			explodeAndFinish();
			return;
		}

		Location ground = projectile.clone().subtract(0, 1.0, 0);
		Material mat = EarthAbility.isEarthbendable(player, ground.getBlock()) ? ground.getBlock().getType() : Material.DIRT;

		// Visual ground rupture wave
		projectile.getWorld().spawnParticle(Particle.BLOCK, projectile.clone().add(0, 0.2, 0), 12, 0.6, 0.2, 0.6, 0.1, mat.createBlockData());
		projectile.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, projectile.clone().add(0, 0.2, 0), 2, 0.3, 0.1, 0.3, 0.01);
		projectile.getWorld().playSound(projectile, Sound.BLOCK_STONE_BREAK, 0.8f, 0.9f);

		// Upheaval of earth blocks along the path
		for (Block b : GeneralMethods.getBlocksAroundPoint(projectile, radius)) {
			if (b.getY() <= projectile.getY() && EarthAbility.isEarthbendable(player, b)) {
				Block above = b.getRelative(0, 1, 0);
				if (above.getType().isAir() || PlantAbility.isPlant(above)) {
					if (Math.random() < 0.35) {
						TempBlock tb = new TempBlock(above, Material.STONE);
						tb.setRevertTime(revertTime);
						tempBlocks.add(tb);
					}
				}
			}
		}

		// Damage & knockback
		for (Entity entity : GeneralMethods.getEntitiesAroundPoint(projectile, radius + 1.0)) {
			if (entity instanceof LivingEntity target && !entity.getUniqueId().equals(player.getUniqueId()) && !hitEntities.contains(entity.getUniqueId())) {
				hitEntities.add(target.getUniqueId());
				DamageHandler.damageEntity(target, damage, this);
				Vector knock = direction.clone().multiply(0.9).setY(0.65);
				target.setVelocity(knock);
				target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 0.8f);
			}
		}

		if (travelledDistance >= range || projectile.distance(origin) >= range) {
			explodeAndFinish();
		}
	}

	private void explodeAndFinish() {
		Location burstLoc = projectile.clone().add(0, 0.5, 0);
		burstLoc.getWorld().spawnParticle(Particle.BLOCK, burstLoc, 30, 0.8, 0.6, 0.8, 0.15, Material.DIRT.createBlockData());
		burstLoc.getWorld().spawnParticle(Particle.BLOCK, burstLoc, 30, 0.8, 0.6, 0.8, 0.15, Material.STONE.createBlockData());
		burstLoc.getWorld().playSound(burstLoc, Sound.ENTITY_GENERIC_EXPLODE, 0.9f, 1.1f);
		remove();
	}

	@Override
	public long getCooldown() {
		return cooldown;
	}

	@Override
	public Location getLocation() {
		return projectile != null ? projectile : (player != null ? player.getLocation() : null);
	}

	@Override
	public String getName() {
		return "EarthHammer";
	}

	@Override
	public String getAuthor() {
		return "AmonPack";
	}

	@Override
	public String getVersion() {
		return "1.1";
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

	@Override
	public String getDescription() {
		return "Przytrzymaj SHIFT aby naładować potężny młot ziemi, a po puszczeniu wywołaj pędzącą falę uderzeniową, która wyrzuca wrogów w powietrze.";
	}

	@Override
	public String getInstructions() {
		return "Przytrzymaj SHIFT aby naładować, a następnie puść SHIFT aby wystrzelić młot ziemi.";
	}
}
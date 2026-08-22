package Abilities.PK_Abilities.Chi;

import Plugin.AmonPackPlugin;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.ChiAbility;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class Lasso extends ChiAbility implements AddonAbility {

    public enum State { CHARGING, FLYING, TETHERED }

    private State state;
    private long cooldown;
    private long chargeTime;
    private double range;
    private double speed;
    private double pullRadius;
    private double initialPullForce;
    private double strongPullForce;
    private double maxTetherBreakDistance;
    private long maxDurationMs;
    private double chiDrainPerSecond;

    private long startTime;
    private boolean fullyCharged = false;

    // Flying state
    private Location flyLocation;
    private Vector flyDirection;
    private double distanceTraveled = 0;

    // Tethered state
    private LivingEntity targetEntity;
    private long tetherStartTime;
    private long lastDrainTime;

    public Lasso(Player player) {
        super(player);

        if (hasAbility(player, Lasso.class)) {
            return;
        }
        if (bPlayer.isOnCooldown(this) || !bPlayer.canBend(this)) {
            return;
        }

        loadConfig();

        this.state = State.CHARGING;
        this.startTime = System.currentTimeMillis();

        start();
    }

    private void loadConfig() {
        this.cooldown = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Chi.Lasso.Cooldown", 8000L);
        this.chargeTime = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Chi.Lasso.ChargeTime", 1000L);
        this.range = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Chi.Lasso.Range", 20.0);
        this.speed = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Chi.Lasso.Speed", 1.8);
        this.pullRadius = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Chi.Lasso.PullRadius", 8.0);
        this.initialPullForce = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Chi.Lasso.InitialPullForce", 0.7);
        this.strongPullForce = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Chi.Lasso.StrongPullForce", 1.4);
        this.maxTetherBreakDistance = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Chi.Lasso.MaxTetherBreakDistance", 25.0);
        this.maxDurationMs = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Chi.Lasso.MaxDurationMs", 6000L);
        this.chiDrainPerSecond = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Chi.Lasso.ChiDrainPerSecond", 15.0);
    }

    @Override
    public void progress() {
        if (player == null || !player.isOnline() || player.isDead()) {
            remove();
            return;
        }

        if (state == State.CHARGING) {
            progressCharging();
        } else if (state == State.FLYING) {
            progressFlying();
        } else if (state == State.TETHERED) {
            progressTethered();
        }
    }

    private void progressCharging() {
        if (!player.isSneaking()) {
            // Player stopped sneaking
            if (fullyCharged) {
                launchLasso();
            } else {
                remove();
            }
            return;
        }

        long elapsed = System.currentTimeMillis() - startTime;
        Location eye = player.getEyeLocation();
        double angle = (System.currentTimeMillis() % 1000) / 1000.0 * 2 * Math.PI;

        double r = 0.6;
        double x = r * Math.cos(angle);
        double z = r * Math.sin(angle);
        Location ringLoc = eye.clone().add(0, 0.4, 0).add(x, 0, z);

        Particle.DustOptions ropeColor = new Particle.DustOptions(Color.fromRGB(190, 150, 90), 1.0f);
        player.getWorld().spawnParticle(Particle.DUST, ringLoc, 1, 0, 0, 0, 0, ropeColor);

        if (!fullyCharged) {
            double progress = Math.min(1.0, (double) elapsed / chargeTime);
            int filledBars = (int) (progress * 10);
            StringBuilder bar = new StringBuilder("§a");
            for (int i = 0; i < 10; i++) {
                if (i == filledBars) {
                    bar.append("§7");
                }
                bar.append("■");
            }
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                    TextComponent.fromLegacyText(String.format("§6➰ §lŁADOWANIE LASSA: [%s§r] §e%.0f%%", bar, progress * 100)));

            if (elapsed >= chargeTime) {
                fullyCharged = true;
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.8f);
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.8f, 1.6f);
            }
        } else {
            player.getWorld().spawnParticle(Particle.CRIT, ringLoc, 1, 0.02, 0.02, 0.02, 0.01);
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                    TextComponent.fromLegacyText("§6➰ §lLASSO NAŁADOWANE! §a[■■■■■■■■■■] §e(Puść Shift, aby wystrzelić)"));
        }
    }

    public void onSneakRelease() {
        if (state == State.CHARGING) {
            if (fullyCharged) {
                launchLasso();
            } else {
                remove();
            }
        }
    }

    private void launchLasso() {
        this.state = State.FLYING;
        this.flyLocation = player.getEyeLocation().clone();
        this.flyDirection = player.getEyeLocation().getDirection().normalize();
        this.distanceTraveled = 0;

        player.getWorld().playSound(flyLocation, Sound.ENTITY_ARROW_SHOOT, 1.0f, 0.9f);
        player.getWorld().playSound(flyLocation, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.4f);
    }

    private void progressFlying() {
        double step = speed;
        distanceTraveled += step;

        if (distanceTraveled > range) {
            finishWithCooldown();
            return;
        }

        Vector perp = new Vector(-flyDirection.getZ(), 0, flyDirection.getX()).normalize();
        if (perp.lengthSquared() < 0.01) {
            perp = new Vector(1, 0, 0);
        }

        // Advance and render undulating lasso trail
        for (double d = 0; d < step; d += 0.3) {
            flyLocation.add(flyDirection.clone().multiply(0.3));

            if (flyLocation.getBlock().getType().isSolid()) {
                flyLocation.getWorld().playSound(flyLocation, Sound.BLOCK_WOOL_STEP, 0.8f, 1.2f);
                finishWithCooldown();
                return;
            }

            double wave = Math.sin(distanceTraveled * 1.5) * 0.25;
            Location ropePoint = flyLocation.clone().add(perp.clone().multiply(wave));

            Particle.DustOptions ropeColor = new Particle.DustOptions(Color.fromRGB(190, 150, 90), 1.2f);
            flyLocation.getWorld().spawnParticle(Particle.DUST, ropePoint, 1, 0, 0, 0, 0, ropeColor);
        }

        // Check entity collision
        for (Entity e : GeneralMethods.getEntitiesAroundPoint(flyLocation, 1.5)) {
            if (e instanceof LivingEntity victim && e.getEntityId() != player.getEntityId()) {
                attachToTarget(victim);
                return;
            }
        }
    }

    private void attachToTarget(LivingEntity victim) {
        this.state = State.TETHERED;
        this.targetEntity = victim;
        this.tetherStartTime = System.currentTimeMillis();
        this.lastDrainTime = System.currentTimeMillis();

        // Initial light pull towards player
        Vector initPull = player.getLocation().toVector().subtract(victim.getLocation().toVector()).normalize().multiply(initialPullForce).setY(0.25);
        victim.setVelocity(initPull);

        victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_FISHING_BOBBER_RETRIEVE, 1.0f, 1.2f);
        victim.getWorld().playSound(victim.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1.0f, 1.4f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FISHING_BOBBER_RETRIEVE, 1.0f, 1.2f);
    }

    private void progressTethered() {
        if (targetEntity == null || targetEntity.isDead() || !targetEntity.isValid()) {
            finishWithCooldown();
            return;
        }

        long now = System.currentTimeMillis();
        long elapsed = now - tetherStartTime;

        if (elapsed >= maxDurationMs) {
            finishWithCooldown();
            return;
        }

        double currentDist = player.getLocation().distance(targetEntity.getLocation());

        if (currentDist > maxTetherBreakDistance) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                    TextComponent.fromLegacyText("§c✖ Cel oddalił się za daleko! Lasso pękło!"));
            targetEntity.getWorld().playSound(targetEntity.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.2f);
            finishWithCooldown();
            return;
        }

        // Drain Chi over time (per tick fraction)
        double deltaSec = Math.max(0.001, (now - lastDrainTime) / 1000.0);
        lastDrainTime = now;
        double chiToDrain = chiDrainPerSecond * deltaSec;

        if (!ChiManager.consumeChi(player, chiToDrain)) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                    TextComponent.fromLegacyText("§c✖ Brak Chi! Lasso pękło!"));
            targetEntity.getWorld().playSound(targetEntity.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.2f);
            finishWithCooldown();
            return;
        }

        // Render rope beam connecting player to target
        renderRopeBeam(player.getLocation().add(0, 1.0, 0), targetEntity.getLocation().add(0, 1.0, 0));

        // Distance constraint: if distance > pullRadius -> strongly pull towards player!
        if (currentDist > pullRadius) {
            Vector strongPull = player.getLocation().toVector().subtract(targetEntity.getLocation().toVector()).normalize().multiply(strongPullForce).setY(0.2);
            targetEntity.setVelocity(strongPull);

            targetEntity.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20, 2, false, false));
            targetEntity.getWorld().playSound(targetEntity.getLocation(), Sound.BLOCK_WOOL_STEP, 0.8f, 1.4f);
        }

        // Action bar update
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                TextComponent.fromLegacyText(String.format("§6➰ §eUwięziono: §f%s §7| Chi: §e%.0f",
                        targetEntity.getName(), ChiManager.getChi(player))));
    }

    private void renderRopeBeam(Location start, Location end) {
        double dist = start.distance(end);
        Vector dir = end.toVector().subtract(start.toVector()).normalize();
        Particle.DustOptions ropeColor = new Particle.DustOptions(Color.fromRGB(190, 150, 90), 1.1f);

        for (double d = 0; d < dist; d += 0.4) {
            Location pLoc = start.clone().add(dir.clone().multiply(d));
            // Slight catenary sag in the middle
            double midFactor = Math.sin((d / dist) * Math.PI);
            pLoc.add(0, -0.2 * midFactor, 0);

            start.getWorld().spawnParticle(Particle.DUST, pLoc, 1, 0, 0, 0, 0, ropeColor);
        }
    }

    private void finishWithCooldown() {
        if (bPlayer != null) {
            bPlayer.addCooldown(this, cooldown);
        }
        remove();
    }

    @Override
    public long getCooldown() {
        return cooldown;
    }

    @Override
    public Location getLocation() {
        return flyLocation != null ? flyLocation : (player != null ? player.getLocation() : null);
    }

    @Override
    public String getName() {
        return "Lasso";
    }

    @Override
    public boolean isSneakAbility() {
        return true;
    }

    @Override
    public boolean isHarmlessAbility() {
        return false;
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
    public void load() {
    }

    @Override
    public void stop() {
        remove();
    }

    @Override
    public String getDescription() {
		return "Wystrzeliwuje wijące się lasso z liny, które pęta wroga i przyciąga go gwałtownie, gdy próbuje uciec poza promień.";
	}

    @Override
    public String getInstructions() {
		return "Przytrzymaj SHIFT aby naładować lasso, a po puszczeniu wyceluj i wystrzel w przeciwnika.";
	}
}

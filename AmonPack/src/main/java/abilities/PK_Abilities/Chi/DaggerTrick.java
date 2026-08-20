package Abilities.PK_Abilities.Chi;

import Plugin.AmonPackPlugin;
import RPG.Levels.BendingTree.PlayerBendingBranch;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.ChiAbility;
import com.projectkorra.projectkorra.util.DamageHandler;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DaggerTrick extends ChiAbility implements AddonAbility {

    private long cooldown;
    private double backwardForce;
    private double upForce;
    private int maxArrowClicks;
    private int arrowCount;
    private double arrowDamage;
    private double arrowSpeed;
    private double angleBetweenArrows;
    private double chiCost;

    private int slot;
    private int clicksUsed = 0;
    private long lastClickTime = 0;
    private final List<Arrow> activeArrows = new ArrayList<>();
    private boolean launched = false;
    private boolean cooldownApplied = false;

    public DaggerTrick(Player player) {
        super(player);

        if (hasAbility(player, DaggerTrick.class)) {
            return;
        }
        if (bPlayer.isOnCooldown(this) || !bPlayer.canBend(this)) {
            return;
        }

        loadConfig();

        if (!ChiManager.consumeChi(player, chiCost)) {
            return;
        }

        this.slot = player.getInventory().getHeldItemSlot();

        PlayerBendingBranch branch = (AmonPackPlugin.levelsBending != null) ? AmonPackPlugin.levelsBending.GetBranchByPlayerName(player.getName()) : null;
        boolean hasForward = (branch != null && branch.hasUpgrade("DaggerTrickForward"));
        boolean hasMulti = (branch != null && branch.hasUpgrade("DaggerTrickMulti"));
        if (hasMulti) {
            this.maxArrowClicks = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Chi.DaggerTrick.Upgrades.Multi.MaxArrowClicks", 4);
        }

        performJump(hasForward);
        start();
    }

    private void loadConfig() {
        this.cooldown = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Chi.DaggerTrick.Cooldown", 5000L);
        this.backwardForce = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Chi.DaggerTrick.BackwardForce", 1.2);
        this.upForce = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Chi.DaggerTrick.UpForce", 0.6);
        this.maxArrowClicks = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Chi.DaggerTrick.MaxArrowClicks", 3);
        this.arrowCount = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Chi.DaggerTrick.ArrowCount", 3);
        this.arrowDamage = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Chi.DaggerTrick.ArrowDamage", 2.5);
        this.arrowSpeed = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Chi.DaggerTrick.ArrowSpeed", 2.0);
        this.angleBetweenArrows = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Chi.DaggerTrick.AngleBetweenArrows", 25.0);
        this.chiCost = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Chi.DaggerTrick.ChiCost", 40.0);
    }

    private void performJump(boolean forward) {
        Vector dir = player.getLocation().getDirection().setY(0).normalize();
        Vector vel = forward ? dir.multiply(backwardForce).setY(upForce) : dir.multiply(-backwardForce).setY(upForce);
        player.setVelocity(vel);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.4f);
        player.getWorld().spawnParticle(Particle.CRIT, player.getLocation(), 15, 0.3, 0.3, 0.3, 0.1);
        launched = true;
    }

    @Override
    public void progress() {
        if (player == null || !player.isOnline() || player.isDead()) {
            cleanUpArrows();
            remove();
            return;
        }

        // Tick flying arrows
        if (!activeArrows.isEmpty()) {
            Iterator<Arrow> it = activeArrows.iterator();
            while (it.hasNext()) {
                Arrow arrow = it.next();
                if (arrow == null || !arrow.isValid() || arrow.isDead() || arrow.isInBlock() || arrow.isOnGround()) {
                    if (arrow != null && arrow.isValid()) {
                        arrow.remove();
                    }
                    it.remove();
                    continue;
                }

                // Render particle trail
                arrow.getWorld().spawnParticle(Particle.CRIT, arrow.getLocation(), 2, 0.05, 0.05, 0.05, 0.02);
            }
        }

        // Check if player changed slot or grounded
        if (launched) {
            if (player.getInventory().getHeldItemSlot() != slot || isGrounded()) {
                applyCooldown();
                launched = false; // Jump ended, but fired arrows keep flying
            }
        }

        // If jump has ended and all arrows finished, remove ability instance
        if (!launched && activeArrows.isEmpty()) {
            remove();
        }
    }

    private boolean isGrounded() {
        Location loc = player.getLocation();
        return (player.getFallDistance() > 0.05 || player.getVelocity().getY() < 0)
                && (loc.clone().add(0, -0.1, 0).getBlock().getType().isSolid() || player.isOnGround());
    }

    public void onLeftClick() {
        if (!launched || clicksUsed >= maxArrowClicks) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastClickTime < 200) {
            return;
        }
        lastClickTime = now;
        clicksUsed++;

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0f, 1.4f);

        Location eye = player.getEyeLocation();
        Vector forward = eye.getDirection().normalize();

        int count = Math.max(1, arrowCount);
        double totalSpan = (count - 1) * angleBetweenArrows;
        double startAngle = -totalSpan / 2.0;

        for (int i = 0; i < count; i++) {
            double angleDeg = (count == 1) ? 0 : startAngle + (i * angleBetweenArrows);
            Vector dir = rotateY(forward.clone(), angleDeg).multiply(arrowSpeed);

            Arrow arrow = player.launchProjectile(Arrow.class, dir);
            arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
            arrow.setDamage(0.0); // No vanilla damage!
            arrow.setMetadata("DaggerTrickArrow", new FixedMetadataValue(AmonPackPlugin.plugin, arrowDamage));
            arrow.setMetadata("DaggerTrickCaster", new FixedMetadataValue(AmonPackPlugin.plugin, player.getUniqueId().toString()));

            activeArrows.add(arrow);
        }
    }

    public void handleArrowHit(Arrow arrow, Entity hitEntity) {
        if (arrow == null) return;
        activeArrows.remove(arrow);

        if (hitEntity instanceof LivingEntity victim && hitEntity.getEntityId() != player.getEntityId()) {
            DamageHandler.damageEntity(victim, arrowDamage, this);
            victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1.0f, 1.4f);
            victim.getWorld().spawnParticle(Particle.CRIT, victim.getLocation().add(0, 1, 0), 8, 0.2, 0.2, 0.2, 0.1);
        }

        arrow.remove();
    }

    private Vector rotateY(Vector vector, double angleDegrees) {
        double angle = Math.toRadians(angleDegrees);
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double x = vector.getX() * cos - vector.getZ() * sin;
        double z = vector.getX() * sin + vector.getZ() * cos;
        return new Vector(x, vector.getY(), z).normalize();
    }

    private void applyCooldown() {
        if (!cooldownApplied && bPlayer != null) {
            bPlayer.addCooldown(this, cooldown);
            cooldownApplied = true;
        }
    }

    private void cleanUpArrows() {
        for (Arrow arrow : activeArrows) {
            if (arrow != null && arrow.isValid()) {
                arrow.remove();
            }
        }
        activeArrows.clear();
    }

    @Override
    public void remove() {
        cleanUpArrows();
        super.remove();
    }

    @Override
    public long getCooldown() {
        return cooldown;
    }

    @Override
    public Location getLocation() {
        return player != null ? player.getLocation() : null;
    }

    @Override
    public String getName() {
        return "DaggerTrick";
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
        return "1.2";
    }

    @Override
    public void load() {
    }

    @Override
    public void stop() {
        cleanUpArrows();
        remove();
    }

    @Override
    public String getDescription() {
        return "Wyskakujesz w powietrze w tył (Shift). Podczas lotu klikaj LPM, aby wystrzeliwać lecące strzały zadające obrażenia od umiejętności.";
    }

    @Override
    public String getInstructions() {
        return "Kucnij (Shift), aby wyskoczyć do tyłu, a następnie klikaj LPM w powietrzu, aby strzelać!";
    }
}

package Abilities.PK_Abilities.Water;

import Plugin.AmonPackPlugin;
import RPG.Levels.BendingTree.PlayerBendingBranch;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.WaterAbility;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.TempBlock;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.*;

public class WaterWhip extends WaterAbility implements AddonAbility {

    private enum State {
        SOURCE_SELECTED, PULLING_SOURCE, STRIKING, RETRACTING, ACTIVE_HELD
    }

    private State state;
    private long cooldown;
    private double baseDamage;
    private double baseRange;
    private double knockback;
    private double sourceRange;
    private int slowDuration;
    private long selectWindow;
    private long extendDuration;

    private boolean hasLong;
    private boolean hasPull;
    private boolean hasExtend;

    private Block sourceBlock;
    private Location sourceLoc;
    private Location waterHeadLoc;
    private long selectStartTime;

    private double maxReach;
    private double currentExtendLength = 0;
    private int strikeTick = 0;
    private Vector strikeDir;
    private Set<UUID> hitEntities = new HashSet<>();
    private List<LivingEntity> pulledEntities = new ArrayList<>();

    private int activeHeldTicks = 0;
    private Vector currentHeldWhipDir = null;

    public WaterWhip(Player player) {
        super(player);

        if (hasAbility(player, WaterWhip.class)) {
            return;
        }
        if (bPlayer.isOnCooldown(this) || !bPlayer.canBend(this)) {
            return;
        }

        loadConfigAndUpgrades();

        Block water = WaterAbility.getWaterSourceBlock(player, sourceRange, true);
        if (water != null && (isWaterbendable(water) || isIcebendable(water))) {
            this.sourceBlock = water;
            this.sourceLoc = water.getLocation().add(0.5, 0.5, 0.5);
            this.waterHeadLoc = sourceLoc.clone();
            this.state = State.SOURCE_SELECTED;
            this.selectStartTime = System.currentTimeMillis();

            player.getWorld().playSound(sourceLoc, Sound.BLOCK_WATER_AMBIENT, 1.2f, 1.5f);
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText("§9[WaterWhip] §bŹródło wybrane! Kliknij LPM, aby uderzyć biczem!"));
            start();
        }
    }

    private void loadConfigAndUpgrades() {
        this.cooldown = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Water.WaterWhip.Cooldown", 6000L);
        this.baseDamage = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Water.WaterWhip.Damage", 4.5);
        this.baseRange = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Water.WaterWhip.Range", 12.0);
        this.knockback = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Water.WaterWhip.Knockback", 0.7);
        this.sourceRange = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Water.WaterWhip.SourceRange", 15.0);
        this.slowDuration = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Water.WaterWhip.SlowDuration", 40);
        this.selectWindow = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Water.WaterWhip.SelectWindow", 4000L);
        this.extendDuration = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Water.WaterWhip.ExtendDuration", 5000L);

        PlayerBendingBranch branch = (AmonPackPlugin.levelsBending != null) ? AmonPackPlugin.levelsBending.GetBranchByPlayerName(player.getName()) : null;
        this.hasLong = (branch != null && (branch.hasUpgrade("WaterWhipLong") || branch.hasUpgrade("WhipLong")));
        this.hasPull = (branch != null && (branch.hasUpgrade("WaterWhipPull") || branch.hasUpgrade("WhipPull")));
        this.hasExtend = (branch != null && (branch.hasUpgrade("WaterWhipExtend") || branch.hasUpgrade("WhipExtend")));

        this.maxReach = hasLong ? baseRange + 8.0 : baseRange;
    }

    public void onClick() {
        if (state == State.SOURCE_SELECTED) {
            state = State.PULLING_SOURCE;
            player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_1, 1.0f, 1.2f);
        }
    }

    @Override
    public void progress() {
        if (player == null || player.isDead() || !player.isOnline()) {
            remove();
            return;
        }

        switch (state) {
            case SOURCE_SELECTED:
                if (System.currentTimeMillis() - selectStartTime > selectWindow) {
                    remove();
                    return;
                }

                player.getWorld().spawnParticle(Particle.SPLASH, sourceLoc, 6, 0.25, 0.25, 0.25, 0.05);
                player.getWorld().spawnParticle(Particle.FALLING_WATER, sourceLoc, 2, 0.15, 0.15, 0.15, 0.02);
                break;

            case PULLING_SOURCE:
                Location hand = getHandLocation();
                Vector toHand = hand.toVector().subtract(waterHeadLoc.toVector());
                double dist = toHand.length();

                if (dist < 1.2) {
                    state = State.STRIKING;
                    strikeTick = 0;
                    currentExtendLength = 0;
                    strikeDir = player.getEyeLocation().getDirection().normalize();
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_SPLASH_HIGH_SPEED, 1.2f, 1.4f);
                } else {
                    waterHeadLoc.add(toHand.normalize().multiply(1.4));
                    renderWaterSegment(sourceLoc, waterHeadLoc, 6);
                }
                break;

            case STRIKING:
                strikeTick++;
                currentExtendLength += 1.5;
                renderWhipAnimation(currentExtendLength, true);

                if (currentExtendLength >= maxReach) {
                    state = State.RETRACTING;
                }
                break;

            case RETRACTING:
                currentExtendLength -= 1.8;
                renderWhipAnimation(Math.max(0, currentExtendLength), false);

                if (hasPull && !pulledEntities.isEmpty()) {
                    Location targetPullLoc = getHandLocation();
                    for (LivingEntity target : pulledEntities) {
                        if (target != null && target.isValid() && !target.isDead()) {
                            Vector pullVec = targetPullLoc.toVector().subtract(target.getLocation().toVector()).normalize().multiply(0.6).setY(0.2);
                            target.setVelocity(pullVec);
                        }
                    }
                }

                if (currentExtendLength <= 0.5) {
                    if (hasExtend) {
                        state = State.ACTIVE_HELD;
                        activeHeldTicks = 0;
                        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText("§9🌊 [WaterWhip] §bAktywny bicz w ręce!"));
                    } else {
                        bPlayer.addCooldown(this, cooldown);
                        remove();
                    }
                }
                break;

            case ACTIVE_HELD:
                activeHeldTicks++;
                if (activeHeldTicks * 50L >= extendDuration) {
                    bPlayer.addCooldown(this, cooldown);
                    remove();
                    return;
                }

                renderActiveHeldWhip();
                break;
        }
    }

    private void renderWhipAnimation(double length, boolean forward) {
        Location start = getHandLocation();
        Vector forwardDir = (strikeDir != null) ? strikeDir : player.getEyeLocation().getDirection().normalize();
        Vector right = new Vector(-forwardDir.getZ(), 0, forwardDir.getX()).normalize();
        if (right.lengthSquared() < 0.01) right = new Vector(1, 0, 0);

        int points = Math.max(3, (int) (length * 3.0));
        Location prevLoc = start.clone();

        for (int i = 1; i <= points; i++) {
            double distAlong = (i / (double) points) * length;
            double sinOffset = Math.sin(distAlong * 0.8 + strikeTick * 0.4) * 0.35 * (distAlong / length);
            Location pt = start.clone().add(forwardDir.clone().multiply(distAlong)).add(right.clone().multiply(sinOffset));

            start.getWorld().spawnParticle(Particle.SPLASH, pt, 2, 0.05, 0.05, 0.05, 0.02);
            start.getWorld().spawnParticle(Particle.FALLING_WATER, pt, 1, 0.02, 0.02, 0.02, 0.01);

            if (forward) {
                for (Entity entity : GeneralMethods.getEntitiesAroundPoint(pt, 1.2)) {
                    if (entity instanceof LivingEntity && !entity.getUniqueId().equals(player.getUniqueId())) {
                        LivingEntity target = (LivingEntity) entity;
                        if (!hitEntities.contains(target.getUniqueId())) {
                            hitEntities.add(target.getUniqueId());
                            DamageHandler.damageEntity(target, baseDamage, this);
                            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, slowDuration, 1));
                            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_SPLASH, 1.0f, 1.2f);

                            if (hasPull) {
                                pulledEntities.add(target);
                                Vector pullVec = getHandLocation().toVector().subtract(target.getLocation().toVector()).normalize().multiply(knockback).setY(0.25);
                                target.setVelocity(pullVec);
                            } else {
                                Vector knockVec = forwardDir.clone().multiply(knockback).setY(0.2);
                                target.setVelocity(knockVec);
                            }

                            if (!hasLong) {
                                state = State.RETRACTING;
                                return;
                            }
                        }
                    }
                }
            }
        }
    }

    private void renderActiveHeldWhip() {
        Location hand = getHandLocation();
        Vector targetLook = player.getEyeLocation().getDirection().normalize();

        if (currentHeldWhipDir == null) {
            currentHeldWhipDir = targetLook.clone();
        } else {
            currentHeldWhipDir.add(targetLook.clone().subtract(currentHeldWhipDir).multiply(0.12)).normalize();
        }

        Vector right = new Vector(-currentHeldWhipDir.getZ(), 0, currentHeldWhipDir.getX()).normalize();
        double heldLength = baseRange * 0.85;
        int points = (int) (heldLength * 3.0);

        for (int i = 1; i <= points; i++) {
            double distAlong = (i / (double) points) * heldLength;
            double wave = Math.sin(distAlong * 0.9 + activeHeldTicks * 0.3) * 0.3;
            Location pt = hand.clone().add(currentHeldWhipDir.clone().multiply(distAlong)).add(right.clone().multiply(wave));

            hand.getWorld().spawnParticle(Particle.SPLASH, pt, 2, 0.05, 0.05, 0.05, 0.02);

            for (Entity entity : GeneralMethods.getEntitiesAroundPoint(pt, 1.1)) {
                if (entity instanceof LivingEntity && !entity.getUniqueId().equals(player.getUniqueId())) {
                    LivingEntity target = (LivingEntity) entity;
                    DamageHandler.damageEntity(target, baseDamage * 0.3, this);
                    target.setVelocity(currentHeldWhipDir.clone().multiply(0.4).setY(0.15));
                }
            }
        }
    }

    private void renderWaterSegment(Location start, Location end, int steps) {
        Vector diff = end.toVector().subtract(start.toVector());
        for (int i = 0; i <= steps; i++) {
            Location p = start.clone().add(diff.clone().multiply((double) i / steps));
            p.getWorld().spawnParticle(Particle.SPLASH, p, 3, 0.1, 0.1, 0.1, 0.02);
            p.getWorld().spawnParticle(Particle.DRIPPING_WATER, p, 1, 0.05, 0.05, 0.05, 0.0);
        }
    }

    private Location getHandLocation() {
        Location eye = player.getEyeLocation();
        Vector right = new Vector(-eye.getDirection().getZ(), 0, eye.getDirection().getX()).normalize();
        return eye.clone().add(right.multiply(0.4)).add(0, -0.3, 0);
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
        return "WaterWhip";
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
    public String getAuthor() {
        return "AmonPack";
    }

    @Override
    public String getVersion() {
        return "2.0";
    }

    @Override
    public void load() {}

    @Override
    public void stop() {
        remove();
    }

	@Override
	public String getDescription() {
		return "Pobiera wodę ze źródła i tworzy elastyczny bicz wodny, którym można smagać i odrzucać wrogów.";
	}

	@Override
	public String getInstructions() {
		return "Kliknij LPM na wodę aby pobrać bicz, a następnie klikaj LPM aby uderzać.";
	}
}

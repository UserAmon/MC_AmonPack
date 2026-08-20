package Abilities.PK_Abilities.Chi;

import Plugin.AmonPackPlugin;
import RPG.Levels.BendingTree.PlayerBendingBranch;
import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.ChiAbility;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.util.DamageHandler;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class QuickPalms extends ChiAbility implements AddonAbility {

    public enum Mode { NONE, COUNTER_STANCE, TARGET_STRIKING }

    private Mode mode;
    private long cooldown;
    private double counterRange;
    private long stanceDuration;
    private double initialDamage;
    private double strikeDamage;
    private long chiBlockDuration;
    private double chiCost;

    private boolean hasReflex = false;
    private boolean hasTripleStrike = false;
    private boolean hasPressureBurst = false;

    private long startTime;
    private LivingEntity targetEntity;
    private final List<Vector> targetPointOffsets = new ArrayList<>();
    private final Random random = new Random();

    public QuickPalms(Player player) {
        this(player, false);
    }

    public QuickPalms(Player player, boolean isSneakTrigger) {
        super(player);

        if (hasAbility(player, QuickPalms.class)) {
            return;
        }
        if (bPlayer.isOnCooldown(this) || !bPlayer.canBend(this)) {
            return;
        }

        loadConfig();

        PlayerBendingBranch branch = (AmonPackPlugin.levelsBending != null) ? AmonPackPlugin.levelsBending.GetBranchByPlayerName(player.getName()) : null;
        if (branch != null) {
            hasReflex = branch.hasUpgrade("QuickPalmsReflex");
            hasTripleStrike = branch.hasUpgrade("QuickPalmsTripleStrike");
            hasPressureBurst = branch.hasUpgrade("QuickPalmsPressureBurst");
        }

        if (hasReflex) {
            this.stanceDuration = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Chi.QuickPalms.Upgrades.Reflex.StanceDurationMs", 7000L);
        }
        if (hasTripleStrike) {
            double bonusDmg = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Chi.QuickPalms.Upgrades.TripleStrike.BonusDamage", 2.0);
            this.strikeDamage += bonusDmg;
            this.chiBlockDuration = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Chi.QuickPalms.Upgrades.TripleStrike.ChiBlockDurationMs", 4500L);
        }

        if (isSneakTrigger) {
            // Shift activation: Counter Stance (requires enemy within counterRange)
            LivingEntity nearbyEnemy = findNearestEnemy(player, counterRange);
            if (nearbyEnemy == null) {
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                        TextComponent.fromLegacyText(String.format("§c✖ Brak przeciwników w zasięgu (%.0f bloków)!", counterRange)));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.7f, 0.6f);
                return;
            }

            if (!ChiManager.consumeChi(player, chiCost)) {
                return;
            }

            this.mode = Mode.COUNTER_STANCE;
            this.startTime = System.currentTimeMillis();
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.6f);
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.4f);
            start();
        } else {
            // Left click or direct melee trigger mode
            this.mode = Mode.NONE;
            this.startTime = System.currentTimeMillis();
            start();
        }
    }

    private void loadConfig() {
        this.cooldown = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Chi.QuickPalms.Cooldown", 8000L);
        this.counterRange = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Chi.QuickPalms.Range", 8.0);
        this.stanceDuration = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Chi.QuickPalms.StanceDuration", 5000L);
        this.initialDamage = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Chi.QuickPalms.InitialDamage", 2.5);
        this.strikeDamage = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Chi.QuickPalms.StrikeDamage", 3.0);
        this.chiBlockDuration = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Chi.QuickPalms.ChiBlockDuration", 3500L);
        this.chiCost = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Chi.QuickPalms.ChiCost", 40.0);
    }

    @Override
    public void progress() {
        if (player == null || !player.isOnline() || player.isDead()) {
            remove();
            return;
        }

        if (mode == Mode.COUNTER_STANCE) {
            long elapsed = System.currentTimeMillis() - startTime;
            // Allow 300ms grace period so sneak state is fully synced
            if (elapsed >= stanceDuration || (elapsed > 300L && !player.isSneaking())) {
                finishWithCooldown();
                return;
            }

            // Check if any enemy is still within counter range
            LivingEntity enemy = findNearestEnemy(player, counterRange);
            if (enemy == null) {
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                        TextComponent.fromLegacyText("§c[QuickPalms] Wszyscy przeciwnicy opuścili zasięg kontry!"));
                finishWithCooldown();
                return;
            }

            // Render rotating vertical shield ring in front of player
            renderRotatingShield();

            player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                    TextComponent.fromLegacyText("§b✦ §lQUICK PALMS: STAN KONTRY §b✦ §7(Trzymaj Shift)"));

        } else if (mode == Mode.TARGET_STRIKING) {
            if (targetEntity == null || targetEntity.isDead() || !targetEntity.isValid()) {
                finishWithCooldown();
                return;
            }

            // 8 seconds window to strike vital points
            if (System.currentTimeMillis() - startTime > 8000L) {
                finishWithCooldown();
                return;
            }

            // Render vital points ONLY to the caster player
            renderVitalPointsToCaster();

            player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                    TextComponent.fromLegacyText(String.format("§e✦ Pozostało punktów witalnych: §a%d §e✦", targetPointOffsets.size())));
        } else if (mode == Mode.NONE) {
            // If LPM wasn't used within 3 seconds of initialization, cancel
            if (System.currentTimeMillis() - startTime > 3000L) {
                remove();
            }
        }
    }

    private void renderRotatingShield() {
        Location eye = player.getEyeLocation();
        Vector forward = eye.getDirection().normalize();
        Location center = eye.clone().add(forward.clone().multiply(1.6)).add(0, -0.2, 0);

        double time = (System.currentTimeMillis() % 10000) / 150.0;
        int points = 8;
        double radius = 0.55;

        Vector right = new Vector(-forward.getZ(), 0, forward.getX()).normalize();
        if (right.lengthSquared() < 0.01) {
            right = new Vector(1, 0, 0);
        }
        Vector up = right.clone().crossProduct(forward).normalize();

        Particle.DustOptions outerColor = new Particle.DustOptions(Color.fromRGB(130, 220, 255), 0.75f);

        for (int i = 0; i < points; i++) {
            double angle = time + (i * 2 * Math.PI / points);
            double x = radius * Math.cos(angle);
            double y = radius * Math.sin(angle);

            Location pLoc = center.clone().add(right.clone().multiply(x)).add(up.clone().multiply(y));
            player.getWorld().spawnParticle(Particle.DUST, pLoc, 1, 0, 0, 0, 0, outerColor);
        }
    }

    private void renderVitalPointsToCaster() {
        if (targetEntity == null) return;
        Location vLoc = targetEntity.getLocation();

        Particle.DustOptions ptDust = new Particle.DustOptions(Color.fromRGB(255, 50, 50), 0.9f);

        for (Vector offset : targetPointOffsets) {
            Location pt = vLoc.clone().add(offset);
            player.spawnParticle(Particle.DUST, pt, 1, 0, 0, 0, 0, ptDust);
        }
    }

    public boolean onDamageReceived(Entity attackerEntity) {
        if (mode != Mode.COUNTER_STANCE) {
            return false;
        }

        if (attackerEntity == null) {
            return false;
        }

        double distSq = attackerEntity.getLocation().distanceSquared(player.getLocation());
        if (distSq > counterRange * counterRange) {
            // Attacker is outside counter radius - damage is received normally
            return false;
        }

        // Teleport behind attacker's back
        Location attackerLoc = attackerEntity.getLocation();
        Vector backDir = attackerLoc.getDirection().setY(0).normalize().multiply(-1.5);
        Location behind = attackerLoc.clone().add(backDir);
        behind.setY(attackerLoc.getY());

        // Point camera directly at attacker
        Vector dirToAttacker = attackerEntity.getLocation().add(0, 1.4, 0).toVector().subtract(behind.clone().add(0, 1.6, 0).toVector()).normalize();
        behind.setDirection(dirToAttacker);

        player.teleport(behind);

        if (hasReflex) {
            double restoreAmount = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Chi.QuickPalms.Upgrades.Reflex.ChiRestore", 30.0);
            ChiManager.addChi(player, restoreAmount);
        }

        // Sound & particles
        player.getWorld().playSound(behind, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.5f);
        player.getWorld().playSound(behind, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.2f, 1.8f);
        player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, behind.clone().add(0, 1.0, 0), 2, 0.2, 0.2, 0.2, 0);

        player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                TextComponent.fromLegacyText("§a✔ §lSKUTECZNA KONTRA! §7Przeniesiono za plecy wroga!"));

        finishWithCooldown();
        return true; // Damage blocked
    }

    public boolean isCounterStanceActive() {
        return mode == Mode.COUNTER_STANCE;
    }

    public boolean handleCounterDamage(Entity damager) {
        return onDamageReceived(damager);
    }

    public void onHitEntity(LivingEntity victim) {
        if (mode == Mode.NONE) {
            if (!ChiManager.consumeChi(player, chiCost)) {
                remove();
                return;
            }

            // First hit - initialize striking mode & generate vital points
            this.mode = Mode.TARGET_STRIKING;
            this.targetEntity = victim;
            this.startTime = System.currentTimeMillis();

            DamageHandler.damageEntity(victim, initialDamage, this);

            targetPointOffsets.clear();
            int totalPoints = hasTripleStrike ? AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Chi.QuickPalms.Upgrades.TripleStrike.VitalPoints", 3) : 2;
            for (int i = 0; i < totalPoints; i++) {
                Vector pt = generateRandomOffset();
                targetPointOffsets.add(pt);
            }

            player.getWorld().playSound(victim.getLocation(), Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 1.0f, 1.5f);
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                    TextComponent.fromLegacyText(String.format("§e✦ Traf w %d odsłonięte punkty witalne na ciele wroga!", totalPoints)));

        } else if (mode == Mode.TARGET_STRIKING && victim.getEntityId() == targetEntity.getEntityId()) {
            // Check if player struck close to one of the vital points
            Location eye = player.getEyeLocation();
            Vector lookDir = eye.getDirection().normalize();

            int hitIndex = -1;

            for (int i = 0; i < targetPointOffsets.size(); i++) {
                Location ptWorld = victim.getLocation().clone().add(targetPointOffsets.get(i));
                Vector toPt = ptWorld.toVector().subtract(eye.toVector()).normalize();
                double dot = lookDir.dot(toPt);
                if (dot > 0.85 || eye.distanceSquared(ptWorld) < 4.0) {
                    hitIndex = i;
                    break;
                }
            }

            if (hitIndex == -1 && !targetPointOffsets.isEmpty()) {
                hitIndex = 0;
            }

            if (hitIndex != -1) {
                targetPointOffsets.remove(hitIndex);

                DamageHandler.damageEntity(victim, strikeDamage, this);

                // Apply Chi Blocking on strike
                if (victim instanceof Player targetPlayer) {
                    BendingPlayer bTarget = BendingPlayer.getBendingPlayer(targetPlayer);
                    if (bTarget != null) {
                        bTarget.blockChi();
                    }
                }

                victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.2f, 1.6f);
                victim.getWorld().spawnParticle(Particle.CRIT, victim.getLocation().add(0, 1.2, 0), 10, 0.2, 0.2, 0.2, 0.1);

                if (targetPointOffsets.isEmpty()) {
                    if (hasPressureBurst) {
                        double burstDmg = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Chi.QuickPalms.Upgrades.PressureBurst.Damage", 8.0);
                        double burstKb = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Chi.QuickPalms.Upgrades.PressureBurst.Knockback", 1.4);
                        long paralyzeMs = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Chi.QuickPalms.Upgrades.PressureBurst.ParalyzeDurationMs", 2000L);

                        RPG.Levels.BendingTree.PlayerBendingBranch pBranch = (AmonPackPlugin.levelsBending != null) ? AmonPackPlugin.levelsBending.GetBranchByPlayerName(player.getName()) : null;
                        if (pBranch != null && pBranch.hasUpgrade("PressureMaster")) {
                            double pMult = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Chi.Passives.PressureMaster.DurationMultiplier", 1.25);
                            paralyzeMs = (long) (paralyzeMs * pMult);
                        }

                        victim.getWorld().spawnParticle(Particle.EXPLOSION, victim.getLocation().add(0, 1.0, 0), 1, 0, 0, 0, 0);
                        victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.4f);
                        DamageHandler.damageEntity(victim, burstDmg, this);
                        Vector kb = victim.getLocation().toVector().subtract(player.getLocation().toVector()).normalize().multiply(burstKb).setY(0.35);
                        victim.setVelocity(kb);
                        ChiManager.paralyzeEntity(victim, paralyzeMs);
                    }

                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                            TextComponent.fromLegacyText("§a✔ §lTRAFIONO WSZYSTKIE PUNKTY WITALNE! §eChi zablokowane!"));
                    finishWithCooldown();
                }
            }
        }
    }

    private Vector generateRandomOffset() {
        double ox = (random.nextDouble() - 0.5) * 0.7;
        double oy = 0.4 + random.nextDouble() * 1.0;
        double oz = (random.nextDouble() - 0.5) * 0.7;
        return new Vector(ox, oy, oz);
    }

    private LivingEntity findNearestEnemy(Player player, double radius) {
        LivingEntity nearest = null;
        double minDistSq = radius * radius;
        Location pLoc = player.getLocation();

        for (Entity e : GeneralMethods.getEntitiesAroundPoint(pLoc, radius)) {
            if (e instanceof LivingEntity le && e.getEntityId() != player.getEntityId()) {
                double distSq = le.getLocation().distanceSquared(pLoc);
                if (distSq < minDistSq) {
                    minDistSq = distSq;
                    nearest = le;
                }
            }
        }
        return nearest;
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
        return player != null ? player.getLocation() : null;
    }

    @Override
    public String getName() {
        return "QuickPalms";
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
        return "Podwójny styl walki: Shift aktywuje stan kontry (teleportacja za plecy atakującego i zablokowanie ciosu). LPM zadaje cios i odsłania punkty witalne na ciele wroga, w które kolejne ciosy blokują Chi!";
    }

    @Override
    public String getInstructions() {
        return "Shift w pobliżu wroga = Kontra | LPM = Uderzenie i odsłonięcie punktów witalnych!";
    }
}

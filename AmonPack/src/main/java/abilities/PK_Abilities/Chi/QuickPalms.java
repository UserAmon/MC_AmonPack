package Abilities.PK_Abilities.Chi;

import Plugin.AmonPackPlugin;
import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.ChiAbility;
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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
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

    private long startTime;
    private LivingEntity targetEntity;
    private final List<Vector> targetPointOffsets = new ArrayList<>();
    private final Random random = new Random();

    public QuickPalms(Player player) {
        super(player);

        if (hasAbility(player, QuickPalms.class)) {
            return;
        }
        if (bPlayer.isOnCooldown(this) || !bPlayer.canBend(this)) {
            return;
        }

        loadConfig();

        if (player.isSneaking()) {
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
            if (elapsed >= stanceDuration || !player.isSneaking()) {
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
        } else if (mode == Mode.NONE) {
            // Waiting for first hit (or expires in 2s if no attack happened)
            if (System.currentTimeMillis() - startTime > 2000L) {
                remove();
            }
        }
    }

    private void renderRotatingShield() {
        Location eye = player.getEyeLocation();
        Vector forward = eye.getDirection().normalize();
        Location center = eye.clone().add(forward.clone().multiply(1.2));

        double time = (System.currentTimeMillis() % 10000) / 120.0;
        int points = 12;
        double radius = 0.65;

        Vector right = new Vector(-forward.getZ(), 0, forward.getX()).normalize();
        if (right.lengthSquared() < 0.01) {
            right = new Vector(1, 0, 0);
        }
        Vector up = right.clone().crossProduct(forward).normalize();

        Particle.DustOptions outerColor = new Particle.DustOptions(Color.fromRGB(150, 230, 255), 0.85f);
        Particle.DustOptions innerColor = new Particle.DustOptions(Color.fromRGB(240, 250, 255), 0.65f);

        for (int i = 0; i < points; i++) {
            double angle = time + (i * 2 * Math.PI / points);
            double x = radius * Math.cos(angle);
            double y = radius * Math.sin(angle);

            Location pLoc = center.clone().add(right.clone().multiply(x)).add(up.clone().multiply(y));
            player.getWorld().spawnParticle(Particle.DUST, pLoc, 1, 0, 0, 0, 0, outerColor);
        }

        // Inner shield core
        for (int i = 0; i < 4; i++) {
            double angle = -time * 1.5 + (i * Math.PI / 2);
            double x = (radius * 0.4) * Math.cos(angle);
            double y = (radius * 0.4) * Math.sin(angle);
            Location pLoc = center.clone().add(right.clone().multiply(x)).add(up.clone().multiply(y));
            player.getWorld().spawnParticle(Particle.DUST, pLoc, 1, 0, 0, 0, 0, innerColor);
        }
    }

    private void renderVitalPointsToCaster() {
        if (targetEntity == null) return;
        Location vLoc = targetEntity.getLocation();

        Particle.DustOptions ptDust = new Particle.DustOptions(Color.fromRGB(255, 60, 60), 1.2f);

        for (Vector offset : targetPointOffsets) {
            Location pt = vLoc.clone().add(offset);
            player.spawnParticle(Particle.DUST, pt, 2, 0.04, 0.04, 0.04, 0, ptDust);
            player.spawnParticle(Particle.CRIT, pt, 1, 0.02, 0.02, 0.02, 0.01);
        }
    }

    public boolean isCounterStanceActive() {
        return mode == Mode.COUNTER_STANCE;
    }

    public boolean handleCounterDamage(Entity damager) {
        if (mode != Mode.COUNTER_STANCE || !(damager instanceof LivingEntity attackerEntity)) {
            return false;
        }

        // Check if attacker is within counter range
        double distSq = player.getLocation().distanceSquared(attackerEntity.getLocation());
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
        Vector dirToAttacker = attackerEntity.getEyeLocation().toVector().subtract(behind.clone().add(0, 1.6, 0).toVector()).normalize();
        behind.setDirection(dirToAttacker);

        player.teleport(behind);

        // Sound & particles
        player.getWorld().playSound(behind, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.5f);
        player.getWorld().playSound(behind, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.2f, 1.8f);
        player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, behind.clone().add(0, 1.0, 0), 2, 0.2, 0.2, 0.2, 0);

        player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                TextComponent.fromLegacyText("§a✔ §lSKUTECZNA KONTRA! §7Przeniesiono za plecy wroga!"));

        finishWithCooldown();
        return true; // Damage blocked
    }

    public void onHitEntity(LivingEntity victim) {
        if (mode == Mode.NONE) {
            if (!ChiManager.consumeChi(player, chiCost)) {
                remove();
                return;
            }

            // First hit - initialize striking mode & generate 2 distant vital points
            this.mode = Mode.TARGET_STRIKING;
            this.targetEntity = victim;
            this.startTime = System.currentTimeMillis();

            DamageHandler.damageEntity(victim, initialDamage, this);

            // Generate 2 points on body separated by minimum distance of 0.6 blocks
            targetPointOffsets.clear();
            Vector pt1 = generateRandomOffset();
            Vector pt2;
            int attempts = 0;
            do {
                pt2 = generateRandomOffset();
                attempts++;
            } while (pt1.distance(pt2) < 0.6 && attempts < 30);

            targetPointOffsets.add(pt1);
            targetPointOffsets.add(pt2);

            player.getWorld().playSound(victim.getLocation(), Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 1.0f, 1.5f);
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                    TextComponent.fromLegacyText("§e✦ Traf w 2 odsłonięte punkty witalne na ciele wroga!"));

        } else if (mode == Mode.TARGET_STRIKING && victim.getEntityId() == targetEntity.getEntityId()) {
            // Check if player struck close to one of the vital points
            Location eye = player.getEyeLocation();
            Vector lookDir = eye.getDirection().normalize();

            int hitIndex = -1;
            double bestAngle = 0.5; // close aiming

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
                // Default to closest remaining point on hit
                hitIndex = 0;
            }

            if (hitIndex != -1) {
                targetPointOffsets.remove(hitIndex);

                DamageHandler.damageEntity(victim, strikeDamage, this);
                applyChiBlockAndSlow(victim);

                player.playSound(victim.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
                player.playSound(victim.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 1.4f);

                if (targetPointOffsets.isEmpty()) {
                    // Both 2 points struck! Combo finish!
                    Vector kb = victim.getLocation().toVector().subtract(player.getLocation().toVector()).normalize().multiply(0.8).setY(0.3);
                    victim.setVelocity(kb);

                    player.getWorld().playSound(victim.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.7f, 1.6f);
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                            TextComponent.fromLegacyText("§a✔ §lZABLOKOWANO CHI I ZNISZCZONO WSZYSTKIE PUNKTY!"));

                    finishWithCooldown();
                } else {
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                            TextComponent.fromLegacyText("§6✦ Zniszczono 1/2 punktów! Pozostał 1 punkt!"));
                }
            }
        }
    }

    private Vector generateRandomOffset() {
        double offX = (random.nextDouble() - 0.5) * 0.7;
        double offY = 0.35 + (random.nextDouble() * 1.1); // between 0.35m and 1.45m height
        double offZ = (random.nextDouble() - 0.5) * 0.7;
        return new Vector(offX, offY, offZ);
    }

    private void applyChiBlockAndSlow(LivingEntity victim) {
        victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1, false, false));

        if (victim instanceof Player targetPlayer) {
            BendingPlayer targetBPlayer = BendingPlayer.getBendingPlayer(targetPlayer);
            if (targetBPlayer != null) {
                targetBPlayer.blockChi();
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (targetBPlayer.isChiBlocked()) {
                            targetBPlayer.unblockChi();
                        }
                    }
                }.runTaskLater(AmonPackPlugin.plugin, Math.max(20L, chiBlockDuration / 50L));
            }
        }
    }

    private LivingEntity findNearestEnemy(Player player, double range) {
        LivingEntity nearest = null;
        double bestDist = range * range;
        for (Entity e : GeneralMethods.getEntitiesAroundPoint(player.getLocation(), range)) {
            if (e instanceof LivingEntity le && e.getEntityId() != player.getEntityId()) {
                double dist = player.getLocation().distanceSquared(le.getLocation());
                if (dist < bestDist) {
                    bestDist = dist;
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
        return "1.1";
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
        return "Shift: Wchodzi w stan kontry gdy w pobliżu (8 bloków) są wrogowie. Otrzymanie ciosu anuluje obrażenia i przenosi za plecy atakującego. LPM: Uderzenie generuje 2 punkty witalne na ciele wroga (widoczne tylko dla usera). Trafienie w punkty zadaje bonusowy DMG i blokuje Chi.";
    }

    @Override
    public String getInstructions() {
        return "Shift przy wrogach: Stan Kontry. LPM w walce: Wykryj i uderzaj w 2 punkty witalne wroga!";
    }
}

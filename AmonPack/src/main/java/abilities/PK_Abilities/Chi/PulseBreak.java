package Abilities.PK_Abilities.Chi;

import Plugin.AmonPackPlugin;
import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.ChiAbility;
import com.projectkorra.projectkorra.util.DamageHandler;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class PulseBreak extends ChiAbility implements AddonAbility {

    private long cooldown;
    private double damage;
    private double knockback;
    private int abilitiesToCooldownCount;
    private long addedCooldownMs;
    private double chiCost;

    public PulseBreak(Player player) {
        super(player);

        if (hasAbility(player, PulseBreak.class)) {
            return;
        }
        if (bPlayer.isOnCooldown(this) || !bPlayer.canBend(this)) {
            return;
        }

        loadConfig();
        start();
    }

    private void loadConfig() {
        this.cooldown = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Chi.PulseBreak.Cooldown", 10000L);
        this.damage = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Chi.PulseBreak.Damage", 3.0);
        this.knockback = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Chi.PulseBreak.Knockback", 0.8);
        this.abilitiesToCooldownCount = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Chi.PulseBreak.AbilitiesToCooldownCount", 2);
        this.addedCooldownMs = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Chi.PulseBreak.AddedCooldownMs", 5000L);
        this.chiCost = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Chi.PulseBreak.ChiCost", 40.0);
    }

    @Override
    public void progress() {
        if (player == null || !player.isOnline() || player.isDead()) {
            remove();
            return;
        }

        // PulseBreak is a melee-triggered ability on hit
        if (System.currentTimeMillis() - getStartTime() > 2000L) {
            remove();
        }
    }

    public void onHitEntity(LivingEntity victim) {
        if (victim == null || player == null) return;

        if (!ChiManager.consumeChi(player, chiCost)) {
            remove();
            return;
        }

        DamageHandler.damageEntity(victim, damage, this);

        Vector kb = victim.getLocation().toVector().subtract(player.getLocation().toVector()).normalize().multiply(knockback).setY(0.25);
        victim.setVelocity(kb);

        playBodyPulseAnimation(victim);

        if (victim instanceof Player targetPlayer) {
            BendingPlayer targetBPlayer = BendingPlayer.getBendingPlayer(targetPlayer);
            if (targetBPlayer != null) {
                List<String> boundAbilities = new ArrayList<>();
                for (String abi : targetBPlayer.getAbilities().values()) {
                    if (abi != null && !abi.isEmpty() && !boundAbilities.contains(abi)) {
                        boundAbilities.add(abi);
                    }
                }

                if (!boundAbilities.isEmpty()) {
                    Collections.shuffle(boundAbilities);
                    int countToDisable = Math.min(abilitiesToCooldownCount, boundAbilities.size());
                    for (int i = 0; i < countToDisable; i++) {
                        String abiName = boundAbilities.get(i);
                        long currentCd = targetBPlayer.getCooldown(abiName);
                        long newCd = (currentCd > 0 ? (currentCd - System.currentTimeMillis()) : 0) + addedCooldownMs;
                        targetBPlayer.addCooldown(abiName, newCd);
                    }
                }
            }
        }

        if (bPlayer != null) {
            bPlayer.addCooldown(this, cooldown);
        }
        remove();
    }

    private void playBodyPulseAnimation(LivingEntity victim) {
        Location baseLoc = victim.getLocation();
        victim.getWorld().playSound(baseLoc, Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 1.2f, 0.8f);
        victim.getWorld().playSound(baseLoc, Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 1.0f, 1.6f);

        new BukkitRunnable() {
            int step = 0;

            @Override
            public void run() {
                if (victim.isDead() || !victim.isValid() || step >= 3) {
                    cancel();
                    return;
                }

                Location center = victim.getLocation().add(0, 1.0, 0);
                double radius = 0.5 + (step * 0.4);
                int points = 16;

                for (int i = 0; i < points; i++) {
                    double angle = (2 * Math.PI / points) * i;
                    double x = radius * Math.cos(angle);
                    double z = radius * Math.sin(angle);
                    Location pLoc = center.clone().add(x, 0, z);

                    pLoc.getWorld().spawnParticle(Particle.CRIT, pLoc, 1, 0, 0, 0, 0);
                    pLoc.getWorld().spawnParticle(Particle.ENCHANTED_HIT, pLoc, 1, 0, 0, 0, 0.05);
                }

                step++;
            }
        }.runTaskTimer(AmonPackPlugin.plugin, 0L, 2L);
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
        return "PulseBreak";
    }

    @Override
    public boolean isSneakAbility() {
        return false;
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
        return "Uderz przeciwnika w walce wręcz, aby wywołać impuls Chi rozbijający jego równowagę: zadaje obrażenia, odrzuca oraz nakłada wydłużony cooldown na losowe przypisane umiejętności celu.";
    }

    @Override
    public String getInstructions() {
        return "Wybierz slot z PulseBreak i zaatakuj wroga wręcz!";
    }
}

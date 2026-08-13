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
    }

    @Override
    public void progress() {
        if (player == null || !player.isOnline() || player.isDead()) {
            remove();
            return;
        }
    }

    public void onHitEntity(LivingEntity victim) {
        if (victim == null || player == null) return;

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
                        long currentCd = 0;
                        if (targetBPlayer.isOnCooldown(abiName)) {
                            if (targetBPlayer.getCooldowns().containsKey(abiName)) {
                                currentCd = targetBPlayer.getCooldowns().get(abiName).getCooldown() - System.currentTimeMillis();
                            }
                        }
                        long newCd = Math.max(0, currentCd) + addedCooldownMs;
                        targetBPlayer.addCooldown(abiName, newCd);
                    }
                }
            }
        }

        bPlayer.addCooldown(this, cooldown);
        remove();
    }

    private void playBodyPulseAnimation(LivingEntity victim) {
        victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.2f, 0.7f);
        victim.getWorld().playSound(victim.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.4f, 1.8f);

        new BukkitRunnable() {
            private int step = 0;

            @Override
            public void run() {
                step++;
                if (step > 15 || victim.isDead()) {
                    cancel();
                    return;
                }

                Location loc = victim.getLocation().add(0, 0.9, 0);
                double radius = 0.5 + (step * 0.08);

                for (int i = 0; i < 12; i++) {
                    double angle = (2 * Math.PI / 12) * i;
                    double x = radius * Math.cos(angle);
                    double z = radius * Math.sin(angle);
                    Location pLoc = loc.clone().add(x, Math.sin(step * 0.4) * 0.4, z);

                    victim.getWorld().spawnParticle(Particle.CRIT, pLoc, 1, 0, 0, 0, 0);
                    if (step % 3 == 0) {
                        victim.getWorld().spawnParticle(Particle.ENCHANTED_HIT, pLoc, 1, 0, 0, 0, 0);
                    }
                }
            }
        }.runTaskTimer(AmonPackPlugin.plugin, 0L, 1L);
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
        return "Uderzenie paraliżujące punkty chi przeciwnika. Zadaje obrażenia, odpycha oraz dodaje sekundy cooldownu do wybranych losowo skilli wroga.";
    }

    @Override
    public String getInstructions() {
        return "Zadzaj cios przeciwnikowi (LPM) mając wybrany PulseBreak!";
    }
}

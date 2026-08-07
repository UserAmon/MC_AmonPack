package Abilities.PK_Abilities.Air;

import Plugin.AmonPackPlugin;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.AirAbility;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.ParticleEffect;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class AirSteps extends AirAbility implements AddonAbility {

    private long cooldown;
    private int maxJumps;
    private int usedJumps = 0;
    private double damage;
    private double knockback;
    private double radius;
    private int ticksElapsed = 0;
    private long lastJumpTime = 0L;

    public AirSteps(Player player) {
        super(player);

        AirSteps active = getAbility(player, AirSteps.class);
        if (active != null) {
            active.performJump();
            return;
        }

        this.cooldown = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Air.AirSteps.Cooldown", 6000L);
        this.maxJumps = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Air.AirSteps.MaxJumps", 3);
        this.damage = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Air.AirSteps.Damage", 3.5);
        this.knockback = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Air.AirSteps.Knockback", 1.2);
        this.radius = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Air.AirSteps.Radius", 3.5);

        if (bPlayer.isOnCooldown(this) || !bPlayer.canBend(this)) {
            return;
        }

        performJump();
        start();
    }

    public void performJump() {
        if (usedJumps >= maxJumps) {
            return;
        }

        if (System.currentTimeMillis() - lastJumpTime < 500L) {
            return;
        }
        lastJumpTime = System.currentTimeMillis();

        usedJumps++;

        // Podskok w górę i w stronę patrzenia
        Vector lookDir = player.getEyeLocation().getDirection().clone();
        Vector jumpVel = lookDir.multiply(0.4).setY(0.75);
        player.setVelocity(jumpVel);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_HORSE_JUMP, 0.8f, 1.4f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 0.6f, 1.6f);

        // Podmuch powietrza w dół pod graczem
        Location blastLoc = player.getLocation().clone();
        blastLoc.getWorld().spawnParticle(org.bukkit.Particle.DUST, blastLoc, 15, 0.5, 0.5, 0.5, 0.05, new org.bukkit.Particle.DustOptions(org.bukkit.Color.WHITE, 1.2f));
        ParticleEffect.CLOUD.display(blastLoc, 15, 0.8, 0.2, 0.8, 0.1);
        ParticleEffect.SWEEP_ATTACK.display(blastLoc, 3, 0.5, 0.5, 0.5, 0.1);

        // Odrzucenie pobliskich wrogów na boki
        for (Entity entity : GeneralMethods.getEntitiesAroundPoint(blastLoc, radius)) {
            if (entity instanceof LivingEntity && entity.getUniqueId() != player.getUniqueId()) {
                LivingEntity target = (LivingEntity) entity;
                Vector kb = target.getLocation().toVector().subtract(blastLoc.toVector()).normalize().multiply(knockback).setY(0.2);
                target.setVelocity(kb);

                // Wykrywanie uderzenia w ścianę
                trackWallImpact(target, this);
            }
        }

        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText("§b[AirSteps] Skoki: §e" + usedJumps + "/" + maxJumps));

        if (usedJumps >= maxJumps) {
            bPlayer.addCooldown(this, cooldown);
            remove();
        }
    }

    private void trackWallImpact(LivingEntity target, AirSteps ability) {
        new BukkitRunnable() {
            int count = 0;
            @Override
            public void run() {
                count++;
                if (target.isDead() || !target.isValid() || count > 12) {
                    cancel();
                    return;
                }

                Location tLoc = target.getLocation();
                for (int x = -1; x <= 1; x++) {
                    for (int z = -1; z <= 1; z++) {
                        if (x == 0 && z == 0) continue;
                        Block sideBlock = tLoc.clone().add(x, 0.5, z).getBlock();
                        if (sideBlock.getType().isSolid()) {
                            DamageHandler.damageEntity(target, damage, ability);
                            target.getWorld().playSound(tLoc, Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, 0.8f, 1.2f);
                            ParticleEffect.BLOCK_CRACK.display(tLoc, 20, 0.4, 0.4, 0.4, 0.1, sideBlock.getBlockData());
                            cancel();
                            return;
                        }
                    }
                }
            }
        }.runTaskTimer(AmonPackPlugin.plugin, 1L, 1L);
    }

    @Override
    public void progress() {
        if (player == null || player.isDead() || !player.isOnline()) {
            remove();
            return;
        }

        ticksElapsed++;
        if (ticksElapsed > 140 || (ticksElapsed > 15 && player.isOnGround())) {
            bPlayer.addCooldown(this, cooldown);
            remove();
        }
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
        return "AirSteps";
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

    @Override
    public String getDescription() {
        return "Pozwala na 3 podskoki powietrzne w górę i do przodu podczas kucania. Podmuchy odrzucają wrogów na boki, zadając obrażenia przy uderzeniu w ścianę.";
    }

    @Override
    public String getInstructions() {
        return "Kucnij (SHIFT), aby podskoczyć w powietrzu! Możesz wykonać 3 skoki przed cooldownem.";
    }
}

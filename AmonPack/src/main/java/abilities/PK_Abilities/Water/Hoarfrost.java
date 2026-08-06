package Abilities.PK_Abilities.Water;

import Abilities.Bending.SpecialTriggerable;
import Abilities.Bending.SpecialTriggerManager;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.WaterAbility;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.ParticleEffect;
import com.projectkorra.projectkorra.util.TempBlock;
import Plugin.AmonPackPlugin;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class Hoarfrost extends WaterAbility implements AddonAbility, SpecialTriggerable {

    private int durationTicks = 0;
    private final int maxDurationTicks = 100; // 5 sekund na wchłonięcie kolejnych stacków / wystrzał

    private int stacks = 0;
    private final int maxStacks = 3;

    private double damagePerSpear;
    private double knockback;
    private long cooldown;

    private boolean isFiring = false;

    public Hoarfrost(Player player) {
        super(player);

        // Jeśli instancja już działa, nowa próba aktywacji (np. ponowne F/Q) dodaje kolejny stack
        Hoarfrost activeInstance = getAbility(player, Hoarfrost.class);
        if (activeInstance != null) {
            activeInstance.tryAddStack();
            return;
        }

        if (bPlayer.isOnCooldown(this) || !bPlayer.canBendIgnoreBinds(this)) {
            return;
        }

        // Warunek aktywacji: Gracz musi patrzeć na źródło wody
        Block targetWaterBlock = getWaterSourceBlock(player, 20);
        if (targetWaterBlock == null) {
            return;
        }

        this.damagePerSpear = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Water.Hoarfrost.DamagePerSpear", 4.5);
        this.knockback = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Water.Hoarfrost.Knockback", 0.65);
        this.cooldown = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Water.Hoarfrost.Cooldown", 6000);

        SpecialTriggerManager.registerActiveSpecial(player);
        this.stacks = 1;
        pullWaterFromSource(targetWaterBlock.getLocation());

        start();
    }

    public void tryAddStack() {
        if (isFiring || stacks >= maxStacks) return;

        Block targetWaterBlock = getWaterSourceBlock(player, 20);
        if (targetWaterBlock != null) {
            stacks++;
            durationTicks = 0; // Resetujemy 5-sekundowe okno czasowe
            pullWaterFromSource(targetWaterBlock.getLocation());
            player.getWorld().playSound(player.getLocation(), Sound.ITEM_BOTTLE_FILL, 0.8f, 1.4f);
        }
    }

    @Override
    public void progress() {
        if (player == null || player.isDead() || !player.isOnline()) {
            finish();
            return;
        }

        if (isFiring) return;

        durationTicks++;
        if (durationTicks >= maxDurationTicks) {
            finish();
            return;
        }

        SpecialTriggerManager.applySoftCooldownToToolbar(player);

        // Wyświetlanie staku na action barze
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                new TextComponent(ChatColor.AQUA + "✦ HOARFROST STACKI: " + ChatColor.WHITE + stacks + " / 3 " + ChatColor.YELLOW + "[SHIFT - Wystrzał]"));

        // Efekt cząsteczek lodu obok dłoni gracza zależny od ilości stacków
        displayHandIceParticles();

        // Jeśli gracz naciśnie SHIFT - wystrzeliwujemy sople lodu!
        if (player.isSneaking()) {
            fireIceSpears();
        }
    }

    private void fireIceSpears() {
        isFiring = true;
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.2f, 0.7f);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0f, 1.3f);

        Location eye = player.getEyeLocation();
        Vector baseDir = eye.getDirection().clone().normalize();

        for (int i = 0; i < stacks; i++) {
            double angleOffset = (i - (stacks - 1) / 2.0) * 0.25;
            Vector spreadDir = baseDir.clone().add(new Vector(angleOffset, 0.05, 0.0)).normalize();

            Location spawnLoc = eye.clone().add(spreadDir.clone().multiply(1.2));
            FallingBlock iceBlock = player.getWorld().spawnFallingBlock(spawnLoc, Material.ICE.createBlockData());
            iceBlock.setDropItem(false);
            iceBlock.setHurtEntities(false);
            iceBlock.setVelocity(spreadDir.clone().multiply(1.4));

            // Customowy timer czyszczący FallingBlock (aby nie osiadały na ziemi)
            new BukkitRunnable() {
                private int ticks = 0;
                @Override
                public void run() {
                    ticks++;
                    if (!iceBlock.isValid() || iceBlock.isOnGround() || ticks > 60) {
                        Location hitLoc = iceBlock.getLocation();
                        ParticleEffect.WATER_DROP.display(hitLoc, 10, 0.3, 0.3, 0.3, 0.1);
                        ParticleEffect.CRIT_MAGIC.display(hitLoc, 10, 0.3, 0.3, 0.3, 0.1);
                        hitLoc.getWorld().playSound(hitLoc, Sound.BLOCK_GLASS_BREAK, 0.8f, 1.5f);

                        for (Entity entity : GeneralMethods.getEntitiesAroundPoint(hitLoc, 1.8)) {
                            if (entity instanceof LivingEntity && !entity.getUniqueId().equals(player.getUniqueId())) {
                                LivingEntity target = (LivingEntity) entity;
                                DamageHandler.damageEntity(target, damagePerSpear, Hoarfrost.this);
                                target.setVelocity(spreadDir.clone().multiply(knockback).setY(0.25));
                            }
                        }

                        iceBlock.remove();
                        this.cancel();
                    }
                }
            }.runTaskTimer(AmonPackPlugin.plugin, 1, 1);
        }

        finish();
    }

    private void pullWaterFromSource(Location sourceLoc) {
        new BukkitRunnable() {
            private Location current = sourceLoc.clone();
            @Override
            public void run() {
                if (!player.isOnline() || current.distanceSquared(player.getLocation()) < 2.0) {
                    this.cancel();
                    return;
                }
                Vector dir = player.getLocation().add(0, 1, 0).toVector().subtract(current.toVector()).normalize();
                current.add(dir.multiply(0.8));
                ParticleEffect.WATER_WAKE.display(current, 3, 0.08, 0.08, 0.08, 0.02);
                ParticleEffect.WATER_SPLASH.display(current, 3, 0.08, 0.08, 0.08, 0.02);

                Block b = current.getBlock();
                if (b.getType() == Material.AIR) {
                    new TempBlock(b, Material.WATER).setRevertTime(120);
                }
            }
        }.runTaskTimer(AmonPackPlugin.plugin, 0, 1);
    }

    private void displayHandIceParticles() {
        Location hand = player.getLocation().add(0, 1.2, 0);
        ParticleEffect.CRIT_MAGIC.display(hand, stacks * 2, 0.2, 0.2, 0.2, 0.02);
        ParticleEffect.WATER_DROP.display(hand, stacks * 2, 0.2, 0.2, 0.2, 0.02);
    }

    private Block getWaterSourceBlock(Player player, int range) {
        Block target = player.getTargetBlockExact(range);
        if (target != null && (isWater(target) || isIce(target))) {
            return target;
        }
        for (Block b : player.getLineOfSight(null, range)) {
            if (isWater(b) || isIce(b)) {
                return b;
            }
        }
        return null;
    }

    private void finish() {
        SpecialTriggerManager.unregisterActiveSpecial(player);
        bPlayer.addCooldown(this);
        remove();
    }

    @Override
    public TriggerType getSupportedTriggerType() {
        return TriggerType.SWAP;
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
        return "Hoarfrost";
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
        return "Amon";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public void load() {}

    @Override
    public void stop() {}
}

package Abilities.PK_Abilities.Water;

import Abilities.Bending.SpecialTriggerable;
import Abilities.Bending.SpecialTriggerManager;
import Plugin.AmonPackPlugin;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.PlantAbility;
import com.projectkorra.projectkorra.ability.WaterAbility;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.ParticleEffect;
import com.projectkorra.projectkorra.util.TempBlock;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class VineSnare extends WaterAbility implements AddonAbility, SpecialTriggerable {

    private enum State {
        PREVIEW,
        ERUPTION,
        HOLDING
    }

    private State state;
    private long cooldown;
    private int range;
    private double radius;
    private long chargeTime;
    private long holdDuration;
    private double damage;
    private long flowerRevertTime;

    private Location targetLoc;
    private List<LivingEntity> trappedEntities = new ArrayList<>();
    private int ticksElapsed = 0;

    public VineSnare(Player player) {
        super(player);

        VineSnare active = getAbility(player, VineSnare.class);
        if (active != null) {
            return;
        }

        this.cooldown = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Water.Plant.VineSnare.Cooldown", 9000L);
        this.range = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Water.Plant.VineSnare.Range", 20);
        this.radius = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Water.Plant.VineSnare.Radius", 3.5);
        this.chargeTime = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Water.Plant.VineSnare.ChargeTime", 2000L);
        this.holdDuration = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Water.Plant.VineSnare.HoldDuration", 3000L);
        this.damage = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Water.Plant.VineSnare.Damage", 3.0);
        this.flowerRevertTime = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Water.Plant.VineSnare.FlowerRevertTime", 10000L);

        if (bPlayer.isOnCooldown(this) || !bPlayer.canBendIgnoreBinds(this)) {
            return;
        }

        Block targetBlock = player.getTargetBlockExact(range);
        if (targetBlock == null || !isPlantbendableGround(targetBlock)) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText("§cMusisz patrzeć na ziemię roślinną!"));
            return;
        }

        this.targetLoc = targetBlock.getLocation().add(0.5, 1.0, 0.5);
        this.state = State.PREVIEW;
        SpecialTriggerManager.registerActiveSpecial(player);
        start();
    }

    private boolean isPlantbendableGround(Block b) {
        if (b == null) return false;
        Material m = b.getType();
        return m == Material.GRASS_BLOCK || m == Material.DIRT || m == Material.COARSE_DIRT || m == Material.PODZOL
                || m == Material.MOSS_BLOCK || m == Material.FARMLAND || m == Material.OAK_LEAVES || m == Material.JUNGLE_LEAVES
                || WaterAbility.isPlantbendable(player, m, false) || PlantAbility.isPlant(b);
    }

    @Override
    public void progress() {
        if (player == null || player.isDead() || !player.isOnline()) {
            cancelAbility();
            return;
        }

        ticksElapsed++;

        if (state == State.PREVIEW) {
            for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 8) {
                double x = radius * Math.cos(angle);
                double z = radius * Math.sin(angle);
                Location pLoc = targetLoc.clone().add(x, 0.1, z);
                ParticleEffect.VILLAGER_HAPPY.display(pLoc, 2, 0.1, 0.1, 0.1, 0.02);
                ParticleEffect.COMPOSTER.display(pLoc, 1, 0.1, 0.1, 0.1, 0.01);
                ParticleEffect.BLOCK_CRACK.display(pLoc, 1, 0.1, 0.1, 0.1, 0.02, Material.OAK_LEAVES.createBlockData());
            }

            if (ticksElapsed % 8 == 0) {
                targetLoc.getWorld().playSound(targetLoc, Sound.BLOCK_GRASS_STEP, 0.8f, 0.8f);
            }

            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText("§a[VineSnare] Pnącza się rozrastają..."));

            if (ticksElapsed >= (chargeTime / 50)) {
                eruptVines();
            }
        } else if (state == State.HOLDING) {
            for (LivingEntity entity : trappedEntities) {
                if (entity != null && !entity.isDead() && entity.isValid()) {
                    Location eLoc = entity.getLocation();
                    for (double y = 0; y <= 2.0; y += 0.5) {
                        double angle = ticksElapsed * 0.5 + y * 2.0;
                        double x = 0.6 * Math.cos(angle);
                        double z = 0.6 * Math.sin(angle);
                        Location pLoc = eLoc.clone().add(x, y, z);
                        ParticleEffect.BLOCK_CRACK.display(pLoc, 2, 0.1, 0.1, 0.1, 0.05, Material.OAK_LEAVES.createBlockData());
                        ParticleEffect.SLIME.display(pLoc, 1, 0.05, 0.05, 0.05, 0.01);
                    }

                    entity.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20, 10, false, false));
                    entity.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 20, 128, false, false));

                    Vector pull = targetLoc.clone().toVector().subtract(entity.getLocation().toVector()).multiply(0.2);
                    entity.setVelocity(pull);
                }
            }

            if (ticksElapsed >= (chargeTime + holdDuration) / 50) {
                finishAbility();
            }
        }
    }

    private void eruptVines() {
        state = State.HOLDING;

        targetLoc.getWorld().playSound(targetLoc, Sound.BLOCK_GRASS_BREAK, 1.2f, 0.8f);
        targetLoc.getWorld().playSound(targetLoc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.2f);

        for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 10) {
            for (double height = 0; height <= 3.5; height += 0.5) {
                double r = radius * (1.0 - (height / 5.0));
                double x = r * Math.cos(angle);
                double z = r * Math.sin(angle);
                Location pLoc = targetLoc.clone().add(x, height, z);
                ParticleEffect.VILLAGER_HAPPY.display(pLoc, 2, 0.15, 0.15, 0.15, 0.05);
                ParticleEffect.BLOCK_CRACK.display(pLoc, 3, 0.15, 0.15, 0.15, 0.05, Material.OAK_LEAVES.createBlockData());
            }
        }

        for (Entity entity : GeneralMethods.getEntitiesAroundPoint(targetLoc, radius)) {
            if (entity instanceof LivingEntity && entity.getUniqueId() != player.getUniqueId()) {
                LivingEntity le = (LivingEntity) entity;
                DamageHandler.damageEntity(le, damage, this);

                Vector pull = targetLoc.clone().toVector().subtract(le.getLocation().toVector()).normalize().multiply(0.8);
                pull.setY(0.2);
                le.setVelocity(pull);

                trappedEntities.add(le);
            }
        }
    }

    private void finishAbility() {
        Material[] flowers = {
            Material.DANDELION, Material.POPPY, Material.BLUE_ORCHID, Material.ALLIUM,
            Material.AZURE_BLUET, Material.RED_TULIP, Material.ORANGE_TULIP, Material.PINK_TULIP,
            Material.OXEYE_DAISY, Material.CORNFLOWER, Material.LILY_OF_THE_VALLEY
        };
        Random rand = new Random();

        List<Block> area = GeneralMethods.getBlocksAroundPoint(targetLoc, (int) radius);
        for (Block b : area) {
            if (isPlantbendableGround(b)) {
                Block above = b.getRelative(0, 1, 0);
                if (above.getType() == Material.AIR) {
                    Material flowerMat = flowers[rand.nextInt(flowers.length)];
                    TempBlock tb = new TempBlock(above, flowerMat);
                    tb.setRevertTime(flowerRevertTime);
                }
            }
        }

        targetLoc.getWorld().playSound(targetLoc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.2f);
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText("§a[VineSnare] Kwiaty zakwitły!"));

        for (LivingEntity entity : trappedEntities) {
            if (entity != null && entity.isValid()) {
                entity.removePotionEffect(PotionEffectType.SLOWNESS);
                entity.removePotionEffect(PotionEffectType.JUMP_BOOST);
            }
        }

        bPlayer.addCooldown(this, cooldown);
        SpecialTriggerManager.unregisterActiveSpecial(player);
        remove();
    }

    private void cancelAbility() {
        for (LivingEntity entity : trappedEntities) {
            if (entity != null && entity.isValid()) {
                entity.removePotionEffect(PotionEffectType.SLOWNESS);
                entity.removePotionEffect(PotionEffectType.JUMP_BOOST);
            }
        }
        if (bPlayer != null) {
            bPlayer.addCooldown(this, cooldown);
        }
        SpecialTriggerManager.unregisterActiveSpecial(player);
        remove();
    }

    @Override
    public TriggerType getSupportedTriggerType() {
        return TriggerType.BOTH;
    }

    @Override
    public long getCooldown() {
        return cooldown;
    }

    @Override
    public Location getLocation() {
        return targetLoc != null ? targetLoc : (player != null ? player.getLocation() : null);
    }

    @Override
    public String getName() {
        return "VineSnare";
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
        return false;
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
        return "Tworzy krąg z pnączy na ziemi roślinnej, który po 2s strzela w górę, przyciąga i uwięziła wrogów, a na koniec tworzy kwitnące kwiaty.";
    }

    @Override
    public String getInstructions() {
        return "Spójrz na ziemię roślinną i naciśnij F lub L, aby aktywować sidła pnączy!";
    }
}

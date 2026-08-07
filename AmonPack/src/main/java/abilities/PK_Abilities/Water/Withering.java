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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Withering extends WaterAbility implements AddonAbility, SpecialTriggerable {

    private enum State {
        DRYING, ERUPT_THORNS
    }

    private State state;
    private long startTime;
    private long dryingDuration;
    private double radius;
    private double damage;
    private long cooldown;
    private long revertTime;

    private Location centerLoc;
    private int ticksElapsed = 0;
    private List<TempBlock> createdThorns = new ArrayList<>();
    private Random random = new Random();

    public Withering(Player player) {
        super(player);

        Withering active = getAbility(player, Withering.class);
        if (active != null) {
            return;
        }

        if (bPlayer.isOnCooldown(this) || !bPlayer.canBendIgnoreBinds(this)) {
            return;
        }

        this.dryingDuration = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Water.Plant.Withering.DryingDuration", 2000L); // 2s animacja usychania
        this.radius = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Water.Plant.Withering.Radius", 8.0);
        this.damage = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Water.Plant.Withering.Damage", 5.0);
        this.cooldown = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Water.Plant.Withering.Cooldown", 8000L);
        this.revertTime = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Water.Plant.Withering.RevertTime", 10000L);

        Block targetBlock = player.getTargetBlockExact(18);
        if (targetBlock != null && isPlantbendableGround(targetBlock)) {
            this.centerLoc = targetBlock.getLocation().add(0.5, 1.0, 0.5);
        } else {
            this.centerLoc = player.getLocation().clone();
        }

        this.state = State.DRYING;
        this.startTime = System.currentTimeMillis();

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
            finish();
            return;
        }

        ticksElapsed++;

        if (state == State.DRYING) {
            SpecialTriggerManager.applySoftCooldownToToolbar(player);
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText("§8[Withering] Rośliny usychają..."));

            // 2-sekundowa animacja usychania roślin i bloków trawy
            List<Block> area = GeneralMethods.getBlocksAroundPoint(centerLoc, (int) radius);
            for (Block b : area) {
                if (isPlantbendableGround(b) && random.nextDouble() < 0.2) {
                    ParticleEffect.SMOKE_NORMAL.display(b.getLocation().add(0.5, 1.0, 0.5), 1, 0.2, 0.2, 0.2, 0.01);
                    ParticleEffect.ASH.display(b.getLocation().add(0.5, 1.0, 0.5), 1, 0.2, 0.2, 0.2, 0.01);
                }
            }

            if (ticksElapsed % 6 == 0) {
                centerLoc.getWorld().playSound(centerLoc, Sound.BLOCK_GRASS_BREAK, 0.8f, 0.5f);
                centerLoc.getWorld().playSound(centerLoc, Sound.ENTITY_WITHER_AMBIENT, 0.3f, 1.6f);
            }

            if (System.currentTimeMillis() - startTime >= dryingDuration) {
                eruptThorns();
            }
        }
    }

    private void eruptThorns() {
        state = State.ERUPT_THORNS;

        centerLoc.getWorld().playSound(centerLoc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.2f, 0.7f);
        centerLoc.getWorld().playSound(centerLoc, Sound.ENTITY_WITHER_HURT, 0.6f, 1.2f);

        Material[] thornMats = {
            Material.POINTED_DRIPSTONE, Material.SWEET_BERRY_BUSH, Material.DEAD_BUSH, Material.TWISTING_VINES
        };

        List<Block> area = GeneralMethods.getBlocksAroundPoint(centerLoc, (int) radius);
        for (Block b : area) {
            if (isPlantbendableGround(b)) {
                // Trawa i podłoże usychają w wyjałowioną ziemię
                if (b.getType() == Material.GRASS_BLOCK || b.getType() == Material.DIRT || b.getType() == Material.MOSS_BLOCK) {
                    new TempBlock(b, Material.COARSE_DIRT).setRevertTime(revertTime);
                }

                Block above = b.getRelative(0, 1, 0);
                if (above.getType() == Material.AIR || PlantAbility.isPlant(above)) {
                    // Tworzenie wyższych pól cierni (2-3 bloki w górę)
                    int thornHeight = 2 + random.nextInt(2);
                    for (int h = 0; h < thornHeight; h++) {
                        Block tBlock = b.getRelative(0, 1 + h, 0);
                        if (tBlock.getType() == Material.AIR || PlantAbility.isPlant(tBlock)) {
                            Material thornMat = thornMats[random.nextInt(thornMats.length)];
                            TempBlock tb = new TempBlock(tBlock, thornMat);
                            tb.setRevertTime(revertTime);
                            createdThorns.add(tb);
                        }
                    }
                }
            }
        }

        // Obrażenia i debuff usychania dla wrogów w obszarze
        for (Entity entity : GeneralMethods.getEntitiesAroundPoint(centerLoc, radius + 1.0)) {
            if (entity instanceof LivingEntity && entity.getUniqueId() != player.getUniqueId()) {
                LivingEntity target = (LivingEntity) entity;
                DamageHandler.damageEntity(target, damage, this);
                target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 100, 1, false, false));
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 2, false, false));
                ParticleEffect.SMOKE_LARGE.display(target.getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0.05);
            }
        }

        finish();
    }

    private void finish() {
        SpecialTriggerManager.unregisterActiveSpecial(player);
        bPlayer.addCooldown(this, cooldown);
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
        return centerLoc != null ? centerLoc : (player != null ? player.getLocation() : null);
    }

    @Override
    public String getName() {
        return "Withering";
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
        remove();
    }

    @Override
    public String getDescription() {
        return "Powoduje 2-sekundowe usychanie roślin w obszarze, po czym podłoże zamienia się w wyjałowioną ziemię i wystrzeliwuje wysokie pola cierni (2-3 bloki) zadające obrażenia i efekty Wither/Slowness.";
    }

    @Override
    public String getInstructions() {
        return "Naciśnij F (SWAP), aby aktywować usychanie roślin!";
    }
}

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
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class Withering extends WaterAbility implements AddonAbility, SpecialTriggerable {

    private long cooldown;
    private double radius;
    private double damage;
    private long revertTime;

    private Block initialTargetBlock;
    private Location centerLoc;
    private Set<Block> witheredBlocks = new HashSet<>();
    private List<TempBlock> deadBushTempBlocks = new ArrayList<>();
    private int ticksElapsed = 0;
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

        this.radius = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Water.Plant.Withering.Radius", 8.0);
        this.damage = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Water.Plant.Withering.Damage", 4.0);
        this.cooldown = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Water.Plant.Withering.Cooldown", 8000L);
        this.revertTime = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Water.Plant.Withering.RevertTime", 10000L);

        Block targetBlock = player.getTargetBlockExact(18);
        if (targetBlock == null || !isPlantbendableGround(targetBlock)) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText("§cMusisz patrzeć na blok roślinny!"));
            return;
        }

        this.initialTargetBlock = targetBlock;
        this.centerLoc = targetBlock.getLocation().add(0.5, 1.0, 0.5);

        SpecialTriggerManager.registerActiveSpecial(player);
        start();
        startWitheringProcess();
    }

    private boolean isPlantbendableGround(Block b) {
        if (b == null) return false;
        Material m = b.getType();
        return m == Material.GRASS_BLOCK || m == Material.DIRT || m == Material.COARSE_DIRT || m == Material.PODZOL
                || m == Material.MOSS_BLOCK || m == Material.FARMLAND
                || WaterAbility.isPlantbendable(player, m, false) || PlantAbility.isPlant(b);
    }

    private void startWitheringProcess() {
        // Faza 1 (1s / 20 ticków): Animacja usychania na celowanym bloku
        new BukkitRunnable() {
            private int phase1Ticks = 0;

            @Override
            public void run() {
                phase1Ticks++;
                SpecialTriggerManager.applySoftCooldownToToolbar(player);
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText("§8[Withering] Usychanie na bloku..."));

                ParticleEffect.SMOKE_NORMAL.display(initialTargetBlock.getLocation().add(0.5, 1.0, 0.5), 3, 0.2, 0.2, 0.2, 0.02);
                ParticleEffect.ASH.display(initialTargetBlock.getLocation().add(0.5, 1.0, 0.5), 3, 0.2, 0.2, 0.2, 0.02);

                if (phase1Ticks % 4 == 0) {
                    initialTargetBlock.getWorld().playSound(initialTargetBlock.getLocation(), Sound.BLOCK_GRASS_BREAK, 0.8f, 0.5f);
                }

                if (phase1Ticks >= 20) {
                    this.cancel();
                    witherBlock(initialTargetBlock);
                    startSpreadingPhase();
                }
            }
        }.runTaskTimer(AmonPackPlugin.plugin, 0L, 1L);
    }

    private void startSpreadingPhase() {
        // Faza 2: Powolne i losowe rozrastanie usychania na pobliskie bloki plantbendable
        new BukkitRunnable() {
            private int spreadTicks = 0;

            @Override
            public void run() {
                spreadTicks++;
                SpecialTriggerManager.applySoftCooldownToToolbar(player);

                // Losowy rozrost na 1-2 sąsiednie bloki
                List<Block> candidates = GeneralMethods.getBlocksAroundPoint(centerLoc, (int) radius);
                if (!candidates.isEmpty()) {
                    for (int i = 0; i < 2; i++) {
                        Block b = candidates.get(random.nextInt(candidates.size()));
                        if (isPlantbendableGround(b) && !witheredBlocks.contains(b)) {
                            witherBlock(b);
                        }
                    }
                }

                // Uschnięte krzewy (DEAD_BUSH) emitują cząsteczki i zadają obrażenia oraz spowolnienie przechodzącym wrogom
                for (TempBlock deadBushTB : deadBushTempBlocks) {
                    if (deadBushTB != null && deadBushTB.getBlock().getType() == Material.DEAD_BUSH) {
                        Location dLoc = deadBushTB.getLocation().add(0.5, 0.5, 0.5);
                        ParticleEffect.SMOKE_NORMAL.display(dLoc, 1, 0.1, 0.1, 0.1, 0.01);
                        ParticleEffect.ASH.display(dLoc, 1, 0.1, 0.1, 0.1, 0.01);

                        for (Entity entity : GeneralMethods.getEntitiesAroundPoint(dLoc, 1.3)) {
                            if (entity instanceof LivingEntity && entity.getUniqueId() != player.getUniqueId()) {
                                LivingEntity target = (LivingEntity) entity;
                                DamageHandler.damageEntity(target, damage / 3.0, Withering.this);
                                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 30, 2, false, false));
                                ParticleEffect.CRIT.display(target.getLocation().add(0, 1, 0), 3, 0.2, 0.2, 0.2, 0.02);
                            }
                        }
                    }
                }

                if (spreadTicks >= 60) { // 3 sekundy rozrastania
                    this.cancel();
                    finish();
                }
            }
        }.runTaskTimer(AmonPackPlugin.plugin, 0L, 2L);
    }

    private void witherBlock(Block b) {
        witheredBlocks.add(b);

        // Zamiana trawy na PODZOL
        if (b.getType() == Material.GRASS_BLOCK || b.getType() == Material.DIRT || b.getType() == Material.MOSS_BLOCK) {
            new TempBlock(b, Material.PODZOL).setRevertTime(revertTime);
        }

        // Zamiana roślin/kwiatów/trawy na górze w DEAD_BUSH
        Block above = b.getRelative(0, 1, 0);
        if (above.getType() == Material.AIR || PlantAbility.isPlant(above)) {
            TempBlock deadBushTB = new TempBlock(above, Material.DEAD_BUSH);
            deadBushTB.setRevertTime(revertTime);
            deadBushTempBlocks.add(deadBushTB);
        }

        b.getWorld().playSound(b.getLocation(), Sound.BLOCK_GRASS_BREAK, 0.6f, 0.6f);
    }

    private void finish() {
        SpecialTriggerManager.unregisterActiveSpecial(player);
        bPlayer.addCooldown(this, cooldown);
        remove();
    }

    @Override
    public void progress() {
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
        return "2.0";
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
        return "Ususza wskazany blok roślinny (po 1s zmienia go w Podzol i Dead Bush), po czym usychanie losowo rozrasta się na pobliskie bloki. Uschnięte krzewy emitują dym i zadają obrażenia oraz spowolnienie wrogom.";
    }

    @Override
    public String getInstructions() {
        return "Spójrz na blok roślinny i naciśnij F (SWAP), aby go ususzyć!";
    }
}

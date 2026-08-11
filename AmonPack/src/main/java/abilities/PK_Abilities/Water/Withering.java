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
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class Withering extends PlantAbility implements AddonAbility, SpecialTriggerable {

    private static class ThornSpike {
        Location startLoc;
        Vector direction;
        double length = 2.5;

        ThornSpike(Location startLoc, Vector direction) {
            this.startLoc = startLoc;
            this.direction = direction;
        }

        void displayAndHazard(Player caster, double damage, Withering ability) {
            // Cząsteczki martwego kolca: ciemna zieleń, rosnący z kilkoma rozchodzeniami na
            // boki
            Vector dirNorm = direction.clone().normalize();
            Vector sideVec = dirNorm.clone().crossProduct(new Vector(0, 1, 0));
            if (sideVec.lengthSquared() < 0.01)
                sideVec = new Vector(1, 0, 0);

            for (double d = 0; d <= length; d += 0.3) {
                Location spikePt = startLoc.clone().add(dirNorm.clone().multiply(d));
                spikePt.getWorld().spawnParticle(Particle.DUST, spikePt, 1, 0, 0, 0, 0,
                        new Particle.DustOptions(Color.fromRGB(20, 70, 25), 1.2f));

                // Rozchodzenie się na boki (gałązki kolca)
                if (d > 0.6 && d < 2.0) {
                    Location branch1 = spikePt.clone().add(sideVec.clone().multiply(0.35));
                    Location branch2 = spikePt.clone().add(sideVec.clone().multiply(-0.35));
                    branch1.getWorld().spawnParticle(Particle.DUST, branch1, 1, 0, 0, 0, 0,
                            new Particle.DustOptions(Color.fromRGB(25, 80, 30), 1.0f));
                    branch2.getWorld().spawnParticle(Particle.DUST, branch2, 1, 0, 0, 0, 0,
                            new Particle.DustOptions(Color.fromRGB(25, 80, 30), 1.0f));
                }
            }

            // Obrażenia po wejściu w kolec
            Location centerSpike = startLoc.clone().add(dirNorm.clone().multiply(length * 0.5));
            for (Entity entity : GeneralMethods.getEntitiesAroundPoint(centerSpike, 1.4)) {
                if (entity instanceof LivingEntity && entity.getUniqueId() != caster.getUniqueId()) {
                    LivingEntity target = (LivingEntity) entity;
                    DamageHandler.damageEntity(target, damage / 2.0, ability);
                    target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 30, 2, false, false));
                    ParticleEffect.CRIT.display(target.getLocation().add(0, 1, 0), 4, 0.2, 0.2, 0.2, 0.02);
                }
            }
        }
    }

    private long cooldown;
    private double radius;
    private double damage;
    private long revertTime;

    private Block initialTargetBlock;
    private Location centerLoc;
    private Set<Block> witheredBlocks = new HashSet<>();
    private List<TempBlock> deadBushTempBlocks = new ArrayList<>();
    private List<ThornSpike> activeThornSpikes = new ArrayList<>();
    private double densityMultiplier = 1.0;
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
        this.revertTime = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Water.Plant.Withering.RevertTime",
                10000L);

        Block targetBlock = player.getTargetBlockExact(18);
        if (targetBlock == null || !isPlantbendableGround(targetBlock)) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                    TextComponent.fromLegacyText("§cMusisz patrzeć na blok roślinny!"));
            return;
        }

        this.initialTargetBlock = targetBlock;
        this.centerLoc = targetBlock.getLocation().add(0.5, 1.0, 0.5);

        // Obliczanie gęstości bloków roślinnych (skalowanie szybkości do max 300%)
        int plantCount = countPlantbendableBlocks(centerLoc, (int) radius);
        this.densityMultiplier = Math.min(3.0, Math.max(1.0, 1.0 + (plantCount / 12.0)));

        SpecialTriggerManager.registerActiveSpecial(player);
        start();
        startWitheringProcess();
    }

    private int countPlantbendableBlocks(Location loc, int r) {
        int count = 0;
        for (Block b : GeneralMethods.getBlocksAroundPoint(loc, r)) {
            if (isPlantbendableGround(b)) {
                count++;
            }
        }
        return count;
    }

    private boolean isPlantbendableGround(Block b) {
        if (b == null)
            return false;
        Material m = b.getType();
        return m == Material.GRASS_BLOCK || m == Material.DIRT || m == Material.COARSE_DIRT || m == Material.PODZOL
                || m == Material.MOSS_BLOCK || m == Material.FARMLAND || m.name().contains("LOG")
                || m.name().contains("WOOD")
                || WaterAbility.isPlantbendable(player, m, false) || PlantAbility.isPlant(b);
    }

    private void startWitheringProcess() {
        // Faza 1: Animacja usychania na skrócona skrajnie przy dużej gęstości bloków
        new BukkitRunnable() {
            private int phase1Ticks = 0;
            private int maxTicks = (int) Math.max(5, 20 / densityMultiplier);

            @Override
            public void run() {
                phase1Ticks++;
                SpecialTriggerManager.applySoftCooldownToToolbar(player);
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                        TextComponent.fromLegacyText("§8[Withering] Usychanie na bloku..."));

                ParticleEffect.SMOKE_NORMAL.display(initialTargetBlock.getLocation().add(0.5, 1.0, 0.5), 3, 0.2, 0.2,
                        0.2, 0.02);
                ParticleEffect.ASH.display(initialTargetBlock.getLocation().add(0.5, 1.0, 0.5), 3, 0.2, 0.2, 0.2, 0.02);

                if (phase1Ticks % 4 == 0) {
                    initialTargetBlock.getWorld().playSound(initialTargetBlock.getLocation(), Sound.BLOCK_GRASS_BREAK,
                            0.8f, 0.5f);
                }

                if (phase1Ticks >= maxTicks) {
                    this.cancel();
                    witherBlock(initialTargetBlock);
                    startSpreadingPhase();
                }
            }
        }.runTaskTimer(AmonPackPlugin.plugin, 0L, 1L);
    }

    private void startSpreadingPhase() {
        // Faza 2: Rozrastanie usychania z szybkością dostosowaną do gęstości roślin
        int period = Math.max(1, (int) (2.0 / densityMultiplier));
        int blocksPerTick = (int) Math.max(1, 2 * densityMultiplier);

        new BukkitRunnable() {
            private int spreadTicks = 0;

            @Override
            public void run() {
                spreadTicks++;
                SpecialTriggerManager.applySoftCooldownToToolbar(player);

                List<Block> candidates = GeneralMethods.getBlocksAroundPoint(centerLoc, (int) radius);
                if (!candidates.isEmpty()) {
                    for (int i = 0; i < blocksPerTick; i++) {
                        Block b = candidates.get(random.nextInt(candidates.size()));
                        if (isPlantbendableGround(b) && !witheredBlocks.contains(b)) {
                            witherBlock(b);
                        }
                    }
                }

                // Świadczenie hazardu martwych kolców i martwych krzewów
                for (ThornSpike spike : activeThornSpikes) {
                    spike.displayAndHazard(player, damage, Withering.this);
                }

                for (TempBlock deadBushTB : deadBushTempBlocks) {
                    if (deadBushTB != null && deadBushTB.getBlock().getType() == Material.DEAD_BUSH) {
                        Location dLoc = deadBushTB.getLocation().add(0.5, 0.5, 0.5);
                        ParticleEffect.SMOKE_NORMAL.display(dLoc, 1, 0.1, 0.1, 0.1, 0.01);

                        for (Entity entity : GeneralMethods.getEntitiesAroundPoint(dLoc, 1.2)) {
                            if (entity instanceof LivingEntity && entity.getUniqueId() != player.getUniqueId()) {
                                LivingEntity target = (LivingEntity) entity;
                                DamageHandler.damageEntity(target, damage / 3.0, Withering.this);
                                target.addPotionEffect(
                                        new PotionEffect(PotionEffectType.SLOWNESS, 30, 2, false, false));
                            }
                        }
                    }
                }

                if (spreadTicks >= 60) {
                    this.cancel();
                    finish();
                }
            }
        }.runTaskTimer(AmonPackPlugin.plugin, 0L, period);
    }

    private void witherBlock(Block b) {
        witheredBlocks.add(b);

        Material m = b.getType();
        boolean isLog = m.name().contains("LOG") || m.name().contains("WOOD");

        // pnie drzew mają 33% na stworzenie martwego kolca pionowo w bok (w losową
        // stronę świata)
        if (isLog) {
            if (random.nextDouble() < 0.33) {
                BlockFace[] faces = { BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST };
                BlockFace chosenFace = faces[random.nextInt(faces.length)];
                Vector dirVec = new Vector(chosenFace.getModX(), 0, chosenFace.getModZ());
                Location spikeStart = b.getLocation().add(0.5 + chosenFace.getModX() * 0.6, 0.5,
                        0.5 + chosenFace.getModZ() * 0.6);
                activeThornSpikes.add(new ThornSpike(spikeStart, dirVec));
            }
            return;
        }

        // Zamiana podłoża na PODZOL
        if (m == Material.GRASS_BLOCK || m == Material.DIRT || m == Material.MOSS_BLOCK) {
            new TempBlock(b, Material.PODZOL).setRevertTime(revertTime);
        }

        Block above = b.getRelative(0, 1, 0);
        Material abMat = above.getType();

        // Sprawdzanie rodzaju kwiatów do tworzenia pionowych martwych kolców (Tall:
        // 100%, Low: 33%)
        boolean isTallPlant = abMat == Material.TALL_GRASS || abMat == Material.LARGE_FERN
                || abMat == Material.ROSE_BUSH
                || abMat == Material.PITCHER_PLANT || abMat == Material.SUNFLOWER || abMat == Material.PEONY
                || abMat == Material.LILAC;
        boolean isLowPlant = PlantAbility.isPlant(above) || abMat == Material.SHORT_GRASS || abMat == Material.FERN
                || abMat == Material.POPPY || abMat == Material.DANDELION;

        if (isTallPlant || (isLowPlant && random.nextDouble() < 0.33)) {
            Location spikeStart = b.getLocation().add(0.5, 1.0, 0.5);
            activeThornSpikes.add(new ThornSpike(spikeStart, new Vector(0, 1, 0)));
        }

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
        return "2.5";
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
        return "Ususza celowany blok roślinny. Szybkość i ilość infekcji skaluje się do 300% z ilością roślin w obszarze. Wysokie kwiaty (100%), niskie kwiaty (33%) oraz pnie drzew (33% w bok) tworzą martwe kolce zadające obrażenia po wejściu.";
    }

    @Override
    public String getInstructions() {
        return "Spójrz na blok roślinny i naciśnij F (SWAP), aby go ususzyć!";
    }
}

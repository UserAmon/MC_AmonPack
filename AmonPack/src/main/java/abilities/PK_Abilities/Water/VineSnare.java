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
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class VineSnare extends WaterAbility implements AddonAbility, SpecialTriggerable {

    private enum State {
        PREVIEW,
        ERUPTION,
        STANDING,
        RETRACTING
    }

    private State state;
    private long cooldown;
    private int range;
    private double radius;
    private long chargeTime;
    private double damage;
    private long flowerRevertTime;
    private double centerY;

    private Location targetLoc;
    private List<LivingEntity> trappedEntities = new ArrayList<>();
    private List<Item> droppedVines = new ArrayList<>();
    private List<TempBlock> vineBlocks = new ArrayList<>();
    private int ticksElapsed = 0;

    public VineSnare(Player player) {
        super(player);

        VineSnare active = getAbility(player, VineSnare.class);
        if (active != null) {
            return;
        }

        this.cooldown = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Water.Plant.VineSnare.Cooldown", 9000L);
        this.range = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Water.Plant.VineSnare.Range", 20);
        this.radius = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Water.Plant.VineSnare.Radius", 15.0); // Zasięg na boki do 15 kratek
        this.chargeTime = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Water.Plant.VineSnare.ChargeTime", 3000L);
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
        this.centerY = targetLoc.getY();
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

        // Animacja kręgu pola nieprzerwanie do momentu zakwitnięcia kwiatów
        displayPreviewRingParticles();

        if (state == State.PREVIEW) {
            if (ticksElapsed % 5 == 0) {
                spawnGradualPlantsOnGround();
            }

            if (ticksElapsed % 8 == 0) {
                targetLoc.getWorld().playSound(targetLoc, Sound.BLOCK_GRASS_STEP, 0.7f, 0.8f);
            }

            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText("§a[VineSnare] Pnącza się rozrastają..."));

            if (ticksElapsed >= (chargeTime / 50)) {
                eruptVines();
            }
        } else if (state == State.STANDING) {
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
                }
            }

            if (ticksElapsed >= (chargeTime / 50) + 40) {
                startRetractingPhase();
            }
        } else if (state == State.RETRACTING) {
            // Przyciąganie wrogów do środka i W DÓŁ (setY -0.4 zamiast w górę)
            for (LivingEntity entity : trappedEntities) {
                if (entity != null && !entity.isDead() && entity.isValid()) {
                    Vector pull = targetLoc.clone().toVector().subtract(entity.getLocation().toVector()).normalize().multiply(0.65);
                    pull.setY(-0.4);
                    entity.setVelocity(pull);
                    ParticleEffect.SLIME.display(entity.getLocation(), 3, 0.2, 0.2, 0.2, 0.02);
                }
            }
        }
    }

    private void displayPreviewRingParticles() {
        Random rand = new Random();
        for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 10) {
            double outerR = radius + (rand.nextDouble() - 0.5) * 0.4;
            double x1 = outerR * Math.cos(angle);
            double z1 = outerR * Math.sin(angle);
            Location locOuter = targetLoc.clone().add(x1, 0.1, z1);
            ParticleEffect.COMPOSTER.display(locOuter, 1, 0.1, 0.1, 0.1, 0.01);

            double innerR = (radius * 0.45) + (rand.nextDouble() - 0.5) * 0.3;
            double x2 = innerR * Math.cos(angle);
            double z2 = innerR * Math.sin(angle);
            Location locInner = targetLoc.clone().add(x2, 0.1, z2);
            ParticleEffect.BLOCK_CRACK.display(locInner, 1, 0.1, 0.1, 0.1, 0.02, Material.JUNGLE_LEAVES.createBlockData());
        }
    }

    private void spawnGradualPlantsOnGround() {
        Random rand = new Random();
        List<Block> area = GeneralMethods.getBlocksAroundPoint(targetLoc, (int) radius);
        if (area.isEmpty()) return;

        Block randomGround = area.get(rand.nextInt(area.size()));
        if (isPlantbendableGround(randomGround)) {
            Block above = randomGround.getRelative(0, 1, 0);
            if (above.getY() <= centerY + 1.0 && above.getType() == Material.AIR && !TempBlock.isTempBlock(above)) {
                Item vineItem = above.getWorld().dropItem(above.getLocation().add(0.5, 0.2, 0.5), new ItemStack(Material.VINE));
                vineItem.setPickupDelay(32767);
                vineItem.setInvulnerable(true);
                droppedVines.add(vineItem);
            }
        }
    }

    private void eruptVines() {
        state = State.ERUPTION;

        targetLoc.getWorld().playSound(targetLoc, Sound.BLOCK_GRASS_BREAK, 1.4f, 0.7f);
        targetLoc.getWorld().playSound(targetLoc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.2f, 1.0f);

        Random rand = new Random();
        List<Vector> directions = new ArrayList<>();

        // 12 pocisków na boki (zasięg do 15 kratek)
        int projCount = 12;
        for (int p = 0; p < projCount; p++) {
            double angle = (Math.PI * 2 / projCount) * p;
            directions.add(new Vector(Math.cos(angle), 0.3 + (rand.nextDouble() * 0.2), Math.sin(angle)).normalize());
        }

        // Dodatkowe 2 pociski skierowane wyżej w górę
        directions.add(new Vector(0.2, 0.85, 0.2).normalize());
        directions.add(new Vector(-0.2, 0.85, -0.2).normalize());

        for (Vector dir : directions) {
            new BukkitRunnable() {
                Location projLoc = targetLoc.clone();
                double dist = 0;

                @Override
                public void run() {
                    dist += 0.8;
                    projLoc.add(dir.clone().multiply(0.8));

                    Block b = projLoc.getBlock();
                    if (b.getType() == Material.AIR) {
                        Material mat = rand.nextBoolean() ? Material.TWISTING_VINES : (rand.nextBoolean() ? Material.OAK_LEAVES : Material.VINE);
                        TempBlock tb = new TempBlock(b, mat);
                        tb.setRevertTime(6000L);
                        vineBlocks.add(tb);
                    }

                    for (Entity entity : GeneralMethods.getEntitiesAroundPoint(projLoc, 1.6)) {
                        if (entity instanceof LivingEntity && entity.getUniqueId() != player.getUniqueId()) {
                            LivingEntity le = (LivingEntity) entity;
                            if (!trappedEntities.contains(le)) {
                                DamageHandler.damageEntity(le, damage, VineSnare.this);
                                // Przyciąganie w dół (setY -0.4)
                                Vector pull = targetLoc.clone().toVector().subtract(le.getLocation().toVector()).normalize().multiply(0.8);
                                pull.setY(-0.4);
                                le.setVelocity(pull);
                                trappedEntities.add(le);
                            }
                        }
                    }

                    if (dist >= 15.0 || b.getType().isSolid()) {
                        cancel();
                    }
                }
            }.runTaskTimer(AmonPackPlugin.plugin, 0L, 1L);
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                if (state == State.ERUPTION) {
                    state = State.STANDING;
                }
            }
        }.runTaskLater(AmonPackPlugin.plugin, 20L);
    }

    private void startRetractingPhase() {
        state = State.RETRACTING;
        targetLoc.getWorld().playSound(targetLoc, Sound.BLOCK_VINE_STEP, 1.2f, 0.6f);

        new BukkitRunnable() {
            int step = 0;

            @Override
            public void run() {
                step++;
                int removeCount = Math.max(1, vineBlocks.size() / 15);
                for (int i = 0; i < removeCount && !vineBlocks.isEmpty(); i++) {
                    TempBlock tb = vineBlocks.remove(vineBlocks.size() - 1);
                    if (tb != null) {
                        ParticleEffect.BLOCK_CRACK.display(tb.getLocation(), 3, 0.1, 0.1, 0.1, 0.05, tb.getBlock().getBlockData());
                        tb.revertBlock();
                    }
                }

                if (step >= 25 || vineBlocks.isEmpty()) {
                    cancel();
                    finishAbility();
                }
            }
        }.runTaskTimer(AmonPackPlugin.plugin, 0L, 1L);
    }

    private void finishAbility() {
        Material[][] endPlantPairs = {
            {Material.SHORT_GRASS, Material.POPPY},
            {Material.FERN, Material.DANDELION},
            {Material.LILY_OF_THE_VALLEY, Material.BLUE_ORCHID},
            {Material.WITHER_ROSE, Material.ALLIUM},
            {Material.RED_TULIP, Material.AZURE_BLUET},
            {Material.PINK_TULIP, Material.CORNFLOWER}
        };
        Random rand = new Random();
        Material[] chosenFlowers = endPlantPairs[rand.nextInt(endPlantPairs.length)];

        List<Block> area = GeneralMethods.getBlocksAroundPoint(targetLoc, (int) radius);
        for (Block b : area) {
            if (isPlantbendableGround(b)) {
                Block above = b.getRelative(0, 1, 0);
                if (above.getY() <= centerY + 1.0 && above.getType() == Material.AIR && !TempBlock.isTempBlock(above)) {
                    Material flowerMat = chosenFlowers[rand.nextInt(chosenFlowers.length)];
                    TempBlock tb = new TempBlock(above, flowerMat);
                    tb.setRevertTime(flowerRevertTime);
                }
            }
        }

        targetLoc.getWorld().playSound(targetLoc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.2f);
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText("§a[VineSnare] Kwiaty zakwitły!"));

        cleanup();
    }

    private void cancelAbility() {
        cleanup();
    }

    private void cleanup() {
        for (TempBlock tb : vineBlocks) {
            if (tb != null) {
                tb.revertBlock();
            }
        }
        vineBlocks.clear();

        for (Item item : droppedVines) {
            if (item != null && item.isValid()) {
                item.remove();
            }
        }
        droppedVines.clear();

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
        return TriggerType.SWAP;
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
        super.remove();
    }

    @Override
    public String getDescription() {
        return "Tworzy krąg pnączy (15m), które po 3s wystrzeliwują macki (w tym 2 w górę), stoją w miejscu przez 2 sekundy, a następnie powoli się chowają ściągając wrogów w dół do środka i zakwitając kwiatami.";
    }

    @Override
    public String getInstructions() {
        return "Spójrz na ziemię roślinną i naciśnij F (SWAP), aby aktywować sidła pnączy!";
    }
}

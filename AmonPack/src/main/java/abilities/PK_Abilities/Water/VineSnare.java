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
    private double projectileRange;
    private int projectileCount;
    private double centerY;

    private Location targetLoc;
    private List<LivingEntity> trappedEntities = new ArrayList<>();
    private List<TempBlock> vineBlocks = new ArrayList<>();
    private int ticksElapsed = 0;
    private Random random = new Random();

    public VineSnare(Player player) {
        super(player);

        VineSnare active = getAbility(player, VineSnare.class);
        if (active != null) {
            return;
        }

        this.cooldown = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Water.Plant.VineSnare.Cooldown", 9000L);
        this.range = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Water.Plant.VineSnare.Range", 20);
        this.radius = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Water.Plant.VineSnare.Radius", 15.0);
        this.chargeTime = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Water.Plant.VineSnare.ChargeTime", 3000L);
        this.damage = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Water.Plant.VineSnare.Damage", 3.0);
        this.flowerRevertTime = AmonPackPlugin.getAbilitiesConfig().getLong("AmonPack.Water.Plant.VineSnare.FlowerRevertTime", 10000L);
        this.projectileRange = AmonPackPlugin.getAbilitiesConfig().getDouble("AmonPack.Water.Plant.VineSnare.ProjectileRange", 15.0);
        this.projectileCount = AmonPackPlugin.getAbilitiesConfig().getInt("AmonPack.Water.Plant.VineSnare.ProjectileCount", 14);

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
                || m == Material.MOSS_BLOCK || m == Material.FARMLAND
                || WaterAbility.isPlantbendable(player, m, false) || PlantAbility.isPlant(b);
    }

    @Override
    public void progress() {
        if (player == null || player.isDead() || !player.isOnline()) {
            cancelAbility();
            return;
        }

        ticksElapsed++;

        // Cząsteczki BLOCK_CRACK rozproszone losowo po całym obszarze wnętrza koła (BEZ HAPPY VILLAGER)
        displayScatteredGreenParticles();

        // Powolne i stopniowe terraformowanie oraz wyrastanie pojedynczych kwiatów w trakcie działania skilla
        if (ticksElapsed % 4 == 0) {
            terraformAndGradualFlowers();
        }

        if (state == State.PREVIEW) {
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
                        ParticleEffect.BLOCK_CRACK.display(pLoc, 2, 0.1, 0.1, 0.1, 0.05, Material.JUNGLE_LEAVES.createBlockData());
                    }

                    entity.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20, 10, false, false));
                    entity.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 20, 128, false, false));
                }
            }

            if (ticksElapsed >= (chargeTime / 50) + 40) {
                startRetractingPhase();
            }
        } else if (state == State.RETRACTING) {
            // Ściąganie wrogów do środka i W DÓŁ
            for (LivingEntity entity : trappedEntities) {
                if (entity != null && !entity.isDead() && entity.isValid()) {
                    Vector pull = targetLoc.clone().toVector().subtract(entity.getLocation().toVector()).normalize().multiply(0.65);
                    pull.setY(-0.4);
                    entity.setVelocity(pull);
                }
            }
        }
    }

    private void displayScatteredGreenParticles() {
        for (int i = 0; i < 8; i++) {
            double r = random.nextDouble() * radius;
            double angle = random.nextDouble() * Math.PI * 2;
            double x = r * Math.cos(angle);
            double z = r * Math.sin(angle);
            Location loc = targetLoc.clone().add(x, 0.1, z);
            ParticleEffect.BLOCK_CRACK.display(loc, 1, 0.1, 0.1, 0.1, 0.02, Material.JUNGLE_LEAVES.createBlockData());
        }
    }

    private void terraformAndGradualFlowers() {
        List<Block> area = GeneralMethods.getBlocksAroundPoint(targetLoc, (int) radius);
        if (area.isEmpty()) return;

        Block randomGround = area.get(random.nextInt(area.size()));
        if (randomGround != null) {
            // Terraformowanie: stone -> dirt -> grass_block
            if (randomGround.getType() == Material.STONE || randomGround.getType() == Material.COBBLESTONE || randomGround.getType() == Material.DEEPSLATE) {
                new TempBlock(randomGround, Material.DIRT).setRevertTime(flowerRevertTime);
            } else if (randomGround.getType() == Material.DIRT || randomGround.getType() == Material.COARSE_DIRT || randomGround.getType() == Material.PODZOL) {
                new TempBlock(randomGround, Material.GRASS_BLOCK).setRevertTime(flowerRevertTime);
            }

            // BEZWZGLĘDNA ZASADA: Kwiaty mogą pojawić się TYLKO bezpośrednio na DIRT lub GRASS_BLOCK!
            if (randomGround.getType() == Material.GRASS_BLOCK || randomGround.getType() == Material.DIRT) {
                Block above = randomGround.getRelative(0, 1, 0);
                if (above.getY() <= centerY + 1.0 && above.getType() == Material.AIR && !TempBlock.isTempBlock(above)) {
                    Material[] flowers = {Material.SHORT_GRASS, Material.POPPY, Material.DANDELION, Material.BLUE_ORCHID, Material.ALLIUM, Material.AZURE_BLUET};
                    Material chosen = flowers[random.nextInt(flowers.length)];
                    new TempBlock(above, chosen).setRevertTime(flowerRevertTime);
                }
            }
        }
    }

    private void eruptVines() {
        state = State.ERUPTION;

        targetLoc.getWorld().playSound(targetLoc, Sound.BLOCK_GRASS_BREAK, 1.4f, 0.7f);
        targetLoc.getWorld().playSound(targetLoc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.2f, 1.0f);

        List<Vector> directions = new ArrayList<>();

        // Wystrzał macki pnączy (zależny od opcji z konfigu: projectileCount i projectileRange)
        for (int p = 0; p < projectileCount; p++) {
            double angle = (Math.PI * 2 / projectileCount) * p;
            directions.add(new Vector(Math.cos(angle), 0.3 + (random.nextDouble() * 0.2), Math.sin(angle)).normalize());
        }

        // Dodatkowe 2 pociski skierowane bardziej w górę
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
                        // WYŁĄCZNIE WARPED_ROOTS oraz TWISTING_VINES (BEZ LIŚCI!)
                        Material mat = random.nextBoolean() ? Material.WARPED_ROOTS : Material.TWISTING_VINES;
                        TempBlock tb = new TempBlock(b, mat);
                        tb.setRevertTime(6000L);
                        vineBlocks.add(tb);
                    }

                    for (Entity entity : GeneralMethods.getEntitiesAroundPoint(projLoc, 1.6)) {
                        if (entity instanceof LivingEntity && entity.getUniqueId() != player.getUniqueId()) {
                            LivingEntity le = (LivingEntity) entity;
                            if (!trappedEntities.contains(le)) {
                                DamageHandler.damageEntity(le, damage, VineSnare.this);
                                Vector pull = targetLoc.clone().toVector().subtract(le.getLocation().toVector()).normalize().multiply(0.8);
                                pull.setY(-0.4);
                                le.setVelocity(pull);
                                trappedEntities.add(le);
                            }
                        }
                    }

                    if (dist >= projectileRange || b.getType().isSolid()) {
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
        // Dodatkowe wykwitanie dzikich kwiatów w miejscu zakwitu TYLKO na ziemi/trawie
        List<Block> area = GeneralMethods.getBlocksAroundPoint(targetLoc, (int) radius);
        for (Block b : area) {
            if (b.getType() == Material.GRASS_BLOCK || b.getType() == Material.DIRT) {
                Block above = b.getRelative(0, 1, 0);
                if (above.getY() <= centerY + 1.0 && above.getType() == Material.AIR && !TempBlock.isTempBlock(above)) {
                    Material[] flowers = {Material.SHORT_GRASS, Material.POPPY, Material.DANDELION, Material.BLUE_ORCHID, Material.ALLIUM, Material.AZURE_BLUET, Material.FERN};
                    Material chosen = flowers[random.nextInt(flowers.length)];
                    new TempBlock(above, chosen).setRevertTime(flowerRevertTime);
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
        return "Tworzy pole rozrastających się pnączy (15m), które po 3s wystrzeliwują macki z warped roots i twisting vines, ściągając wrogów w dół i wykwitając kwiatami na ziemi.";
    }

    @Override
    public String getInstructions() {
        return "Spójrz na ziemię roślinną i naciśnij F (SWAP), aby aktywować sidła pnączy!";
    }
}

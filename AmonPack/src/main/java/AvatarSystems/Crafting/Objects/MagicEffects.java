package AvatarSystems.Crafting.Objects;

import AvatarSystems.Crafting.CraftingMenager;
import AvatarSystems.Gathering.CombatMenager;
import abilities.*;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.Ability;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.ability.EarthAbility;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.attribute.AttributeModifier;
import com.projectkorra.projectkorra.attribute.AttributePriority;
import com.projectkorra.projectkorra.util.ParticleEffect;
import com.projectkorra.projectkorra.util.TempBlock;
import commands.Commands;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.FallingBlock;
import methods_plugins.Abilities.SoundAbility;
import methods_plugins.Methods;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.entity.EntityType;
import org.bukkit.World;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import static com.projectkorra.projectkorra.attribute.AttributeModifier.ADDITION;
import static com.projectkorra.projectkorra.attribute.AttributeModifier.SUBTRACTION;
import static methods_plugins.Abilities.SoundAbility.AfffectedEntities;
import static methods_plugins.Methods.getRandom;

public class MagicEffects {
    private final List<MagicEffectsConditions> conditions;
    private final List<ItemStack> cost;
    private final String name;
    private final List<String> lore;
    private final String id;
    private final boolean isMajor;
    private final boolean isItemEffect;
    private final boolean isArmorEffect;
    private final String scrollName;
    private final int scrollModelID;
    private long chargeTime;

    public MagicEffects(List<MagicEffectsConditions> conditions, List<ItemStack> cost, String name, List<String> lore,
            String id, boolean isMajor) {
        this(conditions, cost, name, lore, id, isMajor, false, false, null, 0, 2500);
    }

    public MagicEffects(List<MagicEffectsConditions> conditions, List<ItemStack> cost, String name, List<String> lore,
            String id, boolean isMajor, boolean isItemEffect, boolean isArmorEffect, String scrollName,
            int scrollModelID, long chargeTime) {
        this.conditions = conditions;
        this.cost = cost;
        this.name = name;
        this.lore = lore;
        this.id = id;
        this.isMajor = isMajor;
        this.isItemEffect = isItemEffect;
        this.isArmorEffect = isArmorEffect;
        this.scrollName = scrollName;
        this.scrollModelID = scrollModelID;
        this.chargeTime = chargeTime;
    }

    public boolean isArmorEffect() {
        return isArmorEffect;
    }

    public void ExecuteOnKilling(Entity victim, Player player, List<ItemStack> drops, int exp) {
        double ExtraExp = 0;
        double ExtraLoot = 0;
        switch (id) {
            case "Looting":
                ExtraLoot += 20;
                break;
            case "Expierience":
                ExtraExp += 30;
                break;
            case "Exp_Boost":
                ExtraExp += 10;
                break;
            case "Earth_Health_Boost_On_Kill":
                player.addPotionEffect(new PotionEffect(PotionEffectType.HEALTH_BOOST, 200, 1, false, false));
                break;
            case "Monster_Hunter":
                ExtraLoot += 10;
                break;
            case "Midas":
                if (getRandom(0, 10) > 5) {
                    Commands.ExecuteCommandExample example = new Commands.ExecuteCommandExample();
                    example.executeCommand("money give " + player.getName() + " " + (exp * 0.1));
                }
                break;
        }

        for (ItemStack itemdrops : drops) {
            if (new Random().nextInt(100) < ExtraLoot) {
                player.sendMessage("Extra loot z killa, sznasa twoja to " + ExtraLoot);
                HashMap<Integer, ItemStack> leftovers = player.getInventory().addItem(itemdrops);
                for (ItemStack leftover : leftovers.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), leftover);
                }
            }
        }
        if (new Random().nextInt(100) < ExtraExp) {
            player.sendMessage("dodatkowy exp, powinienes dostac " + exp + " a dostales " + (exp * 2)
                    + "bo twoja szansa to " + ExtraExp);
            player.giveExp(exp * 2);
        }
        CombatMenager.ExecuteKill(player, victim, (int) ((ExtraExp / 100) * 2));
    }

    public double ExecuteOnTakinHit(Entity attacker, Player player) {
        double DamageReduction = 0;
        switch (id) {
            case "Monster_Hunter":
                if (attacker instanceof Monster) {
                    DamageReduction = DamageReduction + 1;
                }
                break;
            case "MoltenShell":
                if (attacker instanceof LivingEntity) {
                    if (getRandom(0, 100) < 25) {
                        attacker.setFireTicks(60);
                    }
                }
                break;
            case "Repulse":
                if (attacker instanceof LivingEntity) {
                    Vector forceDir = GeneralMethods.getDirection(attacker.getLocation(),
                            player.getLocation().clone().subtract(0, 1, 0));
                    attacker.setVelocity(forceDir.clone().normalize().multiply(-1));
                }
                break;
            case "Adrenaline":
                if (getRandom(0, 100) < 20) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 1));
                }
                break;
            case "Earth_Resolve_Dmg_Taking":
                List<Block> NearBlocks = new ArrayList<>();
                for (Block b : GeneralMethods.getBlocksAroundPoint(player.getLocation(), 4)) {
                    if (b.getLocation().getY() <= player.getLocation().getY() + 1
                            && b.getLocation().distance(player.getLocation()) < 7
                            && EarthAbility.isEarthbendable(player, b)) {
                        NearBlocks.add(b);
                    }
                }
                if (NearBlocks.size() > 3) {
                    new EarthHammer(player, 0);
                    DamageReduction = DamageReduction + 1;
                    System.out.println("Test 32 earth   " + DamageReduction);
                }

                break;
            case "Slowness_Defense":
                if (player.hasPotionEffect(PotionEffectType.SLOWNESS)) {
                    DamageReduction += 1;
                }
                break;
            default:
                break;
        }
        return DamageReduction;
    }

    public double ExecuteOnHit(Entity victim, Player player) {
        double DamageBoosts = 0;
        switch (id) {
            case "Monster_Hunter":
                if (victim instanceof Monster) {
                    DamageBoosts = DamageBoosts + 1;
                }
                break;
            case "Earth_Damage_Boost_Absorb":
                if (player.hasPotionEffect(PotionEffectType.ABSORPTION)) {
                    DamageBoosts = DamageBoosts + 1;
                }
                break;
            case "Earth_Damage_Boost_Hight":
                if (player.getLocation().getY() > victim.getLocation().getY()) {
                    DamageBoosts = DamageBoosts + 1;
                }
                break;
            case "Fire_Damage_Boost":
                if (victim.getFireTicks() > 10) {
                    DamageBoosts = DamageBoosts + 1;
                }
                break;
            case "Fire_Speed_Boost":
                if (victim.getFireTicks() > 10) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 2, false, false));
                    victim.setFireTicks(0);
                }
                break;
            case "Knockback":
                Vector direction = victim.getLocation().toVector().subtract(player.getLocation().toVector())
                        .normalize();
                victim.setVelocity(direction.multiply(1).setY(0.45));
                break;
            case "Burrow":
                new SandWave(player, true, victim);
                break;
            case "Fire_Aspect":
                victim.setFireTicks(50);
                break;
            case "Smoke_Aspect":
                new SmokeSurge(player, true);
                break;
            case "Minor_Air_Sound_Damage_Buff":
                if (AfffectedEntities != null && !AfffectedEntities.keySet().isEmpty()
                        && AfffectedEntities.containsKey(victim)) {
                    DamageBoosts = DamageBoosts + 1;
                }
                break;
            case "Major_Air_Sound_Hit":
                new SoundCrash(player, victim, 0);
                break;
            case "Earth_Hammer_Aspect":
                new EarthHammer(player, 1);
                break;
            case "Minor_Water_Icy_Slowness_Hit":
                new IceThorn(player, victim, 2);
                break;
            case "Air_Thrust":
                victim.setVelocity(new Vector(0, 1, 0));
                new AirPressure(player, victim, 0);
                ParticleEffect.CLOUD.display(victim.getLocation(), 5, 0.5, 0.5, 0.5, 0.05);
                break;
            case "Air_Damage_Boost_Downward":
                DamageBoosts = DamageBoosts + 1;
                new AirPressure(player, victim, 1);
                break;
            case "Earth_1":
                Methods.spawnFallingBlocks(victim.getLocation(), Material.DIRT, 6, 1.5, player);
                break;
            case "Ice_Thorn_Ability_Aspect":
                new IceThorn(player, victim, 0);
                break;
            case "Ice_Encase":
                new IceThorn(player, victim, 1);
                break;
            case "Lightning_Aspect":
                Methods.LightningProjectile(victim.getLocation().clone().add(0, 1.5, 0), player);
                // player.setVelocity(player.getLocation().getDirection().add(new
                // Vector(0,0.4,0)).multiply(1));
                // player.addPotionEffect(new
                // PotionEffect(PotionEffectType.SPEED,40,2,false,false,false));
                break;
        }
        return DamageBoosts;
    }

    public String getName() {
        return id;
    }

    public static String serializeList(List<MagicEffects> effects) {
        if (effects == null || effects.isEmpty())
            return "";
        StringBuilder sb = new StringBuilder();
        for (MagicEffects effect : effects) {
            sb.append(effect.getName()).append(",");
        }
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    public static List<MagicEffects> deserializeList(String data) {
        List<MagicEffects> list = new ArrayList<>();
        if (data == null || data.isEmpty())
            return list;

        String[] names = data.split(",");
        for (String name : names) {
            MagicEffects effect = CraftingMenager.GetMagicEfectByName(name.trim());
            if (effect != null) {
                list.add(effect);
            } else {
                System.out.println("[Crafting] Nie znaleziono efektu o nazwie: " + name);
            }
        }
        return list;
    }

    public List<MagicEffectsConditions> getConditions() {
        return conditions;
    }

    public String getDisplayName() {
        return name;
    }

    public List<String> getLoreDescription() {
        return lore;
    }

    public boolean isMajorRune() {
        return isMajor;
    }

    public List<ItemStack> getCost() {
        return cost;
    }

    public static List<String> AffectedAbilities = new ArrayList<>(List.of("Torrent",
            "IceArch",
            "IceThorn",
            "Geyser",
            "FrostBreath",
            "AirSwipe",
            "AirBlade",
            "SonicBlast"));

    public boolean isItemEffect() {
        return isItemEffect;
    }

    public String getScrollName() {
        return scrollName;
    }

    public int getScrollModelID() {
        return scrollModelID;
    }

    public long getChargeTime() {
        return chargeTime;
    }

    public double ExecuteOnUse(Player player) {
        if (id.startsWith("Summon_Boss_")) {
            String bossId = id.replace("Summon_Boss_", "");
            Location location = player.getLocation().clone();
            StartRitual(location, () -> Mechanics.BossScrollManager.getInstance().summonBoss(location, bossId));
            return 0; // Return value might be cooldown or something, keeping 0 for now
        }
        double value = 0;
        switch (id) {
            case "Heal":
                player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + 4));
                break;
            case "Speed":
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 200, 1));
                break;
            case "Ice_Wall":
                Location location = player.getLocation().clone();
                StartRitual(location, () -> IceThorns(location, player));
                break;
            case "Summon_Undead":
                Location location1 = player.getLocation().clone();
                StartRitual(location1, () -> SummonZombies(location1));
                break;
            case "Meteor_Strike":
                Location location2 = player.getLocation().clone();
                StartRitual(location2, () -> SummonMeteor(location2));
                break;
            case "Sand_Tornado":
                Location location3 = player.getLocation().clone();
                Vector direction = player.getLocation().getDirection().setY(0).normalize().multiply(0.1); // Move speed
                StartRitual(location3, () -> SummonSandTornado(location3, direction));
                break;
        }
        return value;
    }

    private void IceThorns(Location startLoc, Player player) {
        int duration = 200; // 10 seconds
        double maxRadius = 6.0;

        new BukkitRunnable() {
            int ticks = 0;
            double currentRadius = 1.0;
            int currentHeight = 1;

            @Override
            public void run() {
                if (ticks >= duration) {
                    cancel();
                    return;
                }

                if (ticks % 20 == 0) {
                    if (currentRadius < maxRadius)
                        currentRadius += 0.5;
                    if (currentHeight < 5)
                        currentHeight++;
                }

                for (Block b : GeneralMethods.getBlocksAroundPoint(startLoc, currentRadius)) {
                    if (b.getType() != Material.AIR && b.getType() != Material.BEDROCK
                            && b.getType() != Material.BARRIER && !b.isLiquid()) {
                        if (b.getLocation().distance(startLoc) <= currentRadius) {
                            if (b.getType().isSolid() && b.getRelative(BlockFace.UP).getType() == Material.AIR) {
                                if (Math.random() < 0.3) {
                                    new TempBlock(b.getRelative(BlockFace.UP), Material.SNOW).setRevertTime(3000L);
                                }
                                if (Math.random() < 0.1) {
                                    new TempBlock(b, Material.PACKED_ICE).setRevertTime(4000L);
                                }
                            }
                        }
                    }
                }
                for (int i = 0; i < currentHeight; i++) {
                    Block center = startLoc.clone().add(0, i, 0).getBlock();
                    if (center.getType() == Material.AIR || !center.getType().isSolid()) {
                        new TempBlock(center, Material.BLUE_ICE).setRevertTime(5000L);
                    }
                    if (i == 0 && currentHeight > 3) {
                        for (Block b : GeneralMethods.getBlocksAroundPoint(center.getLocation(), 1.75)) {
                            if (b.getType() == Material.AIR && b.getY() <= (startLoc.getBlockY() + 1)) {
                                new TempBlock(b, Material.BLUE_ICE).setRevertTime(5000L);
                            }
                        }
                    }
                    if (i == 1 && currentHeight > 4) {
                        for (Block b : GeneralMethods.getBlocksAroundPoint(center.getLocation(), 1)) {
                            if (b.getType() == Material.AIR && b.getY() <= (startLoc.getBlockY() + 2)) {
                                new TempBlock(b, Material.BLUE_ICE).setRevertTime(5000L);
                            }
                        }
                    }
                }
                startLoc.getWorld().spawnParticle(Particle.SNOWFLAKE, startLoc.clone().add(0, 2, 0), 10, currentRadius,
                        2, currentRadius, 0.05);
                startLoc.getWorld().spawnParticle(Particle.CLOUD, startLoc.clone().add(0, 1, 0), 5, currentRadius / 2,
                        1, currentRadius / 2, 0.05);
                for (Entity entity : GeneralMethods.getEntitiesAroundPoint(startLoc, currentRadius)) {
                    if (entity instanceof LivingEntity) {
                        LivingEntity le = (LivingEntity) entity;
                        le.damage(0.5);
                        le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 2));
                        le.setFreezeTicks(100);
                    }
                }
                ticks++;
            }
        }.runTaskTimer(methods_plugins.AmonPackPlugin.plugin, 0L, 1L);
    }

    private void StartRitual(Location loc, Runnable onComplete) {
        new BukkitRunnable() {
            int ticks = 0;
            double angle = 0;
            double angle_2 = 60;

            @Override
            public void run() {
                if (ticks >= 80) {
                    onComplete.run();
                    cancel();
                    return;
                }
                double radius = 5;
                for (int i = 0; i < 3; i++) {
                    double currentAngle = angle + (i * (Math.PI * 2 / 3));
                    double currentAngle2 = angle_2 + (i * (Math.PI * 2 / 3));
                    double x = radius * Math.cos(currentAngle);
                    double z = radius * Math.sin(currentAngle);
                    Location particleLoc = loc.clone().add(x, 0.1, z);
                    Location particleLoc2 = loc.clone().add(radius * Math.cos(currentAngle2), 0.1,
                            radius * Math.sin(currentAngle2));

                    Particle.DustOptions dustOptions = new Particle.DustOptions(Color.PURPLE, 1);
                    loc.getWorld().spawnParticle(Particle.DUST, particleLoc, 3, 0.1, 0.1, 0.1, 0, dustOptions);
                    loc.getWorld().spawnParticle(Particle.DUST, particleLoc2, 3, 0.1, 0.1, 0.1, 0,
                            new Particle.DustOptions(Color.RED, 1));
                }
                angle += 0.1;
                angle_2 += 0.1;
                ticks++;
            }
        }.runTaskTimer(methods_plugins.AmonPackPlugin.plugin, 0L, 1L);
    }

    private void SummonZombies(Location loc) {
        World world = loc.getWorld();
        if (world == null)
            return;

        for (int i = 0; i < 3; i++) {
            Location spawnLoc = loc.clone().add((Math.random() - 0.5) * 4, 0, (Math.random() - 0.5) * 4);
            // Emerging animation
            world.spawnParticle(Particle.EXPLOSION, spawnLoc, 1);
            world.spawnParticle(Particle.SOUL_FIRE_FLAME, spawnLoc, 10, 0.5, 0.5, 0.5, 0.1);
            world.spawnEntity(spawnLoc, EntityType.ZOMBIE);
            world.spawnEntity(spawnLoc, EntityType.ZOMBIE);
        }
    }

    private void SummonMeteor(Location loc) {
        Location spawnLoc = loc.clone().add(0, 30, 0);
        // Smart Spawn: Check for ceiling
        for (int i = 1; i <= 30; i++) {
            Location check = loc.clone().add(0, i, 0);
            if (check.getBlock().getType().isSolid()) {
                spawnLoc = check.clone().subtract(0, 2, 0);
                break;
            }
        }

        FallingBlock meteor = loc.getWorld().spawnFallingBlock(spawnLoc, Material.MAGMA_BLOCK.createBlockData());
        meteor.setDropItem(false);
        meteor.setHurtEntities(true);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (meteor.isDead() || meteor.isOnGround()) {
                    // Impact
                    loc.getWorld().playSound(meteor.getLocation(), org.bukkit.Sound.ENTITY_GENERIC_EXPLODE, 4F, 1F);
                    loc.getWorld().spawnParticle(Particle.EXPLOSION, meteor.getLocation(), 1);
                    loc.getWorld().spawnParticle(Particle.LAVA, meteor.getLocation(), 20, 1, 1, 1, 0.5);

                    // Damage entities
                    for (Entity entity : GeneralMethods.getEntitiesAroundPoint(meteor.getLocation(), 4)) {
                        if (entity instanceof org.bukkit.entity.LivingEntity) {
                            ((org.bukkit.entity.LivingEntity) entity).damage(4);
                            entity.setFireTicks(50);
                        }
                    }
                    meteor.getLocation().getBlock().setType(Material.AIR);
                    for (Block b : GeneralMethods.getBlocksAroundPoint(meteor.getLocation(), 4)) {
                        if (b.getType() != Material.AIR && b.getType() != Material.BEDROCK) {
                            new TempBlock(b, Material.AIR).setRevertTime(10000);
                        }
                    }
                    meteor.remove();
                    cancel();
                } else {
                    meteor.getWorld().spawnParticle(Particle.FLAME, meteor.getLocation(), 5, 0.2, 0.2, 0.2, 0.05);
                    meteor.getWorld().spawnParticle(Particle.SMOKE, meteor.getLocation(), 3, 0.2, 0.2, 0.2, 0.05);
                }
            }
        }.runTaskTimer(methods_plugins.AmonPackPlugin.plugin, 0L, 1L);
    }

    private void SummonSandTornado(Location startLoc, Vector initdir) {
        int duration = 240;
        double maxRadius = 6.0;
        Vector direction = initdir.clone();
        Location currentLocation = startLoc.clone();

        new BukkitRunnable() {
            int ticks = 0;
            double angle = 0;
            double currentRadius = 1.5;

            @Override
            public void run() {
                if (ticks >= duration) {
                    cancel();
                    return;
                }

                currentLocation.add(direction);
                if (currentLocation.getBlock().getType() != Material.AIR
                        && currentLocation.getBlock().getType().isSolid()) {
                    currentLocation.add(0, 1, 0); // Climb up
                } else if (currentLocation.clone().subtract(0, 1, 0).getBlock().getType() == Material.AIR) {
                    currentLocation.subtract(0, 1, 0); // Fall down
                }

                Block ground = currentLocation.clone().subtract(0, 1, 0).getBlock();
                if (ground.getType() == Material.AIR || ground.isLiquid()) {
                    cancel();
                    return;
                }

                if (currentRadius < maxRadius) {
                    currentRadius += (maxRadius - 1.0) / duration;
                }

                for (Block b : GeneralMethods.getBlocksAroundPoint(currentLocation, currentRadius)) {
                    if (b.getType() != Material.AIR && b.getType() != Material.BEDROCK
                            && b.getType() != Material.BARRIER && !b.isLiquid()) {
                        if (b.getLocation().distance(currentLocation) <= currentRadius) {
                            if (b.getType().isSolid() && b.getRelative(BlockFace.UP).getType() == Material.AIR) {
                                new TempBlock(b, Material.SAND).setRevertTime(5000L);
                            }
                        }
                    }
                }

                for (int i = 0; i < 5 + (int) currentRadius; i++) { // Height grows slightly
                    double y = currentLocation.getY() + i;
                    double r = (0.5 + (i * 0.2)) * currentRadius; // Radius scales with currentRadius
                    double x = currentLocation.getX() + (r * Math.cos(angle + i));
                    double z = currentLocation.getZ() + (r * Math.sin(angle + i));
                    currentLocation.getWorld().spawnParticle(Particle.FALLING_DUST, x, y, z, 1, 0, 0, 0, 0,
                            Material.SAND.createBlockData());
                }
                angle += 0.5;

                for (Entity entity : GeneralMethods.getEntitiesAroundPoint(currentLocation, currentRadius)) {
                    if (entity instanceof LivingEntity) {
                        LivingEntity le = (LivingEntity) entity;
                        le.damage(1.0);
                        le.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 1));
                        le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 2));
                        if (le.getLocation().getBlock().getType() == Material.SAND) {
                            le.teleport(le.getLocation().subtract(0, 0.05, 0));
                        }
                    }
                }

                ticks++;
            }
        }.runTaskTimer(methods_plugins.AmonPackPlugin.plugin, 0L, 1L);
    }
}

package Mechanics;

//import AvatarSystems.ForestMenager;
import AvatarSystems.Gathering.CombatMenager;
import AvatarSystems.Gathering.FarmMenager;
import AvatarSystems.Gathering.ForestMenager;
import AvatarSystems.Gathering.MiningMenager;
import AvatarSystems.Levels.ElementTree;
import AvatarSystems.Levels.PlayerBendingBranch;
import AvatarSystems.Levels.PlayerLevelMenager;
//import AvatarSystems.Util_Objects.Forest;
import AvatarSystems.Perks.Objects.Perk;
import AvatarSystems.Perks.PerksMenager;
import AvatarSystems.Util_Objects.LevelSkill;
import AvatarSystems.Util_Objects.PlayerLevel;
//import Mechanics.PVP.newPvP;
import UtilObjects.Skills.SkillTree_Ability;
import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.board.BendingBoardManager;
import com.projectkorra.projectkorra.event.AbilityDamageEntityEvent;
import methods_plugins.Abilities.SoundAbility;
import methods_plugins.AmonPackPlugin;
import methods_plugins.Methods;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.*;


import static methods_plugins.AmonPackPlugin.ElementBasedOnSubElement;

public class Listeners implements Listener {
    /*
    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        Entity entity = event.getEntity();
       /* if (entity != null){
        for (AssaultDef A:AssaultMenager.listOfAssaultDef) {
            if (InArenaRange(event.getEntity().getLocation(),A.getArenaLocation(),A.getRange(),A.getRange())){
                if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
                    event.setCancelled(true);
                }
                if (event.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) {
                    event.setCancelled(true);
                }
                if (entity instanceof Player && !event.isCancelled()){
                    Player player = (Player) event.getEntity();
                    if (player.getHealth()-event.getDamage()<1){
                        event.setCancelled(true);
                        player.setGameMode(GameMode.SPECTATOR);
                        float Fspeed= player.getFlySpeed();
                        player.setFlySpeed(0);
                        new BukkitRunnable() {
                            int countdown =5;
                            @Override
                            public void run() {
                                if (countdown > 0) {
                                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText("Odrodzenie za: "+countdown));
                                    countdown = countdown-1;
                                } else {
                                    player.setGameMode(GameMode.SURVIVAL);
                                    player.teleport(A.getArenaLocation());
                                    player.setHealth(player.getMaxHealth());
                                    player.setFlySpeed(Fspeed);
                                    cancel();
                                }
                            }
                        }.runTaskTimer(AmonPackPlugin.plugin, 0L, 20L); // Odświeżanie co 1 sekundę (20 ticków)
                    }
                    List<Upgrades> PlayerUpgrades = new ArrayList<>();
                    if (A.IndividualUpgrades.get(player)!=null){
                        PlayerUpgrades = A.IndividualUpgrades.get(player);
                    }
                    if (GetUpgradeByNameFromPlayer("AirDodge", PlayerUpgrades)!=null){
                        if (GetUpgradeByNameFromPlayer("AirDodge",PlayerUpgrades).isUnlocked()){
                            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,40,5));
                            player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY,40,5));
                        }}
                }
            }
            break;
        }}
    }
    /*@EventHandler
    public void OnMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if(newPvP.playerinzone(player.getLocation())){
        ItemStack item = player.getInventory().getItemInMainHand();
        Location nearestChestLocation = newPvP.findNearestChestLocation(player.getLocation());
        player.setCompassTarget(nearestChestLocation);
        if (item.getType() == Material.COMPASS && item.getEnchantments().isEmpty()) {
            int distance = (int) Math.round(player.getLocation().distance(nearestChestLocation));
            String message = "Najbliższa skrzynia znajduje się: " + distance + " Bloków stąd.";
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
    }}}*/
        /*if (AmonPackPlugin.PvPEnabled){
            if (player.getWorld().equals(PvPLoc.getWorld())){
            if (playerinzone(PvPLoc,Radius,player.getLocation())){
                if (ActEvent.equalsIgnoreCase("Zar")){
                    if (isNearWater(player,25,WMats) && playerinzone(player.getLocation(),Radius,PvPLoc)) {
                        player.removePotionEffect(PotionEffectType.CONFUSION);
                    }
                }else if (ActEvent.equalsIgnoreCase("Chlod")){
                    if (isNearWater(player,25,FMats) && playerinzone(player.getLocation(),Radius,PvPLoc)) {
                        player.removePotionEffect(PotionEffectType.SLOW);
                    }
                }
                ItemStack item = player.getInventory().getItemInMainHand();
                Location nearestChestLocation = findNearestChestLocation(player.getLocation());
                player.setCompassTarget(nearestChestLocation);
                if (item.getType() == Material.COMPASS && item.getEnchantments().isEmpty()) {
                    int distance = (int) Math.round(player.getLocation().distance(nearestChestLocation));
                    String message = "Najbliższa skrzynia znajduje się: " + distance + " Bloków stąd.";
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
                }}else if (playerinzone(PvPLoc1,Radius1,player.getLocation())){
                ItemStack item = player.getInventory().getItemInMainHand();
                Location nearestChestLocation = findNearestChestLocation(player.getLocation());
                player.setCompassTarget(nearestChestLocation);
                if (item.getType() == Material.COMPASS && item.getEnchantments().isEmpty()) {
                    int distance = (int) Math.round(player.getLocation().distance(nearestChestLocation));
                    String message = "Najbliższa skrzynia znajduje się: " + distance + " Bloków stąd.";
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
                }}
            if (playerinzone(PvPLoc,Radius+1700,player.getLocation())){
                for (Location loc:PLocList) {
                    for (FallingChest fc:ChestList) {
                        if (fc.getType().equalsIgnoreCase("Parkour")){
                            if (fc.getEndLoc() == loc){
                                if (player.getWorld().equals(loc.getWorld()) &&player.getLocation().distance(loc) < 2){
                                    for (int LC = 0; LC < LootCounter; LC++) {
                                        for (int i = 0; i < fc.getLoot().size(); i++) {
                                            if (fc.getLootchance().get(i) >= Math.random()) {
                                                player.getInventory().addItem(Commands.QuestItemConfig(fc.getLoot().get(i)));
                                            }}}
                                    Commands.ExecuteCommandExample example = new Commands.ExecuteCommandExample();
                                    example.executeCommand("q event "+event.getPlayer().getName()+" DailyPvP.jesli_PvP_RTP");
                                    player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 80, 10));
                                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 80, 10));                    }}}}}
            }}
        }
    }*/
/*
    @EventHandler
    public void OnInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK||event.getAction() == Action.RIGHT_CLICK_AIR) {
            Player player = event.getPlayer();
            ItemStack itemInHand = player.getInventory().getItemInMainHand();
            for (Menagerie mena:ListOfAllMenageries) {
                if (mena.IsInMenagerie(event.getPlayer().getLocation())){
                    mena.ActivateByClick(event);
                    if (itemInHand.isSimilar(SpiritOrb) ) {
                        itemInHand.setAmount(itemInHand.getAmount() - 1);
                        player.getInventory().setItemInMainHand(itemInHand);
                        Location origin = player.getLocation().clone().add(0, 1, 0);
                        Location location = origin.clone();
                        Vector direction = player.getLocation().getDirection().clone();
                        direction.setY(direction.getY() + 0.4);
                        new BukkitRunnable() {
                            double t = 0;
                            double g = -0.5;
                            @Override
                            public void run() {
                                double x = direction.getX() * t;
                                double y = direction.getY() * t + 0.5 * g * t * t;
                                double z = direction.getZ() * t;
                                location.add(x, y, z);
                                Particle.DustOptions dustOptions = new Particle.DustOptions(Color.PURPLE, 1);
                                location.getWorld().spawnParticle(Particle.DUST, location, 5, 0.2, 0.2, 0.2, 0.3, dustOptions);
                                location.getWorld().spawnParticle(Particle.PORTAL, location, 5, 0.2, 0.2, 0.2, 0.3);
                                for (Entity HitBox : location.getWorld().getNearbyEntities(location, 1, 1, 1)) {
                                    for (Entity nearbyEntity : location.getWorld().getNearbyEntities(location, 2.5, 1, 2.5)) {
                                        if (nearbyEntity instanceof LivingEntity && !(nearbyEntity instanceof ArmorStand) && !(nearbyEntity instanceof Player)) {
                                            Vector direction = nearbyEntity.getLocation().toVector().subtract(location.clone().subtract(0, 3, 0).toVector()).normalize();
                                            ((LivingEntity) nearbyEntity).damage(4);
                                            location.getWorld().spawnParticle(Particle.EXPLOSION, location, 1, 0, 0, 0, 0);
                                            cancel();
                                        }}
                                    break;
                                }
                                if (origin.distance(location) > 25 || location.getBlock().getType().isSolid()) {
                                    location.getWorld().spawnParticle(Particle.EXPLOSION, location, 1, 0, 0, 0, 0);
                                    cancel();
                                }
                                t += 0.20;
                            }
                        }.runTaskTimer(AmonPackPlugin.plugin, 0L, 1L);
                        event.setCancelled(true);
                    }else if (itemInHand.isSimilar(SpiritBlade) ) {
                        Vector direction = player.getLocation().getDirection().clone();
                        direction.setY(direction.getY() + 0.5);
                        player.setVelocity(direction.multiply(1));
                    }else if (itemInHand.isSimilar(MoonBlade) ) {
                        if(player.hasPotionEffect(PotionEffectType.INVISIBILITY)){
                        player.removePotionEffect(PotionEffectType.INVISIBILITY);
                        Location origin = player.getLocation().clone().add(0, 1, 0);
                        Vector direction = player.getLocation().getDirection().clone();
                        direction.setY(direction.getY() + 0.3);
                        new BukkitRunnable() {
                            double t = 0;
                            final double radius = 2;
                            Location location = origin.clone();
                            @Override
                            public void run() {
                                double x = direction.getX() * t;
                                double y = direction.getY() * t + 0.5 * -0.4 * t * t;
                                double z = direction.getZ() * t;
                                location.add(x, y, z);
                                for (double angle = 0; angle < 2 * Math.PI; angle += Math.PI / 16) {
                                    double offsetX = radius * Math.cos(angle);
                                    double offsetZ = radius * Math.sin(angle);
                                    double offsetY = 0;
                                    Vector offset = new Vector(offsetX, offsetY, offsetZ);
                                    Location particleLocation = location.clone().add(offset);
                                    Particle.DustOptions dustOptions = new Particle.DustOptions(Color.PURPLE, 1);
                                    particleLocation.getWorld().spawnParticle(Particle.DUST, particleLocation, 1, 0, 0, 0, 0, dustOptions);
                                    particleLocation.getWorld().spawnParticle(Particle.PORTAL, particleLocation, 1, 0, 0, 0, 0);
                                }
                                for (Entity hitBox : location.getWorld().getNearbyEntities(location, radius, 1, radius)) {
                                    if (hitBox instanceof LivingEntity && !(hitBox instanceof ArmorStand) && !(hitBox instanceof Player)) {
                                        ((LivingEntity) hitBox).damage(3);
                                        hitBox.setVelocity(direction.clone().add(new Vector(0,0.6,0).multiply(0.8)));
                                        ((LivingEntity) hitBox).addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 50, 3, false, false));
                                    }
                                }
                                if (origin.distance(location) > 20 || location.getBlock().getType().isSolid()) {
                                    cancel();
                                }
                                t += 0.15;
                            }
                        }.runTaskTimer(AmonPackPlugin.plugin, 0L, 1L);
                        /*Methods.Spin(player);
                        ParticleEffect.CRIT_MAGIC.display(player.getLocation(), 5, 2, 2, 2, 0);
                        ParticleEffect.CRIT.display(player.getLocation(), 5, 2, 2, 2, 0);
                        for (Entity entity : GeneralMethods.getEntitiesAroundPoint(player.getLocation(), 4)) {
                            if (entity instanceof LivingEntity && !entity.isDead()) {
                                if (entity.getUniqueId() != player.getUniqueId()) {
                                    LivingEntity victim = (LivingEntity) entity;
                                    victim.damage(1);
                                }
                            }}*/
                    /*}}
                    break;
                }}

            /*if (!AmonPackPlugin.BuildingOnArenas) {
                Block block = event.getClickedBlock();
                if(block !=null){
                Forest forest = ForestMenager.GetForestByLocation(block.getLocation());
                if(forest!=null){
                    if(forest.getMaterials().contains(block.getType())){
                        if(block instanceof  Ageable&& ((Ageable) block).getAge()<7){
                            return;
                        }
                        forest.HandleForestInteract(player,block);
                        event.setCancelled(true);
                    }
                }}}*//*}
    }*/

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        if(player.getGameMode().equals(GameMode.CREATIVE)){
            return;
        }
        Block block = event.getClickedBlock();
        FarmMenager.CheckFarmBlock(block,player,true);
    }


    @EventHandler
    public void BlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if(player.getGameMode().equals(GameMode.CREATIVE)){
            return;
        }
        Block b = event.getBlock();
        MiningMenager.PlayerPlaceBlock(player,b);
        /*if (!AmonPackPlugin.BuildingOnArenas && event.getBlock().getWorld().equals(event.getPlayer().getWorld())){
            for (Mine mine:Mining.ListOfMines) {
                if(event.getBlock().getWorld().equals(mine.getLoc().getWorld())&&event.getBlock().getLocation().distance(mine.getLoc())<= mine.getRadius()){
                    event.setCancelled(true);
                    break;
                }}
            for (Menagerie mena:ListOfAllMenageries) {
                if (mena.IsInMenagerie(event.getBlock().getLocation())){
                    event.setCancelled(true);
                    break;
                }}
        }*/
    }
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if(player.getGameMode().equals(GameMode.CREATIVE)){
            return;
        }
        Block b = event.getBlock();
        if(FarmMenager.CheckFarmBlock(b,player,false)
                ||MiningMenager.PlayerBreakBlock(player,b)
                || ForestMenager.PlayerBreakBlock(player,b)){
            event.setCancelled(true);
        }
        /*if (event.getBlock().getWorld().equals(newPvP.Loc.getWorld())){
            if (b.getType() == Material.CHEST){
                event.setCancelled(true);
            }}
        if (!AmonPackPlugin.BuildingOnArenas && event.getBlock().getWorld().equals(event.getPlayer().getWorld())){
            for (Menagerie mena:ListOfAllMenageries) {
                if (mena.IsInMenagerie(event.getBlock().getLocation())){
                    event.setCancelled(true);
                    break;
                }}
        }*/
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if(event.getEntity().getKiller() != null){
        Player player = event.getEntity().getKiller();
        if (player.getGameMode().equals(GameMode.CREATIVE)) {
            return;
        }
            List<ItemStack> drops = new ArrayList<>(event.getDrops());
         event.getDrops().clear();
          for (ItemStack item : drops) {
                HashMap<Integer, ItemStack> leftovers = player.getInventory().addItem(item);
                for (ItemStack leftover : leftovers.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), leftover);
                }
            }
        Entity victim = event.getEntity();
        CombatMenager.ExecuteKill(player,victim);

           /* if(PerksMenager.PlayerConditionPerk(player,"TestowyPerk", Perk.PerkType.PASSIVE)) {
                player.sendMessage("Pomyslnie perk TESTOWY wporowadzono!");
            }
            if(PerksMenager.PlayerConditionPerk(player,"MiningKill", Perk.PerkType.PASSIVE)) {
                player.sendMessage("Pomyslnie perk MINING wporowadzono!");
            }*/

                if(PerksMenager.PlayerConditionPerk(player,"Combat_1_Explosion", Perk.PerkType.PASSIVE)){
            Location location = victim.getLocation();
            /*List<Entity> entities =
                    (List<Entity>) Objects.requireNonNull(location.getWorld()).getNearbyEntities(location,5,5,5,
                            (entity) -> {
                                return !entity.isDead() && entity!=player&& (!(entity instanceof Player) || !((Player)entity).getGameMode().equals(GameMode.SPECTATOR)) || entity instanceof ArmorStand && ((ArmorStand)entity).isMarker();
                            });*/
                    Methods.spawnFallingBlocks(location,Material.DIRT,6,1.5,player);

        }

    }}


    @EventHandler
    public void onCropTrample(EntityInteractEvent event) {
        if (event.getEntityType() != EntityType.PLAYER) return;
        if(PerksMenager.PlayerConditionPerk((Player) event.getEntity(),"Farming_1_Jumping", Perk.PerkType.PASSIVE)){
            Block block = event.getBlock();
            if (block.getType() == Material.FARMLAND || block.getBlockData() instanceof Ageable) {
                System.out.println("Zablokowano zniszczenie");
                event.setCancelled(true);
            }
        }else {
            Block block = event.getBlock();
            if (block.getType() == Material.FARMLAND || block.getBlockData() instanceof Ageable) {
                    event.getEntity().sendMessage("powinno zniszczyć boki");
        }
        }
    }

        /*else{
/*
        for (Menagerie mena:ListOfAllMenageries) {
            if (mena.IsInMenagerie(event.getEntity().getLocation())){
                if(event.getEntity().getKiller() != null){
                    Player p = event.getEntity().getKiller();
                    Entity Victim = event.getEntity();
                    for (Objectives obj:mena.getActiveEncounter().getActiveObjectivesList()) {
                        if(obj.isItemDropBoolean()){
                            Victim.getWorld().dropItem(Victim.getLocation(), obj.getItemDrop());
                        }}
                    List<String> upgrade = AmonPackPlugin.getPlayerUpgrades(p);
                        if (upgrade.contains("Overshield_1_Kill")){
                            Menagerie.addTemporaryHealth(p,2,15);
                        }
                        if (upgrade.contains("SpiritOrbs_1_Kill")){
                            for (int i = 0; i <= new Random().nextInt(5); i++) {
                                Victim.getWorld().dropItem(Victim.getLocation(), SpiritOrb);
                            }
                    }
                        if (upgrade.contains("Sword_1_Kill")){
                            if(!p.getInventory().contains(MoonBlade)){
                            if(new Random().nextBoolean()){
                                Victim.getWorld().dropItem(Victim.getLocation(), MoonBlade);
                                Victim.getWorld().dropItem(Victim.getLocation(), MoonBow);
                            }}}
                    }
                mena.ActivateByKill(event.getEntity());
                break;
                }

            }}
        /*if (playerinzone(PvPLoc, Radius+1700, event.getEntity().getLocation())) {
            if (event.getEntity() instanceof Player) {
                Player killedPlayer = (Player) event.getEntity();
                if (killedPlayer.getKiller() != null) {
                    Player killer = killedPlayer.getKiller();
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.sendMessage(ChatColor.GREEN+ killedPlayer.getName() + ChatColor.GREEN+" Został zabity przez Gracza: " + killer.getName());
                    }
                    for (String st : ChestList.get(0).getLoot()) {
                        killer.getInventory().addItem(Commands.QuestItemConfig(st));
                    }
                    if (fightParticipants.containsKey(killedPlayer) && playerBossBars.containsKey(killedPlayer)){
                        fightParticipants.remove(killedPlayer);
                        lastAttackTimes.remove(killedPlayer);
                        removePrivateBossBar(killedPlayer);
                        if (!fightParticipants.containsValue(killer)){
                            removePrivateBossBar(killer);
                        }
                    }
                }
            }
            if (ActEvent.equalsIgnoreCase("Boss")){
                for (Location loc:RDLocList) {
                    for (RandomEvents re: REventsList) {
                        if (re.getType().equalsIgnoreCase("RaidBoss")){
                            if ((event.getEntity().getType() == EntityType.VINDICATOR|| event.getEntity().getType() == EntityType.SKELETON|| event.getEntity().getType() == EntityType.HUSK) && playerinzone(loc,re.getArenaS(),event.getEntity().getLocation())){
                                for (Player p:Bukkit.getOnlinePlayers()) {
                                    if (p.getWorld().getName().equals(re.getBossLoc().getWorld().getName()) && playerinzone(loc,re.getArenaS(),p.getLocation())){
                                        for (int i = 0; i < re.getLoot().size(); i++) {
                                            if (re.getLootchance().get(i) >= Math.random()) {
                                                p.getInventory().addItem(Commands.QuestItemConfig(re.getLoot().get(i)));
                                            }}
                                        p.sendMessage(ChatColor.RED+"[Ogłoszenie]  "+ChatColor.DARK_PURPLE+"Udało wam się zabić Czempiona Króla! Rozkoszujcie się nagrodami!");
                                        //p.teleport(RTP());
                                        p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 30, 10));
                                        p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 30, 10));
                                    }}
                                break;
                            }}}
                }}}*/
/*
    @EventHandler
    public void onPlayerItemDamage(PlayerItemDamageEvent event) {
        if (event.getPlayer().getInventory().getItemInMainHand().isSimilar(MoonBlade)) {
            for (Menagerie mena:ListOfAllMenageries) {
                if (mena.IsInMenagerie(event.getPlayer().getLocation())) {
                    event.setCancelled(true);
                    break;
                }}}
    }*/
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if(event.getCurrentItem()!=null){
            Player p = (Player) event.getWhoClicked();
            /*
            if(Objects.equals(event.getInventory().getHolder(), ForestMenager.ForestHolder)){
                event.setCancelled(true);
                if (!AmonPackPlugin.BuildingOnArenas) {
                    ItemStack item = event.getCurrentItem();
                    Forest forest = ForestMenager.GetForestByLocation(event.getWhoClicked().getLocation());
                    if(forest!=null){
                        forest.HandleForestInvClick((Player) event.getWhoClicked(),item);
                        event.setCancelled(true);
                    }}
            }else*/
        if(Objects.equals(event.getInventory().getHolder(), PlayerLevelMenager.SkillDetails)){
            event.setCancelled(true);
            if(event.getCurrentItem().getType()==Material.BARRIER){
                PlayerLevelMenager.TryOpenPlayerLevel((Player) event.getWhoClicked());
            }
            if(event.getCurrentItem().getItemMeta().getDisplayName().contains("Poziom") && event.getCurrentItem().getType()!=Material.PLAYER_HEAD){
                LevelSkill.SkillType sktype = PlayerLevelMenager.GetSkillTypeByMaterial(event.getCurrentItem().getType());
                PlayerLevelMenager.ClaimReward(sktype,(Player) event.getWhoClicked(),event.getCurrentItem().getItemMeta().getDisplayName());
            }
        }else
        if(Objects.equals(event.getInventory().getHolder(), PlayerLevelMenager.Holder1)){
            event.setCancelled(true);
            LevelSkill.SkillType sktype = PlayerLevelMenager.GetSkillTypeByMaterial(event.getCurrentItem().getType());
            if(sktype!=null){
                PlayerLevel Level = PlayerLevelMenager.GetPlayerLevelFromList(event.getWhoClicked().getName());
                if(Level!=null){
                    PlayerLevelMenager.OpenSkillDetails(Level.GetSkillByType(sktype), (Player) event.getWhoClicked());
                }}
            Element ele = PlayerLevelMenager.GetElementByPlace(event.getSlot());
            if(ele!=null){

                PlayerBendingBranch playersBranch = AmonPackPlugin.levelsBending.GetBranchByPlayerName(p.getName());
                if(playersBranch==null){
                    p.sendMessage("Nie masz wybranego zywiołu! Przejdz samouczek!");
                    return;
                }
                Element element = playersBranch.getCurrentElement();

                if (element==ele) {
                    AmonPackPlugin.levelsBending.OpenBendingSkillMenu(playersBranch.getName());
                }else{
                    event.getWhoClicked().sendMessage(ChatColor.RED+"Nie masz wybranego tego zywiołu! Twój zywioł to: "+playersBranch.getCurrentElement());
                }}
        }else
            if(Objects.equals(event.getInventory().getHolder(), PlayerLevelMenager.BendingSkillMenu)){
                event.setCancelled(true);
                if (event.getCurrentItem().getType() == Material.CHEST) {
                    AmonPackPlugin.levelsBending.OpenSkillTreeMenuByElement(p,0);
                }
                if (event.getCurrentItem().getType() == Material.PAPER) {
                    AmonPackPlugin.levelsBending.OpenBindingMenu(p.getName(),event.getCurrentItem().getItemMeta().getDisplayName());
                }
                if (event.getCurrentItem().getType() == Material.PAPER && event.getCurrentItem().getItemMeta().getDisplayName().contains("Zamknij")) {
                    PlayerLevelMenager.TryOpenPlayerLevel((Player) event.getWhoClicked());
                }
            } else
                if(Objects.equals(event.getInventory().getHolder(), PlayerLevelMenager.BindingAbilitiesMenu)) {
                    event.setCancelled(true);
                Material clickedItem = event.getCurrentItem().getType();
                BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(p);
                if (clickedItem.equals(Material.PAPER)) {
                    bPlayer.bindAbility(Objects.requireNonNull(event.getClickedInventory().getItem(4).getItemMeta()).getDisplayName(), Integer.parseInt(event.getCurrentItem().getItemMeta().getDisplayName()));
                    bPlayer.saveAbility(Objects.requireNonNull(event.getClickedInventory().getItem(4)).getItemMeta().getDisplayName(), Integer.parseInt(event.getCurrentItem().getItemMeta().getDisplayName()));
                    AmonPackPlugin.levelsBending.OpenBendingSkillMenu(p.getName());
                }
                if (event.getCurrentItem().getType() == Material.PAPER && event.getCurrentItem().getItemMeta().getDisplayName().contains("Zamknij")) {
                    AmonPackPlugin.levelsBending.OpenBendingSkillMenu(p.getName());
                }
            }

            else if(Objects.equals(event.getInventory().getHolder(), PlayerLevelMenager.BendingSkillTree)){
                    event.setCancelled(true);
                PlayerBendingBranch playersBranch = AmonPackPlugin.levelsBending.GetBranchByPlayerName(p.getName());
                    if(playersBranch==null){
                        p.sendMessage("Nie masz wybranego zywiołu! Przejdz samouczek!");
                        return;
                    }
                Element element = playersBranch.getCurrentElement();
                ElementTree SelectedElement = AmonPackPlugin.levelsBending.GetElement(element);
                Material ClickedItem = event.getCurrentItem().getType();
                BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(p);

                    if (ClickedItem == Material.PAPER && event.getCurrentItem().getItemMeta().getDisplayName().contains("Powrot")) {
                        AmonPackPlugin.levelsBending.OpenBendingSkillMenu(p.getName());
                    }else
                    if (ClickedItem.equals(Material.CHEST)) {
                        for (int i = 0; i <= 9; i++) {
                            if(CoreAbility.getAbility(bPlayer.getAbilities().get(i))!=null&&(CoreAbility.getAbility(bPlayer.getAbilities().get(i)).getElement().equals(SelectedElement.getElement())
                                    || Objects.equals(ElementBasedOnSubElement(CoreAbility.getAbility(bPlayer.getAbilities().get(i)).getElement()), SelectedElement.getElement()))){
                                BendingBoardManager.getBoard(p).get().clearSlot(i);
                                bPlayer.getAbilities().remove(i);
                            }}
                        playersBranch.ClearAbilities(SelectedElement);
                        AmonPackPlugin.levelsBending.OpenSkillTreeMenuByElement(p,0);
                        bPlayer.removeUnusableAbilities();
                    }else
                    if (event.getSlot()==26) {
                        if (playersBranch.getCurrentPage()<(SelectedElement.getRows()/54)){
                            AmonPackPlugin.levelsBending.OpenSkillTreeMenuByElement(p, playersBranch.getCurrentPage()+1);
                        }}else
                if (event.getSlot()==35) {
                        if (playersBranch.getCurrentPage()>0){
                            AmonPackPlugin.levelsBending.OpenSkillTreeMenuByElement(p, playersBranch.getCurrentPage()-1);
                        }}else
                    if (ClickedItem.equals(Material.PAPER)) {
                        SkillTree_Ability STA = SelectedElement.getAbilities().stream().filter(abi->abi.getName().equalsIgnoreCase(event.getCurrentItem().getItemMeta().getDisplayName())).findFirst().orElse(null);
                        if (STA!=null) {
                                if (playersBranch.GetPoints(element) >= STA.getCost()) {
                                    if (new HashSet<>(playersBranch.getUnlockedAbilities()).containsAll(STA.getListOfPreAbility()) || STA.getListOfPreAbility().size()==0) {
                                        playersBranch.UnlockAbility(STA.getElement(),STA.getCost(),STA.getName());
                                        AmonPackPlugin.levelsBending.OpenSkillTreeMenuByElement(p, playersBranch.getCurrentPage());
                                    }}}
                }
            }
        //else
        /*if(newPvP.playerinzone(event.getWhoClicked().getLocation())) {
            for (FallingChest fc : newPvP.ChestList) {
                if (event.getView().getTitle().equalsIgnoreCase(fc.getName())) {
                    if (newPvP.isInventoryEmpty(event.getView().getTopInventory()) && event.getClickedInventory().getType() == InventoryType.CHEST) {
                        event.setCancelled(true);
                        event.getView().getTopInventory().getLocation().getBlock().setType(Material.AIR);
                        AmonPackPlugin.getPlayerMenager().AddPoints(LevelSkill.SkillType.COMBAT, (Player) event.getWhoClicked(), fc.getExpgranted(), ChatColor.AQUA + "Exp:");
                        //Commands.ExecuteCommandExample example = new Commands.ExecuteCommandExample();
                        //example.executeCommand("q event "+event.getWhoClicked().getName()+" DailyPvP.punkt_PvP");
                        newPvP.LastFallChest.removeIf(b -> b.getLocation().distance(event.getView().getTopInventory().getLocation()) < 5);
                    }
                }
            }
        }
        else*/
                /*
        if (event.getInventory().getHolder() != p){
            for (Menagerie mena:ListOfAllMenageries) {
                if (mena.IsInMenagerie(p.getLocation())){
                    event.setCancelled(true);
                    mena.OnInventoryClickMenagerie(event);
                    break;
                }}
            }*/
        }}
/*
    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        for (Menagerie mena:ListOfAllMenageries) {
            if (mena.IsInMenagerie(event.getPlayer().getLocation())){
                event.setCancelled(true);
                event.getPlayer().sendMessage("Nie możesz wyrzucić tego przedmiotu!");
                break;
            }}
    /*if(newPvP.playerinzone(event.getPlayer().getLocation())){
            ItemStack item = event.getItemDrop().getItemStack();
            if(QuestItems.ListOfAllQuestItems.contains(item) || item.getType()==Material.STONE){
                event.setCancelled(true);
                event.getPlayer().sendMessage(ChatColor.RED+"Nie mozesz wyrzucac tego przedmiotu na pvp");
            }
    }
    }*/
    @EventHandler
    public void onFallingBlockLand(EntityChangeBlockEvent event) {
        if (event.getEntity() instanceof FallingBlock) {
            if(Methods.SpawnedByMe.contains(event.getEntity())){
                event.setCancelled(true);
                event.getEntity().remove();
            }
        }
    }
    /*
    @EventHandler
    public void onChestOpen(InventoryOpenEvent event) {
        if (event.getInventory().getType() == org.bukkit.event.inventory.InventoryType.CHEST) {
            if (event.getPlayer() instanceof Player) {
                Player player = (Player) event.getPlayer();
                for (FallingChest fc: newPvP.ChestList) {
                    if (event.getView().getTitle().equalsIgnoreCase(fc.getName())) {
                        if (fc.getType().equalsIgnoreCase("Combat")){
                            for (Entity entity : event.getPlayer().getWorld().getNearbyEntities(event.getPlayer().getLocation(), 10, 10, 10)) {
                                if (entity.getType() == EntityType.VINDICATOR || entity.getType() == EntityType.HUSK || entity.getType() == EntityType.SKELETON) {
                                    event.setCancelled(true);
                                }}
                            if (event.getInventory().contains(new ItemStack(Material.BEDROCK))){
                                event.getInventory().remove(new ItemStack(Material.BEDROCK));
                                newPvP.SpawnMobs(event.getInventory().getLocation(), event.getView().getTitle());
                                event.setCancelled(true);
                            }}
                        /*if (fc.getType().equalsIgnoreCase("Command")){
                            if (event.getInventory().contains(new ItemStack(Material.BEDROCK))){
                                event.getInventory().remove(new ItemStack(Material.BEDROCK));
                                Commands.ExecuteCommandExample example = new Commands.ExecuteCommandExample();
                                example.executeCommand(fc.getCommand());
                                event.setCancelled(true);
                            }}
                        if (fc.getType().equalsIgnoreCase("Parkour")){
                            event.getView().getTopInventory().getLocation().getBlock().setType(Material.AIR);
                            LastFallChest.removeIf(b -> b.getLocation().distance(event.getView().getTopInventory().getLocation()) < 5);
                            event.setCancelled(true);
                            PvP.Parkour(player,event.getView().getTitle());
                        }
                        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100, 10)); // Speed for 10 seconds (200 ticks)
                    }}
            }}}*/


/*
    @EventHandler
    public void onEntityShootBow(EntityShootBowEvent event) {
        if (event.getEntity() instanceof Player && event.getBow() != null) {
             new BowAbility(event);
        }
    }
*/

/*
    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (event.getEntity() instanceof Arrow) {
            Arrow arrow = (Arrow) event.getEntity();
            Entity hitEntity = event.getHitEntity();
            if (hitEntity instanceof LivingEntity &&arrow.getCustomName()!=null&& arrow.getCustomName().equalsIgnoreCase("MoonArrow")) {
                //LivingEntity livingEntity = (LivingEntity) hitEntity;
                //livingEntity.setFireTicks(40);
                arrow.setDamage(1);
                arrow.setVelocity(arrow.getVelocity().multiply(0.3));
                arrow.remove();
                //arrow.setVelocity(new Vector().multiply(1));
                /*event.setCancelled(true);
                livingEntity.damage(2);

            }
        }
    }
/*
    @EventHandler
    public void onSmith(FurnaceSmeltEvent event) {

    }*/
/*
    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
       /* if (playerinzone(event.getEntity().getLocation(),Radius,PvPLoc) || playerinzone(event.getEntity().getLocation(),Radius1,PvPLoc1)){
            if (event.getDamager() instanceof Player && event.getEntity() instanceof Player) {
                Player victim = (Player) event.getEntity();
                Player attacker = (Player) event.getDamager();
                for (Location loc:RDLocList) {
                    for (RandomEvents re: REventsList) {
                        if (re.getType().equalsIgnoreCase("RaidBoss")){
                            if (playerinzone(loc,re.getArenaS(),victim.getLocation())){
                                event.setCancelled(true);
                                return;
                            }}}
                }
                if (fightParticipants.containsKey(victim)){
                    if (attacker == fightParticipants.get(victim)){
                        lastAttackTimes.put(attacker, System.currentTimeMillis());
                        lastAttackTimes.put(victim, System.currentTimeMillis());
                        checkFights();
                    }else{
                        attacker.sendMessage(ChatColor.YELLOW+"Zaatakowałeś Gracza: "+ChatColor.RED+victim.getName()+ChatColor.YELLOW+" ! Nie wylogywuj się!");
                        victim.sendMessage(ChatColor.YELLOW+"Zostałeś zaatakowany przez: "+ChatColor.RED+attacker.getName()+ChatColor.YELLOW+" ! Nie wylogywuj się!");
                        fightParticipants.put(victim,attacker);
                        lastAttackTimes.put(attacker, System.currentTimeMillis());
                        lastAttackTimes.put(victim, System.currentTimeMillis());
                        checkFights();
                    }}else{
                    victim.sendMessage(ChatColor.YELLOW+"Zostałeś zaatakowany przez: "+ChatColor.RED+attacker.getName()+ChatColor.YELLOW+" ! Nie wylogywuj się!");
                    attacker.sendMessage(ChatColor.YELLOW+"Zaatakowałeś Gracza: "+ChatColor.RED+victim.getName()+ChatColor.YELLOW+" ! Nie wylogywuj się!");
                    fightParticipants.put(victim,attacker);
                    lastAttackTimes.put(attacker, System.currentTimeMillis());
                    lastAttackTimes.put(victim, System.currentTimeMillis());
                    checkFights();
                }}else if (event.getEntity() instanceof Player && event.getDamager().getType() == EntityType.VINDICATOR|| event.getDamager().getType() == EntityType.SKELETON|| event.getDamager().getType() == EntityType.HUSK){
                Entity attacker = event.getDamager();
                Player victim = (Player) event.getEntity();
                if (fightParticipants.containsKey(victim)){
                    fightParticipants.put(victim,victim);
                    lastAttackTimes.put(victim, System.currentTimeMillis());
                    checkFights();
                }else{
                    victim.sendMessage(ChatColor.YELLOW+"Zostałeś zaatakowany przez: "+ChatColor.RED+attacker.getName()+ChatColor.YELLOW+" ! Nie wylogywuj się!");
                    fightParticipants.put(victim,victim);
                    lastAttackTimes.put(victim, System.currentTimeMillis());
                    checkFights();
                }}else if (event.getDamager() instanceof Player &&( event.getEntity().getType() == EntityType.VINDICATOR|| event.getEntity().getType() == EntityType.SKELETON|| event.getEntity().getType() == EntityType.HUSK)){
                Player attacker = (Player)event.getDamager();
                if (fightParticipants.containsKey(attacker)){
                    fightParticipants.put(attacker,attacker);
                    lastAttackTimes.put(attacker, System.currentTimeMillis());
                    checkFights();
                }else{
                    fightParticipants.put(attacker,attacker);
                    lastAttackTimes.put(attacker, System.currentTimeMillis());
                    checkFights();
                }}}
            if(event.getDamager() instanceof Player){
        Player p = (Player)event.getDamager();
        Entity Victim = event.getEntity();
        List<String> upgrade = AmonPackPlugin.getPlayerUpgrades(p);
        for (Menagerie mena:ListOfAllMenageries) {
            if (mena.IsInMenagerie(p.getLocation())){
                if(p.getInventory().getItemInMainHand().isSimilar(MoonBow)){
                    BowAbility babi =new BowAbility(p, BowAbility.AbilityType.MoonBow);
                    if(babi.GetStacks(p)==0){
                        event.setDamage(4);
                        Vector dir = p.getLocation().getDirection();
                        p.setVelocity(dir.subtract(new Vector (0,0.6,0)).multiply(-0.8));
                    }else{
                        event.setDamage(1);
                    }
                }
                if(p.getInventory().getItemInMainHand().isSimilar(MoonBlade)){
                    event.setDamage(2);
                    if(!p.hasPotionEffect(PotionEffectType.INVISIBILITY)){
                    Vector dir = p.getLocation().getDirection();
                    p.setVelocity(dir.add(new Vector (0,0.6,0)).multiply(1.1));
                    p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 60, 1, false, false));
                }}

                for (Mechanics.Skills.Upgrades UPV: UpgradesMenager.MenagerieUpgradesList) {
                    if (upgrade.contains("SpiritOrbs_1_Dmg") && UPV.getType() == Mechanics.Skills.Upgrades.MenagerieUpgradeType.BUFF){
                        Random rand = new Random();
                        int chance = rand.nextInt(10);
                        if (chance < 3) {
                            ItemStack buddingAmethyst = new ItemStack(Material.BUDDING_AMETHYST);
                            ItemMeta meta = buddingAmethyst.getItemMeta();
                            if (meta != null) {
                                meta.setDisplayName(ChatColor.LIGHT_PURPLE+"Duchowa Kula");
                                buddingAmethyst.setItemMeta(meta);
                                Victim.getWorld().dropItem(Victim.getLocation(), buddingAmethyst);
                            }}
                    }
                    if (upgrade.contains("Ignitions_1_Buff") && UPV.getType() == Mechanics.Skills.Upgrades.MenagerieUpgradeType.BUFF){
                            if (Victim.getFireTicks()>0){
                                if(!event.getCause().equals(EntityDamageEvent.DamageCause.FIRE_TICK)&& !event.getCause().equals(EntityDamageEvent.DamageCause.FIRE)){
                                    if(FireDamageTimer.getInstances().keySet().contains(Victim)){
                                        FireDamageTimer.getInstances().keySet().remove(Victim);
                                        Victim.setFireTicks(Victim.getFireTicks()+60);
                                    }
                                    ((LivingEntity) Victim).damage(1);
                                    if (Victim.getFireTicks()<60){
                                        Victim.setFireTicks(Victim.getFireTicks()+20);
                                    }
                            }}
                    }
                    if (upgrade.contains("Dmg_1_Buff") && UPV.getType() == Mechanics.Skills.Upgrades.MenagerieUpgradeType.BUFF){
                        if(!event.getCause().equals(EntityDamageEvent.DamageCause.FIRE_TICK)&& !event.getCause().equals(EntityDamageEvent.DamageCause.FIRE)){
                            if(FireDamageTimer.getInstances().keySet().contains(Victim)){
                                FireDamageTimer.getInstances().keySet().remove(Victim);
                            }
                            ((LivingEntity) Victim).damage(1);
                    }}
                }
                break;
            }}}

    }


    private final Map<UUID, ItemStack[]> worldAInventories = new HashMap<>();

    @EventHandler
    public void onPlayerChangeWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        if(isInMultiworldsFolder(event.getFrom().getName()) && !isInMultiworldsFolder(event.getPlayer().getWorld().getName())){
            UUID playerId = player.getUniqueId();
            if (worldAInventories.containsKey(playerId)) {
                player.getInventory().setContents(worldAInventories.get(playerId));
            }
            worldAInventories.remove(player.getUniqueId());
        }
        if(!isInMultiworldsFolder(event.getFrom().getName()) && isInMultiworldsFolder(event.getPlayer().getWorld().getName())){
            UUID playerId = player.getUniqueId();
            worldAInventories.put(playerId, player.getInventory().getContents());
            player.getInventory().clear();
        }
    }
    private boolean isInMultiworldsFolder(String worldName) {
        if (worldName.startsWith("MultiWorlds")){
            File worldFolder = new File(worldName);
            if (worldFolder.isDirectory()) {
                return true;
            }}
        return false;
    }
*/
/*
    @EventHandler
    public void cooldown(PlayerCooldownChangeEvent event) {
        if (event.getCooldown()>0 && (event.getAbility().equalsIgnoreCase("FireBlast")||event.getAbility().equalsIgnoreCase("AirSwipe"))){
            /*for (AssaultDef A:AssaultMenager.listOfAssaultDef) {
            if (InArenaRange(event.getPlayer().getLocation(),A.getArenaLocation(),A.getRange(),A.getRange())) {
                Player player = event.getPlayer();
                BendingPlayer bp = BendingPlayer.getBendingPlayer(event.getPlayer());
                List<Upgrades> PlayerUpgrades = new ArrayList<>();
                if (A.IndividualUpgrades.get(player)!=null){
                    PlayerUpgrades = A.IndividualUpgrades.get(player);
                }
                if (event.getAbility().equalsIgnoreCase("FireBlast")){
                    if (event.getCooldown()!= 2500){
                    if (GetUpgradeByNameFromPlayer("FireBlastUpgrade1", PlayerUpgrades)!=null){
                        if (GetUpgradeByNameFromPlayer("FireBlastUpgrade1",PlayerUpgrades).isUnlocked()){
                            event.setCancelled(true);
                            bp.addCooldown("FireBlast",2500);
                        }}}}
                if (event.getAbility().equalsIgnoreCase("AirSwipe")){
                    if (event.getCooldown()!= 5000){
                    if (GetUpgradeByNameFromPlayer("AirSwipeUpgrade1", PlayerUpgrades)!=null){
                        if (GetUpgradeByNameFromPlayer("AirSwipeUpgrade1",PlayerUpgrades).isUnlocked()){
                            event.setCancelled(true);
                            bp.addCooldown("AirSwipe",5000);
                        }}}}
            }break;
            }*///}
    //}

/*
    @EventHandler
    public void test(AbilityStartEvent event) {
        Player p = event.getAbility().getPlayer();
        List<String> upgrade = AmonPackPlugin.getPlayerUpgrades(p);
        for (Menagerie mena:ListOfAllMenageries) {
            if (mena.IsInMenagerie(event.getAbility().getPlayer().getLocation())){
                for (Mechanics.Skills.Upgrades UPV: UpgradesMenager.MenagerieUpgradesList) {
                    if (upgrade.contains(UPV.getName()) && event.getAbility().getName().equalsIgnoreCase(UPV.getAbilityName())){
                        if (UPV.getType() == Mechanics.Skills.Upgrades.MenagerieUpgradeType.ABILITYBUFF){
                            UPV.ApplyEffects((CoreAbility) event.getAbility());
                            break;
                        }}}
                break;
            }}
/*
        for (AssaultDef A:AssaultMenager.listOfAssaultDef) {
            if (InArenaRange(event.getAbility().getPlayer().getLocation(),A.getArenaLocation(),A.getRange(),A.getRange())) {
                try{
            Player player = event.getAbility().getPlayer();
            List<Upgrades> PlayerUpgrades = new ArrayList<>();
            if (A.IndividualUpgrades.get(player)!=null){
                PlayerUpgrades = A.IndividualUpgrades.get(player);
            }
            if (event.getAbility().getName().equalsIgnoreCase("Torrent")){
            if (GetUpgradeByNameFromPlayer("TorrentUpgrade1", PlayerUpgrades)!=null){
            if (GetUpgradeByNameFromPlayer("TorrentUpgrade1",PlayerUpgrades).isUnlocked()){
                CoreAbility coreAbility = (CoreAbility) event.getAbility();
                coreAbility.addAttributeModifier(Attribute.DAMAGE, 3, ADDITION, AttributePriority.LOW);
                coreAbility.addAttributeModifier(Attribute.RANGE, 5, ADDITION, AttributePriority.LOW);
            }}}
            if (event.getAbility().getName().equalsIgnoreCase("FireBlast")){
            if (GetUpgradeByNameFromPlayer("FireBlastUpgrade1", PlayerUpgrades)!=null){
            if (GetUpgradeByNameFromPlayer("FireBlastUpgrade1",PlayerUpgrades).isUnlocked()){
                CoreAbility coreAbility = (CoreAbility) event.getAbility();
                try {
                    coreAbility.addAttributeModifier(Attribute.CHARGE_DURATION, 3, AttributeModifier.DIVISION, AttributePriority.LOW);
                    coreAbility.addAttributeModifier(Attribute.COOLDOWN, 5000, AttributeModifier.ADDITION, AttributePriority.HIGH);
                }catch (Exception ex){
                    coreAbility.addAttributeModifier(Attribute.DAMAGE, 2, ADDITION, AttributePriority.HIGH);
                }
            }}}
            if (event.getAbility().getName().equalsIgnoreCase("AirSwipe")){
            if (GetUpgradeByNameFromPlayer("AirSwipeUpgrade1", PlayerUpgrades)!=null){
            if (GetUpgradeByNameFromPlayer("AirSwipeUpgrade1",PlayerUpgrades).isUnlocked()){
                CoreAbility coreAbility = (CoreAbility) event.getAbility();
                coreAbility.addAttributeModifier(Attribute.RANGE, 5, AttributeModifier.ADDITION, AttributePriority.LOW);
                coreAbility.addAttributeModifier(Attribute.COOLDOWN, 5000, AttributeModifier.ADDITION, AttributePriority.HIGH);
                coreAbility.addAttributeModifier(Attribute.DAMAGE, 2, ADDITION, AttributePriority.HIGH);
                coreAbility.addAttributeModifier(Attribute.CHARGE_DURATION, 1500, SUBTRACTION, AttributePriority.LOW);
            }}}
        }catch(Exception ex){
            System.out.println("Error   "+ex.getMessage());
        }}}*/

    //}
    @EventHandler
    public void test(AbilityDamageEntityEvent event) {
        if (event.getAbility().getName().equalsIgnoreCase("SonicBlast")){
            event.setCancelled(true);
            SoundAbility.HandleDamage(event.getEntity(),10);
        }
            /*for (AssaultDef A:AssaultMenager.listOfAssaultDef) {
                if (InArenaRange(event.getAbility().getPlayer().getLocation(),A.getArenaLocation(),A.getRange(),A.getRange())) {
                    Player player = event.getAbility().getPlayer();
                        List<Upgrades> PlayerUpgrades = new ArrayList<>();
                        if (A.IndividualUpgrades.get(player)!=null){
                            PlayerUpgrades = A.IndividualUpgrades.get(player);
                        }
                    if (event.getAbility().getNagme().equalsIgnoreCase("FireBlast")){
                            if (GetUpgradeByNameFromPlayer("FireBlastUpgrade1", PlayerUpgrades)!=null){
                                if (GetUpgradeByNameFromPlayer("FireBlastUpgrade1",PlayerUpgrades).isUnlocked()){
                                    if (!event.getEntity().isVisualFire() && event.getDamage()>2){
                                        event.getEntity().setFireTicks(40);
                                    }
                                }
                    }}
                }break;
            }*/
    }


}

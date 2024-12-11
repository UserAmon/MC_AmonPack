package OLD.Assault;
/*
import methods_plugins.AmonPackPlugin;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;


public class AssaultMethods {
    static List<Upgrades> Upgrades = new ArrayList<>();

    public static void CreateUpGrades(){
        //Ulepszenia
        Upgrades.add(new Upgrades(Material.REDSTONE_BLOCK,"Heal",0,50,true,List.of(ChatColor.RED+"Ulecz życie strefy",ChatColor.DARK_PURPLE+"Koszt: 50")));
        Upgrades.add(new Upgrades(Material.FIRE_CHARGE,"ZoneExplosion",1,100,false,List.of(ChatColor.RED+"Powoduje Wybuch przy otrzymaniu obrażen strefy",ChatColor.DARK_PURPLE+"Koszt: 100")));
        Upgrades.add(new Upgrades(Material.GOLD_BLOCK,"Score",8,0,false,List.of("")));
        Upgrades.add(new Upgrades(Material.COMPASS,"Reroll",7,100,true,List.of(ChatColor.RED+"Wybierz kolejne Wzmocnienie",ChatColor.DARK_PURPLE+"Koszt: 100")));

        //Spirit Charges
        Upgrades SpiritOrbs = new Upgrades(Material.BUDDING_AMETHYST,"SpiritOrbs",ChatColor.LIGHT_PURPLE+"Duchowe Kule (Obrażenia)",0,0,false,List.of(ChatColor.RED+"Przy zadaniu obrażen, daje szanse na drop Duchowych Ładunków"),false,true,null);
        Upgrades SpiritOrbsOnKill = new Upgrades(Material.BUDDING_AMETHYST,"SpiritOrbsKill",ChatColor.LIGHT_PURPLE+"Duchowe Kule (Zabójstwo)",0,0,false,List.of(ChatColor.RED+"Przy zabiciu wroga, wylecą z niego Duchowe Ładunki"),false,true,null);
        Upgrades SpiritOrbsOnWave = new Upgrades(Material.BUDDING_AMETHYST,"SpiritOrbsWave",ChatColor.LIGHT_PURPLE+"Duchowe Kule (Runda)",0,0,false,List.of(ChatColor.RED+"Przy rozpoczęciu nowej rundy, otrzymaj Duchowe Ładunki"),false,true,null);

        Upgrades.add(SpiritOrbs);
        Upgrades.add(SpiritOrbsOnKill);
        Upgrades.add(SpiritOrbsOnWave);
        List<Upgrades>OrbsConditions = List.of(SpiritOrbs,SpiritOrbsOnKill,SpiritOrbsOnWave);
        Upgrades.add(new Upgrades(Material.BUDDING_AMETHYST,"SpiritOrbs Fire",ChatColor.RED+"Podpalające Kule",0,0,false,List.of(ChatColor.RED+"Duchowe Ładunki podpalają wrogów"),false,true,OrbsConditions));
        Upgrades.add(new Upgrades(Material.BUDDING_AMETHYST,"SpiritOrbs Air",ChatColor.GRAY+"Przyciągające Kule",0,0,false,List.of(ChatColor.RED+"Duchowe Ładunki przyciągają wrogów"),false,true,OrbsConditions));
        Upgrades.add(new Upgrades(Material.BUDDING_AMETHYST,"SpiritOrbsBoost",ChatColor.LIGHT_PURPLE+"Wzmiecnienie Duchowych Kuli",0,0,false,List.of(ChatColor.RED+"Zwiększa zasięg i obrażenia Duchowych Ładunków"),false,true,OrbsConditions));

        //Powietrze
        Upgrades.add(new Upgrades(Material.CHAINMAIL_BOOTS,"AirDodge",ChatColor.GRAY+"Unik Wiatru",0,0,false,List.of(ChatColor.RED+"Otrzymanie obrażeń zapewnia krótką szybkość i niewidzialność"),false,true,null));
        Upgrades.add(new Upgrades(Material.COBWEB,"AirBlast",ChatColor.GRAY+"Odblokuj - AirBlast",0,0,false,List.of(ChatColor.RED+"Odblokuj Zdolnosc"),true,true,null));
        Upgrades.add(new Upgrades(Material.COBWEB,"AirPressure",ChatColor.GRAY+"Odblokuj - AirPressure",0,0,false,List.of(ChatColor.RED+"Odblokuj Zdolnosc"),true,true,null));

        Upgrades.add(new Upgrades(Material.BONE_MEAL,"AirSwipeUpgrade1",ChatColor.RED+"Ulepsz zdolonść - AirSwipe",0,0,false,List.of(ChatColor.GREEN+"Dmg+2",ChatColor.GREEN+"ChargeTime=>1s",ChatColor.GREEN+"Range+5",ChatColor.RED+"Cooldown+5s"),false,true,null));

        //Woda
        Upgrades.add(new Upgrades(Material.WATER_BUCKET,"IceArch",ChatColor.BLUE+"Odblokuj - IceArch",0,0,false,List.of(ChatColor.RED+"Odblokuj Zdolnosc"),true,true,null));
        Upgrades Torrent = new Upgrades(Material.WATER_BUCKET,"Torrent",ChatColor.BLUE+"Odblokuj - Torrent",0,0,false,List.of(ChatColor.RED+"Odblokuj Zdolnosc"),true,true,null);
        Upgrades.add(Torrent);

        Upgrades.add(new Upgrades(Material.BLUE_ICE,"TorrentUpgrade1",ChatColor.BLUE+"Ulepsz zdolonść - Torrent",0,0,false,List.of(ChatColor.GREEN+"Dmg+3",ChatColor.GREEN+"Range+5",ChatColor.RED+"Cooldown+3s"),false,true,List.of(Torrent)));

        //Ogien
        Upgrades.add(new Upgrades(Material.BLAZE_ROD,"FireDmgBoost",ChatColor.RED+"Wysoka Temperatura",0,0,false,List.of(ChatColor.RED+"Zadanie obrażen podpalonemu celowi zwiększa obrażenia i przedłuża podpalenie"),false,true,null));
        Upgrades.add(new Upgrades(Material.FIRE_CHARGE,"SmokeDaggers",ChatColor.RED+"Odblokuj - SmokeDaggers",0,0,false,List.of(ChatColor.RED+"Odblokuj Zdolnosc"),true,true,null));
        Upgrades.add(new Upgrades(Material.FIRE_CHARGE,"SmokeSurge",ChatColor.RED+"Odblokuj - SmokeSurge",0,0,false,List.of(ChatColor.RED+"Odblokuj Zdolnosc"),true,true,null));

        Upgrades.add(new Upgrades(Material.BLAZE_POWDER,"FireBlastUpgrade1",ChatColor.RED+"Ulepsz zdolonść - FireBlast",0,0,false,List.of(ChatColor.GREEN+"LPM Dmg+2",ChatColor.GREEN+"ChargeTime=>1s",ChatColor.GREEN+"Teraz podpala cele",ChatColor.RED+"Charged Cooldown=>5s",ChatColor.RED+"LPM Cooldown=>2.5s"),false,true,null));

        //ziemia
        Upgrades.add(new Upgrades(Material.LEATHER_CHESTPLATE,"EarthArmorKill",ChatColor.RED+"Najlepszą Obroną jest Atak",0,0,false,List.of(ChatColor.RED+"Pokonanie przeciwnika zapewnia tymczasową osłonę"),false,true,null));
        Upgrades.add(new Upgrades(Material.LEATHER_CHESTPLATE,"EarthArmorWave",ChatColor.RED+"Nietykalny",0,0,false,List.of(ChatColor.RED+"Uzyskaj tymczasową osłonę na początku Fali"),false,true,null));
        Upgrades.add(new Upgrades(Material.DIRT,"SandBreath",ChatColor.GREEN+"Odblokuj - SandBreath",0,0,false,List.of(ChatColor.RED+"Odblokuj Zdolnosc"),true,true,null));
        Upgrades.add(new Upgrades(Material.DIRT,"ShockWave",ChatColor.GREEN+"Odblokuj - ShockWave",0,0,false,List.of(ChatColor.RED+"Odblokuj Zdolnosc"),true,true,null));

    }


    public static boolean InArenaRange(Location p, Location Arena, int x, int z){
        if (Objects.equals(p.getWorld(), Arena.getWorld())){
            if ((p.getX() < Arena.getX()+x && p.getX() > Arena.getX()-x)&&(p.getZ() < Arena.getZ()+z && p.getZ() > Arena.getZ()-z)) {
                return true;
            }}
        return false;
    }

    public static List<Player> PInArena(Location arena, int x, int z){
        List<Player> PInDung = new ArrayList<>();
        for (Player p: Bukkit.getOnlinePlayers()) {
            if (p.getWorld().equals(arena.getWorld())){
                Location l = p.getLocation();
                if ((l.getX() < arena.getX()+x && l.getX() > arena.getX()-x)&&(l.getZ() < arena.getZ()+z && l.getZ() > arena.getZ()-z)) {
                    PInDung.add(p);
                }}}
        return PInDung;
    }

    public static void CurrentObjectiveUpdater(Player player, Map<Player, BossBar> playerBossBars,String CurrentTitle) {
        if (!playerBossBars.containsKey(player)){
            createPrivateBossBar(player,playerBossBars);
        }
        updatePrivateBossBar(player,1, 1,CurrentTitle,playerBossBars);
    }
    public static void createPrivateBossBar(Player player,Map<Player, BossBar> playerBossBars) {
        BossBar bossBar = Bukkit.createBossBar("", BarColor.GREEN, BarStyle.SOLID);
        bossBar.setVisible(true);
        bossBar.addPlayer(player);
        playerBossBars.put(player, bossBar);
    }
    public static void updatePrivateBossBar(Player player, float f, float max, String st,Map<Player, BossBar> playerBossBars) {
        if (!playerBossBars.containsKey(player)){
            createPrivateBossBar(player,playerBossBars);
        }
        BossBar bossBar = playerBossBars.get(player);
        bossBar.setProgress(f*((float)1/(max)));
        if (bossBar != null) {
            bossBar.setTitle(st);
        }}
    public static void removePrivateBossBar(Player player,Map<Player, BossBar> playerBossBars) {
        BossBar bossBar = playerBossBars.get(player);
        if (bossBar != null) {
            bossBar.removeAll();
            playerBossBars.remove(player);
            player.removePotionEffect(PotionEffectType.NIGHT_VISION);
        }}

    public static void Create1SecondZone(Location ArenaLocation, int DamageRadius,Material mat) {
            BukkitRunnable runnable = new BukkitRunnable() {
                int ticks = 0;
                @Override
                public void run() {
                    spawnParticleCircle(ArenaLocation,DamageRadius,DamageRadius*10,mat);
                    ticks++;
                    if (ticks >= 4) {
                        cancel();
                    }}};
            runnable.runTaskTimer(AmonPackPlugin.plugin, 0L, 5L);
    }

    public static void spawnParticleCircle(Location center, double radius, int numParticles, Material b) {
        World world = center.getWorld();
        double increment = 360.0 / numParticles;
        for (int i = 0; i < numParticles; i++) {
            double angle = Math.toRadians(i * increment);
            double xOffset = radius * Math.cos(angle);
            double zOffset = radius * Math.sin(angle);
            Location particleLocation = center.clone().add(xOffset, 1.5, zOffset);
            world.spawnParticle(Particle.BLOCK_CRACK, particleLocation, 1, 0, 0, 0, 0, b.createBlockData());
            world.spawnParticle(Particle.BLOCK_DUST, particleLocation, 1, 0, 0, 0, 0, b.createBlockData());
        }
    }

    public static class Doors {
        final Location l1;
        final Location l2;
        final Material m;
        public Doors(Location l1, Location l2, Material m) {
            this.l1=l1;
            this.l2=l2;
            this.m=m;
        }
        public  void DoorsMechanics(Material mat, boolean close) {
            World world = this.l1.getWorld();
            int minX = Math.min(l1.getBlockX(), l2.getBlockX());
            int minY = Math.min(l1.getBlockY(), l2.getBlockY());
            int minZ = Math.min(l1.getBlockZ(), l2.getBlockZ());
            int maxX = Math.max(l1.getBlockX(), l2.getBlockX());
            int maxY = Math.max(l1.getBlockY(), l2.getBlockY());
            int maxZ = Math.max(l1.getBlockZ(), l2.getBlockZ());
            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        Block block = world.getBlockAt(x, y, z);
                        if (close){
                            if (block.getType().equals(Material.AIR)){
                                block.setType(mat);
                            }}else{if (block.getType().equals(mat)) {
                            block.setType(org.bukkit.Material.AIR);
                        }}}}}}

    }
    public static void ResetAddonAbis(Player p){
        /*for (SkillTreeObj STO: PGrowth.SkillPoints) {
            if (STO.getPlayer().equalsIgnoreCase(p.getName())){
                BendingPlayer bp = BendingPlayer.getBendingPlayer(p);
                for (int i = 0; i <= 9; i++) {
                    String abilityName = bp.getAbilities().get(i);
                    if (!STO.getSelectedPath().contains(abilityName) && !GetDefAbiName().contains(abilityName)){
                        BendingBoardManager.getBoard(p).get().clearSlot(i);
                        bp.getAbilities().remove(i);
                    }}
                break;
            }}
    }


    public static void addTemporaryHealth(Player player, double healthAmount, long seconds) {
        player.setAbsorptionAmount(player.getAbsorptionAmount()+healthAmount);
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.getAbsorptionAmount()-healthAmount>=0){
                    player.setAbsorptionAmount(player.getAbsorptionAmount()-healthAmount);
                }else{
                    player.setAbsorptionAmount(0);
                }
            }
        }.runTaskLater(AmonPackPlugin.plugin, 20*seconds);
    }

    public static Upgrades GetUpgradeByNameFromPlayer(String name,List<Upgrades> ListOfUp){
        for (Upgrades up:ListOfUp) {
            if (up.getName().equalsIgnoreCase(name)){
                return up;
            }}
        return null;
    }
    public static Upgrades GetUpgradeByMaterialAndName(Material mat,String name,List<Upgrades> GeneralUpgradesList){
        for (Upgrades up:GeneralUpgradesList) {
            if (up.getItemStackReturn().getType().equals(mat) && up.getItemStackReturn().getItemMeta().getDisplayName().equalsIgnoreCase(name)){
                return up;
            }}
        return null;
    }



}
*/
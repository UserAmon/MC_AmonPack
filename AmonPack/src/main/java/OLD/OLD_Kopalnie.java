package OLD;
/*
import org.bukkit.event.Listener;

import static org.bukkit.Material.getMaterial;
import static org.bukkit.Material.matchMaterial;

public class OLD_Kopalnie implements Listener {
    public static ItemStack PickaxeTier1;
    public static ItemStack PickaxeTier2;
    public static ItemStack PickaxeTier3;
    static List<Location> MineList = new ArrayList<>();
    static List<Mine> ListOfMines = new ArrayList<>();
    static List<String> MinesListRadius = new ArrayList<>();
    public static void SetMiningItems() {

        PickaxeTier1 = new ItemStack(Material.WOODEN_PICKAXE, 1);
        ItemMeta PickaxeTier1Meta = PickaxeTier1.getItemMeta();
        PickaxeTier1Meta.addEnchant(Enchantment.DURABILITY, 10, true);
        PickaxeTier1Meta.addEnchant(Enchantment.DIG_SPEED, 2, true);
        PickaxeTier1Meta.setDisplayName("" + ChatColor.GRAY +ChatColor.BOLD + "Kilof Tier 1");
        PickaxeTier1.setItemMeta(PickaxeTier1Meta);

        PickaxeTier2 = new ItemStack(Material.IRON_PICKAXE, 1);
        ItemMeta PickaxeTier2Meta = PickaxeTier2.getItemMeta();
        PickaxeTier2Meta.addEnchant(Enchantment.DURABILITY, 10, true);
        PickaxeTier2Meta.addEnchant(Enchantment.DIG_SPEED, 4, true);
        PickaxeTier2Meta.setDisplayName("" + ChatColor.GRAY +ChatColor.BOLD + "Kilof Tier 2");
        PickaxeTier2.setItemMeta(PickaxeTier2Meta);

        PickaxeTier3 = new ItemStack(Material.GOLDEN_PICKAXE, 1);
        ItemMeta PickaxeTier3Meta = PickaxeTier3.getItemMeta();
        PickaxeTier3Meta.addEnchant(Enchantment.DURABILITY, 10, true);
        PickaxeTier3Meta.addEnchant(Enchantment.DIG_SPEED, 6, true);
        PickaxeTier3Meta.setDisplayName("" + ChatColor.GRAY +ChatColor.BOLD + "Kilof Tier 3");
        PickaxeTier3.setItemMeta(PickaxeTier3Meta);

        for(String key : Objects.requireNonNull(AmonPackPlugin.getNewConfigz().getConfigurationSection("AmonPack.Mining")).getKeys(false)) {
            Double X = AmonPackPlugin.getNewConfigz().getDouble("AmonPack.Mining." + key + ".X");
            Double Y = AmonPackPlugin.getNewConfigz().getDouble("AmonPack.Mining." + key + ".Y");
            Double Z = AmonPackPlugin.getNewConfigz().getDouble("AmonPack.Mining." + key + ".Z");
            String World = AmonPackPlugin.getNewConfigz().getString("AmonPack.Mining." + key + ".World");
            int YOffsetUp = AmonPackPlugin.getNewConfigz().getInt("AmonPack.Mining." + key + ".Y-Offset-Up");
            int YOffsetDown = AmonPackPlugin.getNewConfigz().getInt("AmonPack.Mining." + key + ".Y-Offset-Down");
            int Radius = AmonPackPlugin.getNewConfigz().getInt("AmonPack.Mining." + key + ".Radius");
            int RestoreTime = AmonPackPlugin.getNewConfigz().getInt("AmonPack.Mining." + key + ".RestoreTime");
            Material b = matchMaterial(AmonPackPlugin.getNewConfigz().getString("AmonPack.Mining." + key + ".RevertBlock"));
            HashMap<Material, Integer> LChance = new HashMap<>();
            HashMap<Material, Integer> OChance = new HashMap<>();
            for(String LootName : AmonPackPlugin.getNewConfigz().getConfigurationSection("AmonPack.Mining."+key+".Loot").getKeys(false)) {
                LChance.put(matchMaterial(LootName),AmonPackPlugin.getNewConfigz().getInt("AmonPack.Mining." + key + ".Loot."+LootName));
            }
            for(String OresName : AmonPackPlugin.getNewConfigz().getConfigurationSection("AmonPack.Mining."+key+".Loot").getKeys(false)) {
                OChance.put(matchMaterial(OresName),AmonPackPlugin.getNewConfigz().getInt("AmonPack.Mining." + key + ".Loot."+OresName));
            }

            Location loc = new Location(Bukkit.getWorld(World), X, Y, Z);
            //ListOfMines.add(new Mine(loc,YOffsetUp,YOffsetDown,Radius,RestoreTime,b,LChance,OChance));
            MineList.add(loc);
            MinesListRadius.add(key);
        }
    }

    public int checkifwithinmines(Block b){
        for (int i = 0; i < MineList.size(); i++) {
            if (b.getLocation().getWorld() == MineList.get(i).getWorld()){
                if (b.getLocation().distance(MineList.get(i)) <= AmonPackPlugin.getNewConfigz().getInt("AmonPack.Mining." + MinesListRadius.get(i) + ".Radius")){
                    Location loc1 = MineList.get(i).clone();
                    Location loc2 = b.getLocation().clone();
                    loc1.setX(loc2.getX());
                    loc1.setZ(loc2.getZ());
                    double distanceY = Math.abs(loc1.getY() - loc2.getY());
                    if (distanceY <= AmonPackPlugin.getNewConfigz().getDouble("AmonPack.Mining." + MinesListRadius.get(i) + ".Y-Offset")){
                        return i;
                }}}
        }
        return 1000;
    }


    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (AmonPackPlugin.MiningAndGatheringOn == true){
        Player player = event.getPlayer();
            int minezone = checkifwithinmines(event.getBlock());
            if (minezone != 1000){
            event.setCancelled(true);
        if (player.getInventory().getItemInMainHand().isSimilar(PickaxeTier1)) {
        for(String key : AmonPackPlugin.getNewConfigz().getConfigurationSection("AmonPack.MiningOresDrops").getKeys(false)) {
        if (event.getBlock().getType() == getMaterial(key)) {
                Kopanie(player, event.getBlock(),minezone);
        }}
        }else if (player.getInventory().getItemInMainHand().isSimilar(PickaxeTier2)) {
            for (Block blocks : GeneralMethods.getBlocksAroundPoint(event.getBlock().getLocation(), 1)) {
            for(String key : AmonPackPlugin.getNewConfigz().getConfigurationSection("AmonPack.MiningOresDrops").getKeys(false)) {
                if (blocks.getType() == getMaterial(key)) {
                        Kopanie(player, blocks,minezone);
                    }}}
        }else if (player.getInventory().getItemInMainHand().isSimilar(PickaxeTier3)) {
            for (Block blocks : GeneralMethods.getBlocksAroundPoint(event.getBlock().getLocation(), 2)) {
                for(String key : AmonPackPlugin.getNewConfigz().getConfigurationSection("AmonPack.MiningOresDrops").getKeys(false)) {
                    if (blocks.getType() == getMaterial(key)) {
                        Kopanie(player, blocks,minezone);
                    }}}
        }else {
            for(String key : AmonPackPlugin.getNewConfigz().getConfigurationSection("AmonPack.MiningOresDrops").getKeys(false)) {
                if (event.getBlock().getType() == getMaterial(key)) {
                    Kopanie(player, event.getBlock(),minezone);
                }}
        }}else {
                if (player.getInventory().getItemInMainHand().isSimilar(PickaxeTier1) || player.getInventory().getItemInMainHand().isSimilar(PickaxeTier2) || player.getInventory().getItemInMainHand().isSimilar(PickaxeTier3)) {
                    event.setCancelled(true);
                }}}}
    public int getRandom(int lower, int upper) {
        Random random = new Random();
        return random.nextInt((upper - lower) + 1) + lower;
    }

    private void Kopanie(Player player, Block b, int minezone) {
        int DropRandom = getRandom(0, 40);
        int OreRandom = getRandom(0, 40);
        Material mat = b.getType();

        List<String> Ores = new ArrayList<>();
        for(String key : AmonPackPlugin.getNewConfigz().getConfigurationSection("AmonPack.Mining.Kopalnia"+(minezone+1)+".Ores").getKeys(false)) {
            for (int i = 0; i < AmonPackPlugin.getNewConfigz().getInt("AmonPack.Mining.Kopalnia"+(minezone+1)+".Ores." + key); i++) {
                Ores.add(key);
        }}
        if (OreRandom > Ores.size()) {
            b.setType(Material.getMaterial(AmonPackPlugin.getNewConfigz().getString("AmonPack.Mining.Kopalnia"+(minezone+1)+".RevertBlock")));
            TempBlock tb1 = new TempBlock(b, Material.AIR);
            tb1.setRevertTime(AmonPackPlugin.getNewConfigz().getLong("AmonPack.Mining.Kopalnia"+(minezone+1)+".RestoreTime")*1000);
        } else if (OreRandom <= Ores.size()) {
            Random rand = new Random();
            String st = Ores.get(rand.nextInt(Ores.size()));
            b.setType(Material.getMaterial(st));
            TempBlock tb1 = new TempBlock(b, Material.AIR);
            tb1.setRevertTime(AmonPackPlugin.getNewConfigz().getLong("AmonPack.Mining.Kopalnia"+(minezone+1)+".RestoreTime")*1000);
        }
            for(String key : AmonPackPlugin.getNewConfigz().getConfigurationSection("AmonPack.MiningOresDrops").getKeys(false)) {
            if (mat == getMaterial(key)){
                player.getInventory().addItem(Commands.QuestItemConfig(AmonPackPlugin.getNewConfigz().getString("AmonPack.MiningOresDrops." + key)));
            }}

            if (AmonPackPlugin.getNewConfigz().getConfigurationSection("AmonPack.Mining.Kopalnia"+(minezone+1)) != null){
            if (AmonPackPlugin.getNewConfigz().getConfigurationSection("AmonPack.Mining.Kopalnia"+(minezone+1)+".Loot") != null){
        List<String> Loot = new ArrayList<String>();
        for(String key : AmonPackPlugin.getNewConfigz().getConfigurationSection("AmonPack.Mining.Kopalnia"+(minezone+1)+".Loot").getKeys(false)) {
            for (int i = 0; i < AmonPackPlugin.getNewConfigz().getInt("AmonPack.Mining.Kopalnia"+(minezone+1)+".Loot." + key); i++) {
                Loot.add(key);
            }}
        if (DropRandom < Loot.size()) {
            Random rand = new Random();
            String st = Loot.get(rand.nextInt(Loot.size()));
            player.sendMessage("" + ChatColor.GRAY + "Udało Ci się wydobyć " + Commands.QuestItemConfig(st).getItemMeta().getDisplayName());
            player.getInventory().addItem(Commands.QuestItemConfig(st));
        }}}}
}
*/
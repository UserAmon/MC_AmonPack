package AvatarSystems.Util_Objects;

import commands.Commands;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class Resource{
    private Material BlockName;
    private ItemStack Loot;
    private long RestoreTime;
    private int ClickRequired;
    private long ChangeLocationTimer;
    private int Exp;
    public Resource(String blockName, long changeLocationTimer, int clickRequired, String lootName, double restoreTime,int exp) {
        BlockName = Material.getMaterial(blockName);
        ChangeLocationTimer = changeLocationTimer;
        ClickRequired = clickRequired;
        Loot = Commands.QuestItemConfig(lootName);
        RestoreTime = (long) restoreTime;
        Exp=exp;
    }
    public Material getBlockName() {
        return BlockName;
    }
    public long getChangeLocationTimer() {
        return ChangeLocationTimer;
    }
    public int getClickRequired() {
        return ClickRequired;
    }
    public ItemStack getLootName() {
        return Loot;
    }
    public long getRestoreTime() {
        return RestoreTime;
    }
    public int getExp() {
        return Exp;
    }
}

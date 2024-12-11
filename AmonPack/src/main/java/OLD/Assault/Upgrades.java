package OLD.Assault;
/*
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class Upgrades {
    private String Name;
    private ItemStack ItemStackReturn;
    private int inmenu;
    private boolean Unlocked;
    private boolean MultiBuy;
    private double price;
    private boolean IsAbility;
    private boolean IsBlessing;
    private List<Upgrades> ReqUpgrades;
    //Lore

    public Upgrades(Material itemType, String name, int locinmenu, int p, boolean canbuymulti, List<String>lore) {
        price = p;
        Name = name;
        inmenu=locinmenu;
        ItemStackReturn = new ItemStack(itemType);
        ItemMeta meta = ItemStackReturn.getItemMeta();
        meta.setLore(lore);
        ItemStackReturn.setItemMeta(meta);
        MultiBuy= canbuymulti;
        IsBlessing=false;
        IsAbility=false;
    }
    public Upgrades(Material itemType, String name,String Displayname, int locinmenu, int p, boolean canbuymulti, List<String>lore,boolean abi,boolean isbless,List<Upgrades> Req) {
        price = p;
        Name = name;
        inmenu=locinmenu;
        ItemStackReturn = new ItemStack(itemType);
        ItemMeta meta = ItemStackReturn.getItemMeta();
        meta.setLore(lore);
        meta.setDisplayName(Displayname);
        ItemStackReturn.setItemMeta(meta);
        MultiBuy= canbuymulti;
        IsAbility = abi;
        IsBlessing=isbless;
        ReqUpgrades = Req;
    }


    public ItemStack SetName(String st){
        ItemMeta meta = ItemStackReturn.getItemMeta();
        meta.setDisplayName(st);
        ItemStackReturn.setItemMeta(meta);
        return ItemStackReturn;
    }

    public String getName() {
        return Name;
    }

    public ItemStack getItemStackReturn() {
        return ItemStackReturn;
    }

    public int getInmenu() {
        return inmenu;
    }

    public boolean isUnlocked() {
        return Unlocked;
    }

    public boolean isMultiBuy() {
        return MultiBuy;
    }

    public void setUnlocked(boolean unlocked) {
        Unlocked = unlocked;
    }

    public double getPrice() {
        return price;
    }

    public boolean isAbility() {
        return IsAbility;
    }

    public boolean isBlessing() {
        return IsBlessing;
    }

    public List<Upgrades> getReqUpgrades() {
        return ReqUpgrades;
    }
}*/
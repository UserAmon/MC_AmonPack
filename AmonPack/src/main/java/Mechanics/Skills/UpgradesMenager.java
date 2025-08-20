package Mechanics.Skills;

import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.attribute.AttributeModifier;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.inventory.ItemStack;

import javax.swing.text.html.parser.Entity;
import java.util.ArrayList;
import java.util.List;

import static methods_plugins.AmonPackPlugin.FastEasyStack;
import static methods_plugins.AmonPackPlugin.FastEasyStackWithLore;

public class UpgradesMenager {

    public static List<Upgrades>MenagerieUpgradesList = new ArrayList<>();
    public static ItemStack SpiritOrb=FastEasyStack(Material.BUDDING_AMETHYST,ChatColor.LIGHT_PURPLE+"Duchowa Kula");
    public static ItemStack SpiritBlade=FastEasyStack(Material.GOLDEN_SWORD,ChatColor.LIGHT_PURPLE+"Duchowa Ostrze");
    public static ItemStack SpiritShield=FastEasyStack(Material.SHIELD,ChatColor.DARK_PURPLE+"Duchowa Tarcza");


    public static ItemStack MoonBlade=FastEasyStack(Material.STONE_SWORD,ChatColor.DARK_PURPLE+"Ostrze Księżyca");
    public static ItemStack MoonBow=FastEasyStack(Material.BOW,ChatColor.DARK_PURPLE+"Łuk Księżyca");
    public UpgradesMenager(){


        Upgrades.UpgradeValues UV = new Upgrades.UpgradeValues(5,AttributeModifier.ADDITION,"Range");
        Upgrades.UpgradeValues UV2 = new Upgrades.UpgradeValues(5,AttributeModifier.ADDITION,"Damage");
        Upgrades.UpgradeValues UV3 = new Upgrades.UpgradeValues(3000,AttributeModifier.ADDITION,"Cooldown");
        Upgrades.UpgradeValues UV4 = new Upgrades.UpgradeValues(1500,AttributeModifier.SUBTRACTION,"ChargeTime");
        //Attribute.
        List<Upgrades.UpgradeValues> ListOfValues = new ArrayList<>();
        ListOfValues.add(UV);
        ListOfValues.add(UV2);
        ListOfValues.add(UV3);
        ListOfValues.add(UV4);




        List<String>Lore1=new ArrayList<>();
        List<String>Lore2=new ArrayList<>();
        List<String>Lore3=new ArrayList<>();
        List<String>Lore4=new ArrayList<>();
        List<String>Lore5=new ArrayList<>();
        List<String>Lore6=new ArrayList<>();
        List<String>Lore7=new ArrayList<>();
        List<String>Lore8=new ArrayList<>();
        List<String>Lore9=new ArrayList<>();
        Lore1.add(ChatColor.BLUE+"Odblokuj zdolność: AirSwipe");
        Lore2.add(ChatColor.GRAY+"Ulepsza Zasięg i Obrażenia");
        Lore2.add(ChatColor.GRAY+"Ulepsza Cooldown i ChargeTime");
        Lore3.add(ChatColor.BLUE+"Odblokuj zdolność");
        Lore4.add(ChatColor.BLUE+"Pokonanie przeciwnika ma szanse na upuszczenie Duchowych Kul");
        Lore5.add(ChatColor.BLUE+"Zadanie obrażen ma szanse na upuszczenie Duchowych Kul");
        Lore6.add(ChatColor.RED+"Zadawanie obrażen podpalonemu celowi");
        Lore6.add(ChatColor.RED+"wydłuża czas palenia i zadaje dodatkowe obrażenia");
        Lore7.add(ChatColor.BLUE+"Zabójstwo ma szanse na przyznanie osłony");
        Lore8.add(ChatColor.BLUE+"Zwiększa twoje obrażenia");
        Lore9.add(ChatColor.GOLD+"Pokonanie wroga ma szanse na upuszczenie");
        Lore9.add(ChatColor.GOLD+"Ostrze Księżyca");


        ItemStack item1 =FastEasyStackWithLore(Material.COBWEB, Element.AIR.getColor()+"AirSwipe",Lore3);
        ItemStack item2 =FastEasyStackWithLore(Material.COBWEB, Element.AIR.getColor()+"AirSwipe 2",Lore2);
        ItemStack item3 =FastEasyStackWithLore(Material.WATER_BUCKET, Element.WATER.getColor()+"Torrent",Lore3);
        ItemStack item4 =FastEasyStackWithLore(Material.AMETHYST_BLOCK, Element.AVATAR.getColor()+"Duchowe Kule - Kill",Lore4);
        ItemStack item5 =FastEasyStackWithLore(Material.AMETHYST_BLOCK, Element.AVATAR.getColor()+"Duchowe Kule - Dmg",Lore5);
        ItemStack item6 =FastEasyStackWithLore(Material.BLAZE_POWDER, Element.FIRE.getColor()+"Zapłon",Lore6);
        ItemStack item7 =FastEasyStackWithLore(Material.GOLDEN_CHESTPLATE, Element.EARTH.getColor()+"Osłona",Lore7);
        ItemStack item11 =FastEasyStackWithLore(Material.BLUE_ICE, Element.WATER.getColor()+"IceArch",Lore3);
        ItemStack item8 =FastEasyStackWithLore(Material.WOODEN_SWORD, Element.CHI.getColor()+"Obrażenia",Lore8);
        ItemStack item9 =FastEasyStackWithLore(Material.WOODEN_SWORD, Element.CHI.getColor()+"Ostrze Księżyca",Lore9);

        Upgrades AirSwipe1 = new Upgrades("AirSwipe1","AirSwipe" ,10.0,item1);
        Upgrades AirSwipe2 = new Upgrades("AirSwipe2","AirSwipe" ,40.0,ListOfValues,item2);
        Upgrades Torrent1  = new Upgrades("Torrent1","Torrent" ,10.0,item3);
        Upgrades IceArch  = new Upgrades("IceArch1","IceArch" ,10.0,item11);



        Upgrades SpiritOrbs_1_Kill  = new Upgrades("SpiritOrbs_1_Kill",item4);
        Upgrades SpiritOrbs_1_Dmg  = new Upgrades("SpiritOrbs_1_Dmg",item5);
        Upgrades Ignitions_1_Buff  = new Upgrades("Ignitions_1_Buff",item6);
        Upgrades Overshield_1_Kill  = new Upgrades("Overshield_1_Kill",item7);
        Upgrades Dmg_1_Buff  = new Upgrades("Dmg_1_Buff",item8);
        Upgrades Sword_1_Kill  = new Upgrades("Sword_1_Kill",item9);


        //Upgrades FireBlast1 = new Upgrades("FireBlast1", Upgrades.UpgradeType.MENAGERIE,ListOfValues);
        MenagerieUpgradesList.add(AirSwipe1);
        MenagerieUpgradesList.add(AirSwipe2);
        MenagerieUpgradesList.add(Torrent1);
        MenagerieUpgradesList.add(IceArch);
        MenagerieUpgradesList.add(SpiritOrbs_1_Kill);
        MenagerieUpgradesList.add(SpiritOrbs_1_Dmg);
        MenagerieUpgradesList.add(Ignitions_1_Buff);
        MenagerieUpgradesList.add(Overshield_1_Kill);
        MenagerieUpgradesList.add(Dmg_1_Buff);
        MenagerieUpgradesList.add(Sword_1_Kill);

    }

}

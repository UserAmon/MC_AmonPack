package methods_plugins.Abilities;
import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.ability.Ability;
import com.projectkorra.projectkorra.ability.ChiAbility;
import com.projectkorra.projectkorra.ability.SubAbility;

import methods_plugins.AmonPackPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public abstract class BladesAbility extends ChiAbility implements SubAbility {
    public static ItemStack Sword1;
    public static ItemStack Sword2;
    public BladesAbility(Player player) {
        super(player);
    }
    public static void CreateSwords() {
		Sword1 = new ItemStack(Material.WOODEN_SWORD, 1);
        ItemMeta Sword1Meta = Sword1.getItemMeta();
        Sword1Meta.addEnchant(Enchantment.DURABILITY, 10, true);
        Sword1Meta.setDisplayName("" + ChatColor.GOLD + "Sword");
        Sword1.setItemMeta(Sword1Meta);
        
		Sword2 = new ItemStack(Material.IRON_SWORD, 1);
        ItemMeta Sword2Meta = Sword2.getItemMeta();
        Sword2Meta.addEnchant(Enchantment.DURABILITY, 10, true);
        Sword2Meta.setDisplayName("" + ChatColor.GOLD + "Sword");
        Sword2.setItemMeta(Sword2Meta);
    }

    public Class<? extends Ability> getParentAbility() {
        return ChiAbility.class;
    }

    public Element getElement() {
        return AmonPackPlugin.getBladesElement();
    }
}

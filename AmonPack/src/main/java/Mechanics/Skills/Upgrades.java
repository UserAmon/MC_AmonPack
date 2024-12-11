package Mechanics.Skills;

import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.attribute.AttributeModifier;
import com.projectkorra.projectkorra.attribute.AttributePriority;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class Upgrades {
    String name;
    String AbilityName;
    MenagerieUpgradeType Type;
    List<UpgradeValues>ListUpgradeValues;
    List<String>ReqUpdates;
    ItemStack Item;
    double Price;


    public Upgrades(String name, String abilityName, double price, List<UpgradeValues> listUpgradeValues, ItemStack item) {
        this.name = name;
        AbilityName = abilityName;
        Type = MenagerieUpgradeType.ABILITYBUFF;
        ListUpgradeValues = listUpgradeValues;
        Item = item;
        ReqUpdates=null;
        Price=price;
    }

    public Upgrades(String name, String abilityName, double price, ItemStack item) {
        this.name = name;
        AbilityName = abilityName;
        Type = MenagerieUpgradeType.ABILITY;
        Item = item;
        ReqUpdates=null;
        Price=price;
    }

    public Upgrades(String name, ItemStack item) {
        this.name = name;
        Item = item;
        Type = MenagerieUpgradeType.BUFF;
    }

    public void setReqUpdates(List<String> reqUpdates) {
        ReqUpdates = reqUpdates;
    }
    public double getPrice() {
        return Price;
    }
    public String getAbilityName() {
        return AbilityName;
    }
    public ItemStack getItem() {
        return Item;
    }
    public List<String> getReqUpdates() {
        return ReqUpdates;
    }
    public void ApplyEffects(CoreAbility Ability){
        for (UpgradeValues UV :ListUpgradeValues) {
            Ability.addAttributeModifier(UV.attribute, UV.value, UV.AttributeM, AttributePriority.HIGH);
        }
    }
    public MenagerieUpgradeType getType() {
        return Type;
    }
    public String getName() {
        return name;
    }
    public enum MenagerieUpgradeType {
        ABILITY,
        ABILITYBUFF,
        BUFF
    }
    static class UpgradeValues{
        int value;
        AttributeModifier AttributeM;
        String attribute;
        public UpgradeValues(int value, AttributeModifier attributeM, String attribute) {
            this.value = value;
            AttributeM = attributeM;
            this.attribute = attribute;
        }
    }
}

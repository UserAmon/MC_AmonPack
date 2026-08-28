package RPG.Crafting.Objects;

import Plugin.AmonPackPlugin;
import RPG.Magic.model.SpellElement;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Craftable_Armor extends ItemMold {
    private final double armorValue;
    private final double manaReductionPercent;
    private final double manaReductionFlat;
    private final String manaElement;
    private final double cdReductionPercent;
    private final double cdReductionFlat;
    private final String cdElement;
    private final double speedIncreasePercent;

    public Craftable_Armor(String weaponID, List<ItemStack> itemsRequiredToShapeMold, String itemName, Material itemMaterial,
                           List<String> itemLore, Integer customModelID, List<MagicEffects> allowedMagicEffects,
                           double armorValue, double manaReductionPercent, double manaReductionFlat, String manaElement,
                           double cdReductionPercent, double cdReductionFlat, String cdElement, double speedIncreasePercent) {
        super(weaponID, itemsRequiredToShapeMold, itemName, itemMaterial, itemLore, customModelID, allowedMagicEffects, ItemType.ARMOR);
        this.armorValue = armorValue;
        this.manaReductionPercent = manaReductionPercent;
        this.manaReductionFlat = manaReductionFlat;
        this.manaElement = manaElement != null ? manaElement.toUpperCase(Locale.ROOT) : "ALL";
        this.cdReductionPercent = cdReductionPercent;
        this.cdReductionFlat = cdReductionFlat;
        this.cdElement = cdElement != null ? cdElement.toUpperCase(Locale.ROOT) : "ALL";
        this.speedIncreasePercent = speedIncreasePercent;
    }

    public Craftable_Armor(String weaponID, List<ItemStack> itemsRequiredToShapeMold, String itemName, Material itemMaterial,
                           List<String> itemLore, Integer customModelID, List<MagicEffects> allowedMagicEffects, double armorValue) {
        this(weaponID, itemsRequiredToShapeMold, itemName, itemMaterial, itemLore, customModelID, allowedMagicEffects,
                armorValue, 0.0, 0.0, "ALL", 0.0, 0.0, "ALL", 0.0);
    }

    public double getArmorValue() {
        return armorValue;
    }

    public double getDmgReduction() {
        return armorValue;
    }

    public double getManaReductionPercent() {
        return manaReductionPercent;
    }

    public double getManaReductionFlat() {
        return manaReductionFlat;
    }

    public String getManaElement() {
        return manaElement;
    }

    public double getCdReductionPercent() {
        return cdReductionPercent;
    }

    public double getCdReductionFlat() {
        return cdReductionFlat;
    }

    public String getCdElement() {
        return cdElement;
    }

    public double getSpeedIncreasePercent() {
        return speedIncreasePercent;
    }

    public boolean appliesToElement(String filterElement, SpellElement element) {
        if (filterElement == null || filterElement.isEmpty() || filterElement.equalsIgnoreCase("ALL")) return true;
        if (element == null) return true;
        return filterElement.equalsIgnoreCase(element.name());
    }

    public List<String> getFormattedArmorLore() {
        List<String> lore = new ArrayList<>();
        if (getItemLore() != null && !getItemLore().isEmpty()) {
            lore.addAll(getItemLore());
        }
        lore.add("");
        lore.add("§6Statystyki Pancerza:");
        lore.add(" §9🛡 Pancerz: §f+" + (armorValue == (long) armorValue ? String.format(Locale.ROOT, "%d", (long) armorValue) : String.format(Locale.ROOT, "%.1f", armorValue)));

        boolean hasBuffs = (manaReductionPercent > 0 || manaReductionFlat > 0 || cdReductionPercent > 0 || cdReductionFlat > 0 || speedIncreasePercent > 0);
        if (hasBuffs) {
            lore.add("");
            lore.add("§dMistyczne Właściwości:");
            if (manaReductionPercent > 0) {
                String elStr = manaElement != null && !manaElement.equalsIgnoreCase("ALL") ? " §7(" + manaElement + ")" : "";
                lore.add(" §b✦ Koszt many czarów: §f-" + (int)(manaReductionPercent * 100) + "%" + elStr);
            }
            if (manaReductionFlat > 0) {
                String elStr = manaElement != null && !manaElement.equalsIgnoreCase("ALL") ? " §7(" + manaElement + ")" : "";
                lore.add(" §b✦ Koszt many czarów: §f-" + (int)manaReductionFlat + " MP" + elStr);
            }
            if (cdReductionPercent > 0) {
                String elStr = cdElement != null && !cdElement.equalsIgnoreCase("ALL") ? " §7(" + cdElement + ")" : "";
                lore.add(" §e✦ Czas odnowienia: §f-" + (int)(cdReductionPercent * 100) + "%" + elStr);
            }
            if (cdReductionFlat > 0) {
                String elStr = cdElement != null && !cdElement.equalsIgnoreCase("ALL") ? " §7(" + cdElement + ")" : "";
                lore.add(" §e✦ Czas odnowienia: §f-" + String.format(Locale.ROOT, "%.1f", cdReductionFlat) + "s" + elStr);
            }
            if (speedIncreasePercent > 0) {
                lore.add(" §a✦ Prędkość ruchu: §f+" + (int)(speedIncreasePercent * 100) + "%");
            }
        }
        return lore;
    }

    @Override
    public ItemStack toItemStack() {
        ItemStack item = super.toItemStack();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setLore(getFormattedArmorLore());
            item.setItemMeta(meta);
        }
        return item;
    }

    public double ExecutePlayerGetDamaged(Entity victim, ItemStack item, Player player) {
        double DamageTaken = armorValue;
        if (item == null || !item.hasItemMeta()) return DamageTaken;
        ItemMeta meta = item.getItemMeta();
        NamespacedKey key = new NamespacedKey(AmonPackPlugin.plugin, "magic_effects");
        String data = meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        if (data != null && !data.isEmpty() && victim != player) {
            for (MagicEffects effects : MagicEffects.deserializeList(data)) {
                DamageTaken = DamageTaken + effects.ExecuteOnTakinHit(victim, player);
            }
        }
        return DamageTaken;
    }
}

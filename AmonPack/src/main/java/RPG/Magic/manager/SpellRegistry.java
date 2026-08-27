package RPG.Magic.manager;

import RPG.Magic.model.Spell;
import RPG.Magic.model.SpellElement;
import RPG.Magic.spells.fire.FireBlastSpell;

import java.util.*;

public class SpellRegistry {

    private final Map<String, Spell> spells = new LinkedHashMap<>();

    public SpellRegistry() {
        loadDefaults();
    }

    public void loadDefaults() {
        spells.clear();
        registerSpell(new FireBlastSpell());
        registerSpell(new RPG.Magic.spells.fire.BlazingSpell());
        registerSpell(new RPG.Magic.spells.fire.FlashPointSpell());
        registerSpell(new RPG.Magic.spells.fire.FireCircleSpell());
        registerSpell(new RPG.Magic.spells.fire.BarrageSpell());
    }

    public void registerSpell(Spell spell) {
        if (spell != null) {
            spells.put(spell.getId().toLowerCase(Locale.ROOT), spell);
        }
    }

    public Spell getSpell(String id) {
        if (id == null) return null;
        return spells.get(id.toLowerCase(Locale.ROOT));
    }

    public Collection<Spell> getAllSpells() {
        return Collections.unmodifiableCollection(spells.values());
    }

    public List<Spell> getSpellsByElement(SpellElement element) {
        List<Spell> list = new ArrayList<>();
        for (Spell s : spells.values()) {
            if (s.getElement() == element) {
                list.add(s);
            }
        }
        return list;
    }
}

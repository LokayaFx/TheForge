package lk.cwresports.theforge.model;

import lk.cwresports.theforge.hook.AureliumSkillsHook;
import lk.cwresports.theforge.hook.EcoSkillsHook;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Requirements a player must meet before a forge slot unlocks. Every listed
 * requirement must be satisfied.
 */
public final class SlotRequirement {

    private final int slot;
    private final int forgeLevel;
    private final Map<String, Integer> aureliumSkills;
    private final Map<String, Integer> ecoSkills;

    public SlotRequirement(int slot, int forgeLevel,
                           Map<String, Integer> aureliumSkills, Map<String, Integer> ecoSkills) {
        this.slot = slot;
        this.forgeLevel = Math.max(1, forgeLevel);
        this.aureliumSkills = Collections.unmodifiableMap(new LinkedHashMap<>(aureliumSkills));
        this.ecoSkills = Collections.unmodifiableMap(new LinkedHashMap<>(ecoSkills));
    }

    public int slot() {
        return slot;
    }

    public int forgeLevel() {
        return forgeLevel;
    }

    public Map<String, Integer> aureliumSkills() {
        return aureliumSkills;
    }

    public Map<String, Integer> ecoSkills() {
        return ecoSkills;
    }

    public boolean isMet(Player player, int playerForgeLevel) {
        if (playerForgeLevel < forgeLevel) {
            return false;
        }
        for (Map.Entry<String, Integer> entry : aureliumSkills.entrySet()) {
            if (!AureliumSkillsHook.isAvailable()) {
                return false;
            }
            if (AureliumSkillsHook.getLevel(player, entry.getKey()) < entry.getValue()) {
                return false;
            }
        }
        for (Map.Entry<String, Integer> entry : ecoSkills.entrySet()) {
            if (!EcoSkillsHook.isAvailable()) {
                return false;
            }
            if (EcoSkillsHook.getLevel(player, entry.getKey()) < entry.getValue()) {
                return false;
            }
        }
        return true;
    }
}

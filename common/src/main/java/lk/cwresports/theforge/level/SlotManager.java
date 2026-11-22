package lk.cwresports.theforge.level;

import lk.cwresports.theforge.TheForgePlugin;
import lk.cwresports.theforge.hook.AureliumSkillsHook;
import lk.cwresports.theforge.hook.EcoSkillsHook;
import lk.cwresports.theforge.model.SlotRequirement;
import lk.cwresports.theforge.util.Log;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Loads per-slot unlock requirements from {@code slots.yml} and decides which
 * slots a player may use.
 */
public final class SlotManager {

    private final TheForgePlugin plugin;
    private final Map<Integer, SlotRequirement> requirements = new LinkedHashMap<>();
    private int maxSlots;

    public SlotManager(TheForgePlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        requirements.clear();
        maxSlots = Math.max(1, plugin.getConfig().getInt("max-slots", 7));

        File file = new File(plugin.getDataFolder(), "slots.yml");
        if (!file.exists()) {
            plugin.saveResource("slots.yml", false);
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection slots = config.getConfigurationSection("slots");
        if (slots != null) {
            for (String key : slots.getKeys(false)) {
                int slot;
                try {
                    slot = Integer.parseInt(key);
                } catch (NumberFormatException ignored) {
                    continue;
                }
                if (slot < 1) {
                    continue;
                }
                ConfigurationSection section = slots.getConfigurationSection(key);
                if (section == null) {
                    continue;
                }
                int forgeLevel = section.getInt("required-level", slot);
                Map<String, Integer> aureliumSkills = readLevels(section.getConfigurationSection("aureliumskills"));
                Map<String, Integer> ecoSkills = readLevels(section.getConfigurationSection("ecoskills"));
                requirements.put(slot, new SlotRequirement(slot, forgeLevel, aureliumSkills, ecoSkills));
            }
        }

        Log.debug("Loaded unlock requirements for " + requirements.size() + " forge slot(s).");
    }

    private Map<String, Integer> readLevels(ConfigurationSection section) {
        Map<String, Integer> levels = new LinkedHashMap<>();
        if (section == null) {
            return levels;
        }
        for (String key : section.getKeys(false)) {
            int level = section.getInt(key, -1);
            if (level > 0) {
                levels.put(key.toLowerCase(Locale.ROOT), level);
            }
        }
        return levels;
    }

    public int maxSlots() {
        return maxSlots;
    }

    public SlotRequirement requirement(int slot) {
        SlotRequirement requirement = requirements.get(slot);
        if (requirement != null) {
            return requirement;
        }
        return new SlotRequirement(slot, slot, Map.of(), Map.of());
    }

    public boolean isUnlocked(Player player, int forgeLevel, int slot) {
        if (slot < 1 || slot > maxSlots) {
            return false;
        }
        return requirement(slot).isMet(player, forgeLevel);
    }

    public int unlockedCount(Player player, int forgeLevel) {
        int count = 0;
        for (int slot = 1; slot <= maxSlots; slot++) {
            if (isUnlocked(player, forgeLevel, slot)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Human readable list of requirements that are still missing for a slot.
     */
    public List<String> unmet(Player player, int forgeLevel, int slot) {
        SlotRequirement requirement = requirement(slot);
        List<String> missing = new ArrayList<>();
        if (forgeLevel < requirement.forgeLevel()) {
            missing.add("Forge Level " + requirement.forgeLevel());
        }
        for (Map.Entry<String, Integer> entry : requirement.aureliumSkills().entrySet()) {
            int current = AureliumSkillsHook.isAvailable()
                    ? AureliumSkillsHook.getLevel(player, entry.getKey())
                    : -1;
            if (current < entry.getValue()) {
                missing.add(AureliumSkillsHook.displayName() + " " + entry.getKey() + " " + entry.getValue());
            }
        }
        for (Map.Entry<String, Integer> entry : requirement.ecoSkills().entrySet()) {
            int current = EcoSkillsHook.isAvailable()
                    ? EcoSkillsHook.getLevel(player, entry.getKey())
                    : -1;
            if (current < entry.getValue()) {
                missing.add("EcoSkills " + entry.getKey() + " " + entry.getValue());
            }
        }
        return missing;
    }
}

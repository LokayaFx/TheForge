package lk.cwresports.theforge.level;

import lk.cwresports.theforge.TheForgePlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

public final class LevelManager {

    private final int maxLevel;
    private final int maxSlots;
    private final NavigableMap<Integer, Integer> requirements = new TreeMap<>();
    private final Map<Integer, Double> reductions = new HashMap<>();

    public LevelManager(TheForgePlugin plugin) {
        FileConfiguration config = plugin.getConfig();
        this.maxSlots = Math.max(1, config.getInt("max-slots", 5));

        requirements.put(1, 0);
        ConfigurationSection levelsSection = config.getConfigurationSection("levels");
        if (levelsSection != null) {
            for (String key : levelsSection.getKeys(false)) {
                int level;
                try {
                    level = Integer.parseInt(key);
                } catch (NumberFormatException ignored) {
                    continue;
                }
                if (level < 2) {
                    continue;
                }
                requirements.put(level, Math.max(0, levelsSection.getInt(key)));
            }
        }
        this.maxLevel = Math.max(1, requirements.lastKey());

        ConfigurationSection reductionsSection = config.getConfigurationSection("reductions");
        for (int level = 1; level <= maxLevel; level++) {
            double fallback = Math.min(0.9D, (level - 1) * 0.10D);
            double value = reductionsSection == null
                    ? fallback
                    : reductionsSection.getDouble(String.valueOf(level), fallback);
            reductions.put(level, Math.max(0.0D, Math.min(1.0D, value)));
        }
    }

    public int maxLevel() {
        return maxLevel;
    }

    public int maxSlots() {
        return maxSlots;
    }

    public int levelFor(int xp) {
        int level = 1;
        for (Map.Entry<Integer, Integer> entry : requirements.entrySet()) {
            if (entry.getKey() <= maxLevel && xp >= entry.getValue()) {
                level = entry.getKey();
            }
        }
        return level;
    }

    public int requirementFor(int level) {
        if (level <= 1) {
            return 0;
        }
        Integer requirement = requirements.get(level);
        if (requirement != null) {
            return requirement;
        }
        return requirements.lastEntry().getValue();
    }

    public int nextRequirement(int level) {
        return level >= maxLevel ? -1 : requirementFor(level + 1);
    }

    public double reductionFor(int level) {
        return reductions.getOrDefault(Math.max(1, Math.min(maxLevel, level)), 0.0D);
    }

    public int reductionPercent(int level) {
        return (int) Math.round(reductionFor(level) * 100.0D);
    }

    public double progressToNext(int xp) {
        int level = levelFor(xp);
        if (level >= maxLevel) {
            return 1.0D;
        }
        int previous = requirementFor(level);
        int next = requirementFor(level + 1);
        if (next <= previous) {
            return 1.0D;
        }
        double progress = (xp - previous) / (double) (next - previous);
        return Math.max(0.0D, Math.min(1.0D, progress));
    }
}

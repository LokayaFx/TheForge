package lk.cwresports.theforge.model;

import org.bukkit.configuration.ConfigurationSection;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class PlayerData {

    private final UUID uuid;
    private int xp;
    private int totalSeconds;
    private final Map<Integer, ForgeJob> jobs = new LinkedHashMap<>();

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUuid() {
        return uuid;
    }

    public int getXp() {
        return xp;
    }

    public void setXp(int xp) {
        this.xp = Math.max(0, xp);
    }

    public void addXp(int amount) {
        if (amount <= 0) {
            return;
        }
        long next = (long) xp + amount;
        this.xp = (int) Math.min(Integer.MAX_VALUE, next);
    }

    public int getTotalSeconds() {
        return totalSeconds;
    }

    public void setTotalSeconds(int totalSeconds) {
        this.totalSeconds = Math.max(0, totalSeconds);
    }

    public void addTotalSeconds(int seconds) {
        if (seconds <= 0) {
            return;
        }
        long next = (long) totalSeconds + seconds;
        this.totalSeconds = (int) Math.min(Integer.MAX_VALUE, next);
    }

    public Map<Integer, ForgeJob> getJobs() {
        return Collections.unmodifiableMap(jobs);
    }

    public ForgeJob getJob(int slot) {
        return jobs.get(slot);
    }

    public void putJob(ForgeJob job) {
        jobs.put(job.getSlot(), job);
    }

    public ForgeJob removeJob(int slot) {
        return jobs.remove(slot);
    }

    public void save(ConfigurationSection section) {
        section.set("xp", xp);
        section.set("total-seconds", totalSeconds);
        ConfigurationSection jobsSection = section.createSection("jobs");
        for (Map.Entry<Integer, ForgeJob> entry : jobs.entrySet()) {
            entry.getValue().save(jobsSection.createSection(String.valueOf(entry.getKey())));
        }
    }

    public void load(ConfigurationSection section) {
        this.totalSeconds = Math.max(0, section.getInt("total-seconds", 0));
        // Older versions used total forging time for leveling. Keep that progress
        // by migrating the value to XP when no XP value has been stored yet.
        this.xp = Math.max(0, section.getInt("xp", totalSeconds));
        this.jobs.clear();
        ConfigurationSection jobsSection = section.getConfigurationSection("jobs");
        if (jobsSection == null) {
            return;
        }
        for (String key : jobsSection.getKeys(false)) {
            int slot;
            try {
                slot = Integer.parseInt(key);
            } catch (NumberFormatException ignored) {
                continue;
            }
            ForgeJob job = ForgeJob.fromSection(slot, jobsSection.getConfigurationSection(key));
            if (job != null) {
                this.jobs.put(slot, job);
            }
        }
    }
}

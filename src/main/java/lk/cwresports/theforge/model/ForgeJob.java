package lk.cwresports.theforge.model;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.Base64;

public final class ForgeJob {

    private final int slot;
    private final String recipeId;
    private final String displayName;
    private final byte[] resultData;
    private final int baseSeconds;
    private final int durationSeconds;
    private final int experience;
    private final long startedAt;
    private boolean credited;

    public ForgeJob(int slot, String recipeId, String displayName, byte[] resultData,
                    int baseSeconds, int durationSeconds, int experience, long startedAt) {
        this.slot = slot;
        this.recipeId = recipeId;
        this.displayName = displayName;
        this.resultData = resultData;
        this.baseSeconds = Math.max(1, baseSeconds);
        this.durationSeconds = Math.max(1, durationSeconds);
        this.experience = Math.max(1, experience);
        this.startedAt = startedAt;
        this.credited = false;
    }

    public int getSlot() {
        return slot;
    }

    public String getRecipeId() {
        return recipeId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getBaseSeconds() {
        return baseSeconds;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public int getExperience() { return experience; }

    public long getStartedAt() {
        return startedAt;
    }

    public boolean isCredited() {
        return credited;
    }

    public void setCredited(boolean credited) {
        this.credited = credited;
    }

    public long endAt() {
        return startedAt + durationSeconds;
    }

    public boolean isComplete(long now) {
        return now >= endAt();
    }

    public int getRemaining(long now) {
        long remaining = endAt() - now;
        if (remaining <= 0) {
            return 0;
        }
        return (int) Math.min(Integer.MAX_VALUE, remaining);
    }

    public double getProgress(long now) {
        if (durationSeconds <= 0) {
            return 1.0D;
        }
        double elapsed = now - startedAt;
        return Math.max(0.0D, Math.min(1.0D, elapsed / (double) durationSeconds));
    }

    public ItemStack resultItem() {
        try {
            return ItemStack.deserializeBytes(resultData);
        } catch (Exception exception) {
            return null;
        }
    }

    public void save(ConfigurationSection section) {
        section.set("recipe-id", recipeId);
        section.set("display-name", displayName);
        section.set("result", Base64.getEncoder().encodeToString(resultData));
        section.set("base-seconds", baseSeconds);
        section.set("duration-seconds", durationSeconds);
        section.set("experience", experience);
        section.set("started-at", startedAt);
        section.set("credited", credited);
    }

    public static ForgeJob fromSection(int slot, ConfigurationSection section) {
        if (section == null) {
            return null;
        }
        try {
            String recipeId = section.getString("recipe-id", "unknown");
            String displayName = section.getString("display-name", "Item");
            String data = section.getString("result");
            if (data == null || data.isBlank()) {
                return null;
            }
            byte[] resultData = Base64.getDecoder().decode(data);
            int baseSeconds = Math.max(1, section.getInt("base-seconds", 60));
            int durationSeconds = Math.max(1, section.getInt("duration-seconds", baseSeconds));
            long startedAt = section.getLong("started-at", System.currentTimeMillis() / 1000L);
            int experience = Math.max(1, section.getInt("experience", baseSeconds));
            ForgeJob job = new ForgeJob(slot, recipeId, displayName, resultData, baseSeconds, durationSeconds, experience, startedAt);
            job.setCredited(section.getBoolean("credited", false));
            return job;
        } catch (Exception exception) {
            return null;
        }
    }
}

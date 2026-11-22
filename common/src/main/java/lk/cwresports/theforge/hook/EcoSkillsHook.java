package lk.cwresports.theforge.hook;

import lk.cwresports.theforge.util.Log;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.Locale;

/**
 * Optional EcoSkills integration. Skill levels are read through the EcoSkills
 * Java API ({@code com.willfp.ecoskills.api.EcoSkillsAPI}) using reflection so
 * TheForge still builds and runs on servers without EcoSkills installed.
 */
public final class EcoSkillsHook {

    private static boolean available;
    private static Object skillsInstance;
    private static Method getSkillMethod;
    private static Method getSkillLevelMethod;

    private EcoSkillsHook() {
    }

    public static void init() {
        available = false;
        skillsInstance = null;

        Plugin plugin = Bukkit.getPluginManager().getPlugin("EcoSkills");
        if (plugin == null) {
            Log.info("EcoSkills not installed; EcoSkills slot requirements are disabled.");
            return;
        }
        if (!plugin.isEnabled()) {
            Log.warn("EcoSkills is installed but not enabled; EcoSkills slot requirements are disabled.");
            return;
        }

        try {
            Class<?> apiClass = Class.forName("com.willfp.ecoskills.api.EcoSkillsAPI");
            Class<?> skillClass = Class.forName("com.willfp.ecoskills.skills.Skill");
            Class<?> skillsClass = Class.forName("com.willfp.ecoskills.skills.Skills");

            getSkillLevelMethod = apiClass.getMethod("getSkillLevel", OfflinePlayer.class, skillClass);
            skillsInstance = skillsClass.getField("INSTANCE").get(null);
            getSkillMethod = skillsClass.getMethod("get", String.class);

            available = true;
            Log.info("EcoSkills hook enabled.");
        } catch (Throwable throwable) {
            Log.error("Could not hook into EcoSkills.", throwable);
        }
    }

    public static boolean isAvailable() {
        return available;
    }

    /**
     * Returns the player's level for the given skill id, or -1 if the plugin or
     * skill could not be resolved.
     */
    public static int getLevel(Player player, String skillId) {
        if (!available || player == null || skillId == null || skillId.isBlank()) {
            return -1;
        }
        try {
            Object skill = getSkillMethod.invoke(skillsInstance, skillId.trim().toLowerCase(Locale.ROOT));
            if (skill == null) {
                return -1;
            }
            Object result = getSkillLevelMethod.invoke(null, player, skill);
            return result instanceof Integer level ? level : -1;
        } catch (Throwable throwable) {
            return -1;
        }
    }
}

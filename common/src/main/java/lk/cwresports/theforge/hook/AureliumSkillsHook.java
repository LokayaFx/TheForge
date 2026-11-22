package lk.cwresports.theforge.hook;

import lk.cwresports.theforge.util.Log;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.UUID;

/**
 * Optional AuraSkills / AureliumSkills integration. Both the modern AuraSkills
 * API ({@code dev.aurelium.auraskills}) and the legacy AureliumSkills API
 * ({@code com.archyx.aureliumskills}) are supported through reflection, so
 * TheForge still builds and runs when neither plugin is installed.
 */
public final class AureliumSkillsHook {

    private static boolean available;
    private static boolean modern;

    private static Object api;
    private static Method getUserMethod;
    private static Method userGetSkillLevelMethod;
    private static Method getGlobalRegistryMethod;
    private static Method registryGetSkillMethod;
    private static Method namespacedIdMethod;

    private static Object legacyRegistry;
    private static Method legacyGetSkillMethod;
    private static Method legacyGetSkillLevelMethod;

    private AureliumSkillsHook() {
    }

    public static void init() {
        available = false;
        modern = false;
        api = null;
        legacyRegistry = null;

        Plugin aura = Bukkit.getPluginManager().getPlugin("AuraSkills");
        if (aura != null && aura.isEnabled() && initModern()) {
            return;
        }

        Plugin aurelium = Bukkit.getPluginManager().getPlugin("AureliumSkills");
        if (aurelium != null && aurelium.isEnabled() && initLegacy()) {
            return;
        }

        Log.info("AuraSkills/AureliumSkills not installed; skill slot requirements are disabled.");
    }

    private static boolean initModern() {
        try {
            Class<?> apiClass = Class.forName("dev.aurelium.auraskills.api.AuraSkillsApi");
            api = apiClass.getMethod("get").invoke(null);
            if (api == null) {
                return false;
            }

            Class<?> skillClass = Class.forName("dev.aurelium.auraskills.api.skill.Skill");
            Class<?> userClass = Class.forName("dev.aurelium.auraskills.api.user.SkillsUser");
            Class<?> registryClass = Class.forName("dev.aurelium.auraskills.api.registry.GlobalRegistry");
            Class<?> idClass = Class.forName("dev.aurelium.auraskills.api.registry.NamespacedId");

            getUserMethod = apiClass.getMethod("getUser", UUID.class);
            userGetSkillLevelMethod = userClass.getMethod("getSkillLevel", skillClass);
            getGlobalRegistryMethod = apiClass.getMethod("getGlobalRegistry");
            registryGetSkillMethod = registryClass.getMethod("getSkill", idClass);
            namespacedIdMethod = findIdFactory(idClass);

            modern = true;
            available = true;
            Log.info("AuraSkills hook enabled.");
            return true;
        } catch (Throwable throwable) {
            Log.warn("Could not hook into AuraSkills: " + throwable);
            return false;
        }
    }

    private static Method findIdFactory(Class<?> idClass) throws NoSuchMethodException {
        for (String name : new String[]{"fromDefaultWithColon", "fromDefault", "of"}) {
            try {
                return idClass.getMethod(name, String.class);
            } catch (NoSuchMethodException ignored) {
                // try the next factory name
            }
        }
        throw new NoSuchMethodException("No usable NamespacedId factory on " + idClass.getName());
    }

    private static boolean initLegacy() {
        try {
            Class<?> apiClass = Class.forName("com.archyx.aureliumskills.api.AureliumAPI");
            Class<?> skillClass = Class.forName("com.archyx.aureliumskills.skills.Skill");

            legacyGetSkillLevelMethod = apiClass.getMethod("getSkillLevel", Player.class, skillClass);
            Object main = apiClass.getMethod("getPlugin").invoke(null);
            legacyRegistry = main.getClass().getMethod("getSkillRegistry").invoke(main);
            if (legacyRegistry == null) {
                return false;
            }
            legacyGetSkillMethod = legacyRegistry.getClass().getMethod("getSkill", String.class);

            modern = false;
            available = true;
            Log.info("AureliumSkills hook enabled (legacy API).");
            return true;
        } catch (Throwable throwable) {
            Log.warn("Could not hook into AureliumSkills: " + throwable);
            return false;
        }
    }

    public static boolean isAvailable() {
        return available;
    }

    public static String displayName() {
        return modern ? "AuraSkills" : "AureliumSkills";
    }

    /**
     * Returns the player's level for the given skill id, or -1 if the plugin or
     * skill could not be resolved.
     */
    public static int getLevel(Player player, String skillId) {
        if (!available || player == null || skillId == null || skillId.isBlank()) {
            return -1;
        }
        String id = skillId.trim().toLowerCase(Locale.ROOT);
        try {
            if (modern) {
                Object registry = getGlobalRegistryMethod.invoke(api);
                if (registry == null) {
                    return -1;
                }
                Object namespacedId = namespacedIdMethod.invoke(null, id);
                Object skill = registryGetSkillMethod.invoke(registry, namespacedId);
                if (skill == null) {
                    return -1;
                }
                Object user = getUserMethod.invoke(api, player.getUniqueId());
                if (user == null) {
                    return -1;
                }
                Object result = userGetSkillLevelMethod.invoke(user, skill);
                return result instanceof Integer level ? level : -1;
            }

            Object skill = legacyGetSkillMethod.invoke(legacyRegistry, id);
            if (skill == null) {
                return -1;
            }
            Object result = legacyGetSkillLevelMethod.invoke(null, player, skill);
            return result instanceof Integer level ? level : -1;
        } catch (Throwable throwable) {
            return -1;
        }
    }
}

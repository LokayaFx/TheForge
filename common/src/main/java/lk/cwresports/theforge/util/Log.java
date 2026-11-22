package lk.cwresports.theforge.util;

import lk.cwresports.theforge.TheForgePlugin;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Small console logging helper. Paper already prefixes messages with
 * [TheForge], so the messages here stay plain.
 */
public final class Log {

    private Log() {
    }

    public static void info(String message) {
        logger().info(message);
    }

    public static void warn(String message) {
        logger().warning(message);
    }

    public static void error(String message, Throwable throwable) {
        logger().log(Level.SEVERE, message, throwable);
    }

    /**
     * Job starts, completions, claims and level-ups.
     */
    public static void gameplay(String message) {
        if (enabled("logging.gameplay", true)) {
            info(message);
        }
    }

    /**
     * Reload, settime, setlevel and recipe/category changes.
     */
    public static void admin(String message) {
        if (enabled("logging.admin", true)) {
            info(message);
        }
    }

    /**
     * Extra detail useful while troubleshooting.
     */
    public static void debug(String message) {
        if (enabled("logging.debug", false)) {
            info("[debug] " + message);
        }
    }

    private static boolean enabled(String path, boolean fallback) {
        TheForgePlugin plugin = TheForgePlugin.get();
        return plugin == null || plugin.getConfig().getBoolean(path, fallback);
    }

    private static Logger logger() {
        TheForgePlugin plugin = TheForgePlugin.get();
        return plugin == null ? Logger.getLogger("TheForge") : plugin.getLogger();
    }
}

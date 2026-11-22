package lk.cwresports.theforge.hook;

import lk.cwresports.theforge.util.Log;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;

/**
 * Optional EcoItems integration. EcoItems items are resolved through eco's
 * item lookup system ({@code ecoitems:<id>}) using reflection so TheForge
 * still builds and runs on servers without EcoItems installed.
 */
public final class EcoItemsHook {

    private static boolean available;
    private static Method lookupMethod;
    private static Method getItemMethod;
    private static Method matchesMethod;
    private static Method isCustomItemMethod;

    private EcoItemsHook() {
    }

    public static void init() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("EcoItems");
        if (plugin == null) {
            Log.info("EcoItems not installed; EcoItems items are disabled.");
            return;
        }
        if (!plugin.isEnabled()) {
            Log.warn("EcoItems is installed but not enabled; EcoItems items are disabled.");
            return;
        }
        try {
            Class<?> itemsClass = Class.forName("com.willfp.eco.core.items.Items");
            lookupMethod = itemsClass.getMethod("lookup", String.class);

            Class<?> itemLookup = loadItemLookupInterface();
            getItemMethod = itemLookup.getMethod("getItem");
            matchesMethod = itemLookup.getMethod("matches", ItemStack.class);

            try {
                isCustomItemMethod = itemsClass.getMethod("isCustomItem", ItemStack.class);
            } catch (NoSuchMethodException ignored) {
                isCustomItemMethod = null;
            }

            available = true;
            Log.info("EcoItems hook enabled (eco lookup).");
        } catch (Throwable throwable) {
            Log.error("Could not hook into EcoItems.", throwable);
        }
    }

    private static Class<?> loadItemLookupInterface() throws ClassNotFoundException {
        try {
            return Class.forName("com.willfp.eco.core.items.TestableItem");
        } catch (ClassNotFoundException ignored) {
            return Class.forName("com.willfp.eco.core.items.ItemLookup");
        }
    }

    public static boolean isAvailable() {
        return available;
    }

    public static ItemStack build(String id, int amount) {
        if (!available || id == null || id.isBlank()) {
            return null;
        }
        try {
            Object testable = lookupMethod.invoke(null, "ecoitems:" + normalize(id));
            if (testable == null) {
                return null;
            }
            Object built = getItemMethod.invoke(testable);
            if (!(built instanceof ItemStack stack) || stack.getType().isAir()) {
                return null;
            }
            ItemStack copy = stack.clone();
            copy.setAmount(Math.max(1, amount));
            return copy;
        } catch (Throwable throwable) {
            Log.warn("Could not build EcoItems item " + id + " - " + throwable);
            return null;
        }
    }

    public static boolean matches(ItemStack stack, String id) {
        if (!available || stack == null || id == null || id.isBlank()) {
            return false;
        }
        try {
            Object testable = lookupMethod.invoke(null, "ecoitems:" + normalize(id));
            if (testable == null) {
                return false;
            }
            Object result = matchesMethod.invoke(testable, stack);
            return result instanceof Boolean matches && matches;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean isCustomItem(ItemStack stack) {
        if (!available || isCustomItemMethod == null || stack == null) {
            return false;
        }
        try {
            Object result = isCustomItemMethod.invoke(null, stack);
            return result instanceof Boolean custom && custom;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static String normalize(String id) {
        String value = id.trim();
        if (value.toLowerCase().startsWith("ecoitems:")) {
            value = value.substring("ecoitems:".length());
        }
        return value;
    }
}

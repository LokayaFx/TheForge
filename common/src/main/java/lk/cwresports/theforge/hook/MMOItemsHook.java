package lk.cwresports.theforge.hook;

import lk.cwresports.theforge.util.Log;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;

/**
 * Optional MMOItems integration. Everything is reflective so TheForge still
 * builds and runs on servers without MMOItems installed.
 */
public final class MMOItemsHook {

    private static boolean available;
    private static Object mmoItems;
    private static Method getTypesMethod;
    private static Method getMmoItemMethod;
    private static Class<?> typeClass;

    private static Method nbtGetMethod;
    private static Method nbtGetStringMethod;
    private static boolean nbtAvailable;

    private MMOItemsHook() {
    }

    public static void init() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("MMOItems");
        if (plugin == null) {
            Log.info("MMOItems not installed; MMOItems items are disabled.");
            return;
        }
        if (!plugin.isEnabled()) {
            Log.warn("MMOItems is installed but not enabled; MMOItems items are disabled.");
            return;
        }
        try {
            Class<?> mainClass = Class.forName("net.Indyuce.mmoitems.MMOItems");
            Field pluginField = mainClass.getField("plugin");
            mmoItems = pluginField.get(null);
            if (mmoItems == null) {
                Log.warn("MMOItems plugin instance is null; MMOItems items are disabled.");
                return;
            }
            getTypesMethod = mainClass.getMethod("getTypes");

            for (Method method : mainClass.getMethods()) {
                if (!method.getName().equals("getMMOItem") || method.getParameterCount() != 2) {
                    continue;
                }
                if (method.getParameterTypes()[1] != String.class) {
                    continue;
                }
                getMmoItemMethod = method;
                typeClass = method.getParameterTypes()[0];
                break;
            }
            if (getMmoItemMethod == null) {
                Log.warn("MMOItems getMMOItem(type, id) was not found; MMOItems items are disabled.");
                return;
            }

            available = true;
            initNbt();
            Log.info("MMOItems hook enabled (NBT tags: "
                    + (nbtAvailable ? "available" : "unavailable") + ").");
        } catch (Throwable throwable) {
            Log.error("Could not hook into MMOItems.", throwable);
        }
    }

    private static void initNbt() {
        try {
            Class<?> nbtItem = Class.forName("io.lumine.mythic.lib.api.item.NBTItem");
            nbtGetMethod = nbtItem.getMethod("get", ItemStack.class);
            nbtGetStringMethod = nbtItem.getMethod("getString", String.class);
            nbtAvailable = true;
        } catch (Throwable ignored) {
            nbtAvailable = false;
        }
    }

    public static boolean isAvailable() {
        return available;
    }

    public static boolean hasNbt() {
        return nbtAvailable;
    }

    public static ItemStack build(String type, String id, int amount) {
        if (!available || type == null || id == null) {
            return null;
        }
        try {
            Object typeArg;
            if (typeClass == String.class) {
                typeArg = type.toUpperCase(Locale.ROOT);
            } else {
                Object types = getTypesMethod.invoke(mmoItems);
                if (types == null) {
                    return null;
                }
                Method get = findMethod(types.getClass(), "get", String.class);
                if (get == null) {
                    return null;
                }
                typeArg = get.invoke(types, type.toUpperCase(Locale.ROOT));
                if (typeArg == null) {
                    return null;
                }
            }

            Object mmoItem = getMmoItemMethod.invoke(mmoItems, typeArg, id);
            return buildFromTemplate(mmoItem, amount);
        } catch (Throwable throwable) {
            Log.warn("Could not build MMOItems item " + type + ":" + id
                    + " - " + throwable);
            return null;
        }
    }

    /**
     * Builds an MMOItems item by its id only, searching every loaded type.
     * Useful for the {@code mmoitems:<id>} YAML shorthand.
     */
    public static ItemStack buildAny(String id, int amount) {
        if (!available || id == null || id.isBlank()) {
            return null;
        }
        try {
            Object types = getTypesMethod.invoke(mmoItems);
            if (types == null) {
                return null;
            }
            Method getAll = findMethod(types.getClass(), "getAll");
            if (getAll == null) {
                return null;
            }
            Object all = getAll.invoke(types);
            if (!(all instanceof Iterable<?> iterable)) {
                return null;
            }
            for (Object typeArg : iterable) {
                if (typeArg == null) {
                    continue;
                }
                Object mmoItem = getMmoItemMethod.invoke(mmoItems, typeArg, id);
                ItemStack stack = buildFromTemplate(mmoItem, amount);
                if (stack != null) {
                    return stack;
                }
            }
            return null;
        } catch (Throwable throwable) {
            Log.warn("Could not build MMOItems item " + id + " - " + throwable);
            return null;
        }
    }

    private static ItemStack buildFromTemplate(Object mmoItem, int amount) throws Exception {
        if (mmoItem == null) {
            return null;
        }
        Method newBuilder = findMethod(mmoItem.getClass(), "newBuilder");
        if (newBuilder == null) {
            return null;
        }
        Object builder = newBuilder.invoke(mmoItem);
        if (builder == null) {
            return null;
        }
        Method build = findMethod(builder.getClass(), "build");
        if (build == null) {
            return null;
        }
        Object built = build.invoke(builder);
        if (!(built instanceof ItemStack stack)) {
            return null;
        }
        stack.setAmount(Math.max(1, amount));
        return stack;
    }

    public static String getType(ItemStack stack) {
        return getTag(stack, "MMOITEMS_ITEM_TYPE");
    }

    public static String getId(ItemStack stack) {
        return getTag(stack, "MMOITEMS_ITEM_ID");
    }

    private static String getTag(ItemStack stack, String key) {
        if (!nbtAvailable || stack == null) {
            return null;
        }
        try {
            Object nbt = nbtGetMethod.invoke(null, stack);
            if (nbt == null) {
                return null;
            }
            Object value = nbtGetStringMethod.invoke(nbt, key);
            if (value instanceof String text && !text.isBlank()) {
                return text;
            }
            return null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static boolean matches(ItemStack stack, String type, String id) {
        if (stack == null || type == null || id == null) {
            return false;
        }
        String stackType = getType(stack);
        String stackId = getId(stack);
        return stackType != null && stackId != null
                && stackType.equalsIgnoreCase(type)
                && stackId.equalsIgnoreCase(id);
    }

    /**
     * Matches an MMOItems item by its id regardless of type.
     */
    public static boolean matchesId(ItemStack stack, String id) {
        if (stack == null || id == null) {
            return false;
        }
        String stackId = getId(stack);
        return stackId != null && stackId.equalsIgnoreCase(id);
    }

    private static Method findMethod(Class<?> start, String name, Class<?>... params) {
        for (Class<?> current = start; current != null && current != Object.class; current = current.getSuperclass()) {
            try {
                Method method = current.getDeclaredMethod(name, params);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException ignored) {
                // keep looking
            }
            try {
                Method method = current.getMethod(name, params);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException ignored) {
                // keep looking
            }
        }
        return null;
    }
}

package lk.cwresports.theforge.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.ArrayList;
import java.util.List;

public final class Text {

    private static final LegacyComponentSerializer AMPERSAND = LegacyComponentSerializer.legacyAmpersand();

    private Text() {
    }

    public static Component color(String input) {
        return input == null ? Component.empty() : AMPERSAND.deserialize(input);
    }

    public static Component name(String input) {
        return color(input).decoration(TextDecoration.ITALIC, false);
    }

    public static List<Component> lore(List<String> lines) {
        List<Component> result = new ArrayList<>(lines.size());
        for (String line : lines) {
            result.add(name(line));
        }
        return result;
    }

    public static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    public static String legacy(Component component) {
        return component == null ? "" : AMPERSAND.serialize(component);
    }

    public static String strip(String input) {
        if (input == null) {
            return "";
        }
        return plain(AMPERSAND.deserialize(input));
    }

    public static String bar(double progress, int length) {
        int filled = (int) Math.round(Math.max(0.0D, Math.min(1.0D, progress)) * length);
        return "&a" + "|".repeat(filled) + "&7" + "|".repeat(Math.max(0, length - filled));
    }

    /** Solid horizontal line bar, matching the recipe-book progress style. */
    public static String lineBar(double progress, int length) {
        int filled = (int) Math.round(Math.max(0.0D, Math.min(1.0D, progress)) * length);
        return "&a" + "\u2501".repeat(filled) + "&f" + "\u2501".repeat(Math.max(0, length - filled));
    }

    public static String pretty(String raw) {
        if (raw == null || raw.isBlank()) {
            return "Unknown";
        }
        String[] parts = raw.toLowerCase().replace('-', '_').split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return builder.isEmpty() ? raw : builder.toString();
    }
}

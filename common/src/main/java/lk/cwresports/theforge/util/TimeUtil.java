package lk.cwresports.theforge.util;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TimeUtil {

    private static final Pattern PART = Pattern.compile("(\\d+)([hms])");

    private TimeUtil() {
    }

    /**
     * Accepts plain seconds ("3600") or a compact duration ("30m", "1h30m", "2h15m30s").
     */
    public static int parseSeconds(String input) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("Empty time");
        }
        String value = input.trim().toLowerCase(Locale.ROOT).replace(" ", "");
        if (value.matches("\\d+")) {
            long seconds = Long.parseLong(value);
            if (seconds > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("Time is too large");
            }
            return (int) seconds;
        }
        if (!value.matches("(\\d+[hms])+")) {
            throw new IllegalArgumentException("Invalid time");
        }
        Matcher matcher = PART.matcher(value);
        long total = 0;
        while (matcher.find()) {
            long amount = Long.parseLong(matcher.group(1));
            switch (matcher.group(2)) {
                case "h" -> total += amount * 3600L;
                case "m" -> total += amount * 60L;
                case "s" -> total += amount;
                default -> throw new IllegalArgumentException("Invalid time");
            }
        }
        if (total <= 0 || total > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Invalid time");
        }
        return (int) total;
    }

    public static String format(int seconds) {
        if (seconds <= 0) {
            return "0s";
        }
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        int secs = seconds % 60;
        StringBuilder builder = new StringBuilder();
        if (hours > 0) {
            builder.append(hours).append("h ");
        }
        if (minutes > 0) {
            builder.append(minutes).append("m ");
        }
        if (secs > 0 || builder.isEmpty()) {
            builder.append(secs).append("s");
        }
        return builder.toString().trim();
    }

    public static String formatMinutes(int seconds) {
        return format(seconds);
    }
}

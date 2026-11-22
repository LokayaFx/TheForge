package lk.cwresports.theforge.platform;

/**
 * Holds the platform adapter supplied by the active server-version module.
 */
public final class Platforms {

    private static Platform platform;

    private Platforms() {
    }

    public static void set(Platform platform) {
        Platforms.platform = platform;
    }

    public static Platform get() {
        Platform current = platform;
        if (current == null) {
            throw new IllegalStateException("TheForge platform adapter has not been initialised");
        }
        return current;
    }
}

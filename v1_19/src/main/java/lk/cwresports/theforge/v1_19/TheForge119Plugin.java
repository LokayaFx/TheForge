package lk.cwresports.theforge.v1_19;

import lk.cwresports.theforge.TheForgePlugin;
import lk.cwresports.theforge.platform.Platform;

/**
 * Paper 1.19.x entry point.
 */
public final class TheForge119Plugin extends TheForgePlugin {

    @Override
    protected Platform createPlatform() {
        return new GlowPlatform();
    }
}

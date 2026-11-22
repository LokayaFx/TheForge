package lk.cwresports.theforge.v1_18;

import lk.cwresports.theforge.TheForgePlugin;
import lk.cwresports.theforge.platform.Platform;

/**
 * Paper 1.18.2 entry point.
 */
public final class TheForge118Plugin extends TheForgePlugin {

    @Override
    protected Platform createPlatform() {
        return new GlowPlatform();
    }
}

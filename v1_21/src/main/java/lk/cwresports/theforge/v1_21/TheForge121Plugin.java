package lk.cwresports.theforge.v1_21;

import lk.cwresports.theforge.TheForgePlugin;
import lk.cwresports.theforge.platform.Platform;

/**
 * Paper 1.21.x entry point.
 */
public final class TheForge121Plugin extends TheForgePlugin {

    @Override
    protected Platform createPlatform() {
        return new GlowPlatform();
    }
}

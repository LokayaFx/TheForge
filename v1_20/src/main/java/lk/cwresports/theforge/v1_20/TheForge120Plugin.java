package lk.cwresports.theforge.v1_20;

import lk.cwresports.theforge.TheForgePlugin;
import lk.cwresports.theforge.platform.Platform;

/**
 * Paper 1.20.x entry point.
 */
public final class TheForge120Plugin extends TheForgePlugin {

    @Override
    protected Platform createPlatform() {
        return new GlowPlatform();
    }
}

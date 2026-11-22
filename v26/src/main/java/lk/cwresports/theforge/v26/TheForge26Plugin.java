package lk.cwresports.theforge.v26;

import lk.cwresports.theforge.TheForgePlugin;
import lk.cwresports.theforge.platform.Platform;

/**
 * Paper 26.2 entry point.
 */
public final class TheForge26Plugin extends TheForgePlugin {

    @Override
    protected Platform createPlatform() {
        return new GlowPlatform();
    }
}

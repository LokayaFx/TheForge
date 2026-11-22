package lk.cwresports.theforge.v26;

import lk.cwresports.theforge.platform.Platform;
import org.bukkit.inventory.ItemStack;

/**
 * Modern Paper versions support an explicit enchantment glint override.
 */
public final class GlowPlatform implements Platform {

    @Override
    public void applyGlow(ItemStack stack) {
        stack.editMeta(meta -> meta.setEnchantmentGlintOverride(true));
    }
}

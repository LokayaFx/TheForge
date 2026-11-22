package lk.cwresports.theforge.platform;

import org.bukkit.inventory.ItemStack;

/**
 * Version-specific behaviour that cannot be shared between Paper API releases.
 * Each server-version module provides its own implementation.
 */
public interface Platform {

    /**
     * Adds the enchantment glint to an item without changing its real enchantments.
     */
    void applyGlow(ItemStack stack);
}

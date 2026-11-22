package lk.cwresports.theforge.v1_20;

import lk.cwresports.theforge.platform.Platform;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

/**
 * This build targets 1.20.4 so it still runs on Java 17 servers.
 * A hidden dummy enchantment is used instead of the 1.20.5+ glint override.
 */
public final class GlowPlatform implements Platform {

    @Override
    public void applyGlow(ItemStack stack) {
        Enchantment unbreaking = Enchantment.getByKey(NamespacedKey.minecraft("unbreaking"));
        if (unbreaking == null) {
            return;
        }
        stack.addUnsafeEnchantment(unbreaking, 1);
        stack.addItemFlags(ItemFlag.HIDE_ENCHANTS);
    }
}

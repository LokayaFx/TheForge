package lk.cwresports.theforge.v1_18;

import lk.cwresports.theforge.platform.Platform;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

/**
 * 1.18.2 has no glint-override API, so a hidden dummy enchantment is used.
 * The enchantment is looked up by key so this jar keeps working on newer
 * servers where the old {@code Enchantment.DURABILITY} field was renamed.
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

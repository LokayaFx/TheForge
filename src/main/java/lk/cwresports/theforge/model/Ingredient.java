package lk.cwresports.theforge.model;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

public final class Ingredient {

    private final ItemSpec item;
    private final int amount;

    public Ingredient(ItemSpec item, int amount) {
        this.item = item;
        this.amount = Math.max(1, amount);
    }

    public ItemSpec getItem() {
        return item;
    }

    public int getAmount() {
        return amount;
    }

    public String displayName() {
        return item.displayName();
    }

    public String key() {
        return item.key();
    }

    public boolean matches(ItemStack stack) {
        return item.matches(stack);
    }

    public void saveItem(ConfigurationSection section) {
        item.saveItem(section);
        section.set("amount", amount);
    }

    public static Ingredient fromItemSection(ConfigurationSection section) {
        if (section == null) {
            return null;
        }
        ItemSpec item = ItemSpec.fromItemSection(section);
        if (item == null) {
            return null;
        }
        int amount = Math.max(1, section.getInt("amount", 1));
        return new Ingredient(item, amount);
    }

    /**
     * Legacy entry point kept for compatibility. New code should use
     * {@link #saveItem(ConfigurationSection)}.
     */
    public void save(ConfigurationSection section) {
        saveItem(section);
    }

    /**
     * Legacy entry point kept for compatibility. New code should use
     * {@link #fromItemSection(ConfigurationSection)}.
     */
    public static Ingredient fromSection(ConfigurationSection section) {
        return fromItemSection(section);
    }
}

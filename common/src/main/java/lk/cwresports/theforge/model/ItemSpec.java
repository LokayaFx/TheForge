package lk.cwresports.theforge.model;

import lk.cwresports.theforge.hook.EcoItemsHook;
import lk.cwresports.theforge.hook.MMOItemsHook;
import lk.cwresports.theforge.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

/**
 * A serializable description of an item used by a recipe.
 * Supports vanilla materials, MMOItems items and exact custom item snapshots.
 */
public final class ItemSpec {

    public enum Kind {
        VANILLA,
        MMOITEMS,
        ECOITEMS,
        CUSTOM
    }

    /** Sentinel type used when an MMOItems id should be searched across all types. */
    private static final String ANY_MMO_TYPE = "*";

    private final Kind kind;
    private final Material material;
    private final String mmoType;
    private final String mmoId;
    private final String ecoId;
    private final byte[] customData;

    private ItemSpec(Kind kind, Material material, String mmoType, String mmoId,
                     String ecoId, byte[] customData) {
        this.kind = kind;
        this.material = material;
        this.mmoType = mmoType;
        this.mmoId = mmoId;
        this.ecoId = ecoId;
        this.customData = customData;
    }

    public static ItemSpec vanilla(Material material) {
        if (material == null || material.isAir()) {
            return null;
        }
        return new ItemSpec(Kind.VANILLA, material, null, null, null, null);
    }

    public static ItemSpec mmoItems(String type, String id) {
        if (type == null || type.isBlank() || id == null || id.isBlank()) {
            return null;
        }
        return new ItemSpec(Kind.MMOITEMS, null, type, id, null, null);
    }

    /**
     * An MMOItems item referenced by id only. The type is resolved by searching
     * every loaded type when the item is built or matched.
     */
    public static ItemSpec mmoItemsAny(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        return new ItemSpec(Kind.MMOITEMS, null, ANY_MMO_TYPE, id, null, null);
    }

    public static ItemSpec ecoItems(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        String value = id.trim();
        if (value.toLowerCase(Locale.ROOT).startsWith("ecoitems:")) {
            value = value.substring("ecoitems:".length()).trim();
        }
        return value.isEmpty() ? null : new ItemSpec(Kind.ECOITEMS, null, null, null, value, null);
    }

    public static ItemSpec custom(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) {
            return null;
        }
        ItemStack copy = stack.clone();
        copy.setAmount(1);
        return new ItemSpec(Kind.CUSTOM, null, null, null, null, copy.serializeAsBytes());
    }

    public Kind getKind() {
        return kind;
    }

    public Material getMaterial() {
        return material;
    }

    public String getMmoType() {
        return mmoType;
    }

    public String getMmoId() {
        return mmoId;
    }

    public String getEcoId() {
        return ecoId;
    }

    public boolean isMMOItems() {
        return kind == Kind.MMOITEMS;
    }

    public boolean isEcoItems() {
        return kind == Kind.ECOITEMS;
    }

    public boolean matches(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) {
            return false;
        }
        switch (kind) {
            case MMOITEMS:
                return ANY_MMO_TYPE.equals(mmoType)
                        ? MMOItemsHook.matchesId(stack, mmoId)
                        : MMOItemsHook.matches(stack, mmoType, mmoId);
            case ECOITEMS:
                return EcoItemsHook.matches(stack, ecoId);
            case CUSTOM:
                ItemStack template = create(1);
                return template != null && stack.isSimilar(template);
            case VANILLA:
            default:
                if (MMOItemsHook.isAvailable() && MMOItemsHook.getType(stack) != null) {
                    return false;
                }
                if (EcoItemsHook.isCustomItem(stack)) {
                    return false;
                }
                return stack.getType() == material;
        }
    }

    public ItemStack create(int amount) {
        int safeAmount = Math.max(1, amount);
        try {
            return switch (kind) {
                case MMOITEMS -> ANY_MMO_TYPE.equals(mmoType)
                        ? MMOItemsHook.buildAny(mmoId, safeAmount)
                        : MMOItemsHook.build(mmoType, mmoId, safeAmount);
                case ECOITEMS -> EcoItemsHook.build(ecoId, safeAmount);
                case CUSTOM -> {
                    ItemStack stack = ItemStack.deserializeBytes(customData);
                    stack.setAmount(safeAmount);
                    yield stack;
                }
                case VANILLA -> new ItemStack(material, safeAmount);
            };
        } catch (Exception exception) {
            return null;
        }
    }

    public String displayName() {
        if (kind == Kind.CUSTOM) {
            ItemStack stack = create(1);
            if (stack != null) {
                ItemMeta meta = stack.getItemMeta();
                if (meta != null && meta.hasDisplayName()) {
                    return Text.plain(meta.displayName());
                }
                return Text.pretty(stack.getType().name());
            }
        }
        if (kind == Kind.MMOITEMS) {
            ItemStack stack = ANY_MMO_TYPE.equals(mmoType)
                    ? MMOItemsHook.buildAny(mmoId, 1)
                    : MMOItemsHook.build(mmoType, mmoId, 1);
            if (stack != null) {
                ItemMeta meta = stack.getItemMeta();
                if (meta != null && meta.hasDisplayName()) {
                    return Text.plain(meta.displayName());
                }
                return Text.pretty(stack.getType().name());
            }
            return Text.pretty(mmoId);
        }
        if (kind == Kind.ECOITEMS) {
            ItemStack stack = EcoItemsHook.build(ecoId, 1);
            if (stack != null) {
                ItemMeta meta = stack.getItemMeta();
                if (meta != null && meta.hasDisplayName()) {
                    return Text.plain(meta.displayName());
                }
                return Text.pretty(stack.getType().name());
            }
            return Text.pretty(ecoId);
        }
        return Text.pretty(material == null ? "Unknown" : material.name());
    }

    /**
     * A stable, lowercase key used for ingredient ids when saving recipes.
     */
    public String key() {
        String base;
        switch (kind) {
            case MMOITEMS -> base = ANY_MMO_TYPE.equals(mmoType)
                    ? "mmoitems_" + mmoId
                    : mmoType + "_" + mmoId;
            case ECOITEMS -> base = "ecoitems_" + ecoId;
            case CUSTOM -> {
                ItemStack stack = create(1);
                base = stack == null ? "item" : stack.getType().name();
            }
            case VANILLA -> base = material == null ? "item" : material.name();
            default -> base = "item";
        }
        String cleaned = base.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        return cleaned.isEmpty() ? "item" : cleaned;
    }

    /**
     * Writes this item in the categories.yml style:
     * material + name + amount (amount handled by the caller), optional lore and
     * custom_model_data. MMOItems items use mmoitems_type / mmoitems_id.
     */
    public void saveItem(ConfigurationSection section) {
        switch (kind) {
            case MMOITEMS -> {
                if (ANY_MMO_TYPE.equals(mmoType)) {
                    section.set("mmoitems", mmoId);
                } else {
                    section.set("mmoitems_type", mmoType);
                    section.set("mmoitems_id", mmoId);
                }
            }
            case ECOITEMS -> section.set("ecoitems", ecoId);
            case CUSTOM -> {
                ItemStack stack = create(1);
                if (stack == null) {
                    return;
                }
                section.set("material", stack.getType().name());
                ItemMeta meta = stack.getItemMeta();
                if (meta == null) {
                    return;
                }
                if (meta.hasDisplayName()) {
                    section.set("name", Text.legacy(meta.displayName()));
                }
                List<Component> lore = meta.lore();
                if (lore != null && !lore.isEmpty()) {
                    List<String> lines = new ArrayList<>(lore.size());
                    for (Component line : lore) {
                        lines.add(Text.legacy(line));
                    }
                    section.set("lore", lines);
                }
                if (meta.hasCustomModelData()) {
                    section.set("custom_model_data", meta.getCustomModelData());
                }
            }
            case VANILLA -> section.set("material", material == null ? "AIR" : material.name());
            default -> {
                // nothing to write
            }
        }
    }

    public static ItemSpec fromItemSection(ConfigurationSection section) {
        if (section == null) {
            return null;
        }

        String legacyType = section.getString("type", "");
        if (legacyType.equalsIgnoreCase("custom")) {
            String data = section.getString("data");
            if (data != null && !data.isBlank()) {
                try {
                    ItemStack stack = ItemStack.deserializeBytes(Base64.getDecoder().decode(data));
                    return custom(stack);
                } catch (Exception ignored) {
                    // fall through to the readable format below
                }
            }
        }

        // Support the shorthand forms directly on the material field:
        //   material: ecoitems:my_item
        //   material: mmoitems:TYPE:ID
        String materialRaw = section.getString("material");
        if (materialRaw != null) {
            String trimmed = materialRaw.trim();
            String lower = trimmed.toLowerCase(Locale.ROOT);
            if (lower.startsWith("ecoitems:")) {
                return ecoItems(trimmed.substring("ecoitems:".length()));
            }
            if (lower.startsWith("mmoitems:")) {
                return parseMmoShorthand(trimmed.substring("mmoitems:".length()));
            }
            if (lower.startsWith("ia:")) {
                return parseMmoShorthand(trimmed.substring("ia:".length()));
            }
        }

        // EcoItems: ecoitems: item_name
        String ecoId = firstNonBlank(
                section.getString("ecoitems"),
                section.getString("eco-item"),
                section.getString("ecoitems_id"),
                section.getString("ecoitems-id"),
                legacyType.equalsIgnoreCase("ecoitems") || legacyType.equalsIgnoreCase("eco")
                        ? section.getString("id") : null);
        if (ecoId != null) {
            return ecoItems(ecoId);
        }

        // MMOItems shorthand: mmoitems: TYPE:ID (or just ID to search all types)
        String mmoShorthand = firstNonBlank(
                section.getString("mmoitems"),
                section.getString("mmo-item"));
        if (mmoShorthand != null) {
            return parseMmoShorthand(mmoShorthand);
        }

        String mmoType = firstNonBlank(
                section.getString("mmoitems_type"),
                section.getString("mmo-type"),
                legacyType.equalsIgnoreCase("mmoitems") || legacyType.equalsIgnoreCase("ia")
                        ? section.getString("namespace") : null);
        String mmoId = firstNonBlank(
                section.getString("mmoitems_id"),
                section.getString("mmo-id"),
                legacyType.equalsIgnoreCase("mmoitems") || legacyType.equalsIgnoreCase("ia")
                        ? section.getString("id") : null);
        if (mmoType != null && mmoId != null) {
            return mmoItems(mmoType, mmoId);
        }

        Material material = Material.matchMaterial(section.getString("material", "AIR"));
        if (material == null || material.isAir()) {
            return null;
        }

        String name = section.getString("name");
        List<String> lore = section.getStringList("lore");
        boolean hasModelData = section.contains("custom_model_data");
        int modelData = hasModelData ? section.getInt("custom_model_data") : 0;

        if ((name == null || name.isBlank()) && lore.isEmpty() && !hasModelData) {
            return vanilla(material);
        }

        ItemStack stack = new ItemStack(material);
        stack.editMeta(meta -> {
            if (name != null && !name.isBlank()) {
                meta.displayName(Text.name(name));
            }
            if (!lore.isEmpty()) {
                meta.lore(Text.lore(lore));
            }
            if (hasModelData) {
                meta.setCustomModelData(modelData);
            }
        });
        return custom(stack);
    }

    private static ItemSpec parseMmoShorthand(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        int colon = trimmed.indexOf(':');
        if (colon > 0 && colon < trimmed.length() - 1) {
            return mmoItems(trimmed.substring(0, colon).trim(), trimmed.substring(colon + 1).trim());
        }
        return mmoItemsAny(trimmed);
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
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
    public static ItemSpec fromSection(ConfigurationSection section) {
        return fromItemSection(section);
    }
}

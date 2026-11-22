package lk.cwresports.theforge.model;

import org.bukkit.Material;

public final class ForgeCategory {

    private final String id;
    private final String displayName;
    private final String iconName;
    private final Material icon;
    private final int order;
    private final int requiredLevel;

    public ForgeCategory(String id, String displayName, String iconName, Material icon, int order, int requiredLevel) {
        this.id = id;
        this.displayName = displayName;
        this.iconName = iconName;
        this.icon = icon;
        this.order = order;
        this.requiredLevel = Math.max(1, requiredLevel);
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Optional display name used for the category icon inside the GUI.
     * Falls back to {@link #getDisplayName()} when not configured.
     */
    public String getIconName() {
        return iconName;
    }

    public String iconDisplayName() {
        return iconName == null || iconName.isBlank() ? displayName : iconName;
    }

    public Material getIcon() {
        return icon;
    }

    public int getOrder() {
        return order;
    }

    public int getRequiredLevel() {
        return requiredLevel;
    }
}

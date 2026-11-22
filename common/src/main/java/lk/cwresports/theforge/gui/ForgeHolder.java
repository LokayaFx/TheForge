package lk.cwresports.theforge.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

public abstract class ForgeHolder implements InventoryHolder {

    private final UUID uuid;
    private Inventory inventory;

    protected ForgeHolder(UUID uuid) {
        this.uuid = uuid;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public UUID getUuid() {
        return uuid;
    }

    public static final class Main extends ForgeHolder {
        public Main(UUID uuid) {
            super(uuid);
        }
    }

    public static final class Category extends ForgeHolder {
        private final int selectedSlot;
        private final int page;

        public Category(UUID uuid, int selectedSlot, int page) {
            super(uuid);
            this.selectedSlot = selectedSlot;
            this.page = page;
        }

        public int getSelectedSlot() {
            return selectedSlot;
        }

        public int getPage() {
            return page;
        }
    }

    public static final class Recipe extends ForgeHolder {
        private final int selectedSlot;
        private final String categoryId;
        private final int page;

        public Recipe(UUID uuid, int selectedSlot, String categoryId, int page) {
            super(uuid);
            this.selectedSlot = selectedSlot;
            this.categoryId = categoryId;
            this.page = page;
        }

        public int getSelectedSlot() {
            return selectedSlot;
        }

        public String getCategoryId() {
            return categoryId;
        }

        public int getPage() {
            return page;
        }
    }
}

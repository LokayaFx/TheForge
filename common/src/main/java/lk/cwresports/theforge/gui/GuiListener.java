package lk.cwresports.theforge.gui;

import lk.cwresports.theforge.TheForgePlugin;
import lk.cwresports.theforge.model.ForgeCategory;
import lk.cwresports.theforge.model.ForgeJob;
import lk.cwresports.theforge.model.ForgeRecipe;
import lk.cwresports.theforge.model.PlayerData;
import lk.cwresports.theforge.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

import java.util.List;

public final class GuiListener implements Listener {

    private final TheForgePlugin plugin;

    public GuiListener(TheForgePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof ForgeHolder holder)) {
            return;
        }
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!holder.getUuid().equals(player.getUniqueId())) {
            return;
        }
        if (event.getClickedInventory() == null
                || !event.getClickedInventory().equals(event.getView().getTopInventory())) {
            return;
        }

        int slot = event.getRawSlot();
        if (holder instanceof ForgeHolder.Main) {
            handleMain(player, slot);
        } else if (holder instanceof ForgeHolder.Category category) {
            handleCategory(player, category, slot);
        } else if (holder instanceof ForgeHolder.Recipe recipe) {
            handleRecipe(player, recipe, slot);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof ForgeHolder) {
            event.setCancelled(true);
        }
    }

    private void handleMain(Player player, int slot) {
        if (slot == plugin.getGuiConfig().slot("main", "milestone", 48)) {
            next(() -> plugin.getGui().openCategories(player, 0, 0));
            return;
        }
        int forgePosition = plugin.getGui().mainForgePosition(slot);
        if (forgePosition >= 0) {
            int forgeSlot = forgePosition + 1;
            PlayerData data = plugin.getPlayers().get(player.getUniqueId());
            int level = plugin.getForge().levelOf(data);
            if (!plugin.getSlots().isUnlocked(player, level, forgeSlot)) {
                plugin.getMessages().send(player, "slot-locked-requirements",
                        "slot", String.valueOf(forgeSlot),
                        "requirements", String.join(", ", plugin.getSlots().unmet(player, level, forgeSlot)));
                return;
            }
            ForgeJob job = data.getJob(forgeSlot);
            if (job == null) {
                next(() -> plugin.getGui().openCategories(player, forgeSlot, 0));
                return;
            }
            if (job.isComplete(System.currentTimeMillis() / 1000L)) {
                plugin.getForge().collect(player, forgeSlot);
                plugin.getGui().refresh(player);
            } else {
                plugin.getMessages().send(player, "job-active",
                        "recipe", job.getDisplayName(),
                        "time", TimeUtil.format(job.getRemaining(System.currentTimeMillis() / 1000L)));
            }
            return;
        }
        if (slot == plugin.getGuiConfig().slot("main", "collect-all", 49)) {
            plugin.getForge().collectAll(player);
            plugin.getGui().refresh(player);
            return;
        }
        if (slot == plugin.getGuiConfig().slot("main", "close", 50)) {
            player.closeInventory();
            return;
        }
    }

    private void handleCategory(Player player, ForgeHolder.Category holder, int slot) {
        int position = plugin.getGui().categoryPosition(slot);
        if (position >= 0) {
            List<ForgeCategory> categories = plugin.getRecipes().getCategories();
            int index = holder.getPage() * ForgeGui.categoryPageSize() + position;
            if (index < categories.size()) {
                ForgeCategory category = categories.get(index);
                PlayerData data = plugin.getPlayers().get(player.getUniqueId());
                int level = plugin.getForge().levelOf(data);
                if (level < category.getRequiredLevel()) {
                    plugin.getMessages().send(player, "category-level-locked",
                            "category", category.getDisplayName(),
                            "level", String.valueOf(category.getRequiredLevel()));
                    return;
                }
                next(() -> plugin.getGui().openRecipes(player, holder.getSelectedSlot(),
                        category.getId(), 0));
            }
            return;
        }
        if (slot == plugin.getGuiConfig().slot("categories", "back", 22)) {
            next(() -> plugin.getGui().openMain(player));
        } else if (slot == plugin.getGuiConfig().slot("categories", "close", 26)) {
            player.closeInventory();
        } else if (slot == plugin.getGuiConfig().slot("categories", "previous", 18)) {
            next(() -> plugin.getGui().openCategories(player, holder.getSelectedSlot(), holder.getPage() - 1));
        } else if (slot == plugin.getGuiConfig().slot("categories", "next", 25)) {
            next(() -> plugin.getGui().openCategories(player, holder.getSelectedSlot(), holder.getPage() + 1));
        }
    }

    private void handleRecipe(Player player, ForgeHolder.Recipe holder, int slot) {
        if (slot == plugin.getGuiConfig().slot("recipes", "previous", 45)) {
            next(() -> plugin.getGui().openRecipes(player, holder.getSelectedSlot(),
                    holder.getCategoryId(), holder.getPage() - 1));
            return;
        }
        if (slot == plugin.getGuiConfig().slot("recipes", "next", 53)) {
            next(() -> plugin.getGui().openRecipes(player, holder.getSelectedSlot(),
                    holder.getCategoryId(), holder.getPage() + 1));
            return;
        }
        if (slot == plugin.getGuiConfig().slot("recipes", "back", 49)) {
            next(() -> plugin.getGui().openCategories(player, holder.getSelectedSlot(), 0));
            return;
        }
        if (slot == plugin.getGuiConfig().slot("recipes", "close", 51)) {
            player.closeInventory();
            return;
        }

        int position = plugin.getGui().recipePosition(slot);
        if (position < 0) {
            return;
        }
        List<ForgeRecipe> recipes = plugin.getRecipes().getRecipes(holder.getCategoryId());
        int index = holder.getPage() * ForgeGui.recipePageSize() + position;
        if (index >= recipes.size()) {
            return;
        }

        ForgeRecipe recipe = recipes.get(index);
        int targetSlot = holder.getSelectedSlot();
        PlayerData data = plugin.getPlayers().get(player.getUniqueId());
        if (targetSlot <= 0) {
            targetSlot = plugin.getForge().firstFreeSlot(player, data);
        }
        if (targetSlot <= 0) {
            plugin.getMessages().send(player, "gui-no-slot");
            return;
        }
        if (plugin.getForge().start(player, targetSlot, recipe)) {
            next(() -> plugin.getGui().openMain(player));
        }
    }

    private void next(Runnable runnable) {
        Bukkit.getScheduler().runTask(plugin, runnable);
    }
}

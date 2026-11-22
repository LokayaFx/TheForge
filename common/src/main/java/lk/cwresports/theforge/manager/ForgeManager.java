package lk.cwresports.theforge.manager;

import lk.cwresports.theforge.TheForgePlugin;
import lk.cwresports.theforge.model.ForgeCategory;
import lk.cwresports.theforge.model.ForgeJob;
import lk.cwresports.theforge.model.ForgeRecipe;
import lk.cwresports.theforge.model.Ingredient;
import lk.cwresports.theforge.model.ItemSpec;
import lk.cwresports.theforge.model.PlayerData;
import lk.cwresports.theforge.util.Log;
import lk.cwresports.theforge.util.Text;
import lk.cwresports.theforge.util.TimeUtil;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ForgeManager {

    private final TheForgePlugin plugin;

    public ForgeManager(TheForgePlugin plugin) {
        this.plugin = plugin;
    }

    private long now() {
        return System.currentTimeMillis() / 1000L;
    }

    public void handleJoin(Player player) {
        PlayerData data = plugin.getPlayers().get(player.getUniqueId());
        evaluate(player, data, false);
        int ready = readyCount(data);
        Log.debug("Join " + player.getName() + ": level="
                + levelOf(data)
                + ", xp=" + data.getXp()
                + ", jobs=" + data.getJobs().size());
        if (ready > 0) {
            Log.gameplay(player.getName() + " has " + ready + " completed job(s) waiting.");
            plugin.getMessages().send(player, "ready-join", "count", String.valueOf(ready));
        }
    }

    public void handleQuit(Player player) {
        Log.debug("Quit " + player.getName() + "; saving forge data.");
        plugin.getPlayers().unload(player.getUniqueId());
    }

    public void tick() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerData data = plugin.getPlayers().getIfLoaded(player.getUniqueId());
            if (data == null) {
                continue;
            }
            evaluate(player, data, true);
            plugin.getGui().refresh(player);
        }
    }

    /**
     * Credits completed jobs, levels the player up and optionally notifies them.
     */
    public boolean evaluate(Player player, PlayerData data, boolean notify) {
        long now = now();
        int oldLevel = levelOf(data);
        boolean changed = false;

        for (ForgeJob job : new ArrayList<>(data.getJobs().values())) {
            if (job.isCredited() || !job.isComplete(now)) {
                continue;
            }
            job.setCredited(true);
            int xp = job.getExperience();
            data.addXp(xp);
            data.addTotalSeconds(job.getBaseSeconds());
            changed = true;
            Log.gameplay(player.getName() + " completed " + job.getRecipeId()
                    + " in slot " + job.getSlot()
                    + " (+" + xp + " Forge XP -> " + data.getXp() + " XP)");
            if (notify) {
                plugin.getMessages().send(player, "ready",
                        "recipe", job.getDisplayName(),
                        "slot", String.valueOf(job.getSlot()),
                        "xp", String.valueOf(xp));
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.2F);
            }
        }

        int newLevel = levelOf(data);
        if (newLevel > oldLevel) {
            announceLevelUp(player, oldLevel, newLevel);
            changed = true;
        }
        if (changed) {
            plugin.getPlayers().save(data);
        }
        return changed;
    }

    private void announceLevelUp(Player player, int oldLevel, int newLevel) {
        int reduction = plugin.getLevels().reductionPercent(newLevel);
        int slots = plugin.getSlots().unlockedCount(player, newLevel);
        Log.gameplay(player.getName() + " reached forge level " + newLevel
                + " (" + reduction + "% time reduction, " + slots + " slots)");
        plugin.getMessages().send(player, "level-up",
                "level", String.valueOf(newLevel),
                "reduction", String.valueOf(reduction),
                "slots", String.valueOf(slots));

        player.showTitle(Title.title(
                Text.color(plugin.getMessages().format("level-up-title",
                        "level", String.valueOf(newLevel))),
                Text.color(plugin.getMessages().format("level-up-subtitle",
                        "reduction", String.valueOf(reduction))),
                Title.Times.times(Duration.ofMillis(200), Duration.ofSeconds(1), Duration.ofMillis(300))));
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);

        for (int slot = 1; slot <= plugin.getSlots().maxSlots(); slot++) {
            if (!plugin.getSlots().isUnlocked(player, oldLevel, slot)
                    && plugin.getSlots().isUnlocked(player, newLevel, slot)) {
                plugin.getMessages().send(player, "slot-unlocked", "slot", String.valueOf(slot));
            }
        }
    }

    public boolean start(Player player, int slot, ForgeRecipe recipe) {
        if (recipe == null) {
            return false;
        }
        PlayerData data = plugin.getPlayers().get(player.getUniqueId());
        evaluate(player, data, false);

        int maxSlots = plugin.getSlots().maxSlots();
        if (slot < 1 || slot > maxSlots) {
            Log.debug(player.getName() + " tried invalid forge slot " + slot
                    + " (max " + maxSlots + ").");
            plugin.getMessages().send(player, "invalid-slot");
            return false;
        }
        int level = levelOf(data);
        if (!plugin.getSlots().isUnlocked(player, level, slot)) {
            List<String> unmet = plugin.getSlots().unmet(player, level, slot);
            Log.debug(player.getName() + " tried locked slot " + slot + " at level " + level
                    + " (missing: " + String.join(", ", unmet) + ").");
            plugin.getMessages().send(player, "slot-locked-requirements",
                    "slot", String.valueOf(slot),
                    "requirements", String.join(", ", unmet));
            return false;
        }
        if (data.getJob(slot) != null) {
            Log.debug(player.getName() + " tried busy slot " + slot + ".");
            plugin.getMessages().send(player, "slot-busy");
            return false;
        }
        ForgeCategory category = plugin.getRecipes().getCategory(recipe.getCategoryId());
        if (category != null && level < category.getRequiredLevel()) {
            Log.debug(player.getName() + " tried locked category " + category.getId()
                    + " at level " + level + ".");
            plugin.getMessages().send(player, "category-level-locked",
                    "category", category.getDisplayName(),
                    "level", String.valueOf(category.getRequiredLevel()));
            return false;
        }
        if (level < recipe.getRequiredLevel()) {
            Log.debug(player.getName() + " tried locked recipe " + recipe.getId()
                    + " at level " + level + ".");
            plugin.getMessages().send(player, "recipe-level-locked",
                    "recipe", recipe.displayName(),
                    "level", String.valueOf(recipe.getRequiredLevel()));
            return false;
        }
        List<String> missing = new ArrayList<>();
        for (Ingredient ingredient : recipe.getIngredients()) {
            int have = count(player, ingredient.getItem());
            if (have < ingredient.getAmount()) {
                missing.add(ingredient.displayName() + " x" + (ingredient.getAmount() - have));
            }
        }
        if (!missing.isEmpty()) {
            Log.debug(player.getName() + " is missing ingredients for " + recipe.getId()
                    + ": " + String.join(", ", missing));
            plugin.getMessages().send(player, "missing-ingredients", "list", String.join(", ", missing));
            return false;
        }

        ItemStack result = recipe.createResult();
        if (result == null) {
            Log.warn("Could not create result item for recipe " + recipe.getId()
                    + " for " + player.getName() + ".");
            plugin.getMessages().send(player, "result-broken");
            return false;
        }

        for (Ingredient ingredient : recipe.getIngredients()) {
            remove(player, ingredient.getItem(), ingredient.getAmount());
        }

        int duration = effectiveSeconds(recipe, level);
        ForgeJob job = new ForgeJob(slot, recipe.getId(), recipe.displayName(),
                result.serializeAsBytes(), recipe.getBaseSeconds(), duration,
                recipe.getExperience(), now());
        data.putJob(job);
        plugin.getPlayers().save(data);
        Log.gameplay(player.getName() + " started " + recipe.getId() + " in slot " + slot
                + " (" + TimeUtil.format(duration) + ", level " + level + ")");

        plugin.getMessages().send(player, "started",
                "recipe", recipe.displayName(),
                "slot", String.valueOf(slot),
                "time", TimeUtil.format(duration));
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 0.8F, 1.1F);
        return true;
    }

    public boolean collect(Player player, int slot) {
        return collect(player, slot, true);
    }

    private boolean collect(Player player, int slot, boolean announce) {
        PlayerData data = plugin.getPlayers().get(player.getUniqueId());
        evaluate(player, data, false);
        ForgeJob job = data.getJob(slot);
        if (job == null) {
            return false;
        }
        long now = now();
        if (!job.isComplete(now)) {
            if (announce) {
                plugin.getMessages().send(player, "job-active",
                        "recipe", job.getDisplayName(),
                        "time", TimeUtil.format(job.getRemaining(now)));
            }
            return false;
        }

        ItemStack item = job.resultItem();
        if (item == null) {
            Log.warn("Could not restore result item for " + player.getName()
                    + " in slot " + slot + ".");
            plugin.getMessages().send(player, "result-broken");
            return false;
        }
        if (!canFit(player, item)) {
            if (announce) {
                plugin.getMessages().send(player, "inventory-full");
            }
            return false;
        }

        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(item);
        for (ItemStack leftover : leftovers.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }
        data.removeJob(slot);
        plugin.getPlayers().save(data);
        if (announce) {
            Log.gameplay(player.getName() + " claimed " + item.getAmount() + "x "
                    + job.getDisplayName() + " from slot " + slot);
            plugin.getMessages().send(player, "claimed",
                    "item", job.getDisplayName() + " x" + item.getAmount(),
                    "slot", String.valueOf(slot));
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.9F, 1.1F);
        }
        return true;
    }

    public int collectAll(Player player) {
        PlayerData data = plugin.getPlayers().get(player.getUniqueId());
        evaluate(player, data, false);

        long now = now();
        int ready = 0;
        for (ForgeJob job : data.getJobs().values()) {
            if (job.isComplete(now)) {
                ready++;
            }
        }
        if (ready == 0) {
            plugin.getMessages().send(player, "nothing-ready");
            return 0;
        }

        int claimed = 0;
        int failed = 0;
        for (int slot : new ArrayList<>(data.getJobs().keySet())) {
            ForgeJob job = data.getJob(slot);
            if (job == null || !job.isComplete(now)) {
                continue;
            }
            if (collect(player, slot, false)) {
                claimed++;
            } else {
                failed++;
            }
        }

        if (claimed > 0) {
            plugin.getMessages().send(player, "claimed-all", "count", String.valueOf(claimed));
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.9F, 1.1F);
        }
        if (failed > 0) {
            if (claimed == 0) {
                plugin.getMessages().send(player, "inventory-full");
            } else {
                plugin.getMessages().send(player, "claim-all-partial",
                        "count", String.valueOf(claimed),
                        "left", String.valueOf(failed));
            }
        }
        if (claimed > 0) {
            Log.gameplay(player.getName() + " claimed " + claimed + " job(s) in one batch"
                    + (failed > 0 ? " (" + failed + " failed)" : "") + ".");
        }
        return claimed;
    }

    public int count(Player player, ItemSpec spec) {
        int count = 0;
        for (ItemStack stack : player.getInventory().getStorageContents()) {
            if (stack != null && spec.matches(stack)) {
                count += stack.getAmount();
            }
        }
        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (offhand != null && spec.matches(offhand)) {
            count += offhand.getAmount();
        }
        return count;
    }

    private void remove(Player player, ItemSpec spec, int amount) {
        int remaining = amount;
        ItemStack[] storage = player.getInventory().getStorageContents();
        for (int index = 0; index < storage.length && remaining > 0; index++) {
            ItemStack stack = storage[index];
            if (stack == null || !spec.matches(stack)) {
                continue;
            }
            int take = Math.min(remaining, stack.getAmount());
            int left = stack.getAmount() - take;
            remaining -= take;
            if (left <= 0) {
                player.getInventory().setItem(index, null);
            } else {
                stack.setAmount(left);
                player.getInventory().setItem(index, stack);
            }
        }
        if (remaining > 0) {
            ItemStack offhand = player.getInventory().getItemInOffHand();
            if (offhand != null && spec.matches(offhand)) {
                int take = Math.min(remaining, offhand.getAmount());
                int left = offhand.getAmount() - take;
                if (left <= 0) {
                    player.getInventory().setItemInOffHand(null);
                } else {
                    offhand.setAmount(left);
                    player.getInventory().setItemInOffHand(offhand);
                }
            }
        }
    }

    private boolean canFit(Player player, ItemStack item) {
        int remaining = item.getAmount();
        int maxStack = Math.max(1, item.getMaxStackSize());
        for (ItemStack content : player.getInventory().getStorageContents()) {
            if (content == null || content.getType().isAir()) {
                remaining -= maxStack;
            } else if (content.isSimilar(item)) {
                remaining -= Math.max(0, maxStack - content.getAmount());
            }
            if (remaining <= 0) {
                return true;
            }
        }
        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (offhand == null || offhand.getType().isAir()) {
            remaining -= maxStack;
        } else if (offhand.isSimilar(item)) {
            remaining -= Math.max(0, maxStack - offhand.getAmount());
        }
        return remaining <= 0;
    }

    public boolean canCraft(Player player, ForgeRecipe recipe) {
        PlayerData data = plugin.getPlayers().getIfLoaded(player.getUniqueId());
        if (data == null) {
            return false;
        }
        for (Ingredient ingredient : recipe.getIngredients()) {
            if (count(player, ingredient.getItem()) < ingredient.getAmount()) {
                return false;
            }
        }
        return true;
    }

    public int firstFreeSlot(Player player, PlayerData data) {
        int level = levelOf(data);
        for (int slot = 1; slot <= plugin.getSlots().maxSlots(); slot++) {
            if (plugin.getSlots().isUnlocked(player, level, slot) && data.getJob(slot) == null) {
                return slot;
            }
        }
        return 0;
    }

    public int readyCount(PlayerData data) {
        long now = now();
        int count = 0;
        for (ForgeJob job : data.getJobs().values()) {
            if (job.isComplete(now)) {
                count++;
            }
        }
        return count;
    }

    public int effectiveSeconds(ForgeRecipe recipe, int level) {
        double reduction = plugin.getLevels().reductionFor(level);
        return Math.max(1, (int) Math.ceil(recipe.getBaseSeconds() * (1.0D - reduction)));
    }

    public int levelOf(PlayerData data) {
        return data == null ? 1 : plugin.getLevels().levelFor(data.getXp());
    }

    public int remainingSeconds(ForgeJob job) {
        return job.getRemaining(now());
    }
}

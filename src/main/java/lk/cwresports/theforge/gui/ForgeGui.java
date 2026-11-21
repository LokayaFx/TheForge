package lk.cwresports.theforge.gui;

import lk.cwresports.theforge.TheForgePlugin;
import lk.cwresports.theforge.hook.AureliumSkillsHook;
import lk.cwresports.theforge.hook.EcoSkillsHook;
import lk.cwresports.theforge.model.ForgeCategory;
import lk.cwresports.theforge.model.ForgeJob;
import lk.cwresports.theforge.model.ForgeRecipe;
import lk.cwresports.theforge.model.Ingredient;
import lk.cwresports.theforge.model.PlayerData;
import lk.cwresports.theforge.model.SlotRequirement;
import lk.cwresports.theforge.util.Text;
import lk.cwresports.theforge.util.TimeUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ForgeGui {

    private static final Pattern TEXTURE_URL = Pattern.compile(
            "https?://textures\\.minecraft\\.net/texture/[A-Za-z0-9]+"
    );

    public static final int[] FORGE_SLOTS = {10, 11, 12, 13, 14, 15, 16};
    private static final int[] PROGRESS_SLOTS = {
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };
    private static final int[] CATEGORY_SLOTS = {10, 11, 12, 13, 14, 15, 16};
    private static final int[] RECIPE_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };
    private static final int CATEGORY_PAGE_SIZE = CATEGORY_SLOTS.length;
    private static final int RECIPE_PAGE_SIZE = RECIPE_SLOTS.length;

    private final TheForgePlugin plugin;

    public ForgeGui(TheForgePlugin plugin) {
        this.plugin = plugin;
    }

    public void openMain(Player player) {
        PlayerData data = plugin.getPlayers().get(player.getUniqueId());
        ForgeHolder.Main holder = new ForgeHolder.Main(player.getUniqueId());
        String title = plugin.getGuiConfig().title("main", "&8The Forge");
        Inventory inventory = Bukkit.createInventory(holder, 54, Text.color(title));
        holder.setInventory(inventory);
        renderMain(inventory, player, data);
        player.openInventory(inventory);
    }

    public void refresh(Player player) {
        Inventory top = player.getOpenInventory().getTopInventory();
        if (!(top.getHolder() instanceof ForgeHolder.Main holder)) {
            return;
        }
        if (!holder.getUuid().equals(player.getUniqueId())) {
            return;
        }
        PlayerData data = plugin.getPlayers().getIfLoaded(player.getUniqueId());
        if (data == null) {
            return;
        }
        renderMain(top, player, data);
    }

    private void renderMain(Inventory inventory, Player player, PlayerData data) {
        inventory.clear();
        ItemStack filler = filler("main");
        for (int index = 0; index < inventory.getSize(); index++) {
            inventory.setItem(index, filler);
        }

        int level = plugin.getLevels().levelFor(data.getXp());
        int unlocked = plugin.getSlots().unlockedCount(player, level);

        inventory.setItem(plugin.getGuiConfig().slot("main", "milestone", 48), milestoneItem(data, level, unlocked));
        int[] forgeSlots = forgeSlots();
        for (int index = 0; index < forgeSlots.length; index++) {
            inventory.setItem(forgeSlots[index], slotItem(player, data, index + 1, level));
        }
        renderProgressPanes(inventory, data, forgeSlots.length);

        inventory.setItem(plugin.getGuiConfig().slot("main", "collect-all", 49), claimItem(data));
        inventory.setItem(plugin.getGuiConfig().slot("main", "close", 50), customItem("main", "close",
                Material.BARRIER, msg("gui.close-name"), List.of()));
    }

    private ItemStack slotItem(Player player, PlayerData data, int slot, int level) {
        if (!plugin.getSlots().isUnlocked(player, level, slot)) {
            SlotRequirement requirement = plugin.getSlots().requirement(slot);
            List<String> lore = new ArrayList<>();
            if (level < requirement.forgeLevel()) {
                int requiredXp = plugin.getLevels().requirementFor(requirement.forgeLevel());
                lore.add(msg("gui.slot-locked-lore", "level", String.valueOf(requirement.forgeLevel())));
                lore.add(msg("gui.slot-locked-xp", "xp", String.valueOf(requiredXp)));
                lore.add("&7Progress: &e" + data.getXp() + " &7/ &e" + requiredXp + " &7XP");
            }
            for (var entry : requirement.aureliumSkills().entrySet()) {
                int current = AureliumSkillsHook.isAvailable()
                        ? AureliumSkillsHook.getLevel(player, entry.getKey())
                        : -1;
                lore.add(msg("gui.slot-requirement-skill",
                        "plugin", AureliumSkillsHook.displayName(),
                        "skill", entry.getKey(),
                        "level", String.valueOf(entry.getValue()),
                        "current", current < 0 ? "?" : String.valueOf(current)));
            }
            for (var entry : requirement.ecoSkills().entrySet()) {
                int current = EcoSkillsHook.isAvailable()
                        ? EcoSkillsHook.getLevel(player, entry.getKey())
                        : -1;
                lore.add(msg("gui.slot-requirement-skill",
                        "plugin", "EcoSkills",
                        "skill", entry.getKey(),
                        "level", String.valueOf(entry.getValue()),
                        "current", current < 0 ? "?" : String.valueOf(current)));
            }
            return customItem("main", "locked-slot", Material.SMOKER,
                    msg("gui.slot-locked-name", "slot", String.valueOf(slot)), lore,
                    "slot", String.valueOf(slot), "level", String.valueOf(requirement.forgeLevel()),
                    "requirements", String.join(", ", plugin.getSlots().unmet(player, level, slot)));
        }

        ForgeJob job = data.getJob(slot);
        if (job == null) {
            return customItem("main", "empty-slot", Material.FURNACE,
                    msg("gui.slot-empty-name", "slot", String.valueOf(slot)),
                    List.of(msg("gui.slot-empty-lore"), msg("gui.slot-click")), "slot", String.valueOf(slot));
        }

        long now = System.currentTimeMillis() / 1000L;
        ItemStack display = job.resultItem();
        if (display == null) {
            display = simpleItem(Material.BARRIER, job.getDisplayName(), List.of());
        }

        List<String> lore = new ArrayList<>();
        if (job.isComplete(now)) {
            lore.add(msg("gui.slot-complete-lore"));
            lore.add(msg("gui.slot-claim-click"));
            lore = new ArrayList<>(plugin.getGuiConfig().itemLore("main", "completed-slot", lore,
                    "slot", String.valueOf(slot), "recipe", job.getDisplayName()));
            return decorate(display, lore, true);
        }

        double progress = job.getProgress(now);
        lore.add("&7Recipe: &f" + job.getDisplayName());
        lore.add(Text.bar(progress, 20) + " &e" + (int) Math.round(progress * 100.0D) + "%");
        lore.add(msg("gui.slot-time-left", "time", TimeUtil.format(job.getRemaining(now))));
        lore = new ArrayList<>(plugin.getGuiConfig().itemLore("main", "active-slot", lore,
                "slot", String.valueOf(slot), "recipe", job.getDisplayName(),
                "time", TimeUtil.format(job.getRemaining(now)),
                "progress", String.valueOf((int) Math.round(progress * 100.0D))));
        return decorate(display, lore, false);
    }

    /** Renders three pane milestones beneath each forge slot: 50%, 75% and 100%. */
    private void renderProgressPanes(Inventory inventory, PlayerData data, int forgeSlotCount) {
        int[] progressSlots = progressSlots();
        long now = System.currentTimeMillis() / 1000L;
        double[] milestones = {0.50D, 0.75D, 1.00D};

        for (int forgeIndex = 0; forgeIndex < forgeSlotCount; forgeIndex++) {
            ForgeJob job = data.getJob(forgeIndex + 1);
            double progress = job == null ? 0.0D : job.getProgress(now);
            for (int milestoneIndex = 0; milestoneIndex < milestones.length; milestoneIndex++) {
                int progressIndex = milestoneIndex * forgeSlotCount + forgeIndex;
                boolean complete = progress >= milestones[milestoneIndex];
                String key = complete ? "progress-complete" : "progress-pending";
                Material material = complete ? Material.GREEN_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE;
                inventory.setItem(progressSlots[progressIndex], customItem("main", key, material,
                        " ", List.of(), "progress", String.valueOf((int) Math.round(progress * 100.0D)),
                        "milestone", String.valueOf((int) Math.round(milestones[milestoneIndex] * 100.0D))));
            }
        }
    }

    private ItemStack milestoneItem(PlayerData data, int level, int slots) {
        double progress = plugin.getLevels().progressToNext(data.getXp());
        int percent = (int) Math.round(progress * 100.0D);
        int next = plugin.getLevels().nextRequirement(level);
        String nextText = next < 0 ? "MAX" : String.valueOf(next);
        String bar = Text.lineBar(progress, 20);
        String progressText = next < 0 ? "MAX LEVEL" : data.getXp() + "/" + next;

        int total = plugin.getRecipes().recipeCount();
        int unlocked = plugin.getRecipes().unlockedRecipeCount(level);
        double unlockedProgress = total == 0 ? 1.0D : unlocked / (double) total;
        int unlockedPercent = (int) Math.round(unlockedProgress * 100.0D);
        String unlockedBar = Text.lineBar(unlockedProgress, 20);

        List<String> lore = new ArrayList<>();
        lore.add(msg("gui.level-progress", "xp", String.valueOf(data.getXp())));
        lore.add(msg("gui.level-next", "xp", nextText, "percent", String.valueOf(percent)));
        lore.add(bar + " &e" + progressText);
        if (next >= 0) {
            lore.add("&7Need: &e" + Math.max(0, next - data.getXp()) + " &7more XP");
        }
        lore.add(msg("gui.level-reduction",
                "reduction", String.valueOf(plugin.getLevels().reductionPercent(level))));
        lore.add(msg("gui.level-slots",
                "used", String.valueOf(data.getJobs().size()),
                "slots", String.valueOf(slots)));
        lore.add("");
        lore.add(msg("gui.recipe-book-description-1"));
        lore.add(msg("gui.recipe-book-description-2"));
        lore.add("");
        lore.add(msg("gui.recipe-book-unlocked", "unlocked-percent", String.valueOf(unlockedPercent)));
        lore.add(msg("gui.recipe-book-count", "unlocked-bar", unlockedBar,
                "unlocked", String.valueOf(unlocked), "total", String.valueOf(total)));
        lore.add("");
        lore.add(msg("gui.recipe-book-click"));

        return customItem("main", "milestone", Material.KNOWLEDGE_BOOK,
                msg("gui.milestone-name", "level", String.valueOf(level)), lore,
                "level", String.valueOf(level), "xp", String.valueOf(data.getXp()),
                "reduction", String.valueOf(plugin.getLevels().reductionPercent(level)),
                "used", String.valueOf(data.getJobs().size()), "slots", String.valueOf(slots),
                "next", nextText, "percent", String.valueOf(percent),
                "bar", bar, "progress", progressText,
                "unlocked", String.valueOf(unlocked), "total", String.valueOf(total),
                "unlocked-percent", String.valueOf(unlockedPercent), "unlocked-bar", unlockedBar);
    }

    private ItemStack claimItem(PlayerData data) {
        int ready = plugin.getForge().readyCount(data);
        return customItem("main", "collect-all", Material.HOPPER, msg("gui.claim-name"), List.of(
                msg("gui.claim-lore", "count", String.valueOf(ready)),
                msg("gui.claim-click")
        ), "count", String.valueOf(ready));
    }

    private ItemStack categoryItem(Player player, ForgeCategory category) {
        int count = plugin.getRecipes().countRecipes(category.getId());
        PlayerData data = plugin.getPlayers().getIfLoaded(player.getUniqueId());
        int level = plugin.getForge().levelOf(data);
        boolean locked = level < category.getRequiredLevel();
        List<String> lore = new ArrayList<>();
        lore.add("&7Recipes: &e" + count);
        if (locked) {
            lore.add(msg("gui.category-locked-lore", "level", String.valueOf(category.getRequiredLevel())));
        } else {
            lore.add("&eClick to browse");
        }
        Material icon = locked ? Material.BARRIER : category.getIcon();
        String name = locked
                ? msg("gui.category-locked-name", "name", Text.strip(category.iconDisplayName()),
                        "level", String.valueOf(category.getRequiredLevel()))
                : category.iconDisplayName();
        return customItem("categories", "category-entry", icon, name, lore,
                "category", category.getId(), "name", Text.strip(category.iconDisplayName()),
                "count", String.valueOf(count), "level", String.valueOf(category.getRequiredLevel()));
    }

    public void openCategories(Player player, int selectedSlot, int page) {
        List<ForgeCategory> categories = plugin.getRecipes().getCategories();
        int maxPage = Math.max(0, (categories.size() - 1) / CATEGORY_PAGE_SIZE);
        int safePage = Math.max(0, Math.min(maxPage, page));

        ForgeHolder.Category holder = new ForgeHolder.Category(player.getUniqueId(), selectedSlot, safePage);
        Inventory inventory = Bukkit.createInventory(holder, 27,
                Text.color(plugin.getGuiConfig().title("categories", "&8Categories")));
        holder.setInventory(inventory);

        ItemStack filler = filler("categories");
        for (int index = 0; index < inventory.getSize(); index++) {
            inventory.setItem(index, filler);
        }

        String selected = selectedSlot > 0 ? "&7Slot: &e#" + selectedSlot : "&7Auto slot";
        inventory.setItem(plugin.getGuiConfig().slot("categories", "header", 4),
                customItem("categories", "header", Material.CHEST, "&6Categories", List.of(selected),
                        "slot", selectedSlot > 0 ? String.valueOf(selectedSlot) : "Auto"));

        int start = safePage * CATEGORY_PAGE_SIZE;
        for (int index = 0; index < CATEGORY_PAGE_SIZE; index++) {
            int categoryIndex = start + index;
            if (categoryIndex >= categories.size()) {
                break;
            }
            inventory.setItem(categorySlots()[index], categoryItem(player, categories.get(categoryIndex)));
        }
        if (categories.isEmpty()) {
            inventory.setItem(plugin.getGuiConfig().slot("categories", "empty", 13),
                    customItem("categories", "empty", Material.BARRIER, msg("gui.empty-name"), List.of()));
        }

        inventory.setItem(plugin.getGuiConfig().slot("categories", "back", 22),
                customItem("categories", "back", Material.ARROW, msg("gui.back-name"), List.of()));
        inventory.setItem(plugin.getGuiConfig().slot("categories", "close", 26),
                customItem("categories", "close", Material.BARRIER, msg("gui.close-name"), List.of()));
        if (maxPage > 0) {
            inventory.setItem(plugin.getGuiConfig().slot("categories", "page", 24), customItem("categories", "page", Material.PAPER,
                    msg("gui.page-name", "page", String.valueOf(safePage + 1),
                            "pages", String.valueOf(maxPage + 1)), List.of(),
                    "page", String.valueOf(safePage + 1), "pages", String.valueOf(maxPage + 1)));
            if (safePage > 0) {
                inventory.setItem(plugin.getGuiConfig().slot("categories", "previous", 18),
                        customItem("categories", "previous", Material.ARROW, msg("gui.prev-name"), List.of()));
            }
            if (safePage < maxPage) {
                inventory.setItem(plugin.getGuiConfig().slot("categories", "next", 25),
                        customItem("categories", "next", Material.ARROW, msg("gui.next-name"), List.of()));
            }
        }

        player.openInventory(inventory);
    }

    public void openRecipes(Player player, int selectedSlot, String categoryId, int page) {
        ForgeCategory category = plugin.getRecipes().getCategory(categoryId);
        if (category == null) {
            plugin.getMessages().send(player, "category-not-found", "category", categoryId);
            openMain(player);
            return;
        }
        List<ForgeRecipe> recipes = plugin.getRecipes().getRecipes(categoryId);
        int maxPage = Math.max(0, (recipes.size() - 1) / RECIPE_PAGE_SIZE);
        int safePage = Math.max(0, Math.min(maxPage, page));

        ForgeHolder.Recipe holder = new ForgeHolder.Recipe(player.getUniqueId(), selectedSlot, categoryId, safePage);
        String title = plugin.getGuiConfig().title("recipes", "&8Recipes - {category}")
                .replace("{category}", Text.strip(category.getDisplayName()));
        Inventory inventory = Bukkit.createInventory(holder, 54, Text.color(title));
        holder.setInventory(inventory);

        ItemStack filler = filler("recipes");
        for (int index = 0; index < inventory.getSize(); index++) {
            inventory.setItem(index, filler);
        }

        String selected = selectedSlot > 0 ? "&7Slot: &e#" + selectedSlot : "&7Auto slot";
        inventory.setItem(plugin.getGuiConfig().slot("recipes", "header", 4), customItem("recipes", "header",
                category.getIcon(), category.iconDisplayName(), List.of(
                "&7Recipes: &e" + recipes.size(),
                "&7Page: &e" + (safePage + 1) + "&7/&e" + (maxPage + 1),
                selected
        ), "category", category.getId(), "name", Text.strip(category.iconDisplayName()),
                "count", String.valueOf(recipes.size()), "page", String.valueOf(safePage + 1),
                "pages", String.valueOf(maxPage + 1),
                "slot", selectedSlot > 0 ? String.valueOf(selectedSlot) : "Auto"));

        int start = safePage * RECIPE_PAGE_SIZE;
        for (int index = 0; index < RECIPE_PAGE_SIZE; index++) {
            int recipeIndex = start + index;
            if (recipeIndex >= recipes.size()) {
                break;
            }
            inventory.setItem(recipeSlots()[index],
                    recipeItem(player, recipes.get(recipeIndex), selectedSlot));
        }

        inventory.setItem(plugin.getGuiConfig().slot("recipes", "previous", 45),
                customItem("recipes", "previous", Material.ARROW, msg("gui.prev-name"), List.of()));
        inventory.setItem(plugin.getGuiConfig().slot("recipes", "back", 49),
                customItem("recipes", "back", Material.ARROW, msg("gui.back-name"), List.of()));
        inventory.setItem(plugin.getGuiConfig().slot("recipes", "close", 51),
                customItem("recipes", "close", Material.BARRIER, msg("gui.close-name"), List.of()));
        inventory.setItem(plugin.getGuiConfig().slot("recipes", "next", 53),
                customItem("recipes", "next", Material.ARROW, msg("gui.next-name"), List.of()));

        player.openInventory(inventory);
    }

    private ItemStack recipeItem(Player player, ForgeRecipe recipe, int selectedSlot) {
        ItemStack result = recipe.createResult();
        if (result == null) {
            return simpleItem(Material.BARRIER, recipe.displayName(), List.of(msg("gui.recipe-locked")));
        }

        PlayerData data = plugin.getPlayers().getIfLoaded(player.getUniqueId());
        int level = plugin.getForge().levelOf(data);
        int effective = plugin.getForge().effectiveSeconds(recipe, level);
        boolean canCraft = plugin.getForge().canCraft(player, recipe);
        boolean levelLocked = level < recipe.getRequiredLevel();
        if (levelLocked) {
            result = simpleItem(Material.GRAY_DYE, "&7" + Text.strip(recipe.displayName()), List.of());
        }

        boolean slotFree;
        if (data == null) {
            slotFree = false;
        } else if (selectedSlot > 0) {
            slotFree = data.getJob(selectedSlot) == null
                    && plugin.getSlots().isUnlocked(player, level, selectedSlot);
        } else {
            slotFree = plugin.getForge().firstFreeSlot(player, data) > 0;
        }
        boolean ready = canCraft && slotFree && !levelLocked;

        List<String> lore = new ArrayList<>();
        lore.add(msg("gui.recipe-time",
                "time", TimeUtil.format(effective),
                "base", TimeUtil.format(recipe.getBaseSeconds())));
        lore.add(msg("gui.recipe-xp", "xp", String.valueOf(recipe.getExperience())));
        if (levelLocked) {
            lore.add(msg("gui.recipe-level-lore", "level", String.valueOf(recipe.getRequiredLevel())));
        }
        lore.add(msg("gui.recipe-ingredients"));
        for (Ingredient ingredient : recipe.getIngredients()) {
            int have = plugin.getForge().count(player, ingredient.getItem());
            String key = have >= ingredient.getAmount() ? "gui.recipe-have" : "gui.recipe-missing";
            lore.add(msg(key,
                    "amount", String.valueOf(have),
                    "needed", String.valueOf(ingredient.getAmount()),
                    "item", ingredient.displayName()));
        }
        lore.add("");
        if (levelLocked) {
            lore.add(msg("gui.recipe-locked"));
        } else {
            lore.add(ready ? msg("gui.recipe-click") : msg("gui.recipe-need"));
        }
        lore = new ArrayList<>(plugin.getGuiConfig().itemLore("recipes", "recipe-entry", lore,
                "recipe", recipe.getId(), "item", recipe.displayName(),
                "time", TimeUtil.format(effective), "base", TimeUtil.format(recipe.getBaseSeconds()),
                "xp", String.valueOf(recipe.getExperience()),
                "level", String.valueOf(recipe.getRequiredLevel())));
        return decorate(result, lore, ready);
    }

    private ItemStack simpleItem(Material material, String name, List<String> lore) {
        ItemStack stack = new ItemStack(material);
        stack.editMeta(meta -> {
            meta.displayName(Text.name(name));
            if (!lore.isEmpty()) {
                meta.lore(Text.lore(lore));
            }
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        });
        return stack;
    }

    private ItemStack decorate(ItemStack base, List<String> extraLore, boolean glow) {
        ItemStack stack = base.clone();
        stack.editMeta(meta -> {
            List<Component> existing = meta.lore();
            List<Component> lore = existing == null ? new ArrayList<>() : new ArrayList<>(existing);
            if (!lore.isEmpty()) {
                lore.add(Component.empty());
            }
            lore.addAll(Text.lore(extraLore));
            meta.lore(lore);
            if (glow) {
                meta.setEnchantmentGlintOverride(true);
            }
        });
        return stack;
    }

    private ItemStack filler(String menu) {
        ItemStack stack = new ItemStack(plugin.getGuiConfig().fillerMaterial(menu));
        List<String> lore = plugin.getGuiConfig().itemLore(menu, "filler", List.of());
        stack.editMeta(meta -> {
            meta.displayName(Text.name(plugin.getGuiConfig().fillerName(menu)));
            if (!lore.isEmpty()) meta.lore(Text.lore(lore));
        });
        applyHeadData(stack, plugin.getGuiConfig().headOwner(menu, "filler"),
                plugin.getGuiConfig().headTexture(menu, "filler"));
        return stack;
    }

    private Material guiMaterial(String menu, String item, Material fallback) {
        return plugin.getGuiConfig().material(menu, item, fallback);
    }

    private ItemStack customItem(String menu, String key, Material material, String name,
                                 List<String> lore, String... placeholders) {
        Material configured = guiMaterial(menu, key, material);
        String configuredName = plugin.getGuiConfig().itemName(menu, key, name, placeholders);
        List<String> configuredLore = plugin.getGuiConfig().itemLore(menu, key, lore, placeholders);
        ItemStack stack = simpleItem(configured, configuredName, configuredLore);
        applyHeadData(stack, plugin.getGuiConfig().headOwner(menu, key),
                plugin.getGuiConfig().headTexture(menu, key));
        return stack;
    }

    private void applyHeadData(ItemStack stack, String owner, String texture) {
        if (stack.getType() != Material.PLAYER_HEAD || !(stack.getItemMeta() instanceof SkullMeta skull)) {
            return;
        }
        if (owner != null && !owner.isBlank()) {
            skull.setOwningPlayer(Bukkit.getOfflinePlayer(owner));
        }
        String textureUrl = textureUrl(texture);
        if (textureUrl != null) {
            try {
                PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID());
                PlayerTextures textures = profile.getTextures();
                textures.setSkin(new URL(textureUrl));
                profile.setTextures(textures);
                skull.setOwnerProfile(profile);
            } catch (MalformedURLException ignored) {
                // An invalid texture is ignored and the normal player head is shown.
            }
        }
        stack.setItemMeta(skull);
    }

    private String textureUrl(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        String value = input.trim();
        if (!value.startsWith("http")) {
            try {
                value = new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8)
                        .replace("\\/", "/");
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
        Matcher matcher = TEXTURE_URL.matcher(value);
        return matcher.find() ? matcher.group() : null;
    }

    private String msg(String key, String... placeholders) {
        return plugin.getMessages().format(key, placeholders);
    }

    public int[] forgeSlots() {
        return plugin.getGuiConfig().slots("main", "forge", FORGE_SLOTS);
    }

    public int[] progressSlots() {
        return plugin.getGuiConfig().slots("main", "progress", PROGRESS_SLOTS);
    }

    public int[] categorySlots() {
        return plugin.getGuiConfig().slots("categories", "categories", CATEGORY_SLOTS);
    }

    public int[] recipeSlots() {
        return plugin.getGuiConfig().slots("recipes", "recipes", RECIPE_SLOTS);
    }

    public int mainForgePosition(int rawSlot) {
        return position(rawSlot, forgeSlots());
    }

    public int categoryPosition(int rawSlot) {
        return position(rawSlot, categorySlots());
    }

    public int recipePosition(int rawSlot) {
        return position(rawSlot, recipeSlots());
    }

    private int position(int rawSlot, int[] slots) {
        for (int index = 0; index < slots.length; index++) {
            if (slots[index] == rawSlot) return index;
        }
        return -1;
    }

    public static int categoryPageSize() {
        return CATEGORY_PAGE_SIZE;
    }

    public static int recipePageSize() {
        return RECIPE_PAGE_SIZE;
    }
}

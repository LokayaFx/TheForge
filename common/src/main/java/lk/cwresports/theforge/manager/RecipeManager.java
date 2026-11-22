package lk.cwresports.theforge.manager;

import lk.cwresports.theforge.TheForgePlugin;
import lk.cwresports.theforge.hook.EcoItemsHook;
import lk.cwresports.theforge.hook.MMOItemsHook;
import lk.cwresports.theforge.model.ForgeCategory;
import lk.cwresports.theforge.model.ForgeRecipe;
import lk.cwresports.theforge.util.Log;
import lk.cwresports.theforge.util.Text;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class RecipeManager {

    private static final String ROOT = "recipe_categories";

    private final TheForgePlugin plugin;
    private final File categoriesFile;
    private final Map<String, ForgeCategory> categories = new LinkedHashMap<>();
    private final Map<String, ForgeRecipe> recipes = new LinkedHashMap<>();

    public RecipeManager(TheForgePlugin plugin) {
        this.plugin = plugin;
        this.categoriesFile = new File(plugin.getDataFolder(), "categories.yml");
        if (!categoriesFile.exists()) {
            plugin.saveResource("categories.yml", false);
        }
        reload();
    }

    public void reload() {
        load();
    }

    private void load() {
        categories.clear();
        recipes.clear();

        YamlConfiguration config = YamlConfiguration.loadConfiguration(categoriesFile);
        ConfigurationSection root = config.getConfigurationSection(ROOT);
        if (root == null) {
            // Backwards compatibility with the old categories.yml layout.
            root = config.getConfigurationSection("categories");
        }
        if (root == null) {
            Log.warn("No 'recipe_categories' section found in categories.yml; using defaults.");
            addDefaultCategories();
            logSummary();
            return;
        }

        int order = 0;
        for (String key : root.getKeys(false)) {
            ConfigurationSection categorySection = root.getConfigurationSection(key);
            if (categorySection == null) {
                continue;
            }
            String id = key.toLowerCase(Locale.ROOT);
            String displayName = categorySection.getString("display_name",
                    categorySection.getString("display-name", "&e" + Text.pretty(id)));

            Material icon = Material.CHEST;
            String iconName = null;
            ConfigurationSection iconSection = categorySection.getConfigurationSection("icon");
            if (iconSection != null) {
                Material parsed = Material.matchMaterial(iconSection.getString("material", "CHEST"));
                if (parsed != null && !parsed.isAir()) {
                    icon = parsed;
                }
                iconName = iconSection.getString("name");
            } else {
                // Old layout stored the icon as a plain material string.
                Material parsed = Material.matchMaterial(categorySection.getString("icon", "CHEST"));
                if (parsed != null && !parsed.isAir()) {
                    icon = parsed;
                }
            }

            int categoryOrder = categorySection.contains("order")
                    ? categorySection.getInt("order", order)
                    : order;
            int requiredLevel = Math.max(1, categorySection.getInt("required-level", 1));
            categories.put(id, new ForgeCategory(id, displayName, iconName, icon, categoryOrder, requiredLevel));
            order++;

            ConfigurationSection recipesSection = categorySection.getConfigurationSection("recipes");
            if (recipesSection == null) {
                continue;
            }
            for (String recipeKey : recipesSection.getKeys(false)) {
                String recipeId = recipeKey.toLowerCase(Locale.ROOT);
                if (recipes.containsKey(recipeId)) {
                    Log.warn("Skipping duplicate recipe id: " + recipeKey);
                    continue;
                }
                ForgeRecipe recipe = ForgeRecipe.fromSection(recipeId, id,
                        recipesSection.getConfigurationSection(recipeKey));
                if (recipe == null) {
                    Log.warn("Skipping invalid recipe: " + key + "." + recipeKey);
                    continue;
                }
                recipes.put(recipeId, recipe);
            }
        }

        if (categories.isEmpty()) {
            Log.warn("No categories loaded from categories.yml; using defaults.");
            addDefaultCategories();
        }
        logSummary();
    }

    private void logSummary() {
        int mmoRecipes = 0;
        int ecoRecipes = 0;
        for (ForgeRecipe recipe : recipes.values()) {
            if (recipe.hasMMOItems()) {
                mmoRecipes++;
            }
            if (recipe.hasEcoItems()) {
                ecoRecipes++;
            }
        }

        Log.info("Loaded " + categories.size() + " categories and " + recipes.size()
                + " recipes from categories.yml.");
        for (ForgeCategory category : getCategories()) {
            Log.info("  - " + category.getId() + ": " + countRecipes(category.getId())
                    + " recipe(s), icon " + category.getIcon().name());
        }
        if (mmoRecipes > 0) {
            Log.info("  MMOItems recipes: " + mmoRecipes
                    + (MMOItemsHook.isAvailable()
                    ? " (enabled)" : " (MMOItems not installed - unavailable)"));
        }
        if (ecoRecipes > 0) {
            Log.info("  EcoItems recipes: " + ecoRecipes
                    + (EcoItemsHook.isAvailable()
                    ? " (enabled)" : " (EcoItems not installed - unavailable)"));
        }
    }

    private void addDefaultCategories() {
        categories.put("parts", new ForgeCategory("parts", "&eParts", null, Material.IRON_INGOT, 0, 1));
        categories.put("ores", new ForgeCategory("ores", "&bOres", null, Material.RAW_IRON, 1, 2));
        categories.put("armor", new ForgeCategory("armor", "&9Armor", null, Material.IRON_CHESTPLATE, 2, 4));
    }

    public List<ForgeCategory> getCategories() {
        List<ForgeCategory> result = new ArrayList<>(categories.values());
        result.sort(Comparator.comparingInt(ForgeCategory::getOrder)
                .thenComparing(ForgeCategory::getId));
        return result;
    }

    public ForgeCategory getCategory(String id) {
        return id == null ? null : categories.get(id.toLowerCase(Locale.ROOT));
    }

    public List<ForgeRecipe> getRecipes(String categoryId) {
        List<ForgeRecipe> result = new ArrayList<>();
        for (ForgeRecipe recipe : recipes.values()) {
            if (recipe.getCategoryId().equalsIgnoreCase(categoryId)) {
                result.add(recipe);
            }
        }
        result.sort(Comparator.comparing(ForgeRecipe::displayName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(ForgeRecipe::getId));
        return result;
    }

    public ForgeRecipe getRecipe(String id) {
        return id == null ? null : recipes.get(id.toLowerCase(Locale.ROOT));
    }

    public boolean recipeExists(String id) {
        return getRecipe(id) != null;
    }

    public int recipeCount() {
        return recipes.size();
    }

    /** Recipes the player can use at the given forge level. */
    public int unlockedRecipeCount(int level) {
        int count = 0;
        for (ForgeRecipe recipe : recipes.values()) {
            ForgeCategory category = categories.get(recipe.getCategoryId());
            if (category != null && level < category.getRequiredLevel()) {
                continue;
            }
            if (level >= recipe.getRequiredLevel()) {
                count++;
            }
        }
        return count;
    }

    public String generateRecipeId(String categoryId, String baseName) {
        String base = sanitize(categoryId + "_" + baseName);
        String candidate = base;
        int counter = 2;
        while (recipes.containsKey(candidate)) {
            candidate = base + "_" + counter++;
        }
        return candidate;
    }

    public int nextCategoryOrder() {
        return categories.values().stream()
                .mapToInt(ForgeCategory::getOrder)
                .max()
                .orElse(-1) + 1;
    }

    public void addRecipe(ForgeRecipe recipe) {
        recipes.put(recipe.getId(), recipe);
        save();
    }

    public boolean removeRecipe(String id) {
        if (recipes.remove(id) == null) {
            return false;
        }
        save();
        return true;
    }

    public void addCategory(ForgeCategory category) {
        categories.put(category.getId(), category);
        save();
    }

    public int removeCategory(String id) {
        int removed = 0;
        List<String> toRemove = new ArrayList<>();
        for (ForgeRecipe recipe : recipes.values()) {
            if (recipe.getCategoryId().equalsIgnoreCase(id)) {
                toRemove.add(recipe.getId());
            }
        }
        for (String recipeId : toRemove) {
            if (recipes.remove(recipeId) != null) {
                removed++;
            }
        }
        categories.remove(id);
        save();
        return removed;
    }

    public int countRecipes(String categoryId) {
        int count = 0;
        for (ForgeRecipe recipe : recipes.values()) {
            if (recipe.getCategoryId().equalsIgnoreCase(categoryId)) {
                count++;
            }
        }
        return count;
    }

    public static String sanitize(String input) {
        String value = input == null ? "" : input.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        return value.isEmpty() ? "recipe" : value;
    }

    private void save() {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(categoriesFile);
        config.set(ROOT, null);
        ConfigurationSection root = config.createSection(ROOT);

        for (ForgeCategory category : getCategories()) {
            ConfigurationSection categorySection = root.createSection(category.getId());
            categorySection.set("display_name", category.getDisplayName());
            categorySection.set("required-level", category.getRequiredLevel());

            ConfigurationSection iconSection = categorySection.createSection("icon");
            iconSection.set("material", category.getIcon() == null
                    ? Material.CHEST.name() : category.getIcon().name());
            if (category.getIconName() != null && !category.getIconName().isBlank()) {
                iconSection.set("name", category.getIconName());
            }

            List<ForgeRecipe> categoryRecipes = getRecipes(category.getId());
            if (!categoryRecipes.isEmpty()) {
                ConfigurationSection recipesSection = categorySection.createSection("recipes");
                for (ForgeRecipe recipe : categoryRecipes) {
                    recipe.save(recipesSection.createSection(recipe.getId()));
                }
            }
        }

        try {
            config.save(categoriesFile);
            Log.admin("Saved categories.yml (" + categories.size() + " categories, "
                    + recipes.size() + " recipes).");
        } catch (IOException exception) {
            Log.error("Could not save categories.yml.", exception);
        }
    }
}

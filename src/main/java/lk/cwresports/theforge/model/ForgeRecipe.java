package lk.cwresports.theforge.model;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class ForgeRecipe {

    private final String id;
    private final String categoryId;
    private final ItemSpec result;
    private final int resultAmount;
    private final int baseSeconds;
    private final int experience;
    private final int requiredLevel;
    private final List<Ingredient> ingredients;

    public ForgeRecipe(String id, String categoryId, ItemSpec result, int resultAmount,
                       int baseSeconds, int experience, int requiredLevel, List<Ingredient> ingredients) {
        this.id = id;
        this.categoryId = categoryId;
        this.result = result;
        this.resultAmount = Math.max(1, resultAmount);
        this.baseSeconds = Math.max(1, baseSeconds);
        this.experience = Math.max(1, experience);
        this.requiredLevel = Math.max(1, requiredLevel);
        this.ingredients = List.copyOf(ingredients);
    }

    public String getId() {
        return id;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public ItemSpec getResult() {
        return result;
    }

    public int getResultAmount() {
        return resultAmount;
    }

    public int getBaseSeconds() {
        return baseSeconds;
    }

    public int getExperience() { return experience; }

    public int getRequiredLevel() { return requiredLevel; }

    public List<Ingredient> getIngredients() {
        return Collections.unmodifiableList(ingredients);
    }

    public String displayName() {
        return result.displayName();
    }

    public ItemStack createResult() {
        return result.create(resultAmount);
    }

    public boolean hasMMOItems() {
        if (result.isMMOItems()) {
            return true;
        }
        for (Ingredient ingredient : ingredients) {
            if (ingredient.getItem().isMMOItems()) {
                return true;
            }
        }
        return false;
    }

    public boolean hasEcoItems() {
        if (result.isEcoItems()) {
            return true;
        }
        for (Ingredient ingredient : ingredients) {
            if (ingredient.getItem().isEcoItems()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Writes the recipe in the categories.yml format. The category is implied
     * by the section the recipe is saved under, so it is not written again.
     */
    public void save(ConfigurationSection section) {
        ConfigurationSection resultSection = section.createSection("result");
        result.saveItem(resultSection);
        resultSection.set("amount", resultAmount);

        section.set("time_in_seconds", baseSeconds);
        section.set("experience", experience);
        section.set("required-level", requiredLevel);

        ConfigurationSection ingredientsSection = section.createSection("ingredients");
        Set<String> usedKeys = new HashSet<>();
        for (Ingredient ingredient : ingredients) {
            String baseKey = ingredient.key();
            String key = baseKey;
            int counter = 2;
            while (!usedKeys.add(key)) {
                key = baseKey + "_" + counter++;
            }
            ingredient.saveItem(ingredientsSection.createSection(key));
        }
    }

    public static ForgeRecipe fromSection(String id, String categoryId, ConfigurationSection section) {
        if (section == null || categoryId == null || categoryId.isBlank()) {
            return null;
        }
        ConfigurationSection resultSection = section.getConfigurationSection("result");
        if (resultSection == null) {
            return null;
        }
        ItemSpec result = ItemSpec.fromItemSection(resultSection);
        if (result == null) {
            return null;
        }
        int resultAmount = Math.max(1, resultSection.getInt("amount", 1));
        int time = Math.max(1, section.getInt("time_in_seconds", section.getInt("time", 60)));
        int experience = Math.max(1, section.getInt("experience", time));
        int requiredLevel = Math.max(1, section.getInt("required-level", 1));

        List<Ingredient> ingredients = new ArrayList<>();
        ConfigurationSection ingredientsSection = section.getConfigurationSection("ingredients");
        if (ingredientsSection != null) {
            for (String key : ingredientsSection.getKeys(false)) {
                Ingredient ingredient = Ingredient.fromItemSection(
                        ingredientsSection.getConfigurationSection(key));
                if (ingredient != null) {
                    ingredients.add(ingredient);
                }
            }
        }
        if (ingredients.isEmpty()) {
            return null;
        }
        return new ForgeRecipe(id, categoryId.toLowerCase(Locale.ROOT), result, resultAmount,
                time, experience, requiredLevel, ingredients);
    }

    /**
     * Legacy entry point kept for compatibility. New code should use
     * {@link #fromSection(String, String, ConfigurationSection)}.
     */
    public static ForgeRecipe fromSection(String id, ConfigurationSection section) {
        if (section == null) {
            return null;
        }
        String category = section.getString("category");
        return fromSection(id, category, section);
    }
}

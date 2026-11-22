package lk.cwresports.theforge.command;

import lk.cwresports.theforge.TheForgePlugin;
import lk.cwresports.theforge.hook.EcoItemsHook;
import lk.cwresports.theforge.hook.MMOItemsHook;
import lk.cwresports.theforge.model.ForgeCategory;
import lk.cwresports.theforge.model.ForgeRecipe;
import lk.cwresports.theforge.model.Ingredient;
import lk.cwresports.theforge.model.ItemSpec;
import lk.cwresports.theforge.model.PlayerData;
import lk.cwresports.theforge.util.Log;
import lk.cwresports.theforge.util.Text;
import lk.cwresports.theforge.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class ForgeCommand implements CommandExecutor, TabCompleter {

    private final TheForgePlugin plugin;

    public ForgeCommand(TheForgePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                plugin.getMessages().send(sender, "players-only");
                return true;
            }
            if (!has(sender, "theforge.use")) {
                return true;
            }
            plugin.getGui().openMain(player);
            Log.debug(player.getName() + " opened the forge GUI.");
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "help" -> plugin.getMessages().sendList(sender, "help");
            case "reload" -> reload(sender);
            case "settime" -> setTime(sender, args);
            case "setlevel" -> setLevel(sender, args);
            case "setxp" -> setXp(sender, args, false);
            case "addxp" -> setXp(sender, args, true);
            case "recipe" -> recipe(sender, args);
            case "category" -> category(sender, args);
            default -> plugin.getMessages().send(sender, "unknown-command");
        }
        return true;
    }

    private void reload(CommandSender sender) {
        if (!has(sender, "theforge.admin")) {
            return;
        }
        plugin.reloadAll();
        Log.admin(sender.getName() + " reloaded TheForge configuration.");
        plugin.getMessages().send(sender, "reload-success");
    }

    private void setTime(CommandSender sender, String[] args) {
        if (!has(sender, "theforge.admin")) {
            return;
        }
        if (args.length < 3) {
            plugin.getMessages().sendRaw(sender, "recipe-usage");
            plugin.getMessages().sendRaw(sender, "time-format-example");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            plugin.getMessages().send(sender, "player-not-found", "player", args[1]);
            return;
        }
        int seconds;
        try {
            seconds = TimeUtil.parseSeconds(args[2]);
        } catch (IllegalArgumentException exception) {
            plugin.getMessages().send(sender, "invalid-time", "value", args[2]);
            return;
        }
        PlayerData data = plugin.getPlayers().get(target.getUniqueId());
        data.setTotalSeconds(seconds);
        // Keep the legacy time-based command aligned with the XP progression.
        data.setXp(seconds);
        plugin.getPlayers().save(data);
        String time = TimeUtil.format(seconds);
        Log.admin(sender.getName() + " set " + target.getName() + "'s total forging time to "
                + time + " (" + seconds + "s).");
        plugin.getMessages().send(sender, "settime-done", "player", target.getName(), "time", time);
        plugin.getMessages().send(target, "settime-target", "time", time);
    }

    private void setLevel(CommandSender sender, String[] args) {
        if (!has(sender, "theforge.admin")) {
            return;
        }
        if (args.length < 3) {
            plugin.getMessages().sendRaw(sender, "&cUsage: &e/forge setlevel <player> <1-"
                    + plugin.getLevels().maxLevel() + ">");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            plugin.getMessages().send(sender, "player-not-found", "player", args[1]);
            return;
        }
        int level;
        try {
            level = Integer.parseInt(args[2]);
        } catch (NumberFormatException exception) {
            plugin.getMessages().send(sender, "invalid-number", "value", args[2]);
            return;
        }
        if (level < 1 || level > plugin.getLevels().maxLevel()) {
            plugin.getMessages().send(sender, "invalid-number", "value", args[2]);
            return;
        }
        PlayerData data = plugin.getPlayers().get(target.getUniqueId());
        data.setXp(plugin.getLevels().requirementFor(level));
        plugin.getPlayers().save(data);
        Log.admin(sender.getName() + " set " + target.getName() + "'s forge level to " + level + ".");
        plugin.getMessages().send(sender, "setlevel-done",
                "player", target.getName(), "level", String.valueOf(level));
        plugin.getMessages().send(target, "setlevel-target", "level", String.valueOf(level));
    }

    private void setXp(CommandSender sender, String[] args, boolean add) {
        if (!has(sender, "theforge.admin")) {
            return;
        }
        if (args.length < 3) {
            plugin.getMessages().sendRaw(sender, "&cUsage: &e/forge " + (add ? "addxp" : "setxp")
                    + " <player> <amount>");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            plugin.getMessages().send(sender, "player-not-found", "player", args[1]);
            return;
        }
        int amount;
        try {
            amount = Integer.parseInt(args[2]);
        } catch (NumberFormatException exception) {
            plugin.getMessages().send(sender, "invalid-number", "value", args[2]);
            return;
        }
        PlayerData data = plugin.getPlayers().get(target.getUniqueId());
        if (add) {
            data.addXp(amount);
        } else {
            data.setXp(amount);
        }
        plugin.getPlayers().save(data);
        int after = plugin.getForge().levelOf(data);
        Log.admin(sender.getName() + (add ? " added " : " set ") + target.getName()
                + "'s Forge XP to " + data.getXp() + " (level " + after + ").");
        plugin.getMessages().send(sender, add ? "addxp-done" : "setxp-done",
                "player", target.getName(),
                "xp", String.valueOf(data.getXp()),
                "level", String.valueOf(after));
        plugin.getMessages().send(target, "setxp-target",
                "xp", String.valueOf(data.getXp()),
                "level", String.valueOf(after));
    }

    private void recipe(CommandSender sender, String[] args) {
        if (!has(sender, "theforge.admin")) {
            return;
        }
        if (args.length < 2) {
            plugin.getMessages().sendRaw(sender, "&cUsage: &e/forge recipe <add|remove|list>");
            return;
        }
        String action = args[1].toLowerCase(Locale.ROOT);
        switch (action) {
            case "add" -> recipeAdd(sender, args);
            case "remove" -> recipeRemove(sender, args);
            case "list" -> recipeList(sender, args);
            default -> plugin.getMessages().send(sender, "unknown-command");
        }
    }

    private void recipeAdd(CommandSender sender, String[] args) {
        if (args.length < 6) {
            plugin.getMessages().sendRaw(sender, "recipe-usage");
            plugin.getMessages().sendRaw(sender, "time-format-example");
            return;
        }
        ForgeCategory category = plugin.getRecipes().getCategory(args[2]);
        if (category == null) {
            plugin.getMessages().send(sender, "category-not-found", "category", args[2]);
            return;
        }

        ItemSpec result;
        String itemArg = args[3];
        if (itemArg.equalsIgnoreCase("hand")) {
            if (!(sender instanceof Player player)) {
                plugin.getMessages().send(sender, "players-only");
                return;
            }
            ItemStack held = player.getInventory().getItemInMainHand();
            if (held == null || held.getType().isAir()) {
                plugin.getMessages().send(sender, "hand-empty");
                return;
            }
            String mmoType = MMOItemsHook.getType(held);
            String mmoId = MMOItemsHook.getId(held);
            result = mmoType != null && mmoId != null
                    ? ItemSpec.mmoItems(mmoType, mmoId)
                    : ItemSpec.custom(held);
        } else if (isEcoArgument(itemArg)) {
            if (!EcoItemsHook.isAvailable()) {
                plugin.getMessages().send(sender, "eco-missing");
                return;
            }
            String[] parts = itemArg.split(":", 2);
            if (parts.length != 2) {
                plugin.getMessages().send(sender, "unknown-material", "material", itemArg);
                return;
            }
            result = ItemSpec.ecoItems(parts[1]);
            if (result == null || result.create(1) == null) {
                plugin.getMessages().send(sender, "eco-not-found", "id", parts[1]);
                return;
            }
        } else if (isMMOArgument(itemArg)) {
            if (!MMOItemsHook.isAvailable()) {
                plugin.getMessages().send(sender, "mmo-missing");
                return;
            }
            String[] parts = itemArg.split(":");
            if (parts.length == 2) {
                result = ItemSpec.mmoItemsAny(parts[1]);
            } else if (parts.length == 3) {
                result = ItemSpec.mmoItems(parts[1], parts[2]);
            } else {
                plugin.getMessages().send(sender, "unknown-material", "material", itemArg);
                return;
            }
            if (result == null || result.create(1) == null) {
                plugin.getMessages().send(sender, "mmo-not-found",
                        "type", parts.length == 3 ? parts[1] : "ANY",
                        "id", parts.length == 3 ? parts[2] : parts[1]);
                return;
            }
        } else {
            Material material = Material.matchMaterial(itemArg);
            result = ItemSpec.vanilla(material);
            if (result == null) {
                plugin.getMessages().send(sender, "unknown-material", "material", itemArg);
                return;
            }
        }

        int time;
        try {
            time = TimeUtil.parseSeconds(args[4]);
        } catch (IllegalArgumentException exception) {
            plugin.getMessages().send(sender, "invalid-time", "value", args[4]);
            return;
        }

        List<Ingredient> ingredients = new ArrayList<>();
        String[] tokens = args[5].split(",");
        for (String token : tokens) {
            try {
                Ingredient ingredient = parseIngredient(token.trim());
                if (ingredient == null) {
                    throw new IllegalArgumentException("Invalid ingredient");
                }
                ingredients.add(ingredient);
            } catch (IllegalArgumentException exception) {
                plugin.getMessages().send(sender, "ingredient-format", "value", token.trim());
                return;
            }
        }
        if (ingredients.isEmpty()) {
            plugin.getMessages().send(sender, "recipe-usage");
            return;
        }

        String id = plugin.getRecipes().generateRecipeId(category.getId(), result.displayName());
        ForgeRecipe recipe = new ForgeRecipe(id, category.getId(), result, 1, time, time, 1, ingredients);
        plugin.getRecipes().addRecipe(recipe);
        Log.admin(sender.getName() + " added recipe " + id + " to category " + category.getId()
                + " (result " + result.displayName() + ", "
                + TimeUtil.format(time) + ", " + ingredients.size() + " ingredient(s)).");
        plugin.getMessages().send(sender, "recipe-added",
                "recipe", id, "category", category.getId());
    }

    private Ingredient parseIngredient(String token) {
        if (token.isEmpty()) {
            return null;
        }
        if (isEcoArgument(token)) {
            if (!EcoItemsHook.isAvailable()) {
                throw new IllegalArgumentException("EcoItems is missing");
            }
            String[] parts = token.split(":", 3);
            if (parts.length != 3) {
                return null;
            }
            int amount = parseAmount(parts[2]);
            ItemSpec spec = ItemSpec.ecoItems(parts[1]);
            if (spec == null || spec.create(1) == null) {
                throw new IllegalArgumentException("Unknown EcoItems item");
            }
            return new Ingredient(spec, amount);
        }
        if (isMMOArgument(token)) {
            if (!MMOItemsHook.isAvailable()) {
                throw new IllegalArgumentException("MMOItems is missing");
            }
            String[] parts = token.split(":");
            int amount;
            ItemSpec spec;
            if (parts.length == 3) {
                amount = parseAmount(parts[2]);
                spec = ItemSpec.mmoItemsAny(parts[1]);
            } else if (parts.length == 4) {
                amount = parseAmount(parts[3]);
                spec = ItemSpec.mmoItems(parts[1], parts[2]);
            } else {
                return null;
            }
            if (spec == null || spec.create(1) == null) {
                throw new IllegalArgumentException("Unknown MMOItems item");
            }
            return new Ingredient(spec, amount);
        }
        String[] parts = token.split(":");
        if (parts.length != 2) {
            return null;
        }
        Material material = Material.matchMaterial(parts[0]);
        if (material == null || material.isAir()) {
            return null;
        }
        int amount = parseAmount(parts[1]);
        return new Ingredient(ItemSpec.vanilla(material), amount);
    }

    private int parseAmount(String value) {
        try {
            int amount = Integer.parseInt(value);
            if (amount <= 0) {
                throw new IllegalArgumentException("Amount must be positive");
            }
            return amount;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid amount");
        }
    }

    private void recipeRemove(CommandSender sender, String[] args) {
        if (args.length < 4) {
            plugin.getMessages().sendRaw(sender, "recipe-remove-usage");
            return;
        }
        ForgeCategory category = plugin.getRecipes().getCategory(args[2]);
        if (category == null) {
            plugin.getMessages().send(sender, "category-not-found", "category", args[2]);
            return;
        }
        ForgeRecipe recipe = plugin.getRecipes().getRecipe(args[3]);
        if (recipe == null || !recipe.getCategoryId().equalsIgnoreCase(category.getId())) {
            plugin.getMessages().send(sender, "recipe-not-found", "recipe", args[3]);
            return;
        }
        plugin.getRecipes().removeRecipe(recipe.getId());
        Log.admin(sender.getName() + " removed recipe " + recipe.getId()
                + " from category " + category.getId() + ".");
        plugin.getMessages().send(sender, "recipe-removed", "recipe", recipe.getId());
    }

    private void recipeList(CommandSender sender, String[] args) {
        if (args.length < 3) {
            plugin.getMessages().send(sender, "category-list-header");
            for (ForgeCategory category : plugin.getRecipes().getCategories()) {
                plugin.getMessages().sendRaw(sender, "category-list-line",
                        "category", category.getId(),
                        "recipes", String.valueOf(plugin.getRecipes().countRecipes(category.getId())));
            }
            return;
        }
        ForgeCategory category = plugin.getRecipes().getCategory(args[2]);
        if (category == null) {
            plugin.getMessages().send(sender, "category-not-found", "category", args[2]);
            return;
        }
        List<ForgeRecipe> recipes = plugin.getRecipes().getRecipes(category.getId());
        plugin.getMessages().send(sender, "recipe-list-header", "category", category.getId());
        if (recipes.isEmpty()) {
            plugin.getMessages().sendRaw(sender, "recipe-list-empty");
            return;
        }
        for (ForgeRecipe recipe : recipes) {
            plugin.getMessages().sendRaw(sender, "recipe-list-line",
                    "recipe", recipe.getId(),
                    "item", recipe.displayName(),
                    "time", TimeUtil.format(recipe.getBaseSeconds()),
                    "xp", String.valueOf(recipe.getExperience()),
                    "level", String.valueOf(recipe.getRequiredLevel()));
        }
    }

    private void category(CommandSender sender, String[] args) {
        if (!has(sender, "theforge.admin")) {
            return;
        }
        if (args.length < 2) {
            plugin.getMessages().sendRaw(sender, "&cUsage: &e/forge category <add|remove|list>");
            return;
        }
        String action = args[1].toLowerCase(Locale.ROOT);
        switch (action) {
            case "add" -> categoryAdd(sender, args);
            case "remove" -> categoryRemove(sender, args);
            case "list" -> categoryList(sender);
            default -> plugin.getMessages().send(sender, "unknown-command");
        }
    }

    private void categoryAdd(CommandSender sender, String[] args) {
        if (args.length < 3) {
            plugin.getMessages().sendRaw(sender, "category-usage");
            return;
        }
        String id = lk.cwresports.theforge.manager.RecipeManager.sanitize(args[2]);
        if (plugin.getRecipes().getCategory(id) != null) {
            plugin.getMessages().send(sender, "category-exists", "category", id);
            return;
        }
        String display = args.length >= 4 ? args[3].replace('_', ' ') : "&e" + Text.pretty(id);
        Material icon = Material.CHEST;
        if (args.length >= 5) {
            icon = Material.matchMaterial(args[4]);
            if (icon == null || icon.isAir()) {
                plugin.getMessages().send(sender, "unknown-material", "material", args[4]);
                return;
            }
        }
        int order = plugin.getRecipes().nextCategoryOrder();
        plugin.getRecipes().addCategory(new ForgeCategory(id, display, null, icon, order, 1));
        Log.admin(sender.getName() + " added category " + id + " (icon " + icon.name()
                + ", display " + Text.strip(display) + ").");
        plugin.getMessages().send(sender, "category-added", "category", id);
    }

    private void categoryRemove(CommandSender sender, String[] args) {
        if (args.length < 3) {
            plugin.getMessages().sendRaw(sender, "category-remove-usage");
            return;
        }
        ForgeCategory category = plugin.getRecipes().getCategory(args[2]);
        if (category == null) {
            plugin.getMessages().send(sender, "category-not-found", "category", args[2]);
            return;
        }
        int removed = plugin.getRecipes().removeCategory(category.getId());
        Log.admin(sender.getName() + " removed category " + category.getId()
                + " and " + removed + " recipe(s).");
        plugin.getMessages().send(sender, "category-removed",
                "category", category.getId(),
                "recipes", String.valueOf(removed));
    }

    private void categoryList(CommandSender sender) {
        plugin.getMessages().send(sender, "category-list-header");
        for (ForgeCategory category : plugin.getRecipes().getCategories()) {
            plugin.getMessages().sendRaw(sender, "category-list-line",
                    "category", category.getId(),
                    "recipes", String.valueOf(plugin.getRecipes().countRecipes(category.getId())));
        }
    }

    private boolean has(CommandSender sender, String permission) {
        if (!sender.hasPermission(permission)) {
            plugin.getMessages().send(sender, "no-permission");
            return false;
        }
        return true;
    }

    private boolean isMMOArgument(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        return lower.startsWith("mmoitems:") || lower.startsWith("ia:");
    }

    private boolean isEcoArgument(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        return lower.startsWith("ecoitems:") || lower.startsWith("eco:");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(List.of("help", "reload", "settime", "setlevel", "setxp", "addxp",
                    "recipe", "category"), args[0]);
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("settime")) {
            if (args.length == 2) {
                return filter(playerNames(), args[1]);
            }
            if (args.length == 3) {
                return filter(List.of("600", "1800", "3600", "7200", "1h", "2h"), args[2]);
            }
        }
        if (sub.equals("setlevel")) {
            if (args.length == 2) {
                return filter(playerNames(), args[1]);
            }
            if (args.length == 3) {
                List<String> levels = new ArrayList<>();
                for (int level = 1; level <= plugin.getLevels().maxLevel(); level++) {
                    levels.add(String.valueOf(level));
                }
                return filter(levels, args[2]);
            }
        }
        if (sub.equals("setxp") || sub.equals("addxp")) {
            if (args.length == 2) {
                return filter(playerNames(), args[1]);
            }
            if (args.length == 3) {
                return filter(List.of("100", "500", "1000", "5000"), args[2]);
            }
        }
        if (sub.equals("recipe")) {
            if (args.length == 2) {
                return filter(List.of("add", "remove", "list"), args[1]);
            }
            if (args.length == 3) {
                return filter(categoryIds(), args[2]);
            }
            String action = args[1].toLowerCase(Locale.ROOT);
            if (action.equals("remove") && args.length == 4) {
                return filter(recipeIds(args[2]), args[3]);
            }
            if (action.equals("list") && args.length == 4) {
                return filter(categoryIds(), args[3]);
            }
            if (action.equals("add")) {
                if (args.length == 4) {
                    return filter(List.of("hand", "IRON_PICKAXE", "DIAMOND_SWORD",
                            "GOLD_INGOT", "IRON_CHESTPLATE",
                            "ecoitems:item_id", "mmoitems:SWORD:CUTLASS"), args[3]);
                }
                if (args.length == 5) {
                    return filter(List.of("30s", "10m", "1h", "1h30m", "2h"), args[4]);
                }
                if (args.length == 6) {
                    return filter(List.of("IRON_INGOT:4,COAL:2", "RAW_IRON:2,COAL:4",
                            "DIAMOND:2,STICK:1", "ecoitems:item_id:4,COAL:2",
                            "mmoitems:SWORD:CUTLASS:2"), args[5]);
                }
            }
        }
        if (sub.equals("category")) {
            if (args.length == 2) {
                return filter(List.of("add", "remove", "list"), args[1]);
            }
            String action = args[1].toLowerCase(Locale.ROOT);
            if (action.equals("remove") && args.length == 3) {
                return filter(categoryIds(), args[2]);
            }
            if (action.equals("add") && args.length == 5) {
                return filter(List.of("CHEST", "IRON_INGOT", "RAW_IRON", "IRON_CHESTPLATE",
                        "NETHER_STAR", "DIAMOND", "FURNACE", "ANVIL"), args[4]);
            }
        }
        return List.of();
    }

    private List<String> categoryIds() {
        List<String> ids = new ArrayList<>();
        for (ForgeCategory category : plugin.getRecipes().getCategories()) {
            ids.add(category.getId());
        }
        return ids;
    }

    private List<String> recipeIds(String categoryId) {
        List<String> ids = new ArrayList<>();
        for (ForgeRecipe recipe : plugin.getRecipes().getRecipes(categoryId)) {
            ids.add(recipe.getId());
        }
        return ids;
    }

    private List<String> playerNames() {
        List<String> names = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            names.add(player.getName());
        }
        return names;
    }

    private List<String> filter(Collection<String> values, String prefix) {
        String lower = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String value : values) {
            if (value.toLowerCase(Locale.ROOT).startsWith(lower)) {
                result.add(value);
            }
        }
        Collections.sort(result);
        return result;
    }
}

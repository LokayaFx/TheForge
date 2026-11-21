package lk.cwresports.theforge;

import lk.cwresports.theforge.command.ForgeCommand;
import lk.cwresports.theforge.config.MessageManager;
import lk.cwresports.theforge.config.GuiConfig;
import lk.cwresports.theforge.gui.ForgeGui;
import lk.cwresports.theforge.gui.GuiListener;
import lk.cwresports.theforge.hook.AureliumSkillsHook;
import lk.cwresports.theforge.hook.EcoItemsHook;
import lk.cwresports.theforge.hook.EcoSkillsHook;
import lk.cwresports.theforge.hook.MMOItemsHook;
import lk.cwresports.theforge.level.LevelManager;
import lk.cwresports.theforge.level.SlotManager;
import lk.cwresports.theforge.listener.PlayerListener;
import lk.cwresports.theforge.manager.ForgeManager;
import lk.cwresports.theforge.manager.PlayerDataManager;
import lk.cwresports.theforge.manager.RecipeManager;
import lk.cwresports.theforge.util.Log;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class TheForgePlugin extends JavaPlugin {

    private static TheForgePlugin instance;

    private MessageManager messages;
    private GuiConfig guiConfig;
    private LevelManager levels;
    private SlotManager slots;
    private RecipeManager recipes;
    private PlayerDataManager players;
    private ForgeManager forge;
    private ForgeGui gui;

    public static TheForgePlugin get() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        long started = System.currentTimeMillis();

        Log.info("========================================");
        Log.info("Starting TheForge v" + getDescription().getVersion());
        Log.info("Server: " + Bukkit.getName() + " " + Bukkit.getVersion());
        Log.info("Java: " + System.getProperty("java.version"));
        Log.info("========================================");

        saveDefaultConfig();
        upgradeDefaultProgression();
        saveIfMissing("messages.yml");
        saveIfMissing("categories.yml");
        saveIfMissing("slots.yml");

        Log.info("Data folder: " + getDataFolder().getAbsolutePath());
        Log.info("Loading optional integrations...");
        MMOItemsHook.init();
        EcoItemsHook.init();
        AureliumSkillsHook.init();
        EcoSkillsHook.init();

        Log.info("Loading configuration and forge data...");
        messages = new MessageManager(this);
        guiConfig = new GuiConfig(this);
        levels = new LevelManager(this);
        slots = new SlotManager(this);
        recipes = new RecipeManager(this);
        players = new PlayerDataManager(this);
        forge = new ForgeManager(this);
        gui = new ForgeGui(this);
        Log.info("Core systems ready (" + recipes.recipeCount() + " recipes, up to level "
                + levels.maxLevel() + " and " + levels.maxSlots() + " slots).");

        PluginCommand command = getCommand("forge");
        if (command != null) {
            ForgeCommand executor = new ForgeCommand(this);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
            Log.debug("Registered /forge command (alias /tf).");
        } else {
            Log.error("Command 'forge' is missing from plugin.yml!", null);
        }

        Bukkit.getPluginManager().registerEvents(new GuiListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PlayerListener(this), this);
        Log.info("Registered forge commands and player/GUI listeners.");

        int interval = Math.max(1, getConfig().getInt("forge.check-interval-ticks", 20));
        Bukkit.getScheduler().runTaskTimer(this, () -> forge.tick(), interval, interval);

        Log.info("Configuration: max-level=" + levels.maxLevel()
                + ", max-slots=" + levels.maxSlots()
                + ", check-interval=" + interval + " ticks"
                + ", leveling=Forge XP.");
        Log.info("Logging: gameplay=" + getConfig().getBoolean("logging.gameplay", true)
                + ", admin=" + getConfig().getBoolean("logging.admin", true)
                + ", debug=" + getConfig().getBoolean("logging.debug", false) + ".");
        Log.info("TheForge enabled in " + (System.currentTimeMillis() - started) + " ms.");
        Log.info("========================================");
    }

    @Override
    public void onDisable() {
        Log.info("Saving player data before shutdown...");
        if (players != null) {
            players.saveAll();
        }
        Bukkit.getScheduler().cancelTasks(this);
        Log.info("TheForge disabled.");
        instance = null;
    }

    public void reloadAll() {
        Log.admin("Reloading TheForge configuration...");
        reloadConfig();
        AureliumSkillsHook.init();
        EcoSkillsHook.init();
        messages = new MessageManager(this);
        guiConfig.reload();
        levels = new LevelManager(this);
        slots = new SlotManager(this);
        recipes = new RecipeManager(this);
        if (gui != null) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                gui.refresh(player);
            }
        }
    }

    private void saveIfMissing(String name) {
        File file = new File(getDataFolder(), name);
        if (!file.exists()) {
            saveResource(name, false);
        }
    }

    /** Adds the sixth and seventh default forge slots for existing installs. */
    private void upgradeDefaultProgression() {
        boolean changed = false;
        if (getConfig().getInt("max-slots", 5) == 5) {
            getConfig().set("max-slots", 7);
            changed = true;
        }
        if (!getConfig().contains("levels.6")) {
            getConfig().set("levels.6", 10800);
            changed = true;
        }
        if (!getConfig().contains("levels.7")) {
            getConfig().set("levels.7", 14400);
            changed = true;
        }
        if (!getConfig().contains("reductions.6")) {
            getConfig().set("reductions.6", 0.50D);
            changed = true;
        }
        if (!getConfig().contains("reductions.7")) {
            getConfig().set("reductions.7", 0.60D);
            changed = true;
        }
        if (changed) {
            saveConfig();
        }
    }

    public MessageManager getMessages() {
        return messages;
    }

    public GuiConfig getGuiConfig() {
        return guiConfig;
    }

    public LevelManager getLevels() {
        return levels;
    }

    public SlotManager getSlots() {
        return slots;
    }

    public RecipeManager getRecipes() {
        return recipes;
    }

    public PlayerDataManager getPlayers() {
        return players;
    }

    public ForgeManager getForge() {
        return forge;
    }

    public ForgeGui getGui() {
        return gui;
    }
}

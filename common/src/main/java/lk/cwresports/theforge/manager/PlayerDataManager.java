package lk.cwresports.theforge.manager;

import lk.cwresports.theforge.TheForgePlugin;
import lk.cwresports.theforge.model.PlayerData;
import lk.cwresports.theforge.util.Log;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PlayerDataManager {

    private final TheForgePlugin plugin;
    private final File dataFile;
    private final File legacyFolder;
    private final YamlConfiguration config;
    private final Map<UUID, PlayerData> cache = new HashMap<>();

    public PlayerDataManager(TheForgePlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "playerdata.yml");
        this.legacyFolder = new File(plugin.getDataFolder(), "playerdata");
        this.config = YamlConfiguration.loadConfiguration(dataFile);
        this.config.options().header("TheForge player data. Do not edit while the server is running.");
        this.config.options().copyHeader(true);
        if (!dataFile.exists()) {
            saveFile();
        }
        Log.info("Player data file: " + dataFile.getAbsolutePath());
    }

    public PlayerData get(UUID uuid) {
        PlayerData data = cache.get(uuid);
        if (data == null) {
            data = load(uuid);
            cache.put(uuid, data);
        }
        return data;
    }

    public PlayerData getIfLoaded(UUID uuid) {
        return cache.get(uuid);
    }

    public void save(PlayerData data) {
        String path = "players." + data.getUuid();
        config.set(path, null);
        data.save(config.createSection(path));
        if (saveFile()) {
            Log.debug("Saved data for " + data.getUuid() + " (total="
                    + data.getTotalSeconds() + "s, jobs=" + data.getJobs().size() + ").");
        }
    }

    public void unload(UUID uuid) {
        PlayerData data = cache.remove(uuid);
        if (data != null) {
            save(data);
            Log.debug("Unloaded data for " + uuid + ".");
        }
    }

    public void saveAll() {
        Log.debug("Saving " + cache.size() + " loaded player record(s)...");
        for (PlayerData data : new ArrayList<>(cache.values())) {
            save(data);
        }
    }

    private PlayerData load(UUID uuid) {
        PlayerData data = new PlayerData(uuid);
        org.bukkit.configuration.ConfigurationSection playerSection =
                config.getConfigurationSection("players." + uuid);
        if (playerSection != null) {
            data.load(playerSection);
            Log.debug("Loaded data for " + uuid + " (total=" + data.getTotalSeconds()
                    + "s, jobs=" + data.getJobs().size() + ").");
            return data;
        }

        File legacyFile = new File(legacyFolder, uuid + ".yml");
        if (!legacyFile.exists()) {
            Log.debug("No data file for " + uuid + "; starting fresh.");
            return data;
        }
        try {
            data.load(YamlConfiguration.loadConfiguration(legacyFile));
            save(data);
            Log.info("Migrated player data for " + uuid + " into playerdata.yml.");
        } catch (Exception exception) {
            Log.error("Could not read forge data for " + uuid + ".", exception);
            File backup = new File(legacyFolder, uuid + ".corrupt-" + System.currentTimeMillis() + ".yml");
            if (legacyFile.renameTo(backup)) {
                Log.warn("Moved the broken file to " + backup.getName());
            }
        }
        return data;
    }

    private boolean saveFile() {
        try {
            config.save(dataFile);
            return true;
        } catch (IOException exception) {
            Log.error("Could not save playerdata.yml.", exception);
            return false;
        }
    }
}

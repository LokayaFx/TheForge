package lk.cwresports.theforge.config;

import lk.cwresports.theforge.TheForgePlugin;
import lk.cwresports.theforge.util.Text;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class MessageManager {

    private final YamlConfiguration config;
    private final YamlConfiguration defaults;

    public MessageManager(TheForgePlugin plugin) {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        this.config = YamlConfiguration.loadConfiguration(file);
        this.defaults = loadDefaults(plugin);
    }

    private static YamlConfiguration loadDefaults(TheForgePlugin plugin) {
        InputStream stream = plugin.getResource("messages.yml");
        if (stream == null) {
            return new YamlConfiguration();
        }
        return YamlConfiguration.loadConfiguration(
                new InputStreamReader(stream, StandardCharsets.UTF_8));
    }

    public String get(String key) {
        String value = config.getString(key);
        return value != null ? value : defaults.getString(key, "");
    }

    public List<String> getList(String key) {
        List<String> values = config.getStringList(key);
        return values.isEmpty() ? defaults.getStringList(key) : values;
    }

    public String format(String key, String... placeholders) {
        return apply(get(key), placeholders);
    }

    public void send(CommandSender sender, String key, String... placeholders) {
        String message = get(key);
        if (message.isEmpty()) {
            return;
        }
        sender.sendMessage(Text.color(get("prefix") + apply(message, placeholders)));
    }

    public void sendRaw(CommandSender sender, String key, String... placeholders) {
        String message = get(key);
        if (!message.isEmpty()) {
            sender.sendMessage(Text.color(apply(message, placeholders)));
        }
    }

    public void sendList(CommandSender sender, String key, String... placeholders) {
        for (String line : getList(key)) {
            sender.sendMessage(Text.color(apply(line, placeholders)));
        }
    }

    private String apply(String message, String... placeholders) {
        String result = message;
        if (placeholders != null) {
            for (int index = 0; index + 1 < placeholders.length; index += 2) {
                String token = placeholders[index];
                String value = placeholders[index + 1];
                result = result.replace("{" + token + "}", value == null ? "" : value);
            }
        }
        return result;
    }
}

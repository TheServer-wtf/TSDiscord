package hu.Pdani.TSDiscord.utils;

import hu.Pdani.TSDiscord.TSDiscordPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class ImportantConfig {
    private static File configFile;
    private static FileConfiguration config;
    public static void loadConfig(){
        if(configFile != null && config != null)
            return;
        configFile = new File(TSDiscordPlugin.getPlugin().getDataFolder(),"donotmodify.yml");
        if(!configFile.exists()){
            TSDiscordPlugin.getPlugin().saveResource("donotmodify.yml",false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);
    }
    public static void reloadConfig(){
        try {
            saveConfig();
        } catch (IOException ignored) {}
        config = null;
        configFile = null;
        loadConfig();
    }
    public static void saveConfig() throws IOException {
        config.options().setHeader(List.of(
                "You should not feel the need to edit this file by yourself",
                "That's why it is called 'donotmodify.yml'",
                "The plugin author is not responsible for any problems that occur if this file is manually changed"
        ));
        config.save(configFile);
    }
    public static FileConfiguration getConfig(){
        return config;
    }

}

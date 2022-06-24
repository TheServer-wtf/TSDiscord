package hu.Pdani.TSDiscord;

import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
/// TODO: Split the API into a separate project
public abstract class TSDPlugin extends JavaPlugin {
    private static TSDPlugin plugin = null;
    void onEnable(TSDPlugin self){
        assert self != null;
        plugin = self;
    }
    public static boolean isStarted(){
        return plugin != null;
    }

    /**
     * Send a message to all channels on Discord
     * @param sender the plugin
     * @param message the message
     * @throws IllegalAccessException if the rate limit is exceeded
     */
    public abstract void sendMessage(@NotNull JavaPlugin sender, @NotNull String message) throws IllegalAccessException;
}

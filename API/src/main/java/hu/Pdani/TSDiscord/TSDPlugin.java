package hu.Pdani.TSDiscord;

import org.bukkit.plugin.java.JavaPlugin;

public abstract class TSDPlugin extends JavaPlugin {
    private static TSDPlugin plugin = null;
    public void onEnable(TSDPlugin self){
        assert self != null;
        plugin = self;
    }
    public static boolean isStarted(){
        return plugin != null;
    }
}

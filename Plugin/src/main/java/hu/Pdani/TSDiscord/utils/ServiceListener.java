package hu.Pdani.TSDiscord.utils;

import hu.Pdani.TSDiscord.TSDiscordPlugin;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServiceRegisterEvent;
import org.bukkit.event.server.ServiceUnregisterEvent;

public class ServiceListener implements Listener {
    @EventHandler
    public void onServiceChange(ServiceRegisterEvent e) {
        if (e.getProvider().getService() == Chat.class || e.getProvider().getService() == Permission.class) {
            TSDiscordPlugin.refreshVault(e.getProvider().getService());
        }
    }

    @EventHandler
    public void onServiceChange(ServiceUnregisterEvent e) {
        if (e.getProvider().getService() == Chat.class || e.getProvider().getService() == Permission.class) {
            TSDiscordPlugin.refreshVault(e.getProvider().getService());
        }
    }
}

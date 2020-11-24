package hu.Pdani.TSDiscord;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.Set;

public class DiscordChatEvent extends Event implements Cancellable {
    private boolean isCancelled;
    private static final HandlerList HANDLERS = new HandlerList();
    private String user;
    private String userId;
    private String message;
    private Set<Player> players;

    public DiscordChatEvent(String user, String message, Set<Player> players){
        super(true);
        this.user = user;
        this.userId = "";
        this.message = message;
        this.players = players;
    }

    public DiscordChatEvent(String user, String userId, String message, Set<Player> players){
        super(true);
        this.user = user;
        this.userId = userId;
        this.message = message;
        this.players = players;
    }

    public String getUser() {
        return user;
    }

    public String getUserId() {
        return userId;
    }

    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }

    public Set<Player> getPlayers() {
        return players;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    @Override
    public boolean isCancelled() {
        return isCancelled;
    }
    @Override
    public void setCancelled(boolean b) {
        isCancelled = b;
    }
}

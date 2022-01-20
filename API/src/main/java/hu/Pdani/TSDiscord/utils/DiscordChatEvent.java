package hu.Pdani.TSDiscord.utils;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class DiscordChatEvent extends Event implements Cancellable {
    private boolean isCancelled;
    private static final HandlerList HANDLERS = new HandlerList();
    private String user;
    private final DiscordChatEventOrigin origin;
    private String message;
    private final Set<Player> players;
    private final Player sender;

    public DiscordChatEvent(@NotNull String user, @NotNull String message, Set<Player> players){
        super(true);
        this.user = user;
        this.origin = DiscordChatEventOrigin.DISCORD;
        this.message = message;
        this.players = players;
        this.sender = null;
    }

    public DiscordChatEvent(@NotNull String user, @NotNull String message, Player sender){
        super(true);
        this.user = user;
        this.origin = DiscordChatEventOrigin.MINECRAFT;
        this.message = message;
        this.players = null;
        this.sender = sender;
    }

    /**
     * Get the name of the user who sent the message
     * @return Displayed name of user
     */
    public String getUser() {
        return user;
    }

    /**
     * Change the displayed name of the user
     * @param user the new display name
     */
    public void setUser(String user) {
        this.user = user;
    }

    /**
     * Get the origin of the message
     * @return origin of the message
     */
    public DiscordChatEventOrigin getOrigin() {
        return origin;
    }

    /**
     * Get the message the user sent
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Alter the message the user sent. <br>
     * This may be an empty string, but it cannot be null.
     * @param message the new message
     */
    public void setMessage(@NotNull String message) {
        this.message = message;
    }

    /**
     * Returns a set of players who'll receive the message
     * @return a set of players or null if {@link #getOrigin()} is Minecraft
     */
    public Set<Player> getPlayers() {
        return players;
    }

    /**
     * Get the sender of the message
     * @return sender of the message or null if {@link #getOrigin()} is Discord
     */
    public Player getSender() {
        return sender;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
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

    public enum DiscordChatEventOrigin {
        DISCORD,
        MINECRAFT
    }
}

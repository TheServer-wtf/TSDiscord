package hu.Pdani.TSDiscord;

import hu.Pdani.TSDiscord.exception.RateLimitException;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/// TODO: Split the API into a separate project
public abstract class TSDPlugin extends JavaPlugin {
    private static TSDPlugin plugin = null;

    // <Channel, Owner>
    private final Map<String, Channel> channels = new HashMap<>();

    void onEnable(TSDPlugin self){
        assert self != null;
        plugin = self;
    }
    public static boolean isStarted(){
        return plugin != null;
    }

    /**
     * Send a message to the given channel on Discord as the bot
     * @param sender the caller plugin
     * @param channelName the name of the channel
     * @param message the text message
     * @throws RateLimitException if the rate limit is exceeded
     * @throws IllegalCallerException if the sender is not the owner of the channel
     * @throws IllegalArgumentException if no channel is found with the given name
     */
    public abstract void sendMessage(@NotNull JavaPlugin sender, @NotNull String channelName, @NotNull String message) throws RateLimitException, IllegalCallerException, IllegalArgumentException;

    /**
     * Send a chat message to the given channel on Discord as a Player
     * @param sender the caller plugin
     * @param channelName the name of the channel
     * @param user the Player that sent the message
     * @param message the text message
     * @throws IllegalCallerException if the sender is not the owner of the channel
     * @throws IllegalArgumentException if no channel is found with the given name
     */
    public abstract void sendChat(@NotNull JavaPlugin sender, @NotNull String channelName, @NotNull Player user, @NotNull String message) throws IllegalCallerException, IllegalArgumentException;

    /**
     * Register a new message channel for use <br>
     * Great care should be taken that channels no longer in use are unregistered
     * @param owner the caller plugin
     * @param channelName the name of the channel
     * @deprecated this method will be removed in a future version, use {@link #registerChannel(JavaPlugin, Channel)} instead
     */
    @Deprecated(forRemoval = true)
    public void registerChannel(@NotNull JavaPlugin owner, @NotNull String channelName) {
        if(channelName.equalsIgnoreCase("status"))
            throw new IllegalArgumentException("Invalid channel name");
        if(channels.containsKey(channelName.toLowerCase()))
            throw new IllegalArgumentException("A channel already exists with the given name");
        Channel channel = new Channel(channelName, channelName);
        channel.plugin = owner;
        channels.put(channelName.toLowerCase(), channel);
    }

    /**
     * Unregister a channel that is no longer in use
     * @param owner the caller plugin
     * @param channelName the name of the channel
     */
    public void unregisterChannel(@NotNull JavaPlugin owner, @NotNull String channelName) {
        if(channelName.equalsIgnoreCase("status"))
            throw new IllegalArgumentException("Invalid channel name");
        if(!channels.containsKey(channelName.toLowerCase()))
            throw new IllegalArgumentException("A channel does not exists with the given name");
        if(!channels.get(channelName.toLowerCase()).plugin.equals(owner))
            throw new IllegalCallerException("The channel with the given name does not belong to caller");
        channels.remove(channelName.toLowerCase());
    }

    /**
     * Register a new message channel for use <br>
     * Great care should be taken that channels no longer in use are unregistered
     * @param owner the caller plugin
     * @param channel the name of the channel
     */
    public void registerChannel(@NotNull JavaPlugin owner, @NotNull Channel channel) {
        if(channel.name.equalsIgnoreCase("status"))
            throw new IllegalArgumentException("Invalid channel name");
        if(channels.containsKey(channel.name.toLowerCase()))
            throw new IllegalArgumentException("A channel already exists with the given name");
        channel.plugin = owner;
        channels.put(channel.name.toLowerCase(), channel);
    }

    /**
     * Gets the list of all currently registered channel names
     * @return a set of channel names
     */
    public Set<String> getChannelList() {
        return new HashSet<>(channels.keySet());
    }

    /**
     * Gets the list of all currently registered channels
     * @return a set of channels
     */
    public Set<Channel> getChannels() {
        return new HashSet<>(channels.values());
    }

    /**
     * Returns whether the sender is the owner of the given channel
     * @param caller the plugin to check against
     * @param channelName the name of the channel
     * @return {@code true} if the channel exists with the given name and {@code caller} is the owner, {@code false} otherwise
     */
    public boolean isChannelOwner(@NotNull JavaPlugin caller, @NotNull String channelName) {
        return channels.containsKey(channelName.toLowerCase()) && channels.get(channelName.toLowerCase()).plugin.equals(caller);
    }
}

package hu.Pdani.TSDiscord;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public class Channel {
    public final String name;
    public final String label;
    protected JavaPlugin plugin;

    /**
     * A text channel to use by another plugin
     * @param name name of the channel
     * @param label text to display when using the {@code addchannel} or {@code delchannel} commands
     */
    public Channel(String name, String label) {
        this.name = name;
        this.label = label;
    }

    /**
     * A text channel to use by another plugin
     * @param name name and label of the channel
     */
    public Channel(String name) {
        this(name, name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Channel channel)) return false;
        return Objects.equals(name, channel.name) && Objects.equals(label, channel.label);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, label);
    }
}

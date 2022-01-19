package hu.Pdani.TSDiscord.cmds;

import hu.Pdani.TSDiscord.TSDiscordPlugin;
import hu.Pdani.TSDiscord.utils.ProgramCommand;
import org.bukkit.entity.Player;
import org.javacord.api.interaction.callback.InteractionImmediateResponseBuilder;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OnlineCommand implements ProgramCommand {
    @Override
    public void run(InteractionImmediateResponseBuilder builder) {
        int online = TSDiscordPlugin.getPlugin().getServer().getOnlinePlayers().size();
        int max = TSDiscordPlugin.getPlugin().getServer().getMaxPlayers();
        Stream<? extends Player> players = TSDiscordPlugin.getPlugin().getServer().getOnlinePlayers().stream();
        builder.append("Online players ("+online+"/"+max+")");
        if(online > 0)
            builder.append(":")
                    .appendCode("",players.map(Player::getName).collect(Collectors.joining(", ")));
        builder.respond();
    }

    @Override
    public String getLabel() {
        return "online";
    }

    @Override
    public String getDescription() {
        return "Get a list of online players";
    }
}

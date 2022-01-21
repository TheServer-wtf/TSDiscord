package hu.Pdani.TSDiscord.cmds;

import hu.Pdani.TSDiscord.TSDiscordPlugin;
import hu.Pdani.TSDiscord.utils.ProgramCommand;
import org.bukkit.entity.Player;
import org.javacord.api.interaction.SlashCommandInteractionOption;
import org.javacord.api.interaction.SlashCommandOption;
import org.javacord.api.interaction.callback.InteractionImmediateResponseBuilder;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OnlineCommand implements ProgramCommand {
    @Override
    public void run(InteractionImmediateResponseBuilder builder, List<SlashCommandInteractionOption> options) {
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

    @Override
    public List<SlashCommandOption> getOptions() {
        return null;
    }
}

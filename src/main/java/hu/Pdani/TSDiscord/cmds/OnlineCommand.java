package hu.Pdani.TSDiscord.cmds;

import hu.Pdani.TSDiscord.TSDiscordPlugin;
import hu.Pdani.TSDiscord.utils.ProgramCommand;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.MessageAuthor;
import org.javacord.api.entity.server.Server;

import java.util.List;

public class OnlineCommand implements ProgramCommand {
    @Override
    public void run(MessageAuthor author, TextChannel channel, Server server, List<String> args) {
        int online = TSDiscordPlugin.getPlugin().getServer().getOnlinePlayers().size();
        int max = TSDiscordPlugin.getPlugin().getServer().getMaxPlayers();
        channel.sendMessage("Online players: "+ online+"/"+max);
    }

    @Override
    public String getLabel() {
        return "online";
    }
}

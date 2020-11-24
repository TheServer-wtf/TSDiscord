package hu.Pdani.TSDiscord.cmds;

import hu.Pdani.TSDiscord.TSDiscordPlugin;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.List;

import hu.Pdani.TSDiscord.utils.ProgramCommand;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.MessageAuthor;
import org.javacord.api.entity.server.Server;

public class TPSCommand implements ProgramCommand {
    @Override
    public void run(MessageAuthor author, TextChannel channel, Server server, List<String> args) {
        DecimalFormat df = new DecimalFormat("#.##");
        df.setRoundingMode(RoundingMode.CEILING);
        channel.sendMessage("Current TPS: "+ df.format(TSDiscordPlugin.tps));
    }

    @Override
    public String getLabel() {
        return "tps";
    }
}

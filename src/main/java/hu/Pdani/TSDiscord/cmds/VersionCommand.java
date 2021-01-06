package hu.Pdani.TSDiscord.cmds;

import hu.Pdani.TSDiscord.TSDiscordPlugin;
import hu.Pdani.TSDiscord.utils.ProgramCommand;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.MessageAuthor;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class VersionCommand implements ProgramCommand {
    @Override
    public void run(MessageAuthor author, TextChannel channel, Server server, List<String> args) {
        TSDiscordPlugin plugin = TSDiscordPlugin.getPlugin();
        EmbedBuilder builder = new EmbedBuilder();
        StringBuilder maker = new StringBuilder();
        for(String a : plugin.getDescription().getAuthors()){
            if(maker.length() == 0){
                maker.append(a);
            } else {
                maker.append(",").append(a);
            }
        }
        builder.setTitle("TSDiscord");
        builder.setDescription("Discord<->MC link plugin");
        builder.addField("Made by",maker.toString());
        builder.addField("Version",plugin.getDescription().getVersion());
        String timeFormat = plugin.getConfig().getString("message.topic.time", "dd/MM/yyyy HH:mm:ss");
        SimpleDateFormat formatter = new SimpleDateFormat(timeFormat);
        Date date = new Date();
        builder.setFooter(formatter.format(date));
        channel.sendMessage(builder).join();
    }

    @Override
    public String getLabel() {
        return "version";
    }
}

package hu.Pdani.TSDiscord.cmds;

import hu.Pdani.TSDiscord.TSDiscordPlugin;
import hu.Pdani.TSDiscord.utils.ProgramCommand;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.interaction.SlashCommandInteractionOption;
import org.javacord.api.interaction.SlashCommandOption;
import org.javacord.api.interaction.callback.InteractionImmediateResponseBuilder;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class VersionCommand implements ProgramCommand {
    @Override
    public void run(InteractionImmediateResponseBuilder builder, List<SlashCommandInteractionOption> options) {
        TSDiscordPlugin plugin = TSDiscordPlugin.getPlugin();
        EmbedBuilder embed = new EmbedBuilder();
        StringBuilder maker = new StringBuilder();
        for(String a : plugin.getDescription().getAuthors()){
            if(maker.length() == 0){
                maker.append(a);
            } else {
                maker.append(",").append(a);
            }
        }
        embed.setTitle("TSDiscord");
        embed.setDescription("Discord<->MC link plugin");
        embed.addField("Made by",maker.toString());
        embed.addField("Version",plugin.getDescription().getVersion());
        String timeFormat = plugin.getConfig().getString("message.time", "dd/MM/yyyy HH:mm:ss");
        SimpleDateFormat formatter = new SimpleDateFormat(timeFormat);
        Date date = new Date();
        embed.setFooter(formatter.format(date));
        builder.addEmbed(embed).respond();
    }

    @Override
    public String getLabel() {
        return "version";
    }

    @Override
    public String getDescription() {
        return "Returns information about the plugin";
    }

    @Override
    public List<SlashCommandOption> getOptions() {
        return null;
    }
}

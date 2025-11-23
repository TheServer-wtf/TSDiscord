package hu.Pdani.TSDiscord.cmds;

import hu.Pdani.TSDiscord.TSDiscordPlugin;
import hu.Pdani.TSDiscord.utils.ProgramCommand;
import org.javacord.api.entity.message.MessageFlag;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.interaction.SlashCommandInteractionOption;
import org.javacord.api.interaction.SlashCommandOption;
import org.javacord.api.interaction.callback.InteractionOriginalResponseUpdater;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class VersionCommand implements ProgramCommand {
    @Override
    public void run(InteractionOriginalResponseUpdater builder, List<SlashCommandInteractionOption> options) {
        TSDiscordPlugin plugin = TSDiscordPlugin.getPlugin();
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("TSDiscord");
        embed.setDescription("Discord<->MC link plugin");
        embed.addField("Made by",join(",", plugin.getDescription().getAuthors()));
        embed.addField("Version",plugin.getDescription().getVersion());
        embed.addField("Created for", "TheServer.wtf");
        String timeFormat = plugin.getConfig().getString("message.time", "dd/MM/yyyy HH:mm:ss");
        SimpleDateFormat formatter = new SimpleDateFormat(timeFormat);
        Date date = new Date();
        embed.setFooter(formatter.format(date));
        builder.addEmbed(embed).update();
    }

    private String join(CharSequence delimiter, List<String> list){
        list = new ArrayList<>(list);
        return switch (list.size()) {
            case 0 -> "";
            case 1 -> list.get(0);
            default -> {
                String last = list.remove(list.size()-1);
                yield String.join(delimiter, list) + " and " + last;
            }
        };
    }

    @NotNull
    @Override
    public String getLabel() {
        return "version";
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Returns information about the plugin";
    }

}

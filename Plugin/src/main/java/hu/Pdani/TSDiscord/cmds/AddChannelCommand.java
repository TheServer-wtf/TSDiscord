package hu.Pdani.TSDiscord.cmds;

import hu.Pdani.TSDiscord.TSDiscordPlugin;
import hu.Pdani.TSDiscord.utils.ProgramCommand;
import org.javacord.api.entity.channel.ChannelType;
import org.javacord.api.entity.channel.ServerChannel;
import org.javacord.api.entity.message.MessageFlag;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.interaction.SlashCommandInteractionOption;
import org.javacord.api.interaction.SlashCommandOption;
import org.javacord.api.interaction.SlashCommandOptionChoice;
import org.javacord.api.interaction.SlashCommandOptionType;
import org.javacord.api.interaction.callback.InteractionOriginalResponseUpdater;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class AddChannelCommand implements ProgramCommand {
    @Override
    public void run(InteractionOriginalResponseUpdater builder, List<SlashCommandInteractionOption> options) {
        SlashCommandInteractionOption typeOption = options.get(0);
        String type = typeOption.getStringValue().get();
        SlashCommandInteractionOption channelOption = options.get(1);
        if(channelOption.getChannelValue().isEmpty()){
            builder.setContent("You have to select the channel to be used!").update();
            return;
        }
        ServerChannel serverChannel = channelOption.getChannelValue().get();
        boolean isList = TSDiscordPlugin.getPlugin().getConfig().isList("channels."+type);
        String channel = TSDiscordPlugin.getPlugin().getConfig().getString("channels."+type,"");
        List<String> channels = new ArrayList<>();
        if(isList) {
            channels = TSDiscordPlugin.getPlugin().getConfig().getStringList("channels."+type);
        } else {
            if(!channel.isEmpty())
                channels.add(channel);
        }
        channels.add(serverChannel.getIdAsString());
        TSDiscordPlugin.getPlugin().getConfig().set("channels."+type,channels);
        TSDiscordPlugin.getPlugin().saveConfig();
        TSDiscordPlugin.getPlugin().reloadConfig();
        builder.setContent("New "+typeOption.getStringRepresentationValue().get()+" channel created for "+serverChannel.getName()+" ("+serverChannel.getIdAsString()+")").update();
    }

    @NotNull
    @Override
    public String getLabel() {
        return "addchannel";
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Set the provided channel to be used by the plugin";
    }

    @Nullable
    @Override
    public List<SlashCommandOption> getOptions() {
        return List.of(
            SlashCommandOption.createWithChoices(SlashCommandOptionType.STRING, "type", "The type of channel to set", true,
                List.of(
                    SlashCommandOptionChoice.create("text", "main"),
                    SlashCommandOptionChoice.create("status", "status")
                )
            ),
            SlashCommandOption.createChannelOption("channel", "The channel to use", true, List.of(ChannelType.SERVER_TEXT_CHANNEL))
        );
    }

    @NotNull
    @Override
    public PermissionType[] getDefaultPermissions() {
        return new PermissionType[]{PermissionType.ADMINISTRATOR};
    }

}

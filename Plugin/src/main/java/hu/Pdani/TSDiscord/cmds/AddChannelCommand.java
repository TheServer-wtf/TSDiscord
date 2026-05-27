package hu.Pdani.TSDiscord.cmds;

import hu.Pdani.TSDiscord.TSDiscordPlugin;
import hu.Pdani.TSDiscord.utils.ProgramCommand;
import org.javacord.api.entity.channel.ChannelType;
import org.javacord.api.entity.channel.ServerChannel;
import org.javacord.api.entity.message.MessageFlag;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.interaction.AutocompleteInteraction;
import org.javacord.api.interaction.SlashCommandInteractionOption;
import org.javacord.api.interaction.SlashCommandOption;
import org.javacord.api.interaction.SlashCommandOptionChoice;
import org.javacord.api.interaction.SlashCommandOptionType;
import org.javacord.api.interaction.callback.InteractionOriginalResponseUpdater;
import org.javacord.api.util.logging.ExceptionLogger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class AddChannelCommand implements ProgramCommand {
    @Override
    public void run(InteractionOriginalResponseUpdater builder, List<SlashCommandInteractionOption> options) {
        SlashCommandInteractionOption typeOption = options.get(0);
        String name = typeOption.getStringValue().get();
        SlashCommandInteractionOption channelOption = options.get(1);
        if(channelOption.getChannelValue().isEmpty()){
            builder.setContent("You have to select the channel to be used!").update();
            return;
        }
        ServerChannel serverChannel = channelOption.getChannelValue().get();
        boolean isList = TSDiscordPlugin.getPlugin().getConfig().isList("channels."+name);
        String channel = TSDiscordPlugin.getPlugin().getConfig().getString("channels."+name,"");
        List<String> channels = new ArrayList<>();
        if(isList) {
            channels = TSDiscordPlugin.getPlugin().getConfig().getStringList("channels."+name);
        } else {
            if(!channel.isEmpty())
                channels.add(channel);
        }
        channels.add(serverChannel.getIdAsString());
        TSDiscordPlugin.getPlugin().getConfig().set("channels."+name,channels);
        TSDiscordPlugin.getPlugin().saveConfig();
        TSDiscordPlugin.getPlugin().reloadConfig();
        builder.setContent("New "+typeOption.getStringRepresentationValue().get()+" channel created for "+serverChannel.getName()+" ("+serverChannel.getIdAsString()+")").update();
    }

    @Override
    public void autocomplete(AutocompleteInteraction interaction) {
        interaction.getOptionByName("type").ifPresent(option -> {
            if (option.isFocused().orElse(false)) {
                List<SlashCommandOptionChoice> choices = new ArrayList<>();
                choices.add(SlashCommandOptionChoice.create("status", "status"));
                choices.addAll(TSDiscordPlugin.getPlugin().getChannels().stream().map(channel -> SlashCommandOptionChoice.create(channel.label, channel.name)).toList());
                interaction.respondWithChoices(choices).exceptionally(ExceptionLogger.get());
            }
        });
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
            SlashCommandOption.createStringOption("type", "The type of channel to set", true, true),
            SlashCommandOption.createChannelOption("channel", "The channel to use", true, List.of(ChannelType.SERVER_TEXT_CHANNEL))
        );
    }

    @NotNull
    @Override
    public PermissionType[] getDefaultPermissions() {
        return new PermissionType[]{PermissionType.ADMINISTRATOR};
    }

}

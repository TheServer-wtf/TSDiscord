package hu.Pdani.TSDiscord.cmds;

import hu.Pdani.TSDiscord.TSDiscordPlugin;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.List;

import hu.Pdani.TSDiscord.utils.ProgramCommand;
import org.javacord.api.entity.message.MessageFlag;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.interaction.SlashCommandInteractionOption;
import org.javacord.api.interaction.SlashCommandOption;
import org.javacord.api.interaction.callback.InteractionOriginalResponseUpdater;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TPSCommand implements ProgramCommand {
    @Override
    public void run(InteractionOriginalResponseUpdater builder, List<SlashCommandInteractionOption> options) {
        DecimalFormat df = new DecimalFormat("#.##");
        df.setRoundingMode(RoundingMode.CEILING);
        builder.setContent("Current TPS: "+ df.format(TSDiscordPlugin.tps)).update();
    }

    @NotNull
    @Override
    public String getLabel() {
        return "tps";
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Get the current server TPS";
    }

    @NotNull
    @Override
    public PermissionType[] getDefaultPermissions() {
        return new PermissionType[]{PermissionType.MODERATE_MEMBERS};
    }
}

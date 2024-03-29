package hu.Pdani.TSDiscord.cmds;

import hu.Pdani.TSDiscord.TSDiscordPlugin;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.List;

import hu.Pdani.TSDiscord.utils.ProgramCommand;
import org.javacord.api.interaction.SlashCommandInteractionOption;
import org.javacord.api.interaction.SlashCommandOption;
import org.javacord.api.interaction.callback.InteractionImmediateResponseBuilder;

public class TPSCommand implements ProgramCommand {
    @Override
    public void run(InteractionImmediateResponseBuilder builder, List<SlashCommandInteractionOption> options) {
        DecimalFormat df = new DecimalFormat("#.##");
        df.setRoundingMode(RoundingMode.CEILING);
        builder.setContent("Current TPS: "+ df.format(TSDiscordPlugin.tps)).respond();
    }

    @Override
    public String getLabel() {
        return "tps";
    }

    @Override
    public String getDescription() {
        return "Get the current server TPS";
    }

    @Override
    public List<SlashCommandOption> getOptions() {
        return null;
    }
}

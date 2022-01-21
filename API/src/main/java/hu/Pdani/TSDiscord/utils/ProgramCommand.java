package hu.Pdani.TSDiscord.utils;

import org.javacord.api.interaction.SlashCommandInteractionOption;
import org.javacord.api.interaction.SlashCommandOption;
import org.javacord.api.interaction.callback.InteractionImmediateResponseBuilder;

import java.util.List;

public interface ProgramCommand {
    void run(InteractionImmediateResponseBuilder builder, List<SlashCommandInteractionOption> options);
    String getLabel();
    String getDescription();
    List<SlashCommandOption> getOptions();
}

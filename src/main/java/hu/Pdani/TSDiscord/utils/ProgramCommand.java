package hu.Pdani.TSDiscord.utils;

import org.javacord.api.interaction.callback.InteractionImmediateResponseBuilder;

public interface ProgramCommand {
    void run(InteractionImmediateResponseBuilder builder);
    String getLabel();
    String getDescription();
}

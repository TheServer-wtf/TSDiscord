package hu.Pdani.TSDiscord.utils;

import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.interaction.SlashCommandInteractionOption;
import org.javacord.api.interaction.SlashCommandOption;
import org.javacord.api.interaction.callback.InteractionOriginalResponseUpdater;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface ProgramCommand {
    void run(InteractionOriginalResponseUpdater builder, List<SlashCommandInteractionOption> options);
    @NotNull String getLabel();
    @NotNull String getDescription();

    @Nullable
    default List<SlashCommandOption> getOptions() {
        return null;
    }

    default boolean isEphemeral() {
        return true;
    }

    @NotNull
    default PermissionType[] getDefaultPermissions() {
        return new PermissionType[0];
    }
}

package hu.Pdani.TSDiscord.utils;

import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.MessageAuthor;
import org.javacord.api.entity.server.Server;

import java.util.List;

public interface ProgramCommand {
    void run(MessageAuthor author, TextChannel channel, Server server, List<String> args);
    String getLabel();
    String getDescription();
}

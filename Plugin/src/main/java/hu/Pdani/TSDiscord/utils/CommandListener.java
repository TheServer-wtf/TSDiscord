package hu.Pdani.TSDiscord.utils;

import hu.Pdani.TSDiscord.TSDiscordPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.event.interaction.SlashCommandCreateEvent;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.listener.interaction.SlashCommandCreateListener;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class CommandListener implements CommandExecutor, SlashCommandCreateListener {
    @Override
    public boolean onCommand(CommandSender sender, @NotNull Command cmd, @NotNull String alias, String[] args) {
        if(!sender.hasPermission("tsdiscord.admin"))
            return true;
        if(args.length > 0){
            if ("debug".equalsIgnoreCase(args[0])) {
                if (!(sender instanceof Player))
                    return true;
                if (TSDiscordPlugin.getPlugin().debug.contains((Player) sender)) {
                    TSDiscordPlugin.getPlugin().debug.remove((Player) sender);
                    sender.sendMessage("Disabled debug.");
                } else {
                    TSDiscordPlugin.getPlugin().debug.add((Player) sender);
                    sender.sendMessage("Enabled debug.");
                }
                return true;
            } else if("reload".equalsIgnoreCase(args[0])){
                TSDiscordPlugin.getPlugin().reloadConfig();
                ImportantConfig.reloadConfig();
                sender.sendMessage("Config reloaded.");
                return true;
            }
        }
        sender.sendMessage(ChatColor.GOLD+"TSDiscord v"+TSDiscordPlugin.getPlugin().getDescription().getVersion());
        sender.sendMessage(ChatColor.RED+"Usage: /"+alias+" [reload/debug]");
        return true;
    }

    @Override
    public void onSlashCommandCreate(SlashCommandCreateEvent cmdEvent) {
        SlashCommandInteraction interaction = cmdEvent.getSlashCommandInteraction();
        ProgramCommand cmd = CommandManager.get(interaction.getCommandName());
        if(cmd != null) {
            TSDiscordPlugin.getPlugin().sendDebug("Handling command "+cmd.getLabel());
            cmd.run(interaction.respondLater(cmd.isEphemeral()).join(), interaction.getOptions());
        }
    }
}

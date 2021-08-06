package hu.Pdani.TSDiscord.utils;

import hu.Pdani.TSDiscord.BotHandler;
import hu.Pdani.TSDiscord.TSDiscordPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageAuthor;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.message.MessageCreateListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class CommandListener implements CommandExecutor, MessageCreateListener {
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String alias, String[] args) {
        if(!sender.hasPermission("tsdiscord.admin"))
            return true;
        if(args.length > 0){
            switch (args[0].toLowerCase()){
                case "debug":
                    if(!(sender instanceof Player))
                        return true;
                    if(TSDiscordPlugin.getPlugin().debug.contains((Player) sender)){
                        TSDiscordPlugin.getPlugin().debug.remove((Player) sender);
                        sender.sendMessage("Disabled debug.");
                    } else {
                        TSDiscordPlugin.getPlugin().debug.add((Player) sender);
                        sender.sendMessage("Enabled debug.");
                    }
                    return true;
                default:
                    break;
            }
        }
        TSDiscordPlugin.getPlugin().reloadConfig();
        ImportantConfig.reloadConfig();
        sender.sendMessage("Config reloaded.");
        return true;
    }

    @Override
    public void onMessageCreate(MessageCreateEvent gmre) {
        TSDiscordPlugin plugin = TSDiscordPlugin.getPlugin();
        if(BotHandler.shutdown) {
            return;
        }
        if(!gmre.isServerMessage())
            return;
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("channels");
        if(section != null) {
            if(!gmre.getChannel().getIdAsString().equals(plugin.getConfig().getString("channels.main",""))
                    && !gmre.getChannel().getIdAsString().equals(plugin.getConfig().getString("channels.mature","")))
                return;
        }
        Message msg = gmre.getMessage();
        MessageAuthor author = gmre.getMessageAuthor();
        if(author.isWebhook() || author.isBotUser() || author.isYourself())
            return;
        String message = msg.getReadableContent();
        if(!BotHandler.isCommand(message)
                || !BotHandler.hasCommand(message)) {
            return;
        }
        String prefix = plugin.getConfig().getString("prefix",">");
        if(prefix == null)
            prefix = ">";
        ArrayList<String> add = new ArrayList<>(Arrays.asList(message.split(" ")));
        String label = add.remove(0).replaceFirst(Pattern.quote(prefix),"");
        List<String> args = message.contains(" ") ? add : new ArrayList<>();
        ProgramCommand programCommand = CommandManager.get(label);
        if(programCommand != null){
            programCommand.run(author,gmre.getChannel(),gmre.getServer().orElse(null),args);
        }
    }
}

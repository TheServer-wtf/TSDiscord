package hu.Pdani.TSDiscord.utils;

import hu.Pdani.TSDiscord.TSDiscordPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandListener implements CommandExecutor {
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
}

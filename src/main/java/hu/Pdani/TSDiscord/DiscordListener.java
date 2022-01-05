package hu.Pdani.TSDiscord;

import hu.Pdani.TSDiscord.utils.DiscordChatEvent;
import org.apache.commons.lang.StringEscapeUtils;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageAuthor;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.message.MessageCreateListener;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static hu.Pdani.TSDiscord.TSDiscordPlugin.c;

public class DiscordListener implements Listener, MessageCreateListener {
    private final TSDiscordPlugin plugin;

    public DiscordListener(TSDiscordPlugin plugin){
        this.plugin = plugin;
    }

    public String escapeName(String name){
        name = name.replaceAll("_","\\_");
        return name;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event){
        if(!BotHandler.isEnabled() || BotHandler.shutdown)
            return;
        String join = plugin.getConfig().getString("message.join","%player% joined the server.");
        boolean isList = plugin.getConfig().isList("channels.main");
        String channel = plugin.getConfig().getString("channels.main","");
        List<String> channels = new ArrayList<>();
        if(isList) {
            channels = plugin.getConfig().getStringList("channels.main");
        } else {
            if(!channel.isEmpty())
                channels.add(channel);
        }
        if(!channels.isEmpty()) {
            channels.forEach((c)->plugin.getBot().getTextChannelById(c).ifPresent(tc -> tc.sendMessage(join.replace("%player%", escapeName(event.getPlayer().getName()))).join()));
        }
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event){
        if(!BotHandler.isEnabled() || BotHandler.shutdown)
            return;
        String quit = plugin.getConfig().getString("message.leave","%player% left the server.");
        boolean isList = plugin.getConfig().isList("channels.main");
        String channel = plugin.getConfig().getString("channels.main","");
        List<String> channels = new ArrayList<>();
        if(isList) {
            channels = plugin.getConfig().getStringList("channels.main");
        } else {
            if(!channel.isEmpty())
                channels.add(channel);
        }
        if(!channels.isEmpty()) {
            channels.forEach((c)->plugin.getBot().getTextChannelById(c).ifPresent(tc -> tc.sendMessage(quit.replace("%player%", escapeName(event.getPlayer().getName()))).join()));
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e){
        TSDiscordPlugin.getPlugin().sendDebug("PlayerChatEvent received!");
        if(!e.isCancelled())
            BotHandler.chat(e.getPlayer(),e.getMessage());
    }

    @Override
    public void onMessageCreate(MessageCreateEvent gmre) {
        if(BotHandler.shutdown) {
            return;
        }
        if(!gmre.isServerMessage())
            return;
        boolean isList = plugin.getConfig().isList("channels.main");
        String channel = plugin.getConfig().getString("channels.main","");
        List<String> channels = new ArrayList<>();
        if(isList) {
            channels = plugin.getConfig().getStringList("channels.main");
        } else {
            if(!channel.isEmpty())
                channels.add(channel);
        }
        if(!channels.contains(gmre.getChannel().getIdAsString()))
            return;
        Message msg = gmre.getMessage();
        MessageAuthor author = gmre.getMessageAuthor();
        String message = msg.getReadableContent();
        if(author.isWebhook() || author.isBotUser() || author.isYourself() || message.isEmpty())
            return;
        if(BotHandler.hasCommand(message)) {
            TSDiscordPlugin.getPlugin().sendDebug("The message contains a valid command, skipping.");
            return;
        }
        TSDiscordPlugin.getPlugin().sendDebug("Message received!");
        String chatFormat = plugin.getConfig().getString("chatFormat","&8[&eDISCORD&8] &a{user}: &f{msg}");
        if(chatFormat == null || chatFormat.isEmpty())
            chatFormat = plugin.getConfig().getDefaults().getString("chatFormat","&8[&eDISCORD&8] &a{user}: &f{msg}");
        chatFormat = chatFormat.replace("{user}","%1$s");
        chatFormat = chatFormat.replace("{msg}","%2$s");
        chatFormat = StringEscapeUtils.unescapeJava(chatFormat);
        if(plugin.getConfig().getBoolean("hexColor",false)){
            chatFormat = getHexColors(chatFormat);
        }
        message = getMentionNicks(message,gmre.getServer().orElse(null), msg.getMentionedUsers());
        plugin.getServer().getConsoleSender().sendMessage(String.format(c(chatFormat),author.getDisplayName(),message));
        DiscordChatEvent dcevent = new DiscordChatEvent(author.getDisplayName(),author.getIdAsString(),message,new HashSet<>(plugin.getServer().getOnlinePlayers()));
        plugin.getServer().getPluginManager().callEvent(dcevent);
        if(dcevent.isCancelled())
            return;
        for(Player p : dcevent.getPlayers()){
            p.sendMessage(String.format(c(chatFormat),dcevent.getUser(),format(ChatColor.stripColor(dcevent.getMessage()))));
        }
    }

    private String format(String message){
        message = message.replaceAll("\\*\\*\\*(.+?)\\*\\*\\*",ChatColor.ITALIC+""+ChatColor.BOLD+"$1"+ChatColor.RESET);
        message = message.replaceAll("\\*\\*(.+?)\\*\\*",ChatColor.BOLD+"$1"+ChatColor.RESET);
        message = message.replaceAll("\\*(.+?)\\*",ChatColor.ITALIC+"$1"+ChatColor.RESET);
        message = message.replaceAll("___(.+?)___",ChatColor.ITALIC+""+ChatColor.UNDERLINE+"$1"+ChatColor.RESET);
        message = message.replaceAll("__(.+?)__",ChatColor.UNDERLINE+"$1"+ChatColor.RESET);
        message = message.replaceAll("_(.+?)_",ChatColor.ITALIC+"$1"+ChatColor.RESET);
        message = message.replaceAll("~~(.+?)~~",ChatColor.STRIKETHROUGH+"$1"+ChatColor.RESET);
        return message;
    }

    private String getHexColors(String text){
        Pattern hexPattern = Pattern.compile("#[A-Fa-f0-9]{6}");
        Matcher matcher = hexPattern.matcher(text);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(result, net.md_5.bungee.api.ChatColor.of(matcher.group()).toString());
        }
        matcher.appendTail(result);
        text = result.toString();
        return text;
    }

    private String getMentionNicks(String text, Server server, List<User> mentioned){
        Pattern hexPattern = Pattern.compile("<@!([0-9]+)>");
        Matcher matcher = hexPattern.matcher(text);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            User user = null;
            for(User m : mentioned) {
                if(m.getIdAsString().equals(matcher.group(1))){
                    user = m;
                    break;
                }
            }
            if(user == null)
                continue;
            String nick = server != null ? "@"+user.getDisplayName(server) : user.getNicknameMentionTag();
            matcher.appendReplacement(result, nick);
        }
        matcher.appendTail(result);
        text = result.toString();
        return text;
    }

    private boolean hasIgnoredRole(Server guild, List<Role> roles){
        List<String> ignoredString = plugin.getConfig().getStringList("roleIgnore");
        if(ignoredString.isEmpty())
            return false;
        List<Role> ignored = new ArrayList<>();
        for(Role r : guild.getRoles()){
            if(ignoredString.contains(r.getIdAsString())){
                ignored.add(r);
            }
        }
        for(Role r : roles){
            if(ignored.contains(r))
                return true;
        }
        return false;
    }
}

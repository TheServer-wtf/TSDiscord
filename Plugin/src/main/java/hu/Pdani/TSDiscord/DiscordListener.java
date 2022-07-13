package hu.Pdani.TSDiscord;

import hu.Pdani.TSDiscord.utils.DiscordChatEvent;
import hu.Pdani.TSDiscord.utils.ImportantConfig;
import org.apache.commons.lang.StringEscapeUtils;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageAuthor;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.entity.webhook.Webhook;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.message.MessageCreateListener;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static hu.Pdani.TSDiscord.TSDiscordPlugin.c;

public class DiscordListener implements Listener, MessageCreateListener {
    private final TSDiscordPlugin plugin;

    public DiscordListener(TSDiscordPlugin plugin){
        this.plugin = plugin;
    }

    public String escapeName(String name){
        name = name.replaceAll(Pattern.quote("_"),"\\_");
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
            String msg = "**`" + join.replace("%player%", escapeName(event.getPlayer().getName())) + "`**";
            channels.forEach((c)->plugin.getBot().getTextChannelById(c).ifPresent(tc -> tc.sendMessage(msg).join()));
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
            String msg = "**`" + quit.replace("%player%", escapeName(event.getPlayer().getName())) + "`**";
            channels.forEach((c)->plugin.getBot().getTextChannelById(c).ifPresent(tc -> tc.sendMessage(msg).join()));
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e){
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
        if(!author.isRegularUser() || message.isEmpty())
            return;
        boolean isReply = msg.getReferencedMessage().isPresent();
        String chatFormat = plugin.getConfig().getString("chatFormat.normal","&8[&eDISCORD&8] &a{user}: &f{msg}");
        String replyFormat = plugin.getConfig().getString("chatFormat.reply","&8[&eDISCORD&8] &a{user} replied to {target}: &f{msg}");
        if(chatFormat.isEmpty() || replyFormat.isEmpty())
            return;
        chatFormat = chatFormat.replace("{user}","%1$s");
        chatFormat = chatFormat.replace("{msg}","%2$s");
        chatFormat = StringEscapeUtils.unescapeJava(chatFormat);
        replyFormat = replyFormat.replace("{user}","%1$s");
        replyFormat = replyFormat.replace("{target}", "%2$s");
        replyFormat = replyFormat.replace("{msg}","%3$s");
        replyFormat = StringEscapeUtils.unescapeJava(replyFormat);
        if(plugin.getConfig().getBoolean("hexColor",false)){
            chatFormat = getHexColors(chatFormat);
            replyFormat = getHexColors(replyFormat);
        }
        message = getMentionNicks(message,gmre.getServer().orElse(null), msg.getMentionedUsers());
        DiscordChatEvent dcevent = new DiscordChatEvent(getUserNick(Objects.requireNonNull(gmre.getServer().orElse(null)), Objects.requireNonNull(author.asUser().orElse(null))),message,new HashSet<>(plugin.getServer().getOnlinePlayers()));
        plugin.getServer().getPluginManager().callEvent(dcevent);
        if(dcevent.isCancelled() || dcevent.getMessage().isEmpty())
            return;
        if(!isReply)
            plugin.getServer().getConsoleSender().sendMessage(String.format(c(chatFormat),dcevent.getUser(),dcevent.getMessage()));
        else
            plugin.getServer().getConsoleSender().sendMessage(String.format(c(replyFormat),dcevent.getUser(),msg.getReferencedMessage().get().getAuthor().getDisplayName(),dcevent.getMessage()));
        for(Player p : dcevent.getPlayers()){
            if(!isReply)
                p.sendMessage(String.format(c(chatFormat),dcevent.getUser(),format(ChatColor.stripColor(dcevent.getMessage()))));
            else
                p.sendMessage(String.format(c(replyFormat),dcevent.getUser(),msg.getReferencedMessage().get().getAuthor().getDisplayName(),format(ChatColor.stripColor(dcevent.getMessage()))));
        }
        FileConfiguration important = ImportantConfig.getConfig();
        boolean modify = false;
        for(String c : channels){
            if(c.equals(gmre.getChannel().getIdAsString()))
                continue;
            String hookId = important.getString("webhooks." + c, "");
            if (hookId.isEmpty()) {
                ServerTextChannel tc = gmre.getApi().getServerTextChannelById(c).orElse(null);
                if(tc == null)
                    continue;
                Webhook webhook = tc.createWebhookBuilder().setName("TSDiscord").setAuditLogReason("Missing webhook for TSDiscord").setAvatar(gmre.getApi().getYourself().getAvatar()).create().join();
                hookId = webhook.getIdAsString();
                important.set("webhooks." + tc.getId(), hookId);
                modify = true;
            }
            BotHandler.sendWebhook(hookId, message, dcevent.getUser(), author.getAvatar().getUrl().toString(), msg);
        }
        if(modify){
            try {
                ImportantConfig.saveConfig();
            } catch (IOException ex) {
                plugin.getLogger().warning("Unable to save 'donotmodify.yml' to disk: "+ex.toString());
            }
            ImportantConfig.reloadConfig();
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
        Pattern hexPattern = Pattern.compile("<@([0-9]+)>");
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

    private String getUserNick(@NotNull Server server, @NotNull User user) {
        return user.getNickname(server).orElse(user.getDisplayName(server));
    }
}

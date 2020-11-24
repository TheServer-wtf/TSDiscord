package hu.Pdani.TSDiscord;

import hu.Pdani.TSDiscord.utils.ImportantConfig;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageAttachment;
import org.javacord.api.entity.message.MessageAuthor;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.message.MessageCreateListener;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.*;
//import java.util.*;

import static hu.Pdani.TSDiscord.TSDiscordPlugin.c;

public class DiscordListener implements Listener, MessageCreateListener {
    private final TSDiscordPlugin plugin;

    public DiscordListener(TSDiscordPlugin plugin){
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event){
        if(!BotHandler.isEnabled() || BotHandler.shutdown)
            return;
        String join = plugin.getConfig().getString("message.join","%player% joined the server.");
        String channel = plugin.getConfig().getString("channels.main","");
        String mature = plugin.getConfig().getString("channels.mature","");
        if(!channel.isEmpty()) {
            plugin.getBot().getTextChannelById(channel).ifPresent(tc -> tc.sendMessage(join.replace("%player%", event.getPlayer().getName())).join());
        }
        if(!mature.isEmpty()) {
            plugin.getBot().getTextChannelById(mature).ifPresent(tc -> tc.sendMessage(join.replace("%player%", event.getPlayer().getName())).join());
        }
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event){
        if(!BotHandler.isEnabled() || BotHandler.shutdown)
            return;
        String quit = plugin.getConfig().getString("message.leave","%player% left the server.");
        String channel = plugin.getConfig().getString("channels.main","");
        String mature = plugin.getConfig().getString("channels.mature","");
        if(!channel.isEmpty()) {
            plugin.getBot().getTextChannelById(channel).ifPresent(tc -> tc.sendMessage(quit.replace("%player%", event.getPlayer().getName())).join());
        }
        if(!mature.isEmpty()) {
            plugin.getBot().getTextChannelById(mature).ifPresent(tc -> tc.sendMessage(quit.replace("%player%", event.getPlayer().getName())).join());
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e){
        if(!e.isCancelled())
            BotHandler.chat(e.getPlayer(),e.getMessage());
        else
            TSDiscordPlugin.getPlugin().sendDebug("PlayerChatEvent was cancelled");
    }

    @Override
    public void onMessageCreate(MessageCreateEvent gmre) {
        if(BotHandler.shutdown) {
            TSDiscordPlugin.getPlugin().sendDebug("Bot is marked for shutdown?");
            return;
        }
        /*if(e instanceof ReadyEvent){
            String channel = plugin.getConfig().getString("channelId","");
            if(channel.isEmpty()){
                channel = TSDiscordPlugin.getPlugin().getConfig().getString("channels.main","");
            }
            String mature = TSDiscordPlugin.getPlugin().getConfig().getString("channels.mature","");
            if(!channel.isEmpty()) {
                TextChannel tc = plugin.getBot().getBot().getTextChannelById(channel);
                if(tc != null) {
                    String startup = plugin.getConfig().getString("message.startup",":white_check_mark: Server is online.");
                    if(startup != null && !startup.isEmpty())
                        tc.sendMessage(startup).complete();
                } else {
                    TSDiscordPlugin.getPlugin().sendDebug("Channel is not found");
                }
            } else {
                TSDiscordPlugin.getPlugin().sendDebug("Channel is empty in config");
            }
            if(!mature.isEmpty()) {
                TextChannel tc = plugin.getBot().getBot().getTextChannelById(mature);
                if(tc != null) {
                    String startup = plugin.getConfig().getString("message.startup",":white_check_mark: Server is online.");
                    if(startup != null && !startup.isEmpty())
                        tc.sendMessage(startup).complete();
                }
            }
            return;
        }
        if(!(e instanceof GuildMessageReceivedEvent)) {
            return;
        }*/
        //GuildMessageReceivedEvent gmre = (GuildMessageReceivedEvent) e;
        if(!gmre.isServerMessage())
            return;
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("channels");
        if(section != null) {
            if(!gmre.getChannel().getIdAsString().equals(plugin.getConfig().getString("channels.main",""))
            && !gmre.getChannel().getIdAsString().equals(plugin.getConfig().getString("channels.mature","")))
                return;
        }
        Message msg = gmre.getMessage();
        if(msg.getAuthor().getIdAsString().equals(plugin.getBot().getYourself().getIdAsString()))
            return;
        /*if(BotHandler.isCommand(gmre.getMessage().getContentDisplay())
                && BotHandler.hasCommand(gmre.getMessage().getContentDisplay())) {
            TSDiscordPlugin.getPlugin().sendDebug("The message contains a valid command, skipping.");   // TODO: Reimplement this using Javacord
            return;
        }*/
        TSDiscordPlugin.getPlugin().sendDebug("Message received!");
        boolean isMature = gmre.getChannel().getIdAsString().equals(plugin.getConfig().getString("channels.mature",""));
        String message = gmre.getMessage().getReadableContent();
        MessageAuthor author = gmre.getMessageAuthor();
        if(!isMature && plugin.checkSwearing(message)){
            List<MessageAttachment> files = gmre.getMessage().getAttachments();
            gmre.getMessage().delete().join();
            String hook = ImportantConfig.getConfig().getString("webhooks."+gmre.getChannel().getIdAsString(),"");
            if(!hook.isEmpty()) {
                Object censored = null;
                if(plugin.getCPlugin() != null) {
                    try {
                        Method method = plugin.getCPlugin().getClass().getDeclaredMethod("convert", String.class, Integer.class);
                        if (!method.isAccessible()) {
                            method.setAccessible(true);
                            censored = method.invoke(plugin.getCPlugin(), message, 2);
                            method.setAccessible(false);
                        } else {
                            censored = method.invoke(plugin.getCPlugin(), message, 2);
                        }
                    } catch (NoSuchMethodException | SecurityException | IllegalAccessException | InvocationTargetException ex) {
                        plugin.getLogger().severe("There was an error trying to censor the message: " + ex.toString());
                        plugin.sendDebug("There was an error trying to censor the message: " + ex.toString());
                    }
                }
                if(censored != null)
                    BotHandler.sendWebhook(hook, (String)censored,author.getDisplayName(),author.getAvatar().getUrl().toString(),files);
            }
            Server guild = gmre.getServer().orElse(null);
            User member = author.asUser().orElse(null);
            if(guild != null && member != null && !hasIgnoredRole(guild,member.getRoles(guild))) {
                String log = plugin.getConfig().getString("channels.log", "");
                TextChannel logChannel = plugin.getBot().getTextChannelById(log).orElse(null);
                if (logChannel != null) {
                    EmbedBuilder embed = new EmbedBuilder();
                    embed.setTitle("Swear Log");
                    embed.addField("User", gmre.getMessageAuthor().getDiscriminatedName(), false);
                    embed.addField("Message", message, false);
                    String timeFormat = TSDiscordPlugin.getPlugin().getConfig().getString("message.topic.time", "dd/MM/yyyy HH:mm:ss");
                    SimpleDateFormat formatter = new SimpleDateFormat(timeFormat);
                    Date date = new Date();
                    embed.setFooter(formatter.format(date));
                    logChannel.sendMessage(embed).join();
                }
            }
        }
        Set<Player> players = new HashSet<>(plugin.getServer().getOnlinePlayers());
        DiscordChatEvent event = new DiscordChatEvent(author.getDisplayName(), author.getIdAsString(), message, players);
        plugin.getServer().getPluginManager().callEvent(event);
        boolean censorError = false;
        Method method = null;
        Object censored = null;
        if(plugin.getCPlugin() != null) {
            try {
                method = plugin.getCPlugin().getClass().getDeclaredMethod("convert", String.class, Integer.class);
                if (!method.isAccessible()) {
                    method.setAccessible(true);
                }
            } catch (NoSuchMethodException | SecurityException ex) {
                plugin.getLogger().severe("There was an error trying to censor the message: " + ex.toString());
                plugin.sendDebug("There was an error trying to censor the message: " + ex.toString());
            }
        }
        if(!event.isCancelled()){
            String chatFormat = plugin.getConfig().getString("chatFormat","&8[&eDISCORD&8] &a{user}: &f{msg}");
            if(chatFormat == null || chatFormat.isEmpty())
                chatFormat = plugin.getConfig().getDefaults().getString("chatFormat","&8[&eDISCORD&8] &a{user}: &f{msg}");
            plugin.getServer().getConsoleSender().sendMessage(c("&8[&eDISCORD&8] &a"+event.getUser()+": &f")+format(event.getMessage()));
            Field field = null;
            Object censObj = null;
            HashMap<String, Double> cens = new HashMap<>();
            for(Player p : event.getPlayers()){
                if(!censorError && method != null) {
                    try {
                        if(field == null){
                            field = plugin.getCPlugin().getClass().getDeclaredField("cens");
                            field.setAccessible(true);
                            censObj = field.get(plugin.getCPlugin());
                            cens = (HashMap<String, Double>) censObj;
                        }
                        censored = method.invoke(plugin.getCPlugin(), message, Integer.parseInt(cens.get(p.getUniqueId().toString()).toString().split("\\.")[0]));
                    } catch (IllegalAccessException | InvocationTargetException | NoSuchFieldException | SecurityException ex) {
                        plugin.getLogger().severe("There was an error trying to censor the message: " + ex.toString());
                        plugin.sendDebug("There was an error trying to censor the message: " + ex.toString());
                        censorError = true;
                        censored = null;
                    }
                }
                String send = (censored == null) ? message : (String)censored;
                send = format(ChatColor.stripColor(send));
                p.sendMessage(c("&8[&eDISCORD&8] &a"+event.getUser()+": &f")+send);
            }
        }
    }

    private String format(String message){
        message = message.replace("***(.+)***","&l&o$1&r");
        message = message.replace("**(.+)**","&l$1&r");
        message = message.replace("*(.+)*","&o$1&r");
        message = message.replace("___(.+)___","&o&n$1&r");
        message = message.replace("__(.+)__","&n$1&r");
        message = message.replace("_(.+)_","&o$1&r");
        message = message.replace("~~(.+)~~","&m$1&r");
        return ChatColor.translateAlternateColorCodes('&',message);
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

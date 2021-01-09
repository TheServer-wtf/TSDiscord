package hu.Pdani.TSDiscord;

import com.mikemik44.censor.Censor;
import hu.Pdani.TSDiscord.utils.ImportantConfig;
import hu.Pdani.TSDiscord.utils.SwearUtil;
import org.apache.commons.lang.StringEscapeUtils;
import org.bukkit.Bukkit;
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
import org.javacord.api.entity.message.MessageAuthor;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.message.MessageCreateListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static hu.Pdani.TSDiscord.TSDiscordPlugin.c;

public class DiscordListener implements Listener, MessageCreateListener {
    private final TSDiscordPlugin plugin;

    public DiscordListener(TSDiscordPlugin plugin){
        this.plugin = plugin;
    }

    public String escapeName(String name){
        name = name.replaceAll("_(.+?)_","\\_$1\\_");
        return name;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event){
        if(!BotHandler.isEnabled() || BotHandler.shutdown)
            return;
        String join = plugin.getConfig().getString("message.join","%player% joined the server.");
        String channel = plugin.getConfig().getString("channels.main","");
        String mature = plugin.getConfig().getString("channels.mature","");
        if(!channel.isEmpty()) {
            plugin.getBot().getTextChannelById(channel).ifPresent(tc -> tc.sendMessage(join.replace("%player%", escapeName(event.getPlayer().getName()))).join());
        }
        if(!mature.isEmpty()) {
            plugin.getBot().getTextChannelById(mature).ifPresent(tc -> tc.sendMessage(join.replace("%player%", escapeName(event.getPlayer().getName()))).join());
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
            plugin.getBot().getTextChannelById(channel).ifPresent(tc -> tc.sendMessage(quit.replace("%player%", escapeName(event.getPlayer().getName()))).join());
        }
        if(!mature.isEmpty()) {
            plugin.getBot().getTextChannelById(mature).ifPresent(tc -> tc.sendMessage(quit.replace("%player%", escapeName(event.getPlayer().getName()))).join());
        }
    }

    boolean alreadySent = false;

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e){
        if(e == null || e.getRecipients().size() < TSDiscordPlugin.getPlugin().getServer().getOnlinePlayers().size())
            return;
        if(plugin.getServer().getOnlinePlayers().size() == 1){
            if(alreadySent) {
                alreadySent = false;
                return;
            } else {
                alreadySent = true;
            }
        } else {
            alreadySent = false;
        }
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
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("channels");
        if(section != null) {
            if(!gmre.getChannel().getIdAsString().equals(plugin.getConfig().getString("channels.main",""))
            && !gmre.getChannel().getIdAsString().equals(plugin.getConfig().getString("channels.mature","")))
                return;
        }
        Message msg = gmre.getMessage();
        MessageAuthor author = gmre.getMessageAuthor();
        String message = msg.getReadableContent();
        if(author.isWebhook() || author.isBotUser() || author.isYourself() || message.isEmpty())
            return;
        if(BotHandler.isCommand(message)
                && BotHandler.hasCommand(message)) {
            TSDiscordPlugin.getPlugin().sendDebug("The message contains a valid command, skipping.");
            return;
        }
        TSDiscordPlugin.getPlugin().sendDebug("Message received!");
        boolean isMature = gmre.getChannel().getIdAsString().equals(plugin.getConfig().getString("channels.mature",""));
        if(!isMature && SwearUtil.checkSwearing(message)){
            String hook = ImportantConfig.getConfig().getString("webhooks."+gmre.getChannel().getIdAsString(),"");
            if(!hook.isEmpty()) {
                String censored = Censor.censor(message,true,false);
                if(censored != null)
                    BotHandler.sendWebhook(hook, censored, author.getDisplayName(), author.getAvatar().getUrl().toString(), msg);
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
        String censored = null;
        boolean allowCensor = plugin.getCPlugin() != null;
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
        StringBuilder sent = new StringBuilder();
        for(Player p : Bukkit.getOnlinePlayers()){
            if(allowCensor) {
                int mode = plugin.getCPlugin().getPlayerMode(p.getUniqueId().toString());
                switch (mode){
                    case 0:
                        censored = Censor.censor(message,false,false);
                        break;
                    case 2:
                        censored = Censor.censor(message,true,true);
                        break;
                    case 1:
                    default:
                        censored = Censor.censor(message,true,false);
                        break;
                }
            }
            String format = (censored == null) ? message : censored;
            p.sendMessage(String.format(c(chatFormat),author.getDisplayName(),format(ChatColor.stripColor(format))));
            if(sent.length() > 0)
                sent.append(", ");
            sent.append(p.getName());
        }
        plugin.sendDebug("Sent message to: "+sent);
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

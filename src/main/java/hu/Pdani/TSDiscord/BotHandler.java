package hu.Pdani.TSDiscord;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import com.mikemik44.censor.Censor;
import hu.Pdani.TSDiscord.cmds.OnlineCommand;
import hu.Pdani.TSDiscord.cmds.TPSCommand;
import hu.Pdani.TSDiscord.cmds.VersionCommand;
import hu.Pdani.TSDiscord.utils.CommandManager;
import hu.Pdani.TSDiscord.utils.ImportantConfig;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.activity.ActivityType;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageAttachment;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.webhook.Webhook;
import org.javacord.api.listener.channel.server.ServerChannelDeleteListener;
import org.javacord.api.util.event.ListenerManager;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static hu.Pdani.TSDiscord.TSDiscordPlugin.c;

public class BotHandler {
    private static final String DEF_AVATAR = "https://crafatar.com/avatars/%1$s?size=300&default=MHF_Steve&overlay&v=%2$d";
    private static DiscordApi bot;
    public static boolean shutdown = false;
    public static int task = -1;
    private static boolean started = false;
    private static final List<CompletableFuture> updates = new ArrayList<>();
    protected static void startup(DiscordApi bot){
        BotHandler.bot = bot;
        FileConfiguration config = TSDiscordPlugin.getPlugin().getConfig();
        String online = config.getString("message.presence.starting","Startup...");
        if(online != null && !online.isEmpty())
            BotHandler.bot.updateActivity(ActivityType.WATCHING,online);
        BotHandler.bot.addMessageCreateListener(TSDiscordPlugin.dl);
        BotHandler.bot.addMessageCreateListener(TSDiscordPlugin.cl);
        CommandManager.add(new TPSCommand());
        CommandManager.add(new OnlineCommand());
        CommandManager.add(new VersionCommand());
    }
    protected static void started(){
        if(bot == null)
            return;
        TSDiscordPlugin plugin = TSDiscordPlugin.getPlugin();
        boolean isList = plugin.getConfig().isList("channels.main");
        String channel = plugin.getConfig().getString("channels.main","");
        List<String> channels = new ArrayList<>();
        if(isList) {
            channels = plugin.getConfig().getStringList("channels.main");
        } else {
            if(!channel.isEmpty())
                channels.add(channel);
        }
        String startup = plugin.getConfig().getString("message.startup",":white_check_mark: Server is online.");
        if(startup != null && !startup.isEmpty()) {
            if (!channels.isEmpty()) {
                channels.forEach((c)-> plugin.getBot().getTextChannelById(c).ifPresent((tc) -> tc.sendMessage(startup)));
            } else {
                TSDiscordPlugin.getPlugin().sendDebug("No channel set in config");
            }
        }
        FileConfiguration config = TSDiscordPlugin.getPlugin().getConfig();
        String online = config.getString("message.presence.online","Server");
        if(online != null && !online.isEmpty())
            bot.updateActivity(ActivityType.WATCHING,online);
        else
            bot.updateActivity(ActivityType.WATCHING,"Server");
        startTopicUpdate();
    }
    protected static void end(){
        shutdown = true;
        if(bot == null)
            return;
        TSDiscordPlugin plugin = TSDiscordPlugin.getPlugin();
        boolean isList = plugin.getConfig().isList("channels.main");
        String channel = plugin.getConfig().getString("channels.main","");
        List<String> channels = new ArrayList<>();
        if(isList) {
            channels = plugin.getConfig().getStringList("channels.main");
        } else {
            if(!channel.isEmpty())
                channels.add(channel);
        }
        if(channels.isEmpty()) {
            stopTopicUpdate();
            return;
        }
        String shutdown = plugin.getConfig().getString("message.shutdown", ":octagonal_sign: Server shutdown.");
        channels.forEach((c)-> bot.getTextChannelById(c).ifPresent(tc -> tc.sendMessage(shutdown)));
        stopTopicUpdate();
    }
    protected static void startTopicUpdate(){
        if(started)
            return;
        started = true;
        changeTopic();
        startTask();
    }
    private static void startTask(){
        int topicUpdate = TSDiscordPlugin.getPlugin().getConfig().getInt("message.topic.update",13);
        if(TSDiscordPlugin.getPlugin().getConfig().getBoolean("message.topic.enabled",true)) {
            if (topicUpdate < 13)
                topicUpdate = 13;
        } else {
            topicUpdate = TSDiscordPlugin.getPlugin().getConfig().getInt("message.status.update",2);
            if(topicUpdate < 1)
                topicUpdate = 1;
        }
        long time = 20 * (topicUpdate*60L);
        task = TSDiscordPlugin.getPlugin().getServer().getScheduler().runTaskLaterAsynchronously(TSDiscordPlugin.getPlugin(),
                () -> {
                    if(shutdown)
                        return;
                    startTask();
                    changeTopic();
                }, time).getTaskId();
        if(task == -1){
            changeTopic();
        }
    }
    private static void changeTopic(){
        if(bot == null || shutdown)
            return;
        updates.removeIf(CompletableFuture::isDone);
        if(!updates.isEmpty()) {
            return;
        }
        int online = TSDiscordPlugin.getPlugin().getServer().getOnlinePlayers().size();
        int max = TSDiscordPlugin.getPlugin().getServer().getMaxPlayers();
        String timeFormat = TSDiscordPlugin.getPlugin().getConfig().getString("message.topic.time","dd/MM/yyyy HH:mm:ss");
        SimpleDateFormat formatter = new SimpleDateFormat(timeFormat);
        Date date = new Date();
        if(TSDiscordPlugin.getPlugin().getConfig().getBoolean("message.topic.enabled",true)) {
            boolean isList = TSDiscordPlugin.getPlugin().getConfig().isList("channels.main");
            String channel = TSDiscordPlugin.getPlugin().getConfig().getString("channels.main","");
            List<String> channels = new ArrayList<>();
            if(isList) {
                channels = TSDiscordPlugin.getPlugin().getConfig().getStringList("channels.main");
            } else {
                if(!channel.isEmpty())
                    channels.add(channel);
            }
            if (!channels.isEmpty()) {
                channels.forEach((c)-> {
                    ServerTextChannel tc = TSDiscordPlugin.getDiscordBot().getServerTextChannelById(c).orElse(null);
                    if (tc != null) {
                        String msg = TSDiscordPlugin.getPlugin().getConfig().getString("message.topic.online", "Online | Players: %current%/%max% | Updated: %time%");
                        updates.add(tc.updateTopic(msg.replace("%current%", String.valueOf(online)).replace("%max%", String.valueOf(max)).replace("%time%", formatter.format(date))));
                    }
                });
            }
        }
        boolean isList = TSDiscordPlugin.getPlugin().getConfig().isList("channels.status");
        String channel = TSDiscordPlugin.getPlugin().getConfig().getString("channels.status","");
        List<String> channels = new ArrayList<>();
        if(isList) {
            channels = TSDiscordPlugin.getPlugin().getConfig().getStringList("channels.status");
        } else {
            if(!channel.isEmpty())
                channels.add(channel);
        }
        if (!channels.isEmpty()) {
            channels.forEach(c -> {
                TextChannel tc = TSDiscordPlugin.getDiscordBot().getTextChannelById(c).orElse(null);
                if (tc != null) {
                    FileConfiguration important = ImportantConfig.getConfig();
                    String statusId = important.getString("statusId." + c, "");
                    EmbedBuilder embed = new EmbedBuilder();
                    String title = TSDiscordPlugin.getPlugin().getConfig().getString("message.status.title", "Current Server Status");
                    embed.setTitle(title);
                    String state = TSDiscordPlugin.getPlugin().getConfig().getString("message.status.state.title", "State");
                    String text = TSDiscordPlugin.getPlugin().getConfig().getString("message.status.state.online", ":white_check_mark: Server is online.");
                    embed.addField(state, text, false);
                    String playertext = TSDiscordPlugin.getPlugin().getConfig().getString("message.status.players", "Players online");
                    embed.addField(playertext, online + "/" + max, false);
                    embed.setFooter(formatter.format(date));
                    if (!statusId.isEmpty()) {
                        Message message = tc.getMessageById(statusId).join();
                        if (message == null) {
                            important.set("statusId." + c, null);
                            try {
                                ImportantConfig.saveConfig();
                            } catch (IOException e) {
                                TSDiscordPlugin.getPlugin().getLogger().warning("Unable to save 'donotmodify.yml' to disk: " + e.toString());
                            }
                            ImportantConfig.reloadConfig();
                            return;
                        }
                        message.edit(embed);
                    } else {
                        Message msg = tc.sendMessage(embed).join();
                        important.set("statusId." + c, msg.getIdAsString());
                        try {
                            ImportantConfig.saveConfig();
                        } catch (IOException e) {
                            TSDiscordPlugin.getPlugin().getLogger().warning("Unable to save 'donotmodify.yml' to disk: " + e.toString());
                        }
                        ImportantConfig.reloadConfig();
                    }
                }
            });
        }
    }
    protected static void stopTopicUpdate(){
        if(task != -1){
            TSDiscordPlugin.getPlugin().getServer().getScheduler().cancelTask(task);
        }
        for(CompletableFuture cf : updates){
            cf.cancel(true);
        }
        updates.clear();
        String timeFormat = TSDiscordPlugin.getPlugin().getConfig().getString("message.topic.time", "dd/MM/yyyy HH:mm:ss");
        SimpleDateFormat formatter = new SimpleDateFormat(timeFormat);
        Date date = new Date();
        if(TSDiscordPlugin.getPlugin().getConfig().getBoolean("message.topic.enabled",true)) {
            boolean isList = TSDiscordPlugin.getPlugin().getConfig().isList("channels.main");
            String channel = TSDiscordPlugin.getPlugin().getConfig().getString("channels.main","");
            List<String> channels = new ArrayList<>();
            if(isList) {
                channels = TSDiscordPlugin.getPlugin().getConfig().getStringList("channels.main");
            } else {
                if(!channel.isEmpty())
                    channels.add(channel);
            }
            if (!channels.isEmpty()) {
                channels.forEach((c)-> {
                    ServerTextChannel tc = TSDiscordPlugin.getDiscordBot().getServerTextChannelById(c).orElse(null);
                    if (tc != null) {
                        String msg = TSDiscordPlugin.getPlugin().getConfig().getString("message.topic.offline", "Offline | Updated: %time%");
                        tc.updateTopic(msg.replace("%time%", formatter.format(date)));
                    }
                });
            }
        }
        boolean isList = TSDiscordPlugin.getPlugin().getConfig().isList("channels.status");
        String channel = TSDiscordPlugin.getPlugin().getConfig().getString("channels.status","");
        List<String> channels = new ArrayList<>();
        if(isList) {
            channels = TSDiscordPlugin.getPlugin().getConfig().getStringList("channels.status");
        } else {
            if(!channel.isEmpty())
                channels.add(channel);
        }
        if (!channels.isEmpty()) {
            channels.forEach(c -> {
                TextChannel tc = TSDiscordPlugin.getDiscordBot().getTextChannelById(c).orElse(null);
                if (tc != null) {
                    EmbedBuilder embed = new EmbedBuilder();
                    String title = TSDiscordPlugin.getPlugin().getConfig().getString("message.status.title", "Current Server Status");
                    embed.setTitle(title);
                    String state = TSDiscordPlugin.getPlugin().getConfig().getString("message.status.state.title", "State");
                    String text = TSDiscordPlugin.getPlugin().getConfig().getString("message.status.state.offline", ":x: Server is offline.");
                    embed.addField(state, text, false);
                    embed.setFooter(formatter.format(date));
                    FileConfiguration important = ImportantConfig.getConfig();
                    String statusId = important.getString("statusId." + c, "");
                    if (!statusId.isEmpty()) {
                        Message message = tc.getMessageById(statusId).join();
                        if (message == null) {
                            important.set("statusId." + c, null);
                            try {
                                ImportantConfig.saveConfig();
                            } catch (IOException e) {
                                TSDiscordPlugin.getPlugin().getLogger().warning("Unable to save 'donotmodify.yml' to disk: " + e.toString());
                            }
                            ImportantConfig.reloadConfig();
                            return;
                        }
                        message.edit(embed);
                    } else {
                        Message msg = tc.sendMessage(embed).join();
                        important.set("statusId." + c, msg.getIdAsString());
                        try {
                            ImportantConfig.saveConfig();
                        } catch (IOException e) {
                            TSDiscordPlugin.getPlugin().getLogger().warning("Unable to save 'donotmodify.yml' to disk: " + e.toString());
                        }
                        ImportantConfig.reloadConfig();
                    }
                }
            });
        }
        bot.unsetActivity();
        bot.disconnect();
    }
    public static void deleteMsg(String channel, String id){
        if(bot == null || shutdown)
            return;
        if(channel.isEmpty() || id.isEmpty()) {
            return;
        }
        TextChannel tc = bot.getTextChannelById(channel).orElse(null);
        if(tc == null) {
            return;
        }
        tc.getMessageById(id).join().delete().join();
    }
    public static String sendMessage(String channel, String message){
        String id = null;
        if(!channel.isEmpty() && !message.isEmpty()) {
            TextChannel tc = TSDiscordPlugin.getDiscordBot().getTextChannelById(channel).orElse(null);
            if(tc != null) {
                id = tc.sendMessage(message).join().getIdAsString();
            }
        }
        return id;
    }
    @Deprecated
    public static String getServerChannel(){
        return TSDiscordPlugin.getPlugin().getConfig().getString("channelId",null);
    }
    public static boolean isCommand(String m){
        String prefix = TSDiscordPlugin.getPlugin().getConfig().getString("prefix",">");
        if(prefix == null || prefix.isEmpty())
            return false;
        return m.startsWith(prefix);
    }
    public static boolean hasCommand(String m){
        if(!isCommand(m) || bot == null)
            return false;
        String prefix = TSDiscordPlugin.getPlugin().getConfig().getString("prefix",">");
        String cmd = m.replaceFirst(prefix,"");
        if(cmd.contains(" "))
            cmd = cmd.split(" ")[0];
        for(String label : CommandManager.getList()){
            if(label.equalsIgnoreCase(cmd))
                return true;
        }
        return false;
    }

    private static final HashMap<String, ServerTextChannel> textChannelMap = new HashMap<>();
    private static final HashMap<String, ListenerManager> listenerMap = new HashMap<>();
    protected static void chat(Player user, String message){
        if(bot == null) {
            TSDiscordPlugin.getPlugin().sendDebug("Can't send chat: BOT is null !!!");
            return;
        }
        boolean modify = false;
        String player = ChatColor.stripColor(user.getDisplayName());
        String group = null;
        //String avatar = "https://minotar.net/helm/"+user.getName()+"/300.png?v="+(System.currentTimeMillis()/1000);
        String avatar = String.format(DEF_AVATAR,user.getUniqueId().toString(),(System.currentTimeMillis()/1000));
        if(TSDiscordPlugin.getVaultPerms() != null) {
            try {
                group = TSDiscordPlugin.getVaultPerms().getPrimaryGroup(user);
            } catch (Exception ignored){
            }
        }
        String userPrefix = "";
        String userSuffix = "";

        if(TSDiscordPlugin.getVaultChat() != null) {
            userPrefix = ChatColor.stripColor(c(TSDiscordPlugin.getVaultChat().getPlayerPrefix(null, user)));
            userSuffix = ChatColor.stripColor(c(TSDiscordPlugin.getVaultChat().getPlayerSuffix(null, user)));
        }
        if(group != null && TSDiscordPlugin.getVaultChat() != null) {
            //player += " [" + group + "]";
            String prefix = ChatColor.stripColor(c(TSDiscordPlugin.getVaultChat().getGroupPrefix(user.getWorld(),group)));
            String suffix = ChatColor.stripColor(c(TSDiscordPlugin.getVaultChat().getGroupSuffix(user.getWorld(),group)));
            player = (!userPrefix.isEmpty()) ? userPrefix + player : prefix + player;
            player += (!userSuffix.isEmpty()) ? userSuffix : suffix;
        } else {
            player = (!userPrefix.isEmpty()) ? userPrefix + player : "" + player;
            player += (!userSuffix.isEmpty()) ? userSuffix : "";
        }
        FileConfiguration important = ImportantConfig.getConfig();
        boolean isList = TSDiscordPlugin.getPlugin().getConfig().isList("channels.main");
        List<String> channels = new ArrayList<>();
        if(!isList) {
            String channel = TSDiscordPlugin.getPlugin().getConfig().getString("channels.main","");
            if(channel != null && !channel.isEmpty())
                channels.add(channel);
        } else {
            channels = TSDiscordPlugin.getPlugin().getConfig().getStringList("channels.main");
        }
        if(channels.isEmpty()) {
            TSDiscordPlugin.getPlugin().sendDebug("Can't send chat: No channel set in config");
            return;
        }
        for(String c : channels) {
            ServerTextChannel tc;
            if(textChannelMap.containsKey(c)) {
                tc = textChannelMap.get(c);
            } else {
                tc = bot.getServerTextChannelById(c).orElse(null);
                if(tc != null) {
                    List<String> finalChannels = channels;
                    ListenerManager<ServerChannelDeleteListener> mgr = tc.addServerChannelDeleteListener((event) -> {
                        if (event.getChannel().getIdAsString().equals(c)) {
                            textChannelMap.remove(c);
                            finalChannels.remove(c);
                            listenerMap.remove(c).remove();
                            if(isList)
                                TSDiscordPlugin.getPlugin().getConfig().set("channels.main",finalChannels);
                            else
                                TSDiscordPlugin.getPlugin().getConfig().set("channels.main","");
                        }
                    });
                    listenerMap.put(c,mgr);
                }
            }
            if (tc == null) {
                TSDiscordPlugin.getPlugin().sendDebug("Channel not found: "+c);
                continue;
            }
            String censored = null;
            if (TSDiscordPlugin.getCensorPlugin() != null)
                censored = Censor.censor(message, true, false);
            message = (censored == null) ? message : censored;
            if (TSDiscordPlugin.getCensorPlugin() != null)
                censored = Censor.censor(player, true, false);
            player = (censored == null) ? player : censored;
            String hookId = important.getString("webhooks." + tc.getId(), "");
            if (!hookId.isEmpty()) {
                sendWebhook(hookId, message, player, avatar);
            } else {
                Webhook webhook = tc.createWebhookBuilder().setName("TSDiscord").setAuditLogReason("Missing webhook for TSDiscord").create().join();
                hookId = webhook.getIdAsString();
                important.set("webhooks." + tc.getId(), hookId);
                modify = true;
                sendWebhook(hookId, message, player, avatar);
            }
        }
        TSDiscordPlugin.getPlugin().saveConfig();
        if(modify){
            try {
                ImportantConfig.saveConfig();
            } catch (IOException ex) {
                TSDiscordPlugin.getPlugin().getLogger().warning("Unable to save 'donotmodify.yml' to disk: "+ex.toString());
            }
            ImportantConfig.reloadConfig();
        }
    }
    public static boolean isEnabled(){
        return bot != null;
    }
    public static void sendWebhook(String webhookId, String message, String name, String avatarUrl, Message original){
        validateWebhook(webhookId).whenComplete((hook,error)->{
            if(error != null){
                TSDiscordPlugin.getPlugin().getLogger().severe(String.format("Error while trying to send webhook: %s",error));
                TSDiscordPlugin.getPlugin().sendDebug(String.format("Error while trying to send webhook: %s",error));
                return;
            }
            WebhookClient client = WebhookClient.withId(hook.getId(),hook.getToken().orElse(""));
            WebhookMessageBuilder builder = new WebhookMessageBuilder();
            builder.setUsername(name);
            if(avatarUrl != null)
                builder.setAvatarUrl(avatarUrl);
            else
                builder.setAvatarUrl(String.format(DEF_AVATAR,"MHF_ALEX",(System.currentTimeMillis()/1000)));
            builder.setContent(message);
            List<MessageAttachment> attachments = original != null ? original.getAttachments() : null;
            if(attachments != null && !attachments.isEmpty()){
                AtomicInteger failed = new AtomicInteger();
                for(MessageAttachment a : attachments){
                    a.downloadAsByteArray().whenComplete((bytes,throwable)->{
                        if(throwable != null) {
                            failed.getAndIncrement();
                        } else {
                            InputStream stream = new ByteArrayInputStream(bytes);
                            builder.addFile(a.getFileName(), stream);
                        }
                        if(builder.getFileAmount()+ failed.get() == attachments.size()){
                            if(client.isShutdown())
                                return;
                            client.send(builder.build());
                            client.close();
                            original.delete();
                        }
                    });
                }
            } else {
                if(original != null)
                    original.delete();
                client.send(builder.build());
                client.close();
            }
        });
    }
    public static void sendWebhook(String webhookId, String message, String name, String avatarUrl){
        sendWebhook(webhookId, message, name, avatarUrl,null);
    }
    private static CompletableFuture<Webhook> validateWebhook(String webhook){
        return TSDiscordPlugin.getDiscordBot().getWebhookById(Long.parseLong(webhook));
    }
}

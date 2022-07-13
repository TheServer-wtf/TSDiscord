package hu.Pdani.TSDiscord;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.send.AllowedMentions;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import hu.Pdani.TSDiscord.utils.CommandManager;
import hu.Pdani.TSDiscord.utils.DiscordChatEvent;
import hu.Pdani.TSDiscord.utils.ImportantConfig;
import hu.Pdani.TSDiscord.utils.ProgramCommand;
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
import org.javacord.api.interaction.SlashCommand;
import org.javacord.api.interaction.SlashCommandBuilder;
import org.javacord.api.interaction.SlashCommandOption;
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
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import static hu.Pdani.TSDiscord.TSDiscordPlugin.c;

public class BotHandler {
    private static final String DEF_AVATAR = "https://pdani.hu/mcauth/render.php?url=%1$s&size=300&helm";
    private static final String FALLBACK_AVATAR = "https://crafatar.com/avatars/%1$s?size=300&default=MHF_Steve&overlay";
    private static DiscordApi bot;
    public static boolean shutdown = false;
    public static int task = -1;
    private static boolean started = false;
    protected static void startup(DiscordApi bot){
        BotHandler.bot = bot;
        FileConfiguration config = TSDiscordPlugin.getPlugin().getConfig();
        String online = config.getString("message.presence.starting","Startup...");
        if(!online.isEmpty())
            BotHandler.bot.updateActivity(ActivityType.WATCHING,online);
        BotHandler.bot.addMessageCreateListener(TSDiscordPlugin.dl);
        BotHandler.bot.addSlashCommandCreateListener(TSDiscordPlugin.cl);
        List<SlashCommandBuilder> commands = new ArrayList<>();
        for(String label : CommandManager.getList()){
            ProgramCommand command = CommandManager.get(label);
            SlashCommandBuilder builder = new SlashCommandBuilder().setName(label).setDescription(command.getDescription());
            if(command.getOptions() != null && command.getOptions().size() > 0){
                for(SlashCommandOption option : command.getOptions())
                    builder.addOption(option);
            }
            commands.add(builder);
        }
        /*BotHandler.bot.getServers().forEach(server -> {
            SlashCommand command = SlashCommand.with("cmdName","description").createForServer(server).join();
        });*/
        BotHandler.bot.bulkOverwriteGlobalApplicationCommands(commands).join();
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
        String startup = plugin.getConfig().getString("message.startup",":white_check_mark: Server started.");
        if(!startup.isEmpty()) {
            if (!channels.isEmpty()) {
                channels.forEach((c)-> plugin.getBot().getTextChannelById(c).ifPresent((tc) -> tc.sendMessage(startup)));
            } else {
                TSDiscordPlugin.getPlugin().sendDebug("No channel set in config");
            }
        }
        FileConfiguration config = TSDiscordPlugin.getPlugin().getConfig();
        String online = config.getString("message.presence.online","Server");
        if(!online.isEmpty())
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
        //changeTopic();
        startTask();
    }
    private static void startTask(){
        int topicUpdate = TSDiscordPlugin.getPlugin().getConfig().getInt("message.status.update",2);
        if(topicUpdate < 1)
            topicUpdate = 1;
        long time = 20 * (topicUpdate*60L);
        task = TSDiscordPlugin.getPlugin().getServer().getScheduler().scheduleAsyncRepeatingTask(TSDiscordPlugin.getPlugin(),
                () -> {
                    if(shutdown)
                        return;
                    changeTopic();
                }, 0, time);
        if(task == -1){
            changeTopic();
        }
    }
    private static final HashMap<String,Integer> MsgFailAttempt = new HashMap<>();
    private static void changeTopic(){
        if(bot == null || shutdown)
            return;
        int online = TSDiscordPlugin.getPlugin().getServer().getOnlinePlayers().size();
        int max = TSDiscordPlugin.getPlugin().getServer().getMaxPlayers();
        String timeFormat = TSDiscordPlugin.getPlugin().getConfig().getString("message.time","dd/MM/yyyy HH:mm:ss");
        SimpleDateFormat formatter = new SimpleDateFormat(timeFormat);
        Date date = new Date();
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
                        Message message = null;
                        try{
                            message = tc.getMessageById(statusId).join();
                        } catch (CompletionException ignored){}
                        if (message == null) {
                            int attempt = MsgFailAttempt.getOrDefault(c,0);
                            TSDiscordPlugin.getPlugin().sendDebug("Status message with ID '"+statusId+"' not found (Attempt #"+attempt+1+")");
                            if(attempt+1 >= 5) {
                                TSDiscordPlugin.getPlugin().sendDebug("Removing message ID from storage (5 failed attempts)");
                                important.set("statusId." + c, null);
                                try {
                                    ImportantConfig.saveConfig();
                                } catch (IOException e) {
                                    TSDiscordPlugin.getPlugin().getLogger().warning("Unable to save 'donotmodify.yml' to disk: " + e.toString());
                                }
                                ImportantConfig.reloadConfig();
                                MsgFailAttempt.remove(c);
                            } else {
                                MsgFailAttempt.put(c,attempt+1);
                            }
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
        String timeFormat = TSDiscordPlugin.getPlugin().getConfig().getString("message.time", "dd/MM/yyyy HH:mm:ss");
        SimpleDateFormat formatter = new SimpleDateFormat(timeFormat);
        Date date = new Date();
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

    private static String escapeName(String name){
        name = name.replaceAll(Pattern.quote("_"),"\\_");
        return name;
    }

    private static final HashMap<String, ServerTextChannel> textChannelMap = new HashMap<>();
    private static final HashMap<String, ListenerManager> listenerMap = new HashMap<>();

    /**
     * This method must only be called if a player sends a message in the minecraft chat
     * @param user the Player
     * @param message the sent message
     */
    protected static void chat(Player user, String message){
        if(bot == null) {
            TSDiscordPlugin.getPlugin().sendDebug("Can't send chat: BOT is null !!!");
            return;
        }
        boolean modify = false;
        String player = user.getDisplayName();
        String group = null;
        String avatar = "";
        if(TSDiscordPlugin.getCentralPlugin() != null)
            avatar = String.format(DEF_AVATAR,TSDiscordPlugin.getCentralPlugin().getPlayerSkin(user.getUniqueId()));
        else
            avatar = String.format(FALLBACK_AVATAR,user.getUniqueId(),(System.currentTimeMillis()/1000));
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
            if(!channel.isEmpty())
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
            String hookId = important.getString("webhooks." + tc.getId(), "");
            if (hookId.isEmpty()) {
                Webhook webhook = tc.createWebhookBuilder().setName("TSDiscord").setAuditLogReason("Missing webhook for TSDiscord").setAvatar(bot.getYourself().getAvatar()).create().join();
                hookId = webhook.getIdAsString();
                important.set("webhooks." + tc.getId(), hookId);
                modify = true;
            }
            DiscordChatEvent event = new DiscordChatEvent(player, message, user);
            TSDiscordPlugin.getPlugin().getServer().getPluginManager().callEvent(event);
            if(!event.isCancelled() && !event.getMessage().isEmpty())
                sendWebhook(hookId, event.getMessage(), event.getUser(), avatar);
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
            WebhookClient client = WebhookClient.withId(hook.getId(),hook.asIncomingWebhook().get().getToken());
            WebhookMessageBuilder builder = new WebhookMessageBuilder();
            builder.setAllowedMentions(AllowedMentions.none());
            builder.setUsername(ChatColor.stripColor(escapeName(name)));
            if(avatarUrl != null)
                builder.setAvatarUrl(avatarUrl);
            else
                builder.setAvatarUrl(String.format(DEF_AVATAR,"MHF_ALEX",(System.currentTimeMillis()/1000)));
            String msg = ChatColor.stripColor(message);
            builder.setContent(msg);
            builder.resetEmbeds();
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
                            client.send(builder.build()).join();
                            client.close();
                        }
                    });
                }
            } else {
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

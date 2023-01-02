package hu.Pdani.TSDiscord;

import hu.Pdani.TSDiscord.cmds.OnlineCommand;
import hu.Pdani.TSDiscord.cmds.TPSCommand;
import hu.Pdani.TSDiscord.cmds.VersionCommand;
import hu.Pdani.TSDiscord.utils.CommandListener;
import hu.Pdani.TSDiscord.utils.CommandManager;
import hu.Pdani.TSDiscord.utils.ImportantConfig;
import hu.Pdani.TSDiscord.utils.ServiceListener;
import hu.Pdani.TSDiscord.utils.Updater;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.intent.Intent;
import org.jetbrains.annotations.NotNull;
import wtf.TheServer.TSCPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TSDiscordPlugin extends TSDPlugin {
    private static DiscordApi bot;
    protected static DiscordListener dl;
    protected static CommandListener cl;
    private static TSDiscordPlugin plugin;
    public static double tps = 0.00;
    private static Permission perms = null;
    private static Chat chat = null;
    public ArrayList<Player> debug = new ArrayList<>();
    private static TSCPlugin central;

    @Override
    public void onLoad() {
        CommandManager.add(new TPSCommand());
        CommandManager.add(new OnlineCommand());
        CommandManager.add(new VersionCommand());
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        addNewConfigOptions();
        super.onEnable(this);
        plugin = this;
        if(getServer().getPluginManager().isPluginEnabled("TSCentralPlugin")){
            central = (TSCPlugin) getServer().getPluginManager().getPlugin("TSCentralPlugin");
        }
        Updater updater = new Updater("TheServer-wtf/TSDiscord");
        startUpdateCheck(updater);
        ImportantConfig.loadConfig();
        cl = new CommandListener();
        String token = getConfig().getString("token","");
        if(!token.isEmpty()) {
            new DiscordApiBuilder().setToken(token).addIntents(Intent.MESSAGE_CONTENT).login().whenComplete((api, error) -> {
                if(error != null){
                    getLogger().severe(String.format("There was an error trying to login to the bot: %s",error));
                    return;
                }
                bot = api;
                BotHandler.startup(bot);
                startTps();
            });
            dl = new DiscordListener(this);
            getServer().getPluginManager().registerEvents(dl,this);
            getServer().getPluginManager().registerEvents(new ServiceListener(),this);
        } else {
            getLogger().warning("Bot token is empty!");
        }
        getCommand("tsdiscord").setExecutor(cl);
        Plugin vault = getServer().getPluginManager().getPlugin("Vault");
        if(vault != null){
            setupPermissions();
            setupChat();
        }
        messageLimit.clear();
        getLogger().info("The plugin is now enabled.");
    }

    public static void refreshVault(Class<?> service) {
        if(service == Chat.class) {
            Chat vaultChat = plugin.getServer().getServicesManager().load(Chat.class);
            if (vaultChat != chat) {
                plugin.getLogger().info("New Vault Chat implementation registered: " + (vaultChat == null ? "null" : vaultChat.getName()));
            }
            chat = vaultChat;
        } else if(service == Permission.class) {
            Permission vaultPerms = plugin.getServer().getServicesManager().load(Permission.class);
            if (vaultPerms != perms) {
                plugin.getLogger().info("New Vault Permission implementation registered: " + (vaultPerms == null ? "null" : vaultPerms.getName()));
            }
            perms = vaultPerms;
        }
    }
    private void setupPermissions() {
        RegisteredServiceProvider<Permission> rsp = getServer().getServicesManager().getRegistration(Permission.class);
        if(rsp == null)
            return;
        perms = rsp.getProvider();
    }
    private void setupChat() {
        RegisteredServiceProvider<Chat> rsp = getServer().getServicesManager().getRegistration(Chat.class);
        if(rsp == null)
            return;
        chat = rsp.getProvider();
    }
    private void addNewConfigOptions(){
        getConfig().options().copyDefaults(true);
        saveConfig();
    }


    public Permission getPerms(){
        return getVaultPerms();
    }
    public static Permission getVaultPerms(){
        return perms;
    }

    public Chat getChat(){
        return getVaultChat();
    }
    public static Chat getVaultChat(){
        return chat;
    }

    public TSCPlugin getCentral() {
        return getCentralPlugin();
    }
    public static TSCPlugin getCentralPlugin() {
        return central;
    }

    public static TSDiscordPlugin getPlugin(){
        return plugin;
    }

    public DiscordApi getBot(){
        return getDiscordBot();
    }
    public static DiscordApi getDiscordBot(){
        return bot;
    }

    @Override
    public void onDisable() {
        BotHandler.end();
        getLogger().info("The plugin is now disabled.");
    }

    private final HashMap<JavaPlugin,Long> messageLimit = new HashMap<>();

    @Override
    public void sendMessage(@NotNull JavaPlugin sender, @NotNull String message) throws IllegalAccessException {
        if(BotHandler.isEnabled()){
            long time = System.currentTimeMillis();
            if(messageLimit.containsKey(sender)){
                if(messageLimit.get(sender) > time - 250) {
                    throw new IllegalAccessException("Rate limit exceeded.");
                }
            }
            messageLimit.put(sender,time);
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
                return;
            }
            for(String c : channels) {
                ServerTextChannel tc = bot.getServerTextChannelById(c).orElse(null);
                if(tc == null)
                    continue;
                tc.sendMessage(message);
            }
        }
    }

    public static String c(String m){
        return ChatColor.translateAlternateColorCodes('&',m);
    }

    public void sendDebug(String msg){
        debug.forEach((target)->target.sendMessage("["+getLogger().getName()+"] DEBUG: "+msg));
        if(getConfig().getBoolean("consoleDebug",false))
            getLogger().info("DEBUG: "+msg);
    }

    private void startTps(){
        getServer().getScheduler().scheduleSyncRepeatingTask(this,
                new Runnable() {
                    long sec;
                    long currentSec;
                    int ticks;

                    @Override
                    public void run() {
                        sec = (System.currentTimeMillis() / 1000);
                        if (currentSec == sec) {
                            ticks++;
                        } else {
                            currentSec = sec;
                            tps = (tps == 0 ? ticks : ((tps + ticks) / 2));
                            ticks = 1;
                        }
                    }
                }, 0, 1);
        getServer().getScheduler().runTaskLater(this,
                BotHandler::started,1);
    }
    private void startUpdateCheck(Updater updater){
        int id = getServer().getScheduler().scheduleAsyncRepeatingTask(this,
                () -> {
                    getLogger().info("Checking for updates...");
                    if(updater.check(getDescription().getVersion())){
                        getLogger().warning("There is a new version ("+updater.getLatest()+") available! Download it at https://github.com/"+updater.getRepo());
                    } else {
                        getLogger().info("You are running the latest version.");
                    }
                },0,20L*3600);
        if(id == -1){
            getLogger().severe("Failed to check for new version: Unable to schedule task.");
        }
    }
}

package hu.Pdani.TSDiscord;

import com.mikemik44.main.Main;
import hu.Pdani.TSDiscord.utils.CommandListener;
import hu.Pdani.TSDiscord.utils.ImportantConfig;
import hu.Pdani.TSDiscord.utils.SwearUtil;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TSDiscordPlugin extends JavaPlugin {
    private static DiscordApi bot;
    protected static DiscordListener dl;
    protected static CommandListener cl;
    private static TSDiscordPlugin plugin;
    private static Main censorPlugin;
    public static double tps = 0.00;
    private static Permission perms = null;
    private static Chat chat = null;
    public ArrayList<Player> debug = new ArrayList<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        plugin = this;
        Updater updater = new Updater("TheServer-wtf/TSDiscord");
        startUpdateCheck(updater);
        ImportantConfig.loadConfig();
        cl = new CommandListener();
        String token = getConfig().getString("token","");
        String prefix = getConfig().getString("prefix",">");
        if((token != null && !token.isEmpty()) && (prefix != null && !prefix.isEmpty())) {
            new DiscordApiBuilder().setToken(token).login().whenComplete((api, error) -> {
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
        } else {
            getLogger().warning("Bot token or prefix is empty!");
        }
        getCommand("tsdiscord").setExecutor(cl);
        Plugin cp = getServer().getPluginManager().getPlugin("OptionalCensor-mikemik44");
        if(cp instanceof Main)
            censorPlugin = (Main) cp;
        Plugin vault = getServer().getPluginManager().getPlugin("Vault");
        if(vault != null){
            setupPermissions();
            setupChat();
        }
        getLogger().info("The plugin is now enabled.");
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

    public static TSDiscordPlugin getPlugin(){
        return plugin;
    }

    public static Main getCensorPlugin() {
        return censorPlugin;
    }
    public Main getCPlugin() {
        return getCensorPlugin();
    }

    protected DiscordApi getBot(){
        return getDiscordBot();
    }

    protected static DiscordApi getDiscordBot(){
        return bot;
    }

    @Override
    public void onDisable() {
        BotHandler.end();
        getLogger().info("The plugin is now disabled.");
    }

    public static String c(String m){
        return ChatColor.translateAlternateColorCodes('&',m);
    }

    public void sendDebug(String msg){
        debug.forEach((target)->{
            target.sendMessage(msg);
        });
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
                BotHandler::started,40);
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

    /**
     * @deprecated use {@link SwearUtil#checkSwearing} instead
     * @param data The message
     * @return true if the message contains swearing
     */
    @Deprecated
    protected boolean checkSwearing(String data){
        HashMap<String,ArrayList<String>> censor = new HashMap<>();
        censor.put("allow",censorPlugin.allow);
        censor.put("repl",censorPlugin.repl);
        censor.put("blocked",censorPlugin.blocked);
        censor.put("words",censorPlugin.words);
        return SwearUtil.checkSwearing(censor,data);
    }
}

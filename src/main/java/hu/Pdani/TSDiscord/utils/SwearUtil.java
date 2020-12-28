package hu.Pdani.TSDiscord.utils;

import com.mikemik44.censor.Censor;
import hu.Pdani.TSDiscord.TSDiscordPlugin;

public class SwearUtil {
    /**
     * Checks if the message contains swearing.
     * @param data The message to be checked
     * @return true if the message contains swearing
     */
    public static boolean checkSwearing(String data){
        if(TSDiscordPlugin.getCensorPlugin() != null){
            return !data.equals(Censor.censor(data, true, false));
        }
        return false;
    }
}

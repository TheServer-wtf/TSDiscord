package hu.Pdani.TSDiscord.utils;

import hu.Pdani.TSDiscord.TSDPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class CommandManager {
    private static final Map<String,ProgramCommand> commandMap = new HashMap<>();
    public static void add(ProgramCommand command){
        if(TSDPlugin.isStarted())
            throw new IllegalStateException("You can't add commands after the plugin started");
        if(command.getLabel() == null || command.getLabel().isEmpty())
            throw new IllegalArgumentException("Command label can not be null or empty");
        if(command.getDescription() == null || command.getDescription().isEmpty())
            throw new IllegalArgumentException("Command description can not be null or empty");
        if(commandMap.containsKey(command.getLabel().toLowerCase()))
            throw new IllegalArgumentException("Command already added");
        commandMap.put(command.getLabel().toLowerCase(),command);
    }
    public static ProgramCommand get(String label){
        return commandMap.get(label.toLowerCase());
    }
    public static Set<String> getList(){
        return commandMap.keySet();
    }
}

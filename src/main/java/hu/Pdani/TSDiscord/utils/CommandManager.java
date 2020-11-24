package hu.Pdani.TSDiscord.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class CommandManager {
    private static final Map<String,ProgramCommand> commandMap = new HashMap<>();
    public static void addCommand(ProgramCommand command){
        if(command.getLabel() == null || command.getLabel().isEmpty())
            throw new IllegalArgumentException("Command label can not be null or empty!");
        if(commandMap.containsKey(command.getLabel().toLowerCase()))
            return;
        commandMap.put(command.getLabel().toLowerCase(),command);
    }
    public static ProgramCommand getCommand(String label){
        return commandMap.get(label.toLowerCase());
    }
    public static Set<String> getCommandList(){
        return commandMap.keySet();
    }
}

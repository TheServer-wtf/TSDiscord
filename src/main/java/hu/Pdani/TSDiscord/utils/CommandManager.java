package hu.Pdani.TSDiscord.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

public class CommandManager {
    private static final Map<String,ProgramCommand> commandMap = new HashMap<>();
    public static void add(ProgramCommand command){
        if(command.getLabel() == null || command.getLabel().isEmpty())
            throw new IllegalArgumentException("Command label can not be null or empty");
        if(commandMap.containsKey(command.getLabel().toLowerCase()))
            return;
        commandMap.put(command.getLabel().toLowerCase(),command);
    }
    public static void remove(String label){
        if(commandMap.remove(label) == null)
            throw new NoSuchElementException("The specified command is not registered");
    }
    public static ProgramCommand get(String label){
        return commandMap.get(label.toLowerCase());
    }
    public static Set<String> getList(){
        return commandMap.keySet();
    }
}

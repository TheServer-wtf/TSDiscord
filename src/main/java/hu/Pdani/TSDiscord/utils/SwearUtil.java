package hu.Pdani.TSDiscord.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SwearUtil {
    /**
     * Checks if the message contains swearing. <br>
     * Mike, please make this in your plugin, so I don't feel bad about stealing your code !!!
     * @param data The message to be checked
     * @return true if the message contains swearing
     */
    public static boolean checkSwearing(HashMap<String,ArrayList<String>> censorPlugin, String data){
        if(censorPlugin == null || censorPlugin.isEmpty())
            return false;
        HashMap<String, String> desc = new HashMap<>();
        ArrayList<String> allow = censorPlugin.get("allow");
        int counter = 0;
        for (String s : allow) {
            Matcher m = Pattern.compile("(?i)" + s).matcher(data);
            while (m.find()) {
                String got = m.group();
                if (!desc.containsKey(got)) {
                    counter++;
                    String unique = "[{[(" + counter + ")]}]";
                    desc.put(got, unique);
                }
                data = m.replaceFirst(desc.get(got));
                m = Pattern.compile("(?i)" + s).matcher(data);
            }
        }
        ArrayList<String> repl = censorPlugin.get("repl");
        ArrayList<String> blocked = censorPlugin.get("blocked");
        ArrayList<String> words = censorPlugin.get("words");
        for (String s : blocked) {
            String dp = "";
            int modes = 0;
            for (int i = 0; i < s.length(); i++) {
                if (modes == 0) {
                    if (s.charAt(i) == '(') {
                        dp = dp + "(" + s.charAt(i);
                        modes++;
                    } else {
                        String sel = (new StringBuilder(String.valueOf(s.charAt(i)))).toString();
                        for (int j = 0; j < repl.size(); j += 2) {
                            String sane = repl.get(j);
                            String son = repl.get(j + 1);
                            sel = sel.replace(sane, son);
                        }
                        dp = dp + "(" + sel + ")+";
                        if (i != s.length() - 1)
                            dp = dp + "(-|_|\\.|,|(&|§)([0-9a-fm-o]|r)|\\||;|:|/|\\+|\\*|=|\\(|\\)|]|\\[|}|\\{|'|\"|<|>|\\\\|\\?|( ))*";
                    }
                } else {
                    if (s.charAt(i) == ')') {
                        modes--;
                    } else if (s.charAt(i) == '(') {
                        modes++;
                    }
                    dp = dp + s.charAt(i);
                    if (modes == 0) {
                        dp = dp + ")+";
                        if (i != s.length() - 1)
                            dp = dp + "(-|_|\\.|,|(&|§)([0-9a-fm-o]|r)|\\||;|:|/|\\+|\\*|=|\\(|\\)|]|\\[|}|\\{|'|\"|<|>|\\\\|\\?|( ))*";
                    }
                }
            }
            Matcher m = Pattern.compile("(?i)" + dp).matcher(data);
            if (m.find())
                return true;
        }
        for (String s : words) {
            String dp = "";
            int modes = 0;
            for (int i = 0; i < s.length(); i++) {
                if (modes == 0) {
                    if (s.charAt(i) == '(') {
                        dp = dp + "(" + s.charAt(i);
                        modes++;
                    } else {
                        String sel = String.valueOf(s.charAt(i));
                        for (int j = 0; j < repl.size(); j += 2) {
                            String sane = repl.get(j);
                            String son = repl.get(j + 1);
                            sel = sel.replace(sane, son);
                        }
                        dp = dp + "(" + sel + ")+";
                        if (i != s.length() - 1)
                            dp = dp + "(-|_|\\.|,|(&|§)([0-9a-fm-o]|r)|\\||;|:|/|\\+|\\*|=|\\(|\\)|]|\\[|}|\\{|'|\"|<|>|\\\\|\\?|( ))*";
                    }
                } else {
                    if (s.charAt(i) == ')') {
                        modes--;
                    } else if (s.charAt(i) == '(') {
                        modes++;
                    }
                    dp = dp + s.charAt(i);
                    if (modes == 0) {
                        dp = dp + ")+";
                        if (i != s.length() - 1)
                            dp = dp + "(-|_|\\.|,|(&|§)([0-9a-fm-o]|r)|\\||;|:|/|\\+|\\*|=|\\(|\\)|]|\\[|}|\\{|'|\"|<|>|\\\\|\\?|( ))*";
                    }
                }
            }
            Matcher m = Pattern.compile("(?i)" + dp).matcher(data);
            if(m.find())
                return true;
        }
        return false;
    }
}

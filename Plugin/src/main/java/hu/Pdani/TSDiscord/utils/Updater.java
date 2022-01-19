package hu.Pdani.TSDiscord.utils;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.yaml.snakeyaml.Yaml;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Updater {
    private Map<String,Object> obj;
    private String repo;
    private String latest;
    private long lastLoad = -1;
    private boolean useMaven = false;

    /**
     * Creates a new Updater object
     * @param repo Github repository (e.g. Owner/Plugin)
     */
    public Updater(String repo){
        if(repo == null || repo.isEmpty())
            return;
        this.repo = repo;
        loadConfig(false);
    }

    /**
     * Attempts to load the config from the github repository
     */
    private void loadConfig(boolean isMaven){
        try {
            URL url;
            if(!isMaven) {
                url = new URL("https://raw.githubusercontent.com/" + repo + "/master/src/plugin.yml");
                Scanner scan = new Scanner(url.openStream());
                StringBuilder sb = new StringBuilder();
                while(scan.hasNext()){
                    sb.append(scan.nextLine());
                    sb.append(System.getProperty("line.separator"));
                }
                Yaml yaml = new Yaml();
                obj = yaml.load(sb.toString());
            } else {
                url = new URL("https://raw.githubusercontent.com/" + repo + "/master/pom.xml");
                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                Document doc = dBuilder.parse(url.openStream());
                doc.getDocumentElement().normalize();
                NodeList nodelist = doc.getElementsByTagName("version");
                if(nodelist.getLength() > 0) {
                    String version = nodelist.item(0).getTextContent();
                    obj = new HashMap<>();
                    obj.put("version", version);
                }
            }
            lastLoad = System.currentTimeMillis()/1000;
            useMaven = isMaven;
        } catch (FileNotFoundException e2) {
            if(!isMaven) {
                loadConfig(true);
                return;
            }
            obj = null;
        } catch (IOException e) {
            e.printStackTrace();
            obj = null;
        } catch (ParserConfigurationException | SAXException e) {
            obj = null;
        }
    }

    /**
     * Get the current repo
     * @return the current repo
     */
    public String getRepo(){
        return repo;
    }

    /**
     * Get the latest version if one is available
     * @return the latest version if available, null otherwise
     */
    public String getLatest(){
        return latest;
    }

    /**
     * Check if there is a newer version available
     * @param current The current plugin version
     * @return true if there is a newer version
     */
    public boolean check(String current){
        long currentLoad = System.currentTimeMillis()/1000;
        if(currentLoad-lastLoad > 1800){
            obj = null;
            loadConfig(useMaven);
        }
        if(obj != null){
            String latest = String.valueOf(obj.get("version"));
            if(compareTo(current,latest) == -1){
                this.latest = latest;
                return true;
            }
        }
        return false;
    }

    /**
     * Compare two version numbers
     * @param current the version of the software
     * @param check version to compare to
     * @return 0 if either version is null, or they are the same, 1 if current is newer, and -1 if current is older
     */
    private int compareTo(String current, String check) {
        if(current == null || check == null)
            return 0;
        String[] thisParts = current.split("\\.");
        String[] thatParts = check.split("\\.");
        int length = Math.max(thisParts.length, thatParts.length);
        for(int i = 0; i < length; i++) {
            int thisPart = i < thisParts.length ?
                    Integer.parseInt(thisParts[i]) : 0;
            int thatPart = i < thatParts.length ?
                    Integer.parseInt(thatParts[i]) : 0;
            if(thisPart < thatPart)
                return -1;
            if(thisPart > thatPart)
                return 1;
        }
        return 0;
    }
}
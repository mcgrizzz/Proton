package me.drepic.proton;

import org.bstats.bukkit.Metrics;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLogger;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.JavaPluginLoader;

import java.io.File;
import java.util.logging.Logger;

public class Proton extends JavaPlugin implements Listener {

    private static ProtonManager manager;
    private static Logger logger;

    private static final int BSTATS_PLUGIN_ID = 9866;

    // Required for unit tests
    protected Proton(JavaPluginLoader loader, PluginDescriptionFile descriptionFile, File dataFolder, File file) {
        super(loader, descriptionFile, dataFolder, file);
    }

    @Override
    public void onEnable(){

        logger = new PluginLogger(this);

        FileConfiguration config = getConfig();
        config.options().copyDefaults(true);
        saveConfig();

        String name = config.getString("identification.clientName");
        String[] groups = config.getStringList("identification.groups").toArray(new String[0]);
        String host = config.getString("rabbitHost");
        String virtualHost = config.getString("rabbitVirtualHost");
        int port = config.getInt("rabbitPort");


        try {
            if(config.getBoolean("authorization.useAuthorization")){
                String user = config.getString("authorization.username");
                String password = config.getString("authorization.password");
                manager = new RabbitMQManager(name, groups, host, virtualHost, port, user, password);
            }else{
                manager = new RabbitMQManager(name, groups, host, virtualHost, port);
            }
        } catch (Exception e) {
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }

        boolean bStats = config.getBoolean("bStatsEnabled");

        if(bStats){
            Metrics metrics = new Metrics(this, BSTATS_PLUGIN_ID);
        }
    }

    @Override
    public void onDisable() {
        if (getProtonManager() != null) {
            getProtonManager().tearDown();
        }
    }

    /**
     * @return ProtonManager An instance of ProtonManager
     */
    public static ProtonManager getProtonManager(){
        return manager;
    }

    protected static void setPluginLogger(Logger newLogger){
        logger = newLogger;
    }

    protected static Logger pluginLogger(){
        return logger;
    }
}

package me.drepic.proton;

import org.bstats.bukkit.Metrics;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLogger;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.JavaPluginLoader;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
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

        String clientName = config.getString("identification.clientName");
        String[] groups = config.getStringList("identification.groups").toArray(new String[0]);

        boolean useRabbitMQ = config.getBoolean("rabbitMQ.useRabbitMQ");
        boolean useRedis = config.getBoolean("redis.useRedis");
        if(useRabbitMQ){
            try {
                setupRabbitMQ(clientName, groups);
            } catch (IOException | TimeoutException e) {
                e.printStackTrace();
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
        }else if(useRedis){
            setupRedis(clientName, groups);
        }else{
            logger.log(Level.INFO, "Neither RabbitMQ nor Redis is enabled. Shutting down.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        boolean bStats = config.getBoolean("bStatsEnabled");

        if(bStats){
            new Metrics(this, BSTATS_PLUGIN_ID);
        }
    }

    private void setupRabbitMQ(String clientName, String[] groups) throws IOException, TimeoutException {

        String host = getConfig().getString("rabbitMQ.host");
        String virtualHost = getConfig().getString("rabbitMQ.virtualHost");
        int port = getConfig().getInt("rabbitMQ.port");
        boolean useAuthorization = getConfig().getBoolean("rabbitMQ.authorization.useAuthorization");

        if(!useAuthorization){
            manager = new RabbitMQManager(clientName, groups, host, virtualHost, port);
        }else{
            String username = getConfig().getString("rabbitMQ.authorization.username");
            String password = getConfig().getString("rabbitMQ.authorization.password");
            manager = new RabbitMQManager(clientName, groups, host, virtualHost, port, username, password);
        }
    }

    private void setupRedis(String clientName, String[] groups){
        String host = getConfig().getString("redis.host");
        int port = getConfig().getInt("redis.port");
        boolean usePassword = getConfig().getBoolean("redis.usePassword");

        if(!usePassword){
            manager = new RedisManager(clientName, groups, host, port);
        }else{
            String password = getConfig().getString("redis.password");
            manager = new RedisManager(clientName, groups, host, port, password);
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

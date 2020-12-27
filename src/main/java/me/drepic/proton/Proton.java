package me.drepic.proton;

import me.drepic.proton.test.TestProton;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginLogger;
import org.bukkit.plugin.java.JavaPlugin;

public class Proton extends JavaPlugin implements Listener {

    private static ProtonManager manager;
    private FileConfiguration config;
    private static PluginLogger logger;

    private boolean test = false;

    @Override
    public void onEnable(){

        logger = new PluginLogger(this);

        config = getConfig();
        config.options().copyDefaults(true);
        saveConfig();

        String name = config.getString("clientName");
        String host = config.getString("rabbitHost");
        String virtualHost = config.getString("rabbitVirtualHost");
        int port = config.getInt("rabbitPort");
        try {
            if(config.getBoolean("authorization.useAuthorization")){
                String user = config.getString("authorization.username");
                String password = config.getString("authorization.password");
                manager = new ProtonManager(this, name, host, virtualHost, port, user, password); //Create manager
            }else{
                manager = new ProtonManager(this, name, host, virtualHost, port); //Create manager
            }

        } catch (Exception e) {
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if(test){
            getServer().getPluginManager().registerEvents(new TestProton(manager, this), this);
        }
    }

    protected static PluginLogger pluginLogger(){
        return logger;
    }

    /**
     * @return ProtonManager An instance of ProtonManager
     */
    public static ProtonManager getProtonManager(){
        return manager;
    }
}

package me.drepic.proton;

import org.bstats.bukkit.Metrics;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Proton extends JavaPlugin {

    private static ProtonManager manager;
    private static Logger logger;

    private static final int BSTATS_PLUGIN_ID = 9866;

    @Override
    public void onEnable() {

        logger = getLogger();

        FileConfiguration config = getConfig();
        config.options().copyDefaults(true);
        saveConfig();

        String clientName = config.getString("identification.clientName");
        if (clientName == null) {
            logger.log(Level.SEVERE, "The clientName must be set.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        String[] groups = config.getStringList("identification.groups").toArray(new String[0]);

        if (!verifyIdentification(clientName, groups)) {
            logger.log(Level.SEVERE, "The clientName/groups cannot contain `.` - Shutting down.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        boolean useRabbitMQ = config.getBoolean("rabbitMQ.useRabbitMQ");
        boolean useRedis = config.getBoolean("redis.useRedis");

        if (!useRedis && !useRabbitMQ) {
            logger.log(Level.SEVERE, "Neither RabbitMQ nor Redis is enabled. Shutting down.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (useRabbitMQ) {
            try {
                manager = setupRabbitMQ(clientName, groups);
                getServer().getServicesManager().register(ProtonManager.class, manager, this, ServicePriority.Normal);
            } catch (IOException | TimeoutException e) {
                logger.severe("Failed to setup RabbitMQ Connection");
                e.printStackTrace();
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
        } else {
            try {
                manager = setupRedis(clientName, groups);
                getServer().getServicesManager().register(ProtonManager.class, manager, this, ServicePriority.Normal);
            } catch (Exception e) {
                logger.severe("Failed to setup Redis Connection");
                e.printStackTrace();
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
        }

        boolean bStats = config.getBoolean("bStatsEnabled");

        if (bStats) {
            new Metrics(this, BSTATS_PLUGIN_ID);
        }

        boolean checkForUpdates = config.getBoolean("checkForUpdates");

        if (checkForUpdates) {
            try {
                new UpdateChecker(getDescription().getVersion());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private RabbitMQManager setupRabbitMQ(String clientName, String[] groups) throws IOException, TimeoutException {
        String host = getConfig().getString("rabbitMQ.host");
        String virtualHost = getConfig().getString("rabbitMQ.virtualHost");
        int port = getConfig().getInt("rabbitMQ.port");
        boolean useAuthorization = getConfig().getBoolean("rabbitMQ.authorization.useAuthorization");

        if (!useAuthorization) {
            return new RabbitMQManager(clientName, groups, host, virtualHost, port);
        } else {
            String username = getConfig().getString("rabbitMQ.authorization.username");
            String password = getConfig().getString("rabbitMQ.authorization.password");
            return new RabbitMQManager(clientName, groups, host, virtualHost, port, username, password);
        }
    }

    private RedisManager setupRedis(String clientName, String[] groups) {
        String host = getConfig().getString("redis.host");
        int port = getConfig().getInt("redis.port");
        boolean usePassword = getConfig().getBoolean("redis.usePassword");

        if (!usePassword) {
            return new RedisManager(clientName, groups, host, port);
        } else {
            String password = getConfig().getString("redis.password");
            return new RedisManager(clientName, groups, host, port, password);
        }
    }

    private boolean verifyIdentification(String clientName, String[] groups) {
        if (clientName.contains("\\.")) {
            return false;
        }

        for (String group : groups) {
            if (group.contains("\\.")) {
                return false;
            }
        }

        return true;
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
    public static ProtonManager getProtonManager() {
        return manager;
    }

    protected static void setPluginLogger(Logger newLogger) {
        logger = newLogger;
    }

    protected static Logger pluginLogger() {
        return logger;
    }
}

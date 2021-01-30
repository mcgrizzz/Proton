package me.drepic.proton.bukkit;

import me.drepic.proton.common.adapters.ConfigAdapter;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class BukkitConfigAdapter implements ConfigAdapter {

    JavaPlugin plugin;

    public BukkitConfigAdapter(JavaPlugin plugin){
        this.plugin = plugin;
    }

    @Override
    public void saveDefault() {
        this.plugin.getConfig().options().copyDefaults(true);
    }

    @Override
    public void loadConfig() {
        this.plugin.getConfig();
    }

    @Override
    public String getString(String path) {
        return this.plugin.getConfig().getString(path);
    }

    @Override
    public List<String> getStringList(String path) {
        return this.plugin.getConfig().getStringList(path);
    }

    @Override
    public boolean getBoolean(String path) {
        return this.plugin.getConfig().getBoolean(path);
    }

    @Override
    public int getInt(String path) {
        return this.plugin.getConfig().getInt(path);
    }
}

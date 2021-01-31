package me.drepic.proton.bukkit;

import me.drepic.proton.common.Proton;
import me.drepic.proton.common.ProtonBootstraper;
import me.drepic.proton.common.adapters.ConfigAdapter;
import me.drepic.proton.common.adapters.SchedulerAdapter;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public class BukkitBootstrap extends JavaPlugin implements ProtonBootstraper {

    private BukkitSchedulerAdapter schedulerAdapter;
    private BukkitConfigAdapter configAdapter;
    private Proton proton;

    @Override
    public void onEnable() {
        this.schedulerAdapter = new BukkitSchedulerAdapter(this);
        this.configAdapter = new BukkitConfigAdapter(this);
        this.proton = new Proton(this);
        this.proton.enable();
    }

    @Override
    public void onDisable() {
        this.proton.disable();
    }

    /*
        Bootstrap methods
     */

    @Override
    public void disable() {
        getServer().getPluginManager().disablePlugin(this);
    }


    @Override
    public Logger getPluginLogger() {
        return getLogger();
    }

    @Override
    public SchedulerAdapter getScheduler() {
        return this.schedulerAdapter;
    }

    @Override
    public ConfigAdapter getConfiguration() {
        return this.configAdapter;
    }

    @Override
    public String getVersion() {
        return getDescription().getVersion();
    }
}

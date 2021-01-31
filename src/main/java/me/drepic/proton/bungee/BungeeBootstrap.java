package me.drepic.proton.bungee;

import me.drepic.proton.common.Proton;
import me.drepic.proton.common.ProtonBootstraper;
import me.drepic.proton.common.adapters.ConfigAdapter;
import me.drepic.proton.common.adapters.SchedulerAdapter;
import net.md_5.bungee.api.plugin.Plugin;

import java.util.logging.Logger;

public class BungeeBootstrap extends Plugin implements ProtonBootstraper {

    private BungeeSchedulerAdapter schedulerAdapter;
    private BungeeConfigAdapter configAdapter;
    private Proton proton;

    @Override
    public void onEnable() {
        this.schedulerAdapter = new BungeeSchedulerAdapter(this);
        this.configAdapter = new BungeeConfigAdapter(this);
        this.proton = new Proton(this);
        this.proton.enable();
    }

    @Override
    public void onDisable() {
        this.proton.disable();
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

    @Override
    public void disable() {
        //TODO: No built in way to disable a bungee plugin
        getLogger().severe("Disabling...");
        this.onDisable();
    }
}

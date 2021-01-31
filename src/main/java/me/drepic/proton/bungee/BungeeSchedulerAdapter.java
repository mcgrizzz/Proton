package me.drepic.proton.bungee;

import me.drepic.proton.common.adapters.SchedulerAdapter;
import net.md_5.bungee.api.plugin.Plugin;

public class BungeeSchedulerAdapter implements SchedulerAdapter {

    private final Plugin plugin;

    public BungeeSchedulerAdapter(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void runTask(Runnable runnable) {
        //There is no synchronous runTask in bungee, it is generally threadsafe
        this.plugin.getProxy().getScheduler().runAsync(plugin, runnable);
    }

    @Override
    public void runTaskAsynchronously(Runnable runnable) {
        this.plugin.getProxy().getScheduler().runAsync(plugin, runnable);
    }
}

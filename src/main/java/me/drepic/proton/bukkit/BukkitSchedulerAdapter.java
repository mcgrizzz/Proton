package me.drepic.proton.bukkit;

import me.drepic.proton.common.adapters.SchedulerAdapter;
import org.bukkit.plugin.java.JavaPlugin;

public class BukkitSchedulerAdapter implements SchedulerAdapter {

    private JavaPlugin plugin;

    protected BukkitSchedulerAdapter(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void runTask(Runnable runnable) {
        this.plugin.getServer().getScheduler().runTask(this.plugin, runnable);
    }

    @Override
    public void runTaskAsynchronously(Runnable runnable) {
        this.plugin.getServer().getScheduler().runTaskAsynchronously(this.plugin, runnable);
    }
}

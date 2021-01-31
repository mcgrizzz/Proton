package me.drepic.proton.common;

import be.seeseemelk.mockbukkit.scheduler.BukkitSchedulerMock;
import me.drepic.proton.common.adapters.SchedulerAdapter;
import org.bukkit.plugin.java.JavaPlugin;

public class MockBukkitSchedulerAdapter implements SchedulerAdapter {

    BukkitSchedulerMock scheduler;
    JavaPlugin plugin;

    public MockBukkitSchedulerAdapter(BukkitSchedulerMock scheduler, JavaPlugin plugin){
        this.scheduler = scheduler;
        this.plugin = plugin;
    }

    @Override
    public void runTask(Runnable runnable) {
        this.scheduler.runTask(plugin, runnable);
    }

    @Override
    public void runTaskAsynchronously(Runnable runnable) {
        this.scheduler.runTaskAsynchronously(plugin, runnable);
    }
}

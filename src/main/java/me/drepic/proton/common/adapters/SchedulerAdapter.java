package me.drepic.proton.common.adapters;

public interface SchedulerAdapter {

    void runTask(Runnable runnable);
    void runTaskAsynchronously(Runnable runnable);
}

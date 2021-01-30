package me.drepic.proton.common;

import me.drepic.proton.common.adapters.ConfigAdapter;
import me.drepic.proton.common.adapters.SchedulerAdapter;

import java.util.logging.Logger;

public interface ProtonBootstrap {

    Logger getPluginLogger();

    SchedulerAdapter getScheduler();

    ConfigAdapter getConfiguration();

    String getVersion();

    void disable();

}

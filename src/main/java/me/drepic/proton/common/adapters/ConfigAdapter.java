package me.drepic.proton.common.adapters;

import java.util.List;

public interface ConfigAdapter {

    void saveDefault();
    void loadConfig();

    String getString(String path);
    List<String> getStringList(String path);

    boolean getBoolean(String path);
    int getInt(String path);

}

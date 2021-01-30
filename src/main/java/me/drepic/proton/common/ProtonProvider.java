package me.drepic.proton.common;

import reactor.util.annotation.NonNull;

public class ProtonProvider {

    private static ProtonManager instance;

    public static void register(ProtonManager manager){
        ProtonProvider.instance = manager;
    }

    public static void unregister(){
        ProtonProvider.instance = null;
    }

    @NonNull
    public static ProtonManager get(){
        if(ProtonProvider.instance == null){
            throw new IllegalStateException("ProtonManager is not loaded yet.");
        }
        return ProtonProvider.instance;
    }

}

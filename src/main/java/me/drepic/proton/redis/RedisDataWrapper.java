package me.drepic.proton.redis;

import java.util.UUID;

public class RedisDataWrapper {
    
    //class to store meta
    public String senderName;
    public UUID senderID;
    public byte[] data;

    public RedisDataWrapper(String senderName, UUID senderID, byte[] data) {
        this.senderName = senderName;
        this.senderID = senderID;
        this.data = data;
    }
}

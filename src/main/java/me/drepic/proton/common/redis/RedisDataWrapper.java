package me.drepic.proton.common.redis;

import java.util.Arrays;
import java.util.UUID;

public class RedisDataWrapper {

    //class to store metadata
    final public String senderName;
    final public UUID senderID;
    final public byte[] data;

    public RedisDataWrapper(String senderName, UUID senderID, byte[] data) {
        this.senderName = senderName;
        this.senderID = senderID;
        this.data = data;
    }

    @Override
    public String toString() {
        return "RedisDataWrapper{" +
                "senderName='" + senderName + '\'' +
                ", senderID=" + senderID +
                ", data=" + Arrays.toString(data) +
                '}';
    }
}

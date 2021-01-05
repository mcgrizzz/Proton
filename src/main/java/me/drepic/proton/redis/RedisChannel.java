package me.drepic.proton.redis;

import me.drepic.proton.message.MessageContext;

public class RedisChannel {

    //Class which defines a redis channel

    public MessageContext context;
    public String recipient;

    public RedisChannel(MessageContext context, String recipient) {
        this.context = context;
        this.recipient = recipient;
    }

    @Override
    public String toString(){

        return this.context.toContextString() + "." + this.recipient;
    }

    public static RedisChannel fromString(String s){
        String[] l = s.split("\\.");
        if(l.length != 3){
            throw new IllegalArgumentException();
        }
        MessageContext context = new MessageContext(l[0], l[1]);
        return new RedisChannel(context, l[2]);
    }
}

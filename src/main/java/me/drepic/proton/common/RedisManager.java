package me.drepic.proton.common;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import io.lettuce.core.pubsub.RedisPubSubListener;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.sync.RedisPubSubCommands;
import me.drepic.proton.common.adapters.SchedulerAdapter;
import me.drepic.proton.common.message.MessageContext;
import me.drepic.proton.common.redis.RedisChannel;
import me.drepic.proton.common.redis.RedisDataWrapper;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.logging.Logger;

public class RedisManager extends ProtonManager {

    String host;
    String password;
    int port;

    RedisClient client;
    RedisPubSubCommands<String, String> subCommands;
    RedisPubSubCommands<String, String> pubCommands;
    StatefulRedisPubSubConnection<String, String> subConnection;
    StatefulRedisPubSubConnection<String, String> pubConnection;

    protected RedisManager(Proton proton, String name, String[] groups, String host, int port, String password) {
        super(proton, name, groups);
        this.host = host;
        this.port = port;
        this.password = password;
        this.connect();
    }

    protected RedisManager(SchedulerAdapter scheduler, Logger logger, String name, String[] groups, String host, int port, String password){
        super(scheduler, logger, name, groups);
        this.host = host;
        this.port = port;
        this.password = password;
        this.connect();
    }

    protected RedisManager(Proton proton, String name, String[] groups, String host, int port) {
        this(proton, name, groups, host, port, "");
    }

    @Override
    protected void connect() {
        if (this.password.isEmpty()) {
            this.client = RedisClient.create(RedisURI.Builder.redis(this.host, this.port).build());
        } else {
            this.client = RedisClient.create(RedisURI.Builder.redis(this.host, this.port).withPassword(this.password.toCharArray()).build());
        }

        subConnection = client.connectPubSub();
        pubConnection = client.connectPubSub();

        RedisPubSubListener<String, String> listener = new RedisPubSubAdapter<String, String>() {
            @Override
            public void message(String channel, String data) {
                deliveryCallback(channel, data);
            }
        };

        subConnection.addListener(listener);

        subCommands = subConnection.sync();
        pubCommands = pubConnection.sync();

    }

    protected void deliveryCallback(String channelString, String json) {
        RedisChannel channel = RedisChannel.fromString(channelString);
        MessageContext context = channel.context;
        RedisDataWrapper wrapper = gson.fromJson(json, RedisDataWrapper.class);

        String jsonData = new String(wrapper.data, StandardCharsets.UTF_8);

        String recipient = channel.recipient;
        String senderName = wrapper.senderName;
        UUID senderID = wrapper.senderID;

        notifyHandlers(recipient, senderName, senderID, context, jsonData);
    }

    @Override
    protected void sendData(String sender, UUID senderID, String recipient, MessageContext context, byte[] data) {
        RedisChannel channel = new RedisChannel(context, recipient);
        RedisDataWrapper wrapper = new RedisDataWrapper(sender, senderID, data);
        String channelString = channel.toString();
        String wrapperString = gson.toJson(wrapper);

        pubCommands.publish(channelString, wrapperString);
    }

    @Override
    protected void broadcastData(String sender, UUID senderID, MessageContext context, byte[] data) {
        RedisChannel channel = new RedisChannel(context, "");
        RedisDataWrapper wrapper = new RedisDataWrapper(sender, senderID, data);
        String channelString = channel.toString();
        String wrapperString = gson.toJson(wrapper);

        pubCommands.publish(channelString, wrapperString);
    }

    @Override
    protected void bindRecipient(MessageContext context, String recipient) {
        RedisChannel channel = new RedisChannel(context, recipient);
        subCommands.subscribe(channel.toString());
    }

    @Override
    protected void bindBroadcast(MessageContext context) {
        RedisChannel channel = new RedisChannel(context, "");
        subCommands.subscribe(channel.toString());
    }

    @Override
    protected void tearDown() {
        pubConnection.close();
        subConnection.close();
        client.shutdown();
    }

}

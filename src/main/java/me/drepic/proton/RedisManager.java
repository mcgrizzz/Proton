package me.drepic.proton;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.pubsub.RedisPubSubListener;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.sync.RedisPubSubCommands;
import me.drepic.proton.message.MessageAttributes;
import me.drepic.proton.message.MessageContext;
import me.drepic.proton.redis.RedisChannel;
import me.drepic.proton.redis.RedisDataWrapper;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class RedisManager extends ProtonManager {

    String host;
    String password;
    int port;

    RedisClient client;
    RedisPubSubCommands<String, String> subCommands;
    RedisPubSubCommands<String, String> pubCommands;
    StatefulRedisPubSubConnection<String, String> subConnection;
    StatefulRedisPubSubConnection<String, String> pubConnection;

    protected RedisManager(String name, String[] groups, String host, int port, String password) {
        super(name, groups);
        this.host = host;
        this.port = port;
        this.password = password;
        connect();
    }

    protected RedisManager(String name, String[] groups, String host, int port) {
        this(name, groups, host, port, "");
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
        subConnection.addListener(new RedisPubSubListener<String, String>() {
            @Override
            public void message(String channel, String data) {
                deliveryCallback(channel, data);
            }

            @Override
            public void message(String s, String k1, String s2) {
            }

            @Override
            public void subscribed(String s, long l) {
            }

            @Override
            public void psubscribed(String s, long l) {
            }

            @Override
            public void unsubscribed(String s, long l) {
            }

            @Override
            public void punsubscribed(String s, long l) {
            }

        });

        subCommands = subConnection.sync();
        pubCommands = pubConnection.sync();

    }

    protected void deliveryCallback(String channelString, String json) {
        RedisChannel channel = RedisChannel.fromString(channelString);
        MessageContext context = channel.context;
        RedisDataWrapper wrapper = gson.fromJson(json, RedisDataWrapper.class);
        String recipient = channel.recipient;
        String senderName = wrapper.senderName;
        UUID senderID = wrapper.senderID;

        if (senderID.equals(this.id) && recipient.isEmpty()) { //Implies this was a broadcast from us. Ignore
            return;                                          //Conversely, we don't want to ignore messages we
        }                                                    //purposefully sent ourself

        if (!this.contextClassMap.containsKey(context)) { //Someone sent something to us that we're not listening for
            getLogger().warning("Received message that has no registered handlers.");
            return;
        }

        Class type = this.contextClassMap.get(context);
        String data = new String(wrapper.data, StandardCharsets.UTF_8);
        try {
            Object body = gson.fromJson(data, type);
            MessageAttributes messageAttributes = new MessageAttributes(context.getNamespace(), context.getSubject(), senderName, senderID);
            this.messageHandlers.get(context).forEach((biConsumer) -> {
                try {
                    biConsumer.accept(body, messageAttributes);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
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

package me.drepic.proton;

import com.rabbitmq.client.*;
import me.drepic.proton.message.MessageAttributes;
import me.drepic.proton.message.MessageContext;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

public class RabbitMQManager extends ProtonManager {

    private Connection connection;
    private Channel channel;
    private String queueName;

    private final String host;
    private final String virtualHost;
    private final int port;
    private final String username;
    private final String password;

    RabbitMQManager(String name, String[] groups, String host, String virtualHost, int port, String username, String password) throws IOException, TimeoutException {
        super(name, groups);
        this.host = host;
        this.virtualHost = virtualHost;
        this.port = port;
        this.username = username;
        this.password = password;
        this.connect();
    }

    RabbitMQManager(String name, String[] groups, String host, String virtualHost, int port) throws IOException, TimeoutException {
        this(name, groups, host, virtualHost, port, "", "");
    }

    @Override
    protected void connect() throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(host);
        factory.setPort(port);
        factory.setVirtualHost(virtualHost);

        if (!username.isEmpty()) {
            factory.setUsername(username);
            factory.setPassword(password);
        }

        connection = factory.newConnection();
        channel = connection.createChannel();
        channel.exchangeDeclare("proton.broadcast", "headers");
        channel.exchangeDeclare("proton.direct", "headers");

        queueName = channel.queueDeclare().getQueue();
        channel.basicConsume(queueName, true, this::deliverCallback, consumerTag -> { });

        getLogger().info(String.format("Connected as '%s' with id:%s\n", this.name, this.id.toString()));
    }

    protected void deliverCallback(String consumerTag, Delivery delivery) {
        String msg = new String(delivery.getBody(), StandardCharsets.UTF_8);
        String contextString = delivery.getProperties().getHeaders().get("messageContext").toString();
        MessageContext context;
        try {
            context = MessageContext.fromString(contextString);
        } catch (Exception e) {
            getLogger().warning(String.format("Unable to parse namespace and subject from given MessageContext: %s.", contextString));
            return;
        }

        String recipient = delivery.getProperties().getHeaders().get("recipient").toString();
        String senderName = delivery.getProperties().getHeaders().get("x-senderName").toString();
        UUID senderID = UUID.fromString(delivery.getProperties().getHeaders().get("x-senderID").toString());

        if (senderID.equals(this.id) && recipient.isEmpty()) { //Implies this was a broadcast from us. Ignore
            return;                                          //Conversely, we don't want to ignore messages we
        }                                                    //purposefully sent ourself

        if (!this.contextClassMap.containsKey(context)) { //Someone sent something to us that we're not listening for
            getLogger().warning("Received message that has no registered handlers.");
            return;
        }

        Class type = this.contextClassMap.get(context);
        try {
            Object body = gson.fromJson(msg, type);
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
    protected void bindRecipient(MessageContext context, String recipient) throws IOException {
        Map<String, Object> headers = new HashMap<>();

        headers.put("x-match", "all");
        headers.put("recipient", recipient);
        headers.put("messageContext", context.toContextString());

        channel.queueBind(queueName, "proton.direct", "", headers);
    }

    @Override
    protected void bindBroadcast(MessageContext context) throws IOException {
        Map<String, Object> headers = new HashMap<>();
        headers.put("x-match", "all");
        headers.put("messageContext", context.toContextString());

        channel.queueBind(queueName, "proton.broadcast", "", headers);
    }

    @Override
    protected void sendData(String sender, UUID senderID, String recipient, MessageContext context, byte[] data) throws IOException {
        Map<String, Object> headers = new HashMap<>();
        AMQP.BasicProperties.Builder propBuilder = new AMQP.BasicProperties.Builder();

        headers.put("x-senderName", sender);
        headers.put("x-senderID", senderID.toString());
        headers.put("recipient", recipient);
        headers.put("messageContext", context.toContextString());
        channel.basicPublish("proton.direct", "", propBuilder.headers(headers).build(), data);
    }

    @Override
    protected void broadcastData(String sender, UUID senderID, MessageContext context, byte[] data) throws IOException {

        Map<String, Object> headers = new HashMap<>();
        AMQP.BasicProperties.Builder propBuilder = new AMQP.BasicProperties.Builder();

        headers.put("x-senderName", sender);
        headers.put("x-senderID", senderID.toString());
        headers.put("recipient", "");
        headers.put("messageContext", context.toContextString());
        channel.basicPublish("proton.broadcast", "", propBuilder.headers(headers).build(), data);
    }

    @Override
    protected void tearDown() {
        try {
            channel.close();
            connection.close();
        } catch (Exception ignored) {
        }
    }
}

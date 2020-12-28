package me.drepic.proton;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.rabbitmq.client.*;
import me.drepic.proton.message.MessageAttributes;
import me.drepic.proton.message.MessageContext;
import me.drepic.proton.message.MessageHandler;
import me.drepic.proton.message.MessageSendException;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;


/**
 * Main entry point for using Proton
 * @author Drepic
 *
 */
public class ProtonManager {
    private String name; //What is this client's name (should be unique, if not multiple clients will receive the same message)
    private UUID id; //Guaranteed unique, used to prevent broadcast to self

    private Map<MessageContext, Class> contextClassMap;
    private Map<Class, Class> primitiveMapping;
    private Map<MessageContext, List<BiConsumer<Object, MessageAttributes>>> messageHandlers;

    private Connection connection;
    private Channel channel;
    private String queueName;

    private Gson gson;

    protected ProtonManager(String name, String host, String virtualHost, int port) throws Exception {
        this(name, host, virtualHost, port, "", "");
    }

    protected ProtonManager(String name, String host, String virtualHost, int port, String username, String password) throws Exception {
        this.name = name;
        this.id = UUID.randomUUID();
        this.contextClassMap = new HashMap<>();
        this.messageHandlers = new HashMap<>();
        this.primitiveMapping = ImmutableMap.<Class, Class>builder()
                .put(Byte.TYPE, Byte.class)
                .put(Short.TYPE, Short.class)
                .put(Integer.TYPE, Integer.class)
                .put(Long.TYPE, Long.class)
                .put(Float.TYPE, Float.class)
                .put(Double.TYPE, Double.class)
                .put(Boolean.TYPE, Boolean.class)
                .put(Character.TYPE, Character.class).build();

        gson = new Gson();

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(host);
        factory.setPort(port);
        factory.setVirtualHost(virtualHost);

        if(!username.isEmpty()){
            factory.setUsername(username);
            factory.setPassword(password);
        }

        connection = factory.newConnection();
        channel = connection.createChannel();
        channel.exchangeDeclare("proton.broadcast", "headers");
        channel.exchangeDeclare("proton.direct", "headers");

        queueName = channel.queueDeclare().getQueue();
        channel.basicConsume(queueName, true, this::deliverCallback, consumerTag -> {});

        Proton.pluginLogger().info(String.format("Connected as '%s' with id:%s\n", this.name, this.id.toString()));
    }

    private void deliverCallback(String consumerTag, Delivery delivery) throws IOException {
        String msg = new String(delivery.getBody(), "UTF-8");

        String contextString = delivery.getProperties().getHeaders().get("messageContext").toString();

        MessageContext context;
        try{
            context = MessageContext.fromString(contextString);
        }catch(Exception e){
            Proton.pluginLogger().warning(String.format("Unable to parse namespace and subject from given MessageContext: %s.", contextString));
            return;
        }

        String recipient = delivery.getProperties().getHeaders().get("recipient").toString();
        String senderName = delivery.getProperties().getHeaders().get("x-senderName").toString();
        UUID senderID = UUID.fromString(delivery.getProperties().getHeaders().get("x-senderID").toString());

        if(senderID.equals(this.id) && recipient.isEmpty()){ //Implies this was a broadcast from us. Ignore
            return;                                          //Conversely, we don't want to ignore messages we
        }                                                    //purposefully sent ourself

        if(!this.contextClassMap.containsKey(context)){ //Someone sent something to us that we're not listening for
            Proton.pluginLogger().warning("Received message that has no registered handlers.");
            return;
        }

        Class type = this.contextClassMap.get(context);
        try{
            Object body = gson.fromJson(msg, type);
            MessageAttributes messageAttributes = new MessageAttributes(context.getNamespace(), context.getSubject(), senderName, senderID);
            this.messageHandlers.get(context).forEach((biConsumer) -> {
                try{
                    biConsumer.accept(body, messageAttributes);
                }catch(Exception e){
                    e.printStackTrace();
                }
            });
        }catch(Exception e){
            e.printStackTrace();
            return;
        }
    }

    /**
     * Registers the MessageContext with the direct exchange
     */
    private void registerMessageContext(MessageContext context) throws IOException {
        Map<String, Object> headers = new HashMap<>();
        headers.put("x-match", "all"); //Match the recipient and the context
        headers.put("recipient", this.name);
        headers.put("messageContext", context.toContextString());

        channel.queueBind(queueName, "proton.direct", "", headers);

        headers = new HashMap<>();
        headers.put("x-match", "all");
        headers.put("messageContext", context.toContextString());

        channel.queueBind(queueName, "proton.broadcast", "", headers);
    }

    private void internalSend(String namespace, String subject, Object data, Optional<String> recipient) {
        MessageContext context = new MessageContext(namespace, subject);
        if(this.contextClassMap.containsKey(context) &&
                !data.getClass().equals(this.contextClassMap.get(context))){
            throw new IllegalArgumentException("Trying to send the wrong datatype for an already defined MessageContext");
        }

        try{
            byte[] bytes = new Gson().toJson(data).getBytes("UTF-8");

            Map<String, Object> headers = new HashMap<>();
            headers.put("x-senderName", this.name);
            headers.put("x-senderID", this.id.toString());
            headers.put("recipient", recipient.orElse(""));
            headers.put("messageContext", context.toContextString());

            AMQP.BasicProperties.Builder propBuilder = new AMQP.BasicProperties.Builder();
            if(!recipient.isPresent()){
                channel.basicPublish("proton.broadcast", "", propBuilder.headers(headers).build(), bytes);
            }else{
                channel.basicPublish("proton.direct", context.toContextString(), propBuilder.headers(headers).build(), bytes);
            }
        }catch(Exception e){
            throw new MessageSendException(e);
        }
    }


    /**
     * Send a message to a specific client with a given namespace and subject.
     * <br><b>NOTE: </b>The namespace and subject together are mapped to just one data type.
     * The namespace and subject together form a {@link MessageContext}
     * @param namespace This is the namespace of the message, usually you want to set this to your plugin name or organization
     * @param subject Set this to further define what your message is doing.
     * @param data This is the data you want to send. It must be JSON serializable
     * @param recipient The client name of the recipient of the message.
     * @throws IllegalArgumentException When trying to send the wrong datatype given a defined {@link MessageContext}
     * @throws IllegalArgumentException When trying to send to an empty or null recipient
     * @throws MessageSendException When unable to send the message
     */
    public void send(String namespace, String subject, Object data, String recipient) {
        if(recipient == null || recipient.isEmpty()){
            throw new IllegalArgumentException("Recipient cannot be null or empty");
        }
        this.internalSend(namespace, subject, data, Optional.of(recipient));
    }

    /**
     * Broadcast data to all clients
     * @see me.drepic.proton.ProtonManager#send
     * @param namespace This is the namespace of the message, usually you want to set this to your plugin name or organization
     * @param subject Set this to further define what your message is doing.
     * @param data This is the data you want to send. It must be JSON serializable
     * @throws IllegalArgumentException When trying to send the wrong datatype given a defined {@link MessageContext}
     * @throws MessageSendException When unable to send the message
     */
    public void broadcast(String namespace, String subject, Object data) {
       this.internalSend(namespace, subject, data, Optional.empty());
    }

    /**
     * Register your message handlers
     * @param object The class instance which holds your annotated MessageHandlers
     */
    public void registerMessageHandlers(Object object, Plugin plugin){
        Class<?> klass = object.getClass();
        for(final Method method : klass.getDeclaredMethods()){
            if(method.isAnnotationPresent(MessageHandler.class)){
                MessageHandler handlerAnnotation = method.getAnnotation(MessageHandler.class);
                String namespace = handlerAnnotation.namespace();
                String subject = handlerAnnotation.subject();
                MessageContext context = new MessageContext(namespace, subject);

                BiConsumer<Object, MessageAttributes> biConsumer;
                Class<?> parameterClass;
                if(method.getParameterCount() == 1){
                    parameterClass = method.getParameterTypes()[0];
                    biConsumer = (data, messageAttributes) -> {
                        try {
                            method.invoke(object, data);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    };
                }else if(method.getParameterCount() == 2){
                    parameterClass = method.getParameterTypes()[0];
                    biConsumer = (data, messageAttributes) -> {
                        try {
                            method.invoke(object, data, messageAttributes);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    };
                }else{
                    Proton.pluginLogger().warning("Annotated MessageHandler has incorrect number of parameters");
                    continue;
                }

                BiConsumer<Object, MessageAttributes> wrappedBiConsumer;
                if(!handlerAnnotation.async()){ //Wrap the BiConsumer so it can be synchronous
                    wrappedBiConsumer = (data, messageAttributes) -> {
                      Bukkit.getScheduler().runTask(plugin, () -> {
                        biConsumer.accept(data, messageAttributes);
                      });
                    };
                }else{
                    wrappedBiConsumer = (data, messageAttributes) -> { //prevent RabbitMQ thread stealing
                        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                            biConsumer.accept(data, messageAttributes);
                        });
                    };
                }

                if(this.primitiveMapping.containsKey(parameterClass)){
                    parameterClass = this.primitiveMapping.get(parameterClass);
                }

                if(!this.contextClassMap.containsKey(context)){
                    this.contextClassMap.put(context, parameterClass);
                    List<BiConsumer<Object, MessageAttributes>> biConsumers = new ArrayList<>();
                    biConsumers.add(wrappedBiConsumer);
                    this.messageHandlers.put(context, biConsumers);
                    try {
                        registerMessageContext(context);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }else{
                    if(!this.contextClassMap.get(context).equals(parameterClass)){
                        Proton.pluginLogger().warning("MessageContext already has defined data type");
                    }else{
                        this.messageHandlers.get(context).add(wrappedBiConsumer);
                    }
                }
            }
        }
    }

    protected void tearDown() {
        try {
            channel.close();
            connection.close();
        } catch (Exception e) {
        }
    }

    /**
     *
     * @return String This returns the client name of the server you're acting on. Names are used to direct messages
     */
    public String getClientName(){
        return this.name;
    }

    /**
     *
     * @return UUID This returns the unique ID for this client.
     */
    public UUID getClientID(){
        return this.id;
    }

}

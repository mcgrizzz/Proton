package me.drepic.proton.common;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import com.google.gson.Gson;
import me.drepic.proton.common.exception.MessageSendException;
import me.drepic.proton.common.exception.RegisterMessageHandlerException;
import me.drepic.proton.common.message.MessageAttributes;
import me.drepic.proton.common.message.MessageContext;
import me.drepic.proton.common.message.MessageHandler;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.logging.Logger;


/**
 * Main entry point for using Proton
 *
 * @author Drepic
 */
public abstract class ProtonManager {
    protected final String name; //The name of this client
    protected final String[] groups; //The groups the client belongs to
    protected final UUID id; //Guaranteed unique, used to prevent broadcast to self

    protected final ConcurrentHashMap<MessageContext, Class<?>> contextClassMap;
    protected final Map<Class<?>, Class<?>> primitiveMapping;
    protected final ListMultimap<MessageContext, BiConsumer<Object, MessageAttributes>> messageHandlers;

    protected final Gson gson;

    protected final Proton proton;

    protected ProtonManager(Proton proton, String name, String[] groups) {
        this.proton = proton;
        this.name = name;
        this.groups = groups;
        this.id = UUID.randomUUID();
        this.contextClassMap = new ConcurrentHashMap<>();
        this.messageHandlers = Multimaps.synchronizedListMultimap(ArrayListMultimap.create()); //Thread-safe update operations
        this.primitiveMapping = ImmutableMap.<Class<?>, Class<?>>builder()
                .put(Byte.TYPE, Byte.class)
                .put(Short.TYPE, Short.class)
                .put(Integer.TYPE, Integer.class)
                .put(Long.TYPE, Long.class)
                .put(Float.TYPE, Float.class)
                .put(Double.TYPE, Double.class)
                .put(Boolean.TYPE, Boolean.class)
                .put(Character.TYPE, Character.class).build();

        gson = new Gson();
    }

    /**
     * Registers the MessageContext with the direct exchange
     */
    private void registerMessageContext(MessageContext context) throws IOException {

        this.bindRecipient(context, this.name);

        for (String group : this.groups) { //For each group this client belongs to, bind
            if (group.equals(this.name)) continue; //prevent duplicate binding
            this.bindRecipient(context, group);
        }

        this.bindBroadcast(context);
    }

    /**
     * Send a message to a specific client with a given namespace and subject.
     * <br><b>NOTE: </b>The namespace and subject together are mapped to just one data type.
     * The namespace and subject together form a {@link MessageContext}
     *
     * @param namespace This is the namespace of the message, usually you want to set this to your plugin name or organization
     * @param subject   Set this to further define what your message is doing.
     * @param data      This is the data you want to send. It must be JSON serializable
     * @param recipient The client name or group for the recipient(s) of the message.
     * @throws IllegalArgumentException When trying to send the wrong datatype given a defined {@link MessageContext}
     * @throws IllegalArgumentException When trying to send to an empty or null recipient
     * @throws MessageSendException     When unable to send the message
     */
    public void send(String namespace, String subject, Object data, String recipient) {
        if (recipient == null || recipient.isEmpty()) {
            throw new IllegalArgumentException("Recipient cannot be null or empty");
        }

        if (namespace.contains("\\.") || subject.contains("\\.")) {
            throw new IllegalArgumentException("MessageContext cannot contain `.`");
        }

        if (recipient.contains("\\.")) {
            throw new IllegalArgumentException("Recipient cannot contain `.`");
        }

        MessageContext context = new MessageContext(namespace, subject);
        if (this.contextClassMap.containsKey(context) &&
                !data.getClass().equals(this.contextClassMap.get(context))) {
            throw new IllegalArgumentException("Trying to send the wrong datatype for an already defined MessageContext");
        }

        try {
            byte[] bytes = gson.toJson(data).getBytes(StandardCharsets.UTF_8);
            this.sendData(this.name, this.id, recipient, context, bytes);
        } catch (Exception e) {
            throw new MessageSendException(e);
        }
    }

    /**
     * Broadcast data to all clients
     *
     * @param namespace This is the namespace of the message, usually you want to set this to your plugin name or organization
     * @param subject   Set this to further define what your message is doing.
     * @param data      This is the data you want to send. It must be JSON serializable
     * @throws IllegalArgumentException When trying to send the wrong datatype given a defined {@link MessageContext}
     * @throws MessageSendException     When unable to send the message
     * @see ProtonManager#send
     */
    public void broadcast(String namespace, String subject, Object data) {
        if (namespace.contains("\\.") || subject.contains("\\.")) {
            throw new IllegalArgumentException("MessageContext cannot contain `.`");
        }
        MessageContext context = new MessageContext(namespace, subject);
        if (this.contextClassMap.containsKey(context) &&
                !data.getClass().equals(this.contextClassMap.get(context))) {
            throw new IllegalArgumentException("Trying to send the wrong datatype for an already defined MessageContext");
        }

        try {
            byte[] bytes = gson.toJson(data).getBytes(StandardCharsets.UTF_8);
            this.broadcastData(this.name, this.id, context, bytes);
        } catch (Exception e) {
            throw new MessageSendException(e);
        }
    }

    /**
     * Register your message handlers
     *
     * @param objects The class(s) which hold your annotated MessageHandlers
     * @throws RegisterMessageHandlerException When trying to register a MessageHandler using the same MessageContext but a different data type
     * @throws RegisterMessageHandlerException When trying to register a MessageHandler when the MessageContext contains the restricted `.` (period)
     * @throws RegisterMessageHandlerException When trying to register a MessageHandler with the incorrect amount of parameters
     */
    public void registerMessageHandlers(Plugin plugin, Object... objects) {
        for (Object obj : objects) {
            registerMessageHandler(plugin, obj);
        }
    }

    private void registerMessageHandler(Plugin plugin, Object object) {
        Class<?> klass = object.getClass();
        for (final Method method : klass.getDeclaredMethods()) {
            if (method.isAnnotationPresent(MessageHandler.class)) {
                MessageHandler handlerAnnotation = method.getAnnotation(MessageHandler.class);
                String namespace = handlerAnnotation.namespace();
                String subject = handlerAnnotation.subject();
                if (namespace.contains("\\.") || subject.contains("\\.")) {
                    throw new RegisterMessageHandlerException("MessageContext cannot contain `.`");
                }

                MessageContext context = new MessageContext(namespace, subject);

                BiConsumer<Object, MessageAttributes> biConsumer;
                Class<?> parameterClass;
                if (method.getParameterCount() == 1) {
                    parameterClass = method.getParameterTypes()[0];
                    biConsumer = (data, messageAttributes) -> {
                        try {
                            method.invoke(object, data);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    };
                } else if (method.getParameterCount() == 2) {
                    parameterClass = method.getParameterTypes()[0];
                    biConsumer = (data, messageAttributes) -> {
                        try {
                            method.invoke(object, data, messageAttributes);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    };
                } else {
                    throw new RegisterMessageHandlerException("Annotated MessageHandler has incorrect number of parameters");
                }

                BiConsumer<Object, MessageAttributes> wrappedBiConsumer;
                if (!handlerAnnotation.async()) { //Wrap the BiConsumer so it can be synchronous
                    wrappedBiConsumer = (data, messageAttributes) -> {
                        this.proton.getBootstrap().getScheduler().runTask(() -> {
                            biConsumer.accept(data, messageAttributes);
                        });
                    };
                } else {
                    wrappedBiConsumer = (data, messageAttributes) -> { //prevent RabbitMQ thread stealing
                        this.proton.getBootstrap().getScheduler().runTaskAsynchronously(() -> {
                            biConsumer.accept(data, messageAttributes);
                        });
                    };
                }

                if (this.primitiveMapping.containsKey(parameterClass)) {
                    parameterClass = this.primitiveMapping.get(parameterClass);
                }

                if (!this.contextClassMap.containsKey(context)) {
                    this.contextClassMap.put(context, parameterClass);
                    this.messageHandlers.put(context, wrappedBiConsumer);
                    try {
                        registerMessageContext(context);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    if (!this.contextClassMap.get(context).equals(parameterClass)) {
                        throw new RegisterMessageHandlerException("MessageContext already has defined data type");
                    } else {
                        this.messageHandlers.put(context, wrappedBiConsumer);
                    }
                }
            }
        }
    }

    protected void notifyHandlers(String recipient, String senderName, UUID senderID, MessageContext context, String jsonData){
        if (senderID.equals(this.id) && recipient.isEmpty()) { //Implies this was a broadcast from us. Ignore
            return;                                            //Conversely, we don't want to ignore messages we
        }                                                      //purposefully sent ourself

        if (!this.contextClassMap.containsKey(context)) { //Someone sent something to us that we're not listening for
            getLogger().warning("Received message that has no registered handlers.");
            return;
        }

        Class<?> type = this.contextClassMap.get(context);
        try {
            Object body = gson.fromJson(jsonData, type);
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

    protected Logger getLogger() {
        return this.proton.getBootstrap().getPluginLogger();
    }

    /**
     * @return String This returns the client name of the server you're acting on. Names are used to direct messages
     */
    public String getClientName() {
        return this.name;
    }

    /**
     * @return UUID This returns the unique ID for this client.
     */
    public UUID getClientID() {
        return this.id;
    }

    /**
     * @return String This returns the list of groups this client belongs to
     */
    public String[] getClientGroups() {
        return this.groups;
    }

    protected abstract void connect() throws Exception;

    protected abstract void sendData(String sender, UUID senderID, String recipient, MessageContext context, byte[] data) throws IOException;

    protected abstract void broadcastData(String sender, UUID senderID, MessageContext context, byte[] data) throws IOException;

    protected abstract void bindRecipient(MessageContext context, String recipient) throws IOException;

    protected abstract void bindBroadcast(MessageContext context) throws IOException;

    protected abstract void tearDown();
}

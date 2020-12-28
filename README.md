# Proton
Proton is a library which aims to give you a reliable and flexible solution to cross-server messaging. 
Other methods, such as Plugin Messaging, are either difficult and messy to implement or have restrictions such as the inability to send messages to a server with no active players. 
Proton is different in that it <b>1.</b> <i>creates a simple system for messaging between servers and</i> <b>2.</b> <i>is robust and versatile enough to where you can implement any messaging need you require.</i>
Proton is still being actively developed and tested. Your feedback is welcome.

### What is RabbitMQ?
RabbitMQ is a queue based messaging broker. In its simplest form, a producer sends a message to a queue, then a consumer consumes that message from the queue. However, RabbitMQ can and usually does support more complex networks than that. Proton acts as an interface between your plugin and the client API for RabbitMQ. RabbitMQ can be hosted easily on your own servers or by a cloud provider. [You can read more here.](https://www.rabbitmq.com/#getstarted)

# Setting up
1. [Download](https://www.rabbitmq.com/download.html) the latest version of RabbitMQ for your desired platform
2. If any of your servers are not on the same local network you must [create a new username and password](https://www.rabbitmq.com/access-control.html#user-management) and enable authorization in the Proton Config.
    * When setting user permissions, use the default virtual host: '/'
    * Alternatively, define a new virtual host and add the corresponding permissions to the user
    * It can be helpful [to enable the webUI for rabbitMQ](https://www.rabbitmq.com/management.html#getting-started) and use that to configure it.
3. In Proton's config.yml, configure the host and port accordingly. Give each configuration file a different clientName
4. In your plugin, make sure to list Proton as a dependency.

# Using Proton
### Getting a reference to ProtonManager
This is the class you will use to send messages and register MessageHandlers
```
    ProtonManager manager = Proton.getProtonManager();
```
### Sending a message
You can send a message either through ProtonManager#send() or ProtonManager#broadcast()
* namespace - A unique identifier to you and/or your plugin
* subject - A key that further narrows your data type
* data - The data you're sending. This can be any object that is Json serializable.
* recipient  - The client name you wish to send to
* **NOTE:** namespace and subject together form a **MessageContext** 
  and identify a specific datatype. You cannot send two different 
  datatypes with the same **MessageContext**.

```
    int ticks = 1500;
    manager.send("MyPlugin", "setTime", ticks, "survival2");
```
Additionally you can broadcast a message to all clients:
```
    int ticks = 1500;
    manager.broadcast("MyPlugin", "setTime", ticks);
```

### Receiving the corresponding message
1. Create a MessageHandler
    In your class create a method with the parameter datatype you're expecting. 
    Annotate it with the namespace and subject

    ```
        World world; //We have a reference to a world in this example
    
        @MessageHandler(namespace="MyPlugin", subject="setTime")
        public void onSetTime(int ticks){
            world.setTime(ticks);
        }
    ```

    If you want more information about the message you can attach a second parameter of type MessageAttributes
    ```
        @MessageHandler(namespace="MyPlugin", subject="setTime")
        public void onSetTime(int ticks, MessageAttributes attributes){
            ...
            String senderName = attributes.getSenderName();
            String senderID = attributes.getSenderID();
        }
    ```
    You can have multiple MessageHandlers for each MessageContext as long as the datatype of the first parameter is the same.
    
    MessageHandlers are run synchronously with the Server unless specified otherwise in the annotation.
    ```
        @MessageHandler(namespace="MyPlugin", subject="setTime", async=true)
    ```
2. Register your MessageHandlers with the ProtocolManager
    ```
        MyHandlers myHandlers = new MyHandlers(); //Any class that you define and contains MessageHandlers
        manager.registerMessageHandlers(myHandlers, plugin);
    ```
   

### TODO
- [x] Implement separate fanout and header exchanges to prevent client side filtering
- [ ] Implement acknowledgements/message confirmations when enabled in the config
- [ ] Client groups, instead of client names.

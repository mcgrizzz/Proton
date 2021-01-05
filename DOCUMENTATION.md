Proton – A cross-server messaging library utilizing RabbitMQ
=
Proton should work on any minecraft version as it does not rely on the spigot/bukkit API other than for its plugin declaration.
It has only been tested on 1.16.4.
## Setting-up

---
### Setting-up RabbitMQ

To start using Proton, you will first need to setup a RabbitMQ instance. 

You can do this in one of two ways: 

<b>1. Host it yourself</b>

Here is a great guide to setting it up yourself: https://www.rabbitmq.com/download.html

You will most likely need to setup a new username and password for your rabbitMQ instance: https://www.rabbitmq.com/access-control.html#user-management

This is for two reasons. One, RabbitMQ restricts the access of the default guest account only to localhost connections. Two, leaving the default username/password leaves you open to attacks.

<b>1. Find an online host</b>

Here are some hosts for RabbitMQ that you may find helpful. One of them actually has a free tier that may fit your needs.

1. [Stackhero](https://www.stackhero.io/en/services/RabbitMQ#pricing) - No free tiers but the lowest tier can probably support up to 200 servers
2. [CloudAMQP](https://www.cloudamqp.com/plans.html) - Expensive dedicated servers but there are cheaper and free shared servers.

*When choosing a host and plan consider your network's size and needs. 

### Setting-up the `config.yml` for Proton

Before integrating with Proton, you should configure your servers' Proton configs. 

Let's take a look at the config. 

```YAML
rabbitMQ:
  useRabbitMQ: true
  host: "localhost"
  virtualHost: '/'
  port: 5672
  authorization:
    useAuthorization: true
    username: guest
    password: guest
redis:
  useRedis: false
  host: "localhost"
  port: 6379
  usePassword: true
  password: "password"
identification:
  clientName: "client1"
  groups: []
bStatsEnabled: true
```

There's a lot of options here, but let's look at them in smaller pieces. 

The first section is for the configuration of RabbitMQ.

`useRabbitMQ` sets whether Proton will try to use RabbitMQ
`host` is the ip of your RabbitMQ instance.  
`port` is its port which you will probably not need to change.  
`virtualHost` is the virtual host which you can think of as an extension of the host. If you want to learn more about this [there's a great writeup here](https://www.rabbitmq.com/vhosts.html).    
`useAuthorization` sets whether Proton should try to authorize the connection  
`username` is the username for the connection  
`password` is the password for the connection
```YAML
rabbitMQ:
  useRabbitMQ: true
  host: "localhost"
  virtualHost: '/'
  port: 5672
  authorization:
    useAuthorization: true
    username: guest
    password: guest
```

Next we have the `Redis` section of the config.

`useRedis` sets whether Proton will try to use Redis. (NOTE: If both Rabbit and Redis are set to be used, it will default to RabbitMQ)  
`host` is the ip of your Redis instance.  
`port` is its port  
`usePassword` sets whether Proton will try to use a password for the connection    
`useAuthorization` is the password Proton will try to use.  
`username` is the username for the connection  
`password` is the password for the connection

```YAML
redis:
  useRedis: false
  host: "localhost"
  port: 6379
  usePassword: true
  password: "password"
```


Lastly, we have the `identification` section of the config. This is what Proton uses to know which servers are which. 

`clientName` should be a unique name to your server, no two servers should have the same name. This is important if you want to know later on which server a message came from. 

`groups` is a list of groups that the current server belongs to. For example, you can have a server that belongs to the group 'hub'. Therefore, when you send a message to the hub group, only servers in that group will receive that message. You can leave groups empty if you don't need it. 

```YAML
identification:
  clientName: "client1"
  groups: []
```

`bStatsEnabled` is a boolean value that enabled basic metric collection. You can disable this if you like. 

### Setting-up your plugin's `plugin.yml`

Just make sure that in your `plugin.yml`, you include the dependency for Proton.

```YAML
main: org.test.Test
name: TestPlugin
version: 1.0
depend:
  - Proton
```

## Proton Usage

---

### Getting an instance to `ProtonManager`

To start using Proton, you should first get an instance of `ProtonManager`. 

```Java
private ProtonManager protonManager;

@Override
public void onEnable() {
    this.protonManager = Proton.getProtonManager();
}
```

`ProtonManager` should not be `null` at this point. If it is, check your console for connection and configuration errors. 
 

### Sending your first message

Now that you have a reference to `ProtonManager`, you can send your first message.

```Java
String namespace = "namespace";
String subject = "subject";
String recipient = "recipient";
Object data = new Object();
protonManager.send(namespace, subject, data, recipient);
```

Lets break down these arguments. 

* `namespace` identifies your organization or plugin. This is what keeps your messages within the scope of your plugin or organization
* `subject` is used to identify the type of message you're sending, you can put any value, but we recommend something relevant and descriptive.
* `recipient` is used to define the client or group you wish to send to.
* `data` is the object that you wish to send. This can be any object or primitive. The only cavaet is that is that is must be Json serializable. Otherwise, you will receive exceptions.

<b>Important</b>: `namespace` and `subject` form what is called a `MessageContext`. Each `MessageContext` can only have one defined datatype. So if you define a namespace and subject, make sure you always send the same type of data through that context.

If you want to send a message to all clients that may be listening to a specific `MessageContext` you can use the broadcast method instead:

```Java
String namespace = "myPluginOrOrganization";
String subject = "subjectOfMyMessage";
Object data = new Object();
protonManager.broadcast(namespace, subject, data);
```


### Receiving a message

We tried to model the message receive system similarly to the Event system you probably use regularly. 

In any class or object, you can define a `MessageHandler`. A `MessageHandler` is an annotated method which receives data for a specific `MessageContext`. 

Let's take a look at the receiving end of the message sent above.

```Java
class MyClass {
    ...
    @MessageHandler(namespace="namespace", subject="subject")
    public void anyMethodName(Object data){
        //do something with the data received
    }
    ...
}
```

If you want to know the sender of the message, you can attach a second parameter to your `MessageHandler` method.

```Java
class MyClass {
    ...
    @MessageHandler(namespace="namespace", subject="subject")
    public void anyMethodName(Object data, MessageAttributes attr){
        String senderName = attr.getSenderName();
        UUID senderID = attr.getSenderID();
    }
    ...
}
```


The code within a `MessageHandler` is synchronous with Bukkit by default. This was a design decision to match the fact that most API calls must be synchronous. 
However, you can receive messages asynchronously if you wish by adding an optional attribute.

```Java
@MessageHandler(namespace="namespace", subject="subject", async=true)
```

The final step to actual receive any messages, is to register your `MessageHandler(s)`. Similarly to the Event API, you just register your class instance with the `ProtonManager`.
```Java
@Override
public void onEnable() {
    this.protonManager = Proton.getProtonManager();
    if(this.protonManager != null){
        this.protonManager.registerMessageHandlers(this, new MyClass());   
    }
}
```

If you want, you can register all of your handlers in one call.

```Java
this.protonManager.registerMessageHandlers(this, handler1, handler2, handler3...);
```

If you have any lingering questions, feel free to consult [the examples repo](https://github.com/mcgrizzz/ProtonExamples). You can also submit `question` issue on this repo. 






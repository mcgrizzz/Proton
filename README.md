### [Documentation](DOCUMENTATION.md) | [Examples](https://github.com/mcgrizzz/ProtonExamples) | [JavaDocs](https://mcgrizzz.github.io/ProtonDocs/) | [Spigot Page](https://www.spigotmc.org/resources/proton.87159/)
# Proton
Proton is a library which aims to give you a reliable and flexible solution to cross-server messaging. 
It uses your choice of Redis or RabbitMQ. Other methods, such as Plugin Messaging, are either difficult and messy to implement or have restrictions such as the inability to send messages to a server with no active players. 
Proton is different in that it <b>1.</b> <i>creates a simple system for messaging between servers and</i> <b>2.</b> <i>is robust and versatile enough to where you can implement any messaging need you require.</i>
Proton is still being actively developed and tested. Your feedback is welcome.

### What is RabbitMQ?
RabbitMQ is a queue based messaging broker. In its simplest form, a producer sends a message to a queue, then a consumer consumes that message from the queue. However, RabbitMQ can and usually does support more complex networks than that. Proton acts as an interface between your plugin and the client API for RabbitMQ. RabbitMQ can be hosted easily on your own servers or by a cloud provider. [You can read more here.](https://www.rabbitmq.com/#getstarted)

### What is Redis?
Redis a in-memory data structure store which is often used as a database or message broker. While it is not solely used for brokering messages, it is very fast and is a certainly a good choice for many situations. You can [You can read more here.](https://redis.io/)

### Using Proton as a dependency

1. Using Maven
```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```
```xml
<dependency>
    <groupId>com.github.mcgrizzz</groupId>
    <artifactId>Proton</artifactId>
    <version>v1.1.0</version>
    <scope>provided</scope>
</dependency>
```
*Proton should not be shaded into your plugin. It should be run as a plugin on your server.*
2. Or directly add the jar file to your project as a dependency. 

### [Documentation](DOCUMENTATION.md)

### [Documentation](DOCUMENTATION.md) | [Examples](https://github.com/mcgrizzz/ProtonExamples) | [JavaDocs](https://mcgrizzz.github.io/ProtonDocs/) | [Spigot Page](https://www.spigotmc.org/resources/proton.87159/)
# Proton
Proton is a library which aims to give you a reliable and flexible solution to cross-server messaging. 
Other methods, such as Plugin Messaging, are either difficult and messy to implement or have restrictions such as the inability to send messages to a server with no active players. 
Proton is different in that it <b>1.</b> <i>creates a simple system for messaging between servers and</i> <b>2.</b> <i>is robust and versatile enough to where you can implement any messaging need you require.</i>
Proton is still being actively developed and tested. Your feedback is welcome.

### What is RabbitMQ?
RabbitMQ is a queue based messaging broker. In its simplest form, a producer sends a message to a queue, then a consumer consumes that message from the queue. However, RabbitMQ can and usually does support more complex networks than that. Proton acts as an interface between your plugin and the client API for RabbitMQ. RabbitMQ can be hosted easily on your own servers or by a cloud provider. [You can read more here.](https://www.rabbitmq.com/#getstarted)

### [Documentation](DOCUMENTATION.md)


### TODO
- [x] Implement separate fanout and header exchanges to prevent client side filtering
- [ ] Implement acknowledgements/message confirmations when enabled in the config
- [ ] Client groups, instead of client names.

package me.drepic.proton.common;

import me.drepic.proton.common.message.MessageAttributes;
import me.drepic.proton.common.message.MessageHandler;
import org.bukkit.Bukkit;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.concurrent.TimeoutException;

class RabbitSendTests extends RabbitTests {

    @Test
    public void testSend__simpleAsyncValid() throws TimeoutException, InterruptedException {
        String myString = "testSend__simpleAsyncValid";
        Object client1Handler = new Object() {
            @MessageHandler(namespace = NAMESPACE, subject = SUBJECT, async = true)
            public void recv1(String recvStr) {
                waiter.assertEquals(recvStr, myString);
                waiter.assertFalse(Bukkit.isPrimaryThread());
                waiter.resume();
            }

            @MessageHandler(namespace = NAMESPACE, subject = SUBJECT, async = true)
            public void recv2(String recvStr, MessageAttributes messageAttributes) {
                waiter.assertEquals(recvStr, myString);
                waiter.assertFalse(Bukkit.isPrimaryThread());
                waiter.assertEquals(messageAttributes.getSenderName(), CLIENT_2_NAME);
                waiter.assertEquals(messageAttributes.getSenderID(), client2ProtonManager.getClientID());
                waiter.assertEquals(messageAttributes.getNamespace(), NAMESPACE);
                waiter.assertEquals(messageAttributes.getSubject(), SUBJECT);
                waiter.resume();
            }
        };
        client1ProtonManager.registerMessageHandlers(client1Handler);
        client2ProtonManager.send(NAMESPACE, SUBJECT, myString, CLIENT_1_NAME);
        waiter.await(1000, 2);
    }

    @Test
    public void testSend__simpleSyncValid() throws TimeoutException, InterruptedException {
        String myString = "testSend__simpleSyncValid";
        Object client1Handler = new Object() {
            @MessageHandler(namespace = NAMESPACE, subject = SUBJECT)
            public void recv1(String recvStr) {
                waiter.assertEquals(recvStr, myString);
                waiter.assertTrue(Bukkit.isPrimaryThread());
                waiter.resume();
            }

            @MessageHandler(namespace = NAMESPACE, subject = SUBJECT)
            public void recv2(String recvStr) {
                waiter.assertEquals(recvStr, myString);
                waiter.assertTrue(Bukkit.isPrimaryThread());
                waiter.resume();
            }
        };
        client1ProtonManager.registerMessageHandlers(client1Handler);
        client2ProtonManager.send(NAMESPACE, SUBJECT, myString, CLIENT_1_NAME);
        // Wait for sync threads to get added to Bukkit from RabbitMQ
        Thread.sleep(500);
        scheduler.performTicks(2);
        waiter.await(1000, 2);
    }

    @Test
    public void testSend__mixedAsync() throws TimeoutException, InterruptedException {
        String myString = "testSend__mixedAsync";
        Object client1Handler = new Object() {
            @MessageHandler(namespace = NAMESPACE, subject = SUBJECT)
            public void recv1(String recvStr) {
                waiter.assertEquals(recvStr, myString);
                waiter.assertTrue(Bukkit.isPrimaryThread());
                waiter.resume();
            }

            @MessageHandler(namespace = NAMESPACE, subject = SUBJECT, async = true)
            public void recv2(String recvStr) {
                waiter.assertEquals(recvStr, myString);
                waiter.assertFalse(Bukkit.isPrimaryThread());
                waiter.resume();
            }
        };
        client1ProtonManager.registerMessageHandlers(client1Handler);
        client2ProtonManager.send(NAMESPACE, SUBJECT, myString, CLIENT_1_NAME);
        // Wait for sync threads to get added to Bukkit from RabbitMQ
        Thread.sleep(500);
        scheduler.performTicks(2);
        waiter.await(1000, 2);
    }

    @Test
    public void testSend__onlyToSender() throws InterruptedException, TimeoutException {
        String myString = "testSend__onlyToSender";
        Object client1Handler = new Object() {
            @MessageHandler(namespace = NAMESPACE, subject = SUBJECT, async = true)
            public void recv(String recvStr) {
                waiter.assertEquals(recvStr, myString);
                waiter.resume();
            }
        };
        Object client2Handler = new Object() {
            @MessageHandler(namespace = NAMESPACE, subject = SUBJECT, async = true)
            public void recv(String recvStr) {
                waiter.fail("Client2 should not receive a message for client1");
            }
        };

        client1ProtonManager.registerMessageHandlers(client1Handler);
        client2ProtonManager.registerMessageHandlers(client2Handler);
        client1ProtonManager.send(NAMESPACE, SUBJECT, myString, CLIENT_1_NAME);
        Thread.sleep(500);
        waiter.await(1000, 1);
    }

    @Test
    public void testSend__noHandlerInRecipient() {
        String myString = "testSend__noHandlerInRecipient";
        client1ProtonManager.send(NAMESPACE, SUBJECT, myString, CLIENT_1_NAME);
    }

    @Test
    public void testSend__complicatedData() throws TimeoutException, InterruptedException {
        ComplicatedData data = new ComplicatedData(1, 5.4f, "This is complicated", Arrays.asList('a', 'b', 'c', 'd'));
        Object client1Handler = new Object() {
            @MessageHandler(namespace = NAMESPACE, subject = SUBJECT, async = true)
            public void recv(ComplicatedData recvData) {
                waiter.assertEquals(recvData, data);
                waiter.resume();
            }
        };

        client1ProtonManager.registerMessageHandlers(client1Handler);
        client2ProtonManager.send(NAMESPACE, SUBJECT, data, CLIENT_1_NAME);
        waiter.await(1000, 1);
    }

    @Test
    public void testSendGroup__commonGroup() throws TimeoutException, InterruptedException {
        String myString = "testSendGroup__commonGroup";
        Object client1Handler = new Object() {
            @MessageHandler(namespace = NAMESPACE, subject = SUBJECT, async = true)
            public void recv(String recvData) {
                waiter.assertEquals(recvData, myString);
                waiter.resume();
            }
        };

        Object client2Handler = new Object() {
            @MessageHandler(namespace = NAMESPACE, subject = SUBJECT, async = true)
            public void recv(String recvData) {
                waiter.assertEquals(recvData, myString);
                waiter.resume();
            }
        };

        client1ProtonManager.registerMessageHandlers(client1Handler);
        client2ProtonManager.registerMessageHandlers(client2Handler);
        client2ProtonManager.send(NAMESPACE, SUBJECT, myString, COMMON_GROUP);
        waiter.await(1000, 2);
    }

    @Test
    public void testSendGroup__differentGroup() throws TimeoutException, InterruptedException {
        String myString = "testSendGroup__differentGroup";
        Object client1Handler = new Object() {
            @MessageHandler(namespace = NAMESPACE, subject = SUBJECT, async = true)
            public void recv(String recvData) {
                waiter.assertEquals(recvData, myString);
                waiter.resume();
            }
        };

        Object client2Handler = new Object() {
            @MessageHandler(namespace = NAMESPACE, subject = SUBJECT, async = true)
            public void recv(String recvData) {
                waiter.fail("Client2 should not receive a message for client1's unique group");
            }
        };

        client1ProtonManager.registerMessageHandlers(client1Handler);
        client2ProtonManager.registerMessageHandlers(client2Handler);
        client2ProtonManager.send(NAMESPACE, SUBJECT, myString, CLIENT_1_GROUP);
        Thread.sleep(500);
        waiter.await(1000, 1);
    }

}

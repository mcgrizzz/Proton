package me.drepic.proton.common;

import me.drepic.proton.common.message.MessageAttributes;
import me.drepic.proton.common.message.MessageHandler;
import org.bukkit.Bukkit;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RabbitBroadcastTests extends RabbitTests {

    @Test
    public void testBroadcast__simpleAsyncValid() throws TimeoutException, InterruptedException {
        String myString = "testBroadcast__simpleAsyncValid";
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
        client1ProtonManager.registerMessageHandlers(proton, client1Handler);
        client2ProtonManager.broadcast(NAMESPACE, SUBJECT, myString);
        waiter.await(1000, 2);
    }

    @Test
    public void testBroadcast__simpleSyncValid() throws TimeoutException, InterruptedException {
        String myString = "testBroadcast__simpleSyncValid";
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
        client1ProtonManager.registerMessageHandlers(proton, client1Handler);
        client2ProtonManager.broadcast(NAMESPACE, SUBJECT, myString);
        // Wait for sync threads to get added to Bukkit from RabbitMQ
        Thread.sleep(500);
        scheduler.performTicks(2);
        waiter.await(1000, 2);
    }

    @Test
    public void testBroadcast__mixedAsync() throws TimeoutException, InterruptedException {
        String myString = "testBroadcast__mixedAsync";
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
        client1ProtonManager.registerMessageHandlers(proton, client1Handler);
        client2ProtonManager.broadcast(NAMESPACE, SUBJECT, myString);
        // Wait for sync threads to get added to Bukkit from RabbitMQ
        Thread.sleep(500);
        scheduler.performTicks(2);
        waiter.await(1000, 2);
    }

    @Test
    public void testBroadcast__multipleReceivers() throws Exception {
        String myString = "testBroadcast__multipleReceivers";
        String client3Name = "client3";
        String[] client3Groups = {};
        ProtonManager client3ProtonManager = createManager(client3Name, client3Groups, HOST, VIRTUAL_HOST, PORT, USERNAME, PASSWORD);
        Object sharedHandler = new Object() {
            @MessageHandler(namespace = NAMESPACE, subject = SUBJECT, async = true)
            public void recv1(String recvStr) {
                waiter.assertEquals(recvStr, myString);
                waiter.resume();
            }
        };
        client1ProtonManager.registerMessageHandlers(proton, sharedHandler);
        client2ProtonManager.registerMessageHandlers(proton, sharedHandler);
        client3ProtonManager.broadcast(NAMESPACE, SUBJECT, myString);
        waiter.await(1000, 2);
        client3ProtonManager.tearDown();
    }

    @Test
    public void testBroadcast__notToSender() throws InterruptedException, TimeoutException {
        String myString = "testBroadcast__notToSender";
        Object client1Handler = new Object() {
            @MessageHandler(namespace = NAMESPACE, subject = SUBJECT, async = true)
            public void recv(String recvStr) {
                waiter.fail("Broadcast should not be sent to sender");
            }
        };
        client1ProtonManager.registerMessageHandlers(proton, client1Handler);
        client1ProtonManager.broadcast(NAMESPACE, SUBJECT, myString);
        // This is hacky, but it is difficult to test for a lack of an async task
        Thread.sleep(500);
        waiter.resume();
        waiter.await(1000, 1);
    }

    @Test
    public void testBroadcast__notToWrongNamespace() throws InterruptedException, TimeoutException {
        String myString = "testBroadcast__notToWrongNamespace";
        Object client1Handler = new Object() {
            @MessageHandler(namespace = "wrong-namespace", subject = SUBJECT, async = true)
            public void recv(String recvStr) {
                waiter.fail("Broadcast should not be sent to incorrect namespace");
            }
        };
        client1ProtonManager.registerMessageHandlers(proton, client1Handler);
        client2ProtonManager.broadcast(NAMESPACE, SUBJECT, myString);
        // This is hacky, but it is difficult to test for a lack of an async task
        Thread.sleep(500);
        waiter.resume();
        waiter.await(1000, 1);
    }

    @Test
    public void testBroadcast__notToWrongSubject() throws InterruptedException, TimeoutException {
        String myString = "testBroadcast__notToWrongSubject";
        Object client1Handler = new Object() {
            @MessageHandler(namespace = NAMESPACE, subject = "wrong-subject", async = true)
            public void recv(String recvStr) {
                waiter.fail("Broadcast should not be sent to incorrect subject");
            }
        };
        client1ProtonManager.registerMessageHandlers(proton, client1Handler);
        client2ProtonManager.broadcast(NAMESPACE, SUBJECT, myString);
        // This is hacky, but it is difficult to test for a lack of an async task
        Thread.sleep(500);
        waiter.resume();
        waiter.await(1000, 1);
    }

    @Test
    public void testBroadcast__wrongTypeForRegisteredListener() {
        Object client1Handler = new Object() {
            @MessageHandler(namespace = NAMESPACE, subject = SUBJECT, async = true)
            public void recv(String recvStr) {
            }
        };
        client1ProtonManager.registerMessageHandlers(proton, client1Handler);
        assertThatThrownBy(() -> client1ProtonManager.broadcast(NAMESPACE, SUBJECT, 12L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Trying to send the wrong datatype for an already defined MessageContext");
    }

    @Test
    public void testBroadcast__wrongTypeForUnknownListener() throws InterruptedException {
        String myString = "testBroadcast__wrongTypeForUnknownListener";
        Object client1Handler = new Object() {
            @MessageHandler(namespace = NAMESPACE, subject = SUBJECT, async = true)
            public void recv(int recvInt) {
            }
        };
        client1ProtonManager.registerMessageHandlers(proton, client1Handler);
        client2ProtonManager.broadcast(NAMESPACE, SUBJECT, myString);
        // No exception thrown, but an error is logged
        Thread.sleep(500);
    }

    @Test
    public void testBroadcast__mismatchPrimitiveAndObject() throws TimeoutException, InterruptedException {
        Integer myInt = "testBroadcast__mismatchPrimitiveAndObject".hashCode();
        Object client1Handler = new Object() {
            @MessageHandler(namespace = NAMESPACE, subject = SUBJECT, async = true)
            public void recv(int recvInt) {
                waiter.assertEquals(recvInt, myInt);
                waiter.resume();
            }
        };
        client1ProtonManager.registerMessageHandlers(proton, client1Handler);
        client2ProtonManager.broadcast(NAMESPACE, SUBJECT, myInt);
        waiter.await(1000, 1);
    }

    @Test
    public void testBroadcast__bothPrimitiveAndObject() throws TimeoutException, InterruptedException {
        Integer myInt = "testBroadcast__bothPrimitiveAndObject".hashCode();
        Object client1Handler = new Object() {
            @MessageHandler(namespace = NAMESPACE, subject = SUBJECT, async = true)
            public void recv1(int recvInt) {
                waiter.assertEquals(recvInt, myInt);
                waiter.resume();
            }

            @MessageHandler(namespace = NAMESPACE, subject = SUBJECT, async = true)
            public void recv2(Integer recvInt) {
                waiter.assertEquals(recvInt, myInt);
                waiter.resume();
            }
        };
        client1ProtonManager.registerMessageHandlers(proton, client1Handler);
        client2ProtonManager.broadcast(NAMESPACE, SUBJECT, myInt);
        waiter.await(1000, 2);
    }

    @Test
    public void testBroadcast__complicatedData() throws TimeoutException, InterruptedException {
        ComplicatedData data = new ComplicatedData(1, 5.4f, "This is complicated", Arrays.asList('a', 'b', 'c', 'd'));
        Object client1Handler = new Object() {
            @MessageHandler(namespace = NAMESPACE, subject = SUBJECT, async = true)
            public void recv(ComplicatedData recvData) {
                waiter.assertEquals(recvData, data);
                waiter.resume();
            }
        };

        client1ProtonManager.registerMessageHandlers(proton, client1Handler);
        client2ProtonManager.broadcast(NAMESPACE, SUBJECT, data);
        waiter.await(1000, 1);
    }
}

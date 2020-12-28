package me.drepic.proton;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.scheduler.BukkitSchedulerMock;
import me.drepic.proton.exception.RegisterMessageHandlerException;
import me.drepic.proton.message.MessageAttributes;
import me.drepic.proton.message.MessageHandler;
import net.jodah.concurrentunit.Waiter;
import org.bukkit.Bukkit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests the ProtonManager. Requires that rabbitmq is running on localhost.
 *
 * Most tests use Waiter to ensure that the message handlers have been called.
 * Waiter.await(timeout, N) will throw an exception if N parties have not called resume.
 * Waiter assertions should be used in any code running in a different thread.
 *
 * Most tests use async = true as this does not require manually progressing through server ticks.
 */
class ProtonManagerTest {
    // ProtonManager Config
    static final String CLIENT_1_NAME = "client1";
    static final String CLIENT_2_NAME = "client2";
    static final String HOST = "localhost";
    static final String VIRTUAL_HOST = "/";
    static final int PORT = 5672;
    static final String USERNAME = "guest";
    static final String PASSWORD = "guest";

    static final String NAMESPACE = "test-namespace";
    static final String SUBJECT = "test-subject";

    Proton proton;
    ProtonManager client1ProtonManager;
    ProtonManager client2ProtonManager;
    BukkitSchedulerMock scheduler;
    Waiter waiter;

    @BeforeEach
    public void setUp() throws Exception {
        ServerMock server = MockBukkit.mock();
        scheduler = server.getScheduler();
        proton = MockBukkit.load(Proton.class);
        client1ProtonManager = Proton.getProtonManager();
        client2ProtonManager = new ProtonManager(CLIENT_2_NAME, HOST, VIRTUAL_HOST, PORT, USERNAME, PASSWORD);
        waiter = new Waiter();
    }

    @AfterEach
    public void tearDown() {
        client1ProtonManager.tearDown();
        client2ProtonManager.tearDown();
        MockBukkit.unmock();
    }

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
        client1ProtonManager.registerMessageHandlers(client1Handler, proton);
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
        client1ProtonManager.registerMessageHandlers(client1Handler, proton);
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
        client1ProtonManager.registerMessageHandlers(client1Handler, proton);
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
        ProtonManager client3ProtonManager = new ProtonManager(client3Name, HOST, VIRTUAL_HOST, PORT, USERNAME, PASSWORD);
        Object sharedHandler = new Object() {
            @MessageHandler(namespace = NAMESPACE, subject = SUBJECT, async = true)
            public void recv1(String recvStr) {
                waiter.assertEquals(recvStr, myString);
                waiter.resume();
            }
        };
        client1ProtonManager.registerMessageHandlers(sharedHandler, proton);
        client2ProtonManager.registerMessageHandlers(sharedHandler, proton);
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
        client1ProtonManager.registerMessageHandlers(client1Handler, proton);
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
        client1ProtonManager.registerMessageHandlers(client1Handler, proton);
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
        client1ProtonManager.registerMessageHandlers(client1Handler, proton);
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
        client1ProtonManager.registerMessageHandlers(client1Handler, proton);
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
        client1ProtonManager.registerMessageHandlers(client1Handler, proton);
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
        client1ProtonManager.registerMessageHandlers(client1Handler, proton);
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
        client1ProtonManager.registerMessageHandlers(client1Handler, proton);
        client2ProtonManager.broadcast(NAMESPACE, SUBJECT, myInt);
        waiter.await(1000, 2);
    }

    @Test
    public void testMultipleHandlers__mismatchDataType() {
        Object client1Handler = new Object() {
            @MessageHandler(namespace = NAMESPACE, subject = SUBJECT, async = true)
            public void recv(int recvInt) {
            }

            @MessageHandler(namespace = NAMESPACE, subject = SUBJECT, async = true)
            public void recv2(char recvChar) {
            }
        };

        assertThatThrownBy(() -> client1ProtonManager.registerMessageHandlers(client1Handler, proton))
                .isInstanceOf(RegisterMessageHandlerException.class)
                .hasMessage("MessageContext already has defined data type");

    }
}
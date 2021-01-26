package me.drepic.proton;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.scheduler.BukkitSchedulerMock;
import me.drepic.proton.exception.RegisterMessageHandlerException;
import me.drepic.proton.message.MessageHandler;
import net.jodah.concurrentunit.Waiter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests the ProtonManager. Requires that rabbitmq is running on localhost.
 * <p>
 * Most tests use Waiter to ensure that the message handlers have been called.
 * Waiter.await(timeout, N) will throw an exception if N parties have not called resume.
 * Waiter assertions should be used in any code running in a different thread.
 * <p>
 * Most tests use async = true as this does not require manually progressing through server ticks.
 */
class RabbitTests {
    // ProtonManager Config
    static final String COMMON_GROUP = "commonGroup";
    static final String CLIENT_1_NAME = "client1";
    static final String CLIENT_1_GROUP = "client1Group";
    static final String[] CLIENT_1_GROUPS = {COMMON_GROUP, CLIENT_1_GROUP};
    static final String CLIENT_2_NAME = "client2";
    static final String CLIENT_2_GROUP = "client2Group";
    static final String[] CLIENT_2_GROUPS = {COMMON_GROUP, CLIENT_2_GROUP};
    static final String HOST = System.getenv("RABBIT_HOST");
    static final String VIRTUAL_HOST = System.getenv("RABBIT_VHOST");
    static final int PORT = 5672;
    static final String USERNAME = System.getenv("RABBIT_USER");
    static final String PASSWORD = System.getenv("RABBIT_PASS");

    static final String NAMESPACE = "test-namespace";
    static final String SUBJECT = "test-subject";

    Proton proton;
    ProtonManager client1ProtonManager;
    ProtonManager client2ProtonManager;
    BukkitSchedulerMock scheduler;
    Waiter waiter;

    public ProtonManager createManager(String name, String[] groups, String host, String virtualHost, int port, String username, String password) throws Exception {
        return new RabbitMQManager(name, groups, host, virtualHost, port, username, password);
    }

    @BeforeEach
    public void setUp() throws Exception {
        ServerMock server = MockBukkit.mock();
        scheduler = server.getScheduler();
        Proton.setPluginLogger(Logger.getLogger("proton"));
        client1ProtonManager = createManager(CLIENT_1_NAME, CLIENT_1_GROUPS, HOST, VIRTUAL_HOST, PORT, USERNAME, PASSWORD);
        client2ProtonManager = createManager(CLIENT_2_NAME, CLIENT_2_GROUPS, HOST, VIRTUAL_HOST, PORT, USERNAME, PASSWORD);
        waiter = new Waiter();
    }

    @AfterEach
    public void tearDown() {
        client1ProtonManager.tearDown();
        client2ProtonManager.tearDown();
        MockBukkit.unmock();
    }

    static class ComplicatedData {

        public int a;
        public float b;
        public String c;
        public List<Character> d;

        public ComplicatedData(int a, float b, String c, List<Character> d) {
            this.a = a;
            this.b = b;
            this.c = c;
            this.d = d;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ComplicatedData that = (ComplicatedData) o;
            return a == that.a && Float.compare(that.b, b) == 0 && c.equals(that.c) && d.equals(that.d);
        }

        @Override
        public int hashCode() {
            return Objects.hash(a, b, c, d);
        }
    }

}
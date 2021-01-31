package me.drepic.proton.common;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.MockPlugin;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.scheduler.BukkitSchedulerMock;
import net.jodah.concurrentunit.Waiter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

public class RedisTests {

    static final String COMMON_GROUP = "commonGroup";
    static final String CLIENT_1_NAME = "client1";
    static final String CLIENT_1_GROUP = "client1Group";
    static final String[] CLIENT_1_GROUPS = {COMMON_GROUP, CLIENT_1_GROUP};
    static final String CLIENT_2_NAME = "client2";
    static final String CLIENT_2_GROUP = "client2Group";
    static final String[] CLIENT_2_GROUPS = {COMMON_GROUP, CLIENT_2_GROUP};

    static final String HOST = System.getenv("REDIS_HOST");
    static final int PORT = 9949;
    static final String PASSWORD = System.getenv("REDIS_PASSWORD");

    static final String NAMESPACE = "test-namespace";
    static final String SUBJECT = "test-subject";

    MockPlugin plugin;
    ProtonManager client1ProtonManager;
    ProtonManager client2ProtonManager;

    Logger logger;
    BukkitSchedulerMock scheduler;
    MockBukkitSchedulerAdapter schedulerAdapter;
    Waiter waiter;

    public ProtonManager createManager(String name, String[] groups) throws Exception {
        return new RedisManager(schedulerAdapter, logger, name, groups, HOST, PORT, PASSWORD);
    }

    @BeforeEach
    public void setUp() throws Exception {
        ServerMock server = MockBukkit.mock();
        plugin = MockBukkit.createMockPlugin();

        this.logger = Logger.getLogger("proton");
        scheduler = server.getScheduler();
        schedulerAdapter = new MockBukkitSchedulerAdapter(scheduler, plugin);

        client1ProtonManager = createManager(CLIENT_1_NAME, CLIENT_1_GROUPS);
        client2ProtonManager = createManager(CLIENT_2_NAME, CLIENT_2_GROUPS);
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

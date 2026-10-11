package za.co.lightshed.monitor;

import io.javalin.testtools.JavalinTest;
import org.junit.jupiter.api.Test;
import za.co.lightshed.common.Alert;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.*;

class MonitorTest {
    @Test void panicsOnceWhenServiceGoesDownAndInfoWhenItRecovers() {
        List<Alert> sent = new ArrayList<>();
        boolean[] stageUp = {true};
        Map<String, BooleanSupplier> checks = Map.of("stage", () -> stageUp[0]);
        var monitor = new HealthMonitor(checks, sent::add);

        monitor.runOnce();
        assertTrue(sent.isEmpty());             // healthy: no noise

        stageUp[0] = false;
        monitor.runOnce();
        monitor.runOnce();                      // still down: no second alert
        assertEquals(1, sent.size());
        assertEquals("PANIC", sent.get(0).severity());

        stageUp[0] = true;
        monitor.runOnce();
        assertEquals(2, sent.size());
        assertEquals("INFO", sent.get(1).severity());
    }

    @Test void serviceDownAtFirstCheckPanics() {
        List<Alert> sent = new ArrayList<>();
        new HealthMonitor(Map.of("schedule", () -> false), sent::add).runOnce();
        assertEquals(1, sent.size());
    }

    @Test void throwingCheckCountsAsDown() {
        List<Alert> sent = new ArrayList<>();
        new HealthMonitor(Map.of("x", () -> { throw new RuntimeException("boom"); }), sent::add).runOnce();
        assertEquals("PANIC", sent.get(0).severity());
    }

    @Test void storeKeepsOnlyNewestAlerts() {
        var store = new AlertStore(2);
        store.add(Alert.of("a", "INFO", "1"));
        store.add(Alert.of("a", "INFO", "2"));
        store.add(Alert.of("a", "INFO", "3"));
        assertEquals(List.of("3", "2"), store.recent().stream().map(Alert::message).toList());
    }

    @Test void alertsEndpointListsAlerts() {
        var store = new AlertStore(10);
        store.add(Alert.of("schedule", "PANIC", "Stage service unreachable"));
        JavalinTest.test(Main.createApp(store), (server, client) -> {
            assertTrue(client.get("/alerts").body().string().contains("Stage service unreachable"));
            assertEquals(200, client.get("/health").code());
        });
    }
}

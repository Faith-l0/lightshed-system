package za.co.lightshed.common;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class CommonTest {
    @Test void throttleDropsRepeatsInsideWindow() {
        List<Alert> sent = new ArrayList<>();
        AtomicLong now = new AtomicLong(0);
        var throttled = new ThrottledAlertPublisher(sent::add, 1000, now::get);
        throttled.publish(Alert.of("schedule", "PANIC", "stage down"));
        throttled.publish(Alert.of("schedule", "PANIC", "stage down"));
        assertEquals(1, sent.size());
        throttled.publish(Alert.of("schedule", "PANIC", "placename down"));
        assertEquals(2, sent.size());
        now.set(1500);
        throttled.publish(Alert.of("schedule", "PANIC", "stage down"));
        assertEquals(3, sent.size());
    }

    @Test void alertTravelsThroughTheQueue() throws Exception {
        String url = "vm://commontest?broker.persistent=false";
        List<Alert> received = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);
        try (var consumer = new JmsAlertConsumer(url, a -> { received.add(a); latch.countDown(); })) {
            try (var publisher = new JmsAlertPublisher(url)) {
                publisher.publish(Alert.of("schedule", "PANIC", "Stage service unreachable"));
            }
            assertTrue(latch.await(5, TimeUnit.SECONDS), "alert was not received");
        }
        assertEquals("schedule", received.get(0).source());
        assertEquals("PANIC", received.get(0).severity());
        assertEquals("Stage service unreachable", received.get(0).message());
    }

    @Test void publishingWithNoBrokerDoesNotThrow() {
        try (var publisher = new JmsAlertPublisher("tcp://localhost:1")) {
            assertDoesNotThrow(() -> publisher.publish(Alert.of("x", "PANIC", "nobody is listening")));
        }
    }
}

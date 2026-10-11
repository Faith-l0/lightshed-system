package za.co.lightshed.common;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class StageChangeTest {
    @Test void stageChangeTravelsThroughTheTopic() throws Exception {
        String url = "vm://stagetopictest?broker.persistent=false";
        List<StageChange> received = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch latch = new CountDownLatch(1);
        try (var listener = new JmsStageChangeListener(url, "test-bot", c -> { received.add(c); latch.countDown(); })) {
            try (var publisher = new JmsStageChangePublisher(url)) {
                publisher.publish(StageChange.of(0, 4));
            }
            assertTrue(latch.await(5, TimeUnit.SECONDS), "stage change was not received");
        }
        assertEquals(0, received.get(0).previous());
        assertEquals(4, received.get(0).current());
    }
}

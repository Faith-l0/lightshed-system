package za.co.lightshed.stage;

import io.javalin.testtools.JavalinTest;
import org.junit.jupiter.api.Test;

import za.co.lightshed.common.StageChange;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StageTest {
    @Test void startsAtStageZero() {
        assertEquals(0, new StageService().current());
    }

    @Test void rejectsOutOfRange() {
        var s = new StageService();
        assertThrows(IllegalArgumentException.class, () -> s.set(-1));
        assertThrows(IllegalArgumentException.class, () -> s.set(9));
        assertEquals(0, s.current());
    }

    @Test void getAndSetOverHttp() {
        JavalinTest.test(Main.createApp(new StageService()), (server, client) -> {
            assertTrue(client.get("/stage").body().string().contains("\"stage\":0"));
            assertEquals(200, client.post("/stage", Map.of("stage", 4)).code());
            assertTrue(client.get("/stage").body().string().contains("\"stage\":4"));
        });
    }

    @Test void badRequestsGive400() {
        JavalinTest.test(Main.createApp(new StageService()), (server, client) -> {
            assertEquals(400, client.post("/stage", Map.of("stage", 12)).code());
            assertEquals(400, client.post("/stage", Map.of("level", 3)).code());
            assertEquals(400, client.post("/stage", "not json").code());
        });
    }

    @Test void publishesAnEventOnlyWhenTheStageChanges() {
        List<StageChange> events = new ArrayList<>();
        JavalinTest.test(Main.createApp(new StageService(), events::add), (server, client) -> {
            client.post("/stage", Map.of("stage", 4));
            client.post("/stage", Map.of("stage", 4));   // same stage again: no event
            client.post("/stage", Map.of("stage", 2));
            client.post("/stage", Map.of("stage", 99));  // invalid: no event
        });
        assertEquals(2, events.size());
        assertEquals(0, events.get(0).previous());
        assertEquals(4, events.get(0).current());
        assertEquals(4, events.get(1).previous());
        assertEquals(2, events.get(1).current());
    }
}

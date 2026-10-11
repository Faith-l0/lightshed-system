package za.co.lightshed.schedule;

import org.junit.jupiter.api.Test;
import za.co.lightshed.common.Alert;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ResilienceTest {
    private final List<Alert> alerts = new ArrayList<>();

    private static ServiceUnavailableException down(String service) {
        return new ServiceUnavailableException(service, service + " down", null);
    }

    @Test void retryRecoversFromTemporaryFailure() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        int result = Retry.run(3, 0, () -> {
            if (calls.incrementAndGet() < 3) throw down("stage");
            return 7;
        });
        assertEquals(7, result);
        assertEquals(3, calls.get());
    }

    @Test void retryGivesUpAfterAllAttempts() {
        AtomicInteger calls = new AtomicInteger();
        assertThrows(ServiceUnavailableException.class, () -> Retry.run(3, 0, () -> {
            calls.incrementAndGet();
            throw down("stage");
        }));
        assertEquals(3, calls.get());
    }

    @Test void stageFallsBackToLastKnownAndWarns() throws Exception {
        boolean[] up = {true};
        StageLookup flaky = () -> { if (up[0]) return 4; throw down("stage"); };
        var lookup = new ResilientStageLookup(flaky, alerts::add, 2, 0);
        assertEquals(4, lookup.currentStage());
        up[0] = false;
        assertEquals(4, lookup.currentStage());
        assertEquals(1, alerts.size());
        assertEquals("WARN", alerts.get(0).severity());
    }

    @Test void stageWithNoHistoryPanics() {
        StageLookup dead = () -> { throw down("stage"); };
        var lookup = new ResilientStageLookup(dead, alerts::add, 2, 0);
        assertThrows(ServiceUnavailableException.class, lookup::currentStage);
        assertEquals("PANIC", alerts.get(0).severity());
    }

    @Test void rememberedTownSurvivesPlaceServiceOutage() throws Exception {
        boolean[] up = {true};
        PlaceLookup flaky = (province, town) -> {
            if (up[0]) return Optional.of(new Place("Paarl", "Western Cape"));
            throw down("placename");
        };
        var lookup = new ResilientPlaceLookup(flaky, alerts::add, 2, 0);
        assertTrue(lookup.find("Western Cape", "paarl").isPresent());
        up[0] = false;
        assertTrue(lookup.find("western cape", "PAARL").isPresent());
        assertEquals("WARN", alerts.get(0).severity());
    }

    @Test void unknownTownDuringOutagePanics() {
        PlaceLookup dead = (province, town) -> { throw down("placename"); };
        var lookup = new ResilientPlaceLookup(dead, alerts::add, 2, 0);
        assertThrows(ServiceUnavailableException.class, () -> lookup.find("Gauteng", "Springs"));
        assertEquals("PANIC", alerts.get(0).severity());
    }
}

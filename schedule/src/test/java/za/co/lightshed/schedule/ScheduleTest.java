package za.co.lightshed.schedule;

import io.javalin.testtools.JavalinTest;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ScheduleTest {
    private static final Place PAARL = new Place("Paarl", "Western Cape");

    private final PlaceLookup knowsPaarl = (province, town) ->
            province.equalsIgnoreCase("western cape") && town.equalsIgnoreCase("paarl")
                    ? Optional.of(PAARL) : Optional.empty();
    private final PlaceLookup placeDown = (p, t) -> { throw new ServiceUnavailableException("placename", "Place-name service unreachable", null); };
    private final StageLookup stage3 = () -> 3;
    private final StageLookup stageDown = () -> { throw new ServiceUnavailableException("stage", "Stage service unreachable", null); };

    @Test void stageZeroHasNoOutages() {
        var s = new ScheduleGenerator().generate(PAARL, 0);
        assertEquals(7, s.days().size());
        s.days().forEach(d -> assertTrue(d.slots().isEmpty()));
    }

    @Test void higherStageMeansMoreSlots() {
        var gen = new ScheduleGenerator();
        assertEquals(1, gen.generate(PAARL, 2).days().get(0).slots().size());
        assertEquals(2, gen.generate(PAARL, 3).days().get(0).slots().size());
        assertEquals(4, gen.generate(PAARL, 8).days().get(0).slots().size());
    }

    @Test void sameInputGivesSameSchedule() {
        var gen = new ScheduleGenerator();
        assertEquals(gen.generate(PAARL, 4), gen.generate(PAARL, 4));
    }

    @Test void usesCurrentStageFromStageService() {
        JavalinTest.test(Main.createApp(knowsPaarl, stage3), (server, client) -> {
            var res = client.get("/schedule/western%20cape/paarl");
            assertEquals(200, res.code());
            assertTrue(res.body().string().contains("\"stage\":3"));
        });
    }

    @Test void stageQueryParamOverridesStageService() {
        JavalinTest.test(Main.createApp(knowsPaarl, stageDown), (server, client) -> {
            // Stage service is down, but we gave a stage ourselves, so it must still work.
            assertEquals(200, client.get("/schedule/western%20cape/paarl?stage=5").code());
        });
    }

    @Test void unknownTownIs404() {
        JavalinTest.test(Main.createApp(knowsPaarl, stage3), (server, client) ->
                assertEquals(404, client.get("/schedule/gauteng/atlantis").code()));
    }

    @Test void invalidStageIs400() {
        JavalinTest.test(Main.createApp(knowsPaarl, stage3), (server, client) -> {
            assertEquals(400, client.get("/schedule/western%20cape/paarl?stage=9").code());
            assertEquals(400, client.get("/schedule/western%20cape/paarl?stage=abc").code());
        });
    }

    @Test void downstreamFailureIs503() {
        JavalinTest.test(Main.createApp(placeDown, stage3), (server, client) -> {
            var res = client.get("/schedule/western%20cape/paarl");
            assertEquals(503, res.code());
            assertTrue(res.body().string().contains("placename"));
        });
        JavalinTest.test(Main.createApp(knowsPaarl, stageDown), (server, client) ->
                assertEquals(503, client.get("/schedule/western%20cape/paarl").code()));
    }
}

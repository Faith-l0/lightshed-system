package za.co.lightshed.schedule;

import io.javalin.Javalin;
import za.co.lightshed.common.JmsAlertPublisher;
import za.co.lightshed.common.Queues;
import za.co.lightshed.common.ThrottledAlertPublisher;

import java.util.Map;
import java.util.Optional;

public class Main {
    public static final int DEFAULT_PORT = 7003;

    public static Javalin createApp(PlaceLookup places, StageLookup stages) {
        ScheduleGenerator generator = new ScheduleGenerator();
        Javalin app = Javalin.create();
        app.get("/health", ctx -> ctx.json(Map.of("status", "UP")));

        // GET /schedule/{province}/{town}            -> uses the current stage from the Stage service
        // GET /schedule/{province}/{town}?stage=4    -> uses the stage you give
        app.get("/schedule/{province}/{town}", ctx -> {
            String province = ctx.pathParam("province");
            String town = ctx.pathParam("town");
            try {
                String stageParam = ctx.queryParam("stage");
                int stage;
                if (stageParam != null) {
                    try {
                        stage = Integer.parseInt(stageParam);
                    } catch (NumberFormatException e) {
                        ctx.status(400).json(Map.of("error", "stage must be a whole number between 0 and 8"));
                        return;
                    }
                    if (stage < 0 || stage > 8) {
                        ctx.status(400).json(Map.of("error", "stage must be a whole number between 0 and 8"));
                        return;
                    }
                } else {
                    stage = stages.currentStage();
                }

                Optional<Place> place = places.find(province, town);
                if (place.isEmpty()) {
                    ctx.status(404).json(Map.of("error", "Unknown town: " + town + ", " + province));
                    return;
                }
                ctx.json(generator.generate(place.get(), stage));
            } catch (ServiceUnavailableException e) {
                ctx.status(503).json(Map.of("error", e.getMessage(), "service", e.service()));
            }
        });
        return app;
    }

    public static void main(String[] args) {
        String placeUrl = System.getProperty("placename.url", "http://localhost:7001");
        String stageUrl = System.getProperty("stage.url", "http://localhost:7002");
        int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
        var alerts = new ThrottledAlertPublisher(new JmsAlertPublisher(Queues.brokerUrl()), 30_000);
        PlaceLookup places = new ResilientPlaceLookup(new HttpPlaceLookup(placeUrl), alerts, 3, 200);
        StageLookup stages = new ResilientStageLookup(new HttpStageLookup(stageUrl), alerts, 3, 200);
        createApp(places, stages).start(port);
    }
}

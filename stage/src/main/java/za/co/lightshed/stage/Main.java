package za.co.lightshed.stage;

import io.javalin.Javalin;
import za.co.lightshed.common.JmsStageChangePublisher;
import za.co.lightshed.common.Queues;
import za.co.lightshed.common.StageChange;

import java.util.Map;
import java.util.function.Consumer;

public class Main {
    public static final int DEFAULT_PORT = 7002;

    /** Body for POST /stage, e.g. {"stage": 4}. A null stage means the field was missing. */
    public record StageRequest(Integer stage) {}

    public static Javalin createApp(StageService service) {
        return createApp(service, change -> {});
    }

    /** onChange is called only when the stage really changes (setting the same stage again is ignored). */
    public static Javalin createApp(StageService service, Consumer<StageChange> onChange) {
        return Javalin.create()
                .get("/health", ctx -> ctx.json(Map.of("status", "UP")))
                .get("/stage", ctx -> ctx.json(Map.of("stage", service.current())))
                .post("/stage", ctx -> {
                    StageRequest req;
                    try {
                        req = ctx.bodyAsClass(StageRequest.class);
                    } catch (Exception e) {
                        ctx.status(400).json(Map.of("error", "Body must be JSON like {\"stage\": 4}"));
                        return;
                    }
                    if (req.stage() == null || !StageService.isValid(req.stage())) {
                        ctx.status(400).json(Map.of("error",
                                "Stage must be a whole number between 0 and 8"));
                        return;
                    }
                    int previous;
                    synchronized (service) {
                        previous = service.current();
                        service.set(req.stage());
                    }
                    if (previous != req.stage()) {
                        onChange.accept(StageChange.of(previous, req.stage()));
                    }
                    ctx.json(Map.of("stage", service.current()));
                });
    }

    public static void main(String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
        var publisher = new JmsStageChangePublisher(Queues.brokerUrl());
        createApp(new StageService(), publisher::publish).start(port);
    }
}

package za.co.lightshed.stage;

import io.javalin.Javalin;

import java.util.Map;

public class Main {
    public static final int DEFAULT_PORT = 7002;

    /** Body for POST /stage, e.g. {"stage": 4}. A null stage means the field was missing. */
    public record StageRequest(Integer stage) {}

    public static Javalin createApp(StageService service) {
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
                    service.set(req.stage());
                    ctx.json(Map.of("stage", service.current()));
                });
    }

    public static void main(String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
        createApp(new StageService()).start(port);
    }
}

package za.co.lightshed.placename;

import io.javalin.Javalin;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

public class Main {
    public static final int DEFAULT_PORT = 7001;

    public static Javalin createApp(PlaceNameService service) {
        return Javalin.create()
                .get("/health", ctx -> ctx.json(java.util.Map.of("status", "UP")))
                .get("/provinces", ctx -> ctx.json(service.provinces()))
                .get("/towns", ctx -> ctx.json(service.all()))
                .get("/towns/{province}", ctx -> {
                    String province = ctx.pathParam("province");
                    if (!service.isKnownProvince(province)) {
                        ctx.status(404).json(java.util.Map.of("error", "Unknown province: " + province));
                        return;
                    }
                    ctx.json(service.townsIn(province));
                });
    }

    public static void main(String[] args) throws Exception {
        PlaceNameCleaner.Result result;
        try (Reader r = new InputStreamReader(
                Main.class.getResourceAsStream("/towns_raw.csv"), StandardCharsets.UTF_8)) {
            result = new PlaceNameCleaner().clean(r);
        }
        System.out.printf("Loaded %d places (%d rejected, %d duplicates removed)%n",
                result.places().size(), result.rejected(), result.duplicates());
        int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
        createApp(new PlaceNameService(result.places())).start(port);
    }
}

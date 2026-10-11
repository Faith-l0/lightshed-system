package za.co.lightshed.monitor;

import io.javalin.Javalin;
import za.co.lightshed.common.JmsAlertConsumer;
import za.co.lightshed.common.JmsAlertPublisher;
import za.co.lightshed.common.Queues;
import za.co.lightshed.common.ThrottledAlertPublisher;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

public class Main {
    public static final int DEFAULT_PORT = 7004;

    public static Javalin createApp(AlertStore store) {
        return Javalin.create()
                .get("/health", ctx -> ctx.json(Map.of("status", "UP")))
                .get("/alerts", ctx -> ctx.json(store.recent()));
    }

    public static void main(String[] args) throws Exception {
        AlertStore store = new AlertStore(100);
        String brokerUrl = Queues.brokerUrl();

        try {
            new JmsAlertConsumer(brokerUrl, alert -> {
                store.add(alert);
                System.err.printf("[%s] %s: %s%n", alert.severity(), alert.source(), alert.message());
            });
        } catch (Exception e) {
            System.err.println("Cannot reach the broker at " + brokerUrl
                    + ". Start BrokerMain first. (" + e.getMessage() + ")");
            System.exit(1);
        }

        Map<String, BooleanSupplier> checks = new LinkedHashMap<>();
        checks.put("placename", HttpHealthCheck.of("http://localhost:7001"));
        checks.put("stage", HttpHealthCheck.of("http://localhost:7002"));
        checks.put("schedule", HttpHealthCheck.of("http://localhost:7003"));
        var monitor = new HealthMonitor(checks, new ThrottledAlertPublisher(new JmsAlertPublisher(brokerUrl), 30_000));
        Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "health-monitor");
            t.setDaemon(true);
            return t;
        }).scheduleWithFixedDelay(monitor::runOnce, 5, 10, TimeUnit.SECONDS);

        int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
        createApp(store).start(port);
    }
}

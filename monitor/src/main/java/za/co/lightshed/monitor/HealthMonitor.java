package za.co.lightshed.monitor;

import za.co.lightshed.common.Alert;
import za.co.lightshed.common.AlertPublisher;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;

/**
 * Proactive monitoring: checks each service and raises a PANIC alert when one goes down, and an
 * INFO alert when it comes back. Only changes in state raise alerts, so a long outage = one alert.
 */
public class HealthMonitor {
    private final Map<String, BooleanSupplier> checks;
    private final AlertPublisher alerts;
    private final Map<String, Boolean> lastState = new ConcurrentHashMap<>();

    public HealthMonitor(Map<String, BooleanSupplier> checks, AlertPublisher alerts) {
        this.checks = checks;
        this.alerts = alerts;
    }

    public void runOnce() {
        checks.forEach((name, check) -> {
            boolean up;
            try {
                up = check.getAsBoolean();
            } catch (Exception e) {
                up = false;
            }
            Boolean previous = lastState.put(name, up);
            if (!up && (previous == null || previous)) {
                alerts.publish(Alert.of("monitor", "PANIC", name + " service is DOWN"));
            } else if (up && previous != null && !previous) {
                alerts.publish(Alert.of("monitor", "INFO", name + " service has recovered"));
            }
        });
    }
}

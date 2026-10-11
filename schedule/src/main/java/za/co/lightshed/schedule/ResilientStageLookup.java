package za.co.lightshed.schedule;

import za.co.lightshed.common.Alert;
import za.co.lightshed.common.AlertPublisher;

/**
 * Wraps a StageLookup with retries. If the Stage service stays down it falls back to the last
 * stage it saw (WARN alert), or fails with a PANIC alert if it has never had one.
 */
public class ResilientStageLookup implements StageLookup {
    private final StageLookup delegate;
    private final AlertPublisher alerts;
    private final int attempts;
    private final long delayMillis;
    private volatile Integer lastKnown;

    public ResilientStageLookup(StageLookup delegate, AlertPublisher alerts, int attempts, long delayMillis) {
        this.delegate = delegate;
        this.alerts = alerts;
        this.attempts = attempts;
        this.delayMillis = delayMillis;
    }

    @Override
    public int currentStage() throws ServiceUnavailableException {
        try {
            int stage = Retry.run(attempts, delayMillis, delegate::currentStage);
            lastKnown = stage;
            return stage;
        } catch (ServiceUnavailableException e) {
            Integer cached = lastKnown;
            if (cached != null) {
                alerts.publish(Alert.of("schedule", "WARN",
                        "Stage service unreachable; using last known stage " + cached));
                return cached;
            }
            alerts.publish(Alert.of("schedule", "PANIC",
                    "Stage service unreachable and no earlier stage known: " + e.getMessage()));
            throw e;
        }
    }
}

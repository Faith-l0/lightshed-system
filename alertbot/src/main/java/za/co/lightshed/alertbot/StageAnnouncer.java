package za.co.lightshed.alertbot;

import za.co.lightshed.common.Alert;
import za.co.lightshed.common.AlertPublisher;
import za.co.lightshed.common.StageChange;

/**
 * Posts each stage change to the platform. It retries with backoff, skips a stage it has already
 * announced (so redelivered messages don't cause duplicate public posts), and if posting still fails
 * it sends a PANIC alert and throws so the message is redelivered later instead of being lost.
 */
public class StageAnnouncer {
    private final SocialPlatform platform;
    private final AlertPublisher alerts;
    private final int attempts;
    private final long delayMillis;
    private Integer lastAnnounced;

    public StageAnnouncer(SocialPlatform platform, AlertPublisher alerts, int attempts, long delayMillis) {
        this.platform = platform;
        this.alerts = alerts;
        this.attempts = attempts;
        this.delayMillis = delayMillis;
    }

    public synchronized void handle(StageChange change) {
        if (lastAnnounced != null && lastAnnounced == change.current()) return;
        String text = StageMessages.format(change);
        PostFailedException last = null;
        for (int i = 0; i < attempts; i++) {
            try {
                platform.post(text);
                lastAnnounced = change.current();
                return;
            } catch (PostFailedException e) {
                last = e;
                if (i < attempts - 1 && delayMillis > 0) {
                    try {
                        Thread.sleep(delayMillis * (1L << i));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        alerts.publish(Alert.of("alertbot", "PANIC",
                "Could not post stage " + change.current() + " to " + platform.name() + ": "
                        + (last == null ? "interrupted" : last.getMessage())));
        throw new IllegalStateException("Posting to " + platform.name() + " failed", last);
    }
}

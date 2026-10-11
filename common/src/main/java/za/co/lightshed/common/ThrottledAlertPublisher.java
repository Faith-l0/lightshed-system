package za.co.lightshed.common;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/** Drops repeats of the same alert inside a time window, so an outage doesn't flood the queue. */
public class ThrottledAlertPublisher implements AlertPublisher {
    private final AlertPublisher delegate;
    private final long windowMillis;
    private final LongSupplier clock;
    private final Map<String, Long> lastSent = new ConcurrentHashMap<>();

    public ThrottledAlertPublisher(AlertPublisher delegate, long windowMillis) {
        this(delegate, windowMillis, System::currentTimeMillis);
    }

    public ThrottledAlertPublisher(AlertPublisher delegate, long windowMillis, LongSupplier clock) {
        this.delegate = delegate;
        this.windowMillis = windowMillis;
        this.clock = clock;
    }

    @Override
    public void publish(Alert alert) {
        String key = alert.source() + "|" + alert.severity() + "|" + alert.message();
        long now = clock.getAsLong();
        Long previous = lastSent.get(key);
        if (previous != null && now - previous < windowMillis) return;
        lastSent.put(key, now);
        delegate.publish(alert);
    }
}

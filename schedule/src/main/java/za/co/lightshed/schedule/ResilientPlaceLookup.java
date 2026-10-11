package za.co.lightshed.schedule;

import za.co.lightshed.common.Alert;
import za.co.lightshed.common.AlertPublisher;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Wraps a PlaceLookup with retries and a cache of towns it has already confirmed. If the
 * Place-name service stays down, known towns still work (WARN alert); unknown ones fail (PANIC).
 */
public class ResilientPlaceLookup implements PlaceLookup {
    private final PlaceLookup delegate;
    private final AlertPublisher alerts;
    private final int attempts;
    private final long delayMillis;
    private final Map<String, Place> known = new ConcurrentHashMap<>();

    public ResilientPlaceLookup(PlaceLookup delegate, AlertPublisher alerts, int attempts, long delayMillis) {
        this.delegate = delegate;
        this.alerts = alerts;
        this.attempts = attempts;
        this.delayMillis = delayMillis;
    }

    private static String key(String province, String town) {
        return province.trim().toLowerCase() + "|" + town.trim().toLowerCase();
    }

    @Override
    public Optional<Place> find(String province, String town) throws ServiceUnavailableException {
        try {
            Optional<Place> place = Retry.run(attempts, delayMillis, () -> delegate.find(province, town));
            place.ifPresent(p -> known.put(key(province, town), p));
            return place;
        } catch (ServiceUnavailableException e) {
            Place cached = known.get(key(province, town));
            if (cached != null) {
                alerts.publish(Alert.of("schedule", "WARN",
                        "Place-name service unreachable; using remembered town " + cached.name()));
                return Optional.of(cached);
            }
            alerts.publish(Alert.of("schedule", "PANIC",
                    "Place-name service unreachable and town not remembered: " + e.getMessage()));
            throw e;
        }
    }
}

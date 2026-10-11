package za.co.lightshed.common;

/** Sends alerts somewhere. Implementations must never throw: alerting must not break a request. */
public interface AlertPublisher {
    void publish(Alert alert);
}

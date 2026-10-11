package za.co.lightshed.common;

import java.time.Instant;

/** An alert sent over the message queue. Severity is INFO, WARN or PANIC. */
public record Alert(String source, String severity, String message, String timestamp) {
    public static Alert of(String source, String severity, String message) {
        return new Alert(source, severity, message, Instant.now().toString());
    }
}

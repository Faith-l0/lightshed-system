package za.co.lightshed.common;

import java.time.Instant;

/** Published whenever the loadshedding stage actually changes. */
public record StageChange(int previous, int current, String timestamp) {
    public static StageChange of(int previous, int current) {
        return new StageChange(previous, current, Instant.now().toString());
    }
}

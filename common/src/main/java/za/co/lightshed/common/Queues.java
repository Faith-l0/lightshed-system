package za.co.lightshed.common;

public final class Queues {
    /** Queue that all services publish alerts to; the Monitor service consumes it. */
    public static final String PANIC = "panic.alerts";
    public static final String DEFAULT_BROKER_URL = "tcp://localhost:61616";

    private Queues() {}

    /** Broker URL from -Dbroker.url=..., or the default local broker. */
    public static String brokerUrl() {
        return System.getProperty("broker.url", DEFAULT_BROKER_URL);
    }
}

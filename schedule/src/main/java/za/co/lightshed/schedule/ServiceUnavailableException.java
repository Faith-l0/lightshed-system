package za.co.lightshed.schedule;

/** Thrown when a downstream service can't be reached or answers with a server error. */
public class ServiceUnavailableException extends Exception {
    private final String service;

    public ServiceUnavailableException(String service, String message, Throwable cause) {
        super(message, cause);
        this.service = service;
    }

    public String service() { return service; }
}

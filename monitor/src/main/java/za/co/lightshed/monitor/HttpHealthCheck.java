package za.co.lightshed.monitor;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.function.BooleanSupplier;

public final class HttpHealthCheck {
    private static final HttpClient CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();

    private HttpHealthCheck() {}

    /** True if GET {baseUrl}/health answers 200 within a few seconds. */
    public static BooleanSupplier of(String baseUrl) {
        return () -> {
            try {
                HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/health"))
                        .timeout(Duration.ofSeconds(3)).GET().build();
                return CLIENT.send(request, HttpResponse.BodyHandlers.discarding()).statusCode() == 200;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            } catch (Exception e) {
                return false;
            }
        };
    }
}

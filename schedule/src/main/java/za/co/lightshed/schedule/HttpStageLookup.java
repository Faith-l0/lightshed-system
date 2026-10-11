package za.co.lightshed.schedule;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/** Calls GET {baseUrl}/stage on the Stage service. */
public class HttpStageLookup implements StageLookup {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    private final String baseUrl;

    public HttpStageLookup(String baseUrl) { this.baseUrl = baseUrl; }

    @Override
    public int currentStage() throws ServiceUnavailableException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/stage"))
                .timeout(Duration.ofSeconds(3)).GET().build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new ServiceUnavailableException("stage",
                        "Stage service answered " + response.statusCode(), null);
            }
            JsonNode stage = MAPPER.readTree(response.body()).get("stage");
            if (stage == null || !stage.isInt()) {
                throw new ServiceUnavailableException("stage", "Stage service sent an unexpected reply", null);
            }
            return stage.asInt();
        } catch (IOException e) {
            throw new ServiceUnavailableException("stage", "Stage service unreachable", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServiceUnavailableException("stage", "Interrupted calling Stage service", e);
        }
    }
}

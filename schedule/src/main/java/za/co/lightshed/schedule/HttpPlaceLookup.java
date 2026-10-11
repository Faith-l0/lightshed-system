package za.co.lightshed.schedule;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

/** Calls GET {baseUrl}/towns/{province} on the Place-name service. */
public class HttpPlaceLookup implements PlaceLookup {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    private final String baseUrl;

    public HttpPlaceLookup(String baseUrl) { this.baseUrl = baseUrl; }

    @Override
    public Optional<Place> find(String province, String town) throws ServiceUnavailableException {
        String encoded = URLEncoder.encode(province.trim(), StandardCharsets.UTF_8).replace("+", "%20");
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/towns/" + encoded))
                .timeout(Duration.ofSeconds(3)).GET().build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 404) return Optional.empty(); // unknown province
            if (response.statusCode() != 200) {
                throw new ServiceUnavailableException("placename",
                        "Place-name service answered " + response.statusCode(), null);
            }
            List<Place> places = MAPPER.readValue(response.body(),
                    MAPPER.getTypeFactory().constructCollectionType(List.class, Place.class));
            return places.stream().filter(p -> p.name().equalsIgnoreCase(town.trim())).findFirst();
        } catch (IOException e) {
            throw new ServiceUnavailableException("placename", "Place-name service unreachable", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServiceUnavailableException("placename", "Interrupted calling Place-name service", e);
        }
    }
}

package za.co.lightshed.alertbot;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/** Posts to a Telegram chat or channel through the Bot API (sendMessage). */
public class TelegramPlatform implements SocialPlatform {
    public static final String DEFAULT_BASE_URL = "https://api.telegram.org";
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    private final String token;
    private final String chatId;
    private final String baseUrl;

    public TelegramPlatform(String token, String chatId) { this(token, chatId, DEFAULT_BASE_URL); }

    public TelegramPlatform(String token, String chatId, String baseUrl) {
        this.token = token;
        this.chatId = chatId;
        this.baseUrl = baseUrl;
    }

    @Override public String name() { return "telegram"; }

    @Override
    public void post(String text) throws PostFailedException {
        String body = "{\"chat_id\":" + json(chatId) + ",\"text\":" + json(text) + "}";
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/bot" + token + "/sendMessage"))
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        try {
            HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() != 200) {
                // The token is part of the URL, so never put the URL in the message.
                throw new PostFailedException("Telegram answered " + response.statusCode(), null);
            }
        } catch (IOException e) {
            throw new PostFailedException("Telegram unreachable", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PostFailedException("Interrupted while posting to Telegram", e);
        }
    }

    /** Minimal JSON string encoder. */
    static String json(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
                }
            }
        }
        return sb.append('"').toString();
    }
}

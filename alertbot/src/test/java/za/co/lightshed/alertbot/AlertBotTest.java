package za.co.lightshed.alertbot;

import com.sun.net.httpserver.HttpServer;
import io.javalin.testtools.JavalinTest;
import org.junit.jupiter.api.Test;
import za.co.lightshed.common.Alert;
import za.co.lightshed.common.StageChange;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class AlertBotTest {
    private final List<Alert> alerts = new ArrayList<>();

    private static StageChange change(int from, int to) { return new StageChange(from, to, "t"); }

    /** A platform that records posts and can be told to fail a number of times first. */
    static class FakePlatform implements SocialPlatform {
        final List<String> posted = new ArrayList<>();
        int failuresLeft;
        int attempts;
        FakePlatform(int failures) { this.failuresLeft = failures; }
        @Override public String name() { return "fake"; }
        @Override public void post(String text) throws PostFailedException {
            attempts++;
            if (failuresLeft-- > 0) throw new PostFailedException("down", null);
            posted.add(text);
        }
    }

    @Test void messagesDescribeTheChange() {
        assertTrue(StageMessages.format(change(2, 4)).contains("increased from Stage 2 to Stage 4"));
        assertTrue(StageMessages.format(change(4, 2)).contains("reduced from Stage 4 to Stage 2"));
        assertTrue(StageMessages.format(change(0, 3)).contains("back at Stage 3"));
        assertTrue(StageMessages.format(change(5, 0)).contains("suspended"));
    }

    @Test void postsAStageChange() {
        var platform = new FakePlatform(0);
        new StageAnnouncer(platform, alerts::add, 3, 0).handle(change(0, 4));
        assertEquals(1, platform.posted.size());
        assertTrue(alerts.isEmpty());
    }

    @Test void doesNotPostTheSameStageTwice() {
        var platform = new FakePlatform(0);
        var announcer = new StageAnnouncer(platform, alerts::add, 3, 0);
        announcer.handle(change(0, 4));
        announcer.handle(change(0, 4));   // redelivered message
        assertEquals(1, platform.posted.size());
    }

    @Test void retriesAndSucceeds() {
        var platform = new FakePlatform(2);
        new StageAnnouncer(platform, alerts::add, 3, 0).handle(change(0, 4));
        assertEquals(3, platform.attempts);
        assertEquals(1, platform.posted.size());
        assertTrue(alerts.isEmpty());
    }

    @Test void panicsAndThrowsWhenPostingKeepsFailing() {
        var platform = new FakePlatform(99);
        var announcer = new StageAnnouncer(platform, alerts::add, 3, 0);
        assertThrows(IllegalStateException.class, () -> announcer.handle(change(0, 4)));
        assertEquals(3, platform.attempts);
        assertEquals("PANIC", alerts.get(0).severity());
        assertEquals("alertbot", alerts.get(0).source());
        // Not marked as announced, so a later redelivery tries again.
        platform.failuresLeft = 0;
        announcer.handle(change(0, 4));
        assertEquals(1, platform.posted.size());
    }

    @Test void telegramPlatformSendsTheRightRequest() throws Exception {
        AtomicReference<String> path = new AtomicReference<>();
        AtomicReference<String> body = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", exchange -> {
            path.set(exchange.getRequestURI().getPath());
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.start();
        try {
            var telegram = new TelegramPlatform("TOKEN123", "@lightshed", "http://localhost:" + server.getAddress().getPort());
            telegram.post("Stage 4 \"now\"\nline two");
        } finally {
            server.stop(0);
        }
        assertEquals("/botTOKEN123/sendMessage", path.get());
        assertEquals("{\"chat_id\":\"@lightshed\",\"text\":\"Stage 4 \\\"now\\\"\\nline two\"}", body.get());
    }

    @Test void telegramErrorBecomesPostFailedWithoutLeakingTheToken() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", exchange -> { exchange.sendResponseHeaders(500, -1); exchange.close(); });
        server.start();
        try {
            var telegram = new TelegramPlatform("SECRET", "1", "http://localhost:" + server.getAddress().getPort());
            var e = assertThrows(PostFailedException.class, () -> telegram.post("hi"));
            assertFalse(e.getMessage().contains("SECRET"));
        } finally {
            server.stop(0);
        }
    }

    @Test void unreachableTelegramBecomesPostFailed() {
        var telegram = new TelegramPlatform("T", "1", "http://localhost:1");
        assertThrows(PostFailedException.class, () -> telegram.post("hi"));
    }

    @Test void recentPostsEndpointShowsSuccessfulPosts() {
        var posts = new RecentPosts(10);
        var platform = posts.recording(new FakePlatform(0));
        new StageAnnouncer(platform, alerts::add, 3, 0).handle(change(0, 5));
        JavalinTest.test(Main.createApp(posts), (server, client) -> {
            assertTrue(client.get("/posts").body().string().contains("Stage 5"));
            assertEquals(200, client.get("/health").code());
        });
    }
}

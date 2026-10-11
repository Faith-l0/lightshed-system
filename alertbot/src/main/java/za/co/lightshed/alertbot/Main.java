package za.co.lightshed.alertbot;

import io.javalin.Javalin;
import za.co.lightshed.common.JmsAlertPublisher;
import za.co.lightshed.common.JmsStageChangeListener;
import za.co.lightshed.common.Queues;
import za.co.lightshed.common.ThrottledAlertPublisher;

import java.util.Map;

public class Main {
    public static final int DEFAULT_PORT = 7005;

    public static Javalin createApp(RecentPosts posts) {
        return Javalin.create()
                .get("/health", ctx -> ctx.json(Map.of("status", "UP")))
                .get("/posts", ctx -> ctx.json(posts.recent()));
    }

    /** Telegram if TELEGRAM_BOT_TOKEN and TELEGRAM_CHAT_ID are set, otherwise the console. */
    static SocialPlatform choosePlatform() {
        String token = System.getenv("TELEGRAM_BOT_TOKEN");
        String chat = System.getenv("TELEGRAM_CHAT_ID");
        if (token != null && !token.isBlank() && chat != null && !chat.isBlank()) {
            System.out.println("AlertBot posting to Telegram chat " + chat);
            return new TelegramPlatform(token, chat);
        }
        System.out.println("TELEGRAM_BOT_TOKEN / TELEGRAM_CHAT_ID not set: AlertBot will only print posts.");
        return new ConsoleSocialPlatform();
    }

    public static void main(String[] args) {
        String brokerUrl = Queues.brokerUrl();
        RecentPosts posts = new RecentPosts(50);
        var alerts = new ThrottledAlertPublisher(new JmsAlertPublisher(brokerUrl), 30_000);
        var announcer = new StageAnnouncer(posts.recording(choosePlatform()), alerts, 3, 500);

        try {
            new JmsStageChangeListener(brokerUrl, "alertbot", announcer::handle);
        } catch (Exception e) {
            System.err.println("Cannot reach the broker at " + brokerUrl
                    + ". Start BrokerMain first. (" + e.getMessage() + ")");
            System.exit(1);
        }

        int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
        createApp(posts).start(port);
    }
}

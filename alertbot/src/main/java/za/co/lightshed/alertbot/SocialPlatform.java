package za.co.lightshed.alertbot;

/** Somewhere the AlertBot can publish a public message (Telegram, console, ...). */
public interface SocialPlatform {
    String name();

    void post(String text) throws PostFailedException;
}

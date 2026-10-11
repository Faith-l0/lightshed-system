package za.co.lightshed.alertbot;

/** Prints posts to the console. Used when no real platform is configured. */
public class ConsoleSocialPlatform implements SocialPlatform {
    @Override public String name() { return "console"; }

    @Override public void post(String text) {
        System.out.println("[ALERTBOT POST] " + text);
    }
}

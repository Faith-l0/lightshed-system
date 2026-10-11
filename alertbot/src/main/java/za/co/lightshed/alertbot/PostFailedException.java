package za.co.lightshed.alertbot;

/** A post could not be delivered to the social media platform. */
public class PostFailedException extends Exception {
    public PostFailedException(String message, Throwable cause) { super(message, cause); }
}

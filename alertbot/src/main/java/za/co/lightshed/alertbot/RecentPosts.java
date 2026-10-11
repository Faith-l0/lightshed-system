package za.co.lightshed.alertbot;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/** Remembers the last posts the bot made, newest first, so you can see them at GET /posts. */
public class RecentPosts {
    public record Post(String platform, String text, String timestamp) {}

    private final int capacity;
    private final Deque<Post> posts = new ArrayDeque<>();

    public RecentPosts(int capacity) { this.capacity = capacity; }

    public synchronized void add(Post post) {
        posts.addFirst(post);
        while (posts.size() > capacity) posts.removeLast();
    }

    public synchronized List<Post> recent() { return new ArrayList<>(posts); }

    /** Wraps a platform so every successful post is also recorded here. */
    public SocialPlatform recording(SocialPlatform delegate) {
        return new SocialPlatform() {
            @Override public String name() { return delegate.name(); }

            @Override public void post(String text) throws PostFailedException {
                delegate.post(text);
                add(new Post(delegate.name(), text, Instant.now().toString()));
            }
        };
    }
}

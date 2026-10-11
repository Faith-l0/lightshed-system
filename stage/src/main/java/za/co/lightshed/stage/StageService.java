package za.co.lightshed.stage;

/** Holds the current loadshedding stage (0 = no loadshedding, 8 = worst). Thread-safe. */
public class StageService {
    public static final int MIN_STAGE = 0;
    public static final int MAX_STAGE = 8;

    private volatile int stage = 0;

    public int current() { return stage; }

    public static boolean isValid(int stage) { return stage >= MIN_STAGE && stage <= MAX_STAGE; }

    /** @throws IllegalArgumentException if the stage is outside 0-8 */
    public void set(int newStage) {
        if (!isValid(newStage)) {
            throw new IllegalArgumentException("Stage must be between " + MIN_STAGE + " and " + MAX_STAGE);
        }
        this.stage = newStage;
    }
}

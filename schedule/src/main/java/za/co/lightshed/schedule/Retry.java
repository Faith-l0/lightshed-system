package za.co.lightshed.schedule;

/** Retries a call with exponential backoff (delay, 2*delay, 4*delay...). */
public final class Retry {
    private Retry() {}

    public static <T> T run(int attempts, long delayMillis, ThrowingSupplier<T> call)
            throws ServiceUnavailableException {
        ServiceUnavailableException last = null;
        for (int i = 0; i < attempts; i++) {
            try {
                return call.get();
            } catch (ServiceUnavailableException e) {
                last = e;
                if (i < attempts - 1 && delayMillis > 0) {
                    try {
                        Thread.sleep(delayMillis * (1L << i));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw e;
                    }
                }
            }
        }
        throw last;
    }
}

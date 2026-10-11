package za.co.lightshed.schedule;

@FunctionalInterface
public interface ThrowingSupplier<T> {
    T get() throws ServiceUnavailableException;
}

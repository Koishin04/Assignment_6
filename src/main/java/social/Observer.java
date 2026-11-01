package social;

@FunctionalInterface
public interface Observer<T> {
    void onEvent(T event);
}

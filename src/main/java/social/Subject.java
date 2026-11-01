package social;

public interface Subject<T> {
    void subscribe(EventType type, Observer<T> observer);
    void unsubscribe(EventType type, Observer<T> observer);
    void publish(T event);
}

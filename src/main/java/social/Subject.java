package social;

public interface Subject<T> {
    default void subscribe(EventType type, Observer<T> observer) {
        subscribe(type, observer, 0); 
    void subscribe(EventType type, Observer<T> observer);
    void unsubscribe(EventType type, Observer<T> observer);
    void publish(T event);
}



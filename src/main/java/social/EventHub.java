package social;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

public final class EventHub implements Subject<Event> {
    private final Map<EventType, CopyOnWriteArrayList<Observer<Event>>> listeners;

    public EventHub() {
        listeners = new EnumMap<>(EventType.class);
        for (EventType type : EventType.values()) {
            listeners.put(type, new CopyOnWriteArrayList<>());
        }
    }

    public void subscribe(EventType type, Observer<Event> observer) {
        listeners.get(type).addIfAbsent(Objects.requireNonNull(observer));
    }

    public void unsubscribe(EventType type, Observer<Event> observer) {
        listeners.get(type).remove(observer);
    }

    public void publish(Event event) {
        RuntimeException failure = null;
        for (Observer<Event> observer : listeners.get(event.type())) {
            try {
                observer.onEvent(event);
            } catch (RuntimeException exception) {
                if (failure == null) failure = new RuntimeException("Listener failed");
                failure.addSuppressed(exception);
            }
        }
        if (failure != null) throw failure;
    }
}

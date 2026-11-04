package social;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public final class EventHub implements Subject<Event> {
    private record Subscription(Observer<Event> observer, int priority) {}

    private final Map<EventType, AtomicReference<List<Subscription>>> listeners =
            new EnumMap<>(EventType.class);

    public EventHub() {
        for (EventType type : EventType.values()) {
            listeners.put(type, new AtomicReference<>(List.of()));
        }
    }

    @Override
    public void subscribe(EventType type, Observer<Event> observer, int priority) {
        Objects.requireNonNull(observer);

        listeners.get(type).updateAndGet(snapshot -> {
            for (Subscription entry : snapshot) {
                if (observer.equals(entry.observer())) {
                    return snapshot;
                }
            }

            List<Subscription> updated = new ArrayList<>(snapshot);
            int index = 0;

            // Пропускаем всех с более высоким или равным приоритетом.
            while (index < updated.size() && updated.get(index).priority() >= priority) {
                index++;
            }

            updated.add(index, new Subscription(observer, priority));
            return List.copyOf(updated);
        });
    }

    @Override
    public void unsubscribe(EventType type, Observer<Event> observer) {
        listeners.get(type).updateAndGet(snapshot -> snapshot.stream()
                .filter(entry -> !entry.observer().equals(observer))
                .toList());
    }

    @Override
    public void publish(Event event) {
        List<Subscription> snapshot = listeners.get(event.type()).get();
        RuntimeException failure = null;

        for (Subscription entry : snapshot) {
            try {
                entry.observer().onEvent(event);
            } catch (RuntimeException exception) {
                if (failure == null) {
                    failure = new RuntimeException("Listener failed");
                }
                failure.addSuppressed(exception);
            }
        }

        if (failure != null) {
            throw failure;
        }
    }
}
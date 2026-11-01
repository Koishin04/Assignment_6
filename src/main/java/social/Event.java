package social;

import java.util.Objects;

public record Event(EventType type, String groupId, String contentId,
                    String message, long value) {
    public Event {
        Objects.requireNonNull(type);
        Objects.requireNonNull(groupId);
        Objects.requireNonNull(contentId);
        Objects.requireNonNull(message);
        if (value < 0) throw new IllegalArgumentException("Negative value");
    }
}

enum EventType { POSTED, REACTED, REPORTED, SUMMARY, REVIEWED }

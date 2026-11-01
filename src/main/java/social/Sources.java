package social;

final class PostSource {
    private final Subject<Event> events;

    PostSource(Subject<Event> events) { this.events = events; }

    void post(String groupId, String contentId, String text) {
        events.publish(new Event(EventType.POSTED, groupId, contentId, text, 0));
    }

    void report(String groupId, String contentId) {
        events.publish(new Event(EventType.REPORTED, groupId, contentId, "Review requested", 0));
    }
}

final class ReactionSource {
    private final Subject<Event> events;

    ReactionSource(Subject<Event> events) { this.events = events; }

    void like(String groupId, String contentId) {
        events.publish(new Event(EventType.REACTED, groupId, contentId, "Like", 1));
    }
}

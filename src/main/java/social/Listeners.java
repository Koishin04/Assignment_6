package social;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Predicate;

final class Feed implements Observer<Event> {
    private final ConcurrentLinkedQueue<Event> posts = new ConcurrentLinkedQueue<>();
    public void onEvent(Event event) { posts.add(event); }
    int size() { return posts.size(); }
}

final class NotificationCenter implements Observer<Event> {
    private final ConcurrentLinkedQueue<Event> messages = new ConcurrentLinkedQueue<>();
    public void onEvent(Event event) { messages.add(event); }
    int size() { return messages.size(); }
}

final class EngagementStatistics implements Observer<Event>, Subject<Event> {
    private final Subject<Event> summaries;
    private final ConcurrentHashMap<String, Long> totals = new ConcurrentHashMap<>();

    EngagementStatistics(Subject<Event> summaries) { this.summaries = summaries; }

    public void onEvent(Event event) {
        long total = totals.merge(event.groupId(), event.value(), Long::sum);
        publish(new Event(EventType.SUMMARY, event.groupId(), "", "Total reactions", total));
    }
    long total(String groupId) { return totals.getOrDefault(groupId, 0L); }
    public void subscribe(EventType type, Observer<Event> observer) { summaries.subscribe(type, observer); }
    public void unsubscribe(EventType type, Observer<Event> observer) { summaries.unsubscribe(type, observer); }
    public void publish(Event event) { summaries.publish(event); }
}

final class TrendingTracker implements Observer<Event> {
    private final ConcurrentHashMap<String, Long> scores = new ConcurrentHashMap<>();
    public void onEvent(Event event) {
        scores.merge(event.groupId(), event.value(), Long::max);
    }
    long score(String groupId) { return scores.getOrDefault(groupId, 0L); }
}

final class ModerationService implements Observer<Event> {
    private final Map<String, Group> groups;
    private final Predicate<Content> rule;
    private final Subject<Event> reviews;

    ModerationService(Map<String, Group> groups, Predicate<Content> rule, Subject<Event> reviews) {
        this.groups = Map.copyOf(groups);
        this.rule = rule;
        this.reviews = reviews;
    }
    public void onEvent(Event event) {
    Group group = groups.get(event.groupId());
    if (group == null) {
        throw new IllegalArgumentException("Unknown group: " + event.groupId());
    }

    List<Content> matches = group.accept(new FilterVisitor(
            node -> node.id().equals(event.contentId()) && rule.test(node)));

    String ids = matches.stream()
            .map(Content::id)
            .collect(java.util.stream.Collectors.joining(","));

    reviews.publish(new Event(
            EventType.REVIEWED,
            group.id(),
            event.contentId(),
            ids,
            matches.size()));
    }
}

final class ReviewLog implements Observer<Event> {
    private final ConcurrentLinkedQueue<Event> reviews = new ConcurrentLinkedQueue<>();
    public void onEvent(Event event) { reviews.add(event); }
    List<Event> entries() { return List.copyOf(reviews); }
}

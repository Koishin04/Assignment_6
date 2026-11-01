package social;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

@Timeout(40)
class SocialNetworkTest {
    private Event reaction() { return new Event(EventType.REACTED, "java", "p1", "Like", 1); }
    private Group group() {
        return new Group("java", List.of(new Post("p1", "Hello", 10),
                new Comment("c1", "spam link", 2), new Image("i1", "Diagram", 3),
                new Video("v1", "Java tutorial", 5)));
    }
    private Community community() {
        return new Community("campus", List.of(group(),
                new Group("games", List.of(new Post("p2", "spam offer", 4)))));
    }

    @Test void deliversToSubscribers() {
        EventHub hub = new EventHub();
        AtomicInteger a = new AtomicInteger();
        AtomicInteger b = new AtomicInteger();
        hub.subscribe(EventType.REACTED, event -> a.incrementAndGet());
        hub.subscribe(EventType.REACTED, event -> b.incrementAndGet());
        hub.publish(reaction());
        assertEquals(1, a.get());
        assertEquals(1, b.get());
    }

    @Test void filtersEventTypes() {
        EventHub hub = new EventHub();
        AtomicInteger calls = new AtomicInteger();
        hub.subscribe(EventType.POSTED, event -> calls.incrementAndGet());
        hub.publish(reaction());
        assertEquals(0, calls.get());
    }

    @Test void duplicateSubscriptionIsIgnored() {
        EventHub hub = new EventHub();
        AtomicInteger calls = new AtomicInteger();
        Observer<Event> observer = event -> calls.incrementAndGet();
        hub.subscribe(EventType.REACTED, observer);
        hub.subscribe(EventType.REACTED, observer);
        hub.publish(reaction());
        assertEquals(1, calls.get());
    }

    @Test void listenerCanUnsubscribeInsideHandler() {
        EventHub hub = new EventHub();
        AtomicInteger calls = new AtomicInteger();
        Observer<Event> observer = new Observer<>() {
            public void onEvent(Event event) {
                calls.incrementAndGet();
                hub.unsubscribe(event.type(), this);
            }
        };
        hub.subscribe(EventType.REACTED, observer);
        hub.publish(reaction());
        hub.publish(reaction());
        assertEquals(1, calls.get());
    }

    @Test void listenerCanSubscribeInsideHandler() {
        EventHub hub = new EventHub();
        AtomicInteger calls = new AtomicInteger();
        Observer<Event> late = event -> calls.incrementAndGet();
        hub.subscribe(EventType.REACTED, event -> hub.subscribe(EventType.REACTED, late));
        hub.publish(reaction());
        assertEquals(0, calls.get());
        hub.publish(reaction());
        assertEquals(1, calls.get());
    }

    @Test void concurrentRemovalKeepsCurrentSnapshot() throws Exception {
        EventHub hub = new EventHub();
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger calls = new AtomicInteger();
        Observer<Event> observer = event -> calls.incrementAndGet();
        hub.subscribe(EventType.REACTED, event -> {
            entered.countDown();
            try { release.await(); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        });
        hub.subscribe(EventType.REACTED, observer);
        var pool = Executors.newSingleThreadExecutor();
        try {
            var result = pool.submit(() -> hub.publish(reaction()));
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            hub.unsubscribe(EventType.REACTED, observer);
            release.countDown();
            result.get(5, TimeUnit.SECONDS);
            hub.publish(reaction());
            assertEquals(1, calls.get());
        } finally {
            release.countDown();
            pool.shutdownNow();
            assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test void failureDoesNotSkipOtherSubscribers() {
        EventHub hub = new EventHub();
        AtomicInteger calls = new AtomicInteger();
        hub.subscribe(EventType.REACTED, event -> { throw new IllegalStateException(); });
        hub.subscribe(EventType.REACTED, event -> calls.incrementAndGet());
        assertThrows(RuntimeException.class, () -> hub.publish(reaction()));
        assertEquals(1, calls.get());
    }

    @Test void postSourceUpdatesFeed() {
        EventHub hub = new EventHub();
        Feed feed = new Feed();
        hub.subscribe(EventType.POSTED, feed);
        new PostSource(hub).post("java", "p1", "Hello");
        assertEquals(1, feed.size());
    }

    @Test void reactionSourceUpdatesStatistics() {
        EventHub hub = new EventHub();
        EngagementStatistics stats = new EngagementStatistics(new EventHub());
        hub.subscribe(EventType.REACTED, stats);
        new ReactionSource(hub).like("java", "p1");
        assertEquals(1, stats.total("java"));
    }

    @Test void observerChainUpdatesTrending() {
        EngagementStatistics stats = new EngagementStatistics(new EventHub());
        TrendingTracker trending = new TrendingTracker();
        stats.subscribe(EventType.SUMMARY, trending);
        stats.onEvent(reaction());
        stats.onEvent(reaction());
        assertEquals(2, trending.score("java"));
    }

    @Test void olderSummaryCannotLowerScore() {
        TrendingTracker trending = new TrendingTracker();
        trending.onEvent(new Event(EventType.SUMMARY, "java", "", "", 10));
        trending.onEvent(new Event(EventType.SUMMARY, "java", "", "", 4));
        assertEquals(10, trending.score("java"));
    }

    @Test void groupsHaveSeparateTotals() {
        EngagementStatistics stats = new EngagementStatistics(new EventHub());
        stats.onEvent(reaction());
        stats.onEvent(new Event(EventType.REACTED, "games", "p2", "Like", 3));
        assertEquals(1, stats.total("java"));
        assertEquals(3, stats.total("games"));
    }

    @Test void childListIsDefensivelyCopied() {
        List<Content> children = new ArrayList<>();
        children.add(new Post("p", "Hello", 1));
        Group group = new Group("g", children);
        children.clear();
        assertEquals(1, group.children().size());
        assertThrows(UnsupportedOperationException.class, () -> group.children().clear());
    }

    @Test void engagementMatchesHandCalculation() {
        assertEquals(24L, community().accept(new EngagementVisitor()).longValue());
    }

    @Test void statisticsAccumulateAcrossWholeTree() {
        StatisticsVisitor visitor = new StatisticsVisitor();
        community().accept(visitor);
        assertEquals(8, visitor.total());
    }

    @Test void filterReturnsOnlyComments() {
        List<Content> result = community().accept(new FilterVisitor(node -> node instanceof Comment));
        assertEquals(List.of("c1"), result.stream().map(Content::id).toList());
    }

    @Test void doubleDispatchWorksThroughContentReference() {
        Content node = new Image("i", "Photo", 12);
        assertEquals(12L, node.accept(new EngagementVisitor()).longValue());
    }

    @Test void reportInspectsOnlyRelatedGroupAndPublishesResult() {
        Community community = community();
        EventHub reviews = new EventHub();
        ReviewLog log = new ReviewLog();
        reviews.subscribe(EventType.REVIEWED, log);
        ModerationService moderation = new ModerationService(
                Map.of("java", community.groups().get(0), "games", community.groups().get(1)),
                node -> node.id().equals("c1") || node.id().equals("p2"), reviews);
        EventHub posts = new EventHub();
        posts.subscribe(EventType.REPORTED, moderation);
        new PostSource(posts).report("java", "c1");
        assertEquals(1, log.entries().size());
        assertEquals("c1", log.entries().get(0).message());
        assertEquals(1, log.entries().get(0).value());
    }

    @Test void unknownGroupIsReportedAsFailure() {
        ModerationService moderation = new ModerationService(Map.of(), node -> true, new EventHub());
        assertThrows(IllegalArgumentException.class, () -> moderation.onEvent(
                new Event(EventType.REPORTED, "missing", "p", "", 0)));
    }

    @Test void stressFourProducersPublishExactlyFourThousandEvents() throws Exception {
        EventHub hub = new EventHub();
        EngagementStatistics stats = new EngagementStatistics(new EventHub());
        TrendingTracker trending = new TrendingTracker();
        AtomicInteger delivered = new AtomicInteger();
        AtomicInteger summaries = new AtomicInteger();
        hub.subscribe(EventType.REACTED, stats);
        hub.subscribe(EventType.REACTED, event -> delivered.incrementAndGet());
        stats.subscribe(EventType.SUMMARY, trending);
        stats.subscribe(EventType.SUMMARY, event -> summaries.incrementAndGet());
        ReactionSource source = new ReactionSource(hub);
        CyclicBarrier start = new CyclicBarrier(4);
        List<Runnable> producers = new ArrayList<>();
        for (int i = 0; i < 4; i++) producers.add(() -> {
            try { start.await(5, TimeUnit.SECONDS); }
            catch (Exception e) { throw new IllegalStateException(e); }
            for (int j = 0; j < 1000; j++) source.like("java", "p1");
        });
        var pool = Executors.newFixedThreadPool(4);
        new ProducerRunner(pool).run(producers);
        assertTrue(pool.isTerminated());
        assertEquals(4000, delivered.get());
        assertEquals(4000, summaries.get());
        assertEquals(4000, stats.total("java"));
        assertEquals(4000, trending.score("java"));
    }

    @Test void producerFailureIsNotSilentlyIgnored() {
        var pool = Executors.newFixedThreadPool(3);
        assertThrows(ExecutionException.class, () -> new ProducerRunner(pool).run(
                List.of(() -> { throw new IllegalStateException("Failure"); })));
        assertTrue(pool.isTerminated());
    }

    @Test void jsonHasCorrectNestedStructure() {
        Community tree = new Community("c", List.of(new Group("g", List.of(new Post("p", "Hi", 2)))));
        String expected = "{\"type\":\"community\",\"id\":\"c\",\"children\":["
                + "{\"type\":\"group\",\"id\":\"g\",\"children\":["
                + "{\"type\":\"post\",\"id\":\"p\",\"text\":\"Hi\",\"likes\":2}]}]}";
        assertEquals(expected, tree.accept(new JsonVisitor()));
    }

    @Test void jsonEscapesQuotesBackslashesAndControlCharacters() {
        Post post = new Post("p", "\"\\\n\t\b", 0);
        assertEquals("{\"type\":\"post\",\"id\":\"p\",\"text\":\"\\\"\\\\\\u000a\\u0009\\u0008\",\"likes\":0}",
                post.accept(new JsonVisitor()));
    }

    @Test void newVideoElementWorksWithEveryVisitor() {
        Video video = new Video("v", "Tutorial", 5);
        assertEquals(5L, video.accept(new EngagementVisitor()).longValue());
        assertEquals(1, video.accept(new StatisticsVisitor()).intValue());
        assertEquals(List.of(video), video.accept(new FilterVisitor(node -> node instanceof Video)));
        assertEquals("{\"type\":\"video\",\"id\":\"v\",\"text\":\"Tutorial\",\"likes\":5}",
                video.accept(new JsonVisitor()));
    }


    @Test void reportDoesNotFlagAnotherItemInSameGroup() {
        Group group = new Group("java", List.of(
            new Post("p1", "Clean post", 0),
            new Comment("c1", "spam link", 0)));

        EventHub reviews = new EventHub();
        ReviewLog log = new ReviewLog();
        reviews.subscribe(EventType.REVIEWED, log);

        ModerationService moderation = new ModerationService(
            Map.of("java", group),
            node -> node instanceof Comment comment
                    && comment.text().contains("spam"),
            reviews);

        moderation.onEvent(new Event(
                EventType.REPORTED, "java", "p1", "Review requested", 0));
    
        assertEquals(1, log.entries().size());
        assertEquals("p1", log.entries().get(0).contentId());
        assertEquals("", log.entries().get(0).message());
        assertEquals(0, log.entries().get(0).value());
}
}

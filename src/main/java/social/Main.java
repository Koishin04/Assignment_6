package social;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.function.Predicate;

public final class Main {
    public static void main(String[] args) throws Exception {
        int threads = args.length == 0 ? 3 : Integer.parseInt(args[0]);
        if (threads < 3) throw new IllegalArgumentException("Use at least 3 threads");

        Group java = new Group("java", List.of(new Post("p1", "Patterns", 10),
                new Comment("c1", "spam link", 2), new Image("i1", "Diagram", 3),
                new Video("v1", "Java tutorial", 5)));
        Group games = new Group("games", List.of(new Post("p2", "spam offer", 4)));
        Community community = new Community("campus", List.of(java, games));

        EventHub postEvents = new EventHub();
        EventHub reactionEvents = new EventHub();
        EventHub reviewEvents = new EventHub();
        PostSource posts = new PostSource(postEvents);
        ReactionSource reactions = new ReactionSource(reactionEvents);
        Feed feed = new Feed();
        NotificationCenter notifications = new NotificationCenter();
        EngagementStatistics statistics = new EngagementStatistics(new EventHub());
        TrendingTracker trending = new TrendingTracker();
        ReviewLog reviewLog = new ReviewLog();
        Predicate<Content> rule = node ->
                node instanceof Post post && post.text().contains("spam") ||
                node instanceof Comment comment && comment.text().contains("spam");
        ModerationService moderation = new ModerationService(
                Map.of(java.id(), java, games.id(), games), rule, reviewEvents);

        postEvents.subscribe(EventType.POSTED, feed);
        postEvents.subscribe(EventType.POSTED, notifications);
        postEvents.subscribe(EventType.REPORTED, moderation);
        reactionEvents.subscribe(EventType.REACTED, notifications);
        reactionEvents.subscribe(EventType.REACTED, statistics);
        statistics.subscribe(EventType.SUMMARY, trending);
        reviewEvents.subscribe(EventType.REVIEWED, reviewLog);

        List<Runnable> producers = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            int producer = i;
            producers.add(() -> {
                for (int j = 0; j < 100; j++) {
                    posts.post("java", "live-" + producer + "-" + j, "New post");
                    reactions.like("java", "p1");
                }
            });
        }

        //ReportCounter reportCounter = new ReportCounter();
        //postEvents.subscribe(EventType.REPORTED, reportCounter);

        new ProducerRunner(Executors.newFixedThreadPool(threads)).run(producers);
        posts.report("java", "c1");

        System.out.println("Feed: " + feed.size());
        System.out.println("Notifications: " + notifications.size());
        System.out.println("Live reactions: " + statistics.total("java"));
        System.out.println("Trending score: " + trending.score("java"));
        System.out.println("Snapshot engagement: " + community.accept(new EngagementVisitor()));
        System.out.println("Snapshot nodes: " + community.accept(new StatisticsVisitor()));
        System.out.println("Review: " + reviewLog.entries());
        System.out.println("Reports: " + )
        System.out.println(community.accept(new JsonVisitor()));
    }
}

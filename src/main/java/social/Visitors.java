package social;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

final class EngagementVisitor implements Visitor<Long> {
    private long sum(List<? extends Content> children) {
        return children.stream().mapToLong(child -> child.accept(this)).sum();
    }
    public Long visit(Community node) { return sum(node.groups()); }
    public Long visit(Group node) { return sum(node.children()); }
    public Long visit(Post node) { return node.likes(); }
    public Long visit(Comment node) { return node.likes(); }
    public Long visit(Image node) { return node.likes(); }
    public Long visit(Video node) { return node.likes(); }
}

final class StatisticsVisitor implements Visitor<Integer> {
    private int total;
    int total() { return total; }

    private int count(List<? extends Content> children) {
        total++;
        for (Content child : children) child.accept(this);
        return total;
    }
    public Integer visit(Community node) { return count(node.groups()); }
    public Integer visit(Group node) { return count(node.children()); }
    public Integer visit(Post node) { return ++total; }
    public Integer visit(Comment node) { return ++total; }
    public Integer visit(Image node) { return ++total; }
    public Integer visit(Video node) { return ++total; }
}

final class FilterVisitor implements Visitor<List<Content>> {
    private final Predicate<Content> condition;
    FilterVisitor(Predicate<Content> condition) { this.condition = condition; }

    private List<Content> match(Content node) {
        return condition.test(node) ? List.of(node) : List.of();
    }
    private List<Content> collect(Content node, List<? extends Content> children) {
        List<Content> result = new ArrayList<>(match(node));
        for (Content child : children) result.addAll(child.accept(this));
        return result;
    }
    public List<Content> visit(Community node) { return collect(node, node.groups()); }
    public List<Content> visit(Group node) { return collect(node, node.children()); }
    public List<Content> visit(Post node) { return match(node); }
    public List<Content> visit(Comment node) { return match(node); }
    public List<Content> visit(Image node) { return match(node); }
    public List<Content> visit(Video node) { return match(node); }
}








/*
    int total = 0;
        for (Content child : children) {
            total += child.accept(this);
        }
        return total;
    }

    public Integer visit(Community community) {
        return count(community.groups());
    }

    public Integer visit(Group group) {
        return count(group.children());
    }

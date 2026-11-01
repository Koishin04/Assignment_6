package social;

import java.util.List;

interface Content {
    String id();
    <R> R accept(Visitor<R> visitor);
}

record Community(String id, List<Group> groups) implements Content {
    Community { groups = List.copyOf(groups); }
    public <R> R accept(Visitor<R> visitor) { return visitor.visit(this); }
}

record Group(String id, List<Content> children) implements Content {
    Group { children = List.copyOf(children); }
    public <R> R accept(Visitor<R> visitor) { return visitor.visit(this); }
}

record Post(String id, String text, long likes) implements Content {
    public <R> R accept(Visitor<R> visitor) { return visitor.visit(this); }
}

record Comment(String id, String text, long likes) implements Content {
    public <R> R accept(Visitor<R> visitor) { return visitor.visit(this); }
}

record Image(String id, String caption, long likes) implements Content {
    public <R> R accept(Visitor<R> visitor) { return visitor.visit(this); }
}

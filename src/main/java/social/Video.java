package social;

record Video(String id, String title, long likes) implements Content {
    public <R> R accept(Visitor<R> visitor) { return visitor.visit(this); }
}

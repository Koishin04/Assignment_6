package social;

interface Visitor<R> {
    R visit(Community community);
    R visit(Group group);
    R visit(Post post);
    R visit(Comment comment);
    R visit(Image image);
    R visit(Video video);
}

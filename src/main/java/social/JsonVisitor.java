package social;

import java.util.List;
import java.util.stream.Collectors;

final class JsonVisitor implements Visitor<String> {
    private String quote(String text) {
        StringBuilder result = new StringBuilder("\"");
        for (char c : text.toCharArray()) {
            if (c == '"' || c == '\\') result.append('\\').append(c);
            else if (c < 32) result.append(String.format("\\u%04x", (int) c));
            else result.append(c);
        }
        return result.append('"').toString();
    }

    private String container(String type, String id, List<? extends Content> children) {
        String items = children.stream().map(child -> child.accept(this)).collect(Collectors.joining(","));
        return "{\"type\":" + quote(type) + ",\"id\":" + quote(id) + ",\"children\":[" + items + "]}";
    }

    private String leaf(String type, String id, String text, long likes) {
        return "{\"type\":" + quote(type) + ",\"id\":" + quote(id)
                + ",\"text\":" + quote(text) + ",\"likes\":" + likes + "}";
    }

    public String visit(Community node) { return container("community", node.id(), node.groups()); }
    public String visit(Group node) { return container("group", node.id(), node.children()); }
    public String visit(Post node) { return leaf("post", node.id(), node.text(), node.likes()); }
    public String visit(Comment node) { return leaf("comment", node.id(), node.text(), node.likes()); }
    public String visit(Image node) { return leaf("image", node.id(), node.caption(), node.likes()); }
    public String visit(Video node) { return leaf("video", node.id(), node.title(), node.likes()); }
}

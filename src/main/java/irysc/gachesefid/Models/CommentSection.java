package irysc.gachesefid.Models;

public enum CommentSection {
    TEACH, CONTENT, ADVISOR;

    public String getName() {
        return name().toLowerCase();
    }
}

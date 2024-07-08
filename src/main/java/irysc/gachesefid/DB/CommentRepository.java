package irysc.gachesefid.DB;

import irysc.gachesefid.Main.GachesefidApplication;

public class CommentRepository extends Common{
    @Override
    void init() {
        documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection("comments");
    }

    public CommentRepository() {
        init();
    }
}

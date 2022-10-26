package irysc.gachesefid.DB;

import irysc.gachesefid.Main.GachesefidApplication;


public class ContentRepository extends Common {

    public ContentRepository() {
        init();
    }

    @Override
    void init() {
        table = "content";
        documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection(table);
    }
}

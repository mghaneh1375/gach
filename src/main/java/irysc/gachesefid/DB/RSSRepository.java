package irysc.gachesefid.DB;

import irysc.gachesefid.Main.GachesefidApplication;

public class RSSRepository extends Common {

    public RSSRepository() {
        init();
    }

    @Override
    void init() {
        table = "rss";
        secKey = "today";
        documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection(table);
    }

}

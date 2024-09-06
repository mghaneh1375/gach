package irysc.gachesefid.DB;

import irysc.gachesefid.Main.GachesefidApplication;

public class BadgeRepository extends Common {

    public BadgeRepository() {
        init();
    }

    @Override
    void init() {
        table = "badge";
        documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection(table);
    }

}

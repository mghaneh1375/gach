package irysc.gachesefid.DB;

import irysc.gachesefid.Main.GachesefidApplication;


public class GiftRepository extends Common {

    public GiftRepository() {
        init();
    }

    @Override
    void init() {
        table = "gift";
        documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection(table);
    }
}

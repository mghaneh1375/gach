package irysc.gachesefid.DB;

import irysc.gachesefid.Main.GachesefidApplication;

public class NotifRepository extends Common {

    public NotifRepository() {
        init();
    }

    @Override
    void init() {
        table = "notif";
        documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection(table);
    }
}

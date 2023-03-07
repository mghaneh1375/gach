package irysc.gachesefid.DB;

import irysc.gachesefid.Main.GachesefidApplication;

public class AdminNotifRepository extends Common {

    public AdminNotifRepository() {
        init();
    }

    @Override
    void init() {
        table = "admin_notif";
        documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection(table);
    }
}

package irysc.gachesefid.DB;

import irysc.gachesefid.Main.GachesefidApplication;

public class AlertsRepository extends Common {

    public AlertsRepository() {
        init();
    }

    @Override
    void init() {
        documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection("alert");
    }

}

package irysc.gachesefid.DB;

import irysc.gachesefid.Main.GachesefidApplication;

public class DailyAdvRepository extends Common{

    public DailyAdvRepository() {
        init();
    }
    @Override
    void init() {
        documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection("daily_adv");
    }
}

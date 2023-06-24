package irysc.gachesefid.DB;

import irysc.gachesefid.Main.GachesefidApplication;

public class ScheduleRepository extends Common {

    public ScheduleRepository() {
        init();
    }

    @Override
    void init() {
        table = "schedule";
        documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection(table);
    }

}

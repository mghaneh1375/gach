package irysc.gachesefid.DB;

import irysc.gachesefid.Main.GachesefidApplication;

public class TeachScheduleRepository extends Common{
    @Override
    void init() {
        table = "teach_schedule";
        documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection(table);
    }

    public TeachScheduleRepository() {
        init();
    }
}

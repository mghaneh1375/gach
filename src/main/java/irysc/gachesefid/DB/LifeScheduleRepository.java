package irysc.gachesefid.DB;

import irysc.gachesefid.Main.GachesefidApplication;

public class LifeScheduleRepository extends Common {

    public LifeScheduleRepository() {
        init();
    }

    @Override
    void init() {
        table = "life_schedule";
        secKey = "user_id";
        documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection(table);
    }

}

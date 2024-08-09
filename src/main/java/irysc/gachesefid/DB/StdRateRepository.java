package irysc.gachesefid.DB;

import irysc.gachesefid.Main.GachesefidApplication;

public class StdRateRepository extends Common {

    @Override
    void init() {
        documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection("std_rate_in_teach");
    }

    public StdRateRepository() {
        init();
    }
}

package irysc.gachesefid.DB;

import irysc.gachesefid.Main.GachesefidApplication;

public class TeachRateRepository extends Common {

    @Override
    void init() {
        documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection("teach_rate");
    }

    public TeachRateRepository() {
        init();
    }
}

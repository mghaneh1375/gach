package irysc.gachesefid.DB;

import irysc.gachesefid.Main.GachesefidApplication;

public class TeachRateRepository extends Common {

    @Override
    void init() {
        table = "teach_rate";
        documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection(table);
    }

    public TeachRateRepository() {
        init();
    }
}

package irysc.gachesefid.DB;

import irysc.gachesefid.Main.GachesefidApplication;

public class TeachReportRepository extends Common {

    @Override
    void init() {
        table = "teach_report";
        documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection(table);
    }

    public TeachReportRepository() {
        init();
    }
}

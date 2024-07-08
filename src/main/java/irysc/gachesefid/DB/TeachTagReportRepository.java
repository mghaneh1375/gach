package irysc.gachesefid.DB;

import irysc.gachesefid.Main.GachesefidApplication;

public class TeachTagReportRepository extends Common {

    @Override
    void init() {
        table = "teach_tag_report";
        documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection(table);
    }

    public TeachTagReportRepository() {
        init();
    }
}

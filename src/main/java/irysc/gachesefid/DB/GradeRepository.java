package irysc.gachesefid.DB;

import irysc.gachesefid.Main.GachesefidApplication;


public class GradeRepository extends Common {

    public GradeRepository() {
        init();
    }

    @Override
    void init() {
        table = "grade";
        documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection(table);
    }
}

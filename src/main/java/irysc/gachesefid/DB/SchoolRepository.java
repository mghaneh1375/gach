package irysc.gachesefid.DB;

import irysc.gachesefid.Main.GachesefidApplication;

public class SchoolRepository extends Common {

    public SchoolRepository() {
        init();
    }

    @Override
    void init() {
        table = "school";
        documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection(table);
    }
}

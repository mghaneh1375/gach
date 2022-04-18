package irysc.gachesefid.DB;

import irysc.gachesefid.Main.GachesefidApplication;


public class SubjectRepository extends Common {

    public SubjectRepository() {
        init();
    }

    @Override
    void init() {
        table = "subject";
        documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection(table);
    }

}

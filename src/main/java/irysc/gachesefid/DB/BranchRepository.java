package irysc.gachesefid.DB;

import irysc.gachesefid.Main.GachesefidApplication;


public class BranchRepository extends Common {

    public BranchRepository() {
        init();
    }

    @Override
    void init() {
        table = "branch";
        documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection(table);
    }
}

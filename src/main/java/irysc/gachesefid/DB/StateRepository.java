package irysc.gachesefid.DB;

import irysc.gachesefid.Main.GachesefidApplication;

public class StateRepository extends Common {

    public StateRepository() {
        init();
    }

    @Override
    void init() {
        table = "state";
        documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection(table);
    }
}

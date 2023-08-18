package irysc.gachesefid.DB;

import irysc.gachesefid.Main.GachesefidApplication;


public class CreditRepository extends Common {

    public CreditRepository() {
        init();
    }

    @Override
    void init() {
        documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection("credit");
    }
}

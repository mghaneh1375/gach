package irysc.gachesefid.DB;

import irysc.gachesefid.Main.GachesefidApplication;

public class PayLinkRepository extends Common {

    public PayLinkRepository() {
        init();
    }

    @Override
    void init() {
        documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection("pay_link");
    }
}

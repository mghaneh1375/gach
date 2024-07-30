package irysc.gachesefid.DB;

import irysc.gachesefid.Main.GachesefidApplication;

public class SettlementRequestRepository extends Common {

    public SettlementRequestRepository() {
        init();
    }

    @Override
    void init() {
        documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection("settlement_request");
    }

}

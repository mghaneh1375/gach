package irysc.gachesefid.DB;

import irysc.gachesefid.Main.GachesefidApplication;

public class AccessRequestRepository extends Common {

    public AccessRequestRepository() {
        init();
    }

    @Override
    void init() {
        documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection("access_request");
    }
}

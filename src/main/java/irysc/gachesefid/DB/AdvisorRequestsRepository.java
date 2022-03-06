package irysc.gachesefid.DB;

import irysc.gachesefid.Main.GachesefidApplication;

public class AdvisorRequestsRepository extends Common {

    public AdvisorRequestsRepository() {
        init();
    }

    @Override
    void init() {
        documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection("advisor_requests");
    }

}

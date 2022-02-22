package irysc.gachesefid.DB;

import irysc.gachesefid.Main.GachesefidApplication;

public class RequestRepository extends Common {

    public RequestRepository() {
        init();
    }

    @Override
    void init() {
        documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection("request");
    }

}

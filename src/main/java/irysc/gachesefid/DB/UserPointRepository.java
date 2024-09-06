package irysc.gachesefid.DB;

import irysc.gachesefid.Main.GachesefidApplication;

public class UserPointRepository extends Common {

    public UserPointRepository() {
        init();
    }

    @Override
    void init() {
        documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection("user_point");
    }

}

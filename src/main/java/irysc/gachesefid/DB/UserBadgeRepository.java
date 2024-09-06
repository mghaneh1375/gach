package irysc.gachesefid.DB;

import irysc.gachesefid.Main.GachesefidApplication;

public class UserBadgeRepository extends Common {

    public UserBadgeRepository() {
        init();
    }

    @Override
    void init() {
        table = "user_badge";
        secKey = "user_id";
        documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection(table);
    }

}

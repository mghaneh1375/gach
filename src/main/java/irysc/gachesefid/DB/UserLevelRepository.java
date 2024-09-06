package irysc.gachesefid.DB;

import irysc.gachesefid.Main.GachesefidApplication;

public class UserLevelRepository extends Common {

    public UserLevelRepository() {
        init();
    }

    @Override
    void init() {
        table = "user_level";
        secKey = "user_id";
        documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection(table);
    }

}

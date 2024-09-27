package irysc.gachesefid.DB;

import irysc.gachesefid.Main.GachesefidApplication;

public class ProfileConfigRepository extends Common {

    public ProfileConfigRepository() {
        init();
    }

    @Override
    void init() {
        table = "profile_config";
        secKey = "user_id";
        documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection(table);
    }
}

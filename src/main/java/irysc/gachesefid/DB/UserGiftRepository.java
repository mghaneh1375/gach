package irysc.gachesefid.DB;

import irysc.gachesefid.Main.GachesefidApplication;


public class UserGiftRepository extends Common {

    public UserGiftRepository() {
        init();
    }

    @Override
    void init() {
        table = "user_gift";
        secKey = "unique_key";
        documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection(table);
    }
}

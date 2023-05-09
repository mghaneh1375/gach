package irysc.gachesefid.DB;

import irysc.gachesefid.Main.GachesefidApplication;

public class TarazRepository extends Common {

    public TarazRepository() {
        init();
    }

    @Override
    void init() {
        table = "taraz_ranking";
        secKey = "user_id";
        documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection(table);
    }

}

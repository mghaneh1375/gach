package irysc.gachesefid.DB;

import irysc.gachesefid.Main.GachesefidApplication;

public class LevelRepository extends Common {

    public LevelRepository() {
        init();
    }

    @Override
    void init() {
        table = "level";
        documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection(table);
    }

}

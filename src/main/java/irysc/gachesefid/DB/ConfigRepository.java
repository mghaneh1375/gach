package irysc.gachesefid.DB;

import irysc.gachesefid.Main.GachesefidApplication;

public class ConfigRepository extends Common {

    public ConfigRepository() {
        init();
    }

    @Override
    void init() {
        table = "config";
        secKey = "first_row";
        documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection(table);
    }

}

package irysc.gachesefid.DB;

import irysc.gachesefid.Main.GachesefidApplication;

public class CityRepository extends Common {

    public CityRepository() {
        init();
    }

    @Override
    void init() {
        table = "city";
        documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection(table);
    }
}

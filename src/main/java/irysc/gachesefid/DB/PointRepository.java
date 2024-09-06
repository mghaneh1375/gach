package irysc.gachesefid.DB;

import irysc.gachesefid.Main.GachesefidApplication;

public class PointRepository extends Common {

    public PointRepository() {
        init();
    }

    @Override
    void init() {
        table = "point";
        secKey = "action";
        documentMongoCollection = GachesefidApplication.mongoDatabase.getCollection(table);
    }

}
